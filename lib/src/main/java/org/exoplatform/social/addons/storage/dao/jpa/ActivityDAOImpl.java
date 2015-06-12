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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.AStreamQueryBuilder;
import org.exoplatform.social.addons.storage.dao.jpa.synchronization.SynchronizedGenericDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class ActivityDAOImpl extends SynchronizedGenericDAO<Activity, Long> implements ActivityDAO {
  
  private final RelationshipDAO relationshipDAO;
  
  public ActivityDAOImpl(RelationshipDAO relationshipDAO) {
    this.relationshipDAO =  relationshipDAO;
  }
  
  public List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    return AStreamQueryBuilder.builder()
                              .owner(owner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  public List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(memberOfSpaceIds(ownerIdentity))
                              .connectionSize(ownerIdentity, relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED))
                              .offset(offset)
                              .limit(limit)
                              .buildFeed()
                              .getResultList();
    
  }


  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(memberOfSpaceIds(ownerIdentity))
                              .connectionSize(ownerIdentity, relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED))
                              .buildFeedCount().getSingleResult().intValue();
        
  }
  
  @Override
  public List<Activity> getNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(memberOfSpaceIds(ownerIdentity))
                              .connectionSize(ownerIdentity, relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED))
                              .newer(sinceTime)
                              .ascOrder()
                              .offset(0)
                              .limit(limit)
                              .buildFeed()
                              .getResultList();
    
  }

  
  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(memberOfSpaceIds(ownerIdentity))
                              .connectionSize(ownerIdentity, relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED))
                              .newer(sinceTime)
                              .buildFeedCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getOlderOnActivityFeed(Identity ownerIdentity, long sinceTime,int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(memberOfSpaceIds(ownerIdentity))
                              .connectionSize(ownerIdentity, relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED))
                              .older(sinceTime)
                              .offset(0)
                              .limit(limit)
                              .buildFeed()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(memberOfSpaceIds(ownerIdentity))
                              .connectionSize(ownerIdentity, relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED))
                              .older(sinceTime)
                              .buildFeedCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getUserActivities(Identity owner,
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
  public List<Activity> getNewerOnUserActivities(Identity ownerIdentity, long sinceTime, int limit) {
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
  public List<Activity> getOlderOnUserActivities(Identity ownerIdentity, long sinceTime, int limit) {
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
  
  

  public List<Activity> getSpaceActivities(Identity spaceOwner, long offset, long limit) throws ActivityStorageException {
    return AStreamQueryBuilder.builder()
                              .owner(spaceOwner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
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
  public List<Activity> getNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit) {
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
  public List<Activity> getOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit) {
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
  public List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    Collection<String> memberOfSpaceIds = memberOfSpaceIds(ownerIdentity);
    if (memberOfSpaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(memberOfSpaceIds)
                                .offset(offset)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }
  
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    Collection<String> memberOfSpaceIds = memberOfSpaceIds(ownerIdentity);
    if (memberOfSpaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(memberOfSpaceIds)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
  }
  
  @Override
  public List<Activity> getNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                       long sinceTime,
                                                       int limit) {
    Collection<String> memberOfSpaceIds = memberOfSpaceIds(ownerIdentity);
    if (memberOfSpaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(memberOfSpaceIds)
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
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime) {
    Collection<String> memberOfSpaceIds = memberOfSpaceIds(ownerIdentity);
    if (memberOfSpaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(memberOfSpaceIds)
                                .newer(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
  }

  @Override
  public List<Activity> getOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit) {
    Collection<String> memberOfSpaceIds = memberOfSpaceIds(ownerIdentity);
    if (memberOfSpaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(memberOfSpaceIds)
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
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime) {
    Collection<String> memberOfSpaceIds = memberOfSpaceIds(ownerIdentity);
    if (memberOfSpaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(memberOfSpaceIds)
                                .older(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
    
  }

  @Override
  public List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    long connectionSize = relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED);
    if (connectionSize > 0) {
      return AStreamQueryBuilder.builder()
          .connectionSize(ownerIdentity, connectionSize)
          .offset(offset)
          .limit(limit)
          .build()
          .getResultList();
    } else {
      return Collections.emptyList();
    }
    
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    long connectionSize = relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED);
    if (connectionSize > 0) {
      return AStreamQueryBuilder.builder()
                                .connectionSize(ownerIdentity, connectionSize)
                                .buildCount()
                                .getSingleResult()
                                .intValue();

    } else {
      return 0;
    }
  }

  @Override
  public List<Activity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long limit) {
    long connectionSize = relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED);
    if (connectionSize > 0) {
      return AStreamQueryBuilder.builder()
                                .connectionSize(ownerIdentity, connectionSize)
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
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime) {
    long connectionSize = relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED);
    if (connectionSize > 0) {
      return AStreamQueryBuilder.builder()
                                .connectionSize(ownerIdentity, connectionSize)
                                .newer(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();

    } else {
      return 0;
    }

  }

  @Override
  public List<Activity> getOlderOnActivitiesOfConnections(Identity ownerIdentity,
                                                          long sinceTime,
                                                          int limit) {
    long connectionSize = relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED);
    if (connectionSize > 0) {
      return AStreamQueryBuilder.builder()
                                .connectionSize(ownerIdentity, connectionSize)
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
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime) {
    long connectionSize = relationshipDAO.count(ownerIdentity, Relationship.Type.CONFIRMED);
    if (connectionSize > 0) {
      return AStreamQueryBuilder.builder()
                                .connectionSize(ownerIdentity, connectionSize)
                                .older(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();

    } else {
      return 0;
    }
  }

  /**
   * Gets the list of spaceIds what the given identify is member
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
    SpaceStorage spaceStorage = CommonsUtils.getService(SpaceStorage.class);
    IdentityStorage identityStorage = CommonsUtils.getService(IdentityStorage.class);
    
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
