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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.exoplatform.addons.es.index.IndexingOperationProcessor;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.component.test.KernelBootstrap;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.addons.test.AbstractCoreTest;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.api.RelationshipStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 30, 2015  
 */
public class SearchTestIT extends AbstractCoreTest {
  private static KernelBootstrap bootstrap;
  private IndexingService indexingService;
  private IndexingOperationProcessor indexingProcessor;
  private ProfileSearchConnector searchConnector;
  private RelationshipStorage relationshipStorage;
  private String urlClient;
  private HttpClient client = new DefaultHttpClient();

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
    indexingProcessor = getService(IndexingOperationProcessor.class);
    identityManager = getService(IdentityManager.class);
    searchConnector = getService(ProfileSearchConnector.class);
    relationshipStorage = getService(RelationshipStorage.class);
    assertNotNull("identityManager must not be null", identityManager);
    urlClient = PropertyManager.getProperty("exo.es.search.server.url");

    org.exoplatform.services.security.Identity identity = getService(IdentityRegistry.class).getIdentity("root");
    ConversationState.setCurrent(new ConversationState(identity));
    deleteAllProfilesInES();
  }

  @Override
  protected void tearDown() throws Exception {

  }

  public void test_indexedProfile_isReturnedBySearch() throws IOException {
    //Given
    Identity ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
    Identity paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
    indexingService.index(ProfileIndexingServiceConnector.TYPE, paulIdentity.getId());
    indexingProcessor.process();
    refreshIndices();
    ProfileFilter filter = new ProfileFilter();
    //When
    List<Identity> results = searchConnector.search(ghostIdentity, filter, null, 0, 10);
    //Then
    assertThat(results.size(), is(1));
  }

  public void test_outgoingConnection_isReturnedBySearch() throws IOException {
    //Given
    relationshipManager.inviteToConnect(johnIdentity, maryIdentity);

    indexingService.index(ProfileIndexingServiceConnector.TYPE, johnIdentity.getId());
    indexingService.index(ProfileIndexingServiceConnector.TYPE, maryIdentity.getId());
    indexingProcessor.process();
    refreshIndices();
    ProfileFilter filter = new ProfileFilter();
    //When
    //  All the users that have an incoming request from John
    List<Identity> resultsOutJohn = searchConnector.search(johnIdentity, filter, Relationship.Type.INCOMING, 0, 10);
    //  All the users that have sent an outgoing request to John
    List<Identity> resultsInJohn = searchConnector.search(johnIdentity, filter, Relationship.Type.OUTGOING, 0, 10);
    //  All the users that have an incoming request from Mary
    List<Identity> resultsOutMary = searchConnector.search(maryIdentity, filter, Relationship.Type.INCOMING, 0, 10);
    //  All the users that have sent an outgoing request to Mary
    List<Identity> resultsInMary = searchConnector.search(maryIdentity, filter, Relationship.Type.OUTGOING, 0, 10);
    //Then
    assertThat(resultsOutJohn.size(), is(1));
    assertThat(resultsInJohn.size(), is(0));
    assertThat(resultsOutMary.size(), is(0));
    assertThat(resultsInMary.size(), is(1));
  }

  private void refreshIndices() throws IOException {
    HttpPost request = new HttpPost(urlClient + "/profile/_refresh");
    LOG.info("Refreshing ES by calling {}", request.getURI());
    HttpResponse response = client.execute(request);
    assertThat(response.getStatusLine().getStatusCode(), is(200));
  }

  private void deleteAllProfilesInES() {
    indexingService.unindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
  }

}