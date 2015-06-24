package org.exoplatform.social.addons.updater;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.profile.ProfileUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.storage.api.IdentityStorage;

@Managed
@ManagedDescription("Social migration profiles from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-profiles") })
public class ProfileMigrationService extends AbstractMigrationService<Profile> {
  public static final String EVENT_LISTENER_KEY = "SOC_PROFILE_MIGRATION";
  private final ProfileItemDAO profileDAO;
  
  public ProfileMigrationService(InitParams initParams,
                                 ProfileItemDAO profileDAO,
                                 IdentityStorage identityStorage,
                                 EventManager<Profile, String> eventManager,
                                 EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.profileDAO = profileDAO;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);

  }

  @Override
  protected void beforeMigration() throws Exception {
    isDone = false;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run miguration data of profiles from JCR to MYSQL.")
  public void doMigration() throws Exception {
    boolean begunTx = GenericDAOImpl.startTx();
    try {
      if (profileDAO.count() > 0) {
        isDone = true;
        return;
      }
      LOG.info("Stating to migration profiles from JCR to MYSQL........");
      long t = System.currentTimeMillis();
      NodeIterator it = getIdentityNodes();
      Identity owner = null; 
      Node node = null;
      long size = it.getSize();
      long offset = 0;
      try {
        while (it.hasNext()) {
          node = (Node) it.next();
          owner = identityStorage.findIdentityById(node.getUUID());
          ProfileUtils.createOrUpdateProfile(owner.getProfile(), false);
          offset++;
          processLog("Profile migration", (int)size, (int)offset);
          //
          if (offset % LIMIT_THRESHOLD == 0) {
            GenericDAOImpl.endTx(begunTx);
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
            begunTx = GenericDAOImpl.startTx();
            it = getIdentityNodes();
            it.skip(offset);
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to migration for Profile.", e);
      }
      LOG.info(String.format("Done to migration %s profiles from JCR to MYSQL on %s(ms)", offset, (System.currentTimeMillis() - t)));
    } finally {
      GenericDAOImpl.endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());

    }
   
  }

  @Override
  protected void afterMigration() throws Exception {
    if(forkStop) {
      return;
    }
    isDone = true;
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
