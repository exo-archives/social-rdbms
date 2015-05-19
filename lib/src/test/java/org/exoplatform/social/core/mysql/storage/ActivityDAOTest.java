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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;
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
  private Set<Activity> tearDownActivityList;
  private List<Space> tearDownSpaceList;
  private Identity ghostIdentity;
  private Identity raulIdentity;
  private Identity jameIdentity;
  private Identity paulIdentity;

  private IdentityManager identityManager;
  private RelationshipManager relationshipManager;
  private SpaceService spaceService;
  
  private ActivityDAO activityDao;
  private CommentDAO commentDao;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    identityManager = getService(IdentityManager.class);
    relationshipManager = getService(RelationshipManager.class);
    spaceService = getService(SpaceService.class);

    activityDao = getService(ActivityDAO.class);
    commentDao = getService(CommentDAO.class);
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
    //
    Map<String, String> gotTemplateParams = activity.getTemplateParams();
    for (int i = 1; i < 4; i++) {
      assertTrue(gotTemplateParams.values().contains("value " + 1));
    }
    //
    assertTrue(activity.getLocked());
    assertFalse(activity.getHidden());
    tearDownActivityList.add(got);
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
  
  private Activity saveActivity(Identity ownerIdentity, Activity activity) {
    activity.setOwnerId(ownerIdentity.getId());
    activity = activityDao.create(activity);
    tearDownActivityList.add(activity);
    //
    return activity;
  }
  /**
   * Test {@link activityDao#updateActivity(Activity)}
   * 
   * @throws Exception
   */
  public void testUpdateActivity() throws Exception {
    String activityTitle = "activity title";
    String userId = johnIdentity.getId();
    Activity activity = createActivity(activityTitle, userId);
    saveActivity(johnIdentity, activity);
    //
    activity = activityDao.find(activity.getId());
    assertEquals(activityTitle, activity.getTitle());
    assertEquals(userId, activity.getOwnerId());

    String newTitle = "new activity title";
    activity.setTitle(newTitle);
    activityDao.update(activity);

    activity = activityDao.find(activity.getId());
    assertEquals(newTitle, activity.getTitle());
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
    activityDao.delete(activity.getId());
    //
    assertNull(activityDao.find(activity.getId()));
  }

  /**
   * Test {@link activityDao#saveComment(Activity, Activity)}
   * 
   * @throws Exception
   * @since 1.2.0-Beta3
   */
  public void testSaveComment() throws Exception {
    String activityTitle = "activity title";
    String userId = johnIdentity.getId();
    Activity activity = new Activity();
    activity.setTitle(activityTitle);
    activity.setOwnerId(userId);
    saveActivity(johnIdentity, activity);
    String commentTitle = "Comment title";
    
    //demo comments on john's activity
    Comment comment = new Comment();
    comment.setTitle(commentTitle);
    comment.setOwnerId(demoIdentity.getId());
    commentDao.create(comment);
    
    activity.addComment(comment);
    assertNotNull(comment.getId());
    activityDao.update(activity);
    //
    activity = activityDao.find(activity.getId());
    
    List<Comment> demoComments = commentDao.getComments(activity, 0, -1);
    assertNotNull(demoComments);
    assertEquals(1, demoComments.size());
    
    comment = demoComments.get(0);
    assertEquals(commentTitle, comment.getTitle());
    assertEquals(demoIdentity.getId(), comment.getOwnerId());
  }

  /**
   * Tests {@link activityDao#getActivityByComment(Activity)}.
   */
  public void testGetActivityByComment() {
    String activityTitle = "activity title";
    String identityId = johnIdentity.getId();
    Activity demoActivity = new Activity();
    demoActivity.setTitle(activityTitle);
    demoActivity.setOwnerId(identityId);
    saveActivity(johnIdentity, demoActivity);
    // comment
    Comment comment = new Comment();
    comment.setTitle("demo comment");
    comment.setOwnerId(demoIdentity.getId());
    //
    demoActivity = activityDao.find(demoActivity.getId());
    //
    commentDao.create(comment);
    demoActivity.addComment(comment);
    activityDao.update(demoActivity);
    //
    demoActivity = activityDao.find(demoActivity.getId());
    List<Comment> demoComments = demoActivity.getComments();
    Long commentId = demoComments.get(0).getId();
    //
    comment = commentDao.find(commentId);
    assertEquals(1, demoActivity.getComments().size());
    assertEquals(demoIdentity.getId(), comment.getOwnerId());
  }
  
  /**
   * Test {@link activityDao#getComments(Activity)}
   * 
   * @throws Exception
   */
  public void testGetComments() throws Exception {
    Activity demoActivity = new Activity();
    demoActivity.setTitle("demo activity");
    demoActivity.setOwnerId(demoIdentity.getId());
    saveActivity(johnIdentity, demoActivity);
    tearDownActivityList.add(demoActivity);
    
    int total = 10;
    
    for (int i = 0; i < total; i ++) {
      Comment maryComment = new Comment();
      maryComment.setOwnerId(maryIdentity.getId());
      maryComment.setTitle("mary comment");
      commentDao.create(maryComment);
      demoActivity.addComment(maryComment);
      activityDao.update(demoActivity);
    }
    
    demoActivity = activityDao.find(demoActivity.getId());
    List<Comment> maryComments = demoActivity.getComments();
    assertNotNull(maryComments);
    assertEquals(total, maryComments.size());
  }

  
  /**
   * Test {@link activityDao#deleteComment(Activity, Activity)}
   * 
   * @throws Exception
   * @since 4.3.x
   */
  public void testDeleteComment() throws Exception {
    Activity demoActivity = new Activity();
    demoActivity.setTitle("demo activity");
    demoActivity.setOwnerId(demoIdentity.getId());
    saveActivity(demoIdentity, demoActivity);
    tearDownActivityList.add(demoActivity);
    
    Comment maryComment = new Comment();
    maryComment.setTitle("mary comment");
    maryComment.setOwnerId(maryIdentity.getId());
    commentDao.create(maryComment);
    demoActivity.addComment(maryComment);
    activityDao.update(demoActivity);
    //
    maryComment = commentDao.find(maryComment.getId());
    demoActivity = activityDao.find(demoActivity.getId());
    demoActivity.getComments().remove(maryComment);
    activityDao.update(demoActivity);
    
    assertEquals(0, demoActivity.getComments().size());
  }

}
