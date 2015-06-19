package org.exoplatform.social.addons.updater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import org.exoplatform.container.component.RequestLifeCycle;
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
import org.exoplatform.social.core.chromattic.entity.ActivityRefListEntity;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.utils.ActivityIterator;
import org.exoplatform.social.core.chromattic.utils.ActivityRefIterator;
import org.exoplatform.social.core.chromattic.utils.ActivityRefList;
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.ActivityStreamStorageImpl.ActivityRefType;
import org.exoplatform.social.core.storage.streams.StreamConfig;

import com.google.caja.util.Lists;

@Managed
@ManagedDescription("Social migration activities from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-activities") })
public class ActivityMigrationService extends AbstractMigrationService<ExoSocialActivity> {
  private static final int LIMIT_THRESHOLD = 100;
  private static final String EVENT_LISTENER_KEY = "SOC_ACTIVITY_MIGRATION";
  private final ActivityDAO activityDAO;
  private final ActivityStorage activityStorage;
  private final ActivityStorageImpl activityJCRStorage;

  private ActivityEntity currenActivity = null;
  private ActivityEntity lastActivity = null;
  private String lastUserProcess = null;
  private boolean forkStop = false;
  
  public ActivityMigrationService(ActivityDAO activityDAO,
                                  ActivityStorage activityStorage,
                                  ActivityStorageImpl activityJCRStorage,
                                  IdentityStorage identityStorage,
                                  EventManager<ExoSocialActivity, String> eventManager,
                                  RelationshipMigrationService relationshipMigration) {
    super(identityStorage, eventManager);
    this.activityDAO = activityDAO;
    this.activityStorage = activityStorage;
    this.activityJCRStorage = activityJCRStorage;
  }

  @Managed
  @ManagedDescription("Manual to start run miguration data of activities from JCR to MYSQL.")
  public void doMigration() throws Exception {
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
      migrationByUser(userName, null);
      ++count;
      //
      processLog("Activities migration admin+active users", size, count);
    }
    // doing with normal users
    boolean isSkip = (lastUserProcess != null);
    Collection<IdentityEntity> allIdentityEntity  = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values();
    size = allIdentityEntity.size() - size;
    count = 0;
    Iterator<IdentityEntity> iter =  allIdentityEntity.iterator();
    while (iter.hasNext()) {
      if(forkStop) {
        return;
      }
      IdentityEntity identityEntity = (IdentityEntity) iter.next();
      if (isSkip) {
        if (lastUserProcess.equals(identityEntity.getRemoteId())) {
          isSkip = false;
        }
        continue;
      }
      if(activeUsers.contains(identityEntity.getRemoteId())) {
        continue;
      }
      //
      migrationByUser(null, identityEntity);
      //
      ++count;
      processLog("Activities migration normal users", size, count);
    }
    //migrate activities from space
    migrateSpaceActivities();
  }
  
  private void migrateSpaceActivities() throws Exception {
    Collection<IdentityEntity> allIdentityEntity  = getAllIdentityEntity(SpaceIdentityProvider.NAME).values(); 
    Iterator<IdentityEntity> iter = allIdentityEntity.iterator();
    int size = allIdentityEntity.size(), count = 0;
    while (iter.hasNext()) {
      IdentityEntity spaceEntity = (IdentityEntity) iter.next();
      migrationByUser(null, spaceEntity);
      //
      ++count;
      processLog("Activities migration spaces", size, count);
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of activities from JCR to MYSQL.")
  public void stop() {
    super.stop();
  }

  private void migrationByUser(String userName, IdentityEntity identityEntity) throws Exception {
    boolean begunTx = GenericDAOImpl.startTx();
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
    EntityManagerService entityManagerService = CommonsUtils.getService(EntityManagerService.class);
    while (activityIterator.hasNext()) {
      ActivityEntity activityEntity = activityIterator.next();
      LOG.info("Mirgration activity: " + activityEntity.getName());
      //
      ExoSocialActivity activity = activityJCRStorage.getActivity(activityEntity.getId());
      //
      IdentityEntity activityIdentity = activityEntity.getIdentity();
      Identity owner = new Identity(activityIdentity.getProviderId(), activityIdentity.getRemoteId());
      owner.setId(activityIdentity.getId());
      //
      String oldId = activity.getId();
      activity.setId(null);
      activityStorage.saveActivity(owner, activity);
      entityManagerService.getEntityManager().flush();
      //
      doBroadcastListener(activity, oldId);
      //
      List<ActivityEntity> commentEntities = activityEntity.getComments();
      for (ActivityEntity commentEntity : commentEntities) {
        ExoSocialActivity comment = fillCommentFromEntity(commentEntity);
        //
        oldId = comment.getId();
        comment.setId(null);
        activityStorage.saveComment(activity, comment);
        entityManagerService.getEntityManager().flush();
        //
        doBroadcastListener(comment, oldId);
      }
      //
      if(currenActivity != null) {
        _removeMixin(currenActivity, ActivityUpdaterEntity.class);
      }
      _getMixin(activityEntity, ActivityUpdaterEntity.class, true);
      currenActivity = activityEntity;
      ++count;
      //
      if(count % LIMIT_THRESHOLD == 0) {
        GenericDAOImpl.endTx(begunTx);
        RequestLifeCycle.end();
        RequestLifeCycle.begin(entityManagerService);
        begunTx = GenericDAOImpl.startTx();
      }
    }
    LOG.info(String.format("Done migration %s activities for user %s on %s(ms) ",
                           count, identityEntity.getRemoteId(), System.currentTimeMillis() - t));
  }

  private void doBroadcastListener(ExoSocialActivity activity, String oldId) {
    String newId = activity.getId();
    activity.setId(oldId);
    broadcastListener(activity, newId);
    activity.setId(newId);
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

  protected void afterMigration() throws Exception {
    if(forkStop) {
      return;
    }
    if(currenActivity != null) {
      _removeMixin(currenActivity, ActivityUpdaterEntity.class);
    }
    isDone = true;
    LOG.info("Done to migration activities from JCR to MYSQL");
  }

  public void doRemove() throws Exception {
    LOG.info("Start remove activities from JCR to MYSQL");
    //Remove all activities from users
    removeActivities(false);
    //Remove all activities from spaces
    removeActivities(true);
    LOG.info("Done to removed activities from JCR");
  }

  private void removeActivities(boolean isSpaceActivties) {
    Iterator<IdentityEntity> allIdentityEntity = getAllIdentityEntity(isSpaceActivties ? SpaceIdentityProvider.NAME : OrganizationIdentityProvider.NAME).values().iterator();
    while (allIdentityEntity.hasNext()) {
      IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
      if (isSpaceActivties) {
        removeActivityRefs(identityEntity, ActivityRefType.FEED);
        removeActivityRefs(identityEntity, ActivityRefType.SPACE_STREAM);
      } else {
        removeActivityRefs(identityEntity, ActivityRefType.FEED);
        removeActivityRefs(identityEntity, ActivityRefType.MY_ACTIVITIES);
        removeActivityRefs(identityEntity, ActivityRefType.CONNECTION);
        removeActivityRefs(identityEntity, ActivityRefType.MY_SPACES);
      }
    }
    //Remove all activity entity
    allIdentityEntity = getAllIdentityEntity(isSpaceActivties ? SpaceIdentityProvider.NAME : OrganizationIdentityProvider.NAME).values().iterator();
    while (allIdentityEntity.hasNext()) {
      IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
      ActivityListEntity activityListEntity = identityEntity.getActivityList();
      ActivityIterator activityIterator = new ActivityIterator(activityListEntity);
      while (activityIterator.hasNext()) {
        getSession().remove(activityIterator.next());
      }
    }
  }
  
  private void removeActivityRefs(IdentityEntity identityEntity, ActivityRefType type) {
    ActivityRefListEntity listRef = type.refsOf(identityEntity);
    ActivityRefList list = new ActivityRefList(listRef);
    ActivityRefIterator it = list.iterator();
    while (it.hasNext()) {
      getSession().remove(it.next());
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
