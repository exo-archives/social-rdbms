package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.social.core.relationship.model.Relationship.Type;

import javax.persistence.*;

@Entity
@ExoEntity
@Table(name = "SOC_CONNECTIONS",
       uniqueConstraints=@UniqueConstraint(columnNames = {"SENDER_ID", "RECEIVER_ID"}))
@NamedQueries({
        @NamedQuery(name = "getRelationships",
                query = "select r from Connection r"),
        @NamedQuery(name = "SocConnection.findConnectionBySenderAndReceiver",
                query = "SELECT c FROM Connection c WHERE c.sender.id = :sender AND c.receiver.id = :reciver"),
        @NamedQuery(name = "SocConnection.deleteConnectionByIdentity",
                query = "DELETE FROM Connection c WHERE c.sender.id = :identityId OR c.receiver.id = :identityId"),
        @NamedQuery(name = "SocConnection.migrateSenderId", query = "UPDATE Connection c SET c.sender.id = :newId WHERE c.sender.id = :oldId"),
        @NamedQuery(name = "SocConnection.migrateReceiverId", query = "UPDATE Connection c SET c.receiver.id = :newId WHERE c.receiver.id = :oldId")
})
public class Connection {

  @Id
  @SequenceGenerator(name="SEQ_SOC_CONNECTIONS_ID", sequenceName="SEQ_SOC_CONNECTIONS_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_SOC_CONNECTIONS_ID")
  @Column(name = "CONNECTION_ID")
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "SENDER_ID", referencedColumnName = "IDENTITY_ID")
  private IdentityEntity sender;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "RECEIVER_ID", referencedColumnName = "IDENTITY_ID")
  private IdentityEntity receiver;
  
  @Enumerated
  @Column(name="STATUS", nullable = false)
  private Type status;
  
  /** */
  @Column(name="LAST_UPDATED", nullable = false)
  private Long lastUpdated = System.currentTimeMillis();

  public Connection() {
  }

  public Connection(IdentityEntity sender, IdentityEntity receiver) {
    this.sender = sender;
    this.receiver = receiver;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IdentityEntity getSender() {
    return sender;
  }

  public void setSender(IdentityEntity sender) {
    this.sender = sender;
  }

  public IdentityEntity getReceiver() {
    return receiver;
  }

  public void setReceiver(IdentityEntity receiver) {
    this.receiver = receiver;
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
