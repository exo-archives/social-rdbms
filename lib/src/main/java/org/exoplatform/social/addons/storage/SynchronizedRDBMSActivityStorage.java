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

import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.dao.StreamItemDAO;
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
                                          ActivityDAO activityDAO, StreamItemDAO streamItemDAO, CommentDAO commentDAO) {
    super(relationshipStorage, identityStorage, spaceStorage, activityDAO, streamItemDAO, commentDAO);
  }
  
  @Override
  public ExoSocialActivity saveActivity(final Identity owner, final ExoSocialActivity activity) throws ActivityStorageException {
    //the samples for start synchronize the EntityManager
    //this one is following the session-per-operation pattern
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.beginTransaction();
      try {
        return super.saveActivity(owner, activity);
      } finally {
        GenericDAOImpl.endTransaction(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }
  
  @Override
  public void updateActivity(ExoSocialActivity existingActivity) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.beginTransaction();
      try {
        super.updateActivity(existingActivity);
      } finally {
        GenericDAOImpl.endTransaction(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }
  
  @Override
  public void deleteActivity(String activityId) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.beginTransaction();
      try {
        super.deleteActivity(activityId);
      } finally {
        GenericDAOImpl.endTransaction(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
    
  }
  
  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity eXoComment) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.beginTransaction();
      try {
        super.saveComment(activity, eXoComment);
      } finally {
        GenericDAOImpl.endTransaction(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }
  @Override
  public void deleteComment(String activityId, String commentId) throws ActivityStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.beginTransaction();
      try {
        super.deleteComment(activityId, commentId);
      } finally {
        GenericDAOImpl.endTransaction(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
    
  }

}
