package org.exoplatform.social.addons.updater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NodeIterator;

import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProviderEntity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.AbstractStorage;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.query.JCRProperties;

public abstract class AbstractMigrationService<T>  extends AbstractStorage {
  protected Log LOG;
  protected final static String LIMIT_THRESHOLD_KEY = "LIMIT_THRESHOLD";
  protected final IdentityStorage identityStorage;
  protected final EventManager<T, String> eventManager;
  protected final EntityManagerService entityManagerService;

  protected boolean forkStop = false;
  protected boolean isDone = false;
  protected int LIMIT_THRESHOLD = 100;
  protected String process = "";

  public AbstractMigrationService(InitParams initParams,
                                  IdentityStorage identityStorage,
                                  EventManager<T, String> eventManager,
                                  EntityManagerService entityManagerService) {
    super();
    this.identityStorage = identityStorage;
    this.eventManager = eventManager;
    this.entityManagerService = entityManagerService;
    LOG = ExoLogger.getLogger(this.getClass().getName());
  }

  public void addMigrationListener(Listener<T, String> listener) {
    eventManager.addEventListener(getListenerKey(), listener);
  }

  protected void broadcastListener(T t, String newId) {
    List<Listener<T, String>> listeners = eventManager.getEventListeners(getListenerKey());
    for (Listener<T, String> listener : listeners) {
      try {
        Event<T, String> event = new Event<T, String>(getListenerKey(), t, newId);
        listener.onEvent(event);
      } catch (Exception e) {
        LOG.error("Failed to broadcastListener for listener: " + listener.getName(), e);
      }
    }
  }

   public void start() {
    forkStop = false;
    try {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      beforeMigration();
      //
      doMigration();
      //
      afterMigration();
    } catch (Exception e) {
      LOG.error("Failed to run migration data from JCR to Mysql.", e);
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
  protected Map<String, IdentityEntity> getAllIdentityEntity(String providerId) {
    ProviderEntity providerEntity;
    try {
      providerEntity = getProviderRoot().getProviders().get(providerId);
    } catch (Exception ex) {
      lifeCycle.getProviderRoot().set(null);
      providerEntity = getProviderRoot().getProviders().get(providerId);
    }
    return (providerEntity != null) ? providerEntity.getIdentities() : new HashMap<String, IdentityEntity>();
  }
  
  protected void processLog(String msg, int size, int count) {
    size = (size <= 0) ? 1 : size;
    if (count == 1) {
      process = "=";
    }
    int processSize = (size / 10);
    processSize = (processSize <= 0) ? 1 : processSize;
    if ((count * 10) % processSize == 0) {
      process += "=";
    }
    //
    LOG.info(String.format(msg + ":[%s> %s%%]", process, (100 * count) / size));
  }
  
  protected NodeIterator getIdentityNodes() {
    StringBuffer sb = new StringBuffer().append("SELECT * FROM soc:identitydefinition WHERE ");
    sb.append(JCRProperties.path.getName()).append(" LIKE '").append(getProviderRoot().getProviders().get(OrganizationIdentityProvider.NAME).getPath() + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
    return nodes(sb.toString());
  }
  
  protected NodeIterator getSpaceIdentityNodes() {
    ProviderEntity providerEntity = getProviderRoot().getProviders().get(SpaceIdentityProvider.NAME);
    if (providerEntity != null) {
      StringBuffer sb = new StringBuffer().append("SELECT * FROM soc:identitydefinition WHERE ");
      sb.append(JCRProperties.path.getName()).append(" LIKE '").append(providerEntity.getPath() + StorageUtils.SLASH_STR + StorageUtils.PERCENT_STR + "'");
      return nodes(sb.toString());
    } else {
      return null;
    }
    
  }

  protected int getInteger(InitParams params, String key, int defaultValue) {
    try {
      return Integer.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  protected String getString(InitParams params, String key, String defaultValue) {
    try {
      return params.getValueParam(key).getValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  protected abstract void beforeMigration() throws Exception;
  public abstract void doMigration() throws Exception;
  protected abstract void afterMigration() throws Exception;
  public abstract void doRemove() throws Exception;
  protected abstract String getListenerKey();
}
