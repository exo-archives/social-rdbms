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
package org.exoplatform.social.addons.storage.dao.jpa.synchronization;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 11, 2015  
 */
import java.io.Serializable;

import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 18, 2015  
 */
public class SynchronizedGenericDAO<E, ID extends Serializable> extends GenericDAOImpl<E, ID> {
  
  @Override
  public E create(E entity) {
    boolean begun = GenericDAOImpl.startTx();
    try {
      return super.create(entity);
    } finally {
      GenericDAOImpl.endTx(begun);
    }
  }
  
  @Override
  public E update(E entity) {
    boolean begun = GenericDAOImpl.startTx();
    try {
      return super.update(entity);
    } finally {
      GenericDAOImpl.endTx(begun);
    }
  }
  
  @Override
  public void delete(E entity) {
    boolean begun = GenericDAOImpl.startTx();
    try {
      super.delete(entity);
    } finally {
      GenericDAOImpl.endTx(begun);
    }
  }
  
  @Override
  public void delete(ID id) {
    boolean begun = GenericDAOImpl.startTx();
    try {
      super.delete(id);
    } finally {
      GenericDAOImpl.endTx(begun);
    }
  }
}