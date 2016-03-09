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

package org.exoplatform.social.addons.updater.listener;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class IdentityReferenceUpdaterListener extends Listener<Identity, String> {
  @Override
  public void onEvent(Event<Identity, String> event) throws Exception {
    EntityManagerService emService = CommonsUtils.getService(EntityManagerService.class);
    EntityManager em = emService.getEntityManager();
    if (em == null) {
      return;
    }

    Identity identity = event.getSource();
    String newId = event.getData();
    String oldId = identity.getId();

    Query query;

    // Update Connection
    query = em.createQuery("UPDATE Connection c SET c.senderId = :newId WHERE c.senderId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    query = em.createQuery("UPDATE Connection c SET c.receiverId = :newId WHERE c.receiverId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Update activity poster
    query = em.createQuery("UPDATE Activity a SET a.posterId = :newId WHERE a.posterId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Activity owner
    query = em.createQuery("UPDATE Activity a SET a.ownerId = :newId WHERE a.ownerId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Comment poster
    query = em.createQuery("UPDATE Comment c SET c.posterId = :newId WHERE c.posterId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Comment owner
    query = em.createQuery("UPDATE Comment c SET c.ownerId = :newId WHERE c.ownerId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    //TODO: Can not use the JPQL for this?
    // Activity Liker
    query = em.createNativeQuery("UPDATE SOC_ACTIVITY_LIKERS SET LIKER_ID = ? WHERE LIKER_ID = ?");
    query.setParameter(1, newId);
    query.setParameter(2, oldId);
    query.executeUpdate();

    // Stream Item
    query = em.createQuery("UPDATE StreamItem s SET s.ownerId = :newId WHERE s.ownerId = :oldId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();
  }
}
