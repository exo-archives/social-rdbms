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
import org.exoplatform.container.xml.InitParams;

import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.updater.activity.AbstractStrategy;
import org.exoplatform.social.addons.updater.activity.StrategyFactory;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityParameters;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.utils.ActivityIterator;
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.streams.StreamConfig;

import com.google.caja.util.Lists;

@Managed
@ManagedDescription("Social migration activities from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-activities") })
public class ActivityMigrationService extends AbstractMigrationService<ExoSocialActivity> {
  private static final int LIMIT_IDENTITY_THRESHOLD = 50;
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
    boolean begunTx = GenericDAOImpl.startTx();
    //
    if(lastUserProcess == null && activityDAO.count() > 0) {
      return;
    }
    // doing with group administrators and active users
    List<String> activeUsers = getAdminAndActiveUsers();
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
    // doing with normal users
    boolean isSkip = (lastUserProcess != null);
    long t = System.currentTimeMillis();
    NodeIterator it = getIdentityNodes();
    Identity owner = null; 
    Node node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        if(forkStop) {
          return;
        }
        
        node = (Node) it.next();
        owner = identityStorage.findIdentityById(node.getUUID());
        if (isSkip) {
          if (lastUserProcess.equals(owner.getRemoteId())) {
            isSkip = false;
          }
          continue;
        }
        if(activeUsers.contains(owner.getRemoteId())) {
          continue;
        }
        
        IdentityEntity identityEntity = _findById(IdentityEntity.class, owner.getId());
        migrationByIdentity(null, identityEntity);
        offset++;
        
        //
        if (offset % LIMIT_IDENTITY_THRESHOLD == 0) {
          GenericDAOImpl.endTx(begunTx);
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = GenericDAOImpl.startTx();
          it = getIdentityNodes();
          it.skip(offset);
        }
      }
      
      long newSize = count + size;
      LOG.info(String.format("Done to migration %s user activities from JCR to MYSQL on %s(ms)", newSize, (System.currentTimeMillis() - t)));
    } catch (Exception e) {
      LOG.error("Failed to migration for user Activity.", e);
    } finally {
      GenericDAOImpl.endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
    }
    
  }
  
  private void migrateSpaceActivities() throws Exception {
    long t = System.currentTimeMillis();
    NodeIterator it = getSpaceIdentityNodes();
    if (it == null) return;
    
    boolean begunTx = GenericDAOImpl.startTx();
    Node node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        if (forkStop) {
          return;
        }
        node = (Node) it.next();

        IdentityEntity spaceEntity = _findById(IdentityEntity.class, node.getUUID());
        migrationByIdentity(null, spaceEntity);
        offset++;

        //
        if (offset % LIMIT_IDENTITY_THRESHOLD == 0) {
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
      LOG.info("Migration activities for user: " + identityEntity.getRemoteId());
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
        if(count % LIMIT_THRESHOLD == 0) {
          GenericDAOImpl.endTx(begunTx);
          entityManagerService.endRequest(PortalContainer.getInstance());
          entityManagerService.startRequest(PortalContainer.getInstance());
          begunTx = GenericDAOImpl.startTx();
        }
      }
      LOG.info(String.format("Done migration %s activities for user %s on %s(ms) ",
                             count, identityEntity.getRemoteId(), System.currentTimeMillis() - t));
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
    removeActivities();
    LOG.info("Done to removed activities from JCR");
  }

  private void removeActivities() {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    AbstractStrategy<IdentityEntity> refCleanup = StrategyFactory.getActivityCleanupStrategy(removeTypeOfActivity);
    AbstractStrategy<IdentityEntity> activityCleanup = StrategyFactory.getActivityRefCleanupStrategy(removeTypeOfActivityRef);
    long t = System.currentTimeMillis();
    NodeIterator it = getIdentityNodes();
    Node node = null;
    long offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        IdentityEntity identityEntity = _findById(IdentityEntity.class, node.getUUID());
        refCleanup.process(identityEntity);
        LOG.info(String.format("Session save:: ref for %s(ms) ", System.currentTimeMillis() - t));
        offset++;
        //
        if (offset % LIMIT_IDENTITY_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          _skip(it, offset);
        }
      }
      
    } catch (Exception e) {
      LOG.error("Failed to cleanup for Activity Reference.", e);
    } finally {
      LOG.info(String.format("Done cleanup Activity Ref for %s(ms) ", System.currentTimeMillis() - t));
    }
    
    //cleanup activity
    t = System.currentTimeMillis();
    it = getIdentityNodes();
    node = null;
    offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        IdentityEntity identityEntity = _findById(IdentityEntity.class, node.getUUID());
        activityCleanup.process(identityEntity);
        offset++;
        getSession().save();
        LOG.info(String.format("Session save:: activity for %s(ms) ", System.currentTimeMillis() - t));
        //
        if (offset % LIMIT_IDENTITY_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          _skip(it, offset);
        }
      }
      
    } catch (Exception e) {
      LOG.error("Failed to cleanup for user Activities.", e);
    } finally {
      getSession().save();
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("Done cleanup User Activities for %s(ms) ", System.currentTimeMillis() - t));
    }
    
    //space
    t = System.currentTimeMillis();
    it = getSpaceIdentityNodes();
    //
    if (it == null) return;
    
    node = null;
    offset = 0;
    try {
      while (it.hasNext()) {
        node = (Node) it.next();
        IdentityEntity spaceIdentityEntity = _findById(IdentityEntity.class, node.getUUID());
        activityCleanup.process(spaceIdentityEntity);
        offset++;
        getSession().save();
        LOG.info(String.format("Session save:: space activity for %s(ms) ", System.currentTimeMillis() - t));
        //
        if (offset % LIMIT_IDENTITY_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          it = getIdentityNodes();
          _skip(it, offset);
        }
      }
      
    } catch (Exception e) {
      LOG.error("Failed to cleanup for user Space Activity.", e);
    } finally {
      getSession().save();
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("Done cleanup Space Activities for %s(ms) ", System.currentTimeMillis() - t));
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
