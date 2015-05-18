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
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class CommentDAOImpl extends GenericDAOImpl<Comment, Long>  implements CommentDAO {
  //implements customize methods here
  
  public List<Comment> getComments(Activity existingActivity, int offset, int limit) {
    StringBuilder strQuery = new StringBuilder();//DISTINCT
    strQuery.append("select c from Comment c join c.activity a where (a.id ='")
            .append(existingActivity.getId())
            .append("') and (a.hidden = '0') and (a.locked = '0') order by a.lastUpdated desc");
    //
    return getComments(strQuery.toString(), offset, limit);
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
    return existingActivity.getComments().size();
  }
}
