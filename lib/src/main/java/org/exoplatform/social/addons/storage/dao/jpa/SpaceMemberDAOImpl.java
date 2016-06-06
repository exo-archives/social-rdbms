package org.exoplatform.social.addons.storage.dao.jpa;

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

}
