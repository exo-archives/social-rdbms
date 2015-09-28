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

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.CommentDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.CommentQueryBuilder;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class CommentDAOImpl extends GenericDAOJPAImpl<Comment, Long>  implements CommentDAO {
  
  @Override
  public List<Comment> getComments(Activity existingActivity, int offset, int limit) {
    
    return CommentQueryBuilder.builder()
                              .activityId(existingActivity.getId())
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
   
  }
  
  @Override
  public List<Comment> getNewerOfComments(Activity existingActivity, long sinceTime, int limit) {
    
    return CommentQueryBuilder.builder()
                              .activityId(existingActivity.getId())
                              .offset(0)
                              .newer(sinceTime)
                              .ascOrder()
                              .limit(limit)
                              .build()
                              .getResultList();
   
  }
  
  @Override
  public List<Comment> getOlderOfComments(Activity existingActivity, long sinceTime, int limit) {
    return CommentQueryBuilder.builder()
                              .activityId(existingActivity.getId())
                              .offset(0)
                              .older(sinceTime)
                              .limit(limit)
                              .build()
                              .getResultList();
   
  }

  @Override
  public int getNumberOfComments(Activity existingActivity) {
    return CommentQueryBuilder.builder()
                              .activityId(existingActivity.getId())
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  @ExoTransactional
  public Activity findActivity(Long commentId) {
    try {
      return CommentQueryBuilder.builder().commentId(commentId).buildActivty();
    } catch (NoResultException e) {
      return null;
    }
  }

}
