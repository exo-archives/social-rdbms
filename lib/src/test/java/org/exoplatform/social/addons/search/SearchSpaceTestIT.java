package org.exoplatform.social.addons.search;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import org.exoplatform.addons.es.index.IndexingOperationProcessor;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.addons.test.AbstractCoreTest;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.SpaceStorage;

public class SearchSpaceTestIT extends AbstractCoreTest {
  protected final Log                  LOG    = ExoLogger.getLogger(SearchTestIT.class);

  private IndexingService              indexingService;

  private IndexingOperationProcessor   indexingProcessor;

  private SpaceSearchConnector       searchConnector;

  private String                       urlClient;

  private HttpClient                   client = new DefaultHttpClient();
  
  private SpaceStorage spaceStorage;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    indexingService = getService(IndexingService.class);
    indexingProcessor = getService(IndexingOperationProcessor.class);

    searchConnector = getService(SpaceSearchConnector.class);
    spaceStorage = getService(SpaceStorage.class);
    deleteAllSpaceInES();

    urlClient = PropertyManager.getProperty("exo.es.search.server.url");

    org.exoplatform.services.security.Identity identity = getService(IdentityRegistry.class).getIdentity("root");
    ConversationState.setCurrent(new ConversationState(identity));
  }

  @Override
  protected void tearDown() throws Exception {
    deleteAllSpaceInES();
  }

  public void testSpaceName() throws Exception {
    Space space = new Space();
    space.setPrettyName("testSpaceName abcd efgh");
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setManagers(new String[] {"root"});
    spaceStorage.saveSpace(space, true);
    space = spaceStorage.getAllSpaces().get(0);

    indexingService.index(SpaceIndexingServiceConnector.TYPE, space.getId());
    indexingProcessor.process();
    refreshIndices();

    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setSpaceNameSearchCondition("test");
    assertEquals(1, searchConnector.search(filter, 0, -1).size());
    
    filter.setSpaceNameSearchCondition("name");
    assertEquals(1, searchConnector.search(filter, 0, -1).size());
    
    filter.setSpaceNameSearchCondition("abcd");
    assertEquals(1, searchConnector.search(filter, 0, -1).size());
  }
  
  public void testManager() throws Exception {
    Space space = new Space();
    space.setPrettyName("testManager");
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setManagers(new String[] {"root", "john"});
    space.setPendingUsers(new String[] {"mary", "demo"});
    spaceStorage.saveSpace(space, true);
    space = spaceStorage.getAllSpaces().get(0);

    indexingService.index(SpaceIndexingServiceConnector.TYPE, space.getId());
    indexingProcessor.process();
    refreshIndices();

    ESSpaceFilter filter = new ESSpaceFilter();
    filter.addStatus("root", Status.MANAGER);
    assertEquals(1, searchConnector.search(filter, 0, -1).size());
    
    ESSpaceFilter filter2 = new ESSpaceFilter();
    filter2.addStatus("root", Status.PENDING);
    assertEquals(0, searchConnector.search(filter2, 0, -1).size());
    
    ESSpaceFilter filter3 = new ESSpaceFilter();
    filter3.addStatus("john", Status.MANAGER);
    filter3.addStatus("mary", Status.PENDING);
    filter3.addStatus("demo", Status.PENDING);
    assertEquals(1, searchConnector.search(filter3, 0, -1).size());
  }
  
  public void testVisibility() throws Exception {
    Space space = new Space();
    space.setPrettyName("testVisibility");
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setManagers(new String[] {"root", "john"});
    space.setVisibility(Space.HIDDEN);
    spaceStorage.saveSpace(space, true);
    space = spaceStorage.getAllSpaces().get(0);

    indexingService.index(SpaceIndexingServiceConnector.TYPE, space.getId());
    indexingProcessor.process();
    refreshIndices();

    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setNotHidden(true);
    assertEquals(0, searchConnector.search(filter, 0, -1).size());
    
    ESSpaceFilter filter2 = new ESSpaceFilter();
    filter2.setNotHidden(true);
    assertEquals(0, searchConnector.search(filter2, 0, -1).size());    
  }
  
  public void testVisibility2() throws Exception {
    Space space = new Space();
    space.setPrettyName("testVisibility2");
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setManagers(new String[] {"root", "john"});
    space.setVisibility(Space.PRIVATE);
    spaceStorage.saveSpace(space, true);
    space = spaceStorage.getAllSpaces().get(0);

    indexingService.index(SpaceIndexingServiceConnector.TYPE, space.getId());
    indexingProcessor.process();
    refreshIndices();

    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setSpaceNameSearchCondition("visibility");
    filter.setNotHidden(true);
    assertEquals(1, searchConnector.search(filter, 0, -1).size()); 
  }
  
  public void testPrivate() throws Exception {
    Space space = new Space();
    space.setPrettyName("testPrivate");
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setManagers(new String[] {"root", "john"});
    space.setVisibility(Space.PRIVATE);
    spaceStorage.saveSpace(space, true);
    space = spaceStorage.getAllSpaces().get(0);

    indexingService.index(SpaceIndexingServiceConnector.TYPE, space.getId());
    indexingProcessor.process();
    refreshIndices();

    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setIncludePrivate(true);
    filter.addStatus("demo", Status.MANAGER);
    assertEquals(1, searchConnector.search(filter, 0, -1).size()); 
  }
  
  private void deleteAllSpaceInES() {
    indexingService.unindexAll(SpaceIndexingServiceConnector.TYPE);
    indexingProcessor.process();
  }
  
  private void refreshIndices() throws IOException {
    HttpPost request = new HttpPost(urlClient + "/space/_refresh");
    LOG.info("Refreshing ES by calling {}", request.getURI());
    HttpResponse response = client.execute(request);
    assertThat(response.getStatusLine().getStatusCode(), is(200));
  }
}
