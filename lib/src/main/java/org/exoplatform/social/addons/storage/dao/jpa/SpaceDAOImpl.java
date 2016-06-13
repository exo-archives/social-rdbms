package org.exoplatform.social.addons.storage.dao.jpa;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.SpaceDAO;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;

public class SpaceDAOImpl extends GenericDAOJPAImpl<SpaceEntity, Long> implements SpaceDAO {

  @Override
  public void delete(SpaceEntity entity) {
    getEntityManager().refresh(entity);
    super.delete(entity);
  }

  @Override
  public List<SpaceEntity> getLastSpaces(int limit) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("SpaceEntity.getLastSpaces", SpaceEntity.class);
    query.setMaxResults(limit);
    return query.getResultList();
  }

  @Override
  public SpaceEntity getSpaceByGroupId(String groupId) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("SpaceEntity.getSpaceByGroupId", SpaceEntity.class);
    query.setParameter("groupId", groupId);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public SpaceEntity getSpaceByURL(String url) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("SpaceEntity.getSpaceByURL", SpaceEntity.class);
    query.setParameter("url", url);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public SpaceEntity getSpaceByDisplayName(String spaceDisplayName) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("SpaceEntity.getSpaceByDisplayName", SpaceEntity.class);
    query.setParameter("displayName", spaceDisplayName);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public SpaceEntity getSpaceByPrettyName(String spacePrettyName) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery("SpaceEntity.getSpaceByPrettyName", SpaceEntity.class);
    query.setParameter("prettyName", spacePrettyName);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

}
