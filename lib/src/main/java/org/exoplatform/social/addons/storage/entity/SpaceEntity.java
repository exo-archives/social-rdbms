/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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

package org.exoplatform.social.addons.storage.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.core.space.model.Space;

@Entity
@ExoEntity
@Table(name = "SOC_SPACES")
@NamedQueries({
    @NamedQuery(name = "SpaceEntity.getLastSpaces", query = "SELECT sp FROM SpaceEntity sp ORDER BY sp.createdTime DESC"),
    @NamedQuery(name = "SpaceEntity.getVisitedSpaces", query = "SELECT sp FROM SpaceEntity sp INNER JOIN sp.members as mem WHERE mem.userId = :userId AND mem.status = :status AND sp.app like :app ORDER BY mem.visited DESC, sp.prettyName ASC"),
    @NamedQuery(name = "SpaceEntity.getLastAccessedSpace", query = "SELECT sp FROM SpaceEntity sp INNER JOIN sp.members as mem WHERE mem.userId = :userId AND mem.status = :status AND sp.app like :app ORDER BY mem.lastAccess DESC"),
    @NamedQuery(name = "SpaceEntity.getSpaceByGroupId", query = "SELECT sp FROM SpaceEntity sp WHERE sp.groupId = :groupId"),
    @NamedQuery(name = "SpaceEntity.getSpaceByPrettyName", query = "SELECT sp FROM SpaceEntity sp WHERE sp.prettyName = :prettyName"),
    @NamedQuery(name = "SpaceEntity.getSpaceByDisplayName", query = "SELECT sp FROM SpaceEntity sp WHERE sp.displayName = :displayName"),
    @NamedQuery(name = "SpaceEntity.getSpaceByURL", query = "SELECT sp FROM SpaceEntity sp WHERE sp.url = :url") })
public class SpaceEntity implements Serializable {

  private static final long serialVersionUID = 3223615477747436986L;

  @Id
  @SequenceGenerator(name = "SEQ_SOC_SPACES_ID", sequenceName = "SEQ_SOC_SPACES_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_SOC_SPACES_ID")
  @Column(name = "SPACE_ID")
  private Long              id;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<SpaceMember> members          = new HashSet<>();

  /**
   * The list of applications with portlet Id, application name, and its state
   * (installed, activated, deactivated).
   */
  @Column(name = "APP", length = 2000)
  private String            app;

  @Column(name = "PRETTY_NAME", length = 36)
  private String            prettyName;

  @Column(name = "DISPLAY_NAME", length = 36)
  private String            displayName;

  @Column(name = "REGISTRATION", length = 36)
  private String            registration;

  @Column(name = "DESCRIPTION", length = 2000)
  private String            description;

  @Column(name = "AVATAR_LAST_UPDATED")
  private Long              avatarLastUpdated;

  @Column(name = "VISIBILITY", length = 36)
  public String             visibility;

  @Column(name = "PRIORITY", length = 36)
  public String             priority;

  @Column(name = "GROUP_ID", length = 36)
  public String             groupId;

  @Column(name = "URL", length = 255)
  public String             url;

  @Column(name = "CREATED_TIME")
  private Long              createdTime = System.currentTimeMillis();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
  }

  public String getPrettyName() {
    return prettyName;
  }

  public void setPrettyName(String prettyName) {
    this.prettyName = prettyName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRegistration() {
    return registration;
  }

  public void setRegistration(String registration) {
    this.registration = registration;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getAvatarLastUpdated() {
    return avatarLastUpdated;
  }

  public void setAvatarLastUpdated(Long avatarLastUpdated) {
    this.avatarLastUpdated = avatarLastUpdated;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Long getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(Long createdTime) {
    this.createdTime = createdTime;
  }

  public Set<SpaceMember> getMembers() {
    return members;
  }

  public SpaceEntity buildFrom(Space space) {
    this.setApp(space.getApp());
    this.setAvatarLastUpdated(space.getAvatarLastUpdated());
    this.setCreatedTime(space.getCreatedTime());
    this.setDescription(space.getDescription());
    this.setDisplayName(space.getDisplayName());
    this.setGroupId(space.getGroupId());
    this.setPrettyName(space.getPrettyName());
    this.setPriority(space.getPriority());
    this.setRegistration(space.getRegistration());
    this.setUrl(space.getUrl());
    this.setVisibility(space.getVisibility());
    buildMembers(space);
    return this;
  }

  public String[] getPendingMembersId() {
    return getUserIds(Status.PENDING);
  }

  public String[] getInvitedMembersId() {
    return getUserIds(Status.INVITED);
  }

  public String[] getMembersId() {
    return getUserIds(Status.MEMBER);
  }

  public String[] getManagerMembersId() {
    return getUserIds(Status.MANAGER);
  }

  private void buildMembers(Space space) {
    List<SpaceMember> invited = this.getMembers(Status.INVITED);
    merge(invited, space.getInvitedUsers(), Status.INVITED);

    List<SpaceMember> manager = this.getMembers(Status.MANAGER);
    merge(manager, space.getManagers(), Status.MANAGER);

    List<SpaceMember> member = this.getMembers(Status.MEMBER);
    merge(member, space.getMembers(), Status.MEMBER);

    List<SpaceMember> pending = this.getMembers(Status.PENDING);
    merge(pending, space.getPendingUsers(), Status.PENDING);
  }

  private void merge(List<SpaceMember> spaceMembers, String[] userIds, Status status) {
    Set<String> ids = new HashSet<>(userIds != null ? Arrays.asList(userIds) : Collections.<String>emptyList());

    Iterator<SpaceMember> mems = spaceMembers.iterator();
    while (mems.hasNext()) {
      SpaceMember mem = mems.next();
      String id = mem.getUserId();

      if (ids.contains(mem.getUserId())) {
        ids.remove(id);
      } else {
        this.getMembers().remove(mem);
      }
    }

    for (String id : ids) {
      this.getMembers().add(new SpaceMember(this, id, status));
    }
  }

  private List<SpaceMember> getMembers(Status status) {
    List<SpaceMember> mems = new LinkedList<>();
    for (SpaceMember mem : getMembers()) {
      if (mem.getStatus().equals(status)) {
        mems.add(mem);
      }
    }
    return mems;
  }

  private String[] getUserIds(Status status) {
    List<String> ids = new LinkedList<>();
    for (SpaceMember mem : getMembers(status)) {
      ids.add(mem.getUserId());
    }
    return ids.toArray(new String[ids.size()]);
  }
}
