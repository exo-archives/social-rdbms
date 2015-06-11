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

import java.util.List;

import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public interface ActivityDAO extends GenericDAO<Activity, Long> {
  
  /**
   * 
   * @param owner
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  List<Activity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException;
  
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
  List<Activity> getNewerOnUserActivities(Identity ownerIdentity, long sinceTime, int limit);
  
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
  List<Activity> getOlderOnUserActivities(Identity ownerIdentity, long sinceTime, int limit);
  
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
  List<Activity> getSpaceActivities(Identity owner, long offset, long limit) throws ActivityStorageException;
  
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
  List<Activity> getNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit);
  
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
  List<Activity> getOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit);
  
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
  List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException;

  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @return
   */
  int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<Activity> getNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<Activity> getOlderOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @return
   */
  int getNumberOfUserSpacesActivities(Identity ownerIdentity);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<Activity> getNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<Activity> getOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime);
  
  /**
   * 
   * @param ownerIdentity
   * @param offset
   * @param limit
   * @return
   */
  List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit);
  
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
  List<Activity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long limit);
  
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
  List<Activity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, int limit);
  
  /**
   * 
   * @param ownerIdentity
   * @param sinceTime
   * @return
   */
  int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime);
  
}
