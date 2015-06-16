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

import static org.exoplatform.social.core.storage.impl.StorageUtils.ASTERISK_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.EMPTY_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.PERCENT_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.SPACE_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.escapeSpecialCharacter;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.addons.storage.entity.Profile_;
import org.exoplatform.social.addons.storage.entity.RelationshipItem;
import org.exoplatform.social.addons.storage.entity.RelationshipItem_;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 4, 2015  
 */
public final class RelationshipQueryBuilder {

  private Identity owner;
  private Identity sender;
  private Identity receiver;
  private long offset;
  private long limit;
  private Relationship.Type status = null;
  private static Collection<Relationship.Type> types = Arrays.asList(Relationship.Type.INCOMING, Relationship.Type.OUTGOING);
  private ProfileFilter profileFilter;
  
  
  public static RelationshipQueryBuilder builder() {
    return new RelationshipQueryBuilder();
  }
  
  public RelationshipQueryBuilder owner(Identity owner) {
    this.owner = owner;
    return this;
  }
  
  public RelationshipQueryBuilder sender(Identity sender) {
    this.sender = sender;
    return this;
  }
  
  public RelationshipQueryBuilder receiver(Identity receiver) {
    this.receiver = receiver;
    return this;
  }
  
  public RelationshipQueryBuilder status(Relationship.Type status) {
    this.status = status;
    return this;
  }
  
  public RelationshipQueryBuilder offset(long offset) {
    this.offset = offset;
    return this;
  }
  
  public RelationshipQueryBuilder limit(long limit) {
    this.limit = limit;
    return this;
  }
  
  /**
   * Builds the Typed Query
   * @return
   */
  public TypedQuery<RelationshipItem> buildSingleRelationship() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<RelationshipItem> criteria = cb.createQuery(RelationshipItem.class);
    Root<RelationshipItem> relationship = criteria.from(RelationshipItem.class);
    
    Predicate predicate = null;
    if (this.sender != null && this.receiver != null) {
      predicate = cb.equal(relationship.get(RelationshipItem_.senderId), sender.getId()) ;
      predicate = cb.and(predicate, cb.equal(relationship.get(RelationshipItem_.receiverId), receiver.getId()));
    }
    
    CriteriaQuery<RelationshipItem> select = criteria.select(relationship).distinct(true);
    select.where(predicate);
    TypedQuery<RelationshipItem> typedQuery = em.createQuery(select);
    
    return typedQuery;
  }
  
  /**
   * Builds the Typed Query
   * @return
   */
  public TypedQuery<RelationshipItem> build() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<RelationshipItem> criteria = cb.createQuery(RelationshipItem.class);
    Root<RelationshipItem> relationship = criteria.from(RelationshipItem.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(relationship.get(RelationshipItem_.senderId), owner.getId()) ;
    }
    //status
    if (this.status != null) {
      if (Relationship.Type.PENDING.equals(this.status)) {
        predicate = cb.and(predicate, addInClause(cb, relationship.get(RelationshipItem_.status), types));
      } else {
        predicate = cb.and(predicate, cb.equal(relationship.get(RelationshipItem_.status), this.status));
      }
    }
    
    CriteriaQuery<RelationshipItem> select = criteria.select(relationship).distinct(true);
    select.where(predicate);

    TypedQuery<RelationshipItem> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    
    return typedQuery;
  }
  
  /**
   * Builds the Typed Query
   * @return
   */
  public TypedQuery<Long> buildCount() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<RelationshipItem> relationship = criteria.from(RelationshipItem.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(relationship.get(RelationshipItem_.senderId), owner.getId()) ;
    }
    //status
    if (this.status != null) {
      if (Relationship.Type.PENDING.equals(this.status)) {
        predicate = cb.and(predicate, addInClause(cb, relationship.get(RelationshipItem_.status), types));
      } else {
        predicate = cb.and(predicate, cb.equal(relationship.get(RelationshipItem_.status), this.status));
      }
    }
    
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(relationship));
    select.where(predicate);

    return em.createQuery(select);
  }

  public TypedQuery<RelationshipItem> buildLastConnections() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<RelationshipItem> criteria = cb.createQuery(RelationshipItem.class);
    Root<RelationshipItem> relationship = criteria.from(RelationshipItem.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(relationship.get(RelationshipItem_.senderId), owner.getId()) ;
    }
    //status
    if (this.status != null) {
      predicate = cb.and(predicate, cb.equal(relationship.get(RelationshipItem_.status), this.status));
    }
    
    CriteriaQuery<RelationshipItem> select = criteria.select(relationship).distinct(true);
    select.where(predicate);
    select.orderBy(cb.desc(relationship.<Long> get(RelationshipItem_.id)));

    TypedQuery<RelationshipItem> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    
    return typedQuery;
  }

  public TypedQuery<RelationshipItem> buildFilter() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<RelationshipItem> criteria = cb.createQuery(RelationshipItem.class);
    Root<RelationshipItem> relationship = criteria.from(RelationshipItem.class);
    Join<RelationshipItem, Profile> receiver = relationship.join(RelationshipItem_.receiver);
    //
    CriteriaQuery<RelationshipItem> select = criteria.select(relationship);
    select.where(buildPredicateFilter(cb, receiver, relationship));
    select.orderBy(cb.asc(receiver.get(Profile_.fullName)));
    //
    TypedQuery<RelationshipItem> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    //
    return typedQuery;
  }

  public TypedQuery<Long> buildFilterCount() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<RelationshipItem> relationship = criteria.from(RelationshipItem.class);
    Join<RelationshipItem, Profile> receiver = relationship.join(RelationshipItem_.receiver);
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(relationship));
    //
    select.where(buildPredicateFilter(cb, receiver, relationship));
    //
    return em.createQuery(select);
  }
  
  private Predicate buildPredicateFilter(CriteriaBuilder cb, Join<RelationshipItem, Profile> receiver, Root<RelationshipItem> relationship) {
    Predicate predicate = null;
    // owner
    if (this.owner != null) {
      predicate = cb.equal(relationship.get(RelationshipItem_.senderId), owner.getId());
    }
    // status
    if (this.status != null) {
      predicate = cb.and(predicate, cb.equal(relationship.get(RelationshipItem_.status), this.status));
    }

    Predicate pFilter = null;
    if (profileFilter != null) {
      String inputName = addPercentToStringInput(profileFilter.getName()).replace(ASTERISK_STR, SPACE_STR);
      String position = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getPosition()));
      String skills = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getSkills()));
      String company = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getCompany()));
      //
      if (!inputName.isEmpty()) {
        Predicate pName = cb.like(receiver.get(Profile_.fullName), inputName);
        pName = cb.or(pName, cb.like(receiver.get(Profile_.firstName), inputName));
        pFilter = cb.or(pName, cb.like(receiver.get(Profile_.lastName), inputName));
      }
      //
      if (!position.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(receiver.get(Profile_.positions), position));
      }
      //
      if (!skills.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(receiver.get(Profile_.skills), skills));
      }
      if (!company.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(receiver.get(Profile_.organizations), company));
      }

      String all = profileFilter.getAll();
      if (all != null && !all.trim().isEmpty()) {
        all = escapeSpecialCharacter(all.trim()).toLowerCase();
        Predicate pAll = cb.like(receiver.get(Profile_.fullName), all);
        pAll = cb.or(pAll, cb.like(receiver.get(Profile_.firstName), all));
        pAll = cb.or(pAll, cb.like(receiver.get(Profile_.lastName), all));
        pAll = cb.or(pAll, cb.like(receiver.get(Profile_.skills), all));
        pAll = cb.or(pAll, cb.like(receiver.get(Profile_.positions), all));
        pAll = cb.or(pAll, cb.like(receiver.get(Profile_.organizations), all));
        pAll = cb.or(pAll, cb.like(receiver.get(Profile_.jobsDescription), all));
        //
        pFilter = appendPredicate(cb, pFilter, pAll);
      }
    }
    //
    return appendPredicate(cb, predicate, pFilter);
  }
  
  
  public RelationshipQueryBuilder filter(ProfileFilter profileFilter) {
    this.profileFilter = profileFilter;
    return this;
  }
  
  private static String addPercentToStringInput(final String input) {
    if (input != null && !input.trim().isEmpty()) {
      if (input.indexOf(PERCENT_STR) == -1) {
        return PERCENT_STR + input.trim() + PERCENT_STR;
      }
      return input.trim();
    }
    return EMPTY_STR;
  }
  
  private Predicate appendPredicate(CriteriaBuilder cb, Predicate pSource, Predicate input) {
    if (pSource != null) {
      if (input != null) {
        return cb.and(pSource, input);
      }
      return pSource;
    } else {
      return input;
    }
  }
  
  private <T> Predicate addInClause(CriteriaBuilder cb, Path<Type> path, Collection<Type> types) {
    In<Type> in = cb.in(path);
    for (Type value : types) {
      in.value(value);
    }
    return in;
  }
}
