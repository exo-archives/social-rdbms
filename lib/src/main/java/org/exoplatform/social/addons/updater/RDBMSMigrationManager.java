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

  private IdentityMigrationService identityMigration;
  
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

  public IdentityMigrationService getIdentityMigrationService() {
    if (identityMigration == null) {
      identityMigration = CommonsUtils.getService(IdentityMigrationService.class);
    }
    return identityMigration;
  }

  public SpaceMigrationService getSpaceMigrationService() {
    if (spaceMigration == null) {
      spaceMigration = CommonsUtils.getService(SpaceMigrationService.class);
    }
    return spaceMigration;
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

            try {
              getRelationshipMigration().getProviderRoot();
            } catch (Exception ex) {
              LOG.debug("no JCR data, stopping JCR to RDBMS migration");

              // Update and mark that migrate was done
              updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_SPACE_MIGRATION_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_IDENTITY_MIGRATION_KEY, Boolean.TRUE);

              updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_CLEANUP_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_SPACE_CLEANUP_KEY, Boolean.TRUE);
              updateSettingValue(MigrationContext.SOC_RDBMS_IDENTITY_CLEANUP_KEY, Boolean.TRUE);

              updateSettingValue(MigrationContext.SOC_RDBMS_MIGRATION_STATUS_KEY, Boolean.TRUE);
              MigrationContext.setDone(true);

              return;
            }


            field =  SessionImpl.class.getDeclaredField("FORCE_USE_GET_NODES_LAZILY");
            if (field != null) {
              field.setAccessible(true);
              field.set(null, true);
            }
            //
            LOG.info("START ASYNC MIGRATION---------------------------------------------------");
            //
            if (!MigrationContext.isDone()) {
              if (!MigrationContext.isDone() && !MigrationContext.isActivityDone()) {
                getActivityMigrationService().start();
                updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_MIGRATION_KEY, MigrationContext.isActivityDone());
              }
              if (!MigrationContext.isDone() && MigrationContext.isActivityDone()
                  && !MigrationContext.isSpaceDone()) {
                getSpaceMigrationService().start();
                updateSettingValue(MigrationContext.SOC_RDBMS_SPACE_MIGRATION_KEY, MigrationContext.isSpaceDone());
              }

              // Migrate identities
              if (!MigrationContext.isDone()
                      && MigrationContext.isActivityDone() && MigrationContext.isSpaceDone()
                      && !MigrationContext.isIdentityDone()) {
                getIdentityMigrationService().start();
                updateSettingValue(MigrationContext.SOC_RDBMS_IDENTITY_MIGRATION_KEY, MigrationContext.isIdentityDone());
              }

              if (!MigrationContext.isDone() && MigrationContext.isIdentityDone() && !MigrationContext.isConnectionDone()) {
                relationshipMigration = CommonsUtils.getService(RelationshipMigrationService.class);
                relationshipMigration.start();
                updateSettingValue(MigrationContext.SOC_RDBMS_CONNECTION_MIGRATION_KEY, MigrationContext.isConnectionDone());
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
              getActivityMigrationService().doRemove();
              updateSettingValue(MigrationContext.SOC_RDBMS_ACTIVITY_CLEANUP_KEY, Boolean.TRUE);
            }
            
            // cleanup spaces
            if (!MigrationContext.isDone() && MigrationContext.isSpaceDone() && !MigrationContext.isSpaceCleanupDone()) {
              getSpaceMigrationService().doRemove();
              updateSettingValue(MigrationContext.SOC_RDBMS_SPACE_CLEANUP_KEY, Boolean.TRUE);
            }

            // Cleanup identity
            if (!MigrationContext.isDone() && MigrationContext.isIdentityDone() && !MigrationContext.isIdentityCleanupDone()) {
              getIdentityMigrationService().doRemove();
              updateSettingValue(MigrationContext.SOC_RDBMS_IDENTITY_CLEANUP_KEY, Boolean.TRUE);
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

    MigrationContext.setSpaceDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_SPACE_MIGRATION_KEY));
    MigrationContext.setSpaceCleanupDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_SPACE_CLEANUP_KEY));

    MigrationContext.setIdentityDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_IDENTITY_MIGRATION_KEY));
    MigrationContext.setIdentityCleanupDone(getOrCreateSettingValue(MigrationContext.SOC_RDBMS_IDENTITY_CLEANUP_KEY));
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

  private ActivityMigrationService getActivityMigrationService() {
    if (activityMigration == null) {
      activityMigration = CommonsUtils.getService(ActivityMigrationService.class);
    }
    return activityMigration;
  }

  @Override
  public void stop() {
    relationshipMigration.stop();
    getActivityMigrationService().stop();
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