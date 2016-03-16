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
    query = em.createNamedQuery("SocConnection.migrateSenderId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    query = em.createNamedQuery("SocConnection.migrateReceiverId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Update activity poster
    query = em.createNamedQuery("SocActivity.migratePosterId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Activity owner
    query = em.createNamedQuery("SocActivity.migrateOwnerId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Comment poster
    query = em.createNamedQuery("SocComment.migratePosterId");
    query.setParameter("newId", newId);
    query.setParameter("oldId", oldId);
    query.executeUpdate();

    // Comment owner
    query = em.createNamedQuery("SocComment.migrateOwnerId");
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
