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

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Entity
@ExoEntity
@Table(name = "SOC_IDENTITY_PROFILE")
@NamedQueries({
        @NamedQuery(
                name = "SocProfile.findByIdentity",
                query = "SELECT p FROM ProfileEntity p WHERE p.identity.id = :identityId"
        )
})
public class ProfileEntity {
  @Id
  @SequenceGenerator(name="SEQ_SOC_IDENTITY_PROFILE_ID", sequenceName="SEQ_SOC_IDENTITY_PROFILE_ID")
  @GeneratedValue(strategy= GenerationType.AUTO, generator="SEQ_SOC_IDENTITY_PROFILE_ID")
  @Column(name="PROFILE_ID")
  private long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "IDENTITY_ID")
  private IdentityEntity identity;

  private String url;

  @Column(name = "AVATAR_URL")
  private String avatarURL;

  @Column(name = "AVATAR_MIMTYPE")
  private String avatarMimeType;

  @Lob
  @Column(name = "AVATAR_IMAGE")
  private byte[] avatarImage;

  @ElementCollection(fetch = FetchType.EAGER)
  @MapKeyColumn(name = "NAME")
  @Column(name = "VALUE")
  @CollectionTable(name = "SOC_IDENTITY_PROFILE_PROPERTY", joinColumns = {@JoinColumn(name = "PROFILE_ID")})
  private Map<String, String> properties = new HashMap<String, String>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "SOC_IDENTITY_PROFILE_EXPERIENCE", joinColumns = {@JoinColumn(name = "PROFILE_ID")})
  private List<ProfileExperience> experiences = new ArrayList<>();

  @Column(name = "CREATED_TIME")
  private long                      createdTime;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public IdentityEntity getIdentity() {
    return identity;
  }

  public void setIdentity(IdentityEntity identity) {
    this.identity = identity;
  }

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

  public List<ProfileExperience> getExperiences() {
    return experiences;
  }

  public void setExperiences(List<ProfileExperience> experiences) {
    this.experiences = experiences;
  }

  public long getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(long createdTime) {
    this.createdTime = createdTime;
  }
}
