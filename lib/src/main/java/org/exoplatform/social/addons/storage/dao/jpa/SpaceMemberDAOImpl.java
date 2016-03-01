package org.exoplatform.social.addons.storage.dao.jpa;

import javax.persistence.Query;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.SpaceMemberDAO;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMember;

public class SpaceMemberDAOImpl extends GenericDAOJPAImpl<SpaceMember, Long> implements SpaceMemberDAO {

  @Override
  public void deleteBySpace(SpaceEntity entity) {
    Query query = getEntityManager().createNamedQuery("SpaceMember.deleteBySpace");
    query.setParameter("spaceId", entity.getId());
    query.executeUpdate();
  }

}
