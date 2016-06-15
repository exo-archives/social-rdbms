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
package org.exoplatform.social.addons.storage.dao.jpa;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.AStreamQueryBuilder;
import org.exoplatform.social.addons.storage.entity.ActivityEntity;
import org.exoplatform.social.addons.storage.entity.StreamItemEntity_;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class ActivityDAOImpl extends GenericDAOJPAImpl<ActivityEntity, Long> implements ActivityDAO {
  
  public List<ActivityEntity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    return AStreamQueryBuilder.builder()
                              .owner(owner)
                              .viewer(viewer)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }
  
  @Override
  public List<String> getUserIdsActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToIds(AStreamQueryBuilder.builder()
        .owner(owner)
        .offset(offset)
        .limit(limit)
        .buildId()
        .getResultList());
  }

  public List<ActivityEntity> getActivityFeed(Identity ownerIdentity, int offset, int limit, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }
  
  @Override
  public List<String> getActivityIdsFeed(Identity ownerIdentity,
                                           int offset,
                                           int limit,
                                           List<String> spaceIds) {

    return convertActivityEntitiesToIds(AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .offset(offset)
                              .limit(limit)
                              .buildId()
                              .getResultList());
  }

  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .buildCount().getSingleResult().intValue();
        
  }
  
  @Override
  public List<ActivityEntity> getNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .newer(sinceTime)
                              .ascOrder()
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  
  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getOlderOnActivityFeed(Identity ownerIdentity, long sinceTime,int limit, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .older(sinceTime)
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, long sinceTime, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .myIdentity(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getUserActivities(Identity owner,
                                          long offset,
                                          long limit) throws ActivityStorageException {

    return AStreamQueryBuilder.builder()
                              .owner(owner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();

  }
  
  @Override
  public int getNumberOfUserActivities(Identity ownerIdentity) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }
  
  @Override
  public List<ActivityEntity> getNewerOnUserActivities(Identity ownerIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .newer(sinceTime)
                              .ascOrder()
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();

  }

  @Override
  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getOlderOnUserActivities(Identity ownerIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .older(sinceTime)
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  public List<ActivityEntity> getSpaceActivities(Identity spaceOwner, long offset, long limit) throws ActivityStorageException {
    return AStreamQueryBuilder.builder()
                              .owner(spaceOwner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }
  
  public List<String> getSpaceActivityIds(Identity spaceOwner, long offset, long limit) throws ActivityStorageException {
    return convertActivityEntitiesToIds(AStreamQueryBuilder.builder()
                                                           .owner(spaceOwner)
                                                           .offset(offset)
                                                           .limit(limit)
                                                           .buildId()
                                                           .getResultList());
  }
  
  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }
  
  @Override
  public List<ActivityEntity> getNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .ascOrder()
                              .newer(sinceTime)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .older(sinceTime)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .offset(offset)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }
  
  @Override
  public List<String> getUserSpacesActivityIds(Identity ownerIdentity,
                                               int offset,
                                               int limit,
                                               List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return convertActivityEntitiesToIds(AStreamQueryBuilder.builder()
                                                             .memberOfSpaceIds(spaceIds)
                                                             .offset(offset)
                                                             .limit(limit)
                                                             .buildId()
                                                             .getResultList());
    } else {
      return Collections.emptyList();
    }
  }
  
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
  }
  
  @Override
  public List<ActivityEntity> getNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                       long sinceTime,
                                                       int limit, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .newer(sinceTime)
                                .ascOrder()
                                .offset(0)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .newer(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
  }

  @Override
  public List<ActivityEntity> getOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .older(sinceTime)
                                .offset(0)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .older(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
    
  }

  @Override
  public List<ActivityEntity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    return AStreamQueryBuilder.builder()
                              .myIdentity(ownerIdentity)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }
  
  @Override
  public List<String> getActivityIdsOfConnections(Identity ownerIdentity, int offset, int limit) {
    return convertActivityEntitiesToIds(AStreamQueryBuilder.builder()
                                                           .myIdentity(ownerIdentity)
                                                           .offset(offset)
                                                           .limit(limit)
                                                           .buildId()
                                                           .getResultList());
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
      return AStreamQueryBuilder.builder()
                                .myIdentity(ownerIdentity)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
  }

  @Override
  public List<ActivityEntity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long limit) {
    return AStreamQueryBuilder.builder()
                              .myIdentity(ownerIdentity)
                              .newer(sinceTime)
                              .ascOrder()
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .myIdentity(ownerIdentity)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .myIdentity(ownerIdentity)
                              .older(sinceTime)
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .myIdentity(ownerIdentity)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<ActivityEntity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit, String... activityTypes) {
    return AStreamQueryBuilder.builder()
                              .owner(posterIdentity)
                              .activityTypes(activityTypes)
                              .offset(offset)
                              .limit(limit)
                              .buildGetActivitiesByPoster()
                              .getResultList();
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity posterIdentity, String... activityTypes) {
    return AStreamQueryBuilder.builder()
                              .owner(posterIdentity)
                              .activityTypes(activityTypes)
                              .buildActivitiesByPosterCount()
                              .getSingleResult()
                              .intValue();
  }
  
  /**
   * Gets the activity's ID only and return the list of this one
   * 
   * @param list Activity's Ids
   * @return
   */
  private List<String> convertActivityEntitiesToIds(List<Tuple> list) {
    List<String> ids = new LinkedList<String>();
    if (list == null) return ids;
    for (Tuple t : list) {
      ids.add(String.valueOf(t.get(StreamItemEntity_.activityId.getName())));
    }
    return ids;
  }

  @Override
  public long getNumberOfComments(long activityId) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("SocActivity.numberCommentsOfActivity", Long.class);
    query.setParameter("activityId", activityId);
    return query.getSingleResult();
  }

  @Override
  public List<ActivityEntity> getComments(long activityId, int offset, int limit) {
    TypedQuery<ActivityEntity> query = getEntityManager().createNamedQuery("SocActivity.findCommentsOfActivity", ActivityEntity.class);
    query.setParameter("activityId", activityId);
    if (limit > 0) {
      query.setFirstResult(offset >= 0 ? offset : 0);
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  @Override
  public List<ActivityEntity> getNewerComments(long activityId, Date sinceTime, int offset, int limit) {
    TypedQuery<ActivityEntity> query = getEntityManager().createNamedQuery("SocActivity.findNewerCommentsOfActivity", ActivityEntity.class);
    query.setParameter("activityId", activityId);
    query.setParameter("sinceTime", sinceTime != null ? sinceTime.getTime() : 0);
    if (limit > 0) {
      query.setFirstResult(offset >= 0 ? offset : 0);
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  @Override
  public List<ActivityEntity> getOlderComments(long activityId, Date sinceTime, int offset, int limit) {
    TypedQuery<ActivityEntity> query = getEntityManager().createNamedQuery("SocActivity.findOlderCommentsOfActivity", ActivityEntity.class);
    query.setParameter("activityId", activityId);
    query.setParameter("sinceTime", sinceTime != null ? sinceTime.getTime() : 0);
    if (limit > 0) {
      query.setFirstResult(offset >= 0 ? offset : 0);
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  @Override
  public ActivityEntity getParentActivity(long commentId) {
    TypedQuery<ActivityEntity> query = getEntityManager().createNamedQuery("SocActivity.getParentActivity", ActivityEntity.class);
    query.setParameter("commentId", commentId);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public List<ActivityEntity> getAllActivities() {
    TypedQuery<ActivityEntity> query = getEntityManager().createNamedQuery("SocActivity.getAllActivities", ActivityEntity.class);
    return query.getResultList();
  }
}
