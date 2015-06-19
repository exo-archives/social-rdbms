package org.exoplatform.social.addons.updater;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class RDBMSMigrationManager implements Startable {
  private static final Log LOG = ExoLogger.getLogger(RDBMSMigrationManager.class);

  private Thread migrationThread;

  private final RelationshipMigrationService relationshipMigration;

  private final ProfileMigrationService profileMigration;

  private final ActivityMigrationService activityMigration;

  public RDBMSMigrationManager(ProfileMigrationService profileMigration,
                               RelationshipMigrationService relationshipMigration,
                               ActivityMigrationService activityMigration) {
    this.profileMigration = profileMigration;
    this.relationshipMigration = relationshipMigration;
    this.activityMigration = activityMigration;
    //
  }

  @Override
  public void start() {
    Runnable migrateTask = new Runnable() {
      @Override
      public void run() {
        LOG.info("START ASYNC MIGRATION---------------------------------------------------");
        try {
          profileMigration.start();
          //
          if (profileMigration.isDone()) {
            relationshipMigration.start();
            if (relationshipMigration.isDone()) {
              activityMigration.start();
              if(activityMigration.isDone()) {
                relationshipMigration.doRemove();
                activityMigration.doRemove();
              }
            }
          }
        } catch (Exception e) {
          LOG.error("Failed to running Migration data from JCR to RDBMS", e);
        }
        LOG.info("END ASYNC MIGRATION---------------------------------------------------");
      }
    };
    this.migrationThread = new Thread(migrateTask);
    this.migrationThread.setPriority(Thread.NORM_PRIORITY);
    this.migrationThread.setName("SOC-Migration-RDBMS");
    this.migrationThread.start();
  }

  @Override
  public void stop() {
    profileMigration.stop();
    relationshipMigration.stop();
    activityMigration.stop();
    try {
      this.migrationThread.join();
    } catch (InterruptedException e) {
      LOG.error(e);
    }
  }

  /**
   * Get the integer value from Parameter
   * @param params
   * @param key
   * @param defaultValue
   * @return
   */
  private int getInteger(InitParams params, String key, int defaultValue) {
    try {
      return Integer.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

}
