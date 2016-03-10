package org.exoplatform.social.addons.storage.dao.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.SpaceDAO;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.core.space.SpaceFilter;

public class SpaceDAOImpl extends GenericDAOJPAImpl<SpaceEntity, Long> implements SpaceDAO {

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
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public List<SpaceEntity> getVisitedSpaces(SpaceFilter spaceFilter, int offset, int limit) {
    Map<String, Object> params = new HashMap<>();
    params.put("userId", spaceFilter.getRemoteId());
    params.put("status", Status.MEMBER);
    if (spaceFilter.getAppId() != null) {
      params.put("app", "%" + spaceFilter.getAppId() + "%");      
    } else {
      params.put("app", "%");
    }
    
    return getList("SpaceEntity.getVisitedSpaces", params, offset, limit);
  }

  @Override
  public List<SpaceEntity> getLastAccessedSpace(SpaceFilter spaceFilter, int offset, int limit) {
    Map<String, Object> params = new HashMap<>();
    params.put("userId", spaceFilter.getRemoteId());
    params.put("status", Status.MEMBER);
    if (spaceFilter.getAppId() != null) {
      params.put("app", "%" + spaceFilter.getAppId() + "%");      
    } else {
      params.put("app", "%");
    }
    
    return getList("SpaceEntity.getLastAccessedSpace", params, offset, limit);
  }
  
  private List<SpaceEntity> getList(String nameQuery, Map<String, Object> params, int offset, int limit) {
    TypedQuery<SpaceEntity> query = getEntityManager().createNamedQuery(nameQuery, SpaceEntity.class);
    for (String name : params.keySet()) {
      query.setParameter(name, params.get(name));
    }
    if (limit > 0) {
      query.setFirstResult(offset);
      query.setMaxResults(limit);      
    }
    return query.getResultList();
  }

}
