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

import java.util.List;

import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMember;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.addons.test.BaseCoreTest;

public class SpaceDAOTest extends BaseCoreTest {
  private SpaceDAO spaceDAO;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    spaceDAO = getService(SpaceDAO.class);
  }

  @Override
  public void tearDown() throws Exception {
    spaceDAO.deleteAll();
    super.tearDown();
  }

  public void testSaveSpace() throws Exception {
    SpaceEntity spaceEntity = createSpace();

    spaceDAO.create(spaceEntity);

    end();
    begin();

    SpaceEntity result = spaceDAO.find(spaceEntity.getId());
    assertSpace(spaceEntity, result);
  }

  public void testGetSpace() throws Exception {
    SpaceEntity spaceEntity = createSpace();

    spaceDAO.create(spaceEntity);

    end();
    begin();

    SpaceEntity result = spaceDAO.getSpaceByDisplayName(spaceEntity.getDisplayName());
    assertSpace(spaceEntity, result);
    
    result = spaceDAO.getSpaceByGroupId(spaceEntity.getGroupId());
    assertSpace(spaceEntity, result);
    
    result = spaceDAO.getSpaceByPrettyName(spaceEntity.getPrettyName());
    assertSpace(spaceEntity, result);
    
    result = spaceDAO.getSpaceByURL(spaceEntity.getUrl());
    assertSpace(spaceEntity, result);
  }
  
  public void testGetLastSpace() throws Exception {
    SpaceEntity space1 = createSpace();
    spaceDAO.create(space1);
    SpaceEntity space2 = createSpace();
    spaceDAO.create(space2);

    end();
    begin();
    
    List<SpaceEntity> result = spaceDAO.getLastSpaces(1);
    assertEquals(1, result.size());
    assertSpace(space2, result.iterator().next());
  }

  private SpaceEntity createSpace() {
    SpaceEntity spaceEntity = new SpaceEntity();
    spaceEntity.setApp("testApp");
    spaceEntity.setAvatarLastUpdated(1L);
    spaceEntity.setDescription("testDesc");
    spaceEntity.setDisplayName("testDisplayName");
    spaceEntity.setGroupId("testGroupId");
    spaceEntity.setPrettyName("testPrettyName");
    spaceEntity.setPriority("hight");
    spaceEntity.setRegistration("testRegistration");
    spaceEntity.setUrl("testUrl");
    spaceEntity.setVisibility("testVisibility");
    spaceEntity.setAvatarLastUpdated(1L);

    SpaceMember mem = new SpaceMember();
    mem.setSpace(spaceEntity);
    mem.setStatus(Status.PENDING);
    mem.setUserId("root");
    spaceEntity.getMembers().add(mem);
    return spaceEntity;
  }

  private void assertSpace(SpaceEntity spaceEntity, SpaceEntity result) {
    assertNotNull(result);
    assertEquals(spaceEntity.getPrettyName(), result.getPrettyName());
    assertEquals(spaceEntity.getApp(), result.getApp());
    assertEquals(spaceEntity.getDescription(), result.getDescription());
    assertEquals(spaceEntity.getDisplayName(), result.getDisplayName());
    assertEquals(spaceEntity.getGroupId(), result.getGroupId());
    assertEquals(spaceEntity.getPriority(), result.getPriority());
    assertEquals(spaceEntity.getRegistration(), result.getRegistration());
    assertEquals(spaceEntity.getUrl(), result.getUrl());
    assertEquals(spaceEntity.getUrl(), result.getUrl());
    assertEquals(spaceEntity.getVisibility(), result.getVisibility());
    assertEquals(spaceEntity.getAvatarLastUpdated(), result.getAvatarLastUpdated());
    assertEquals(spaceEntity.getCreatedTime(), result.getCreatedTime());
    assertEquals(1, result.getMembers().size());
  }
}
