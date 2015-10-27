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
import org.exoplatform.addons.es.index.IndexingOperationProcessor;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.addons.test.AbstractCoreTest;
import org.exoplatform.social.addons.updater.RelationshipMigrationService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.impl.RelationshipStorageImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 30, 2015  
 */
public class SearchTestIT extends AbstractCoreTest {
  
  protected final Log LOG = ExoLogger.getLogger(SearchTestIT.class);
  private IndexingService indexingService;
  private IndexingOperationProcessor indexingProcessor;
  private ProfileSearchConnector searchConnector;
  private String urlClient;
  private HttpClient client = new DefaultHttpClient();
  
  private RelationshipStorageImpl relationshipStorageImpl;
  private RelationshipMigrationService relationshipMigration;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    indexingService = getService(IndexingService.class);
    indexingProcessor = getService(IndexingOperationProcessor.class);
    identityManager = getService(IdentityManager.class);
    searchConnector = getService(ProfileSearchConnector.class);
    deleteAllProfilesInES();
    
    assertNotNull("identityManager must not be null", identityManager);
    urlClient = PropertyManager.getProperty("exo.es.search.server.url");

    org.exoplatform.services.security.Identity identity = getService(IdentityRegistry.class).getIdentity("root");
    ConversationState.setCurrent(new ConversationState(identity));
    
    identityManager = getService(IdentityManager.class);
    //
    relationshipStorageImpl = getService(RelationshipStorageImpl.class);
    relationshipMigration = getService(RelationshipMigrationService.class);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
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
  
  public void testMigrationAndReIndexing() throws Exception {
    // create jcr data
    LOG.info("Create connection for root,john,mary and demo");
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
    
    IdentityStorage identityStorage = CommonsUtils.getService(IdentityStorage.class);
    
    //ROOT
    Profile profile = rootIdentity.getProfile();
    profile.setProperty(Profile.FIRST_NAME, "Root");
    profile.setProperty(Profile.LAST_NAME, "Root");
    profile.setProperty(Profile.FULL_NAME, "Root Root");
    profile.setProperty(Profile.POSITION, "Admin");
    identityStorage.updateProfile(profile);
    
    //MARY
    profile = maryIdentity.getProfile();
    profile.setProperty(Profile.FIRST_NAME, "Kelly");
    profile.setProperty(Profile.LAST_NAME, "Mary");
    profile.setProperty(Profile.FULL_NAME, "Mary Kelly");
    profile.setProperty(Profile.POSITION, "Team Leader");
    identityStorage.updateProfile(profile);
    
    //DEMO
    profile = demoIdentity.getProfile();
    profile.setProperty(Profile.FIRST_NAME, "Exo");
    profile.setProperty(Profile.LAST_NAME, "Demo");
    profile.setProperty(Profile.FULL_NAME, "Demo Exo");
    profile.setProperty(Profile.POSITION, "Team member");
    identityStorage.updateProfile(profile);
    indexingProcessor.process();

    //John invites Demo
    Relationship johnToDemo = new Relationship(johnIdentity, demoIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(johnToDemo);
    
    //John invites Mary
    Relationship johnToMary = new Relationship(johnIdentity, maryIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(johnToMary);
    
    //John invites Root
    Relationship johnToRoot = new Relationship(johnIdentity, rootIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(johnToRoot);
    
    //Root invites Mary
    Relationship rootToMary = new Relationship(rootIdentity, maryIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(rootToMary);
    
    //Demo invites Mary
    Relationship demoToMary = new Relationship(demoIdentity, maryIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(demoToMary);
    
    //Demo invites Root
    Relationship demoToRoot = new Relationship(demoIdentity, rootIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(demoToRoot);
    
    
    //confirmed john and demo
    johnToDemo.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(johnToDemo);
    
    //confirmed john and demo
    johnToMary.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(johnToMary);
    
    //confirmed john and root
    johnToRoot.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(johnToRoot);
    
    //confirmed demo and mary
    demoToMary.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(demoToMary);
    
    //
    relationshipMigration.doMigration();
    end();
    begin();
    //phase 1: create the profiles re-indexing and push to queue what created by RelationshipMigration    
    indexingProcessor.process();
    //phase 2: create all the profiles indexing from queue
    indexingProcessor.process();
    refreshIndices();
    //
    RelationshipStorage relationshipStorage = CommonsUtils.getService(RelationshipStorage.class);
    
    ProfileFilter profileFilter = new ProfileFilter();
    List<Identity> results = relationshipStorage.getConnectionsByFilter(johnIdentity, profileFilter, 0, 10);
    assertEquals(3, results.size());
    
    ProfileFilter nameFilter = new ProfileFilter();
    nameFilter.setName("Root");
    results = relationshipStorage.getConnectionsByFilter(johnIdentity, nameFilter, 0, 10);
    assertEquals(1, results.size());
    
    //
    results = relationshipStorage.getOutgoingByFilter(rootIdentity, profileFilter, 0, 10);
    assertEquals(1, results.size());
    //
    results = relationshipStorage.getIncomingByFilter(rootIdentity, profileFilter, 0, 10);
    assertEquals(1, results.size());
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