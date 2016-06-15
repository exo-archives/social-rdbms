/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.social.addons.storage.dao;

import java.util.Date;
import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.social.addons.storage.entity.ActivityEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public interface ActivityDAO extends GenericDAO<ActivityEntity, Long> {
  
  /**
   * 
   * @param owner
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  List<ActivityEntity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException;
  
  
  /**
   * Gets Ids for User stream
   * 
   * @param owner
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  List<String> getUserIdsActivities(Identity owner, long offset, long limit) throws ActivityStorageException;
  
  
  /**
   * 
   * @param ownerIdentity
   * @return
   */
  int getNumberOfUserActivities(Identity ownerIdentity);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getNewerOnUserActivities(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnUserActivities(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getOlderOnUserActivities(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnUserActivities(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param owner
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  List<ActivityEntity> getSpaceActivities(Identity owner, long offset, long limit) throws ActivityStorageException;
  
  /**
   * 
   * @param owner
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  List<String> getSpaceActivityIds(Identity owner, long offset, long limit) throws ActivityStorageException;
  
  /**
   * 
   * @param spaceIdentity
   * @return
   */
  int getNumberOfSpaceActivities(Identity spaceIdentity);
  
  /**
   * 
   * @param spaceIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param spaceIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime);
  
  /**
   * 
   * @param spaceIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param spaceIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime);
  
  /**
   * 
   * @param owner
   * @param viewer
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  List<ActivityEntity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException;

  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<ActivityEntity> getActivityFeed(Identity ownerIdentity, int offset, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<String> getActivityIdsFeed(Identity ownerIdentity, int offset, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @return
   */
  int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getOlderOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, long sinceTime, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<ActivityEntity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<String> getUserSpacesActivityIds(Identity ownerIdentity, int offset, int limit, List<String> spaceIds);
  
  
  /**
   * 
   * @param ownerIdentity
   * @return
   */
  int getNumberOfUserSpacesActivities(Identity ownerIdentity, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, List<String> spaceIds);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<ActivityEntity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<String> getActivityIdsOfConnections(Identity ownerIdentity, int offset, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @return
   */
  int getNumberOfActivitiesOfConnections(Identity ownerIdentity);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<ActivityEntity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime);

  /**
   * @param posterIdentity
   * @param offset
   * @param limit
   * @param activityTypes
   * @return
   */
  List<ActivityEntity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit, String... activityTypes);

  /**
   * @param posterIdentity
   * @param activityTypes
   * @return
   */
  int getNumberOfActivitiesByPoster(Identity posterIdentity, String... activityTypes);

  long getNumberOfComments(long activityId);

  List<ActivityEntity> getComments(long activityId, int offset, int limit);

  List<ActivityEntity> getNewerComments(long activityId, Date sinceTime, int offset, int limit);

  List<ActivityEntity> getOlderComments(long activityId, Date sinceTime, int offset, int limit);

  ActivityEntity getParentActivity(long commentId);

  List<ActivityEntity> getAllActivities();
}
