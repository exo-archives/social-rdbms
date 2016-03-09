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

package org.exoplatform.social.addons.storage.dao.jpa;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.ProfileQueryBuilder;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.core.profile.ProfileFilter;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class IdentityDAOImpl extends GenericDAOJPAImpl<IdentityEntity, Long> implements IdentityDAO {
  @Override
  public IdentityEntity findByProviderAndRemoteId(String providerId, String remoteId) {
    TypedQuery<IdentityEntity> query = getEntityManager().createNamedQuery("SocIdentity.findByProviderAndRemoteId", IdentityEntity.class);
    query.setParameter("providerId", providerId);
    query.setParameter("remoteId", remoteId);

    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public long countIdentityByProvider(String providerId) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("SocIdentity.countIdentityByProvider", Long.class);
    query.setParameter("providerId", providerId);
    return query.getSingleResult();
  }

  @Override
  public List<IdentityEntity> findIdentityByProfileFilter(List<Long> relations, ProfileFilter profileFilter, long offset, long limit) {
    ProfileQueryBuilder qb = ProfileQueryBuilder.builder()
            .withIdentityIds(relations)
            .withFilter(profileFilter);

    return qb.build(getEntityManager()).getResultList();
  }

  @Override
  public List<Long> getAllIds(int offset, int limit) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("SocIdentity.getAllIds", Long.class);
    if (limit > 0) {
      query.setFirstResult(offset);
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }
}
