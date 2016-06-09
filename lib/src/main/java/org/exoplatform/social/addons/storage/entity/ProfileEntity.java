/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.social.addons.storage.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Embeddable
public class ProfileEntity {  

  @Column(name = "URL")
  private String url;

  @Column(name = "AVATAR_URL")
  private String avatarURL;

  @Column(name = "AVATAR_MIMETYPE")
  private String avatarMimeType;

  @Lob
  @Column(name = "AVATAR_IMAGE")
  private byte[] avatarImage;

  @ElementCollection(fetch = FetchType.EAGER)
  @MapKeyColumn(name = "NAME")
  @Column(name = "VALUE")
  @CollectionTable(name = "SOC_PROFILE_PROPERTIES", joinColumns = {@JoinColumn(name = "IDENTITY_ID")})
  private Map<String, String> properties = new HashMap<String, String>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "SOC_PROFILE_EXPERIENCES", joinColumns = {@JoinColumn(name = "IDENTITY_ID")})
  private List<ProfileExperienceEntity> experiences = new ArrayList<>();

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_DATE")
  private Date createdDate = new Date();
  
  @Transient
  private IdentityEntity identity;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getAvatarURL() {
    return avatarURL;
  }

  public void setAvatarURL(String avatarURL) {
    this.avatarURL = avatarURL;
  }

  public String getAvatarMimeType() {
    return avatarMimeType;
  }

  public void setAvatarMimeType(String avatarMimeType) {
    this.avatarMimeType = avatarMimeType;
  }

  public byte[] getAvatarImage() {
    return avatarImage;
  }

  public void setAvatarImage(byte[] avatarImage) {
    this.avatarImage = avatarImage;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public List<ProfileExperienceEntity> getExperiences() {
    return experiences;
  }

  public void setExperiences(List<ProfileExperienceEntity> experiences) {
    this.experiences = experiences;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdTime) {
    this.createdDate = createdTime;
  }

  public IdentityEntity getIdentity() {
    return identity;
  }

  public void setIdentity(IdentityEntity identity) {
    this.identity = identity;
    identity.setProfile(this);
  }
}
