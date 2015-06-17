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
  
  public RDBMSMigrationManager(ProfileMigrationService profileMigration,
                               RelationshipMigrationService relationshipMigration,
                               ActivityMigrationService activityMigration,
                               InitParams params) {
    //
    this.completionService = new NotificationCompletionService(params);
    this.profileMigration = profileMigration;
    this.relationshipMigration = relationshipMigration;
    this.activityMigration = activityMigration;
  }

  @Override
  public void start() {
    Callable<Boolean> task = new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
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

}
