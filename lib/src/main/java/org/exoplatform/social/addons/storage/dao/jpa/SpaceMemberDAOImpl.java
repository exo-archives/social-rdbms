/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.SpaceMemberDAO;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMemberEntity;

public class SpaceMemberDAOImpl extends GenericDAOJPAImpl<SpaceMemberEntity, Long> implements SpaceMemberDAO {

  @Override
  public void deleteBySpace(SpaceEntity entity) {
    Query query = getEntityManager().createNamedQuery("SpaceMember.deleteBySpace");
    query.setParameter("spaceId", entity.getId());
    query.executeUpdate();
  }

  @Override
  public SpaceMemberEntity getMember(String remoteId, Long spaceId) {
    TypedQuery<SpaceMemberEntity> query = getEntityManager().createNamedQuery("SpaceMember.getMember", SpaceMemberEntity.class);
    query.setParameter("userId", remoteId);
    query.setParameter("spaceId", spaceId);
    query.setParameter("status", SpaceMemberEntity.Status.MEMBER);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public List<Long> getSpacesIdsByUserName(String userId, int offset, int limit) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("SpaceMember.getSpaceIdentitiesIdByMemberId", Long.class);
    query.setParameter("userId", userId);
    query.setParameter("status", SpaceMemberEntity.Status.MEMBER);
    try {
      if (limit > 0) {
        query.setFirstResult(offset);
        query.setMaxResults(limit);
      }
      return query.getResultList();
    } catch (NoResultException ex) {
      return null;
    }
  }

}
