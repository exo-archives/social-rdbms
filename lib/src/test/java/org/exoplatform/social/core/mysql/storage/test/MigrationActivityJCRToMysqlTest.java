package org.exoplatform.social.core.mysql.storage.test;

import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.mysql.test.BaseCoreTest;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;

public class MigrationActivityJCRToMysqlTest extends BaseCoreTest {
  protected final Log LOG = ExoLogger.getLogger(MigrationActivityJCRToMysqlTest.class);
  private ActivityStorageImpl jcrStorage;
  private ActivityMigrationService migrationService;
  @Override
  public void setUp() throws Exception {
    super.setUp();
    jcrStorage = getService(ActivityStorageImpl.class);
    migrationService = getService(ActivityMigrationService.class);
  }

  @Override
  public void tearDown() throws Exception {
    begin();
    GenericDAOImpl.startTx();
    ActivityDAO dao = getService(ActivityDAO.class);
    //
    List<Activity> items = dao.findAll();
    for (Activity item : items) {
      dao.delete(item.getId());
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
    createActivityToOtherIdentity(rootIdentity, johnIdentity, 5);
    createActivityToOtherIdentity(demoIdentity, maryIdentity, 5);
    createActivityToOtherIdentity(johnIdentity, demoIdentity, 5);
    createActivityToOtherIdentity(maryIdentity, rootIdentity, 5);
    LOG.info("Done created the activities storage on JCR.");
    //
    migrationService.start();
    begin();
    GenericDAOImpl.startTx();
    //
    assertEquals(20, activityStorage.getActivityFeed(rootIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(maryIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(johnIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(demoIdentity, 0, 100).size());
  }
  
  private void createActivityToOtherIdentity(Identity posterIdentity, Identity targetIdentity, int number) {
//    List<ExoSocialActivity> activities = listOf(number, targetIdentity, posterIdentity, false, false);
//    for (ExoSocialActivity activity : activities) {
    for (int i = 0; i < number; i++) {
      try {
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        activity.setUserId(posterIdentity.getId());
        activity.setTitle("activity of " + posterIdentity.getId() + " " + i);
        activity.setBody("Body of "+ activity.getTitle());
        activity.setExternalId("External ID");
        activity = jcrStorage.saveActivity(targetIdentity, activity);
        //
//        List<ExoSocialActivity> comments = listOf(number, targetIdentity, posterIdentity, true, false);
        for (int j = 0; j < 5; j++) {
          ExoSocialActivity comment = new ExoSocialActivityImpl();
          comment.setTitle("comment of " + posterIdentity.getId());
          comment.setUserId(targetIdentity.getId());
          //
          jcrStorage.saveComment(activity, comment);
        }
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }
}
