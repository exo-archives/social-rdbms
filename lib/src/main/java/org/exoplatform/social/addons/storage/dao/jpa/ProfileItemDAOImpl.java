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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.dao.jpa.synchronization.SynchronizedGenericDAO;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.addons.storage.entity.Profile_;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * June 09, 2015  
 */
public class ProfileItemDAOImpl extends SynchronizedGenericDAO<Profile, Long> implements ProfileItemDAO {

  public Profile findProfileItemByIdentityId(final String identityId) {
    try {
      EntityManager em = lifecycleLookup().getCurrentEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Profile> criteria = cb.createQuery(Profile.class);
      Root<Profile> root = criteria.from(Profile.class);
      CriteriaQuery<Profile> select = criteria.select(root);
      select.where(cb.equal(root.get(Profile_.identityId), identityId));
      //
      TypedQuery<Profile> typedQuery = em.createQuery(select);
      return typedQuery.getSingleResult();
    } catch (RuntimeException e) {
      return null;
    }
    
  }

}
