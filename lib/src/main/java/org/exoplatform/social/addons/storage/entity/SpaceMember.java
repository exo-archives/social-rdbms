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

@Entity
@ExoEntity
@Table(name = "SOC_SPACES_MEMBERS")
@NamedQueries({
    @NamedQuery(name = "SpaceMember.deleteBySpace", query = "DELETE FROM SpaceMember mem WHERE mem.space.id = :spaceId") })
public class SpaceMember implements Serializable {

  private static final long serialVersionUID = 1015703779692801839L;

  @Id
  @SequenceGenerator(name = "SEQ_SOC_SPACE_MEMBER_ID", sequenceName = "SEQ_SOC_SPACE_MEMBER_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_SOC_SPACE_MEMBER_ID")
  @Column(name = "SPACE_MEMBER_ID")
  private Long              id;

  @ManyToOne
  @JoinColumn(name = "SPACE_ID")
  private SpaceEntity       space;

  @Column(name = "USER_ID", length = 36)
  private String            userId;

  @Column(name = "STATUS", length = 36)
  private Status            status;

  public SpaceMember() {
  }

  public SpaceMember(SpaceEntity space, String userId, Status status) {
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

  public static enum Status {
    MEMBER, MANAGER, PENDING, INVITED;
  }
}
