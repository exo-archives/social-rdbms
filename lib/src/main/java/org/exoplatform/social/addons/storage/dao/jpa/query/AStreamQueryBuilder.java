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
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Activity_;
import org.exoplatform.social.addons.storage.entity.Comment;
import org.exoplatform.social.addons.storage.entity.Comment_;
import org.exoplatform.social.addons.storage.entity.RelationshipItem;
import org.exoplatform.social.addons.storage.entity.RelationshipItem_;
import org.exoplatform.social.core.identity.model.Identity;
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
  //streamType
  private String type = null;
  private boolean equalType;
  //memberOfSpaceIds
  private Collection<String> memberOfSpaceIds;
  private Identity myIdentity;
  //connectionSize
  private long connectionSize = 0;
  //order by
  private boolean descOrder = true;
  
  public static AStreamQueryBuilder builder() {
    return new AStreamQueryBuilder();
  }
  
  public AStreamQueryBuilder owner(Identity owner) {
    this.owner = owner;
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
  
  public AStreamQueryBuilder connectionSize(Identity myIdentity, long connectionSize) {
    this.myIdentity = myIdentity;
    this.connectionSize = connectionSize;
    return this;
  }
  
  public AStreamQueryBuilder equalType(String type) {
    this.type = type;
    this.equalType = true;
    return this;
  }
  
  public AStreamQueryBuilder notEqualType(String type) {
    this.type = type;
    this.equalType = false;
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
  /**
   * Builds the Typed Query
   * 
   * 1. My Activity Stream: owner's activities
   * 2. My Spaces: spaces's activity what ower is member
   * 3. My Connections: my owner's connections's activity
   * @return TypedQuery<Activity> instance
   */
  public TypedQuery<Activity> build() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Activity> criteria = cb.createQuery(Activity.class);
    Root<Activity> activity = criteria.from(Activity.class);
    
    List<Predicate> predicates = new ArrayList<Predicate>();
    //owner
    if (this.owner != null) {
      Predicate pOwner = cb.equal(activity.get(Activity_.posterId), owner.getId());
      pOwner = cb.or(pOwner, cb.equal(activity.get(Activity_.ownerId), owner.getId()));
      predicates.add(pOwner);
    }
    // space members
    if (this.memberOfSpaceIds != null && memberOfSpaceIds.size() > 0) {
      predicates.add(addInClause(cb, activity.get(Activity_.ownerId), memberOfSpaceIds));
    }
    
    //connections
    if (this.connectionSize > 0) {
      Subquery<String> subQuery1 = criteria.subquery(String.class);
      Root<RelationshipItem> subRoot1 = subQuery1.from(RelationshipItem.class);
      subQuery1.select(subRoot1.<String>get(RelationshipItem_.receiverId));
      subQuery1.where(cb.and(cb.equal(subRoot1.<String>get(RelationshipItem_.senderId), this.myIdentity.getId()),
                             cb.equal(subRoot1.<Relationship.Type>get(RelationshipItem_.status), Relationship.Type.CONFIRMED)));
      
      Predicate posterConnection = cb.and(cb.in(activity.get(Activity_.posterId)).value(subQuery1));
      Predicate ownerConnection = cb.and(cb.in(activity.get(Activity_.ownerId)).value(subQuery1));
      
      predicates.add(cb.and(posterConnection, ownerConnection));
    }

    
    //type
    if (this.type != null) {
      if (equalType) {
        predicates.add(cb.equal(activity.<String>get(Activity_.providerId), this.type));
      } else {
        predicates.add(cb.notEqual(activity.<String>get(Activity_.providerId), this.type));
      }
    }
    
    //newer or older
    if (this.sinceTime > 0) {
      if (isNewer) {
        predicates.add(cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
      } else {
        predicates.add(cb.lessThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
      }
    }

    //filter hidden = FALSE
    predicates.add(cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE));
    
    CriteriaQuery<Activity> select = criteria.select(activity).distinct(true);
    select.where(predicates.toArray(new Predicate[0]));
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
  
  /**
   * Builds query statement for FEED stream
   * 
   * Feed Stream: owner's activities U space's activity U owner's connections's activities
   * 
   * @return TypedQuery<Activity> instance
   */
  public TypedQuery<Activity> buildFeed() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Activity> criteria = cb.createQuery(Activity.class);
    Root<Activity> activity = criteria.from(Activity.class);
    
    Predicate predicate = null;
    
    //owner
    if (this.owner != null) {
      predicate = cb.equal(activity.get(Activity_.posterId), owner.getId());
      predicate = cb.or(predicate, cb.equal(activity.get(Activity_.ownerId), owner.getId()));
    }
    
    //comment
    Subquery<Activity> commentQuery = criteria.subquery(Activity.class);
    Root<Comment> subRootComment = commentQuery.from(Comment.class);
    commentQuery.select(subRootComment.<Activity>get(Comment_.activity));
    commentQuery.where(cb.equal(subRootComment.<String>get(Comment_.posterId), this.owner.getId()));
    
    if (predicate != null) {
      predicate = cb.or(predicate, cb.or(cb.in(activity).value(commentQuery)));
    } else {
      predicate = cb.in(activity).value(commentQuery);
    }
    
    // space members
    if (this.memberOfSpaceIds != null && memberOfSpaceIds.size() > 0) {
      if (predicate != null) {
        predicate = cb.or(predicate, addInClause(cb, activity.get(Activity_.ownerId), memberOfSpaceIds));
      } else {
        predicate = addInClause(cb, activity.get(Activity_.ownerId), memberOfSpaceIds);
      }
    }
    
    if (this.connectionSize > 0) {
      Subquery<String> subQuery1 = criteria.subquery(String.class);
      Root<RelationshipItem> subRoot1 = subQuery1.from(RelationshipItem.class);
      subQuery1.select(subRoot1.<String>get(RelationshipItem_.receiverId));
      subQuery1.where(cb.and(cb.equal(subRoot1.<String>get(RelationshipItem_.senderId), this.myIdentity.getId()),
                             cb.equal(subRoot1.<Relationship.Type>get(RelationshipItem_.status), Relationship.Type.CONFIRMED)));
      
      Predicate posterConnection = cb.and(cb.in(activity.get(Activity_.posterId)).value(subQuery1));
      Predicate ownerConnection = cb.and(cb.in(activity.get(Activity_.ownerId)).value(subQuery1));
      
      if (predicate != null) {
        predicate = cb.or(predicate, cb.and(posterConnection, ownerConnection));
      } else {
        predicate = cb.and(posterConnection, ownerConnection);
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
          predicate = cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime);
        }
      }
    }

    //filter hidden = FALSE
    if (predicate != null) {
      predicate = cb.and(predicate, cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE));
    } else {
      predicate = cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE);
    }
    
    
    CriteriaQuery<Activity> select = criteria.select(activity).distinct(true);
    select.where(predicate);
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
  
  /**
   * Build count statement to get the number of the activity base on given conditions
   * 
   * 1. My Activity Stream: owner's activities
   * 2. My Spaces: spaces's activity what ower is member
   * 3. My Connections: my owner's connections's activity
   * 
   * @return TypedQuery<Long> instance 
   */
  public TypedQuery<Long> buildCount() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Activity> activity = criteria.from(Activity.class);
    
    List<Predicate> predicates = new ArrayList<Predicate>();
    //owner
    if (this.owner != null) {
      Predicate pOwner = cb.equal(activity.get(Activity_.posterId), owner.getId());
      pOwner = cb.or(pOwner, cb.equal(activity.get(Activity_.ownerId), owner.getId()));
      predicates.add(pOwner);
    }
    // space members
    if (this.memberOfSpaceIds != null && memberOfSpaceIds.size() > 0) {
      predicates.add(addInClause(cb, activity.get(Activity_.ownerId), memberOfSpaceIds));
    }
    
    //connections
    if (this.connectionSize > 0) {
      Subquery<String> subQuery1 = criteria.subquery(String.class);
      Root<RelationshipItem> subRoot1 = subQuery1.from(RelationshipItem.class);
      subQuery1.select(subRoot1.<String>get(RelationshipItem_.receiverId));
      subQuery1.where(cb.equal(subRoot1.<String>get(RelationshipItem_.senderId), this.myIdentity.getId()));
      
      predicates.add(cb.in(activity.get(Activity_.posterId)).value(subQuery1));
      
    }
    
    //type
    if (this.type != null) {
      if (equalType) {
        predicates.add(cb.equal(activity.<String>get(Activity_.providerId), this.type));
      } else {
        predicates.add(cb.notEqual(activity.<String>get(Activity_.providerId), this.type));
      }
    }
    
    //newer or older
    if (this.sinceTime > 0) {
      if (isNewer) {
        predicates.add(cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
      } else {
        predicates.add(cb.lessThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
      }
    }

    //filter hidden = FALSE
    predicates.add(cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE));
    
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(activity));
    select.where(predicates.toArray(new Predicate[0]));

    return em.createQuery(select);
  }
  
  /**
   * Build count statement for FEED stream to get the number of the activity base on given conditions
   * 
   * @return TypedQuery<Long> instance 
   */
  public TypedQuery<Long> buildFeedCount() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Activity> activity = criteria.from(Activity.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(activity.get(Activity_.posterId), owner.getId());
      predicate = cb.or(predicate, cb.equal(activity.get(Activity_.ownerId), owner.getId()));
    }
    
    //comment
    Subquery<Activity> commentQuery = criteria.subquery(Activity.class);
    Root<Comment> subRootComment = commentQuery.from(Comment.class);
    commentQuery.select(subRootComment.<Activity>get(Comment_.activity));
    commentQuery.where(cb.equal(subRootComment.<String>get(Comment_.posterId), this.owner.getId()));
    
    if (predicate != null) {
      predicate = cb.or(predicate, cb.or(cb.in(activity).value(commentQuery)));
    } else {
      predicate = cb.in(activity).value(commentQuery);
    }
    
    //mention
    
    // space members
    if (this.memberOfSpaceIds != null && memberOfSpaceIds.size() > 0) {
      if (predicate != null) {
        predicate = cb.or(predicate, addInClause(cb, activity.get(Activity_.ownerId), memberOfSpaceIds));
      } else {
        predicate = addInClause(cb, activity.get(Activity_.ownerId), memberOfSpaceIds);
      }
    }
    
    if (this.connectionSize > 0) {
      Subquery<String> subQuery1 = criteria.subquery(String.class);
      Root<RelationshipItem> subRoot1 = subQuery1.from(RelationshipItem.class);
      subQuery1.select(subRoot1.<String>get(RelationshipItem_.receiverId));
      subQuery1.where(cb.and(cb.equal(subRoot1.<String>get(RelationshipItem_.senderId), this.myIdentity.getId()),
                             cb.equal(subRoot1.<Relationship.Type>get(RelationshipItem_.status), Relationship.Type.CONFIRMED)));
      
      Predicate posterConnection = cb.and(cb.in(activity.get(Activity_.posterId)).value(subQuery1));
      Predicate ownerConnection = cb.and(cb.in(activity.get(Activity_.ownerId)).value(subQuery1));
      
      if (predicate != null) {
        predicate = cb.or(predicate, cb.and(posterConnection, ownerConnection));
      } else {
        predicate = cb.and(posterConnection, ownerConnection);
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
          predicate = cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime);
        }
      }
    }

    //filter hidden = FALSE
    if (predicate != null) {
      predicate = cb.and(predicate, cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE));
    } else {
      predicate = cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE);
    }
    
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(activity));
    select.where(predicate);

    return em.createQuery(select);
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
}
