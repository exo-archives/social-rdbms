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

//import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
//
//import java.util.Arrays;
//import java.util.List;
//
//import org.elasticsearch.common.settings.ImmutableSettings;
//import org.elasticsearch.node.Node;
//import org.elasticsearch.node.internal.InternalNode;
//import org.elasticsearch.rest.RestController;
//import org.exoplatform.addons.es.domain.OperationType;
//import org.exoplatform.addons.es.index.IndexingService;
//import org.exoplatform.commons.utils.CommonsUtils;
//import org.exoplatform.services.security.ConversationState;
//import org.exoplatform.services.security.IdentityRegistry;
//import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
//import org.exoplatform.social.addons.storage.entity.Connection;
//import org.exoplatform.social.addons.test.AbstractCoreTest;
//import org.exoplatform.social.core.identity.model.Identity;
//import org.exoplatform.social.core.identity.model.Profile;
//import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
//import org.exoplatform.social.core.manager.IdentityManager;
//import org.exoplatform.social.core.manager.RelationshipManager;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 30, 2015  
 */
public class IndexingTest { 
//extends AbstractCoreTest {
//  private RelationshipManager relationshipManager;
//  private IdentityManager identityManager;
//  private Identity ghostIdentity, paulIdentity;
//  
//  private Node node;
//
//  @Override
//  protected void setUp() throws Exception {
//    super.setUp();
//    relationshipManager = getService(RelationshipManager.class);
//    identityManager = getService(IdentityManager.class);
//    assertNotNull("relationshipManager must not be null", relationshipManager);
//    assertNotNull("identityManager must not be null", identityManager);
//    
//    ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
//    paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
//    //
//    org.exoplatform.services.security.Identity identity = getService(IdentityRegistry.class).getIdentity("root");
//    ConversationState.setCurrent(new ConversationState(identity));
//    initEnvironment();
//  }
//  
//  private void initEnvironment() {
//    ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder()
//            .put(RestController.HTTP_JSON_ENABLE, true)
//            .put(InternalNode.HTTP_ENABLED, true)
//            .put("network.host", "127.0.0.1")
//            .put("path.data", "target/data");
//    node = nodeBuilder()
//            .local(true)
//            .settings(elasticsearchSettings.build())
//            .node();
//    node.client().admin().cluster().prepareHealth()
//            .setWaitForYellowStatus().execute().actionGet();
//    assertNotNull(node);
//    assertFalse(node.isClosed());
//  }
//
//  @Override
//  protected void tearDown() throws Exception {
//    ConnectionDAO connectionDAO = getService(ConnectionDAO.class);
//    List<Connection> items = connectionDAO.findAll();
//    for (Connection item : items) {
//      connectionDAO.delete(item);
//    }
//    
//    identityManager.deleteIdentity(ghostIdentity);
//    identityManager.deleteIdentity(paulIdentity);
//    //
//    node.client().admin().indices().prepareDelete("profile").execute().actionGet();
//    node.close();
//    //
//    super.tearDown();
//   }
//  
//  
//  public void testProfileCreateIndexProfile() throws Exception {
//    Identity ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
//    Profile profile = ghostIdentity.getProfile();
//    // update profile will be update on user profile properties also
//    profile.setAttachedActivityType(Profile.AttachedActivityType.USER);
//    profile.setProperty(Profile.POSITION, "developer");
//    profile.setProperty(Profile.FULL_NAME, "Mary Kelly");
//    profile.setProperty(Profile.FIRST_NAME, "Mary");
//    profile.setProperty(Profile.LAST_NAME, "Kelly");
//    profile.setProperty(Profile.POSITION, "Senior developer");
//    profile.setProperty(Profile.EXPERIENCES_SKILLS, "Java, J2EE");
//    profile.setListUpdateTypes(Arrays.asList(Profile.UpdateType.CONTACT));
//    identityManager.updateProfile(profile);
//    
//    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
//    indexingService.addToIndexingQueue("profile", profile.getId(), OperationType.CREATE);
//    // When
//    indexingService.process();
//    node.client().admin().indices().prepareRefresh().execute().actionGet();
//    // Then
//    assertEquals(1L, getNumberOfProfileInIndex("profile"));
//  }
//  
//  private long getNumberOfProfileInIndex(String type) {
//    return node.client().prepareCount(type).execute().actionGet().getCount();
//  }

}