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

import javax.persistence.TypedQuery;

import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.dao.jpa.synchronization.SynchronizedGenericDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class CommentDAOImpl extends SynchronizedGenericDAO<Comment, Long>  implements CommentDAO {
  //implements customize methods here
  
  public List<Comment> getComments(Activity existingActivity, long time, Boolean isNewer, int offset, int limit) {
    StringBuilder strQuery = new StringBuilder();
    strQuery.append("select c from Comment c join c.activity a where (a.id ='")
            .append(existingActivity.getId())
            .append("')")
            .append(buildSQLQueryByTime("c.lastUpdated", time, isNewer))
            .append(" and (c.hidden = " + Boolean.FALSE +  ") and (c.locked = " + Boolean.FALSE + ") order by c.lastUpdated asc");
    //
    return  getComments(strQuery.toString(), offset, limit);
  }

  private List<Comment> getComments(String strQuery, long offset, long limit) throws ActivityStorageException {
    TypedQuery<Comment> typeQuery = lifecycleLookup().getCurrentEntityManager().createQuery(strQuery, Comment.class);
    if (limit > 0) {
      typeQuery.setFirstResult((int) offset);
      typeQuery.setMaxResults((int) limit);
    }
    return typeQuery.getResultList();
  }

  public int getNumberOfComments(Activity existingActivity) {
    // TODO: Need use count query
    try {
      return existingActivity.getComments().size();
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public void delete(Comment entity) {
    throw new UnsupportedOperationException("Can not support this method, it replace by remove comment entity from activity entity");
  }

  @Override
  public void delete(Long id) {
    throw new UnsupportedOperationException("Can not support this method, it replace by remove comment entity from activity entity");
  }

}
