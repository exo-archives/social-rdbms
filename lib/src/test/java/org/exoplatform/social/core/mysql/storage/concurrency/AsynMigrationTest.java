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
package org.exoplatform.social.core.mysql.storage.concurrency;

import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.addons.updater.ProfileMigrationService;
import org.exoplatform.social.addons.updater.RDBMSMigrationManager;
import org.exoplatform.social.addons.updater.RelationshipMigrationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.mysql.test.BaseCoreTest;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 19, 2015  
 */
public class AsynMigrationTest extends BaseCoreTest {
  protected final Log LOG = ExoLogger.getLogger(AsynMigrationTest.class);
  private ActivityStorageImpl jcrStorage;
  private ActivityMigrationService activityMigration;
  private ProfileMigrationService profileMigration;
  private RelationshipMigrationService relationshipMigration;
  private RDBMSMigrationManager rdbmsMigrationManager;
  
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    jcrStorage = getService(ActivityStorageImpl.class);
    activityMigration = getService(ActivityMigrationService.class);
    relationshipMigration = getService(RelationshipMigrationService.class);
    profileMigration = getService(ProfileMigrationService.class);
    rdbmsMigrationManager = new RDBMSMigrationManager(profileMigration, relationshipMigration, activityMigration);
  }

  @Override
  public void tearDown() throws Exception {
    begin();
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
    activityMigration.start();
    begin();
    activityMigration.doRemove();
    GenericDAOImpl.startTx();
    //
    assertEquals(20, activityStorage.getActivityFeed(rootIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(maryIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(johnIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(demoIdentity, 0, 100).size());
  }
  
  private void createActivityToOtherIdentity(Identity posterIdentity, Identity targetIdentity, int number) {
    List<ExoSocialActivity> activities = listOf(number, targetIdentity, posterIdentity, false, false);
    for (ExoSocialActivity activity : activities) {
      try {
        activity = jcrStorage.saveActivity(targetIdentity, activity);
        //
        List<ExoSocialActivity> comments = listOf(3, targetIdentity, posterIdentity, true, false);
        for (ExoSocialActivity comment : comments) {
          comment.setTitle("comment of " + posterIdentity.getId());
          //
          jcrStorage.saveComment(activity, comment);
        }
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }
}