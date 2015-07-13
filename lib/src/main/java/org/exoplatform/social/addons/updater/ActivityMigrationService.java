package org.exoplatform.social.addons.updater;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.chromattic.core.api.ChromatticSessionImpl;
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
  private final ActivityDAO activityDAO;
  private final ActivityStorage activityStorage;
  private final ActivityStorageImpl activityJCRStorage;

  private String previousActivityId = null;
  private ActivityEntity lastActivity = null;
  private String lastUserProcess = null;
  private boolean forceStop = false;
  
  public ActivityMigrationService(InitParams initParams,
                                  ActivityDAO activityDAO,
                                  ActivityStorage activityStorage,
                                  ActivityStorageImpl activityJCRStorage,
                                  IdentityStorageImpl identityStorage,
                                  EventManager<ExoSocialActivity, String> eventManager,
                                  EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.activityDAO = activityDAO;
    this.activityStorage = activityStorage;
    this.activityJCRStorage = activityJCRStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
  }

  @Managed
  @ManagedDescription("Manual to start run miguration data of activities from JCR to RDBMS.")
  public void doMigration() throws Exception {
    if(activityDAO.count() > 0) {
      MigrationContext.setActivityDone(true);
      return;
    }
    migrateUserActivities();
    // migrate activities from space
    migrateSpaceActivities();
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
        if (params != null) {
          
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
        activityStorage.saveActivity(owner, activity);
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
            params = comment.getTemplateParams();
            if (params != null) {
              
              for(Map.Entry<String, String> entry: params.entrySet()) {
                String value = entry.getValue();
                if (value.length() >= 1024) {
                  LOG.info("===================== comment id " + oldCommentId + " new value length = " +  value.length() + " - " + value);
                  params.put(entry.getKey(), "");
                }
              }
              
              comment.setTemplateParams(params);
            }
            
            activityStorage.saveComment(activity, comment);
            //
            doBroadcastListener(comment, oldCommentId);
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

    MigrationContext.setActivityDone(true);
    LOG.info("Done to migration activities from JCR to RDBMS");
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
    
    try {
      LOG.info("| \\ START::cleanup User Activity ---------------------------------");
      while (it.hasNext()) {
        node = (Node) it.next();
        LOG.info(String.format("|  \\ START::cleanup user number: %s (%s user)", offset, node.getName()));
        cleanupActivity(node);
        offset++;
        LOG.info(String.format("|  / END::cleanup (%s user)", node.getName()));
        //
        if (offset % LIMIT_REMOVED_THRESHOLD == 0) {
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
      LOG.info(String.format("| \\ START::cleanup Space Activity ---------------------------------"));
      while (it.hasNext()) {
        node = (Node) it.next();
        LOG.info(String.format("|  \\ START::cleanup space number: %s (%s space)", offset, node.getName()));
        cleanupActivity(node);
        offset++;
        LOG.info(String.format("|  / END::cleanup (%s space)", node.getName()));
        //
        if (offset % LIMIT_REMOVED_THRESHOLD == 0) {
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
  private void cleanupSubNode(Node activityNode, String userName) {
    String subNodeQuery = null;
    try {
      subNodeQuery = String.format("SELECT * FROM soc:activityref WHERE soc:target = '%s'", activityNode.getUUID());
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      return;
    }
    
    long totalTime = System.currentTimeMillis();
    NodeIterator it = nodes(subNodeQuery);
    NodeImpl node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        node = (NodeImpl) it.next();
        if (node.getData() != null) {
          node.remove();
          offset++;
        }
        
        if (offset % LIMIT_ACTIVITY_REF_SAVE_THRESHOLD == 0) {
          getSession().save();
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      getSession().save();
      LOG.info(String.format("|     - Done cleanup: %s ref(s) of (%s) consumed time %s(ms) ", offset, userName, System.currentTimeMillis() - totalTime));
    }
  }
  
  /**
   * Cleanup Activity for Identity
   * @param identityNode
   */
  private void cleanupActivity(Node identityNode) {
    String nodeStreamsPath;
    String identityName = "";

    StringBuffer sb = new StringBuffer().append("SELECT * FROM soc:activity WHERE ");
    try {
      nodeStreamsPath = XPathUtils.escapeIllegalSQLName(identityNode.getNode("soc:activities").getPath());
      sb.append(JCRProperties.path.getName()).append(" LIKE '").append(nodeStreamsPath + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }

    long t = System.currentTimeMillis();
    long totalTime = System.currentTimeMillis();
    long offset = 0;
    long size = 0;
    try {
      NodeIterator it = nodes(sb.toString());
      NodeImpl node = null;
      size = it.getSize();
      identityName = identityNode.getName();
      LOG.info(String.format("|   \\ START::cleanup: %d (Activity) for %s identity", size, identityName));
      while (it.hasNext()) {
        node = (NodeImpl) it.next();
        if (node.getData() != null) {
          cleanupSubNode(node, identityName);
          node.remove();
          offset++;
        }

        if (offset % LIMIT_ACTIVITY_SAVE_THRESHOLD == 0) {
          getSession().save();
          LOG.info(String.format("|     - Persist deleted: %s activity consumed time %s(ms) ", LIMIT_ACTIVITY_SAVE_THRESHOLD, System.currentTimeMillis() - t));
          t = System.currentTimeMillis();
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity.", e);
    } finally {
      getSession().save();
      LOG.info(String.format("|   / END::cleanup: %d (Activity) for %s identity consumed time %s(ms) ", size, identityName, System.currentTimeMillis() - totalTime));
    }
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
      comment.setUpdated(getLastUpdatedTime(activityEntity));
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
        comment.setTemplateParams(new HashMap<String, String>());
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

  private long getLastUpdatedTime(ActivityEntity activityEntity) {
    ChromatticSessionImpl chromatticSession = (ChromatticSessionImpl) getSession();
    try {
      Node node = chromatticSession.getNode(activityEntity);
      if (node.hasProperty(ActivityEntity.lastUpdated.getName())) {
        return activityEntity.getLastUpdated();
      }
    } catch (RepositoryException e) {
      LOG.debug("Failed to get last updated by activity with id = " + activityEntity.getId(), e);
    }
    return activityEntity.getPostedTime();
  }

  @Override
  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
