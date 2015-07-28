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

import static org.exoplatform.social.core.storage.impl.StorageUtils.ASTERISK_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.EMPTY_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.PERCENT_STR;
import static org.exoplatform.social.core.storage.impl.StorageUtils.escapeSpecialCharacter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.addons.storage.entity.Profile_;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.search.Sorting;
import org.exoplatform.social.core.storage.IdentityStorageException;
import org.exoplatform.social.core.storage.impl.StorageUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * June 09, 2015  
 */
public class ProfileItemDAOImpl extends GenericDAOJPAImpl<Profile, Long> implements ProfileItemDAO {

  public Profile findProfileItemByIdentityId(final String identityId) {
    try {
      EntityManager em = getEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Profile> criteria = cb.createQuery(Profile.class);
      Root<Profile> root = criteria.from(Profile.class);
      CriteriaQuery<Profile> select = criteria.select(root);
      select.where(cb.equal(root.get(Profile_.identityId), identityId));
      //
      TypedQuery<Profile> typedQuery = em.createQuery(select);
      return typedQuery.getSingleResult();
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Override
  public List<Profile> getIdentitiesForMentions(ProfileFilter profileFilter, long offset, long limit) {
    return getIdentitiesByProfileFilter(profileFilter, offset, limit);
  }

  @Override
  public int getIdentitiesByProfileFilterCount(ProfileFilter profileFilter) {
    EntityManager em = getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Profile> root = criteria.from(Profile.class);
    CriteriaQuery<Long> select = criteria.select(cb.countDistinct(root));
    select.where(buildPredicateFromProfileFilter(profileFilter, cb, root));
    TypedQuery<Long> typedQuery = em.createQuery(select);
    return typedQuery.getSingleResult().intValue();
  }

  @Override
  public List<Profile> getIdentitiesByFirstCharacterOfName(ProfileFilter profileFilter, long offset, long limit) {
    return getIdentitiesByProfileFilter(profileFilter, offset, limit);
  }

  @Override
  public int getIdentitiesByFirstCharacterOfNameCount(ProfileFilter profileFilter) {
    return getIdentitiesByProfileFilterCount(profileFilter);
  }
  
  @Override
  public List<Profile> getIdentitiesByProfileFilter(ProfileFilter profileFilter, long offset, long limit) throws IdentityStorageException {
    EntityManager em = getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Profile> criteria = cb.createQuery(Profile.class);
    Root<Profile> root = criteria.from(Profile.class);
    //
    CriteriaQuery<Profile> select = criteria.select(root);
    select.where(buildPredicateFromProfileFilter(profileFilter, cb, root));
    //Apply order
    applyOrder(select, root, cb, profileFilter);
    TypedQuery<Profile> typedQuery = em.createQuery(select);
    if (limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    return typedQuery.getResultList();
  }
  
  @Override
  public List<Profile> getIdentitiesForUnifiedSearch(ProfileFilter profileFilter, long offset, long limit) {
    EntityManager em = getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Profile> criteria = cb.createQuery(Profile.class);
    Root<Profile> root = criteria.from(Profile.class);
    //
    CriteriaQuery<Profile> select = criteria.select(root);
    select.where(buildPredicateForUnifiedSearch(profileFilter, cb, root));
    //Apply order
    applyOrder(select, root, cb, profileFilter);
    TypedQuery<Profile> typedQuery = em.createQuery(select);
    if (limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    return typedQuery.getResultList();
  }
  
  private Predicate buildPredicateFromProfileFilter(ProfileFilter profileFilter,CriteriaBuilder cb, Root<Profile> root) {
    Predicate predicate = cb.equal(root.<Boolean>get(Profile_.isDeleted), Boolean.FALSE);
    //
    Predicate pFilter = null;
    if (profileFilter != null) {
      //Exclude identities
      List<Identity> excludedIdentityList = profileFilter.getExcludedIdentityList();
      if (excludedIdentityList != null && excludedIdentityList.size() > 0) {
        In<String> in = cb.in(root.get(Profile_.identityId));
        for (Identity id : excludedIdentityList) {
          in.value(id.getId());
        }
        predicate = cb.and(predicate, in.not());
      }
      //
      String inputName = addPercentToStringInput(profileFilter.getName().toLowerCase());
      String position = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getPosition()).toLowerCase());
      String skills = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getSkills()).toLowerCase());
      String company = addPercentToStringInput(StringEscapeUtils.escapeHtml(profileFilter.getCompany()).toLowerCase());
      char firstChar = profileFilter.getFirstCharacterOfName();
      //
      if (firstChar != '\u0000') {
        String fChar = String.valueOf(firstChar).toLowerCase() + PERCENT_STR;
        pFilter = cb.like(cb.lower(root.get(Profile_.lastName)), fChar);
      } else if (!inputName.isEmpty()) {
        Predicate pName = cb.like(cb.lower(root.get(Profile_.fullName)), inputName);
        pName = cb.or(pName, cb.like(cb.lower(root.get(Profile_.firstName)), inputName));
        pFilter = cb.or(pName, cb.like(cb.lower(root.get(Profile_.lastName)), inputName));
      }
      //
      if (!position.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(cb.lower(root.get(Profile_.positions)), position));
      }
      //
      if (!skills.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(cb.lower(root.get(Profile_.skills)), skills));
      }
      if (!company.isEmpty()) {
        pFilter = appendPredicate(cb, pFilter, cb.like(cb.lower(root.get(Profile_.organizations)), company));
      }
      //
      String all = profileFilter.getAll();
      if (all != null && !all.trim().isEmpty()) {
        all = escapeSpecialCharacter(all.trim()).toLowerCase();
        Predicate pAll = cb.like(cb.lower(root.get(Profile_.fullName)), all);
        pAll = cb.or(pAll, cb.like(cb.lower(root.get(Profile_.firstName)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(root.get(Profile_.lastName)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(root.get(Profile_.skills)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(root.get(Profile_.positions)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(root.get(Profile_.organizations)), all));
        pAll = cb.or(pAll, cb.like(cb.lower(root.get(Profile_.jobsDescription)), all));
        //
        pFilter = appendPredicate(cb, pFilter, pAll);
      }
      if (pFilter != null) {
        predicate = cb.and(predicate, pFilter);
      }
    }
    return predicate;
  }
  
  private Predicate buildPredicateForUnifiedSearch(ProfileFilter profileFilter,CriteriaBuilder cb, Root<Profile> root) {
    Predicate predicate = cb.equal(root.<Boolean>get(Profile_.isDeleted), Boolean.FALSE);
    //
    Predicate pFilter = null;
    if (profileFilter != null) {
      String all = profileFilter.getAll();
      if (all != null && !all.trim().isEmpty()) {
        all = escapeSpecialCharacter(all.trim()).toLowerCase();
        List<String> conditions = StorageUtils.processUnifiedSearchCondition(all);
        boolean hasNext = false;
        for (String condition : conditions) {
          condition = condition.toLowerCase();
          if (hasNext) {
            pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.fullName)), condition));
          } else {
            pFilter = cb.like(cb.lower(root.get(Profile_.fullName)), condition);
          }
          pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.firstName)), condition));
          pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.lastName)), condition));
          pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.skills)), condition));
          pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.positions)), condition));
          pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.organizations)), condition));
          pFilter = cb.or(pFilter, cb.like(cb.lower(root.get(Profile_.jobsDescription)), condition));
          hasNext = true;
        }
      }
      if (pFilter != null) {
        predicate = cb.and(predicate, pFilter);
      }
    }
    return predicate;
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
  
  private void applyOrder(CriteriaQuery<Profile> select, Root<Profile> root, CriteriaBuilder cb, ProfileFilter profileFilter) {
    Sorting sorting;
    if (profileFilter == null) {
      sorting = new Sorting(Sorting.SortBy.TITLE, Sorting.OrderBy.ASC);
    } else {
      sorting = profileFilter.getSorting();
    }
    switch (sorting.sortBy) {
      case DATE:
        //builder.orderBy(ProfileEntity.createdTime.getName(), ordering);
        if (sorting.orderBy.equals(Sorting.OrderBy.DESC)) {
          select.orderBy(cb.desc(root.<Long> get(Profile_.id)));
        } else {
          select.orderBy(cb.asc(root.<Long> get(Profile_.id)));
        }
        break;
      case RELEVANCY:
        //builder.orderBy(JCRProperties.JCR_RELEVANCY.getName(), ordering);
        if (sorting.orderBy.equals(Sorting.OrderBy.DESC)) {
          select.orderBy(cb.desc(root.<String> get(Profile_.lastName)), cb.desc(root.<String> get(Profile_.firstName)));
        } else {
          select.orderBy(cb.asc(root.<String> get(Profile_.lastName)), cb.asc(root.<String> get(Profile_.firstName)));
        }
      case TITLE:        
        //builder.orderBy(ProfileEntity.lastName.getName(), ordering).orderBy(ProfileEntity.firstName.getName(), ordering);
        if (sorting.orderBy.equals(Sorting.OrderBy.DESC)) {
          select.orderBy(cb.desc(root.<String> get(Profile_.lastName)), cb.desc(root.<String> get(Profile_.firstName)));
        } else {
          select.orderBy(cb.asc(root.<String> get(Profile_.lastName)), cb.asc(root.<String> get(Profile_.firstName)));
        }
        break;
    }
  }

}
