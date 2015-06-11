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

import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.RelationshipStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 3, 2015  
 */
public class SynchronizedRDBMSRelationshipStorage extends RDBMSRelationshipStorageImpl {

  public SynchronizedRDBMSRelationshipStorage(IdentityStorage identityStorage, RelationshipDAO relationshipDAO, ProfileItemDAO profileItemDAO) {
    super(identityStorage, relationshipDAO, profileItemDAO);
  }
  
  @Override
  public Relationship saveRelationship(final Relationship relationship) throws RelationshipStorageException {
    boolean begunEM = GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.beginTransaction();
      try {
        return super.saveRelationship(relationship);
      } finally {
        GenericDAOImpl.endTransaction(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization(begunEM);
    }
  }

}
