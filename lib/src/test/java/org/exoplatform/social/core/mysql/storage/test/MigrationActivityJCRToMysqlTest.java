package org.exoplatform.social.core.mysql.storage.test;

import java.util.List;

import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.mysql.test.AbstractCoreTest;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;

import com.google.caja.util.Lists;

public class MigrationActivityJCRToMysqlTest extends AbstractCoreTest {
  private ActivityStorageImpl jcrStorage;
  private ActivityMigrationService migrationService;
  private List<ExoSocialActivity> tearDownActivityList;
  @Override
  public void setUp() throws Exception {
    super.setUp();
    jcrStorage = getService(ActivityStorageImpl.class);
    migrationService = getService(ActivityMigrationService.class);
    tearDownActivityList = Lists.newArrayList();
  }

  @Override
  public void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      try {
        jcrStorage.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }
    super.tearDown();
  }
  
  public void testMigrationActivities() throws Exception {
    // create jcr data
    LOG.info("Create connection for root,john,mary and demo");
    relationshipManager.inviteToConnect(johnIdentity, demoIdentity);
    relationshipManager.inviteToConnect(johnIdentity, maryIdentity);
    relationshipManager.inviteToConnect(johnIdentity, rootIdentity);
    relationshipManager.inviteToConnect(rootIdentity, maryIdentity);
    relationshipManager.inviteToConnect(demoIdentity, maryIdentity);
    relationshipManager.inviteToConnect(demoIdentity, rootIdentity);
    //
    relationshipManager.confirm(demoIdentity, johnIdentity);
    relationshipManager.confirm(maryIdentity, johnIdentity);
    relationshipManager.confirm(rootIdentity, johnIdentity);
    relationshipManager.confirm(maryIdentity, rootIdentity);
    relationshipManager.confirm(maryIdentity, demoIdentity);
    relationshipManager.confirm(rootIdentity, demoIdentity);
    //
    LOG.info("Create the activities storage on JCR ....");
    createActivityToOtherIdentity(rootIdentity, johnIdentity, 20);
    createActivityToOtherIdentity(demoIdentity, maryIdentity, 20);
    createActivityToOtherIdentity(johnIdentity, demoIdentity, 20);
    createActivityToOtherIdentity(maryIdentity, rootIdentity, 20);
    LOG.info("Done created the activities storage on JCR.");
    //
    migrationService.start();
    //
    assertEquals(80, activityStorage.getActivityFeed(rootIdentity, 0, 100).size());
    assertEquals(80, activityStorage.getActivityFeed(maryIdentity, 0, 100).size());
    assertEquals(80, activityStorage.getActivityFeed(johnIdentity, 0, 100).size());
    assertEquals(80, activityStorage.getActivityFeed(demoIdentity, 0, 100).size());
  }
  
  private void createActivityToOtherIdentity(Identity posterIdentity, Identity targetIdentity, int number) {
    for (int i = 0; i < number; i++) {
      try {
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        activity.setUserId(posterIdentity.getId());
        activity.setTitle("activity of " + posterIdentity.getId() + " " + i);
        activity.setBody("Body of "+ activity.getTitle());
        activity.setExternalId("External ID");
        activity = jcrStorage.saveActivity(targetIdentity, activity);
        //
        for (int j = 0; j < 5; j++) {
          ExoSocialActivity comment = new ExoSocialActivityImpl();
          comment.setTitle("comment of " + posterIdentity.getId() + " " + i);
          comment.setUserId(targetIdentity.getId());
          //
          jcrStorage.saveComment(activity, comment);
        }
        tearDownActivityList.add(activity);
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }
}
