/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.social.addons.storage.dao.jpa.query;

import org.exoplatform.social.addons.search.ExtendProfileFilter;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.addons.storage.entity.IdentityEntity_;
import org.exoplatform.social.addons.storage.entity.ProfileEntity;
import org.exoplatform.social.addons.storage.entity.ProfileEntity_;
import org.exoplatform.social.addons.storage.entity.ProfileExperience;
import org.exoplatform.social.addons.storage.entity.ProfileExperience_;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.profile.ProfileFilter;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class ProfileQueryBuilder {

  ExtendProfileFilter filter;

  private ProfileQueryBuilder() {

  }

  public static ProfileQueryBuilder builder() {
    return new ProfileQueryBuilder();
  }

  public ProfileQueryBuilder withFilter(ExtendProfileFilter filter) {
    this.filter = filter;
    return this;
  }

  /**
   *
   * @param em
   * @return
   */
  public TypedQuery[] build(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery query = cb.createQuery(IdentityEntity.class);

    Root<IdentityEntity> identity = query.from(IdentityEntity.class);
    Join<IdentityEntity, ProfileEntity> profile = identity.join(IdentityEntity_.profile, JoinType.INNER);;

    List<Predicate> predicates = new ArrayList<>();

    if (filter != null) {
      if (filter.isForceLoadProfile()) {
        Fetch<IdentityEntity,ProfileEntity> fetch = identity.fetch(IdentityEntity_.profile, JoinType.INNER);
      }

      if (filter.isExcludeDeleted()) {
        predicates.add(cb.isFalse(identity.get(IdentityEntity_.deleted)));
      }

      if (filter.getIdentityIds() != null && filter.getIdentityIds().size() > 0) {
        predicates.add(identity.get(IdentityEntity_.id).in(filter.getIdentityIds()));
      }

      if (filter.getProviderId() != null && !filter.getProviderId().isEmpty()) {
        predicates.add(cb.equal(identity.get(IdentityEntity_.providerId), filter.getProviderId()));
      }

      MapJoin<ProfileEntity, String, String> properties = profile.join(ProfileEntity_.properties, JoinType.LEFT);
      ListJoin<ProfileEntity, ProfileExperience> experience = profile.join(ProfileEntity_.experiences, JoinType.LEFT);

      List<Identity> excludes = filter.getExcludedIdentityList();
      if (excludes != null && excludes.size() > 0) {
        List<Long> ids = new ArrayList<>(excludes.size());
        for (Identity id : excludes) {
          ids.add(Long.parseLong(id.getId()));
        }
        predicates.add(cb.not(identity.get(IdentityEntity_.id).in(ids)));
      }

      String name = filter.getName();
      if (name != null && !name.isEmpty()) {
        name = processLikeString(name);
        Predicate[] pred = new Predicate[3];
        pred[0] = cb.and(cb.equal(properties.key(), Profile.FIRST_NAME), cb.like(properties.value(), name));
        pred[1] = cb.and(cb.equal(properties.key(), Profile.LAST_NAME), cb.like(properties.value(), name));
        pred[2] = cb.and(cb.equal(properties.key(), Profile.FULL_NAME), cb.like(properties.value(), name));
        predicates.add(cb.or(pred));
      }

      String val = filter.getPosition();
      if (val != null && !val.isEmpty()) {
        val = processLikeString(val);
        predicates.add(cb.like(experience.get(ProfileExperience_.position), val));
      }

      val = filter.getSkills();
      if (val != null && !val.isEmpty()) {
        val = processLikeString(val);
        predicates.add(cb.like(experience.get(ProfileExperience_.skills), val));
      }

      val = filter.getCompany();
      if (val != null && !val.isEmpty()) {
        val = processLikeString(val);
        predicates.add(cb.like(experience.get(ProfileExperience_.company), val));
      }

      char c = filter.getFirstCharacterOfName();
      if (c != '\u0000') {
        val = c + "%";
        predicates.add(cb.and(cb.equal(properties.key(), Profile.LAST_NAME), cb.like(properties.value(), val)));
      }

      String all = filter.getAll();
      if (all != null && !all.trim().isEmpty()) {
        all = processLikeString(all).toLowerCase();
        Predicate[] p = new Predicate[7];
        p[0] = cb.and(cb.equal(properties.key(), Profile.FIRST_NAME), cb.like(cb.lower(properties.value()), all));
        p[1] = cb.and(cb.equal(properties.key(), Profile.LAST_NAME), cb.like(cb.lower(properties.value()), all));
        p[2] = cb.and(cb.equal(properties.key(), Profile.FULL_NAME), cb.like(cb.lower(properties.value()), all));

        p[3] = cb.like(cb.lower(experience.get(ProfileExperience_.position)), all);
        p[4] = cb.like(cb.lower(experience.get(ProfileExperience_.skills)), all);
        p[5] = cb.like(cb.lower(experience.get(ProfileExperience_.company)), all);
        p[6] = cb.like(cb.lower(experience.get(ProfileExperience_.description)), all);

        predicates.add(cb.or(p));
      }
    }

    Predicate[] pds = predicates.toArray(new Predicate[predicates.size()]);

    query.select(cb.countDistinct(identity)).where(pds);
    TypedQuery<Long> count = em.createQuery(query);

    query.select(identity).where(pds);
    TypedQuery<IdentityEntity> select = em.createQuery(query);


    return new TypedQuery[]{select, count};
  }

  private String processLikeString(String s) {
    return "%" + s + "%";
  }
}
