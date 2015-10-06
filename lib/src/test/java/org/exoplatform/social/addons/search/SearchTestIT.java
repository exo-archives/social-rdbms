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
package org.exoplatform.social.addons.search;

import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.KernelBootstrap;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.addons.test.AbstractCoreTest;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 30, 2015  
 */
public class SearchTestIT extends AbstractCoreTest {
  private IdentityManager identityManager;
  private Identity ghostIdentity, paulIdentity;
  private static KernelBootstrap bootstrap;
  private IndexingService indexingService;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    bootstrap = new KernelBootstrap(Thread.currentThread().getContextClassLoader());
    bootstrap.addConfiguration(AbstractCoreTest.class);
    bootstrap.boot();
    BaseExoTestCase.ownBootstrap = bootstrap;
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    bootstrap.dispose();
    bootstrap = null;
    BaseExoTestCase.ownBootstrap = null;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    indexingService = getService(IndexingService.class);
    identityManager = getService(IdentityManager.class);
    assertNotNull("identityManager must not be null", identityManager);

    ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
    paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
    org.exoplatform.services.security.Identity identity = getService(IdentityRegistry.class).getIdentity("root");
    ConversationState.setCurrent(new ConversationState(identity));
  }

  @Override
  protected void tearDown() throws Exception {

  }

  @Test
  public void testProfileCreateIndexProfile() throws Exception {
    //Given
    indexingService.index(ProfileIndexingServiceConnector.TYPE, paulIdentity.getId());
    setIndexingOperationTimestamp();
    indexingService.process();
    //When

  }

  // TODO This method MUST be removed : we MUST find a way to use exo-es-search Liquibase changelogs
  @ExoTransactional
  private void setIndexingOperationTimestamp() throws NoSuchFieldException, IllegalAccessException {
    EntityManagerService emService = PortalContainer.getInstance().getComponentInstanceOfType(EntityManagerService.class);
    emService.getEntityManager()
            .createQuery("UPDATE IndexingOperation set timestamp = :now")
            .setParameter("now", new Date(0L))
            .executeUpdate();
    //Refresh updated entities of the entity manager
    emService.getEntityManager().clear();
  }

}