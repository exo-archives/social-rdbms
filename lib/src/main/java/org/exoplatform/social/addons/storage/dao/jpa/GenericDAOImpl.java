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

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 11, 2015  
 */
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.exoplatform.commons.utils.CommonsUtils;
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
    E result = lifecycleLookup().getCurrentEntityManager().find(entityClass, id);
    if (result == null) {
      LOG.warn("Entity ID: " + id + " not found!");
    }
    return result;
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
    lifecycleLookup().getCurrentEntityManager().remove(update(entity));
  }
  
  @Override
  public void delete(ID id) {
    try {
      EntityManager em = lifecycleLookup().getCurrentEntityManager();
      em.remove(em.getReference(entityClass, id));
    } catch (EntityNotFoundException nex) {
      LOG.warn("Entity ID " + id + " not found!");
    }
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
  public static void startSynchronization() {
    SocialSessionLifecycle lc = lifecycleLookup();
    lc.startRequest();
  }
  
  /**
   * Starts the transaction if it isn't existing
   * 
   * @return
   */
  public static boolean startTx() {
    SocialSessionLifecycle lc = lifecycleLookup();
    if (!lc.isActive()) {
      lc.getCurrentEntityManager().getTransaction().begin();
      LOG.debug("started new transaction");
      return true;
    }
    return false;
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
   */
  public static void stopSynchronization() {
    SocialSessionLifecycle lc = lifecycleLookup();
    lc.endRequest();
  }
  
  /**
   * Stops the transaction
   * 
   * @param requestClose
   */
  public static void endTx(boolean requestClose) {
   
    SocialSessionLifecycle lc = lifecycleLookup();
    try {
      if (requestClose && lc.isActive()) {
        lc.getCurrentEntityManager().getTransaction().commit();
        LOG.debug("commited transaction");
      }
    } catch (RuntimeException e) {
      LOG.error("Failed to commit to DB::" + e.getMessage(), e);
      lc.getCurrentEntityManager().getTransaction().rollback();
    }
  }

  public static SocialSessionLifecycle lifecycleLookup() {
    return CommonsUtils.getService(SocialSessionLifecycle.class);
  }

}