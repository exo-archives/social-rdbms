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
package org.exoplatform.social.addons.storage.dao.jpa;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.dao.GenericDAO;
import org.exoplatform.social.addons.storage.session.SocialSessionLifecycle;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com May
 * 18, 2015
 */
public class GenericDAOImpl<E,ID extends Serializable> implements GenericDAO<E, ID> {

  private static final Log LOG = ExoLogger.getLogger(GenericDAOImpl.class);

  protected Class<E>       entityClass;

  public GenericDAOImpl() {
    ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
    this.entityClass = (Class) genericSuperclass.getActualTypeArguments()[0];
  }

  @Override
  public Long count() {
    EntityManager em = lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<E> entity = query.from(entityClass);
    // Selecting the count
    query.select(cb.count(entity));
    return em.createQuery(query).getSingleResult();
  }

  @Override
  public E find(ID id) {
    return (E) lifecycleLookup().getCurrentEntityManager().find(entityClass, id);
  }

  @Override
  public List<E> findAll() {
    EntityManager em = lifecycleLookup().getCurrentEntityManager();
    
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<E> query = cb.createQuery(this.entityClass);

    Root<E> entity = query.from(this.entityClass);

    // Selecting the entity
    query.select(entity);

    return em.createQuery(query).getResultList();
  }

  @Override
  public E create(E entity) {
    lifecycleLookup().getCurrentEntityManager().persist(entity);
    return entity;
  }

  @Override
  public void createAll(List<E> entities) {
    for (E entity : entities) {
      lifecycleLookup().getCurrentEntityManager().persist(entity);
    }
  }

  @Override
  public E update(E entity) {
    lifecycleLookup().getCurrentEntityManager().merge(entity);
    return entity;
  }

  @Override
  public void updateAll(List<E> entities) {
    for (E entity : entities) {
      update(entity);
    }
  }

  @Override
  public void delete(E entity) {
    lifecycleLookup().getCurrentEntityManager().remove(entity);
  }
  
  @Override
  public void delete(ID id) {
    EntityManager em = lifecycleLookup().getCurrentEntityManager();
    em.remove(em.getReference(entityClass, id));
  }

  @Override
  public void deleteAll(List<E> entities) {
    for (E entity : entities) {
      delete(entity);
    }
  }
  
  @Override
  public void detach(E entity) {
    lifecycleLookup().getCurrentEntityManager().detach(entity);
  }
  
  @Override
  public boolean contains(E entity) {
    return lifecycleLookup().getCurrentEntityManager().contains(entity);
  }

  /**
   * Starts the synchronization EntityManager
   * 
   * @return
   */
  public static boolean startSynchronization() {
    SocialSessionLifecycle lc = lifecycleLookup();
    if (!lc.hasSynchronization()) {
      lc.startRequest(ExoContainerContext.getCurrentContainer());
      return true;
    }
    return false;
  }
  
  /**
   * Begins the synchronization transaction of EntityManager
   * 
   * @return
   */
  public static boolean beginTransaction() {
    if (!lifecycleLookup().isActive()) {
      lifecycleLookup().getCurrentEntityManager().getTransaction().begin();
      return true;
    }
    
    return false;
    
  }
  
  /**
   * Rollback the synchronization transaction of EntityManager
   * 
   * @return
   */
  public static void rollbackTransaction() {
    if (lifecycleLookup().isActive()) {
      lifecycleLookup().getCurrentEntityManager().getTransaction().rollback();;
    }
    
  }
  
  /**
   * Ends the synchronization transaction of EntityManager
   * Handle the rollback if there is any exception
   * 
   * @return
   */
  public static void endTransaction(boolean closeRequest) {
    if (lifecycleLookup().isActive()) {
      if (closeRequest) {
        try {
          lifecycleLookup().getCurrentEntityManager().getTransaction().commit();
        } catch (Exception e) {
          lifecycleLookup().getCurrentEntityManager().getTransaction().rollback();
        }
      }
    }
    
  }
  /**
   * Synchronize the persistence context to the underlying database.
   * Note: Inside the transaction scope
   * @return
   */
  public static boolean flush() {
    return lifecycleLookup().flush();
  }

  /**
   * Stops the synchronization EntityManager
   * 
   * @param requestClose
   */
  public static void stopSynchronization(boolean requestClose) {
    SocialSessionLifecycle lc = lifecycleLookup();
    if (requestClose) {
      lc.endRequest(ExoContainerContext.getCurrentContainer());
    }
  }

  public static SocialSessionLifecycle lifecycleLookup() {
    return CommonsUtils.getService(SocialSessionLifecycle.class);
  }

  
  

}
