package org.exoplatform.social.addons.storage.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class LikerEntity implements Serializable {
  
  private static final long serialVersionUID = 8345954949487709759L;

  @Column(name = "LIKER_ID", nullable = false)
  private String likerId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_DATE", nullable = false)
  private Date   createdDate = new Date();

  public LikerEntity() {
    this(null);
  }

  public LikerEntity(String likerId) {
    this.likerId = likerId;
  }

  public String getLikerId() {
    return likerId;
  }

  public void setLikerId(String likerId) {
    this.likerId = likerId;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((likerId == null) ? 0 : likerId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LikerEntity other = (LikerEntity) obj;
    if (likerId == null) {
      if (other.likerId != null)
        return false;
    } else if (!likerId.equals(other.likerId))
      return false;
    return true;
  }

}
