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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
  private ProfileSearchConnector searchConnector;

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
    searchConnector = getService(ProfileSearchConnector.class);
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
  public void test_indexedProfile_isReturnedBySearch() throws Exception {
    //Given
    indexingService.index(ProfileIndexingServiceConnector.TYPE, paulIdentity.getId());
    setIndexingOperationTimestamp();
    indexingService.process();
    refreshIndices();
    ProfileFilter filter = new ProfileFilter();
    //When
    List<Identity> results = searchConnector.search(ghostIdentity, filter, null, 0, 10);
    //Then
    assertThat(results.size(), is(1));
  }

  private void refreshIndices() throws IOException {
    String urlClient = PropertyManager.getProperty("exo.es.search.client");
    HttpClient client = new DefaultHttpClient();
    HttpPost request = new HttpPost(urlClient + "/profile/_refresh");
    LOG.info("Refreshing ES by calling {}", request.getURI());
    HttpResponse response = client.execute(request);
    assertThat(response.getStatusLine().getStatusCode(), is(200));
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