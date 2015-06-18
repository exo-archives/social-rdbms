package org.exoplatform.social.addons.updater;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.updater.listeners.RDBMSMigrationListener;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProviderEntity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.AbstractStorage;

import com.google.common.collect.Maps;

public abstract class AbstractMigrationService<T>  extends AbstractStorage {
  protected Log LOG;
  protected final IdentityStorage identityStorage;
  protected boolean forkStop = false;
  protected boolean isDone = false;
  protected Map<String, RDBMSMigrationListener<T>> listeners = Maps.newConcurrentMap();

  public AbstractMigrationService(IdentityStorage identityStorage) {
    super();
    this.identityStorage = identityStorage;
    LOG = ExoLogger.getLogger(this.getClass().getName());
  }

  public void addMigrationListener(RDBMSMigrationListener<T> listener) {
    listeners.put(listener.getName(), listener);
  }

  protected void broadcastListener(T t, String newId) {
    for (RDBMSMigrationListener<T> listener : listeners.values()) {
      try {
        listener.doMigration(t, newId);
      } catch (Exception e) {
        LOG.error("Failed to do migration for " + listener.getName());
      }
    }
  }

   public void start() {
    forkStop = false;
    //
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      boolean begunTx = GenericDAOImpl.startTx();
      try {
        beforeMigration();
        //
        doMigration();
        //
        afterMigration();
      } catch (Exception e) {
        LOG.error("Failed to run migration data from JCR to Mysql.", e);
      } finally {
        GenericDAOImpl.endTx(begunTx);
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void stop() {
    forkStop = true;
  }

  public boolean isDone() {
    return isDone;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, IdentityEntity> getAllIdentityEntity() {
    ProviderEntity providerEntity;
    try {
      providerEntity = getProviderRoot().getProviders().get(OrganizationIdentityProvider.NAME);
    } catch (Exception ex) {
      lifeCycle.getProviderRoot().set(null);
      providerEntity = getProviderRoot().getProviders().get(OrganizationIdentityProvider.NAME);
    }
    return (providerEntity != null) ? providerEntity.getIdentities() : new HashMap<String, IdentityEntity>();
  }

  protected abstract void beforeMigration() throws Exception;
  public abstract void doMigration() throws Exception;
  protected abstract void afterMigration() throws Exception;
}
