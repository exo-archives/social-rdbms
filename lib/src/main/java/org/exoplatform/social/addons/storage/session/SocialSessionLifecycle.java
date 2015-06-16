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

import org.apache.log4j.Logger;
import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class SocialSessionLifecycle {
  /** */
  private final static Logger log = Logger.getLogger(SocialSessionLifecycle.class);
  
  private final EntityManagerService service; 

  public SocialSessionLifecycle(EntityManagerService service) throws MissingResourceException {
    this.service = service;
  }
  
  public EntityManager getCurrentEntityManager() {
    return service.getEntityManager();
  }
  
  /**
   * Checks the synchronization is existing or not
   * @return TRUE: EntityManager has been created, otherwise FALSE
   */
  public boolean hasSynchronization() {
    return (service.getEntityManager() != null);
  }
  
  /**
   * Synchronize the persistence context to the underlying database.
   * Note: Inside the transaction scope
   * 
   * @return TRUE/FALSE
   */
  public boolean flush() {
    service.getEntityManager().flush();
    return true;
  }

  /**
   * Checks the isActive the transaction
   * @return TRUE/FALSE
   */
  public boolean isActive() {
    if(!hasSynchronization()) return false;
    return getCurrentEntityManager().getTransaction().isActive();
  }
  
  public boolean startRequest() {
    if (!hasSynchronization()) {
      log.debug("startRequest::EntityManager is stared!");
      service.startRequest(ExoContainerContext.getCurrentContainer());
      return true;
    }
    return false;
  }

  public void endRequest(boolean requestClose) {
    if (requestClose && hasSynchronization()) {
      log.debug("endRequest::EntityManager is closed!");
      service.endRequest(ExoContainerContext.getCurrentContainer());
    }
  }

}
