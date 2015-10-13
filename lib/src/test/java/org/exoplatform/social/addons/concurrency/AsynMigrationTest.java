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
package org.exoplatform.social.addons.concurrency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.test.BaseCoreTest;
import org.exoplatform.social.addons.test.MaxQueryNumber;
import org.exoplatform.social.addons.test.QueryNumberTest;
import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.addons.updater.MigrationContext;
import org.exoplatform.social.addons.updater.RDBMSMigrationManager;
import org.exoplatform.social.addons.updater.RelationshipMigrationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.RelationshipStorageImpl;
import org.jboss.byteman.contrib.bmunit.BMUnit;
import org.junit.FixMethodOrder;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 19, 2015  
 */
@QueryNumberTest
@FixMethodOrder
public class AsynMigrationTest extends BaseCoreTest {
  protected final Log LOG = ExoLogger.getLogger(AsynMigrationTest.class);
  private ActivityStorageImpl jcrStorage;
  private RelationshipStorageImpl relationshipStorageImpl;
  private ActivityMigrationService activityMigration;
  private RelationshipMigrationService relationshipMigration;
  private SettingService settingService;
  private RDBMSMigrationManager rdbmsMigrationManager;
  
  
  @Override
  public void setUp() throws Exception {
    begin();
    // If is query number test, init byteman
    hasByteMan = getClass().isAnnotationPresent(QueryNumberTest.class);
    if (hasByteMan) {
      count = 0;
      maxQuery = 0;
      BMUnit.loadScriptFile(getClass(), "queryBaseCount", "src/test/resources");
    }
    
    identityManager = getService(IdentityManager.class);
    activityManager =  getService(ActivityManager.class);
    activityStorage = getService(ActivityStorage.class);
    relationshipManager = getService(RelationshipManager.class);
    spaceService = getService(SpaceService.class);
    entityManagerService = getService(EntityManagerService.class);
    //
    jcrStorage = getService(ActivityStorageImpl.class);
    relationshipStorageImpl = getService(RelationshipStorageImpl.class);
    activityMigration = getService(ActivityMigrationService.class);
    relationshipMigration = getService(RelationshipMigrationService.class);
    settingService = getService(SettingService.class);
    rdbmsMigrationManager = new RDBMSMigrationManager();
  }

  @Override
  public void tearDown() throws Exception {
    end();
    begin();
    ActivityDAO dao = getService(ActivityDAO.class);
    //
    List<Activity> items = dao.findAll();
    for (Activity item : items) {
      dao.delete(item);
    }
    // Reset value of settings
    updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY, Boolean.FALSE);
    updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY, Boolean.FALSE);

    updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY, Boolean.FALSE);
    updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY, Boolean.FALSE);
    updateSettingValue(MigrationContext.SOC_RDBMS_MIGRATION_STATUS_KEY, Boolean.FALSE);
    Scope.GLOBAL.id(null);
    //
    super.tearDown();
  }

  public void testMigrationEmptyData() throws Exception {
    end();
    begin();
    //
    rdbmsMigrationManager.start();
    //
    rdbmsMigrationManager.getMigrater().await();
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY));
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY));

    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY));
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY));
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_MIGRATION_STATUS_KEY));
  }

  @MaxQueryNumber(26990)
  public void testMigrationActivities() throws Exception {
    // create jcr data
    LOG.info("Create connection for root,john,mary and demo");
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);

    //John invites Demo
    Relationship johnToDemo = new Relationship(johnIdentity, demoIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(johnToDemo);
    
    //John invites Mary
    Relationship johnToMary = new Relationship(johnIdentity, maryIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(johnToMary);
    
    //John invites Root
    Relationship johnToRoot = new Relationship(johnIdentity, rootIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(johnToRoot);
    
    //Root invites Mary
    Relationship rootToMary = new Relationship(rootIdentity, maryIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(rootToMary);
    
    //Demo invites Mary
    Relationship demoToMary = new Relationship(demoIdentity, maryIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(demoToMary);
    
    //Demo invites Root
    Relationship demoToRoot = new Relationship(demoIdentity, rootIdentity, Type.PENDING);
    relationshipStorageImpl.saveRelationship(demoToRoot);
    
    
    //confirmed john and demo
    johnToDemo.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(johnToDemo);
    
    //confirmed john and demo
    johnToMary.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(johnToMary);
    
    //confirmed john and root
    johnToRoot.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(johnToRoot);
    
    //confirmed root and mary
    rootToMary.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(rootToMary);
    
    //confirmed demo and mary
    demoToMary.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(demoToMary);
    
    //confirmed demo and root
    demoToRoot.setStatus(Type.CONFIRMED);
    relationshipStorageImpl.saveRelationship(demoToRoot);
    
    //
    LOG.info("Create the activities storage on JCR ....");
    createActivityToOtherIdentity(rootIdentity, johnIdentity, 5);
    createActivityToOtherIdentity(demoIdentity, maryIdentity, 5);
    createActivityToOtherIdentity(johnIdentity, demoIdentity, 5);
    createActivityToOtherIdentity(maryIdentity, rootIdentity, 5);
    LOG.info("Done created the activities storage on JCR.");
    end();
    begin();
    //
    rdbmsMigrationManager.start();
    //
    rdbmsMigrationManager.getMigrater().await();
    //
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY));
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY));
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_MIGRATION_STATUS_KEY));
    
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY));
    assertTrue(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY));
    //
    assertEquals(20, activityStorage.getActivityFeed(rootIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(maryIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(johnIdentity, 0, 100).size());
    assertEquals(20, activityStorage.getActivityFeed(demoIdentity, 0, 100).size());
  }
  
  private boolean getOrCreateSettingValue(String key) {
    SettingValue<?> migrationValue =  settingService.get(Context.GLOBAL, Scope.GLOBAL.id(RDBMSMigrationManager.MIGRATION_SETTING_GLOBAL_KEY), key);
    if (migrationValue != null) {
      return Boolean.parseBoolean(migrationValue.getValue().toString());
    } else {
      return false;
    }
  }

  private void updateSettingValue(String key, Boolean value) {
    settingService.set(Context.GLOBAL, Scope.GLOBAL.id(RDBMSMigrationManager.MIGRATION_SETTING_GLOBAL_KEY), key, new SettingValue<Boolean>(value));
  }
  
  private void createActivityToOtherIdentity(Identity posterIdentity, Identity targetIdentity, int number) {
    List<ExoSocialActivity> activities = listOf(number, targetIdentity, posterIdentity, false, false);
    for (ExoSocialActivity activity : activities) {
      try {
        activity = jcrStorage.saveActivity(targetIdentity, activity);
        //
        Map<String, String> params = new HashMap<String, String>();
        params.put("MESSAGE",
                   "                                CRaSH is the open source shell for the JVM. The shell can be accessed by various ways, remotely using network protocols such as SSH, locally by attaching a shell to a running virtual machine or via a web interface. Commands are written Groovy and can be developed live making the extensibility of the shell easy with quick development cycles. Since the version 1.3, the REPL also speaks the Groovy language, allowing Groovy combination of command using pipes.  CRaSH comes with commands such as thread management, log management, database access and JMX. The session will begin with an introduction to the shell. The main part of the session will focus on showing CRaSH commands development with few examples, showing how easy and powerful the development is.  The audience will learn how to use CRaSH for their own needs: it can be a simple usage or more advanced like developing a command or embedding the shell in their own runtime like a web application or a Grails application.");
        List<ExoSocialActivity> comments = listOf(3, targetIdentity, posterIdentity, true, false);
        for (ExoSocialActivity comment : comments) {
          comment.setTitle("comment of " + posterIdentity.getId());
          comment.setTemplateParams(params);
          //
          jcrStorage.saveComment(activity, comment);
        }
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }
}