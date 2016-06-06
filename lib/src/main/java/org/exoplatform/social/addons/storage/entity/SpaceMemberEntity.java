package org.exoplatform.social.addons.storage.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "SocSpaceMember")
@ExoEntity
@Table(name = "SOC_SPACES_MEMBERS")
@NamedQueries({
    @NamedQuery(name = "SpaceMember.deleteBySpace", query = "DELETE FROM SocSpaceMember mem WHERE mem.space.id = :spaceId"),
    @NamedQuery(name = "SpaceMember.getMember", query = "SELECT mem FROM SocSpaceMember mem WHERE mem.userId = :userId AND mem.space.id = :spaceId AND mem.status = :status"),
    @NamedQuery(name = "SpaceMember.deleteByUsername", query = "DELETE FROM SocSpaceMember sm WHERE sm.userId = :username")})
public class SpaceMemberEntity implements Serializable {

  private static final long serialVersionUID = 1015703779692801839L;

  @Id
  @SequenceGenerator(name = "SEQ_SOC_SPACE_MEMBER_ID", sequenceName = "SEQ_SOC_SPACE_MEMBER_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_SOC_SPACE_MEMBER_ID")
  @Column(name = "SPACE_MEMBER_ID")
  private Long              id;

  @ManyToOne
  @JoinColumn(name = "SPACE_ID", nullable = false)
  private SpaceEntity       space;

  @Column(name = "USER_ID", length = 100, nullable = false)
  private String            userId;

  @Column(name = "STATUS", length = 36, nullable = false)
  private Status            status;

  @Column(name = "LAST_ACCESS")
  private long              lastAccess;

  @Column(name = "VISITED")
  private boolean           visited;

  public SpaceMemberEntity() {
    this(null, null, null);
  }

  public SpaceMemberEntity(SpaceEntity space, String userId, Status status) {
    this.setSpace(space);
    this.setUserId(userId);
    this.setStatus(status);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public SpaceEntity getSpace() {
    return space;
  }

  public void setSpace(SpaceEntity space) {
    this.space = space;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public long getLastAccess() {
    return lastAccess;
  }

  public void setLastAccess(long lastAccess) {
    this.lastAccess = lastAccess;
  }

  public boolean isVisited() {
    return visited;
  }

  public void setVisited(boolean visited) {
    this.visited = visited;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SpaceMemberEntity that = (SpaceMemberEntity) o;

    if (!space.equals(that.space)) return false;
    if (!userId.equals(that.userId)) return false;
    return status == that.status;

  }

  @Override
  public int hashCode() {
    int result = space.hashCode();
    result = 31 * result + userId.hashCode();
    result = 31 * result + status.hashCode();
    return result;
  }

  public static enum Status {
    MEMBER, MANAGER, PENDING, INVITED;
  }
}
