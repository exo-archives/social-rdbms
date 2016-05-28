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
package org.exoplatform.social.addons.storage.dao.jpa.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.exoplatform.commons.persistence.impl.EntityManagerHolder;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Activity_;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.addons.storage.entity.Connection_;
import org.exoplatform.social.addons.storage.entity.IdentityEntity_;
import org.exoplatform.social.addons.storage.entity.StreamItem;
import org.exoplatform.social.addons.storage.entity.StreamItem_;
import org.exoplatform.social.addons.storage.entity.StreamType;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 1, 2015  
 */
public final class AStreamQueryBuilder {
  private Identity owner;
  private long offset;
  private long limit;
  //newer or older
  private long sinceTime = 0;
  private boolean isNewer = false;
  //memberOfSpaceIds
  private Collection<String> memberOfSpaceIds;
  private Identity myIdentity;
  private Identity viewer;
  //order by
  private boolean descOrder = true;
  String[] activityTypes;

  public static AStreamQueryBuilder builder() {
    return new AStreamQueryBuilder();
  }

  public AStreamQueryBuilder owner(Identity owner) {
    this.owner = owner;
    return this;
  }
  
  public AStreamQueryBuilder viewer(Identity viewer) {
    this.viewer = viewer;
    return this;
  }
  
  public AStreamQueryBuilder myIdentity(Identity myIdentity) {
    this.myIdentity = myIdentity;
    return this;
  }

  public AStreamQueryBuilder offset(long offset) {
    this.offset = offset;
    return this;
  }

  public AStreamQueryBuilder limit(long limit) {
    this.limit = limit;
    return this;
  }

  public AStreamQueryBuilder activityTypes(String... activityTypes) {
    this.activityTypes = activityTypes;
    return this;
  }

  public AStreamQueryBuilder newer(long sinceTime) {
    this.isNewer = true;
    this.sinceTime = sinceTime;
    return this;
  }

  public AStreamQueryBuilder older(long sinceTime) {
    this.isNewer = false;
    this.sinceTime = sinceTime;
    return this;
  }

  public AStreamQueryBuilder memberOfSpaceIds(Collection<String> spaceIds) {
    this.memberOfSpaceIds = spaceIds;
    return this;
  }

  public AStreamQueryBuilder ascOrder() {
    this.descOrder = false;
    return this;
  }

  public AStreamQueryBuilder descOrder() {
    this.descOrder = true;
    return this;
  }

  
  public TypedQuery<Activity> build() {
    
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Activity> criteria = cb.createQuery(Activity.class);
    Root<Activity> activity = criteria.from(Activity.class);
    Join<Activity, StreamItem> streamItem = activity.join(Activity_.streamItems);

    CriteriaQuery<Activity> select;
    select = criteria.select(activity).distinct(true);
    select.where(getPredicateForStream(activity, streamItem, cb, criteria.subquery(Activity.class), criteria.subquery(Activity.class), criteria.subquery(String.class)));
    if (this.descOrder) {
      select.orderBy(cb.desc(activity.<Long> get(Activity_.lastUpdated)));
    } else {
      select.orderBy(cb.asc(activity.<Long> get(Activity_.lastUpdated)));
    }

    TypedQuery<Activity> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }

    return typedQuery;
  }
  
  public TypedQuery<Tuple> buildId() {
    
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
    Root<StreamItem> streamItem = criteria.from(StreamItem.class);

    criteria.multiselect(streamItem.get(StreamItem_.activityId).alias(StreamItem_.activityId.getName()), streamItem.get(StreamItem_.lastUpdated)).distinct(true);
    List<Predicate> predicates = getPredicateForIdsStream(streamItem, cb, criteria.subquery(String.class));
    criteria.where(cb.or(predicates.toArray(new Predicate[predicates.size()])));
    if (this.descOrder) {
      criteria.orderBy(cb.desc(streamItem.<Long> get(StreamItem_.lastUpdated)));
    } else {
      criteria.orderBy(cb.asc(streamItem.<Long> get(StreamItem_.lastUpdated)));
    }

    TypedQuery<Tuple> typedQuery = em.createQuery(criteria);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }

    return typedQuery;
  }

  /**
   * Build count statement for FEED stream to get the number of the activity base on given conditions
   *
   * @return TypedQuery<Long> instance 
   */
  public TypedQuery<Long> buildCount() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Activity> activity = criteria.from(Activity.class);
    Join<Activity, StreamItem> streamItem = activity.join(Activity_.streamItems);

    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(activity));
    select.where(getPredicateForStream(activity, streamItem, cb, criteria.subquery(Activity.class), criteria.subquery(Activity.class), criteria.subquery(String.class)));

    return em.createQuery(select);
  }
  
  private Predicate getPredicateForStream(Root<Activity> activity, Join<Activity, StreamItem> stream, CriteriaBuilder cb, Subquery<Activity> commentQuery,
                                 Subquery<Activity> mentionQuery, Subquery<String> subQuery1) {

    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(stream.get(StreamItem_.ownerId), owner.getId());
                  
      //view user's stream
      if (this.viewer != null && !this.viewer.getId().equals(this.owner.getId())) {
        predicate = cb.and(predicate, cb.equal(activity.get(Activity_.providerId), OrganizationIdentityProvider.NAME));
      }
    }
    
    // space members
    if (this.memberOfSpaceIds != null && memberOfSpaceIds.size() > 0) {
      if (predicate != null) {
        predicate = cb.or(predicate, addInClause(cb, stream.get(StreamItem_.ownerId), memberOfSpaceIds));
      } else {
        predicate = addInClause(cb, stream.get(StreamItem_.ownerId), memberOfSpaceIds);
      }
    }
    
    if (this.myIdentity != null) {
      long identityId = Long.valueOf(this.myIdentity.getId());
      Root<Connection> subRoot1 = subQuery1.from(Connection.class);

      Path receiver = subRoot1.get(Connection_.receiver).get(IdentityEntity_.id);
      Path sender = subRoot1.get(Connection_.sender).get(IdentityEntity_.id);

      CriteriaBuilder.Case select = cb.selectCase();
      select.when(cb.equal(receiver, identityId), sender).otherwise(receiver);

      subQuery1.select(select.as(String.class));
      subQuery1.where(cb.and(cb.or(cb.equal(sender, identityId), cb.equal(receiver, identityId)),
              cb.equal(subRoot1.<Relationship.Type>get(Connection_.status), Relationship.Type.CONFIRMED)));

      Predicate posterConnection = cb.and(cb.in(stream.get(StreamItem_.ownerId)).value(subQuery1), cb.equal(stream.get(StreamItem_.streamType), StreamType.POSTER));
      if (predicate != null) {
        predicate = cb.or(predicate, posterConnection);
      } else {
        predicate = posterConnection;
      }
    }
    //newer or older
    if (this.sinceTime > 0) {
      if (isNewer) {
        if (predicate != null) {
          predicate = cb.and(predicate, cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
        } else {
          predicate = cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime);
        }

      } else {
        if (predicate != null) {
          predicate = cb.and(predicate, cb.lessThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
        } else {
          predicate = cb.lessThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime);
        }
      }
    }

    //filter hidden = FALSE
    if (predicate != null) {
      predicate = cb.and(predicate, cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE));
    } else {
      predicate = cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE);
    }
    return predicate;
  }
  
  private List<Predicate> getPredicateForIdsStream(Root<StreamItem> stream, 
                                             CriteriaBuilder cb,
                                             Subquery<String> subQuery1) {

    List<Predicate> predicates = new ArrayList<Predicate>();
    Predicate predicate = null;
    // owner
    if (this.owner != null) {
      predicate = cb.equal(stream.get(StreamItem_.ownerId), owner.getId());

      // view user's stream
      if (this.viewer != null && !this.viewer.getId().equals(this.owner.getId())) {
        predicate = cb.and(predicate,
                           cb.equal(stream.get(StreamItem_.streamType), StreamType.POSTER));
      }
    }
    
    
    // space members
    if (this.memberOfSpaceIds != null && memberOfSpaceIds.size() > 0) {
      predicates.add(addInClause(cb, stream.get(StreamItem_.ownerId), memberOfSpaceIds));
    }

    if (this.myIdentity != null) {

      long identityId = Long.valueOf(this.myIdentity.getId());
      Root<Connection> subRoot1 = subQuery1.from(Connection.class);

      Path receiver = subRoot1.get(Connection_.receiver).get(IdentityEntity_.id);
      Path sender = subRoot1.get(Connection_.sender).get(IdentityEntity_.id);

      CriteriaBuilder.Case select = cb.selectCase();
      select.when(cb.equal(sender, identityId), receiver).otherwise(sender);

      subQuery1.select(select.as(String.class));
      subQuery1.where(cb.and(cb.or(cb.equal(sender, identityId), cb.equal(receiver, identityId)),
                             cb.equal(subRoot1.<Relationship.Type> get(Connection_.status), Relationship.Type.CONFIRMED)));
      
      predicates.add(cb.and(cb.in(stream.get(StreamItem_.ownerId)).value(subQuery1), cb.equal(stream.get(StreamItem_.streamType), StreamType.POSTER)));
    }
    // newer or older
    if (this.sinceTime > 0) {
      if (isNewer) {
        if (predicate != null) {
          predicate = cb.and(predicate, cb.greaterThan(stream.<Long> get(StreamItem_.lastUpdated),
                                                       this.sinceTime));
        } else {
          predicate = cb.greaterThan(stream.<Long> get(StreamItem_.lastUpdated), this.sinceTime);
        }

      } else {
        if (predicate != null) {
          predicate = cb.and(predicate,
                             cb.lessThan(stream.<Long> get(StreamItem_.lastUpdated), this.sinceTime));
        } else {
          predicate = cb.lessThan(stream.<Long> get(StreamItem_.lastUpdated), this.sinceTime);
        }
      }
    }

    
    //
    if (predicate != null) {
      predicates.add(predicate);
    }
    
    return predicates;
  }
  

  private <T> Predicate addInClause(CriteriaBuilder cb,
                                    Path<String> pathColumn,
                                    Collection<String> values) {

    In<String> in = cb.in(pathColumn);
    for (String value : values) {
      in.value(value);
    }
    return in;

  }

  public TypedQuery<Activity> buildGetActivitiesByPoster() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Activity> criteria = cb.createQuery(Activity.class);
    Root<Activity> activity = criteria.from(Activity.class);
    Predicate predicate = cb.equal(activity.get(Activity_.posterId), owner.getId());
    if (this.activityTypes != null && this.activityTypes.length > 0) {
      List<String> types = new ArrayList<String>(Arrays.asList(this.activityTypes));
      predicate = cb.and(predicate, addInClause(cb, activity.get(Activity_.type), types));
    }
    //
    CriteriaQuery<Activity> select = criteria.select(activity).distinct(true);
    select.where(predicate);
    select.orderBy(cb.desc(activity.<Long> get(Activity_.lastUpdated)));

    TypedQuery<Activity> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }

    return typedQuery;
  }

  public TypedQuery<Long> buildActivitiesByPosterCount() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Activity> activity = criteria.from(Activity.class);
    Predicate predicate = cb.equal(activity.get(Activity_.posterId), owner.getId());
    if (this.activityTypes != null && this.activityTypes.length > 0) {
      List<String> types = new ArrayList<String>(Arrays.asList(this.activityTypes));
      predicate = cb.and(predicate, addInClause(cb, activity.get(Activity_.type), types));
    }
    //
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(activity));
    select.where(predicate);

    return em.createQuery(select);
  }
}