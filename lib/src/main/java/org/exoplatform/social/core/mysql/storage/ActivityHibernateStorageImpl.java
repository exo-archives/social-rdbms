/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.mysql.storage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.dao.ActivityDao;
import org.exoplatform.social.core.entity.Activity;
import org.exoplatform.social.core.entity.Comment;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;

public class ActivityHibernateStorageImpl extends ActivityStorageImpl {

  private final ActivityDao activityDao;
  private final IdentityStorage identityStorage;
  private final SortedSet<ActivityProcessor> activityProcessors;
  public ActivityHibernateStorageImpl(RelationshipStorage relationshipStorage, 
                                      IdentityStorage identityStorage, 
                                      SpaceStorage spaceStorage,
                                      ActivityDao activityDao) {
    super(relationshipStorage, identityStorage, spaceStorage);
    //
    this.activityDao = activityDao;
    this.identityStorage = identityStorage;
    this.activityProcessors = new TreeSet<ActivityProcessor>(processorComparator());
  }
  
  private static Comparator<ActivityProcessor> processorComparator() {
    return new Comparator<ActivityProcessor>() {

      public int compare(ActivityProcessor p1, ActivityProcessor p2) {
        if (p1 == null || p2 == null) {
          throw new IllegalArgumentException("Cannot compare null ActivityProcessor");
        }
        return p1.getPriority() - p2.getPriority();
      }
    };
  }

  @Override
  public void addPlugin(BaseComponentPlugin baseComponent) {
    super.addPlugin(baseComponent);
  }

  @Override
  public void setInjectStreams(boolean mustInject) {
    super.setInjectStreams(mustInject);
  }

  private ExoSocialActivity convertActivityEntityToActivity(Activity activityEntity) {
    if(activityEntity == null) return null;
    //
    ExoSocialActivity activity = new ExoSocialActivityImpl(activityEntity.getPosterId(), activityEntity.getType(),
                                                           activityEntity.getTitle(), activityEntity.getBody(), false);
    
    activity.setId(activityEntity.getId());
    activity.setLikeIdentityIds(activityEntity.getLikerIds().toArray(new String[]{}));
    activity.setTemplateParams(activityEntity.getTemplateParams());
    
    String ownerIdentityId = activityEntity.getOwnerId();
    ActivityStream stream = new ActivityStreamImpl();
    Identity owner = identityStorage.findIdentityById(ownerIdentityId);
    stream.setType(owner.getProviderId());
    stream.setPrettyId(owner.getRemoteId());
    stream.setId(owner.getId());
    //
    activity.setActivityStream(stream);
    activity.setStreamOwner(ownerIdentityId);
    //
    activity.isLocked(activityEntity.getLocked());
    activity.isHidden(activityEntity.getHidden());
    activity.setTitleId(activityEntity.getTitleId());
    //
    List<String> commentIds = new ArrayList<String>();
    List<String> replyToIds = new ArrayList<String>();
    List<Comment> comments =activityEntity.getComments();
    for (Comment comment : comments) {
      commentIds.add(comment.getPosterId());
      replyToIds.add(String.valueOf(comment.getId()));
    }
    activity.setCommentedIds(commentIds.toArray(new String[commentIds.size()]));
    activity.setReplyToId(replyToIds.toArray(new String[replyToIds.size()]));
    //
    return activity;
  }
  
  private Activity convertActivityToActivityEntity(ExoSocialActivity activity, String ownerId) {
    Activity activityEntity  =  new Activity();
    if (activity.getId() != null) {
      activityEntity = activityDao.getActivity(activity.getId());
    }
    activityEntity.setTitle(activity.getTitle());
    activityEntity.setTitleId(activity.getTitleId());
    activityEntity.setType(activity.getType());
    activityEntity.setBody(activity.getBody());
    if (ownerId != null) {
      activityEntity.setPosterId(activity.getUserId() != null ? activity.getUserId() : ownerId);
    }
    if(activity.getLikeIdentityIds() != null) {
      activityEntity.setLikerIds(new HashSet<String>(Arrays.asList(activity.getLikeIdentityIds())));
    }
    activityEntity.setTemplateParams(activity.getTemplateParams());
    //
    activityEntity.setLocked(activity.isLocked());
    activityEntity.setHidden(activity.isHidden());
    //
    return activityEntity;
  }

  private ExoSocialActivity convertCommentEntityToComment(Comment comment) {
    ExoSocialActivity exoSocialActivity = new ExoSocialActivityImpl(comment.getPosterId(), null,
        comment.getTitle(), comment.getBody(), false);
    exoSocialActivity.setTitle(comment.getTitle());
    exoSocialActivity.setTitleId(comment.getTitleId());
    exoSocialActivity.setBody(comment.getBody());
    exoSocialActivity.setTemplateParams(comment.getTemplateParams());
    //
    exoSocialActivity.isLocked(comment.getLocked().booleanValue());
    exoSocialActivity.isHidden(comment.getHidden().booleanValue());
    //
    return exoSocialActivity;
  }

  private List<ExoSocialActivity> convertCommentEntitiesToComments(List<Comment> comments) {
    List<ExoSocialActivity> exoSocialActivities = new ArrayList<ExoSocialActivity>();
    if (comments == null) return exoSocialActivities;
    for (Comment comment : comments) {
      exoSocialActivities.add(convertCommentEntityToComment(comment));
    }
    return exoSocialActivities;
  }
  
  private Comment convertCommentToCommentEntity(ExoSocialActivity comment) {
    Comment commentEntity = new Comment();
    if (comment.getId() != null) {
      commentEntity = activityDao.getComment(Long.valueOf(comment.getId()));
    }
    commentEntity.setTitle(comment.getTitle());
    commentEntity.setTitleId(comment.getTitleId());
    commentEntity.setBody(comment.getBody());
    commentEntity.setPosterId(comment.getUserId());
    commentEntity.setTemplateParams(comment.getTemplateParams());
    //
    commentEntity.setLocked(comment.isLocked());
    commentEntity.setHidden(comment.isHidden());
    //
    return commentEntity;
  }
  
  private List<ExoSocialActivity> convertActivityEntitiesToActivities(List<Activity> activities) {
    List<ExoSocialActivity> exoSocialActivities = new ArrayList<ExoSocialActivity>();
    if (activities == null) return exoSocialActivities;
    for (Activity activity : activities) {
      exoSocialActivities.add(convertActivityEntityToActivity(activity));
    }
    return exoSocialActivities;
  }
  
  @Override
  public ExoSocialActivity getActivity(String activityId) throws ActivityStorageException {
    Activity activity = activityDao.getActivity(activityId);
    //
    return convertActivityEntityToActivity(activity);
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner) throws ActivityStorageException {
    //
    return convertActivityEntitiesToActivities(activityDao.getUserActivities(owner));
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToActivities(activityDao.getUserActivities(owner, offset, limit));
  }

  @Override
  public List<ExoSocialActivity> getUserActivitiesForUpgrade(Identity owner, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToActivities(activityDao.getUserActivitiesForUpgrade(owner, offset, limit));
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToActivities(activityDao.getActivities(owner, viewer, offset, limit));
  }

  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) throws ActivityStorageException {
    Activity activityEntity = activityDao.getActivity(activity.getId());
    Comment commentEntity = convertCommentToCommentEntity(comment);
    activityDao.saveComment(activityEntity, commentEntity);
    comment.setId(String.valueOf(commentEntity.getId()));
    //
    activity = convertActivityEntityToActivity(activityEntity);
  }

  @Override
  public ExoSocialActivity saveActivity(Identity owner, ExoSocialActivity activity) throws ActivityStorageException {
    Activity activityEntity = convertActivityToActivityEntity(activity, owner.getId());
    activityDao.saveActivity(owner, activityEntity);
    activity.setId(activityEntity.getId());
    return activity;
  }

  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) throws ActivityStorageException {
    Comment commentEntity = activityDao.getComment(Long.valueOf(comment.getId()));
    if (commentEntity != null) {
      return convertActivityEntityToActivity(commentEntity.getActivity());
    }
    return null;
  }

  @Override
  public void deleteActivity(String activityId) throws ActivityStorageException {
    activityDao.deleteActivity(activityId);
  }

  @Override
  public void deleteComment(String activityId, String commentId) throws ActivityStorageException {
    Comment comment = activityDao.getComment(Long.valueOf(commentId));
    
    activityDao.getActivity(activityId).getComments().remove(comment);
    activityDao.deleteComment(comment);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(List<Identity> connectionList, long offset, long limit) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(List<Identity> connectionList, TimestampType type, long offset, long limit) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfUserActivities(Identity owner) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUserActivitiesForUpgrade(Identity owner) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    return getActivityFeedForUpgrade(ownerIdentity, offset, limit);
  }

  @Override
  public List<ExoSocialActivity> getActivityFeedForUpgrade(Identity ownerIdentity, int offset, int limit) {
    return convertActivityEntitiesToActivities(activityDao.getActivityFeed(ownerIdentity, offset, limit));
  }

  @Override
  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return activityDao.getNumberOfActivitesOnActivityFeed(ownerIdentity);
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentity(Identity ownerIdentity, long offset, long limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, long limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity, int offset, int limit) {
    return convertCommentEntitiesToComments(activityDao
        .getComments(activityDao.getActivity(existingActivity.getId()), offset, limit));
  }

  @Override
  public int getNumberOfComments(ExoSocialActivity existingActivity) {
    return activityDao.getNumberOfComments(activityDao.getActivity(existingActivity.getId()));
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SortedSet<ActivityProcessor> getActivityProcessors() {
    return activityProcessors;
  }

  @Override
  public void updateActivity(ExoSocialActivity existingActivity) throws ActivityStorageException {
    Activity activityEntity = convertActivityToActivityEntity(existingActivity, null);
    activityDao.updateActivity(activityEntity);
    
  }

  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(ActivityBuilderWhere where, ActivityFilter filter, long offset, long limit) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity, int index, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(Identity spaceIdentity, int index, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit, String... activityTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity ownerIdentity, Identity viewerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public ExoSocialActivity getComment(String commentId) throws ActivityStorageException {
    return convertCommentEntityToComment( activityDao.getComment(Long.valueOf(commentId)));
  }
  
}
