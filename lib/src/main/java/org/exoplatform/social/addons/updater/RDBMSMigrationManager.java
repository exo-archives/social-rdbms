package org.exoplatform.social.addons.updater;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.impl.SettingServiceImpl;

public class RDBMSMigrationManager implements Startable {
  private static final Log LOG = ExoLogger.getLogger(RDBMSMigrationManager.class);
  
  public static final String MIGRATION_SETTING_GLOBAL_KEY = "MIGRATION_SETTING_GLOBAL";

  private Thread migrationThread;
  
  private final CountDownLatch migrater;

  private RelationshipMigrationService relationshipMigration;

  private ActivityMigrationService activityMigration;
  
  private SpaceMigrationService spaceMigration;
  
  private SettingService settingService;

//  public RDBMSMigrationManager(RelationshipMigrationService relationshipMigration,
//                               ActivityMigrationService activityMigration, 
//                               SettingService settingService) {
//    this.relationshipMigration = relationshipMigration;
//    this.activityMigration = activityMigration;
//    this.settingService = settingService;
//    migrater = new CountDownLatch(1);
//    //
//  }
  
  public RDBMSMigrationManager() {
    CommonsUtils.getService(DataInitializer.class);
    migrater = new CountDownLatch(1);
    //
  }
  
  

  /**
   * Gets the relationship service
   * @return
   */
  public RelationshipMigrationService getRelationshipMigration() {
    return relationshipMigration == null ? CommonsUtils.getService(RelationshipMigrationService.class) : relationshipMigration;
  }

  @Override
  public void start() {
    initMigrationSetting();
    Runnable migrateTask = new Runnable() {
      @Override
      public void run() {
        //
        Field field =  null;
        try {
          if (!MigrationContext.isDone()) {
            field =  SessionImpl.class.getDeclaredField("FORCE_USE_GET_NODES_LAZILY");
            if (field != null) {
              field.setAccessible(true);
              field.set(null, true);
            }
            //
            LOG.info("START ASYNC MIGRATION---------------------------------------------------");
            //
            if (!MigrationContext.isDone()) {
              if (!MigrationContext.isConnectionDone()) {
                relationshipMigration = CommonsUtils.getService(RelationshipMigrationService.class);
                relationshipMigration.start();
                updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY, Boolean.TRUE);
              }
              if (!MigrationContext.isDone() && MigrationContext.isConnectionDone() && !MigrationContext.isActivityDone()) {
                activityMigration = CommonsUtils.getService(ActivityMigrationService.class);
                activityMigration.start();
                updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY, Boolean.TRUE);
              }
              if (!MigrationContext.isDone() && MigrationContext.isConnectionDone() && MigrationContext.isActivityDone() 
                  && !MigrationContext.isSpaceDone()) {
                spaceMigration = CommonsUtils.getService(SpaceMigrationService.class);
                spaceMigration.start();
                updateSettingValue(MigrationContext.SOC_RDBMS_SPACE_MIGRATION_KEY, Boolean.TRUE);
              }
            }

            // cleanup Connections
            if (!MigrationContext.isDone() && MigrationContext.isConnectionDone() && !MigrationContext.isConnectionCleanupDone()) {
              try {
                relationshipMigration.doRemove();
                updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY, Boolean.TRUE);
              } catch(RuntimeException e) {
                LOG.error("Failed to relationship cleanup", e);
                if (!MigrationContext.isConnectionCleanupDone()) {
                  LOG.info("Retry to relationship cleanup.");
                  relationshipMigration.doRemove();
                  updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY, Boolean.TRUE);
                }
              }
            }

            // cleanup activities
            if (!MigrationContext.isDone() && MigrationContext.isActivityDone() && !MigrationContext.isActivityCleanupDone()) {
              activityMigration.doRemove();
              updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY, Boolean.TRUE);
            }
            
            // cleanup spaces
            if (!MigrationContext.isDone() && MigrationContext.isSpaceDone() && !MigrationContext.isSpaceCleanupDone()) {
              spaceMigration.doRemove();
              updateSettingValue(MigrationContext.SOC_RDBMS_SPACE_CLEANUP_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_MIGRATION_STATUS_KEY, Boolean.TRUE);
              MigrationContext.setDone(true);
            }
            
            //
            LOG.info("END ASYNC MIGRATION-----------------------------------------------------");
          }

        } catch (Exception e) {
          LOG.error("Failed to running Migration data from JCR to RDBMS", e);
        } finally {
          if (field != null) {
            try {
              field.set(null, false);
            } catch (IllegalArgumentException e) {
              LOG.warn(e.getMessage(), e);
            } catch (IllegalAccessException e) {
              LOG.warn(e.getMessage(), e);
            }
          }
          migrater.countDown();
        }
      }
    };
    this.migrationThread = new Thread(migrateTask);
    this.migrationThread.setPriority(Thread.NORM_PRIORITY);
    this.migrationThread.setName("SOC-MIGRATION-RDBMS");
    this.migrationThread.start();
  }
  
  private void initMigrationSetting() {
    settingService = CommonsUtils.getService(SettingService.class);
    MigrationContext.setDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_MIGRATION_STATUS_KEY));
    //
    MigrationContext.setConnectionDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY));
    MigrationContext.setConnectionCleanupDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY));
    //
    MigrationContext.setActivityDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY));
    MigrationContext.setActivityCleanupDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY));
  }

  private boolean getOrCreateSettingValue(String key) {
    try {
      SettingValue<?> migrationValue =  settingService.get(Context.GLOBAL, Scope.GLOBAL.id(MIGRATION_SETTING_GLOBAL_KEY), key);
      if (migrationValue != null) {
        return Boolean.parseBoolean(migrationValue.getValue().toString());
      } else {
        updateSettingValue(key, Boolean.FALSE);
        return false;
      }
    } finally {
      Scope.GLOBAL.id(null);
    }
  }

  private void updateSettingValue(String key, Boolean status) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.set(Context.GLOBAL, Scope.GLOBAL.id(MIGRATION_SETTING_GLOBAL_KEY), key, SettingValue.create(status));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.GLOBAL.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  @Override
  public void stop() {
    relationshipMigration.stop();
    activityMigration.stop();
    try {
      this.migrationThread.join();
    } catch (InterruptedException e) {
      LOG.error(e);
    }
  }

  public CountDownLatch getMigrater() {
    return migrater;
  }
  
  
}