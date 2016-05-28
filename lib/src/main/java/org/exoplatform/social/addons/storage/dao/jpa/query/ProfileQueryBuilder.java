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
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.addons.storage.entity.Connection_;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.addons.storage.entity.IdentityEntity_;
import org.exoplatform.social.addons.storage.entity.ProfileEntity;
import org.exoplatform.social.addons.storage.entity.ProfileEntity_;
import org.exoplatform.social.addons.storage.entity.ProfileExperienceEntity;
import org.exoplatform.social.addons.storage.entity.ProfileExperienceEntity_;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.relationship.model.Relationship;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Arrays;
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

      if (filter.isExcludeDisabled()) {
        predicates.add(cb.isTrue(identity.get(IdentityEntity_.enable)));
      }

      if (filter.getIdentityIds() != null && filter.getIdentityIds().size() > 0) {
        predicates.add(identity.get(IdentityEntity_.id).in(filter.getIdentityIds()));
      }

      if (filter.getProviderId() != null && !filter.getProviderId().isEmpty()) {
        predicates.add(cb.equal(identity.get(IdentityEntity_.providerId), filter.getProviderId()));
      }

      if (filter.getConnection() != null) {
        Identity owner = filter.getConnection();
        Long ownerId = Long.valueOf(owner.getId());
        Relationship.Type status = filter.getConnectionStatus();

        if (status == Relationship.Type.INCOMING) {
          Join<IdentityEntity, Connection> outgoing = identity.join(IdentityEntity_.outgoingConnections, JoinType.INNER);
          predicates.add(cb.and(cb.equal(outgoing.get(Connection_.receiver), ownerId), cb.equal(outgoing.get(Connection_.status), Relationship.Type.PENDING)));

        } else if (status == Relationship.Type.OUTGOING) {
          Join<IdentityEntity, Connection> incoming = identity.join(IdentityEntity_.incomingConnections, JoinType.INNER);
          predicates.add(cb.and(cb.equal(incoming.get(Connection_.sender), ownerId), cb.equal(incoming.get(Connection_.status), Relationship.Type.PENDING)));

        } else {
          Join<IdentityEntity, Connection> incoming = identity.join(IdentityEntity_.incomingConnections, JoinType.LEFT);
          Join<IdentityEntity, Connection> outgoing = identity.join(IdentityEntity_.outgoingConnections, JoinType.LEFT);

          Predicate out = cb.equal(outgoing.get(Connection_.receiver), ownerId);
          if (status != null) {
            out = cb.and(out, cb.equal(outgoing.get(Connection_.status), status));
          }

          Predicate in = cb.equal(incoming.get(Connection_.sender), ownerId);
          if (status != null) {
            in = cb.and(in, cb.equal(incoming.get(Connection_.status), status));
          }

          predicates.add(cb.or(in, out));
        }
      }

      ListJoin<ProfileEntity, ProfileExperienceEntity> experience = profile.join(ProfileEntity_.experiences, JoinType.LEFT);

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
        MapJoin<ProfileEntity, String, String> properties = profile.join(ProfileEntity_.properties, JoinType.LEFT);
        predicates.add(cb.and(cb.like(properties.value(), name), properties.key().in(Arrays.asList(Profile.FIRST_NAME, Profile.LAST_NAME, Profile.FULL_NAME))));
      }

      String val = filter.getPosition();
      if (val != null && !val.isEmpty()) {
        val = processLikeString(val);
        Predicate[] p = new Predicate[2];
        MapJoin<ProfileEntity, String, String> properties = profile.join(ProfileEntity_.properties, JoinType.LEFT);
        p[1] = cb.and(cb.like(properties.value(), val), cb.equal(properties.key(), Profile.POSITION));
        p[0] = cb.like(experience.get(ProfileExperienceEntity_.position), val);

        predicates.add(cb.or(p));
      }

      val = filter.getSkills();
      if (val != null && !val.isEmpty()) {
        val = processLikeString(val);
        predicates.add(cb.like(experience.get(ProfileExperienceEntity_.skills), val));
      }

      val = filter.getCompany();
      if (val != null && !val.isEmpty()) {
        val = processLikeString(val);
        predicates.add(cb.like(experience.get(ProfileExperienceEntity_.company), val));
      }

      char c = filter.getFirstCharacterOfName();
      if (c != '\u0000') {
        val = c + "%";
        MapJoin<ProfileEntity, String, String> properties = profile.join(ProfileEntity_.properties, JoinType.LEFT);
        predicates.add(cb.and(cb.equal(properties.key(), Profile.LAST_NAME), cb.like(properties.value(), val)));
      }

      String all = filter.getAll();
      if (all != null && !all.trim().isEmpty()) {
        all = processLikeString(all).toLowerCase();
        Predicate[] p = new Predicate[5];
        MapJoin<ProfileEntity, String, String> properties = profile.join(ProfileEntity_.properties, JoinType.LEFT);
        p[0] = cb.and(cb.like(properties.value(), name), properties.key().in(Arrays.asList(Profile.FIRST_NAME, Profile.LAST_NAME, Profile.FULL_NAME)));

        p[1] = cb.like(cb.lower(experience.get(ProfileExperienceEntity_.position)), all);
        p[2] = cb.like(cb.lower(experience.get(ProfileExperienceEntity_.skills)), all);
        p[3] = cb.like(cb.lower(experience.get(ProfileExperienceEntity_.company)), all);
        p[4] = cb.like(cb.lower(experience.get(ProfileExperienceEntity_.description)), all);

        predicates.add(cb.or(p));
      }
    }

    Predicate[] pds = predicates.toArray(new Predicate[predicates.size()]);

    query.select(cb.countDistinct(identity)).where(pds);
    TypedQuery<Long> count = em.createQuery(query);

    query.select(identity).distinct(true).where(pds);
    TypedQuery<IdentityEntity> select = em.createQuery(query);


    return new TypedQuery[]{select, count};
  }

  private String processLikeString(String s) {
    return "%" + s + "%";
  }
}
