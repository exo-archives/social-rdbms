package org.exoplatform.social.addons.storage.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.social.core.relationship.model.Relationship.Type;

@Entity
@ExoEntity
@Table(name = "SOC_CONNECTIONS")
@NamedQuery(name = "getRelationships",
            query = "select r from Connection r")
public class Connection {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "CONNECTION_ID")
  private Long id;

  @Column(length = 36)
  private String senderId;
  
  @Column(length = 36)
  private String receiverId;
  
  @Enumerated
  private Type status;

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
    this.status = status;
  }
}
