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
package org.exoplatform.social.addons.storage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;

public class RDBMSActivityStorageImpl extends ActivityStorageImpl {

  private static final Log LOG = ExoLogger.getLogger(RDBMSActivityStorageImpl.class);
  private final ActivityDAO activityDAO;
  private final CommentDAO commentDAO;
  private final ConnectionDAO connectionDAO;
  private final IdentityStorage identityStorage;
  private final SpaceStorage spaceStorage;
  private final SortedSet<ActivityProcessor> activityProcessors;

  private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");
  public final static String COMMENT_PREFIX = "comment";
  public RDBMSActivityStorageImpl(RelationshipStorage relationshipStorage, 
                                      IdentityStorage identityStorage, 
                                      SpaceStorage spaceStorage,
                                      ActivityDAO activityDAO,
                                      CommentDAO commentDAO,
                                      ConnectionDAO connectionDAO) {
    
    super(relationshipStorage, identityStorage, spaceStorage);
    //
    this.identityStorage = identityStorage;
    this.activityProcessors = new TreeSet<ActivityProcessor>(processorComparator());
    this.activityDAO = activityDAO;
    this.commentDAO = commentDAO;
    this.spaceStorage = spaceStorage;
    this.connectionDAO = connectionDAO;
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
    
    activity.setId(String.valueOf(activityEntity.getId()));
    activity.setLikeIdentityIds(activityEntity.getLikerIds().toArray(new String[]{}));
    activity.setTemplateParams(activityEntity.getTemplateParams() != null ? new LinkedHashMap<String, String>(activityEntity.getTemplateParams())
                                                                         : new HashMap<String, String>());
    
    String ownerIdentityId = activityEntity.getOwnerId();
    ActivityStream stream = new ActivityStreamImpl();
    Identity owner = identityStorage.findIdentityById(ownerIdentityId);
    stream.setType(owner.getProviderId());
    stream.setPrettyId(owner.getRemoteId());
    stream.setId(owner.getId());
    //
    activity.setActivityStream(stream);
    activity.setStreamOwner(owner.getRemoteId());
    activity.setPosterId(activityEntity.getPosterId());
    //
    activity.isLocked(activityEntity.getLocked());
    activity.isHidden(activityEntity.getHidden());
    activity.setTitleId(activityEntity.getTitleId());
    activity.setPostedTime(activityEntity.getPosted());
    activity.setUpdated(activityEntity.getLastUpdated());
    //
    List<String> commentIds = new ArrayList<String>();
    List<String> replyToIds = new ArrayList<String>();
    List<Comment> comments = activityEntity.getComments() != null ? activityEntity.getComments() : new ArrayList<Comment>();
    for (Comment comment : comments) {
      if (!commentIds.contains(comment.getPosterId())) {
        commentIds.add(comment.getPosterId());
      }
      replyToIds.add(getExoCommentID(comment.getId()));
    }
    activity.setCommentedIds(commentIds.toArray(new String[commentIds.size()]));
    activity.setReplyToId(replyToIds.toArray(new String[replyToIds.size()]));
    activity.setMentionedIds(activityEntity.getMentionerIds().toArray(new String[activityEntity.getMentionerIds().size()]));
    //
    
    //
    processActivity(activity);
    
    return activity;
  }

  private Activity convertActivityToActivityEntity(ExoSocialActivity activity, String ownerId) {
    Activity activityEntity  =  new Activity();
    if (activity.getId() != null) {
      activityEntity = activityDAO.find(Long.valueOf(activity.getId()));
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
    Map<String, String> params = activity.getTemplateParams();
    if (params != null) {
      activityEntity.setTemplateParams(params);
    }
    //
    if (activity.getPostedTime() == null || activity.getPostedTime() <= 0) {
      activity.setPostedTime(System.currentTimeMillis());
    }
    activityEntity.setPosted(activity.getPostedTime());
    activityEntity.setLocked(activity.isLocked());
    activityEntity.setHidden(activity.isHidden());
    activityEntity.setLastUpdated(activity.getUpdated().getTime());
    activityEntity.setMentionerIds(new HashSet<String>(Arrays.asList(processMentions(activity.getTitle()))));
    //
    return activityEntity;
  }

  private ExoSocialActivity convertCommentEntityToComment(Comment comment) {
    ExoSocialActivity exoComment = new ExoSocialActivityImpl(comment.getPosterId(), null,
        comment.getTitle(), comment.getBody(), false);
    exoComment.setId(getExoCommentID(comment.getId()));
    exoComment.setTitle(comment.getTitle());
    exoComment.setTitleId(comment.getTitleId());
    exoComment.setBody(comment.getBody());
    exoComment.setTemplateParams(comment.getTemplateParams() != null ? new LinkedHashMap<String, String>(comment.getTemplateParams())
                                                                    : new HashMap<String, String>());
    exoComment.setPosterId(comment.getPosterId());
    exoComment.isComment(true);
    //
    exoComment.isLocked(comment.getLocked().booleanValue());
    exoComment.isHidden(comment.getHidden().booleanValue());
    exoComment.setUpdated(comment.getLastUpdated());
    //
    exoComment.setParentId(comment.getActivity().getId() + "");
    //
    exoComment.setPostedTime(comment.getPosted());
    exoComment.setUpdated(comment.getLastUpdated());
    //
    processActivity(exoComment);
    
    return exoComment;
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
      commentEntity = commentDAO.find(getCommentID(comment.getId()));
    }
    commentEntity.setTitle(comment.getTitle());
    commentEntity.setTitleId(comment.getTitleId());
    commentEntity.setBody(comment.getBody());
    commentEntity.setPosterId(comment.getPosterId() != null ? comment.getPosterId() : comment.getUserId());
    if (comment.getTemplateParams() != null) {
      commentEntity.setTemplateParams(comment.getTemplateParams());
    }
    //
    commentEntity.setLocked(comment.isLocked());
    commentEntity.setHidden(comment.isHidden());
    //
    long commentMillis = (comment.getPostedTime() != null ? comment.getPostedTime() : System.currentTimeMillis());
    commentEntity.setPosted(commentMillis);
    commentEntity.setLastUpdated(commentMillis);
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
    if (activityId == null || activityId.isEmpty()) {
      return null;
    }
    if(activityId != null && activityId.startsWith(COMMENT_PREFIX)) {
      return getComment(activityId);
    }
    Activity entity = activityDAO.find(Long.valueOf(activityId));
    //
    return convertActivityEntityToActivity(entity);
  }

  public ExoSocialActivity getComment(String commentId) throws ActivityStorageException {
    return convertCommentEntityToComment(commentDAO.find(getCommentID(commentId)));
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner) throws ActivityStorageException {
    return getUserActivities(owner, 0, -1);
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    return getUserActivitiesForUpgrade(owner, offset, limit);
  }

  @Override
  public List<ExoSocialActivity> getUserActivitiesForUpgrade(Identity owner, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToActivities(activityDAO.getUserActivities(owner, offset, limit));
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToActivities(activityDAO.getActivities(owner, viewer, offset, limit));
  }

  @Override
  
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
    activityEntity.setLastUpdated(System.currentTimeMillis());
    activityDAO.update(activityEntity);
    //
    activity = convertActivityEntityToActivity(activityEntity);
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
  
  @Override
  public ExoSocialActivity saveActivity(Identity owner, ExoSocialActivity activity) throws ActivityStorageException {
    Activity entity = convertActivityToActivityEntity(activity, owner.getId());
    //
    entity.setOwnerId(owner.getId());
    entity.setProviderId(owner.getProviderId());
    //
    activityDAO.create(entity);
    activity.setId(Long.toString(entity.getId()));
    //
    
    return activity;
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
  
  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) throws ActivityStorageException {
    try {
      Long commentId = getCommentID(comment.getId());
      return convertActivityEntityToActivity(commentDAO.findActivity(commentId));
    } catch (NumberFormatException e) {
      LOG.warn("The input ExoSocialActivity is not comment, it is Activity");
      return null;
    }
  }

  @Override
  public void deleteActivity(String activityId) throws ActivityStorageException {
    Activity a = activityDAO.find(Long.valueOf(activityId));
    activityDAO.delete(a);
  }

  @Override
  
  public void deleteComment(String activityId, String commentId) throws ActivityStorageException {
    Comment comment = commentDAO.find(getCommentID(commentId));
    commentDAO.delete(comment);
    //
    Activity activity = activityDAO.find(Long.valueOf(activityId));
    activity.getComments().remove(comment);
    //
    activity.setMentionerIds(processMentionOfComment(activity, comment, activity.getMentionerIds().toArray(new String[activity.getMentionerIds().size()]), processMentions(comment.getTitle()), false));
    //
    activityDAO.update(activity);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(List<Identity> connectionList, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(List<Identity> connectionList, TimestampType type, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public int getNumberOfUserActivities(Identity owner) throws ActivityStorageException {
    return getNumberOfUserActivitiesForUpgrade(owner);
  }

  @Override
  public int getNumberOfUserActivitiesForUpgrade(Identity owner) throws ActivityStorageException {
    return activityDAO.getNumberOfUserActivities(owner);
  }

  @Override
  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfNewerOnUserActivities(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getNewerOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getNewerUserActivities(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfOlderOnUserActivities(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getOlderUserActivities(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    return getActivityFeedForUpgrade(ownerIdentity, offset, limit);
  }

  @Override
  
  public List<ExoSocialActivity> getActivityFeedForUpgrade(Identity ownerIdentity, int offset, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getActivityFeed(ownerIdentity, offset, limit, getNumberOfConnections(ownerIdentity), memberOfSpaceIds(ownerIdentity)));
  }

  @Override
  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    return getNumberOfActivitesOnActivityFeedForUpgrade(ownerIdentity);
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    return activityDAO.getNumberOfActivitesOnActivityFeed(ownerIdentity, getNumberOfConnections(ownerIdentity), memberOfSpaceIds(ownerIdentity));
  }

  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfNewerOnActivityFeed(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getNewerFeedActivities(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfOlderOnActivityFeed(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getOlderFeedActivities(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    return getActivitiesOfConnectionsForUpgrade(ownerIdentity, offset, limit);
  }

  @Override
  
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity, int offset, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getActivitiesOfConnections(ownerIdentity, offset, limit, getNumberOfConnections(ownerIdentity)));
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    return getNumberOfActivitiesOfConnectionsForUpgrade(ownerIdentity);
  }

  @Override
  public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
    return activityDAO.getNumberOfActivitiesOfConnections(ownerIdentity, getNumberOfConnections(ownerIdentity));
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentity(Identity ownerIdentity, long offset, long limit) {
    return getUserActivities(ownerIdentity, offset, limit);
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, long limit) {
    return getNewerActivitiesOfConnections(ownerIdentity, baseActivity.getUpdated().getTime(), (int) limit);
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getOlderActivitiesOfConnections(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    return getUserSpacesActivitiesForUpgrade(ownerIdentity, offset, limit);
  }

  @Override
  
  public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity, int offset, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getUserSpacesActivities(ownerIdentity, offset, limit, memberOfSpaceIds(ownerIdentity)));
  }

  @Override
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    return getNumberOfUserSpacesActivitiesForUpgrade(ownerIdentity);
  }

  @Override
  public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
    return activityDAO.getNumberOfUserSpacesActivities(ownerIdentity, memberOfSpaceIds(ownerIdentity));
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfNewerOnUserSpacesActivities(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getNewerOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getNewerUserSpacesActivities(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
    return getOlderUserSpacesActivities(ownerIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity, int offset, int limit) {
    List<Comment> comments = commentDAO.getComments(activityDAO.find(Long.valueOf(existingActivity.getId())), offset, limit);
    
    return convertCommentEntitiesToComments(comments);
  }

  @Override
  public int getNumberOfComments(ExoSocialActivity existingActivity) {
    return commentDAO.getNumberOfComments(activityDAO.find(Long.valueOf(existingActivity.getId())));
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment) {
    return getNewerComments(existingActivity, baseComment, 0).size();
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment, int limit) {
    return getNewerComments(existingActivity, baseComment.getPostedTime(), limit);
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment) {
    return getOlderComments(existingActivity, baseComment, 0).size();
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment, int limit) {
    return getOlderComments(existingActivity, baseComment.getPostedTime(), limit);
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity, Long sinceTime, int limit) {
    List<Comment> comments = commentDAO.getNewerOfComments(activityDAO.find(Long.valueOf(existingActivity.getId())), sinceTime, limit);
    //
    return convertCommentEntitiesToComments(comments);
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity, Long sinceTime, int limit) {
    List<Comment> comments = commentDAO.getOlderOfComments(activityDAO.find(Long.valueOf(existingActivity.getId())), sinceTime, limit);
    return convertCommentEntitiesToComments(comments);
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity, Long sinceTime) {
    return getNewerComments(existingActivity, sinceTime, 0).size();
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity, Long sinceTime) {
    return getOlderComments(existingActivity, sinceTime, 0).size();
  }

  @Override
  public SortedSet<ActivityProcessor> getActivityProcessors() {
    return activityProcessors;
  }

  @Override
  public void updateActivity(ExoSocialActivity existingActivity) throws ActivityStorageException {
    ExoSocialActivity updatedActivity = getActivity(existingActivity.getId());
    if (existingActivity.getTitle() == null) existingActivity.setTitle(updatedActivity.getTitle());
    if (existingActivity.getBody() == null) existingActivity.setBody(updatedActivity.getBody());
    if (existingActivity.getTemplateParams() == null) existingActivity.setTemplateParams(updatedActivity.getTemplateParams());

    if (existingActivity.getId().startsWith(COMMENT_PREFIX)) {
      // update comment
      Comment comment = convertCommentToCommentEntity(existingActivity);
      comment.setLastUpdated(System.currentTimeMillis());
      commentDAO.update(comment);
    } else {
      Activity activityEntity = convertActivityToActivityEntity(existingActivity, null);
      activityEntity.setLastUpdated(System.currentTimeMillis());
      activityDAO.update(activityEntity);
    }
  }

  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfNewerOnActivityFeed(ownerIdentity, sinceTime, getNumberOfConnections(ownerIdentity), memberOfSpaceIds(ownerIdentity));
  }

  @Override
  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfNewerOnUserActivities(ownerIdentity, sinceTime);
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, sinceTime, getNumberOfConnections(ownerIdentity));
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfNewerOnUserSpacesActivities(ownerIdentity, sinceTime, memberOfSpaceIds(ownerIdentity));
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(ActivityBuilderWhere where, ActivityFilter filter, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    return getNumberOfSpaceActivitiesForUpgrade(spaceIdentity);
  }

  @Override
  public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
    return activityDAO.getNumberOfSpaceActivities(spaceIdentity);
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity, int offset, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getSpaceActivities(spaceIdentity, offset, limit));
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(Identity spaceIdentity, int offset, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getSpaceActivities(spaceIdentity, offset, limit));
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit, String... activityTypes) {
    return null;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity ownerIdentity, Identity viewerIdentity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
    return getNewerSpaceActivities(spaceIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfNewerOnSpaceActivities(spaceIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public List<ExoSocialActivity> getOlderOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
    return getOlderSpaceActivities(spaceIdentity, baseActivity.getUpdated().getTime(), limit);
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity) {
    return getNumberOfOlderOnSpaceActivities(spaceIdentity, baseActivity.getUpdated().getTime());
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
    return activityDAO.getNumberOfNewerOnSpaceActivities(spaceIdentity, sinceTime);
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getNewerOnActivityFeed(owner, sinceTime, limit, getNumberOfConnections(owner), memberOfSpaceIds(owner)));
  }

  @Override
  public List<ExoSocialActivity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getNewerOnUserActivities(owner, sinceTime, limit));
  }

  @Override
  public List<ExoSocialActivity> getNewerUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getNewerOnUserSpacesActivities(owner, sinceTime, limit, memberOfSpaceIds(owner)));
  }

  @Override
  public List<ExoSocialActivity> getNewerActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getNewerOnActivitiesOfConnections(owner, sinceTime, limit, getNumberOfConnections(owner)));
  }

  @Override
  public List<ExoSocialActivity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getNewerOnSpaceActivities(owner, sinceTime, limit));
  }

  @Override
  public List<ExoSocialActivity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getOlderOnActivityFeed(owner, sinceTime, limit, getNumberOfConnections(owner), memberOfSpaceIds(owner)));
  }

  @Override
  public List<ExoSocialActivity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getOlderOnUserActivities(owner, sinceTime, limit));
  }

  @Override
  public List<ExoSocialActivity> getOlderUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getOlderOnUserSpacesActivities(owner, sinceTime, limit, memberOfSpaceIds(owner)));
  }

  @Override
  public List<ExoSocialActivity> getOlderActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getOlderOnActivitiesOfConnections(owner, sinceTime, limit, getNumberOfConnections(owner)));
  }

  @Override
  public List<ExoSocialActivity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
    return convertActivityEntitiesToActivities(activityDAO.getOlderOnSpaceActivities(owner, sinceTime, limit));
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfOlderOnActivityFeed(ownerIdentity, sinceTime, getNumberOfConnections(ownerIdentity), memberOfSpaceIds(ownerIdentity));
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfOlderOnUserActivities(ownerIdentity, sinceTime);
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, sinceTime, getNumberOfConnections(ownerIdentity));
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, sinceTime, memberOfSpaceIds(ownerIdentity));
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
    return activityDAO.getNumberOfOlderOnSpaceActivities(ownerIdentity, sinceTime);
  }

  private Long getCommentID(String commentId) {
    return (commentId == null || commentId.trim().isEmpty()) ? null : Long.valueOf(commentId.replace(COMMENT_PREFIX, ""));
  }

  private String getExoCommentID(Long commentId) {
    return String.valueOf(COMMENT_PREFIX + commentId);
  }

  private void processActivity(ExoSocialActivity existingActivity) {
    Iterator<ActivityProcessor> it = activityProcessors.iterator();
    while (it.hasNext()) {
      try {
        it.next().processActivity(existingActivity);
      } catch (Exception e) {
        LOG.debug("activity processing failed ");
      }
    }
  }
  
  /**
   * Return the number of connections of owner
   * 
   * @param owner
   * @return
   */
  private long getNumberOfConnections(Identity owner) {
    return connectionDAO.count(owner, Relationship.Type.CONFIRMED);
  }
  
  /**
   * Gets the list of spaceIds what the given identify is member
   * 
   * @param ownerIdentity
   * @return
   */
  private List<String> memberOfSpaceIds(Identity ownerIdentity) {

    List<String> identitiesId = new ArrayList<String>();
    long offset = 0;
    long limit = 30;
    int loaded = loadIdRange(ownerIdentity, offset, offset + limit, identitiesId);
    while (loaded == limit) {
      loaded = loadIdRange(ownerIdentity, offset, offset + limit, identitiesId);
      offset += limit;
    }

    return identitiesId;

  }
  
  private int loadIdRange(Identity ownerIdentity, long offset, long limit, List<String> result) {
    List<Space> spaces = spaceStorage.getAccessibleSpaces(ownerIdentity.getRemoteId(), offset, limit);
    Identity identity = null;
    for (Space space : spaces) {
      identity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());
      if (identity != null) {
        result.add(identity.getId());
      }
    }
    return spaces.size();
  }
}
