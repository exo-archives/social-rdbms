package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.social.core.relationship.model.Relationship.Type;

import javax.persistence.*;

@Entity(name = "SocConnection")
@ExoEntity
@Table(name = "SOC_CONNECTIONS",
       uniqueConstraints=@UniqueConstraint(columnNames = {"SENDER_ID", "RECEIVER_ID"}))
@NamedQueries({
        @NamedQuery(name = "getRelationships",
                query = "select r from SocConnection r"),
        @NamedQuery(name = "SocConnection.deleteConnectionByIdentity",
                query = "DELETE FROM SocConnection c WHERE c.senderId = :identityId OR c.receiverId = :identityId"),
        @NamedQuery(name = "SocConnection.migrateSenderId", query = "UPDATE SocConnection c SET c.senderId = :newId WHERE c.senderId = :oldId"),
        @NamedQuery(name = "SocConnection.migrateReceiverId", query = "UPDATE SocConnection c SET c.receiverId = :newId WHERE c.receiverId = :oldId")
})
public class Connection {

  @Id
  @SequenceGenerator(name="SEQ_SOC_CONNECTIONS_ID", sequenceName="SEQ_SOC_CONNECTIONS_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_SOC_CONNECTIONS_ID")
  @Column(name = "CONNECTION_ID")
  private Long id;

  @Column(name="SENDER_ID", length = 36, nullable = false)
  private String senderId;
  
  @Column(name="RECEIVER_ID", length = 36, nullable = false)
  private String receiverId;
  
  @Enumerated
  @Column(name="STATUS", nullable = false)
  private Type status;
  
  /** */
  @Column(name="LAST_UPDATED", nullable = false)
  private Long lastUpdated = System.currentTimeMillis();

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
    if (status == Type.ALL) {
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
