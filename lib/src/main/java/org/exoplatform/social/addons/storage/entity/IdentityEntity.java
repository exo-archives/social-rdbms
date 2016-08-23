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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Entity(name = "SocIdentityEntity")
@ExoEntity
@Table(name = "SOC_IDENTITIES")
@NamedQueries({
        @NamedQuery(
                name = "SocIdentity.findByProviderAndRemoteId",
                query = "SELECT id FROM SocIdentityEntity id WHERE id.providerId = :providerId AND id.remoteId = :remoteId"
        ),
        @NamedQuery(
                name = "SocIdentity.countIdentityByProvider",
                query = "SELECT count(id) FROM SocIdentityEntity id WHERE id.deleted = FALSE AND id.enabled = TRUE AND id.providerId = :providerId"
        ),
        @NamedQuery(
                name = "SocIdentity.getAllIds",
                query = "SELECT i.id FROM SocIdentityEntity i WHERE i.deleted = FALSE AND i.enabled = TRUE"
        )
})
public class IdentityEntity {

  @Id
  @SequenceGenerator(name="SEQ_SOC_IDENTITY_ID", sequenceName="SEQ_SOC_IDENTITY_ID")
  @GeneratedValue(strategy= GenerationType.AUTO, generator="SEQ_SOC_IDENTITY_ID")
  @Column(name="IDENTITY_ID")
  private long id;

  @Column(name = "PROVIDER_ID", nullable = false)
  private String providerId;

  @Column(name = "REMOTE_ID", nullable = false)
  private String remoteId;

  @Column(name = "ENABLED", nullable = false)
  private boolean enabled = true;

  @Column(name = "DELETED", nullable = false)
  private boolean deleted = false;

  @Column(name = "AVATAR_FILE_ID")
  private Long avatarFileId;

  @ElementCollection(fetch = FetchType.EAGER)
  @MapKeyColumn(name = "NAME")
  @Column(name = "VALUE")
  @CollectionTable(name = "SOC_IDENTITY_PROPERTIES", joinColumns = {@JoinColumn(name = "IDENTITY_ID")})
  private Map<String, String> properties = new HashMap<String, String>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "SOC_IDENTITY_EXPERIENCES", joinColumns = {@JoinColumn(name = "IDENTITY_ID")})
  private List<ProfileExperienceEntity> experiences = new ArrayList<>();
  //END_OF_PROFILE

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_DATE")
  private Date createdDate = new Date();

  @OneToMany(mappedBy = "receiver", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE}, orphanRemoval = true)
  private List<ConnectionEntity> incomingConnections;

  @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE}, orphanRemoval = true)
  private List<ConnectionEntity> outgoingConnections;

  public long getId() {
    return id;
  }

  public String getStringId() {
    return String.valueOf(id);
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public Long getAvatarFileId() {
    return avatarFileId;
  }

  public void setAvatarFileId(Long avatarFileId) {
    this.avatarFileId = avatarFileId;
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
}
