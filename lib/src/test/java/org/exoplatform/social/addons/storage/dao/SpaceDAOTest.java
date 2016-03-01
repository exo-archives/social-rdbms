/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.social.addons.storage.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMember;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.addons.test.BaseCoreTest;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;

public class SpaceDAOTest extends BaseCoreTest {
  private final Log LOG = ExoLogger.getLogger(SpaceDAOTest.class);
  private Set<Activity> tearDownActivityList;
  private List<Space> tearDownSpaceList;
  private Identity ghostIdentity;
  private Identity raulIdentity;
  private Identity jameIdentity;
  private Identity paulIdentity;
  
  private ActivityDAO activityDao;
  private SpaceDAO spaceDAO;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    activityDao = getService(ActivityDAO.class);
    spaceDAO = getService(SpaceDAO.class);
    //
    tearDownActivityList = new HashSet<Activity>();
    tearDownSpaceList = new ArrayList<Space>();
    //
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
      }
    }

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
  
  public void testSaveSpace() throws Exception {
    SpaceEntity spaceEntity = new SpaceEntity();
    spaceEntity.setApp("testApp");
    spaceEntity.setAvatarLastUpdated(1L);
    spaceEntity.setDescription("testDesc");
    spaceEntity.setDisplayName("testDisplayName");
    spaceEntity.setGroupId("testGroupId");
    SpaceMember mem = new SpaceMember();
    mem.setSpace(spaceEntity);
    mem.setStatus(Status.PENDING);
    mem.setUserId("root");
    spaceEntity.getMembers().add(mem);
    spaceEntity.setPrettyName("testPrettyName");
    spaceEntity.setPriority("hight");
    spaceEntity.setRegistration("testRegistration");
    spaceEntity.setUrl("testUrl");
    spaceEntity.setVisibility("testVisibility");
    spaceDAO.create(spaceEntity);

    end();
    begin();    
    
  }
  
  /**
   * Unit Test for:
   * <p>
   * {@link activityDao#deleteActivity(org.exoplatform.social.core.activity.model.Activity)}
   * 
   * @throws Exception
   */
  public void testDeleteActivity() throws Exception {
    String activityTitle = "activity title";
    String userId = johnIdentity.getId();
    Activity activity = new Activity();
    activity.setTitle(activityTitle);
    activity.setOwnerId(userId);
    activity = activityDao.create(activity);
    //
    activity = activityDao.find(activity.getId());
    
    assertNotNull(activity);
    assertEquals(activityTitle, activity.getTitle());
    assertEquals(userId, activity.getOwnerId());
    activityDao.delete(activity);
    //
    assertNull(activityDao.find(activity.getId()));
  }

}
