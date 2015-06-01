package org.exoplatform.social.addons.storage.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Created by bdechateauvieux on 3/25/15.
 */
@MappedSuperclass
@SuppressWarnings("serial")
public abstract class BaseActivity implements Serializable {
  
  /** */
  @Column(length = 2000)
  private String title;
  
  /** */
  @Column(length = 36)
  private String titleId;
  
  /** */
  protected Long posted;
  
  /** */
  private Long lastUpdated;
  
  /** */
  @Column(length = 36)
  private String posterId;// creator
  
  /** */
  @Column(length = 36)
  private String ownerId;// owner of stream
  
  /** */
  @Column(length = 255)
  private String permaLink;
  
  /** */
  @Column(length = 36)
  private String appId;
  
  /** */
  @Column(length = 36)
  private String externalId;
  
  /** */
  private Boolean locked = false;
  
  /** */
  private Boolean hidden = false;

  @Deprecated
  @Column(length = 2000)
  private String body;
  @Deprecated
  @Column(length = 36)
  private String bodyId;
  @Deprecated
  private float priority;

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getTitleId() {
    return titleId;
  }
  public void setTitleId(String titleId) {
    this.titleId = titleId;
  }
  public Long getPosted() {
    return posted;
  }
  public void setPosted(Long posted) {
    this.posted = posted;
  }
  public Long getLastUpdated() {
    return lastUpdated;
  }
  public void setLastUpdated(Long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
  public String getPosterId() {
    return posterId;
  }
  public void setPosterId(String posterId) {
    this.posterId = posterId;
  }
  public String getOwnerId() {
    return ownerId;
  }
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }
  public String getPermaLink() {
    return permaLink;
  }
  public void setPermaLink(String permaLink) {
    this.permaLink = permaLink;
  }
  public String getAppId() {
    return appId;
  }
  public void setAppId(String appId) {
    this.appId = appId;
  }
  public String getExternalId() {
    return externalId;
  }
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }
  public Boolean getLocked() {
    return locked;
  }
  public void setLocked(Boolean locked) {
    this.locked = locked;
  }
  public Boolean getHidden() {
    return hidden;
  }
  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }
  public String getBody() {
    return body;
  }
  public void setBody(String body) {
    this.body = body;
  }
  public float getPriority() {
    return priority;
  }
  public void setPriority(float priority) {
    this.priority = priority;
  }
}
