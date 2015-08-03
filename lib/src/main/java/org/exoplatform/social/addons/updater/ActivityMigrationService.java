package org.exoplatform.social.addons.updater;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.XPathUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;
import org.exoplatform.social.addons.updater.utils.MigrationCounter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityParameters;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.utils.ActivityIterator;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.query.JCRProperties;

@Managed
@ManagedDescription("Social migration activities from JCR to RDBMS.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-activities") })
public class ActivityMigrationService extends AbstractMigrationService<ExoSocialActivity> {
  private static final int LIMIT_REMOVED_THRESHOLD = 10;
  private static final int LIMIT_ACTIVITY_SAVE_THRESHOLD = 10;
  private static final int LIMIT_ACTIVITY_REF_SAVE_THRESHOLD = 50;
  public static final String EVENT_LISTENER_KEY = "SOC_ACTIVITY_MIGRATION";
  private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");
  public static final Pattern USER_NAME_VALIDATOR_REGEX = Pattern.compile("^[\\p{L}][\\p{L}._\\-\\d]+$");
  public final static String COMMENT_PREFIX = "comment";
  
  private final ActivityStorage activityStorage;
  private final ActivityStorageImpl activityJCRStorage;
  private final CommentDAO commentDAO;
  private final ActivityDAO activityDAO;

  private String previousActivityId = null;
  private ActivityEntity lastActivity = null;
  private String lastUserProcess = null;
  private boolean forceStop = false;
  
  public ActivityMigrationService(InitParams initParams,
                                  CommentDAO commentDAO,
                                  ActivityDAO activityDAO,
                                  ActivityStorage activityStorage,
                                  ActivityStorageImpl activityJCRStorage,
                                  IdentityStorageImpl identityStorage,
                                  EventManager<ExoSocialActivity, String> eventManager,
                                  EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.commentDAO = commentDAO;
    this.activityDAO = activityDAO;
    this.activityStorage = activityStorage;
    this.activityJCRStorage = activityJCRStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
  }

  @Managed
  @ManagedDescription("Manual to start run miguration data of activities from JCR to RDBMS.")
  public void doMigration() throws Exception {
    migrateUserActivities();
    // migrate activities from space
    migrateSpaceActivities();
    
    MigrationContext.setActivityDone(true);
    LOG.info("Done to migration activities from JCR to RDBMS");
  }

  private void migrateUserActivities() throws Exception {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    boolean begunTx = startTx();
    MigrationCounter counter = MigrationCounter.builder().threshold(LIMIT_THRESHOLD).build();
    counter.newTotalAndWatch();
    // doing with normal users
    NodeIterator it = getIdentityNodes(counter.getTotal(), LIMIT_THRESHOLD);
    Identity owner = null; 
    Node node = null;
    try {
      while (it.hasNext()) {
        if(forceStop) {
          return;
        }
        node = (Node) it.next();
        owner = identityStorage.findIdentityById(node.getUUID());
        counter.newBatchAndWatch();
        counter.getAndIncrementTotal();
        LOG.info(String.format("|  \\ START::user number: %s (%s user)", counter.getTotal(), owner.getRemoteId()));
        IdentityEntity identityEntity = _findById(IdentityEntity.class, owner.getId());
        migrationByIdentity(null, identityEntity);
        
        LOG.info(String.format("| / END:: user's activity consumed %s(ms) -------------", counter.endBatchWatch()));
        //
        if (counter.isPersistPoint()) {
          endTx(begunTx);
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = startTx();
          it = getIdentityNodes(counter.getTotal(), LIMIT_THRESHOLD);
        }
      }
      LOG.info(String.format("| / END::%s user(s) consumed %s(ms) -------------", counter.getTotal(), counter.endTotalWatch()));
    } catch (Exception e) {
      LOG.error("Failed to migration for user Activity.", e);
    } finally {
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
    }
  }

  private void migrateSpaceActivities() throws Exception {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    long t = System.currentTimeMillis();
    NodeIterator it = getSpaceIdentityNodes();
    if (it == null) return;
    int size = (int) it.getSize(), count = 0;
    boolean begunTx = startTx();
    Node node = null;
    long offset = 0;
    boolean isSkip = (lastUserProcess != null);
    Identity owner = null; 
    try {
      while (it.hasNext()) {
        if (forceStop) {
          return;
        }
        node = (Node) it.next();
        offset++;
        owner = identityStorage.findIdentityById(node.getUUID());
        if (isSkip) {
          if (lastUserProcess.equals(owner.getRemoteId())) {
            lastUserProcess = null;
            isSkip = false;
          } else {
            continue;
          }
        }
        //
        IdentityEntity spaceEntity = _findById(IdentityEntity.class, node.getUUID());
        migrationByIdentity(null, spaceEntity);
        ++count;
        processLog("Activities migration spaces", size, count);
        //
        if (count % LIMIT_THRESHOLD == 0) {
          LOG.info(String.format("Commit database into RDBMS and reCreate JCR-Session at offset: " + offset));
          endTx(begunTx);
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = startTx();
          it = getSpaceIdentityNodes();
          it.skip(offset);
        }
      }
      LOG.info(String.format("Done to migration %s space activities from JCR to RDBMS on %s(ms)", offset, (System.currentTimeMillis() - t)));
    } catch (Exception e) {
      LOG.error("Failed to migration for Space Activity.", e);
    } finally {
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of activities from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }
  
  protected void beforeMigration() throws Exception {
    MigrationContext.setActivityDone(false);
    LOG.info("Stating to migration activities from JCR to RDBMS........");
    NodeIterator iterator = nodes("SELECT * FROM soc:activityUpdater");
    if (iterator.hasNext()) {
      String currentUUID = iterator.nextNode().getUUID();
      lastActivity = _findById(ActivityEntity.class, currentUUID);
      if (lastActivity != null) {
        lastUserProcess = lastActivity.getPosterIdentity().getRemoteId();
      }
    }
  }

  private void migrationByIdentity(String userName, IdentityEntity identityEntity) throws Exception {
    boolean begunTx = startTx();
    try {
      if (identityEntity == null) {
        Identity poster = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, userName);
        try {
          identityEntity = _findById(IdentityEntity.class, poster.getId());
        } catch (Exception e) {
          LOG.warn("The user " + userName + " has not identity. Do not migration for this user.");
          return;
        }
      }
      String providerId = identityEntity.getProviderId();
      String type = (OrganizationIdentityProvider.NAME.equals(providerId)) ? "user" : "space";
      LOG.info(String.format("    Migration activities for %s: %s", type, identityEntity.getRemoteId()));
      //
      ActivityListEntity activityListEntity = identityEntity.getActivityList();
      ActivityIterator activityIterator = new ActivityIterator(activityListEntity);
      //
      if (lastActivity != null) {
        activityIterator.moveTo(lastActivity);
        //Only goto last on first users
        lastActivity = null;
      }
      long t = System.currentTimeMillis();
      int count = 0;
      while (activityIterator.hasNext()) {
        String activityId = activityIterator.next().getId();
        //
        ExoSocialActivity activity = activityJCRStorage.getActivity(activityId);
        Map<String, String> params = activity.getTemplateParams();
        if (params != null && !params.isEmpty()) {
          
          for(Map.Entry<String, String> entry: params.entrySet()) {
            String value = entry.getValue();
            if (value.length() >= 1024) {
              LOG.info("===================== activity id " + activity.getId() + " new value length = " +  value.length() + " - " + value);
              params.put(entry.getKey(), "");
            }
          }
          
          activity.setTemplateParams(params);
        }
        //
        Identity owner = new Identity(identityEntity.getId());
        owner.setProviderId(providerId);
        //
        activity.setId(null);
        activity = activityStorage.saveActivity(owner, activity);
        //
        doBroadcastListener(activity, activityId);
        
        //
        ActivityEntity activityEntity = getSession().findById(ActivityEntity.class, activityId);
        _getMixin(activityEntity, ActivityUpdaterEntity.class, true);
        //
        if (previousActivityId != null) {
          try {
            ActivityEntity previousActivity = getSession().findById(ActivityEntity.class, previousActivityId);
            if (previousActivity != null) {
              _removeMixin(previousActivity, ActivityUpdaterEntity.class);
            }
          } catch (Exception e) {
            LOG.error("Failed to remove mixin type," + e.getMessage(), e);
          }
        }

        List<ActivityEntity> commentEntities = activityEntity.getComments();
        for (ActivityEntity commentEntity : commentEntities) {
          ExoSocialActivity comment = fillCommentFromEntity(commentEntity);
          if (comment != null) {
            String oldCommentId = comment.getId();
            comment.setId(null);
            Map<String, String> commentParams = comment.getTemplateParams();
            if (commentParams != null && !commentParams.isEmpty()) {
              
              for(Map.Entry<String, String> entry: commentParams.entrySet()) {
                String value = entry.getValue();
                if (value.length() >= 1024) {
                  LOG.info("===================== comment id " + oldCommentId + " new value length = " +  value.length() + " - " + value);
                  commentParams.put(entry.getKey(), "");
                }
              }
              
              comment.setTemplateParams(commentParams);
            }
            activity.setTemplateParams(params);
            saveComment(activity, comment);
            //
            doBroadcastListener(comment, oldCommentId);
            commentParams = null;
            params = null;
          }
        }

        previousActivityId = activityId;
        ++count;
        //
        if(count % LIMIT_ACTIVITY_SAVE_THRESHOLD == 0) {
          endTx(begunTx);
          entityManagerService.endRequest(PortalContainer.getInstance());
          entityManagerService.startRequest(PortalContainer.getInstance());
          begunTx = startTx();
        }
      }
      LOG.info(String.format("    Done migration %s activitie(s) for %s consumed %s(ms) ", count, identityEntity.getRemoteId(), System.currentTimeMillis() - t));
    } finally {
      endTx(begunTx);
    }
  }

  private void doBroadcastListener(ExoSocialActivity activity, String oldId) {
    String newId = activity.getId();
    activity.setId(oldId);
    broadcastListener(activity, newId.replace("comment", ""));
    activity.setId(newId);
  }

  protected void afterMigration() throws Exception {
    if (forceStop) {
      return;
    }
    if (previousActivityId != null) {
      try {
        ActivityEntity previousActivity = getSession().findById(ActivityEntity.class, previousActivityId);
        if (previousActivity != null) {
          _removeMixin(previousActivity, ActivityUpdaterEntity.class);
        }
      } catch (Exception e) {
        LOG.error("Failed to remove mixin type," + e.getMessage(), e);
      }
    }
  }

  public void doRemove() throws Exception {
    LOG.info("Start remove activities from JCR to RDBMS");
    removeActivity();
    LOG.info("Done to removed activities from JCR");
  }

  private void removeActivity() {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    long t = System.currentTimeMillis();
    long offset = 0;
    NodeIterator it = getIdentityNodes(offset, LIMIT_REMOVED_THRESHOLD);
    Node node = null;
    boolean isDone = false;
    try {
      LOG.info("| \\ START::cleanup User Activity ---------------------------------");
      while (it.hasNext()) {
        node = (Node) it.next();
        LOG.info(String.format("|  \\ START::cleanup user number: %s (%s user)", offset, node.getName()));
        isDone = cleanupActivity(node);
        offset++;
        LOG.info(String.format("|  / END::cleanup (%s user)", node.getName()));
        //
        if (offset % LIMIT_REMOVED_THRESHOLD == 0 || isDone == false) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes(offset, LIMIT_REMOVED_THRESHOLD);
        }
      }
      
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::cleanup Activity for (%s) user consumed %s(ms) -------------", offset, System.currentTimeMillis() - t));
    }
    
    //cleanup activity
    t = System.currentTimeMillis();
    offset = 0;
    it = getSpaceIdentityNodes(offset, LIMIT_REMOVED_THRESHOLD);
    //don't have any space.
    if (it == null) {
      return;
    }
    
    node = null;
    offset = 0;
    try {
      LOG.info("| \\ START::cleanup Space Activity ---------------------------------");
      while (it.hasNext()) {
        node = (Node) it.next();
        LOG.info(String.format("|  \\ START::cleanup space number: %s (%s space)", offset, node.getName()));
        isDone = cleanupActivity(node);
        offset++;
        LOG.info(String.format("|  / END::cleanup (%s space)", node.getName()));
        //
        if (offset % LIMIT_REMOVED_THRESHOLD == 0 || isDone == false) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getSpaceIdentityNodes(offset, LIMIT_REMOVED_THRESHOLD);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for user Activities.", e);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::cleanup Activity for (%s) space consumed %s(ms) -------------", offset, System.currentTimeMillis() - t));
    }
    
  }
  
  /**
   * Cleanup ActivityRef for Identity
   * @param identityNode
   */
  private boolean cleanupSubNode(Node activityNode, String userName) {
    long totalTime = System.currentTimeMillis();
    NodeImpl node = null;
    long offset = 0;
    try {
      PropertyIterator pIt = activityNode.getReferences();
      while (pIt.hasNext()) {
        node = (NodeImpl) pIt.nextProperty().getParent();
        if (node.getData() != null) {
          node.remove();
          ++offset;
        }
        if (offset % LIMIT_ACTIVITY_REF_SAVE_THRESHOLD == 0) {
          getSession().save();
        }
      }
      getSession().save();
    } catch (Exception e) {
      LOG.error("Failed to cleanup sub node for Activity Reference.", e);
      return false;
    } finally {
      LOG.info(String.format("|     - Done cleanup: %s ref(s) of (%s) consumed time %s(ms) ", offset, userName, System.currentTimeMillis() - totalTime));
    }
    return true;
  }
  
  /**
   * Cleanup Activity for Identity
   * @param identityNode
   */
  private boolean cleanupActivity(Node identityNode) {
    String identityName = "", path = "";
    long totalTime = System.currentTimeMillis();
    long offset = 0;
    long size = 0;
    boolean isDone = true;
    try {
      StringBuffer sb = new StringBuffer().append("SELECT * FROM soc:activity WHERE ");
      try {
        String nodeStreamsPath = XPathUtils.escapeIllegalSQLName(identityNode.getNode("soc:activities").getPath());
        sb.append(JCRProperties.path.getName()).append(" LIKE '").append(nodeStreamsPath + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
      } catch (RepositoryException e) {
        LOG.error(e.getMessage(), e);
      }

      long t = System.currentTimeMillis();
      boolean reLoad = false;
      NodeIterator it = nodes(sb.toString());
      NodeImpl node = null;
      size = it.getSize();
      identityName = identityNode.getName();
      LOG.info(String.format("|   \\ START::cleanup: %d (Activity) for %s identity", size, identityName));
      while (it.hasNext()) {
        node = (NodeImpl) it.next();
        path = node.getPath();
        if (node.getData() != null) {
          if (cleanupSubNode(node, identityName)) {
            node.remove();
            reLoad = true;
          }
          offset++;
        }
        if (reLoad) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = nodes(sb.toString(), offset, size - offset);
          reLoad = false;
          isDone = false;
        }
        if (offset % LIMIT_ACTIVITY_SAVE_THRESHOLD == 0) {
          getSession().save();
          LOG.info(String.format("|     - Persist deleted: %s activity consumed time %s(ms) ", LIMIT_ACTIVITY_SAVE_THRESHOLD, System.currentTimeMillis() - t));
          t = System.currentTimeMillis();
        }
      }
      getSession().save();
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity: " + path, e);
      return false;
    } finally {
      LOG.info(String.format("|   / END::cleanup: %d (Activity) for %s identity consumed time %s(ms) ", size, identityName, System.currentTimeMillis() - totalTime));
    }
    return isDone;
  }

  private ExoSocialActivity fillCommentFromEntity(ActivityEntity activityEntity) {
    ExoSocialActivity comment = new ExoSocialActivityImpl();
    try {
      //
      comment.setId(activityEntity.getId());
      comment.setTitle(activityEntity.getTitle());
      comment.setTitleId(activityEntity.getTitleId());
      comment.setBody(activityEntity.getBody());
      comment.setBodyId(activityEntity.getBodyId());
      comment.setPostedTime(activityEntity.getPostedTime());
      comment.setUpdated(getLastUpdatedTime(activityEntity, comment.getPostedTime()));
      comment.isComment(activityEntity.isComment());
      comment.setType(activityEntity.getType());
      //
      String posterId = activityEntity.getPosterIdentity().getId();
      comment.setUserId(posterId);
      comment.setPosterId(posterId);
      //
      comment.setParentId(activityEntity.getParentActivity().getId());

      String[] mentioners = activityEntity.getMentioners();
      if (mentioners != null) {
        comment.setMentionedIds(mentioners);
      }
      //
      ActivityParameters params = activityEntity.getParams();
      if (params != null) {
        comment.setTemplateParams(new LinkedHashMap<String, String>(params.getParams()));
      } else {
        comment.setTemplateParams(new LinkedHashMap<String, String>());
      }
      //
      comment.isLocked(false);
      //
      HidableEntity hidable = _getMixin(activityEntity, HidableEntity.class, false);
      if (hidable != null) {
        comment.isHidden(hidable.getHidden());
      }
    } catch (Exception e) {
      LOG.warn("Failed to fill comment from entity : entity null or missing property", e);
      return null;
    }
    return comment;
  }

  private long getLastUpdatedTime(ActivityEntity activityEntity, Long postTime) {
    try {
      return activityEntity.getLastUpdated();
    } catch (Exception e) {
      return postTime;
    }
  }

  @Override
  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
  
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity eXoComment) throws ActivityStorageException {
    Activity activityEntity = activityDAO.find(Long.valueOf(activity.getId()));
    Comment commentEntity = convertCommentToCommentEntity(eXoComment);
    commentEntity.setActivity(activityEntity);
    //
    Identity commenter = identityStorage.findIdentityById(commentEntity.getPosterId());
    mention(commenter, activityEntity, processMentions(eXoComment.getTitle()));
    //
    commentEntity = commentDAO.create(commentEntity);
    eXoComment.setId(getExoCommentID(commentEntity.getId()));
    //
    activityEntity.setMentionerIds(processMentionOfComment(activityEntity, commentEntity, activity.getMentionedIds(), processMentions(eXoComment.getTitle()), true));
    activityEntity.addComment(commentEntity);
    activityDAO.update(activityEntity);
  }
  
  private String getExoCommentID(Long commentId) {
    return String.valueOf(COMMENT_PREFIX + commentId);
  }
  
  private Comment convertCommentToCommentEntity(ExoSocialActivity comment) {
    Comment commentEntity = new Comment();
    commentEntity.setTitle(comment.getTitle());
    commentEntity.setTitleId(comment.getTitleId());
    commentEntity.setBody(comment.getBody());
    commentEntity.setPosterId(comment.getPosterId());
    if (comment.getTemplateParams() != null) {
      commentEntity.setTemplateParams(comment.getTemplateParams());
    }
    //
    commentEntity.setLocked(comment.isLocked());
    commentEntity.setHidden(comment.isHidden());
    //
    commentEntity.setPosted(comment.getPostedTime());
    Calendar c = Calendar.getInstance();
    c.setTime(comment.getUpdated());
    commentEntity.setLastUpdated(c.getTimeInMillis());
    //
    return commentEntity;
  }
  
  private Set<String> processMentionOfComment(Activity activityEntity, Comment commentEntity, String[] activityMentioners, String[] commentMentioners, boolean isAdded) {
    Set<String> mentioners = new HashSet<String>(Arrays.asList(activityMentioners));
    if (commentMentioners.length == 0) return mentioners;
    //
    for (String mentioner : commentMentioners) {
      if (!mentioners.contains(mentioner) && isAdded) {
        mentioners.add(mentioner);
      }
      if (mentioners.contains(mentioner) && !isAdded) {
        if (isAllowedToRemove(activityEntity, commentEntity, mentioner)) {
          mentioners.remove(mentioner);
        }
      }
    }
    return mentioners;
  }
  
  private boolean isAllowedToRemove(Activity activity, Comment comment, String mentioner) {
    if (ArrayUtils.contains(processMentions(activity.getTitle()), mentioner)) {
      return false;
    }
    List<Comment> comments = activity.getComments();
    comments.remove(comment);
    for (Comment cmt : comments) {
      if (ArrayUtils.contains(processMentions(cmt.getTitle()), mentioner)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Processes Mentioners who has been mentioned via the Activity.
   * 
   * @param title
   */
  private String[] processMentions(String title) {
    String[] mentionerIds = new String[0];
    if (title == null || title.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    Matcher matcher = MENTION_PATTERN.matcher(title);
    while (matcher.find()) {
      String remoteId = matcher.group().substring(1);
      if (!USER_NAME_VALIDATOR_REGEX.matcher(remoteId).matches()) {
        continue;
      }
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      // if not the right mention then ignore
      if (identity != null) {
        mentionerIds = (String[]) ArrayUtils.add(mentionerIds, identity.getId());
      }
    }
    return mentionerIds;
  }
  

  /**
   * Creates StreamItem for each user who has mentioned on the activity
   * 
   * @param owner
   * @param activity
   */
  private void mention(Identity owner, Activity activity, String [] mentions) {
    for (String mentioner : mentions) {
      Identity identity = identityStorage.findIdentityById(mentioner);
      if(identity != null) {
      }
    }
  }
}
