package org.exoplatform.social.addons.storage.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.social.core.relationship.model.Relationship.Type;

@Entity
@ExoEntity
@Table(name = "SOC_CONNECTIONS",
       uniqueConstraints=@UniqueConstraint(columnNames = {"SENDER_ID", "RECEIVER_ID"}))
@NamedQuery(name = "getRelationships",
            query = "select r from Connection r")
public class Connection {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "CONNECTION_ID")
  private Long id;

  @Column(name="SENDER_ID", length = 36)
  private String senderId;
  
  @Column(name="RECEIVER_ID", length = 36)
  private String receiverId;
  
  @Enumerated
  @Column(name="STATUS")
  private Type status;
  
  /** */
  @Column(name="LAST_UPDATED")
  private Long lastUpdated;

  public Connection() {
  }

  public Connection(String senderId, String receiverId, Type status) {
    this.senderId = senderId;
    this.receiverId = receiverId;
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(String receiverId) {
    this.receiverId = receiverId;
  }

  public Type getStatus() {
    return status;
  }

  public void setStatus(Type status) {
    if ((status == Type.ALL) || (status == Type.IGNORED) || (status == Type.PENDING)) {
      throw new IllegalArgumentException("Illegal status ["+status+"]");
    }
    this.status = status;
  }

  public Long getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
