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
import org.exoplatform.commons.persistence.impl.EntityManagerHolder;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.addons.storage.entity.Connection_;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.addons.storage.entity.Profile_;
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
  public TypedQuery<Connection> buildSingleRelationship() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Connection> criteria = cb.createQuery(Connection.class);
    Root<Connection> connection = criteria.from(Connection.class);
    
    Predicate predicate = null;
    if (this.sender != null && this.receiver != null) {
      predicate = cb.equal(connection.get(Connection_.senderId), sender.getId()) ;
      predicate = cb.and(predicate, cb.equal(connection.get(Connection_.receiverId), receiver.getId()));
    }
    
    CriteriaQuery<Connection> select = criteria.select(connection).distinct(true);
    select.where(predicate);
    TypedQuery<Connection> typedQuery = em.createQuery(select);
    
    return typedQuery;
  }
  
  /**
   * Builds the Typed Query
   * @return
   */
  public TypedQuery<Connection> build() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Connection> criteria = cb.createQuery(Connection.class);
    Root<Connection> connection = criteria.from(Connection.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(connection.get(Connection_.senderId), owner.getId()) ;
    }
    //status
    if (this.status != null) {
      if (Relationship.Type.PENDING.equals(this.status)) {
        predicate = cb.and(predicate, addInClause(cb, connection.get(Connection_.status), types));
      } else {
        predicate = cb.and(predicate, cb.equal(connection.get(Connection_.status), this.status));
      }
    }
    
    CriteriaQuery<Connection> select = criteria.select(connection).distinct(true);
    select.where(predicate);

    TypedQuery<Connection> typedQuery = em.createQuery(select);
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
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Connection> connection = criteria.from(Connection.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(connection.get(Connection_.senderId), owner.getId()) ;
    }
    //status
    if (this.status != null) {
      if (Relationship.Type.PENDING.equals(this.status)) {
        predicate = cb.and(predicate, addInClause(cb, connection.get(Connection_.status), types));
      } else {
        predicate = cb.and(predicate, cb.equal(connection.get(Connection_.status), this.status));
      }
    }
    
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(connection));
    select.where(predicate);

    return em.createQuery(select);
  }

  public TypedQuery<Connection> buildLastConnections() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Connection> criteria = cb.createQuery(Connection.class);
    Root<Connection> connection = criteria.from(Connection.class);
    
    Predicate predicate = null;
    //owner
    if (this.owner != null) {
      predicate = cb.equal(connection.get(Connection_.senderId), owner.getId()) ;
    }
    //status
    if (this.status != null) {
      predicate = cb.and(predicate, cb.equal(connection.get(Connection_.status), this.status));
    }
    
    CriteriaQuery<Connection> select = criteria.select(connection).distinct(true);
    select.where(predicate);
    select.orderBy(cb.desc(connection.<Long> get(Connection_.id)));

    TypedQuery<Connection> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    
    return typedQuery;
  }

  public TypedQuery<Connection> buildFilter() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Connection> criteria = cb.createQuery(Connection.class);
    Root<Connection> connection = criteria.from(Connection.class);
    Join<Connection, Profile> receiver = connection.join(Connection_.receiver);
    //
    CriteriaQuery<Connection> select = criteria.select(connection);
    select.where(buildPredicateFilter(cb, receiver, connection));
    select.orderBy(cb.asc(receiver.get(Profile_.fullName)));
    //
    TypedQuery<Connection> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    //
    return typedQuery;
  }

  public TypedQuery<Long> buildFilterCount() {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Connection> relationship = criteria.from(Connection.class);
    Join<Connection, Profile> receiver = relationship.join(Connection_.receiver);
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(relationship));
    //
    select.where(buildPredicateFilter(cb, receiver, relationship));
    //
    return em.createQuery(select);
  }
  
  private Predicate buildPredicateFilter(CriteriaBuilder cb, Join<Connection, Profile> receiver, Root<Connection> connection) {
    Predicate predicate = null;
    // owner
    if (this.owner != null) {
      predicate = cb.equal(connection.get(Connection_.senderId), owner.getId());
    }
    // status
    if (this.status != null) {
      predicate = cb.and(predicate, cb.equal(connection.get(Connection_.status), this.status));
    }

    Predicate pFilter = null;
    if (profileFilter != null) {
      String inputName = addPercentToStringInput(profileFilter.getName().toLowerCase());
      String position = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getPosition()).toLowerCase());
      String skills = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getSkills()).toLowerCase());
      String company = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getCompany()).toLowerCase());
      char firstChar = profileFilter.getFirstCharacterOfName();
      //
      if (firstChar != '\u0000') {
        String fChar = addPercentToStringInput(String.valueOf(firstChar).toLowerCase());
        pFilter = cb.like(cb.lower(receiver.get(Profile_.lastName)), fChar);
      } else if (!inputName.isEmpty()) {
        Predicate pName = cb.like(cb.lower(receiver.get(Profile_.fullName)), inputName);
        pName = cb.or(pName, cb.like(cb.lower(receiver.get(Profile_.firstName)), inputName));
        pFilter = cb.or(pName, cb.like(cb.lower(receiver.get(Profile_.lastName)), inputName));
      }
      //
      if (!position.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(cb.lower(receiver.get(Profile_.positions)), position));
      }
      //
      if (!skills.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(cb.lower(receiver.get(Profile_.skills)), skills));
      }
      if (!company.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(cb.lower(receiver.get(Profile_.organizations)), company));
      }
      //
      String all = profileFilter.getAll();
      if (all != null && !all.trim().isEmpty()) {
        all = escapeSpecialCharacter(all.trim()).toLowerCase();
        Predicate pAll = cb.like(cb.lower(receiver.get(Profile_.fullName)), all);
        pAll = cb.or(pAll, cb.like(cb.lower(receiver.get(Profile_.firstName)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(receiver.get(Profile_.lastName)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(receiver.get(Profile_.skills)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(receiver.get(Profile_.positions)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(receiver.get(Profile_.organizations)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(receiver.get(Profile_.jobsDescription)), all));
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
      return PERCENT_STR + input.trim().replace(ASTERISK_STR, PERCENT_STR) + PERCENT_STR;
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
