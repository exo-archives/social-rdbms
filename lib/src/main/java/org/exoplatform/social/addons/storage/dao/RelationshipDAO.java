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

import org.exoplatform.social.addons.storage.entity.RelationshipItem;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.relationship.model.Relationship;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 4, 2015  
 */
public interface RelationshipDAO extends GenericDAO<RelationshipItem, Long> {

  /**
   * Get all connections of an user
   * 
   * @param identity
   * @param status
   * @return
   */
  List<RelationshipItem> getConnections(Identity identity, Relationship.Type status);
  
  /**
   * Has the connections
   * 
   * @param identity
   * @param status
   * @return TRUE/FALSE
   */
  long count(Identity identity, Relationship.Type status);

  /**
   * Get relationship of 2 users
   * 
   * @param identity1
   * @param identity2
   * @return
   */
  RelationshipItem getRelationship(Identity identity1, Identity identity2);

}
