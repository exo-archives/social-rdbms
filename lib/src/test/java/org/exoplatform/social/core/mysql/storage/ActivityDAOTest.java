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
package org.exoplatform.social.core.mysql.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.mysql.test.AbstractCoreTest;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class ActivityDAOTest extends AbstractCoreTest {
  private final Log LOG = ExoLogger.getLogger(ActivityDAOTest.class);
  private List<Activity> tearDownActivityList;
  private List<Space> tearDownSpaceList;
  private Identity rootIdentity;
  private Identity johnIdentity;
  private Identity maryIdentity;
  private Identity demoIdentity;
  private Identity ghostIdentity;
  private Identity raulIdentity;
  private Identity jameIdentity;
  private Identity paulIdentity;

  private IdentityManager identityManager;
  private RelationshipManager relationshipManager;
  private SpaceService spaceService;
  
  private ActivityDAO activityDao;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    identityManager = getService(IdentityManager.class);
    relationshipManager = getService(RelationshipManager.class);
    spaceService = getService(SpaceService.class);

    activityDao = getService(ActivityDAO.class);
    //
    tearDownActivityList = new ArrayList<Activity>();
    tearDownSpaceList = new ArrayList<Space>();
    //
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
    ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
    raulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "raul", true);
    jameIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "jame", true);
    paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
  }

  @Override
  public void tearDown() throws Exception {
    for (Activity activity : tearDownActivityList) {
      try {
        activityDao.delete(activity);
      } catch (Exception e) {
        LOG.warn("Can not delete activity with id: " + activity.getId(), e);
        assertFalse(true);
      }
    }

    identityManager.deleteIdentity(rootIdentity);
    identityManager.deleteIdentity(johnIdentity);
    identityManager.deleteIdentity(maryIdentity);
    identityManager.deleteIdentity(demoIdentity);
    identityManager.deleteIdentity(ghostIdentity);
    identityManager.deleteIdentity(jameIdentity);
    identityManager.deleteIdentity(raulIdentity);
    identityManager.deleteIdentity(paulIdentity);

    for (Space space : tearDownSpaceList) {
      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
      if (spaceIdentity != null) {
        identityManager.deleteIdentity(spaceIdentity);
      }
      spaceService.deleteSpace(space);
    }
    //
    // logout
    ConversationState.setCurrent(null);
    super.tearDown();
  }
  
  public void testSaveActivity() throws Exception {
    
    String activityTitle = "activity title";
    String johnIdentityId = johnIdentity.getId();
    Activity activity = createActivity(activityTitle, maryIdentity.getId());
    activity.setLocked(true);
    
    activity.setPosterId(johnIdentityId);
    activity.setOwnerId(johnIdentityId);
    
    activity = activityDao.create(activity);
    
    Activity got = activityDao.find(activity.getId());
    assertNotNull(got);
    assertEquals(activityTitle, got.getTitle());
    assertEquals(johnIdentityId, got.getPosterId());
    assertEquals(johnIdentityId, got.getOwnerId());
  }
  
  private Activity createActivity(String activityTitle, String posterId) {
    Activity activity = new Activity();
    // test for reserving order of map values for i18n activity
    Map<String, String> templateParams = new LinkedHashMap<String, String>();
    templateParams.put("key1", "value 1");
    templateParams.put("key2", "value 2");
    templateParams.put("key3", "value 3");
    activity.setTemplateParams(templateParams);
    activity.setTitle(activityTitle);
    activity.setBody("The body of " + activityTitle);
    activity.setPosterId(posterId);
    activity.setType("DEFAULT_ACTIVITY");

    //
    activity.setHidden(false);
    activity.setLocked(false);
    //
    return activity;
  }

}
