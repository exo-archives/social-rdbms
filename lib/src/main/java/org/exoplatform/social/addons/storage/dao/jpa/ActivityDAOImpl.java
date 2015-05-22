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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.synchronization.SynchronizedGenericDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.StreamItem;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class ActivityDAOImpl extends SynchronizedGenericDAO<Activity, Long> implements ActivityDAO {
  
  //Add customize methods here

  public List<Activity> getActivityByLikerId(String likerId) {
    TypedQuery<Activity> query = lifecycleLookup().getCurrentEntityManager().createNamedQuery("getActivitiesByLikerId", Activity.class);
    query.setParameter("likerId", likerId);
    return query.getResultList();
  }

  private List<Activity> getActivities(String strQuery, long offset, long limit) throws ActivityStorageException {
    List<Activity> activities = new ArrayList<Activity>();
    if (strQuery == null || strQuery.isEmpty()) {
      return activities;
    }
    TypedQuery<StreamItem> typeQuery = lifecycleLookup().getCurrentEntityManager().createQuery(strQuery, StreamItem.class);
    if (limit > 0) {
      typeQuery.setFirstResult((int) offset);
      typeQuery.setMaxResults((int) limit);
    }
    List<StreamItem> streamItems = typeQuery.getResultList();
    for (StreamItem streamItem : streamItems) {
      activities.add(streamItem.getActivity());
    }
    return activities;
  }

  /**
   * Get activities of user that:
   *  + case 1: User is owner of activity or owner of activity-stream and not on space and not hidden.
   *  + case 2: User is owner of activity post on space and not hidden.
   * @param owner
   * @param offset
   * @param limit
   * @return
   * @throws ActivityStorageException
   */
  public List<Activity> getUserActivities(Identity owner, long time, boolean isNewer, long offset, long limit) throws ActivityStorageException {
    StringBuilder strQuery = new StringBuilder();//DISTINCT
    strQuery.append("select s from StreamItem s join s.activity a where ((a.ownerId ='")
            .append(owner.getId())
            .append("') or (s.ownerId = '")
            .append(owner.getId())
            .append("' and not s.streamType like '%SPACE%')) and (a.hidden = '0')")
            .append(buildSQLQueryByTime("a.lastUpdated", time, isNewer))
            .append(" order by a.lastUpdated desc");
    //
    return getActivities(strQuery.toString(), offset, limit);
  }
  
  @Override
  public int getNumberOfUserActivities(Identity ownerIdentity) {
    return getUserActivities(ownerIdentity, 0, false, 0, -1).size();
  }

  public List<Activity> getSpaceActivities(Identity owner, long time, boolean isNewer, long offset, long limit) throws ActivityStorageException {
    SpaceStorage spaceStorage = CommonsUtils.getService(SpaceStorage.class);
    Space space = spaceStorage.getSpaceByPrettyName(owner.getRemoteId());
    StringBuilder strQuery = new StringBuilder();//DISTINCT
    if (space != null) {
      strQuery.append("select s from StreamItem s join s.activity a where s.ownerId ='")
              .append(space.getId())
              .append("' and a.hidden = '0'")
              .append(buildSQLQueryByTime("a.lastUpdated", time, isNewer))
              .append(" order by a.lastUpdated desc");
    }
    //
    return getActivities(strQuery.toString(), offset, limit);
  }
  
  public List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    StringBuilder strQuery = new StringBuilder();//DISTINCT
    strQuery.append("select s from StreamItem s join s.activity a where s.ownerId = '")
            .append(owner.getId())
            .append("' and not s.streamType like '%SPACE%' and (a.ownerId ='")
            .append(owner.getId())
            .append("' or a.ownerId ='")
            .append(owner.getId())
            .append("') and (a.hidden = '0') order by a.lastUpdated desc");
    //
    return getActivities(strQuery.toString(), offset, limit);
  }

  private String buildSQLQueryByTime(String timeField, long time, boolean isNewer) {
    if (time <= 0) return "";
    StringBuilder sb = new StringBuilder();
    if (isNewer) {
      sb.append(" and (").append(timeField).append(" > '").append(time).append("')");
    } else {
      sb.append(" and (").append(timeField).append(" < '").append(time).append("')");
    }
    return sb.toString();
  }
  
  public List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    return getActivities(getFeedActivitySQLQuery(ownerIdentity, -1, false), offset, limit);
  }

  private String getFeedActivitySQLQuery(Identity ownerIdentity, long time, boolean isNewer) {
    RelationshipStorage relationshipStorage = CommonsUtils.getService(RelationshipStorage.class);
    SpaceStorage spaceStorage = CommonsUtils.getService(SpaceStorage.class);
    //
    List<Identity> relationships = relationshipStorage.getConnections(ownerIdentity);
    Set<String> relationshipIds = new HashSet<String>();
    for (Identity identity : relationships) {
      relationshipIds.add(identity.getId());
    }
    // get spaces where user is member
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getId());
    }
    StringBuilder sql = new StringBuilder();
    sql.append("select s from StreamItem s join s.activity a where ")
       .append(" ((s.ownerId='").append(ownerIdentity.getId()).append("'");
    
    if(CollectionUtils.isNotEmpty(spaces)){
      sql.append(" or s.ownerId in ('").append(StringUtils.join(spaceIds, "','")).append("') ");
    }
    if(CollectionUtils.isNotEmpty(relationships)){
      sql.append(" or (a.posterId in ('").append(StringUtils.join(relationshipIds, "','")).append("') ")
         .append("and s.streamType <> 0)");
    }
    sql.append(") and a.hidden='0'")
       .append(buildSQLQueryByTime("a.lastUpdated", time, isNewer))
       .append(")");
    sql.append(" order by a.lastUpdated desc");
    //
    return sql.toString();
  }

  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    return getActivityFeed(ownerIdentity, 0, -1).size();
  }

  @Override
  public List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    return getActivities(getUserSpacesActivitySQLQuery(ownerIdentity, -1, false), offset, limit);
  }
  
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    return getUserSpacesActivities(ownerIdentity, 0, -1).size();
  }
  
  private String getUserSpacesActivitySQLQuery(Identity ownerIdentity, long time, boolean isNewer) {
    SpaceStorage spaceStorage = CommonsUtils.getService(SpaceStorage.class);
    // get spaces where user is member
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    if (spaces == null || spaces.size() == 0) {
      return StringUtils.EMPTY;
    }
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getId());
    }
    //
    StringBuilder sql = new StringBuilder();
    sql.append("select s from StreamItem s join s.activity a where ")
       .append("s.ownerId in ('").append(StringUtils.join(spaceIds, "','")).append("') ");
    
    sql.append(" and a.hidden='0'")
       .append(buildSQLQueryByTime("a.lastUpdated", time, isNewer));
    sql.append(" order by a.lastUpdated desc");
    //
    return sql.toString();
  }

  @Override
  public List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    return getActivities(getConnectionsActivitySQLQuery(ownerIdentity, -1, false), offset, limit);
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    return getActivitiesOfConnections(ownerIdentity, 0, -1).size();
  }
  
  private String getConnectionsActivitySQLQuery(Identity ownerIdentity, long time, boolean isNewer) {
    RelationshipStorage relationshipStorage = CommonsUtils.getService(RelationshipStorage.class);
    //
    List<Identity> relationships = relationshipStorage.getConnections(ownerIdentity);
    if (relationships == null || relationships.size() == 0) {
      return StringUtils.EMPTY;
    }
    Set<String> relationshipIds = new HashSet<String>();
    for (Identity identity : relationships) {
      relationshipIds.add(identity.getId());
    }
    StringBuilder sql = new StringBuilder();
    sql.append("select s from StreamItem s join s.activity a where ")
       .append("a.posterId in ('").append(StringUtils.join(relationshipIds, "','")).append("') ")
       .append("and s.streamType <> 0");
    
    sql.append(" and a.hidden='0'")
       .append(buildSQLQueryByTime("a.lastUpdated", time, isNewer));
    sql.append(" order by a.lastUpdated desc");
    //
    System.out.println(sql.toString());
    return sql.toString();
  }

  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    return getSpaceActivities(spaceIdentity, 0, false, 0, -1).size();
  }

}
