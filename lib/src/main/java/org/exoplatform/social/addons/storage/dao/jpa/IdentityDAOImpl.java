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
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.addons.search.ExtendProfileFilter;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.ProfileQueryBuilder;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.core.profile.ProfileFilter;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.lang.reflect.Array;
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
  public List<Long> getAllIds(int offset, int limit) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("SocIdentity.getAllIds", Long.class);
    if (limit > 0) {
      query.setFirstResult(offset);
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  @Override
  public ListAccess<IdentityEntity> findIdentities(ExtendProfileFilter filter) {
    ProfileQueryBuilder qb = ProfileQueryBuilder.builder()
            .withFilter(filter);
    TypedQuery[] queries = qb.build(getEntityManager());

    return new JPAListAccess<>(IdentityEntity.class, queries[0], queries[1]);
  }

  public static class JPAListAccess<T> implements ListAccess<T> {
    private final TypedQuery<T> selectQuery;
    private final TypedQuery<Long> countQuery;
    private final Class<T> clazz;

    public JPAListAccess(Class<T> clazz, TypedQuery<T> selectQuery, TypedQuery<Long> countQuery) {
      this.clazz = clazz;
      this.selectQuery = selectQuery;
      this.countQuery = countQuery;
    }

    @Override
    public T[] load(int offset, int limit) throws Exception, IllegalArgumentException {
      if (limit > 0 && offset >= 0) {
        selectQuery.setFirstResult(offset);
        selectQuery.setMaxResults(limit);
      } else {
        selectQuery.setMaxResults(Integer.MAX_VALUE);
      }

      List<T> list = selectQuery.getResultList();
      if (list != null && list.size() > 0) {
        T[] arr = (T[])Array.newInstance(clazz, list.size());
        return list.toArray(arr);
      } else {
        return (T[])Array.newInstance(clazz, 0);
      }
    }

    @Override
    public int getSize() throws Exception {
      return countQuery.getSingleResult().intValue();
    }
  }
}
