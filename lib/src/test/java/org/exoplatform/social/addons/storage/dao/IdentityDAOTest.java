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

package org.exoplatform.social.addons.storage.dao;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.addons.test.BaseCoreTest;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.junit.Test;

import javax.persistence.EntityExistsException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class IdentityDAOTest extends BaseCoreTest {
  private final Log LOG = ExoLogger.getLogger(IdentityDAOTest.class);

  private IdentityDAO identityDAO;

  private List<IdentityEntity> deleteIdentities = new ArrayList<IdentityEntity>();

  public void setUp() throws Exception {
    super.setUp();
    identityDAO = getService(IdentityDAO.class);

    assertNotNull("IdentityDAO must not be null", identityDAO);
    deleteIdentities = new ArrayList<IdentityEntity>();
  }

  @Override
  public void tearDown() throws Exception {
    for (IdentityEntity e : deleteIdentities) {
      identityDAO.delete(e);
    }

    super.tearDown();
  }

  public void testGetAllIds() {
    // Given
    IdentityEntity identityUser1 = identityDAO.create(createIdentity(OrganizationIdentityProvider.NAME, "user1"));
    IdentityEntity identityUser2 = identityDAO.create(createIdentity(OrganizationIdentityProvider.NAME, "user2"));
    IdentityEntity identitySpace1 = identityDAO.create(createIdentity(SpaceIdentityProvider.NAME, "space1"));

    // When
    List<Long> allIds = identityDAO.getAllIds(0, 0);

    // Then
    assertNotNull(allIds);
    assertEquals(3, allIds.size());
    assertTrue(allIds.contains(identityUser1.getId()));
    assertTrue(allIds.contains(identityUser2.getId()));
    assertTrue(allIds.contains(identitySpace1.getId()));

    deleteIdentities.add(identityUser1);
    deleteIdentities.add(identityUser2);
    deleteIdentities.add(identitySpace1);
  }

  public void testGetAllIdsByProvider() {
    // Given
    IdentityEntity identityUser1 = identityDAO.create(createIdentity(OrganizationIdentityProvider.NAME, "user1"));
    IdentityEntity identityUser2 = identityDAO.create(createIdentity(OrganizationIdentityProvider.NAME, "user2"));
    IdentityEntity identitySpace1 = identityDAO.create(createIdentity(SpaceIdentityProvider.NAME, "space1"));

    // When
    List<Long> allOrganizationIds = identityDAO.getAllIdsByProvider(OrganizationIdentityProvider.NAME, 0, 0);
    List<Long> allSpaceIds = identityDAO.getAllIdsByProvider(SpaceIdentityProvider.NAME, 0, 0);

    // Then
    assertNotNull(allOrganizationIds);
    assertEquals(2, allOrganizationIds.size());
    assertTrue(allOrganizationIds.contains(identityUser1.getId()));
    assertTrue(allOrganizationIds.contains(identityUser2.getId()));
    assertNotNull(allSpaceIds);
    assertEquals(1, allSpaceIds.size());
    assertTrue(allSpaceIds.contains(identitySpace1.getId()));

    deleteIdentities.add(identityUser1);
    deleteIdentities.add(identityUser2);
    deleteIdentities.add(identitySpace1);
  }

  public void testSaveNewIdentity() {
    IdentityEntity entity = createIdentity();

    identityDAO.create(entity);

    IdentityEntity e = identityDAO.find(entity.getId());

    assertNotNull(e);
    assertEquals("usera", e.getRemoteId());
    assertEquals(OrganizationIdentityProvider.NAME, e.getProviderId());

    deleteIdentities.add(e);
  }

  public void testDeleteIdentity() {
    IdentityEntity identity = createIdentity();
    identity = identityDAO.create(identity);

    long id = identity.getId();
    assertTrue(id > 0);

    identity = identityDAO.find(id);
    assertNotNull(identity);
    assertEquals(OrganizationIdentityProvider.NAME, identity.getProviderId());
    assertEquals("usera", identity.getRemoteId());

    identityDAO.delete(identity);

    assertNull(identityDAO.find(id));
  }

  public void testUpdateIdentity() {
    IdentityEntity identity = createIdentity();
    identityDAO.create(identity);

    identity = identityDAO.find(identity.getId());
    assertFalse(identity.isDeleted());
    assertTrue(identity.isEnabled());

    identity.setEnabled(false);
    identityDAO.update(identity);

    identity = identityDAO.find(identity.getId());
    assertFalse(identity.isDeleted());
    assertFalse(identity.isEnabled());

    deleteIdentities.add(identity);
  }

  public void testCreateDuplicateIdentity() {
    IdentityEntity identity1 = createIdentity();
    IdentityEntity identity2 = createIdentity();

    deleteIdentities.add(identityDAO.create(identity1));

    try {
      identityDAO.create(identity2);
      fail("EntityExistsException should be thrown");
    } catch (EntityExistsException ex) {
    }
  }

  private IdentityEntity createIdentity() {
    return createIdentity(OrganizationIdentityProvider.NAME, "usera");
  }

  private IdentityEntity createIdentity(String providerId, String remoteId) {
    IdentityEntity entity = new IdentityEntity();
    entity.setProviderId(providerId);
    entity.setRemoteId(remoteId);
    entity.setEnabled(true);
    entity.setDeleted(false);
    return entity;
  }
}
