package org.exoplatform.social.addons.updater;

import java.util.Iterator;

import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.profile.ProfileUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.IdentityStorage;

@Managed
@ManagedDescription("Social migration profiles from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-profiles") })
public class ProfileMigrationService extends AbstractMigrationService<Profile> {
  private static final int LIMIT_THRESHOLD = 200;
  private static final String EVENT_LISTENER_KEY = "SOC_PROFILE_MIGRATION";
  private final ProfileItemDAO profileDAO;
  private final IdentityManager identityManager;
  
  public ProfileMigrationService(ProfileItemDAO profileDAO,
                                 IdentityManager identityManager,
                                 EventManager<Profile, String> eventManager,
                                 IdentityStorage identityStorage) {
    super(identityStorage, eventManager);
    this.profileDAO = profileDAO;
    this.identityManager = identityManager;
  }

  @Override
  protected void beforeMigration() throws Exception {
    isDone = false;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run miguration data of profiles from JCR to MYSQL.")
  public void doMigration() throws Exception {
    if (profileDAO.count() > 0) {
      isDone = true;
      return;
    }
    LOG.info("Stating to migration profiles from JCR to MYSQL........");
    long t = System.currentTimeMillis();
    int count = 0;
    Iterator<IdentityEntity> allIdentityEntity = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values().iterator();
    EntityManagerService entityManagerService = CommonsUtils.getService(EntityManagerService.class);
    while (allIdentityEntity.hasNext()) {
      if(forkStop) {
        return;
      }
      IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
      LOG.info("Migration profile for user: " + identityEntity.getRemoteId());
      //
      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, identityEntity.getRemoteId(), true);
      //
      ProfileUtils.createOrUpdateProfile(identity.getProfile(), false);
      entityManagerService.getEntityManager().flush();
      ++count;
      if(count % LIMIT_THRESHOLD == 0) {
        entityManagerService.endRequest(null);
        entityManagerService.startRequest(null);
        entityManagerService.getEntityManager().getTransaction().begin();
      }
    }
    LOG.info(String.format("Done to migration %s profiles from JCR to MYSQL on %s(ms)", count, (System.currentTimeMillis() - t)));
  }

  @Override
  protected void afterMigration() throws Exception {
    isDone = true;
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
}
