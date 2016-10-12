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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.mockito.Mockito;

import org.exoplatform.addons.es.index.IndexingOperationProcessor;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.addons.test.AbstractCoreTest;
import org.exoplatform.social.addons.updater.RelationshipMigrationService;
import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.model.Profile.UpdateType;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.exoplatform.social.core.storage.impl.RelationshipStorageImpl;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Sep
 * 30, 2015
 */
public class SearchTestIT extends AbstractCoreTest {

  protected final Log                               LOG    = ExoLogger.getLogger(SearchTestIT.class);

  private IndexingService                           indexingService;

  private IndexingOperationProcessor                indexingProcessor;

  private ProfileSearchConnector                    searchConnector;
  
  private PeopleElasticUnifiedSearchServiceConnector peopleSearchConnector;

  private SpaceElasticUnifiedSearchServiceConnector spaceSearchConnector;

  private String                                    urlClient;

  private HttpClient                                client = new DefaultHttpClient();

  private SpaceStorage                              spaceStorage;

  private IdentityStorageImpl                       identityStorageImpl;

  private RelationshipStorage                       relationshipStorage;

  private RelationshipMigrationService              relationshipMigration;

  private IdentityProvider<User>                    identityProvider;
  
  private SearchContext searchContext = Mockito.mock(SearchContext.class);

  private List<Identity> tearDownIdentityList;
  private List<Relationship> tearDownRelationshipList;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    indexingService = getService(IndexingService.class);
    indexingProcessor = getService(IndexingOperationProcessor.class);
    identityManager = getService(IdentityManager.class);
    searchConnector = getService(ProfileSearchConnector.class);
    peopleSearchConnector = getService(PeopleElasticUnifiedSearchServiceConnector.class);
    spaceSearchConnector = getService(SpaceElasticUnifiedSearchServiceConnector.class);
    deleteAllProfilesInES();
    deleteAllSpaceInES();

    assertNotNull("identityManager must not be null", identityManager);
    urlClient = PropertyManager.getProperty("exo.es.search.server.url");

    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity("root");
    ConversationState.setCurrent(new ConversationState(identity));
    
    Mockito.when(searchContext.handler(Mockito.anyString())).thenReturn(searchContext);
    Mockito.when(searchContext.lang(Mockito.anyString())).thenReturn(searchContext);
    Mockito.when(searchContext.siteName(Mockito.anyString())).thenReturn(searchContext);
    Mockito.when(searchContext.siteType(Mockito.anyString())).thenReturn(searchContext);
    Mockito.when(searchContext.path(Mockito.anyString())).thenReturn(searchContext);
    Mockito.doReturn("spaceLink").when(searchContext).renderLink();

    identityManager = getService(IdentityManager.class);
    spaceStorage = getService(SpaceStorage.class);
    //
    identityProvider = getService(IdentityProvider.class);
    identityStorageImpl = getService(IdentityStorageImpl.class);
    relationshipStorage = getService(RelationshipStorage.class);
    relationshipMigration = getService(RelationshipMigrationService.class);

    tearDownIdentityList = new ArrayList<>();
    tearDownRelationshipList = new ArrayList<>();
  }

  @Override
  public void tearDown() throws Exception {
    for (Relationship relationship : tearDownRelationshipList) {
      relationshipStorage.removeRelationship(relationship);
    }

    List<Space> spaces = spaceStorage.getAllSpaces();
    for (Space space : spaces) {
      spaceStorage.deleteSpace(space.getId());
    }

    for (Identity identity : tearDownIdentityList) {
      identityStorage.deleteIdentity(identity);
    }

    deleteAllSpaceInES();
    super.tearDown();
  }

  public void test_indexedProfile_isReturnedBySearch() throws IOException {
    // Given
    Identity ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
    Identity paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
    indexingService.index(ProfileIndexingServiceConnector.TYPE, paulIdentity.getId());
    indexingProcessor.process();
    refreshIndices();
    ProfileFilter filter = new ProfileFilter();
    // When
    List<Identity> results = searchConnector.search(ghostIdentity, filter, null, 0, 10);
    // Then
    assertThat(results.size(), is(1));
  }

  public void test_outgoingConnection_isReturnedBySearch() throws IOException {
    // Given
    relationshipManager.inviteToConnect(johnIdentity, maryIdentity);

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    //indexingService.index(ProfileIndexingServiceConnector.TYPE, maryIdentity.getId());
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();
    ProfileFilter filter = new ProfileFilter();
    // When
    // All the users that have an incoming request from John
    List<Identity> resultsOutJohn = searchConnector.search(johnIdentity, filter, Relationship.Type.OUTGOING, 0, 10);
    // All the users that have sent an outgoing request to John
    List<Identity> resultsInJohn = searchConnector.search(johnIdentity, filter, Relationship.Type.INCOMING, 0, 10);
    // All the users that have an incoming request from Mary
    List<Identity> resultsOutMary = searchConnector.search(maryIdentity, filter, Relationship.Type.OUTGOING, 0, 10);
    // All the users that have sent an outgoing request to Mary
    List<Identity> resultsInMary = searchConnector.search(maryIdentity, filter, Relationship.Type.INCOMING, 0, 10);
    // Then
    assertThat(resultsOutJohn.size(), is(1));
    assertThat(resultsInJohn.size(), is(0));
    assertThat(resultsOutMary.size(), is(0));
    assertThat(resultsInMary.size(), is(1));
  }

  
  public void testPeopleName() throws Exception {    
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", true);

    // ROOT
    Profile profile = rootIdentity.getProfile();
    profile.setListUpdateTypes(Arrays.asList(UpdateType.ABOUT_ME));
    profile.setProperty(Profile.FULL_NAME, "Root Root");
    identityManager.updateProfile(profile);
    indexingService.index(ProfileIndexingServiceConnector.TYPE, rootIdentity.getId());
    
    indexingProcessor.process();
    refreshIndices();
    
    assertEquals(1, peopleSearchConnector.search(searchContext, "Root Root", null, 0, 10, null, null).size());
  }

  public void testSpaceName() throws Exception {
    createSpace("testSpaceName abcd efgh", null, null);

    assertEquals(1, spaceSearchConnector.search(searchContext, "*space*", null, 0, 10, null, null).size());

    assertEquals(1, spaceSearchConnector.search(searchContext, "*name*", null, 0, 10, null, null).size());

    assertEquals(1, spaceSearchConnector.search(searchContext, "*abcd*", null, 0, 10, null, null).size());
  }

  public void testSpaceDisplayName() throws Exception {
    createSpace("pretty", "displayName abc def", null);

    assertEquals(1, spaceSearchConnector.search(searchContext, "diSplayName*", null, 0, 10, null, null).size());
    assertEquals(1, spaceSearchConnector.search(searchContext, "*abc*", null, 0, 10, null, null).size());
    assertEquals(1, spaceSearchConnector.search(searchContext, "*ef*", null, 0, 10, null, null).size());
  }

  public void testSpaceDescription() throws Exception {
    createSpace("pretty", null, "spaceDescription 123 456");

    assertEquals(1, spaceSearchConnector.search(searchContext, "*scription* *23*", null, 0, 10, null, null).size());
    assertEquals(1, spaceSearchConnector.search(searchContext, "*123*", null, 0, 10, null, null).size());
    assertEquals(1, spaceSearchConnector.search(searchContext, "*56*", null, 0, 10, null, null).size());
  }

  //
  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getConnectionsByFilter(providerId, Identity, ProfileFilter)}
   * in case Identity had no connection yet
   * @throws Exception
   */
  public void testGetConnectionsByFilterEmpty() throws Exception {
    populateData();
    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    List<Identity> identities = relationshipStorage.getConnectionsByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be " + identities.size(), 0, identities.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getConnectionsByFilter(providerId, Identity, ProfileFilter)}
   *
   * @throws Exception
   * @since 1.2.3
   */
  public void testGetConnectionsByFilter() throws Exception {
    populateData();
    populateRelationshipData(Relationship.Type.CONFIRMED);

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();

    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    List<Identity> identities = relationshipStorage.getConnectionsByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be " + 8, 8, identities.size());

    pf.setPosition("developer");
    pf.setName("FirstName9");
    identities = relationshipStorage.getConnectionsByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be " + identities.size(), 1, identities.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getIncomingByFilter(providerId, Identity, ProfileFilter)}
   *
   *
   * @throws Exception
   * @since 1.2.3
   */
  public void testGetIncomingByFilter() throws Exception {
    populateData();
    populateRelationshipIncommingData();

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();

    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    List<Identity> identities = relationshipStorage.getIncomingByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be " + identities.size(), 8, identities.size());

    pf.setPosition("developer");
    pf.setName("FirstName6");
    identities = relationshipStorage.getIncomingByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be " + identities.size(), 1, identities.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getOutgoingByFilter(providerId, Identity, ProfileFilter)}
   *
   * @throws Exception
   * @since 1.2.3
   */
  public void testGetOutgoingByFilter() throws Exception {
    populateData();
    populateRelationshipData(Relationship.Type.PENDING);

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();

    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    List<Identity> identities = relationshipStorage.getOutgoingByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be 8", 8, identities.size());

    pf.setPosition("developer");
    pf.setName("FirstName8");
    identities = relationshipStorage.getOutgoingByFilter(tearDownIdentityList.get(0), pf, 0, 20);
    assertEquals("Number of identities must be 1", 1, identities.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getIncomingCountByFilter(providerId, Identity, ProfileFilter)}
   *
   * @throws Exception
   * @since 1.2.3
   */
  public void testGetIncomingCountByFilter() throws Exception {
    populateData();
    populateRelationshipIncommingData();

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();

    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    int countIdentities = relationshipStorage.getIncomingCountByFilter(tearDownIdentityList.get(0), pf);
    assertEquals("Number of identities must be 8", 8, countIdentities);

    pf.setPosition("developer");
    pf.setName("FirstName6");
    countIdentities = relationshipStorage.getIncomingCountByFilter(tearDownIdentityList.get(0), pf);
    assertEquals("Number of identities must be 1", 1, countIdentities);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getConnectionsCountByFilter(providerId, Identity, ProfileFilter)}
   *
   * @throws Exception
   * @since 1.2.2
   */
  public void testGetConnectionsCountByFilter() throws Exception {
    populateData();
    populateRelationshipData(Relationship.Type.CONFIRMED);

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();

    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    int countIdentities = relationshipStorage.getConnectionsCountByFilter(tearDownIdentityList.get(0), pf);
    assertEquals("Number of identities must be 8", 8, countIdentities);

    pf.setPosition("developer");
    pf.setName("FirstName6");
    countIdentities = relationshipStorage.getConnectionsCountByFilter(tearDownIdentityList.get(0), pf);
    assertEquals("Number of identities must be 1", 1, countIdentities);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.api.RelationshipStorage#getOutgoingCountByFilter(providerId, Identity, ProfileFilter)}
   *
   * @throws Exception
   * @since 1.2.3
   */
  public void testGetOutgoingCountByFilter() throws Exception {
    populateData();
    populateRelationshipData(Relationship.Type.PENDING);

    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    indexingProcessor.process();
    refreshIndices();

    ProfileFilter pf = new ProfileFilter();
    pf = buildProfileFilterWithExcludeIdentities(pf);
    int countIdentities = relationshipStorage.getOutgoingCountByFilter(tearDownIdentityList.get(0), pf);
    assertEquals("Number of identities must be 8", 8, countIdentities);

    pf.setPosition("developer");
    pf.setName("FirstName8");
    countIdentities = relationshipStorage.getOutgoingCountByFilter(tearDownIdentityList.get(0), pf);
    assertEquals("Number of identities must be 1", 1, countIdentities);
  }

  /**
   * Creates the identity data index in range [0,9]
   */
  private void populateData() {
    String providerId = "organization";
    int total = 10;
    Map<String, String> xp = new HashMap<String, String>();
    List<Map<String, String>> xps = new ArrayList<Map<String, String>>();
    xp.put(Profile.EXPERIENCES_COMPANY, "exo");
    xps.add(xp);
    for (int i = 0; i < total; i++) {
      String remoteId = "username" + i;
      Identity identity = new Identity(providerId, remoteId);
      identityStorage.saveIdentity(identity);

      Profile profile = new Profile(identity);
      profile.setProperty(Profile.FIRST_NAME, "FirstName" + i);
      profile.setProperty(Profile.LAST_NAME, "LastName" + i);
      profile.setProperty(Profile.FULL_NAME, "FirstName" + i + " " +  "LastName" + i);
      profile.setProperty("position", "developer");
      profile.setProperty("gender", "male");
      if (i == 3 || i==4) {
        profile.setProperty(Profile.EXPERIENCES, xps);
      }
      identity.setProfile(profile);
      tearDownIdentityList.add(identity);
      identityStorage.saveProfile(profile);
    }
  }

  /**
   * Builds the ProfileFilter and exclude the Identity.
   * @param filter
   * @return
   */
  private ProfileFilter buildProfileFilterWithExcludeIdentities(ProfileFilter filter) {

    ProfileFilter result = filter;
    if (result == null) {
      result = new ProfileFilter();
    }

    List<Identity> excludeIdentities = new ArrayList<Identity>();
    if (tearDownIdentityList.size() > 1) {
      Identity identity0 = tearDownIdentityList.get(0);
      excludeIdentities.add(identity0);
      result.setExcludedIdentityList(excludeIdentities);
    }

    return result;

  }

  /**
   * Creates the relationship to connect from 0 to [2, 9].
   * @param type
   */
  private void populateRelationshipData(Relationship.Type type) {
    if (tearDownIdentityList.size() > 1) {
      Identity identity0 = tearDownIdentityList.get(0);

      Relationship firstToSecondRelationship = null;
      for (int i = 2; i< tearDownIdentityList.size(); i++) {
        firstToSecondRelationship = new Relationship(identity0, tearDownIdentityList.get(i), type);
        tearDownRelationshipList.add(firstToSecondRelationship);
        relationshipStorage.saveRelationship(firstToSecondRelationship);
      }
    }
  }

  /**
   * Creates the relationship to connect from 0 to [2, 9].
   */
  private void populateRelationshipIncommingData() {
    if (tearDownIdentityList.size() > 1) {
      Identity identity0 = tearDownIdentityList.get(0);

      Relationship firstToSecondRelationship = null;
      for (int i = 2; i< tearDownIdentityList.size(); i++) {
        firstToSecondRelationship = new Relationship(tearDownIdentityList.get(i), identity0, Relationship.Type.PENDING);
        tearDownRelationshipList.add(firstToSecondRelationship);
        relationshipStorage.saveRelationship(firstToSecondRelationship);
      }
    }
  }

  private Space createSpace(String prettyName, String displayName, String description) throws Exception {
    Space space = new Space();
    space.setPrettyName(prettyName);
    displayName = displayName == null ? prettyName : displayName; 
    space.setDisplayName(displayName);
    space.setDescription(description);
    space.setManagers(new String[] { "root" });
    space.setGroupId("/platform/users");
    space.setRegistration(Space.OPEN);
    space.setVisibility(Space.PUBLIC);
    spaceStorage.saveSpace(space, true);
    space = spaceStorage.getAllSpaces().get(0);

    indexingService.index(SpaceIndexingServiceConnector.TYPE, space.getId());
    indexingProcessor.process();
    refreshSpaceIndices();
    return space;
  }

  private void deleteAllSpaceInES() {
    indexingService.unindexAll(SpaceIndexingServiceConnector.TYPE);
    indexingProcessor.process();
  }

  private void refreshSpaceIndices() throws IOException {
    HttpPost request = new HttpPost(urlClient + "/space/_refresh");
    LOG.info("Refreshing ES by calling {}", request.getURI());
    HttpResponse response = client.execute(request);
    assertThat(response.getStatusLine().getStatusCode(), is(200));
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
