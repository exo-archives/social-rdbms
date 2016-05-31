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

import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 4, 2015  
 */
public interface ConnectionDAO extends GenericDAO<Connection, Long> {

  /**
   * Has the connections
   * 
   * @param identity
   * @param status
   * @return TRUE/FALSE
   */
  long count(Identity identity, Relationship.Type status);

  /**
   * Get connection of 2 users
   * 
   * @param identity1
   * @param identity2
   * @return
   */
  Connection getConnection(Identity identity1, Identity identity2);

  Connection getConnection(Long sender, Long receiver);

  
  /**
   * @param identity
   * @param type
   * @param offset
   * @param limit
   * @return
   */
  List<Connection> getConnections(Identity identity, Type type, long offset, long limit);

  List<Connection> getConnections(Identity sender, Identity receiver, Type status);

  /**
   * @param identity
   * @param type
   * @return
   */
  int getConnectionsCount(Identity identity, Type type);

  /**
   * @param identity
   * @param limit
   * @return
   */
  List<Connection> getLastConnections(Identity identity, int limit);

  List<Connection> getConnectionsByFilter(Identity existingIdentity, ProfileFilter profileFilter, Type type, long offset, long limit);

  int getConnectionsByFilterCount(Identity identity, ProfileFilter profileFilter, Type type);

}
