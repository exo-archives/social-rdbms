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
package org.exoplatform.social.addons.storage;

import java.util.List;

import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class SynchronizedRDBMSActivityStorage extends RDBMSActivityStorageImpl {
  
  
  public SynchronizedRDBMSActivityStorage(RelationshipStorage relationshipStorage, 
                                          IdentityStorage identityStorage, 
                                          SpaceStorage spaceStorage,
                                          ActivityDAO activityDAO, CommentDAO commentDAO, RelationshipDAO relationshipDAO) {
    super(relationshipStorage, identityStorage, spaceStorage, activityDAO, commentDAO, relationshipDAO);
  }
  
  @Override
  public ExoSocialActivity saveActivity(final Identity owner, final ExoSocialActivity activity) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
        return super.saveActivity(owner, activity);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public void updateActivity(ExoSocialActivity existingActivity) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      super.updateActivity(existingActivity);
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }
  
  @Override
  public void deleteActivity(String activityId) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      super.deleteActivity(activityId);
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }
  
  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity eXoComment) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
        super.saveComment(activity, eXoComment);
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }
  
  @Override
  public void deleteComment(String activityId, String commentId) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      super.deleteComment(activityId, commentId);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getActivityFeed(ownerIdentity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getActivitiesOfConnections(ownerIdentity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getUserActivities(Identity ownerIdentity, long offset, long limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getUserActivities(ownerIdentity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getUserSpacesActivities(ownerIdentity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentity(Identity ownerIdentity, long offset, long limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getActivitiesOfIdentity(ownerIdentity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getSpaceActivities(Identity ownerIdentity, int offset, int limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getSpaceActivities(ownerIdentity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getParentActivity(comment);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public int getNumberOfActivitesOnActivityFeed(Identity owner) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getNumberOfActivitesOnActivityFeed(owner);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public int getNumberOfUserActivities(Identity owner) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getNumberOfUserActivitiesForUpgrade(owner);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public int getNumberOfActivitiesOfConnections(Identity owner) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getNumberOfActivitiesOfConnections(owner);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public int getNumberOfUserSpacesActivities(Identity owner) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getNumberOfUserSpacesActivities(owner);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public int getNumberOfSpaceActivities(Identity owner) throws ActivityStorageException {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getNumberOfSpaceActivities(owner);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
  
  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity, int offset, int limit) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getComments(existingActivity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }

  @Override
  public int getNumberOfComments(ExoSocialActivity existingActivity) {
    boolean begun = GenericDAOImpl.startSynchronization();
    try {
      return super.getNumberOfComments(existingActivity);
    } finally {
      GenericDAOImpl.stopSynchronization(begun);
    }
  }
}
