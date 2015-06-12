package org.exoplatform.social.addons.updater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.chromattic.core.api.ChromatticSessionImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.SynchronizedRDBMSActivityStorage;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityParameters;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProviderEntity;
import org.exoplatform.social.core.chromattic.utils.ActivityIterator;
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.AbstractStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.streams.StreamConfig;
import org.picocontainer.Startable;

import com.google.caja.util.Lists;

@Managed
@ManagedDescription("Social migration activities from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration") })
public class ActivityMigrationService extends AbstractStorage implements Startable {

  private static final Log LOG = ExoLogger.getLogger(ActivityMigrationService.class);
  private IdentityStorage identityStorage;
  private SynchronizedRDBMSActivityStorage activityStorage;
  private ActivityStorageImpl activityJCRStorage;
  private ActivityDAO activityDAO;
  private ActivityEntity currenActivity = null;
  private ActivityEntity lastActivity = null;
  private String lastUserProcess = null;
  private boolean forkStop = false;
  
  public ActivityMigrationService() {
    super();
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run miguration database of activities from JCR to MYSQL.")
  public void start() {
    forkStop = false;
    //
    this.activityDAO = CommonsUtils.getService(ActivityDAO.class);
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      beforeMigration();
      if(lastUserProcess == null && activityDAO.count() > 0) {
        return;
      }
      this.identityStorage = CommonsUtils.getService(IdentityStorage.class);
      this.activityJCRStorage = CommonsUtils.getService(ActivityStorageImpl.class);
      this.activityStorage = CommonsUtils.getService(SynchronizedRDBMSActivityStorage.class);
      // doing with group administrators and active users
      List<String> activeUsers = getAdminAndActiveUsers();
      for (String userName : activeUsers) {
        if(forkStop) {
          return;
        }
        //
        migrationByUser(userName, null);
      }
      // doing with normal users
      boolean isSkip = (lastUserProcess != null);
      Iterator<IdentityEntity> allIdentityEntity = getAllIdentityEntity().values().iterator();
      while (allIdentityEntity.hasNext()) {
        if(forkStop) {
          return;
        }
        IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
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
      }
      //
      afterMigration();
    } catch (Exception e) {
      LOG.error("Failed to run migration activities from JCR to Mysql.", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration database of activities from JCR to MYSQL.")
  public void stop() {
    forkStop = true;
  }

  private void migrationByUser(String userName, IdentityEntity identityEntity) throws Exception {
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
      ActivityEntity activityEntity = activityIterator.next();
      LOG.info("Mirgration activity: " + activityEntity.getName());
      //
      ExoSocialActivity activity = activityJCRStorage.getActivity(activityEntity.getId());
      //activity.getActivityStream().getId()
      IdentityEntity activityIdentity = activityEntity.getIdentity();
      Identity owner = new Identity(activityIdentity.getProviderId(), activityIdentity.getRemoteId());
      owner.setId(activityIdentity.getId());
      //
      activity.setId(null);
      activityStorage.saveActivity(owner, activity);
      //
      List<ActivityEntity> commentEntities = activityEntity.getComments();
      for (ActivityEntity commentEntity : commentEntities) {
        ExoSocialActivity comment = fillCommentFromEntity(commentEntity);
        //
        comment.setId(null);
        activityStorage.saveComment(activity, comment);
      }
      //
      if(currenActivity != null) {
        _removeMixin(currenActivity, ActivityUpdaterEntity.class);
      }
      _getMixin(activityEntity, ActivityUpdaterEntity.class, true);
      currenActivity = activityEntity;
      ++count;
    }
    LOG.info(String.format("Done migration %s activities for user %s on %s(ms) ",
                           count, identityEntity.getRemoteId(), System.currentTimeMillis() - t));
  }

  private void beforeMigration() throws Exception {
    LOG.info("Stating to migration activities from JCR to MYSQL........");
    try {
      NodeIterator iterator = nodes("SELECT * FROM soc:activityUpdater");
      if (iterator.hasNext()) {
        String currentUUID = iterator.nextNode().getUUID();
        lastActivity = _findById(ActivityEntity.class, currentUUID);
        if (lastActivity != null) {
          lastUserProcess = lastActivity.getPosterIdentity().getRemoteId();
        }
      }
    } finally {
     
    }
  }

  private void afterMigration() throws Exception {
    if(currenActivity != null) {
      _removeMixin(currenActivity, ActivityUpdaterEntity.class);
    }
    LOG.info("Done to migration activities from JCR to MYSQL");
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

  @SuppressWarnings("unchecked")
  private Map<String, IdentityEntity> getAllIdentityEntity() {
    ProviderEntity providerEntity;
    try {
      providerEntity = getProviderRoot().getProviders().get(OrganizationIdentityProvider.NAME);
    } catch (Exception ex) {
      lifeCycle.getProviderRoot().set(null);
      providerEntity = getProviderRoot().getProviders().get(OrganizationIdentityProvider.NAME);
    }
    return (providerEntity != null) ? providerEntity.getIdentities() : new HashMap<String, IdentityEntity>();
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
}
