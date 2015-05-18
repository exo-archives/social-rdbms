/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.storage.session;


import java.util.MissingResourceException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class SocialSessionLifecycle implements Startable, ComponentRequestLifecycle {
  /** */
  private final static Logger log = Logger.getLogger(SocialSessionLifecycle.class);
  /** */
  private static final ThreadLocal<EntityManager> session = new ThreadLocal<EntityManager>();
  /** */
  private static final String PERSISTENT_UNIT_NAME_PARAMS = "persistent-unit-param";
  /** */
  private final EntityManagerFactory emf;

  public SocialSessionLifecycle(final InitParams params) throws MissingResourceException {
    ValueParam persistentUnitValue = params.getValueParam(PERSISTENT_UNIT_NAME_PARAMS);
    if (persistentUnitValue == null) {
      throw new MissingResourceException("Missing/wrong the persistent unit in /META-INF/persistent.xml", this.getClass().getName(), PERSISTENT_UNIT_NAME_PARAMS);
    }
    //TODO define the default persistent unit
    String persistentValue = persistentUnitValue == null ? "default" : persistentUnitValue.getValue();
    emf = Persistence.createEntityManagerFactory(persistentValue);
  }
  
  public EntityManager getCurrentEntityManager() {
    return openEntityManager();
  }

  /**
   * Opens the new EntityManager
   * @return
   */
  private EntityManager openEntityManager() {
    if (session.get() == null) {
      session.set(emf.createEntityManager());
    }
    return session.get();
  }
  
  /**
   * Checks the synchronization is existing or not
   * @return TRUE: EntityManager has been created, otherwise FALSE
   */
  public boolean hasSynchronization() {
    return (session.get() != null);
  }
  
  /**
   * Synchronize the persistence context to the underlying database.
   * Note: Inside the transaction scope
   * 
   * @return TRUE/FALSE
   */
  public boolean flush() {
    if (session.get() != null) {
      EntityManager em = session.get();
      if (em.getTransaction().isActive()) {
        em.flush();
        return true;
      }
    }
    return false;
  }

  /**
   * Closes the new EntityManager
   */
  public void closeEntityManager() {
    if (session.get() != null) {
      EntityManager em = session.get();
      if (em.getTransaction().isActive()) {
        em.flush();
      }
      if (em.isOpen()) {
        em.close();
      }
      em = null;
      session.remove();
    }
  }

  /**
   * Checks the isActive the transaction
   * @return TRUE/FALSE
   */
  public boolean isActive() {
    if(session.get() == null) return false;
    return session.get().getTransaction().isActive();
  }
  
  @Override
  public void start() {
    if (emf == null) {
      log.error("EntityManager wasn't initialized successfully!");
    }
  }


  @Override
  public void stop() {
    this.emf.close();
  }


  @Override
  public void startRequest(ExoContainer container) {
    if (emf != null) {
      openEntityManager();
      log.debug("startRequest::EntityManager is stared!");
    }
  }

  @Override
  public void endRequest(ExoContainer container) {
    closeEntityManager();
    log.debug("endRequest::EntityManager is closed!");
  }

}
