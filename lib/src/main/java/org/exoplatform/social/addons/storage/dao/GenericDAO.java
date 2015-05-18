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
package org.exoplatform.social.addons.storage.dao;

import java.io.Serializable;
import java.util.List;

/**
 * Created by The eXo Platform SAS Author : 
 *    eXoPlatform exo@exoplatform.com May 18, 2015
 */
public interface GenericDAO<E, ID extends Serializable> {

  /**
   * Get the number of entities with the specified type and id from the
   * datasource.
   *
   * @return the entity, null if none is found
   */
  Long count();

  /**
   * Get the entity with the specified type and id from the database.
   *
   * @return the entity, null if none is found
   */
  E find(ID id);

  /**
   * Get a list of all object of the specified type from the datasource.
   *
   * @return a list of entities
   */
  List<E> findAll();

  /**
   * Insert a new entity. If the entity already exist, use update(E entity)
   * instead
   *
   * @return the new entity
   */
  E create(E entity);

  /**
   * Insert a list of new entities in the persistence context. If the entities
   * already exist, use update(E entity) instead
   */
  void createAll(List<E> entities);

  /**
   * Update the entity in the persistence context. If the entity does not
   * already exist, use create(E entity) instead
   *
   * @return the just created entity
   */
  E update(E entity);

  /**
   * Update the entity in the persistence context. If the entity does not
   * already exist, use create(E entity) instead
   *
   * @return the just created entity
   */
  void updateAll(List<E> entities);

  /**
   * Delete the specified entity from the persistence context.
   * @param entity the given entity to delete
   */
  void delete(E entity);
  
  /**
   * Delete the specified entity ID from the persistence context.
   * @param id the given Id to delete
   */
  void delete(ID id);

  /**
   * Remove all of the specified entities from the persistence context.
   */
  void deleteAll(List<E> entities);

  /**
   * Remove the given entity from the persistence context, causing
   * a managed entity to become detached.  Unflushed changes made
   * to the entity if any (including removal of the entity),
   * will not be synchronized to the database.  Entities which
   * previously referenced the detached entity will continue to
   * reference it.
   */
  void detach(E entity);
  
  /**
   * Check if the instance is a managed entity instance belonging
   * to the current persistence context.
   * @param entity entity instance
   * @return boolean indicating if entity is in persistence context
   */
  boolean contains(E entity);

}
