package org.exoplatform.social.addons.updater;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.profile.ProfileUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.updater.utils.MigrationCounter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;

@Managed
@ManagedDescription("Social migration profiles from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-profiles") })
public class ProfileMigrationService extends AbstractMigrationService<Profile> {
  public static final String EVENT_LISTENER_KEY = "SOC_PROFILE_MIGRATION";
  private final ProfileItemDAO profileDAO;
  
  public ProfileMigrationService(InitParams initParams,
                                 ProfileItemDAO profileDAO,
                                 IdentityStorageImpl identityStorage,
                                 EventManager<Profile, String> eventManager,
                                 EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.profileDAO = profileDAO;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);

  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setProfileDone(false);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run miguration data of profiles from JCR to MYSQL.")
  public void doMigration() throws Exception {
    boolean begunTx = startTx();
    MigrationCounter counter = MigrationCounter.builder().threshold(LIMIT_THRESHOLD).build();
    counter.newTotalAndWatch();
    try {
      if (profileDAO.count() > 0) {
        MigrationContext.setProfileDone(true);
        return;
      }
      
      LOG.info("| \\ START::Profile migration ---------------------------------");
      NodeIterator it = getIdentityNodes(counter.getTotal(), LIMIT_THRESHOLD);
      if (it == null) return;
      Identity owner = null; 
      Node node = null;
      
      try {
        while (it.hasNext()) {
          node = (Node) it.next();
          owner = identityStorage.findIdentityById(node.getUUID());
          counter.newBatchAndWatch();
          counter.getAndIncrementTotal();
          LOG.info(String.format("|  \\ START::user number: %s (%s user)", counter.getTotal(), owner.getRemoteId()));
          ProfileUtils.createOrUpdateProfile(owner.getProfile(), false);
          LOG.info(String.format("|  / END::user number %s (%s user) consumed %s(ms)", counter.getTotal(), owner.getRemoteId(), counter.endBatchWatch()));
          
          //
          if (counter.isPersistPoint()) {
            endTx(begunTx);
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
            begunTx = startTx();
            it = getIdentityNodes(counter.getTotal(), LIMIT_THRESHOLD);
          }
          
        }
      } catch (Exception e) {
        LOG.error("Failed to migration for Profile.", e);
      }
    } finally {
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::Profile migration for (%s) user(s) consumed %s(ms)", counter.getTotal(), counter.endTotalWatch()));
    }
  }

  @Override
  protected void afterMigration() throws Exception {
    if(forkStop) {
      return;
    }
    MigrationContext.setProfileDone(true);
    LOG.info("Done to migration profiles from JCR to MYSQL");
  }
  
  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of profiles from JCR to MYSQL.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }

  @Override
  public void doRemove() throws Exception {
  }
}
