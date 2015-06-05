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
package org.exoplatform.social.addons.storage;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.addons.storage.entity.RelationshipFilterType;
import org.exoplatform.social.addons.storage.entity.RelationshipItem;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.RelationshipStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.RelationshipStorageImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 3, 2015  
 */
public class RDBMSRelationshipStorageImpl extends RelationshipStorageImpl {
  
  private final RelationshipDAO relationshipDAO;
  private final IdentityStorage identityStorage;

  public RDBMSRelationshipStorageImpl(IdentityStorage identityStorage, RelationshipDAO relationshipDAO) {
    super(identityStorage);
    this.relationshipDAO = relationshipDAO;
    this.identityStorage = identityStorage;
  }

  @Override
  public Relationship saveRelationship(Relationship relationship) throws RelationshipStorageException {
    if (relationship.getId() == null) {
      RelationshipItem entity = new RelationshipItem();
      entity.setReceiverId(relationship.getReceiver().getId());
      entity.setSenderId(relationship.getSender().getId());
      entity.setStatus(relationship.getStatus());
      relationshipDAO.create(entity);
      relationship.setId(Long.toString(entity.getId()));
    } else {
      RelationshipItem entity = relationshipDAO.find(Long.valueOf(relationship.getId()));
      entity.setStatus(relationship.getStatus());
      relationshipDAO.update(entity);
    }
    //
    return relationship;
  }
  
  @Override
  public void removeRelationship(Relationship relationship) throws RelationshipStorageException {
    relationshipDAO.delete(Long.valueOf(relationship.getId()));
  }
  
  @Override
  public Relationship getRelationship(Identity identity1, Identity identity2) throws RelationshipStorageException {
    return convertRelationshipItemToRelationship(relationshipDAO.getRelationship(identity1, identity2));
  }
  
  @Override
  public Relationship getRelationship(String relationshipId) throws RelationshipStorageException {
    return convertRelationshipItemToRelationship(relationshipDAO.find(Long.valueOf(relationshipId)));
  }
  
  @Override
  public List<Identity> getConnections(Identity identity) throws RelationshipStorageException {
    return getConnections(identity, 0, -1);
  }
  
  @Override
  public List<Identity> getConnections(Identity identity, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(relationshipDAO.getRelationships(identity, Relationship.Type.CONFIRMED, RelationshipFilterType.ALL, offset, limit), identity.getId());
  }
  
  @Override
  public int getConnectionsCount(Identity identity) throws RelationshipStorageException {
    return relationshipDAO.getRelationshipsCount(identity, Relationship.Type.CONFIRMED, RelationshipFilterType.ALL);
  }
  
  @Override
  public int getRelationshipsCount(Identity identity) throws RelationshipStorageException {
    return relationshipDAO.getRelationshipsCount(identity, null, RelationshipFilterType.ALL);
  }
  
  @Override
  public List<Relationship> getRelationships(Identity identity, Relationship.Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    return getRelationships(identity, type, RelationshipFilterType.ALL);
  }
  
  @Override
  public List<Relationship> getReceiverRelationships(Identity receiver, Relationship.Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    return getRelationships(receiver, type, RelationshipFilterType.RECEIVER);
  }
  
  @Override
  public List<Relationship> getSenderRelationships(Identity sender, Relationship.Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    return getRelationships(sender, type, RelationshipFilterType.SENDER);
  }
  
  public List<Relationship> getRelationships(Identity identity, Relationship.Type type, RelationshipFilterType filterType) {
    return convertRelationshipEntitiesToRelationships(relationshipDAO.getRelationships(identity, type, filterType, 0, -1));
  }
  
  @Override
  public List<Identity> getOutgoingRelationships(Identity sender, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(relationshipDAO.getRelationships(sender, Relationship.Type.PENDING, RelationshipFilterType.SENDER, offset, limit), sender.getId());
  }
  
  @Override
  public int getOutgoingRelationshipsCount(Identity sender) throws RelationshipStorageException {
    return relationshipDAO.getRelationshipsCount(sender, Relationship.Type.PENDING, RelationshipFilterType.SENDER);
  }
  
  @Override
  public List<Identity> getIncomingRelationships(Identity receiver, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(relationshipDAO.getRelationships(receiver, Relationship.Type.PENDING, RelationshipFilterType.RECEIVER, offset, limit), receiver.getId());
  }
  
  @Override
  public int getIncomingRelationshipsCount(Identity receiver) throws RelationshipStorageException {
    return relationshipDAO.getRelationshipsCount(receiver, Relationship.Type.PENDING, RelationshipFilterType.RECEIVER);
  }
  
  @Override
  public List<Identity> getLastConnections(Identity identity, int limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(relationshipDAO.getLastConnections(identity, limit), identity.getId());
  }
  
  private List<Identity> convertRelationshipEntitiesToIdentities(List<RelationshipItem> relationshipItems, String ownerId) {
    List<Identity> identities = new ArrayList<Identity>();
    if (relationshipItems == null) return identities;
    for (RelationshipItem item : relationshipItems) {
      identities.add(getIdentityFromRelationshipItem(item, ownerId));
    }
    return identities;
  }
  
  private List<Relationship> convertRelationshipEntitiesToRelationships(List<RelationshipItem> relationshipItems) {
    List<Relationship> relationships = new ArrayList<Relationship>();
    if (relationshipItems == null) return relationships;
    for (RelationshipItem item : relationshipItems) {
      relationships.add(convertRelationshipItemToRelationship(item));
    }
    return relationships;
  }

  private Identity getIdentityFromRelationshipItem(RelationshipItem item, String ownerId) {
    if (ownerId.equals(item.getSenderId())) {
      return identityStorage.findIdentityById(item.getReceiverId());
    }
    return identityStorage.findIdentityById(item.getSenderId());
  }
  
  private Relationship convertRelationshipItemToRelationship(RelationshipItem item) {
    if (item == null) return null;
    //
    Relationship relationship = new Relationship(Long.toString(item.getId()));
    relationship.setSender(identityStorage.findIdentityById(item.getSenderId()));
    relationship.setReceiver(identityStorage.findIdentityById(item.getReceiverId()));
    relationship.setStatus(item.getStatus());
    return relationship;
  }
}
