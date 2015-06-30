/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.StreamItemDAO;
import org.exoplatform.social.addons.storage.entity.StreamItem;
import org.exoplatform.social.addons.storage.entity.StreamItem_;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class StreamItemDAOImpl extends GenericDAOJPAImpl<StreamItem, Long>  implements StreamItemDAO {

  //Add customize methods here
  
  public List<StreamItem> findStreamItemByActivityId(Long activityId) {
    try {
      EntityManager em = getEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<StreamItem> criteria = cb.createQuery(StreamItem.class);
      Root<StreamItem> root = criteria.from(StreamItem.class);
      CriteriaQuery<StreamItem> select = criteria.select(root);
      select.where(cb.equal(root.get(StreamItem_.activity), activityId));
      //
      TypedQuery<StreamItem> typedQuery = em.createQuery(select);
      return typedQuery.getResultList();
    } catch (RuntimeException e) {
      return null;
    }
  }
}
