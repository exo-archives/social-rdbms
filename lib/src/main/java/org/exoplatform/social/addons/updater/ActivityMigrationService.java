package org.exoplatform.social.addons.updater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.chromattic.core.api.ChromatticSessionImpl;
import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityParameters;
import org.exoplatform.social.core.chromattic.entity.ActivityRef;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.utils.ActivityIterator;
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.query.JCRProperties;
import org.exoplatform.social.core.storage.streams.StreamConfig;

import com.google.caja.util.Lists;

@Managed
@ManagedDescription("Social migration activities from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-activities") })
public class ActivityMigrationService extends AbstractMigrationService<ExoSocialActivity> {
  private static final int LIMIT_IDENTITY_THRESHOLD = 50;
  private static final int LIMIT_ACTIVITY_SAVE_THRESHOLD = 50;
  public static final String EVENT_LISTENER_KEY = "SOC_ACTIVITY_MIGRATION";
  private final ActivityDAO activityDAO;
  private final ActivityStorage activityStorage;
  private final ActivityStorageImpl activityJCRStorage;
  private final String removeTypeOfActivityRef;
  private final String removeTypeOfActivity;

  private String previousActivityId = null;
  private ActivityEntity lastActivity = null;
  private String lastUserProcess = null;
  private boolean forkStop = false;
  
  public ActivityMigrationService(InitParams initParams,
                                  ActivityDAO activityDAO,
                                  ActivityStorage activityStorage,
                                  ActivityStorageImpl activityJCRStorage,
                                  IdentityStorage identityStorage,
                                  RelationshipMigrationService relationshipMigration,
                                  EventManager<ExoSocialActivity, String> eventManager,
                                  EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.activityDAO = activityDAO;
    this.activityStorage = activityStorage;
    this.activityJCRStorage = activityJCRStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
    this.removeTypeOfActivityRef = getString(initParams, "REMOVE_REF_TYPE", "DAY");
    this.removeTypeOfActivity = getString(initParams, "REMOVE_ACTIVITY_TYPE", "DAY");
  }

  @Managed
  @ManagedDescription("Manual to start run miguration data of activities from JCR to MYSQL.")
  public void doMigration() throws Exception {
    migrateUserActivities();
    // migrate activities from space
    migrateSpaceActivities();
  }

  private void migrateUserActivities() throws Exception {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    boolean begunTx = GenericDAOImpl.startTx();
    //
    if(lastUserProcess == null && activityDAO.count() > 0) {
      return;
    }
    // doing with group administrators and active users
    List<String> activeUsers = getAdminAndActiveUsers();
    long t = System.currentTimeMillis();
    int size = activeUsers.size(), count = 0;
    for (String userName : activeUsers) {
      if(forkStop) {
        return;
      }
      //
      migrationByIdentity(userName, null);
      ++count;
      //
      processLog("Activities migration admin & active users", size, count);
    }
    LOG.info(String.format("Done to migration %s admin & active user activities on %s(ms)", size, (System.currentTimeMillis() - t)));
    // doing with normal users
    t = System.currentTimeMillis();
    boolean isSkip = (lastUserProcess != null);
    NodeIterator it = getIdentityNodes();
    size = (int) it.getSize();
    count = 0;
    Identity owner = null; 
    Node node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        if(forkStop) {
          return;
        }
        offset++;
        node = (Node) it.next();
        owner = identityStorage.findIdentityById(node.getUUID());
        if (isSkip) {
          if (lastUserProcess.equals(owner.getRemoteId())) {
            lastUserProcess = null;
            isSkip = false;
          } else {
            continue;
          }
        }
        if(activeUsers.contains(owner.getRemoteId())) {
          continue;
        }
        IdentityEntity identityEntity = _findById(IdentityEntity.class, owner.getId());
        migrationByIdentity(null, identityEntity);
        ++count;
        processLog("Activities migration normal users", size, count);
        //
        if (count % LIMIT_THRESHOLD == 0) {
          LOG.info(String.format("Commit database into mysql and reCreate JCR-Session at offset: " + offset));
          GenericDAOImpl.endTx(begunTx);
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = GenericDAOImpl.startTx();
          it = getIdentityNodes();
          it.skip(offset);
        }
      }
      LOG.info(String.format("Done to migration %s normal user activities on %s(ms)", count, (System.currentTimeMillis() - t)));
    } catch (Exception e) {
      LOG.error("Failed to migration for user Activity.", e);
    } finally {
      GenericDAOImpl.endTx(begunTx);
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
    boolean begunTx = GenericDAOImpl.startTx();
    Node node = null;
    long offset = 0;
    boolean isSkip = (lastUserProcess != null);
    Identity owner = null; 
    try {
      while (it.hasNext()) {
        if (forkStop) {
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
          LOG.info(String.format("Commit database into mysql and reCreate JCR-Session at offset: " + offset));
          GenericDAOImpl.endTx(begunTx);
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = GenericDAOImpl.startTx();
          it = getSpaceIdentityNodes();
          it.skip(offset);
        }
      }
      LOG.info(String.format("Done to migration %s space activities from JCR to MYSQL on %s(ms)", offset, (System.currentTimeMillis() - t)));
    } catch (Exception e) {
      LOG.error("Failed to migration for Space Activity.", e);
    } finally {
      GenericDAOImpl.endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of activities from JCR to MYSQL.")
  public void stop() {
    super.stop();
  }
  
  protected void beforeMigration() throws Exception {
    isDone = false;
    LOG.info("Stating to migration activities from JCR to MYSQL........");
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
    boolean begunTx = GenericDAOImpl.startTx();
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
      String type = (OrganizationIdentityProvider.NAME.equals(identityEntity.getProviderId())) ? "user" : "space";
      LOG.info(String.format("Migration activities for %s: %s", type, identityEntity.getRemoteId()));
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
        //
        Identity owner = new Identity(activity.getPosterId());
        owner.setProviderId(OrganizationIdentityProvider.NAME);
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
          //
          String oldCommentId = comment.getId();
          comment.setId(null);
          activityStorage.saveComment(activity, comment);
          //
          doBroadcastListener(comment, oldCommentId);
        }

        previousActivityId = activityId;
        ++count;
        //
        if(count % 10 == 0) {
          GenericDAOImpl.endTx(begunTx);
          entityManagerService.endRequest(PortalContainer.getInstance());
          entityManagerService.startRequest(PortalContainer.getInstance());
          begunTx = GenericDAOImpl.startTx();
        }
      }
      LOG.info(String.format("Done migration %s activities for %s %s on %s(ms) ",
                             type, count, identityEntity.getRemoteId(), System.currentTimeMillis() - t));
    } finally {
      GenericDAOImpl.endTx(begunTx);
    }
  }

  private void doBroadcastListener(ExoSocialActivity activity, String oldId) {
    String newId = activity.getId();
    activity.setId(oldId);
    broadcastListener(activity, newId);
    activity.setId(newId);
  }

  protected void afterMigration() throws Exception {
    if (forkStop) {
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

    isDone = true;
    LOG.info("Done to migration activities from JCR to MYSQL");
  }

  public void doRemove() throws Exception {
    LOG.info("Start remove activities from JCR to MYSQL");
    removeActivityRef();
    removeActivity();
    LOG.info("Done to removed activities from JCR");
  }

  private void removeActivityRef() {
    long t = System.currentTimeMillis();
    NodeIterator it = getIdentityNodes();
    long size = it.getSize();
    Node node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        cleanupRef(node);
        LOG.info(String.format("Cleanup Activity Ref for %s user for %s(ms) ", node.getName(), System.currentTimeMillis() - t));
        offset++;
        //
        if (offset % LIMIT_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          _skip(it, offset);
        }
      }
      
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("Done cleanup Activity Ref for %s user  %s(ms) ", size, System.currentTimeMillis() - t));
    }
    
    //cleanup activity
    t = System.currentTimeMillis();
    it = getSpaceIdentityNodes();
    //don't have any space.
    if (it == null) {
      return;
    }
    
    size = it.getSize();
    node = null;
    offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        cleanupRef(node);
        offset++;
        LOG.info(String.format("Cleanup Activity Ref for %s space consumed %s(ms) ", node.getName(), System.currentTimeMillis() - t));
        //
        if (offset % LIMIT_IDENTITY_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          it.skip(offset);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for user Activities.", e);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("Done cleanup Activity Ref for %s space consumed %s(ms) ", size, System.currentTimeMillis() - t));
    }
    
  }
  
  private void removeActivity() {
    long t = System.currentTimeMillis();
    NodeIterator it = getIdentityNodes();
    long size = it.getSize();
    Node node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        cleanupActivity(node);
        LOG.info(String.format("Cleanup Activity for %s user for %s(ms) ", node.getName(), System.currentTimeMillis() - t));
        offset++;
        //
        if (offset % LIMIT_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          _skip(it, offset);
        }
      }
      
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("Done cleanup Activity for %s user consumed %s(ms) ", size, System.currentTimeMillis() - t));
    }
    
    //cleanup activity
    t = System.currentTimeMillis();
    it = getSpaceIdentityNodes();
    //don't have any space.
    if (it == null) {
      return;
    }
    
    size = it.getSize();
    node = null;
    offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        cleanupActivity(node);
        offset++;
        LOG.info(String.format("Cleanup Activity for %s space consumed %s(ms) ", node.getName(), System.currentTimeMillis() - t));
        //
        if (offset % LIMIT_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          it.skip(offset);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for user Activities.", e);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("Done cleanup Activity for %s space consumed %s(ms) ", size, System.currentTimeMillis() - t));
    }
    
  }
  
  /**
   * Cleanup ActivityRef for Identity
   * @param identityNode
   */
  private void cleanupRef(Node identityNode) {
    
    String nodeStreamsPath;
    String identityName = "";
    
    StringBuffer sb = new StringBuffer().append("SELECT * FROM soc:activityref WHERE ");
    try {
      nodeStreamsPath = identityNode.getNode("soc:streams").getPath();
      sb.append(JCRProperties.path.getName()).append(" LIKE '").append(nodeStreamsPath + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      return;
    }

    long t = System.currentTimeMillis();
    NodeIterator it = nodes(sb.toString());
    Node node = null;
    long offset = 0;
    
    try {
      identityName = identityNode.getName();
      while (it.hasNext()) {
        node = (Node) it.next();
        ActivityRef ref = _findById(ActivityRef.class, node.getUUID());
        getSession().remove(ref);
        offset++;
        if (offset % LIMIT_ACTIVITY_SAVE_THRESHOLD == 0) {
          getSession().save();
          LOG.info(String.format("Persist Activity Ref: %s activity ref consumed time %s(ms) ", LIMIT_ACTIVITY_SAVE_THRESHOLD, System.currentTimeMillis() - t));
          t = System.currentTimeMillis();
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      LOG.info(String.format("Done cleanup Activity Ref: %s for %s user consumed time %s(ms) ", offset, identityName, System.currentTimeMillis() - t));
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
      nodeStreamsPath = identityNode.getNode("soc:activities").getPath();
      sb.append(JCRProperties.path.getName()).append(" LIKE '").append(nodeStreamsPath + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      return;
    }

    long t = System.currentTimeMillis();
    NodeIterator it = nodes(sb.toString());
    Node node = null;
    long offset = 0;
    
    try {
      identityName = identityNode.getName();
      while (it.hasNext()) {
        node = (Node) it.next();
        ActivityRef ref = _findById(ActivityRef.class, node.getUUID());
        getSession().remove(ref);
        offset++;
        if (offset % LIMIT_ACTIVITY_SAVE_THRESHOLD == 0) {
          getSession().save();
          LOG.info(String.format("Persist Activity Ref: %s Activity consumed time %s(ms) ", LIMIT_ACTIVITY_SAVE_THRESHOLD, System.currentTimeMillis() - t));
          t = System.currentTimeMillis();
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      LOG.info(String.format("Done cleanup Activity: %s for %s user consumed time %s(ms) ", offset, identityName, System.currentTimeMillis() - t));
    }
  }

  private List<String> getAdminAndActiveUsers() {
    StreamConfig streamConfig = CommonsUtils.getService(StreamConfig.class);
    ActiveIdentityFilter filer = new ActiveIdentityFilter(streamConfig.getActiveUserGroups());
    Set<String> adminAndActiveUsers = identityStorage.getActiveUsers(filer);
    //
    filer = new ActiveIdentityFilter(streamConfig.getLastLoginAroundDays());
    Set<String> actives = identityStorage.getActiveUsers(filer);
    if (actives != null && !actives.isEmpty()) {
      adminAndActiveUsers.addAll(actives);
    }
    //
    List<String> users = new ArrayList<String>(adminAndActiveUsers);
    Collections.sort(users);
    //
    if (lastUserProcess != null) {
      int index = users.indexOf(lastUserProcess);
      if (index >= 0) {
        lastUserProcess = null;
        return users.subList(index, users.size());
      } else {
        //Finished administrators and active users
        return Lists.newArrayList();
      }
    }
    return users;
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
      LOG.debug("Failed to fill comment from entity : entity null or missing property", e);
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
