/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.mysql.old;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Huy Quang
 *          quangnh2@exoplatform.com
 * Dec 19, 2013  
 */
public interface StreamItem{
  public String getId();

  public void setId(String _id);

  public String getActivityId();

  public void setActivityId(String activityId);

  public String getOwnerId();

  public void setOwnerId(String ownerId);

  public String getPosterId();

  public void setPosterId(String posterId);

  public String getViewerId();

  public void setViewerId(String viewerId);

  public String getViewerType();

  public void setViewerType(String viewerType);

  public Boolean getHidable();

  public void setHidable(Boolean hidable);

  public Boolean getLockable();

  public void setLockable(Boolean lockable);

  public Long getTime();

  public void setTime(Long time);

  public Integer getMentioner();

  public void setMentioner(Integer mentioner);

  public Integer getCommenter();

  public void setCommenter(Integer commenter);
}
