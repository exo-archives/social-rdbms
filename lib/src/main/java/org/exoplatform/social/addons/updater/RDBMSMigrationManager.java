package org.exoplatform.social.addons.updater;

import java.util.concurrent.Callable;

import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class RDBMSMigrationManager implements Startable {
  private static final Log LOG = ExoLogger.getLogger(RDBMSMigrationManager.class);

  private final NotificationCompletionService completionService;
  private final RelationshipMigrationService relationshipMigration;
  private final ProfileMigrationService profileMigration;
  private final ActivityMigrationService activityMigration;
  private final int THREAD_PRIORITY;
  private boolean isAsync = true;
  
  public RDBMSMigrationManager(ProfileMigrationService profileMigration,
                               RelationshipMigrationService relationshipMigration,
                               ActivityMigrationService activityMigration,
                               InitParams params) {
    //
    this.completionService = new NotificationCompletionService(params);
    this.profileMigration = profileMigration;
    this.relationshipMigration = relationshipMigration;
    this.activityMigration = activityMigration;
    //
    THREAD_PRIORITY = getInteger(params, "thread-priority", Thread.NORM_PRIORITY);
    isAsync = getBoolean(params, "async-execution", true);
  }

  @Override
  public void start() {
    Callable<Boolean> task = new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        setUpThread();
        try {
          profileMigration.start();
          //
          if(profileMigration.isDone()) {
            relationshipMigration.start();
            if(relationshipMigration.isDone()) {
              activityMigration.start();
            }
          }
        } catch (Exception e) {
          LOG.error("Failed to running Migration data from JCR to RDBMS", e);
          return false;
        }
        return true;
      }
    };
    //
    completionService.addTask(task);
  }

  @Override
  public void stop() {
    profileMigration.stop();
    relationshipMigration.stop();
    activityMigration.stop();
  }

  private void setUpThread() {
    if (isAsync) {
      Thread t = Thread.currentThread();
      t.setPriority(THREAD_PRIORITY);
      t.setName("Social-migration-JCR-RDBMS");
    }
  }

  private static int getInteger(InitParams params, String key, int defaultValue) {
    try {
      return Integer.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private static boolean getBoolean(InitParams params, String key, boolean defaultValue) {
    try {
      return Boolean.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
