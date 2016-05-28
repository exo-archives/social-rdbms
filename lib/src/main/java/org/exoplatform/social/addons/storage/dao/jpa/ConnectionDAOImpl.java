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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.RelationshipQueryBuilder;
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
public class ConnectionDAOImpl extends GenericDAOJPAImpl<Connection, Long> implements ConnectionDAO {

  @Override
  @ExoTransactional
  public long count(Identity identity, Type status) {
    return RelationshipQueryBuilder.builder()
                                        .owner(identity)
                                        .status(status)
                                        .buildCount()
                                        .getSingleResult();
  }

  @Override
  public Connection getConnection(Identity identity1, Identity identity2) {
    TypedQuery<Connection> query = RelationshipQueryBuilder.builder()
                                                                 .sender(identity1)
                                                                 .receiver(identity2)
                                                                 .buildSingleRelationship();
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } catch (NonUniqueResultException e) {
      return query.getResultList().get(0);
    }
  }

  @Override
  public Connection getConnection(Long sender, Long reciver) {
    TypedQuery<Connection> query = getEntityManager().createNamedQuery("SocConnection.findConnectionBySenderAndReceiver", Connection.class);
    query.setParameter("sender", sender);
    query.setParameter("reciver", reciver);
    query.setMaxResults(1);

    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public List<Connection> getConnections(Identity identity, Type type, long offset, long limit) {
    return RelationshipQueryBuilder.builder()
                                   .owner(identity)
                                   .status(type)
                                   .offset(offset)
                                   .limit(limit)
                                   .build()
                                   .getResultList();
  }

  @Override
  public List<Connection> getConnections(Identity sender, Identity receiver, Type status) {
    return RelationshipQueryBuilder.builder()
                                   .sender(sender)
                                   .receiver(receiver)
                                   .status(status)
                                   .build()
                                   .getResultList();
  }

  @Override
  public int getConnectionsCount(Identity identity, Type type) {
    return RelationshipQueryBuilder.builder()
                                   .owner(identity)
                                   .status(type)
                                   .buildCount()
                                   .getSingleResult()
                                   .intValue();
  }

  @Override
  public List<Connection> getLastConnections(Identity identity, int limit) {
    return RelationshipQueryBuilder.builder()
                                   .owner(identity)
                                   .status(Relationship.Type.CONFIRMED)
                                   .offset(0)
                                   .limit(limit)
                                   .buildLastConnections()
                                   .getResultList();
  }
  
  public List<Connection> getConnectionsByFilter(Identity existingIdentity, ProfileFilter profileFilter, Type type, long offset, long limit) {
    return RelationshipQueryBuilder.builder()
                                   .owner(existingIdentity)
                                   .status(type)
                                   .offset(0)
                                   .limit(limit)
                                   .filter(profileFilter)
                                   .buildFilter()
                                   .getResultList();
  }

  @Override
  public int getConnectionsByFilterCount(Identity identity, ProfileFilter profileFilter, Type type) {
    return RelationshipQueryBuilder.builder()
                                   .owner(identity)
                                   .status(type)
                                   .filter(profileFilter)
                                   .buildFilterCount()
                                   .getSingleResult()
                                   .intValue();
  }
 
}
