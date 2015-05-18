/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http:www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.mysql.storage.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.addons.storage.dao.jpa.ActivityDao;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.mysql.test.AbstractCoreTest;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Unit Test for {@link activityDao}, including cache tests.
 * @author tu-vu-duy
 */
public class ActivityDAOTest extends AbstractCoreTest {
  private final Log LOG = ExoLogger.getLogger(ActivityDAOTest.class);
  private List<Activity> tearDownActivityList;
  private List<Space> tearDownSpaceList;
  private Identity ghostIdentity;
  private Identity raulIdentity;
  private Identity jameIdentity;
  private Identity paulIdentity;

  private ActivityDao activityDao;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    activityDao = getService(ActivityDao.class);
    //
    tearDownActivityList = new ArrayList<Activity>();
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
        //TODO refactoring
        //activityDao.deleteActivity(activity.getId());
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
    activityDao.close();
    // logout
    ConversationState.setCurrent(null);
    super.tearDown();
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

  /**
   * Test {@link activityDao#saveActivity(Identity, Activity)}
   * 
   * @throws Exception
   */
  public void testActivity() throws Exception {
    String activityTitle = "activity title";
    String johnIdentityId = johnIdentity.getId();
    Activity activity = createActivity(activityTitle, maryIdentity.getId());
    activity.setLocked(true);
    // test for reserving order of map values for i18n activity

    activityDao.saveActivity(johnIdentity, activity);
    tearDownActivityList.add(activity);

    //TODO
    //activity = activityDao.getActivity(activity.getId());
    //
    assertNotNull(activity);
    assertEquals(activityTitle, activity.getTitle());
    assertEquals(johnIdentityId, activity.getOwnerId());
    Map<String, String> gotTemplateParams = activity.getTemplateParams();
    for (int i = 1; i < 4; i++) {
      assertTrue(gotTemplateParams.values().contains("value " + 1));
    }
    //
    assertTrue(activity.getLocked());
    assertFalse(activity.getHidden());
  }

  /**
   * Test {@link activityDao#getActivity(getUserActivities)}
   * 
   * @throws ActivityStorageException
   */
  public void testGetActivitiiesByUser() throws ActivityStorageException {
    List<Activity> rootActivities = activityDao.getUserActivities(rootIdentity);
    assertEquals(0, rootActivities.size());

    String activityTitle = "title";
    String userId = rootIdentity.getId();
    Activity activity = createActivity(activityTitle, userId);

    activityDao.saveActivity(rootIdentity, activity);

    //TODO Refactoring
    //activity = activityDao.getActivity(activity.getId());
    assertNotNull(activity);
    assertEquals(activityTitle, activity.getTitle());
    assertEquals(userId, activity.getOwnerId());

    rootActivities = activityDao.getUserActivities(rootIdentity);
    assertEquals(1, rootActivities.size());
    LOG.info("Create 100 activities...");
    //
    for (int i = 0; i < 100; i++) {
      activityDao.saveActivity(rootIdentity, createActivity(activityTitle + " " + i, userId));
    }
    //
    LOG.info("Loadding 20 activities...");
    rootActivities = activityDao.getUserActivities(rootIdentity, 0, 20);
    assertEquals(20, rootActivities.size());
    //
    LOG.info("Loadding 101 activities...");
    rootActivities = activityDao.getUserActivities(rootIdentity);
    assertEquals(101, rootActivities.size());
    //
    tearDownActivityList.addAll(rootActivities);
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
    activityDao.saveActivity(johnIdentity, activity);
    tearDownActivityList.add(activity);
    //TODO 
    //activity = activityDao.getActivity(activity.getId());
    assertEquals(activityTitle, activity.getTitle());
    assertEquals(userId, activity.getOwnerId());

    String newTitle = "new activity title";
    activity.setTitle(newTitle);
    activityDao.updateActivity(activity);

    //TODO Refactoring
    //activity = activityDao.getActivity(activity.getId());
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
    activityDao.saveActivity(johnIdentity, activity);
    //TODO Refactoring
    //activity = activityDao.getActivity(activity.getId());
    
    assertNotNull(activity);
    assertEquals(activityTitle, activity.getTitle());
    assertEquals(userId, activity.getOwnerId());
    
    //TODO Refactoring
    //activityDao.deleteActivity(activity.getId());
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
    activityDao.saveActivity(johnIdentity, activity);
    tearDownActivityList.add(activity);
    
    String commentTitle = "Comment title";
    
    //demo comments on john's activity
    Comment comment = new Comment();
    comment.setTitle(commentTitle);
    comment.setOwnerId(demoIdentity.getId());
    activityDao.saveComment(activity, comment);
    //
    //TODO Refactoring
    //activity = activityDao.getActivity(activity.getId());
    
    List<Comment> demoComments = activityDao.getComments(activity);
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
    activityDao.saveActivity(johnIdentity, demoActivity);
    tearDownActivityList.add(demoActivity);
    // comment
    Comment comment = new Comment();
    comment.setTitle("demo comment");
    comment.setOwnerId(demoIdentity.getId());
    //
    //TODO Refactoring
    //demoActivity = activityDao.getActivity(demoActivity.getId());
    //
    activityDao.saveComment(demoActivity, comment);
    //TODO Refactoring
    //demoActivity = activityDao.getActivity(demoActivity.getId());
    List<Comment> demoComments = demoActivity.getComments();
    
    Long commentId = demoComments.get(0).getId();
    //
    comment = activityDao.getComment(commentId);
    Activity gotActivity = activityDao.getActivityByComment(comment);
    assertNotNull(gotActivity);
    assertEquals(demoActivity.getId(), gotActivity.getId());
    assertEquals(1, gotActivity.getComments().size());
    assertEquals(comment, gotActivity.getComments().get(0));
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
    //TODO Refactoring
    //demoActivity.setOwnerId(demoActivity.getId());
    activityDao.saveActivity(demoIdentity, demoActivity);
    tearDownActivityList.add(demoActivity);
    
    int total = 10;
    
    for (int i = 0; i < total; i ++) {
      Comment maryComment = new Comment();
      maryComment.setOwnerId(maryIdentity.getId());
      maryComment.setTitle("mary comment");
      activityDao.saveComment(demoActivity, maryComment);
    }
    List<Comment> maryComments = activityDao.getComments(demoActivity);
    assertNotNull(maryComments);
    assertEquals(total, maryComments.size());
  }

  
  /**
   * Test {@link activityDao#deleteComment(Activity, Activity)}
   * 
   * @throws Exception
   * @since 1.2.0-Beta3
   */
  public void testDeleteComment() throws Exception {
    Activity demoActivity = new Activity();
    demoActivity.setTitle("demo activity");
    //TODO Refactoring
    //demoActivity.setOwnerId(demoActivity.getId());
    activityDao.saveActivity(demoIdentity, demoActivity);
    tearDownActivityList.add(demoActivity);
    
    Comment maryComment = new Comment();
    maryComment.setTitle("mary comment");
    maryComment.setOwnerId(maryIdentity.getId());
    activityDao.saveComment(demoActivity, maryComment);
    //
    maryComment = activityDao.getComment(maryComment.getId());
    activityDao.deleteComment(maryComment);
    
    assertEquals(0, activityDao.getComments(demoActivity).size());
  }
//  
//  /**
//   * Test {@link activityDao#saveLike(Activity, Identity)}
//   * 
//   * @throws Exception
//   * @since 1.2.0-Beta3s
//   */
//  public void testSaveLike() throws Exception {
//    Activity demoActivity = new Activity();
//    demoActivity.setTitle("&\"demo activity");
//    demoActivity.setOwnerId(demoActivity.getId());
//    activityDao.saveActivity(demoIdentity, demoActivity);
//    tearDownActivityList.add(demoActivity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds() must return: 0",
//                 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.saveLike(demoActivity, johnIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 1", 1, demoActivity.getLikeIdentityIds().length);
//    assertEquals("&\"demo activity", demoActivity.getTitle());
//  }
//  /**
//   * Test {@link activityDao#saveLike(Activity, Identity)}
//   *  for case not change the template param after liked.
//   * 
//   * @throws Exception
//   * @since 4.0.5
//   */  
//  public void testSaveLikeNotChangeTemplateParam() throws Exception {
//    Activity demoActivity = new Activity();
//    demoActivity.setTitle("title");
//    demoActivity.setOwnerId(demoActivity.getId());
//    
//    Map<String, String> templateParams = new HashMap<String, String>();
//    templateParams.put("link", "http://exoplatform.com?test=<script>");
//    demoActivity.setTemplateParams(templateParams);
//    
//    
//    activityDao.saveActivity(demoIdentity, demoActivity);
//    tearDownActivityList.add(demoActivity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds() must return: 0",
//                 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.saveLike(demoActivity, johnIdentity);
//    
//    Activity likedActivity = activityDao.getActivity(demoActivity.getId());
//    
//    assertEquals(1, likedActivity.getLikeIdentityIds().length);
//    assertEquals(templateParams.get("link"), likedActivity.getTemplateParams().get("link"));
//  }
//  
//  /**
//   * Test {@link activityDao#deleteLike(Activity, Identity)}
//   * 
//   * @throws Exception
//   * @since 1.2.0-Beta3
//   */
//  public void testDeleteLike() throws Exception {
//    Activity demoActivity = new Activity();
//    demoActivity.setTitle("demo activity");
//    demoActivity.setOwnerId(demoActivity.getId());
//    activityDao.saveActivity(demoIdentity, demoActivity);
//    tearDownActivityList.add(demoActivity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds() must return: 0",
//                 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.saveLike(demoActivity, johnIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 1", 1, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.deleteLike(demoActivity, johnIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 0", 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.deleteLike(demoActivity, maryIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 0", 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.deleteLike(demoActivity, rootIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 0", 0, demoActivity.getLikeIdentityIds().length);
//  }
//  
//  /**
//   * Test {@link activityDao#deleteLike(Activity, Identity)}
//   *  for case not change the template param after liked.
//   * 
//   * @throws Exception
//   * @since 4.0.5
//   */  
//  public void testDeleteLikeNotChangeTemplateParam() throws Exception {
//    Activity demoActivity = new Activity();
//    demoActivity.setTitle("title");
//    demoActivity.setOwnerId(demoActivity.getId());
//    
//    Map<String, String> templateParams = new HashMap<String, String>();
//    templateParams.put("link", "http://exoplatform.com?test=<script>");
//    demoActivity.setTemplateParams(templateParams);
//    
//    
//    activityDao.saveActivity(demoIdentity, demoActivity);
//    tearDownActivityList.add(demoActivity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    activityDao.saveLike(demoActivity, johnIdentity);
//    Activity likedActivity = activityDao.getActivity(demoActivity.getId());
//    
//    assertEquals(1, likedActivity.getLikeIdentityIds().length);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    activityDao.deleteLike(demoActivity, johnIdentity);
//    Activity deleteLikeActivity = activityDao.getActivity(demoActivity.getId());
//    
//    assertEquals(0, deleteLikeActivity.getLikeIdentityIds().length);
//    assertEquals(templateParams.get("link"), deleteLikeActivity.getTemplateParams().get("link"));
//  }  
//  /**
//   * Test {@link activityDao#getActivitiesWithListAccess(Identity)}
//   * 
//   * @throws Exception
//   * @since 1.2.0-Beta3
//   */
//  public void testGetActivitiesWithListAccess() throws Exception {
//    int total = 10;
//    for (int i = 0; i < total; i ++) {
//      Activity demoActivity = new Activity();
//      demoActivity.setTitle("demo activity");
//      demoActivity.setOwnerId(demoActivity.getId());
//      activityDao.saveActivity(demoIdentity, demoActivity);
//      tearDownActivityList.add(demoActivity);
//    }
//
//    RealtimeListAccess<Activity> demoListAccess = activityDao.getActivitiesWithListAccess(demoIdentity);
//    assertNotNull("demoListAccess must not be null", demoListAccess);
//    assertEquals("demoListAccess.getSize() must return: 10", 10, demoListAccess.getSize());
//  }
//  
//  
//  /**
//   * Test {@link activityDao#getActivitiesOfConnectionsWithListAccess(Identity)}
//   * 
//   * @throws Exception
//   * @since 1.2.0-Beta3
//   */
//  
//  /**
//  public void testGetActivitiesOfConnectionsWithListAccess() throws Exception {
//    Activity baseActivity = null;
//    for (int i = 0; i < 10; i ++) {
//      Activity activity = new Activity();
//      activity.setTitle("activity title " + i);
//      activity.setOwnerId(johnIdentity.getId());
//      activityDao.saveActivity(johnIdentity, activity);
//      tearDownActivityList.add(activity);
//      if (i == 5) {
//        baseActivity = activity;
//      }
//    }
//    
//    RealtimeListAccess<Activity> demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity);
//    assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
//    assertEquals("demoConnectionActivities.getSize() must return: 0", 0, demoConnectionActivities.getSize());
//    
//    Relationship demoJohnRelationship = relationshipManager.invite(demoIdentity, johnIdentity);
//    relationshipManager.confirm(demoJohnRelationship);
//    
//    demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity);
//    assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
//    assertEquals("demoConnectionActivities.getSize() must return: 10", 10, demoConnectionActivities.getSize());
//    assertEquals("demoConnectionActivities.getNumberOfNewer(baseActivity)", 4,
//                 demoConnectionActivities.getNumberOfNewer(baseActivity));
//    assertEquals("demoConnectionActivities.getNumberOfOlder(baseActivity) must return: 5", 5,
//                 demoConnectionActivities.getNumberOfOlder(baseActivity));
//    
//    for (int i = 0; i < 10; i ++) {
//      Activity activity = new Activity();
//      activity.setTitle("activity title " + i);
//      activity.setOwnerId(maryIdentity.getId());
//      activityDao.saveActivity(maryIdentity, activity);
//      tearDownActivityList.add(activity);
//      if (i == 5) {
//        baseActivity = activity;
//      }
//    }
//    
//    Relationship demoMaryRelationship = relationshipManager.invite(demoIdentity, maryIdentity);
//    relationshipManager.confirm(demoMaryRelationship);
//    
//    demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity);
//    assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
//    assertEquals("demoConnectionActivities.getSize() must return: 20", 20, demoConnectionActivities.getSize());
//    assertEquals("demoConnectionActivities.getNumberOfNewer(baseActivity)", 4,
//                 demoConnectionActivities.getNumberOfNewer(baseActivity));
//    assertEquals("demoConnectionActivities.getNumberOfOlder(baseActivity) must return: 15", 15,
//                 demoConnectionActivities.getNumberOfOlder(baseActivity));
//    
//    relationshipManager.remove(demoJohnRelationship);
//    relationshipManager.remove(demoMaryRelationship);
//  }
//  
//  **/
//  
//  /**
//   * Test {@link activityDao#getActivitiesOfUserSpacesWithListAccess(Identity)}
//   * 
//   * @throws Exception
//   * @since 1.2.0-Beta3s
//   */
//  
//  
//  public void testGetActivitiesOfUserSpacesWithListAccess() throws Exception {
//    Space space = this.getSpaceInstance(spaceService, 0);
//    tearDownSpaceList.add(space);
//    Identity spaceIdentity = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
//    
//    int totalNumber = 10;
//    
//    //demo posts activities to space
//    for (int i = 0; i < totalNumber; i ++) {
//      Activity activity = new Activity();
//      activity.setTitle("activity title " + i);
//      activity.setOwnerId(demoIdentity.getId());
//      activityDao.saveActivity(spaceIdentity, activity);
//      tearDownActivityList.add(activity);
//    }
//    
//    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
//    assertNotNull("space must not be null", space);
//    assertEquals("space.getDisplayName() must return: my space 0", "my space 0", space.getDisplayName());
//    assertEquals("space.getDescription() must return: add new space 0", "add new space 0", space.getDescription());
//    
//    RealtimeListAccess<Activity> demoActivities = activityDao.getActivitiesOfUserSpacesWithListAccess(demoIdentity);
//    assertNotNull("demoActivities must not be null", demoActivities);
//    assertEquals("demoActivities.getSize() must return: 10", 10, demoActivities.getSize());
//    
//    Space space2 = this.getSpaceInstance(spaceService, 1);
//    tearDownSpaceList.add(space2);
//    Identity spaceIdentity2 = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space2.getPrettyName(), false);
//    
//    //demo posts activities to space2
//    for (int i = 0; i < totalNumber; i ++) {
//      Activity activity = new Activity();
//      activity.setTitle("activity title " + i);
//      activity.setOwnerId(demoIdentity.getId());
//      activityDao.saveActivity(spaceIdentity2, activity);
//      tearDownActivityList.add(activity);
//    }
//    
//    space2 = spaceService.getSpaceByDisplayName(space2.getDisplayName());
//    assertNotNull("space2 must not be null", space2);
//    assertEquals("space2.getDisplayName() must return: my space 1", "my space 1", space2.getDisplayName());
//    assertEquals("space2.getDescription() must return: add new space 1", "add new space 1", space2.getDescription());
//    
//    demoActivities = activityDao.getActivitiesOfUserSpacesWithListAccess(demoIdentity);
//    assertNotNull("demoActivities must not be null", demoActivities);
//    assertEquals("demoActivities.getSize() must return: 20", 20, demoActivities.getSize());
//    
//    demoActivities = activityDao.getActivitiesOfUserSpacesWithListAccess(maryIdentity);
//    assertNotNull("demoActivities must not be null", demoActivities);
//    assertEquals("demoActivities.getSize() must return: 0", 0, demoActivities.getSize());
//    
//  }
//  
//  public void testGetActivityFeedWithListAccess() throws Exception {
//    this.populateActivityMass(demoIdentity, 3);
//    this.populateActivityMass(maryIdentity, 3);
//    this.populateActivityMass(johnIdentity, 2);
//    
//    Space space = this.getSpaceInstance(spaceService, 0);
//    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
//    populateActivityMass(spaceIdentity, 5);
//
//    RealtimeListAccess<Activity> demoActivityFeed = activityDao.getActivityFeedWithListAccess(demoIdentity);
//    assertEquals("demoActivityFeed.getSize() must be 8", 8, demoActivityFeed.getSize());
//    assertEquals(8, demoActivityFeed.load(0, 10).length);
//    
//    Relationship demoMaryConnection = relationshipManager.inviteToConnect(demoIdentity, maryIdentity);
//    assertEquals(8, activityDao.getActivityFeedWithListAccess(demoIdentity).getSize());
//
//    relationshipManager.confirm(demoIdentity, maryIdentity);
//    demoActivityFeed = activityDao.getActivityFeedWithListAccess(demoIdentity);
//    assertEquals("demoActivityFeed.getSize() must return 11", 11, demoActivityFeed.getSize());
//    assertEquals(11, demoActivityFeed.load(0, 15).length);
//    assertEquals(6, demoActivityFeed.load(5, 15).length);
//    
//    RealtimeListAccess<Activity> maryActivityFeed = activityDao.getActivityFeedWithListAccess(maryIdentity);
//    assertEquals("maryActivityFeed.getSize() must return 6", 6, maryActivityFeed.getSize());
//    assertEquals(6, maryActivityFeed.load(0, 10).length);
//    
//    // Create demo's activity on space
//    createActivityToOtherIdentity(demoIdentity, spaceIdentity, 5);
//
//    // after that the feed of demo with have 16
//    demoActivityFeed = activityDao.getActivityFeedWithListAccess(demoIdentity);
//    assertEquals(16, demoActivityFeed.getSize());
//
//    // demo's Space feed must be be 5
//    RealtimeListAccess<Activity> demoActivitiesSpaceFeed = activityDao.getActivitiesOfUserSpacesWithListAccess(demoIdentity);
//    assertEquals(10, demoActivitiesSpaceFeed.getSize());
//    assertEquals(10, demoActivitiesSpaceFeed.load(0, 10).length);
//
//    // the feed of mary must be the same because mary not the member of space
//    maryActivityFeed = activityDao.getActivityFeedWithListAccess(maryIdentity);
//    assertEquals(6, maryActivityFeed.getSize());
//
//    // john not friend of demo but member of space
//    RealtimeListAccess<Activity> johnSpaceActivitiesFeed = activityDao.getActivitiesOfUserSpacesWithListAccess(johnIdentity);
//    assertEquals("johnSpaceActivitiesFeed.getSize() must return 10", 10, johnSpaceActivitiesFeed.getSize());
//
//    relationshipManager.delete(demoMaryConnection);
//    spaceService.deleteSpace(space);
//  }
//  
//  public void testLoadMoreActivities() throws Exception {
//    this.populateActivityMass(demoIdentity, 30);
//    RealtimeListAccess<Activity> demoActivityFeed = activityDao.getActivityFeedWithListAccess(demoIdentity);
//    assertEquals(30, demoActivityFeed.getSize());
//    assertEquals(10, demoActivityFeed.load(0, 10).length);
//    assertEquals(20, demoActivityFeed.load(0, 20).length);
//    assertEquals(10, demoActivityFeed.load(20, 10).length);
//    assertEquals(15, demoActivityFeed.load(15, 20).length);
//  }
//  
//  /**
//   * Test {@link activityDao#getComments(Activity)}
//   * 
//   * @throws ActivityStorageException
//   */
//  public  void testGetCommentWithHtmlContent() throws ActivityStorageException {
//    String htmlString = "<span><strong>foo</strong>bar<script>zed</script></span>";
//    String htmlRemovedString = "<span><strong>foo</strong>bar&lt;script&gt;zed&lt;/script&gt;</span>";
//    
//    Activity activity = new Activity();
//    activity.setTitle("blah blah");
//    activityDao.saveActivity(rootIdentity, activity);
//
//    Activity comment = new Activity();
//    comment.setTitle(htmlString);
//    comment.setOwnerId(rootIdentity.getId());
//    comment.setBody(htmlString);
//    activityDao.saveComment(activity, comment);
//    assertNotNull("comment.getId() must not be null", comment.getId());
//
//    List<Activity> comments = activityDao.getComments(activity);
//    assertEquals(1, comments.size());
//    assertEquals(htmlString, comments.get(0).getBody());
//    assertEquals(htmlString, comments.get(0).getTitle());
//    tearDownActivityList.add(activity);    
//  }
//  
//  /**
//   * 
//   * 
//   * @throws ActivityStorageException
//   */
//  public  void testGetComment() throws ActivityStorageException {
//    Activity activity = new Activity();;
//    activity.setTitle("blah blah");
//    activityDao.saveActivity(rootIdentity, activity);
//
//    Activity comment = new Activity();;
//    comment.setTitle("comment blah blah");
//    comment.setOwnerId(rootIdentity.getId());
//
//    activityDao.saveComment(activity, comment);
//
//    assertNotNull("comment.getId() must not be null", comment.getId());
//
//    activity = activityDao.getActivity(activity.getId());
//    String[] commentsId = activity.getReplyToId();
//    assertEquals(comment.getId(), commentsId[0]);
//    tearDownActivityList.add(activity);
//  }
//
//  /**
//   * 
//   * 
//   * @throws ActivityStorageException
//   */
//  public  void testGetComments() throws ActivityStorageException {
//    Activity activity = new Activity();;
//    activity.setTitle("blah blah");
//    activityDao.saveActivity(rootIdentity, activity);
//
//    List<Activity> comments = new ArrayList<Activity>();
//    for (int i = 0; i < 3; i++) {
//      Activity comment = new Activity();;
//      comment.setTitle("comment " + i);
//      comment.setOwnerId(rootIdentity.getId());
//      activityDao.saveComment(activity, comment);
//      assertNotNull("comment.getId() must not be null", comment.getId());
//
//      comments.add(comment);
//    }
//
//    RealtimeListAccess<Activity> listAccess = activityDao.getCommentsWithListAccess(activity);
//    assertEquals(3, listAccess.getSize());
//    List<Activity> listComments = listAccess.loadAsList(0, 5);
//    assertEquals(3, listComments.size());
//    assertEquals("comment 0", listComments.get(0).getTitle());
//    assertEquals("comment 1", listComments.get(1).getTitle());
//    assertEquals("comment 2", listComments.get(2).getTitle());
//    
//    
//    Activity assertActivity = activityDao.getActivity(activity.getId());
//    String[] commentIds = assertActivity.getReplyToId();
//    for (int i = 1; i < commentIds.length; i++) {
//      assertEquals(comments.get(i - 1).getId(), commentIds[i - 1]);
//    }
//    tearDownActivityList.add(activity);
//  }
//  /**
//   * Unit Test for:
//   * <p>
//   * {@link activityDao#deleteComment(String, String)}
//   * 
//   * @throws ActivityStorageException
//   */
//  public void testDeleteCommentWithId() throws ActivityStorageException {
//    final String title = "Activity Title";
//    {
//      //FIXBUG: SOC-1194
//      //Case: a user create an activity in his stream, then give some comments on it.
//      //Delete comments and check
//      Activity activity1 = new Activity();;
//      activity1.setOwnerId(demoIdentity.getId());
//      activity1.setTitle(title);
//      activityDao.saveActivity(demoIdentity, activity1);
//
//      final int numberOfComments = 10;
//      final String commentTitle = "Activity Comment";
//      for (int i = 0; i < numberOfComments; i++) {
//        Activity comment = new Activity();;
//        comment.setOwnerId(demoIdentity.getId());
//        comment.setTitle(commentTitle + i);
//        activityDao.saveComment(activity1, comment);
//      }
//
//      List<Activity> storedCommentList = activityDao.getComments(activity1);
//
//      assertEquals("storedCommentList.size() must return: " + numberOfComments, numberOfComments, storedCommentList.size());
//
//      //delete random 2 comments
//      int index1 = new Random().nextInt(numberOfComments - 1);
//      int index2 = index1;
//      while (index2 == index1) {
//        index2 = new Random().nextInt(numberOfComments - 1);
//      }
//
//      Activity tobeDeletedComment1 = storedCommentList.get(index1);
//      Activity tobeDeletedComment2 = storedCommentList.get(index2);
//
//      activityDao.deleteComment(activity1.getId(), tobeDeletedComment1.getId());
//      activityDao.deleteComment(activity1.getId(), tobeDeletedComment2.getId());
//
//      List<Activity> afterDeletedCommentList = activityDao.getComments(activity1);
//
//      assertEquals("afterDeletedCommentList.size() must return: " + (numberOfComments - 2), numberOfComments - 2, afterDeletedCommentList.size());
//
//
//      tearDownActivityList.add(activity1);
//
//    }
//  }
//
// /**
//  * Unit Test for:
//  * {@link activityDao#getActivities(Identity)}
//  * {@link activityDao#getActivities(Identity, long, long)}
//  * 
//  * @throws ActivityStorageException
//  */
// public void testGetActivities() throws ActivityStorageException {
//   List<Activity> rootActivityList = activityDao.getActivities(rootIdentity);
//   assertNotNull("rootActivityList must not be null", rootActivityList);
//   assertEquals(0, rootActivityList.size());
//
//   populateActivityMass(rootIdentity, 30);
//   List<Activity> activities = activityDao.getActivities(rootIdentity);
//   assertNotNull("activities must not be null", activities);
//   assertEquals(20, activities.size());
//
//   List<Activity> allActivities = activityDao.getActivities(rootIdentity, 0, 30);
//
//   assertEquals(30, allActivities.size());
// }
//
// /**
//  * Unit Test for:
//  * <p>
//  * {@link activityDao#getActivitiesOfConnections(Identity)}
//  * 
//  * @throws Exception
//  */
// public void testGetActivitiesOfConnections() throws Exception {
//   this.populateActivityMass(johnIdentity, 10);
//   
//   List<Activity> demoConnectionActivities = activityDao.getActivitiesOfConnections(demoIdentity);
//   assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
//   assertEquals("demoConnectionActivities.size() must return: 0", 0, demoConnectionActivities.size());
//   
//   Relationship demoJohnRelationship = relationshipManager.invite(demoIdentity, johnIdentity);
//   relationshipManager.confirm(demoJohnRelationship);
//   
//   demoConnectionActivities = activityDao.getActivitiesOfConnections(demoIdentity);
//   assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
//   assertEquals("demoConnectionActivities.size() must return: 10", 10, demoConnectionActivities.size());
//   
//   this.populateActivityMass(maryIdentity, 10);
//   
//   Relationship demoMaryRelationship = relationshipManager.invite(demoIdentity, maryIdentity);
//   relationshipManager.confirm(demoMaryRelationship);
//   
//   demoConnectionActivities = activityDao.getActivitiesOfConnections(demoIdentity);
//   assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
//   assertEquals("demoConnectionActivities.size() must return: 20", 20, demoConnectionActivities.size());
//   
//   relationshipManager.remove(demoJohnRelationship);
//   relationshipManager.remove(demoMaryRelationship);
// }
// 
// public void testGetActivitiesOfConnectionswithOffsetLimit() throws Exception {
//   this.populateActivityMass(johnIdentity, 10);
//   
//   RealtimeListAccess<Activity> demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity); 
//   assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities.load(0, 20));
//   assertEquals("demoConnectionActivities.size() must return: 0", 0, demoConnectionActivities.getSize());
//   
//   Relationship demoJohnRelationship = relationshipManager.inviteToConnect(demoIdentity, johnIdentity);
//   relationshipManager.confirm(demoIdentity, johnIdentity);
//   
//   demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity);
//   assertEquals(10, demoConnectionActivities.load(0, 10).length);
//   
//   demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity);
//   assertEquals("demoConnectionActivities.size() must return: 10", 10, demoConnectionActivities.getSize());
//   
//   this.populateActivityMass(maryIdentity, 10);
//   
//   Relationship demoMaryRelationship = relationshipManager.inviteToConnect(demoIdentity, maryIdentity);
//   relationshipManager.confirm(demoIdentity, maryIdentity);
//   
//   demoConnectionActivities = activityDao.getActivitiesOfConnectionsWithListAccess(demoIdentity);
//   assertEquals(20, demoConnectionActivities.load(0, 20).length);
//   assertEquals("demoConnectionActivities.size() must return: 20", 20, demoConnectionActivities.getSize());
//   
//   relationshipManager.delete(demoJohnRelationship);
//   relationshipManager.delete(demoMaryRelationship);
// }
//
// /**
//  * Unit Test for:
//  * <p>
//  * {@link activityDao#getActivitiesOfUserSpaces(Identity)}
//  * 
//  * @throws Exception
//  */
// public void testGetActivitiesOfUserSpaces() throws Exception {
//   Space space = this.getSpaceInstance(spaceService, 0);
//   Identity spaceIdentity = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
//   
//   int totalNumber = 10;
//   
//   this.populateActivityMass(spaceIdentity, totalNumber);
//   
//   List<Activity> demoActivities = activityDao.getActivitiesOfUserSpaces(demoIdentity);
//   assertNotNull("demoActivities must not be null", demoActivities);
//   assertEquals("demoActivities.size() must return: 10", 10, demoActivities.size());
//   
//   Space space2 = this.getSpaceInstance(spaceService, 1);
//   Identity spaceIdentity2 = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space2.getPrettyName(), false);
//   
//   this.populateActivityMass(spaceIdentity2, totalNumber);
//   
//   demoActivities = activityDao.getActivitiesOfUserSpaces(demoIdentity);
//   assertNotNull("demoActivities must not be null", demoActivities);
//   assertEquals("demoActivities.size() must return: 20", 20, demoActivities.size());
//   
//   demoActivities = activityDao.getActivitiesOfUserSpaces(maryIdentity);
//   assertNotNull("demoActivities must not be null", demoActivities);
//   assertEquals("demoActivities.size() must return: 0", 0, demoActivities.size());
//   
//   spaceService.deleteSpace(space);
//   spaceService.deleteSpace(space2);
// }
//
//  /**
//   * Test {@link activityDao#getActivities(Identity, long, long)}
//   * 
//   * @throws ActivityStorageException
//   */
//  public void testGetActivitiesByPagingWithoutCreatingComments() throws ActivityStorageException {
//    final int totalActivityCount = 9;
//    final int retrievedCount = 7;
//
//    this.populateActivityMass(johnIdentity, totalActivityCount);
//
//    List<Activity> activities = activityDao.getActivities(johnIdentity, 0, retrievedCount);
//    assertEquals(retrievedCount, activities.size());
//  }
//
//  public void testRemoveLike() throws Exception {
//    Activity demoActivity = new Activity();
//    demoActivity.setTitle("demo activity");
//    demoActivity.setOwnerId(demoActivity.getId());
//    activityDao.saveActivity(demoIdentity, demoActivity);
//    tearDownActivityList.add(demoActivity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//
//    assertEquals("demoActivity.getLikeIdentityIds() must return: 0",
//                 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.saveLike(demoActivity, johnIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 1", 1, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.removeLike(demoActivity, johnIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 0", 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.removeLike(demoActivity, maryIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 0", 0, demoActivity.getLikeIdentityIds().length);
//    
//    activityDao.removeLike(demoActivity, rootIdentity);
//    
//    demoActivity = activityDao.getActivity(demoActivity.getId());
//    assertEquals("demoActivity.getLikeIdentityIds().length must return: 0", 0, demoActivity.getLikeIdentityIds().length);
//  }
//  
//  /**
//   * Test {@link activityDao#getActivitiesCount(Identity)}
//   * 
//   * @throws Exception
//   * @since 1.2.0-Beta3
//   */
//  public void testGetActivitiesCount() throws Exception {
//    int count = activityDao.getActivitiesCount(rootIdentity);
//    assertEquals("count must be: 0", 0, count);
//
//    populateActivityMass(rootIdentity, 30);
//    count = activityDao.getActivitiesCount(rootIdentity);
//    assertEquals("count must be: 30", 30, count);
//  }
  
  public void testGetLastIdenties() throws Exception {
    List<Identity> lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    Identity id1 = lastIds.get(0);
    lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    assertEquals(id1, lastIds.get(0));
    lastIds = identityManager.getLastIdentities(5);
    assertEquals(5, lastIds.size());
    assertEquals(id1, lastIds.get(0));
    OrganizationService os = (OrganizationService) getContainer().getComponentInstanceOfType(OrganizationService.class);
    User user1 = os.getUserHandler().createUserInstance("newId1");
    os.getUserHandler().createUser(user1, false);
    Identity newId1 = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "newId1", false);
    lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    assertEquals(newId1, lastIds.get(0));
    identityManager.deleteIdentity(newId1);
    assertNull(identityManager.getIdentity(newId1.getId(), false));
    lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    assertEquals(id1, lastIds.get(0));
    User user2 = os.getUserHandler().createUserInstance("newId2");
    os.getUserHandler().createUser(user2, false);
    Identity newId2 = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "newId2", true);
    lastIds = identityManager.getLastIdentities(5);
    assertEquals(5, lastIds.size());
    assertEquals(newId2, lastIds.get(0));
    identityManager.deleteIdentity(newId2);
    assertNull(identityManager.getIdentity(newId2.getId(), true));
    lastIds = identityManager.getLastIdentities(5);
    assertEquals(5, lastIds.size());
    assertEquals(id1, lastIds.get(0));
    newId1 = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "newId1", false);
    newId2 = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "newId2", true);
    lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    assertEquals(newId2, lastIds.get(0));
    lastIds = identityManager.getLastIdentities(2);
    assertEquals(2, lastIds.size());
    assertEquals(newId2, lastIds.get(0));
    assertEquals(newId1, lastIds.get(1));
    identityManager.deleteIdentity(newId1);
    os.getUserHandler().removeUser("newId1", false);
    assertNull(identityManager.getIdentity(newId1.getId(), true));
    lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    assertEquals(newId2, lastIds.get(0));
    lastIds = identityManager.getLastIdentities(2);
    assertEquals(2, lastIds.size());
    assertEquals(newId2, lastIds.get(0));
    assertFalse(newId1.equals(lastIds.get(1)));
    identityManager.deleteIdentity(newId2);
    os.getUserHandler().removeUser("newId2", false);
    assertNull(identityManager.getIdentity(newId2.getId(), false));
    lastIds = identityManager.getLastIdentities(1);
    assertEquals(1, lastIds.size());
    assertEquals(id1, lastIds.get(0));
  }

  /**
   *
   */
  /*public void testAddProviders() {
    activityDao.addProcessor(new FakeProcessor(10));
    activityDao.addProcessor(new FakeProcessor(9));
    activityDao.addProcessor(new FakeProcessor(8));

    Activity activity = new Activity();
    activity.setTitle("Hello");
    activityDao.processActivitiy(activity);
    //just verify that we run in priority order
    assertEquals("Hello-8-9-10", activity.getTitle());
  }


  class FakeProcessor extends BaseActivityProcessorPlugin {
    public FakeProcessor(int priority) {
      super(null);
      super.priority = priority;
    }

    @Override
    public void processActivity(Activity activity) {
      activity.setTitle(activity.getTitle() + "-" + priority);
    }
  }
*/
  
  /**
   * Populates activity.
   * 
   * @param user
   * @param number
   */
  private void populateActivityMass(Identity user, int number) {
    for (int i = 0; i < number; i++) {
      Activity activity = new Activity();;
      activity.setTitle("title " + i);
      activity.setOwnerId(user.getId());
      try {
        activityDao.saveActivity(user, activity);
        tearDownActivityList.add(activity);
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }
  
  private void createActivityToOtherIdentity(Identity posterIdentity,
      Identity targetIdentity, int number) {

    // if(!relationshipManager.get(posterIdentity,
    // targetIdentity).getStatus().equals(Type.CONFIRMED)){
    // return;
    // }

    for (int i = 0; i < number; i++) {
      Activity activity = new Activity();

      activity.setTitle("title " + i);
      activity.setOwnerId(posterIdentity.getId());
      try {
        activityDao.saveActivity(targetIdentity, activity);
        tearDownActivityList.add(activity);
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }
  
  /**
   * Gets an instance of the space.
   * 
   * @param spaceService
   * @param number
   * @return
   * @throws Exception
   * @since 1.2.0-GA
   */
  private Space getSpaceInstance(SpaceService spaceService, int number)
      throws Exception {
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.OPEN);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/" + space.getPrettyName());
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] { "demo", "john" };
    String[] members = new String[] { "raul", "ghost", "demo", "john" };
    String[] invitedUsers = new String[] { "mary", "paul"};
    String[] pendingUsers = new String[] { "jame"};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    spaceService.saveSpace(space, true);
    return space;
  }
}