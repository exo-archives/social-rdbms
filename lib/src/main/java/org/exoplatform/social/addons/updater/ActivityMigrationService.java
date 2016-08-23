/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.social.addons.updater;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;


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
import org.exoplatform.social.addons.storage.RDBMSIdentityStorageImpl;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.updater.utils.MigrationCounter;
import org.exoplatform.social.addons.updater.utils.StringUtil;
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
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
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

  protected final RDBMSIdentityStorageImpl identityJPAStorage;

  private final ActivityDAO activityDAO;

  private String previousActivityId = null;
  private ActivityEntity lastActivity = null;
  private String lastUserProcess = null;
  private boolean forceStop = false;

  private Set<String> identitiesMigrateFailed = new HashSet<>();
  private Set<String> identitiesCleanupFailed = new HashSet<>();
  
  public ActivityMigrationService(InitParams initParams,
                                  ActivityDAO activityDAO,
                                  ActivityStorage activityStorage,
                                  ActivityStorageImpl activityJCRStorage,
                                  IdentityStorageImpl identityStorage,
                                  RDBMSIdentityStorageImpl rdbmsIdentityStorage,
                                  EventManager<ExoSocialActivity, String> eventManager,
                                  EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.identityJPAStorage = rdbmsIdentityStorage;
    this.activityDAO = activityDAO;
    this.activityStorage = activityStorage;
    this.activityJCRStorage = activityJCRStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
  }

  @Managed
  @ManagedDescription("Manual to start run miguration data of activities from JCR to RDBMS.")
  public void doMigration() throws Exception {
    RequestLifeCycle.end();

    numberFailed += migrateUserActivities();
    // migrate activities from space
    numberFailed += migrateSpaceActivities();

    RequestLifeCycle.begin(PortalContainer.getInstance());
  }

  private long migrateUserActivities() throws Exception {
    LOG.info("| \\ Start:: migrate activity of users -------------");

    long totalUsers = getNumberUserIdentities();

    MigrationCounter counter = MigrationCounter.builder().threshold(LIMIT_THRESHOLD).build();
    counter.newTotalAndWatch();

    long numberUserFailed = 0;

    boolean cont = true;
    while(cont && !forkStop) {

      try {
        RequestLifeCycle.begin(PortalContainer.getInstance());

        NodeIterator it = getIdentityNodes(counter.getTotal(), LIMIT_THRESHOLD);
        if (it == null || it.getSize() <= 0) {
          cont = false;

        } else {

          Identity owner = null;
          Node node = null;

          while(it != null && it.hasNext()) {
            if (forkStop) {
              break;
            }

            node = (Node)it.next();
            owner = identityStorage.findIdentityById(node.getUUID());

            counter.newBatchAndWatch();
            counter.getAndIncrementTotal();

            LOG.info(String.format("|  \\ START::user number: %s/%s (%s user)", counter.getTotal(), totalUsers, owner.getRemoteId()));
            IdentityEntity identityEntity = _findById(IdentityEntity.class, owner.getId());
            try {
              migrationByIdentity(null, identityEntity);
            } catch (Exception ex) {
              numberUserFailed++;
              identitiesMigrateFailed.add(node.getName());
              LOG.error(String.format("Failed migrate user %s", owner.getRemoteId()), ex);
            }
            LOG.info(String.format("|  / END:: migrate activity of user %s consumed %s(ms) -------------", owner.getRemoteId(), counter.endBatchWatch()));

            if (counter.isPersistPoint()) {
              RequestLifeCycle.end();
              RequestLifeCycle.begin(PortalContainer.getInstance());
              it = getIdentityNodes(counter.getTotal(), LIMIT_THRESHOLD);
            }
          }
        }

      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      } finally {
        RequestLifeCycle.end();
      }
    }

    if (numberUserFailed > 0) {
      LOG.error(String.format("|   Failed in migrate activities of %s user(s)", numberUserFailed));
    }
    LOG.info(String.format("| / END:: %s user(s) consumed %s(ms) -------------", counter.getTotal(), counter.endTotalWatch()));

    return numberUserFailed;
  }

  private long migrateSpaceActivities() throws Exception {
    LOG.info("Start to migration space activities from JCR to RDBMS");
    long t = System.currentTimeMillis();
    long totalSpaces = getNumberSpaceIdentities();

    boolean isSkip = (lastUserProcess != null);

    boolean cont = true;
    long offset = 0;
    long numberSpaceFailed = 0;

    while(cont && !forkStop) {

      try {
        RequestLifeCycle.begin(PortalContainer.getInstance());

        NodeIterator it = getSpaceIdentityNodes(offset, LIMIT_THRESHOLD);
        if (it == null || it.getSize() <= 0) {
          cont = false;

        } else {
          Identity owner = null;
          Node node = null;

          while(it.hasNext()) {
            if (forceStop) {
              break;
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

            long t1 = System.currentTimeMillis();
            //
            IdentityEntity spaceEntity = _findById(IdentityEntity.class, node.getUUID());
            LOG.info(String.format("|  \\ START::space number: %s/%s (%s space)", offset, totalSpaces, owner.getRemoteId()));
            try {
              migrationByIdentity(null, spaceEntity);
            } catch (Exception ex) {
              numberSpaceFailed++;
              identitiesMigrateFailed.add(node.getName());
              LOG.error(String.format("Failed migrate space %s", owner.getRemoteId()), ex);
            }
            LOG.info(String.format("|  / END:: migrate activity of space %s consumed %s(ms) -------------", owner.getRemoteId(), (System.currentTimeMillis() - t1)));

          }
        }

      } catch (Exception ex ) {
        LOG.error(ex.getMessage(), ex);
      } finally {
        RequestLifeCycle.end();
      }
    }

    if (numberSpaceFailed > 0) {
      LOG.info(" Failed migration for " + numberSpaceFailed + " space(s)");
    }

    LOG.info(String.format("Done to migration %s space activities from JCR to RDBMS on %s(ms)", offset, (System.currentTimeMillis() - t)));
    return numberSpaceFailed;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of activities from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }
  
  protected void beforeMigration() throws Exception {
    MigrationContext.setActivityDone(false);
    identitiesMigrateFailed = new HashSet<>();

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
    boolean begunTx = false;
    long numberActivitiesFailed = 0;
    long t = System.currentTimeMillis();
    int count = 0;
    String providerId = "";
    String remoteId = "";

    if (identityEntity == null) {
      Identity poster = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, userName);
      try {
        identityEntity = _findById(IdentityEntity.class, poster.getId());
      } catch (Exception e) {
        LOG.warn("The user " + userName + " has not identity. Do not migration for this user.");
        return;
      }
    }

    providerId = identityEntity.getProviderId();
    remoteId = identityEntity.getRemoteId();
    Identity jpaIdentity = identityJPAStorage.findIdentity(identityEntity.getProviderId(), identityEntity.getRemoteId());

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

    while (activityIterator.hasNext()) {
      String activityId = activityIterator.next().getId();
      //
      try {
        begunTx = startTx();

        ExoSocialActivity activity = activityJCRStorage.getActivity(activityId);
        Map<String, String> params = activity.getTemplateParams();

        if (params != null && !params.isEmpty()) {

          for(Map.Entry<String, String> entry: params.entrySet()) {

            String value = entry.getValue();

            if (value.length() >= 1024) {
              LOG.info("===================== activity id " + activity.getId() + " new value length = " +  value.length());
              params.put(entry.getKey(), "");
            }

            //Remove long UTF-8 char
            params.put(entry.getKey(), StringUtil.removeLongUTF(entry.getValue()));
          }

          activity.setTemplateParams(params);
        }
        //
        //Identity owner = new Identity(identityEntity.getId());
        //owner.setProviderId(providerId);
        //
        activity.setId(null);
        activity.setTitle(StringUtil.removeLongUTF(activity.getTitle()));
        activity.setBody(StringUtil.removeLongUTF(activity.getBody()));

        //
        activity.setLikeIdentityIds(convertToNewIds(activity.getLikeIdentityIds()));
        activity.setCommentedIds(convertToNewIds(activity.getCommentedIds()));
        activity.setMentionedIds(convertToNewIds(activity.getMentionedIds()));
        activity.setUserId(getNewIdentityId(activity.getUserId()));
        activity.setPosterId(getNewIdentityId(activity.getPosterId()));

        activity = activityStorage.saveActivity(jpaIdentity, activity);
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

        endTx(begunTx);
        begunTx = startTx();

        List<ActivityEntity> commentEntities = activityEntity.getComments();
        if (commentEntities != null) {
          for (ActivityEntity commentEntity : commentEntities) {
            ExoSocialActivity comment = fillCommentFromEntity(commentEntity);
            if (comment != null) {
              String oldCommentId = comment.getId();
              comment.setId(null);
              Map<String, String> commentParams = comment.getTemplateParams();
              if (commentParams != null && !commentParams.isEmpty()) {

                for (Map.Entry<String, String> entry : commentParams.entrySet()) {

                  String value = entry.getValue();

                  if (value.length() >= 1024) {
                    LOG.info("===================== comment id " + oldCommentId + " new value length = " + value.length());
                    commentParams.put(entry.getKey(), "");
                  }

                  //remove long UTF-8 char
                  commentParams.put(entry.getKey(), StringUtil.removeLongUTF(entry.getValue()));

                }

                comment.setTemplateParams(commentParams);
              }
              activity.setTemplateParams(params);

              comment.setLikeIdentityIds(convertToNewIds(comment.getLikeIdentityIds()));
              comment.setCommentedIds(convertToNewIds(comment.getCommentedIds()));
              comment.setMentionedIds(convertToNewIds(comment.getMentionedIds()));
              comment.setUserId(getNewIdentityId(comment.getUserId()));
              comment.setPosterId(getNewIdentityId(comment.getPosterId()));

              if (comment.getTitle() == null) {
                comment.setTitle("");
              }
              activityStorage.saveComment(activity, comment);
              //
              doBroadcastListener(comment, oldCommentId);
              commentParams = null;
              params = null;
            }
          }
        }

        previousActivityId = activityId;
        ++count;

      } catch (Exception e) {
        LOG.error("Failed to migrate activity id : " + activityId, e);
        numberActivitiesFailed++;
      } finally {
        try {
          endTx(begunTx);
        } catch (Exception ex) {
          LOG.error("Failed to migrate activity id : " + activityId);
          numberActivitiesFailed++;
        }
      }
    }

    LOG.info(String.format("    Done migration %s activitie(s) for %s consumed %s(ms) ", count, providerId + "/" +remoteId, System.currentTimeMillis() - t));
    if (numberActivitiesFailed > 0) {
      LOG.error(String.format("    Failed migration for %s activitie(s)", numberActivitiesFailed));
      throw new Exception("Migration is failed for " + numberActivitiesFailed + " activities of identity " + providerId + "/" + remoteId);
    }
  }

  private void doBroadcastListener(ExoSocialActivity activity, String oldId) {
    String newId = activity.getId();
    activity.setId(oldId);
    broadcastListener(activity, newId.replace("comment", ""));
    activity.setId(newId);
  }

  protected void afterMigration() throws Exception {
    MigrationContext.setIdentitiesMigrateActivityFailed(identitiesMigrateFailed);

    if (!forceStop && identitiesMigrateFailed.isEmpty()) {
      MigrationContext.setActivityDone(true);
    }

    LOG.info("Done to migration activities from JCR to RDBMS");

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
    try {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      removeActivity();
    } finally {
      RequestLifeCycle.end();
    }
    LOG.info("Done to removed activities from JCR");
  }

  private void removeActivity() throws Exception {
    identitiesCleanupFailed = new HashSet<>();

    long t = System.currentTimeMillis();
    long offset = 0;

    long totalUsers = getNumberUserIdentities();
    long totalSpaces = getNumberSpaceIdentities();

    Node node = null;
    boolean isDone = false;
    try {
      LOG.info("| \\ START::cleanup User Activity ---------------------------------");
      NodeIterator it = getIdentityNodes(offset, LIMIT_REMOVED_THRESHOLD);
      while (it != null && it.hasNext()) {
        node = (Node) it.next();
        String name = node.getName();

        if (!MigrationContext.isForceCleanup() && MigrationContext.getIdentitiesMigrateActivityFailed().contains(name)) {
          identitiesCleanupFailed.add(name);
          continue;
        }
        offset++;
        LOG.info(String.format("|  \\ START::cleanup user number: %s/%s (%s user)", offset, totalUsers, node.getName()));
        isDone = cleanupActivity(node);
        if (!isDone) {
          identitiesCleanupFailed.add(name);
        }
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
    node = null;
    offset = 0;
    try {
      LOG.info("| \\ START::cleanup Space Activity ---------------------------------");
      NodeIterator it = getSpaceIdentityNodes(offset, LIMIT_REMOVED_THRESHOLD);
      while (it != null && it.hasNext()) {
        node = (Node) it.next();

        String name = node.getName();

        if (!MigrationContext.isForceCleanup() && MigrationContext.getIdentitiesMigrateActivityFailed().contains(name)) {
          identitiesCleanupFailed.add(name);
          continue;
        }

        LOG.info(String.format("|  \\ START::cleanup space number: %s/%s (%s space)", offset, totalSpaces, node.getName()));
        isDone = cleanupActivity(node);
        if (!isDone) {
          identitiesCleanupFailed.add(name);
        }
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
      MigrationContext.setIdentitiesCleanupActivityFailed(identitiesCleanupFailed);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::cleanup Activity for (%s) space consumed %s(ms) -------------", offset, System.currentTimeMillis() - t));
    }

  }
  
  /**
   * Cleanup ActivityRef for Identity
   * @param activityNode
   * @param userName
   */
  private boolean cleanupSubNode(Node activityNode, String userName) {
    long totalTime = System.currentTimeMillis();
    NodeImpl node = null;
    long offset = 0;
    String nodeId = "";
    String nodePath = "";
    try {
      nodeId = activityNode.getUUID();
      nodePath = activityNode.getPath();
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
      LOG.error("Failed to cleanup sub node for Activity Reference, activity id =" + nodeId + " path=" + nodePath, e);
      try {
        getSession().getJCRSession().refresh(false);
      } catch (RepositoryException ex) {
        LOG.error("RepositoryException", ex);
      }
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
    String identityName = "";
    long totalTime = System.currentTimeMillis();
    long size;

    long failed = 0;

    try {
      identityName = identityNode.getName();
      Node activitiesNode = identityNode.getNode("soc:activities");
      size = activitiesNode.getProperty("soc:number").getLong();

      StringBuffer sb = new StringBuffer().append("SELECT * FROM soc:activity WHERE ");
      try {
        String nodeStreamsPath = XPathUtils.escapeIllegalSQLName(identityNode.getNode("soc:activities").getPath());
        sb.append(JCRProperties.path.getName()).append(" LIKE '").append(nodeStreamsPath + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
        sb.append(" AND soc:isComment = 'false'");
      } catch (RepositoryException e) {
        LOG.error(e.getMessage(), e);
      }

      if (size > 0) {
        long t = System.currentTimeMillis();
        long offset = 0;

        LOG.info(String.format("|   \\ START::cleanup: %d (Activity) for %s identity", size, identityName));
        try {
          NodeIterator it = nodes(sb.toString(), failed, LIMIT_ACTIVITY_SAVE_THRESHOLD);
          while(it != null && it.hasNext()) {
            offset++;
            Node n = it.nextNode();
            try {
              if (cleanupSubNode(n, identityName)) {
                n.remove();
              } else {
                failed++;
              }
              //activityJCRStorage.deleteActivity(n.getUUID());
            } catch (Exception ex) {
              LOG.error("Failed to cleanup activity ID: " + n.getUUID() + " path: " + n.getPath(), ex);
              failed++;
            }

            if (offset % LIMIT_ACTIVITY_SAVE_THRESHOLD == 0) {
              try {
                getSession().save();
                LOG.info(String.format("|     - Persist deleted: %s activity consumed time %s(ms) ", LIMIT_ACTIVITY_SAVE_THRESHOLD, System.currentTimeMillis() - t));
              } catch (Exception ex) {
                LOG.error("Failed, can not persist deleted activities", ex);
                getSession().getJCRSession().refresh(false);
                failed += LIMIT_ACTIVITY_SAVE_THRESHOLD;
              }

              t = System.currentTimeMillis();
              it = nodes(sb.toString(), failed, LIMIT_ACTIVITY_SAVE_THRESHOLD);
            }
          }

          getSession().save();
          if (offset % LIMIT_ACTIVITY_SAVE_THRESHOLD != 0) {
            LOG.info(String.format("|     - Persist deleted: %s activity consumed time %s(ms) ", offset % LIMIT_ACTIVITY_SAVE_THRESHOLD, System.currentTimeMillis() - t));
          }
        } finally {
          LOG.info(String.format("|   / END::cleanup: %d (Activity) for %s identity consumed time %s(ms) ", size, identityName, System.currentTimeMillis() - totalTime));
        }
      } else {
        LOG.info("There is not any activity for identity: " + identityName);
      }

    } catch (Exception e) {
      LOG.error("Failed to cleanup activities for identity: " + identityName, e);
      try {
        getSession().getJCRSession().refresh(false);
      } catch (RepositoryException ex) {
        LOG.error("Repository exception", e);
      }
      return false;
    }
    return failed == 0;
  }

  private ExoSocialActivity fillCommentFromEntity(ActivityEntity activityEntity) {
    ExoSocialActivity comment = new ExoSocialActivityImpl();
    try {
      //
      comment.setId(activityEntity.getId());
      comment.setTitle(StringUtil.removeLongUTF(activityEntity.getTitle()));
      comment.setTitleId(activityEntity.getTitleId());
      comment.setBody(StringUtil.removeLongUTF(activityEntity.getBody()));
      comment.setBodyId(activityEntity.getBodyId());
      comment.setPostedTime(activityEntity.getPostedTime());
      comment.setUpdated(getLastUpdatedTime(activityEntity, comment.getPostedTime()));
      comment.isComment(activityEntity.isComment());
      comment.setType(activityEntity.getType());
      //
      IdentityEntity poster = activityEntity.getPosterIdentity();
      if (poster == null) {
        LOG.warn("Failed to fill comment from entity: poster is null");
        return null;
      }
      String posterId = poster.getId();
      comment.setUserId(posterId);
      comment.setPosterId(posterId);

      //
      ActivityEntity parent = activityEntity.getParentActivity();
      if (parent == null) {
        LOG.warn("Failed to fill comment from entity: parent activity is null");
      }
      comment.setParentId(parent.getId());

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
      return postTime != null ? postTime.longValue() : System.currentTimeMillis();
    }
  }

  @Override
  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }

  private String getNewIdentityId(String oldId) {
    if (oldId == null || oldId.isEmpty()) {
      return null;
    }
    try {
      IdentityEntity entity = _findById(IdentityEntity.class, oldId);
      if (entity != null) {
        Identity id = identityJPAStorage.findIdentity(entity.getProviderId(), entity.getRemoteId());
        if (id != null) {
          return id.getId();
        }
      }
      return null;
    } catch (NodeNotFoundException ex) {
      return null;
    }
  }

  private String[] convertToNewIds(String[] oldIds) {
    if (oldIds == null || oldIds.length == 0) {
      return new String[0];
    }
    List<String> list = new ArrayList<>(oldIds.length);
    for(String old : oldIds) {
      int index = old.indexOf('@');
      if (index != -1) {
        old = old.substring(0, index);
      }
      String id = getNewIdentityId(old);
      if (id != null) {
        list.add(id);
      }
    }

    return list.toArray(new String[list.size()]);
  }
}
