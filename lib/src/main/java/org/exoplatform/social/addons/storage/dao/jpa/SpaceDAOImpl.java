package org.exoplatform.social.addons.storage.dao.jpa;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.SpaceDAO;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;

public class SpaceDAOImpl extends GenericDAOJPAImpl<SpaceEntity, Long> implements SpaceDAO {

  @Override
  public List<SpaceEntity> getLastSpaces(int limit) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("Space.getLastSpaces", SpaceEntity.class);
    query.setMaxResults(limit);
    return query.getResultList();
  }

  @Override
  public SpaceEntity getSpaceByGroupId(String groupId) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("Space.getSpaceByGroupId", SpaceEntity.class);
    query.setParameter("groupId", groupId);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public SpaceEntity getSpaceByURL(String url) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("Space.getSpaceByURL", SpaceEntity.class);
    query.setParameter("url", url);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

}
