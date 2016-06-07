package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Created by bdechateauvieux on 3/25/15.
 */
@MappedSuperclass
@ExoEntity
@SuppressWarnings("serial")
public abstract class BaseActivity implements Serializable {
  
  /** */
  @Column(name="TITLE", nullable = false)
  private String title;
  
  /** */
  @Column(name="TYPE")
  private String type;
  
  /** */
  @Column(name="TITLE_ID")
  private String titleId;
  
  /** */
  @Column(name="POSTED", nullable = false)
  protected Long posted;
  
  /** */
  @Column(name="LAST_UPDATED", nullable = false)
  private Long lastUpdated;
  
  /** */
  @Column(name="POSTER_ID")
  private String posterId;// creator
  
  /** */
  @Column(name="OWNER_ID")
  private String ownerId;// owner of stream
  
  /** */
  @Column(name="PERMALINK")
  private String permaLink;
  
  /** */
  @Column(name="APP_ID")
  private String appId;
  
  /** */
  @Column(name="EXTERNAL_ID")
  private String externalId;
  
  /** */
  @Column(name="LOCKED", nullable = false)
  private Boolean locked = false;
  
  /** */
  @Column(name="HIDDEN", nullable = false)
  private Boolean hidden = false;
  
  @Column(name="BODY", length = 2000)
  private String body;

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
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
}
