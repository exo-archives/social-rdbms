/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.entity.Activity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;

/**
 * ActivityStream implementation.
 */
@Entity
@Table(name="ACTIVITY_STREAM")
public class ActivityStreamEntity {

  /**
   * Logger.
   */
  private static final Log LOG = ExoLogger.getLogger(ActivityStreamEntity.class);

  /**
   * Internal activityStorage uuid, from "published" node.
   */
  @Id 
  @Column(name="ACTIVITY_ID", length=36)
  private String id;
  /**
   * With context of user, prettyId is remoteUser (root, john...). With context
   * of space, prettyId is spaceName (space.getName()). By using this prettyId,
   * we can construct its url to portal ui.
   */
  private String prettyId;

  /**
   * Context Type.
   */
  private Type type;

  /**
   * Stream tittle.
   */
  private String title;

  /**
   * Favicon URL for this stream.
   */
  private String faviconUrl;

  /**
   * Permalink link to this stream (url on Social).
   */
  private String permaLink;

  private Activity activity;
  
  public ActivityStreamEntity() {
  }

  public final void setType(final String name) {
    if (name.equals(OrganizationIdentityProvider.NAME)) {
      setType(Type.USER);
    } else if (name.equals(SpaceIdentityProvider.NAME)) {
      setType(Type.SPACE);
    } else {
      LOG.warn("Failed to set activity stream type with type:" + name);
    }
  }

  public final String getId() {
    return id;
  }

  public final void setId(final String uuid) {
    this.id = uuid;
  }

  public final String getPrettyId() {
    return prettyId;
  }

  public final void setPrettyId(final String sPrettyId) {
    prettyId = sPrettyId;
  }

  public final Type getType() {
    return type;
  }

  public final void setType(final Type sType) {
    type = sType;
  }

  public final String getFaviconUrl() {
    return faviconUrl;
  }

  public final void setFaviconUrl(final String sFaviconUrl) {
    faviconUrl = sFaviconUrl;
  }

  public final String getTitle() {
    return title;
  }

  public final void setTitle(final String sTitle) {
    title = sTitle;
  }

  public final String getPermaLink() {
    return permaLink;
  }

  public final void setPermaLink(final String sPermaLink) {
    permaLink = sPermaLink;
  }

  @OneToOne(fetch = FetchType.LAZY)
  @PrimaryKeyJoinColumn
  public Activity getActivity() {
    return activity;
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  public ActivityStream getSocActivityStream() {
    ActivityStream activityStream = new ActivityStreamImpl();
    activityStream.setId(id);
    activityStream.setType(type);
    activityStream.setTitle(title);
    activityStream.setPermaLink(permaLink);
    activityStream.setPrettyId(prettyId);
    activityStream.setFaviconUrl(faviconUrl);
    return activityStream;
  }
}
