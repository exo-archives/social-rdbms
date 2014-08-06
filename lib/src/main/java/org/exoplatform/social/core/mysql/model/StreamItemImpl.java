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
package org.exoplatform.social.core.mysql.model;


/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Huy Quang
 *          quangnh2@exoplatform.com
 * Dec 19, 2013  
 */
public class StreamItemImpl implements StreamItem {
  private String _id;
  private String activityId;
  private String ownerId;
  private String posterId;
  private String viewerId;
  private String viewerType;
  private Boolean hidable;
  private Boolean lockable;
  private Long time;
  private Integer mentioner;
  private Integer commenter;
  
  
  public String getId() {
    return _id;
  }

  public void setId(String _id) {
    this._id = _id;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getPosterId() {
    return posterId;
  }

  public void setPosterId(String posterId) {
    this.posterId = posterId;
  }

  public String getViewerId() {
    return viewerId;
  }

  public void setViewerId(String viewerId) {
    this.viewerId = viewerId;
  }

  public String getViewerType() {
    return viewerType;
  }

  public void setViewerType(String viewerType) {
    this.viewerType = viewerType;
  }

  public Boolean getHidable() {
    return hidable;
  }

  public void setHidable(Boolean hidable) {
    this.hidable = hidable;
  }

  public Boolean getLockable() {
    return lockable;
  }

  public void setLockable(Boolean lockable) {
    this.lockable = lockable;
  }

  public Long getTime() {
    return time;
  }

  public void setTime(Long time) {
    this.time = time;
  }

  public Integer getMentioner() {
    return mentioner;
  }

  public void setMentioner(Integer mentioner) {
    this.mentioner = mentioner;
  }

  public Integer getCommenter() {
    return commenter;
  }

  public void setCommenter(Integer commenter) {
    this.commenter = commenter;
  }

  @Override
  public String toString() {
    return "StreamItemImpl[id = " + getId() + ", activityId=" + getActivityId() + ", viewerId= " + getViewerId() + " ]";
  }
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StreamItemImpl)) {
      return false;
    }

    StreamItemImpl that = (StreamItemImpl) o;

    if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getId() != null ? getId().hashCode() : 0);
    return result;
  }
}
