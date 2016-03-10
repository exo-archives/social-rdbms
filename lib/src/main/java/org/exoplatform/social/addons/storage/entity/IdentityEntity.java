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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Entity
@ExoEntity
@Table(name = "SOC_IDENTITY")
@NamedQueries({
        @NamedQuery(
                name = "SocIdentity.findByProviderAndRemoteId",
                query = "SELECT id FROM IdentityEntity id WHERE id.providerId = :providerId AND id.remoteId = :remoteId"
        ),
        @NamedQuery(
                name = "SocIdentity.countIdentityByProvider",
                query = "SELECT count(id) FROM IdentityEntity id WHERE id.deleted = FALSE AND id.enable = TRUE AND id.providerId = :providerId"
        ),
        @NamedQuery(
                name = "SocIdentity.getAllIds",
                query = "SELECT i.id FROM IdentityEntity i"
        )
})
public class IdentityEntity {

  @Id
  @SequenceGenerator(name="SEQ_SOC_IDENTITY_ID", sequenceName="SEQ_SOC_IDENTITY_ID")
  @GeneratedValue(strategy= GenerationType.AUTO, generator="SEQ_SOC_IDENTITY_ID")
  @Column(name="IDENTITY_ID")
  private long id;

  @Column(name = "PROVIDER_ID")
  private String providerId;

  @Column(name = "REMOTE_ID")
  private String remoteId;

  @Column(name = "ENABLE")
  private boolean enable = true;

  @Column(name = "DELETED")
  private boolean deleted = false;

  @OneToOne(mappedBy = "identity", fetch = FetchType.LAZY, orphanRemoval = true)
  private ProfileEntity profile;

  public long getId() {
    return id;
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

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public ProfileEntity getProfile() {
    return profile;
  }

  public void setProfile(ProfileEntity profile) {
    this.profile = profile;
  }
}
