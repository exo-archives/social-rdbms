package org.exoplatform.social.addons.updater;

import java.util.concurrent.CountDownLatch;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class RDBMSMigrationManager implements Startable {
  private static final Log LOG = ExoLogger.getLogger(RDBMSMigrationManager.class);

  private Thread migrationThread;
  
  private final CountDownLatch migrater;

  private final RelationshipMigrationService relationshipMigration;

  private final ProfileMigrationService profileMigration;

  private final ActivityMigrationService activityMigration;

  public RDBMSMigrationManager(ProfileMigrationService profileMigration,
                               RelationshipMigrationService relationshipMigration,
                               ActivityMigrationService activityMigration) {
    this.profileMigration = profileMigration;
    this.relationshipMigration = relationshipMigration;
    this.activityMigration = activityMigration;
    migrater = new CountDownLatch(1);
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
        } finally {
          migrater.countDown();
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

  public CountDownLatch getMigrater() {
    return migrater;
  }
}
