package org.exoplatform.social.addons.storage.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.json.JSONObject;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity
@Table(name = "SOC_ACTIVITIES")
public class Activity extends BaseActivity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name="ACTIVITY_ID")
  private Long id;
  
  /** */
  private String type;

  @ElementCollection
  @CollectionTable(
    name = "SOC_ACTIVITY_LIKERS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @Column(name="LIKER_ID")
  private Set<String> likerIds = new HashSet<String>();
  
  @ElementCollection
  @CollectionTable(
    name = "SOC_ACTIVITY_MENTIONERS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @Column(name="MENTIONER_ID")
  private Set<String> mentionerIds = new HashSet<String>();

  @ElementCollection
  @JoinTable(
    name = "SOC_ACTIVITY_TEMPLATE_PARAMS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
  @Column(name="TEMPLATE_PARAM_VALUE")
  private Map<String, String> templateParams = new LinkedHashMap<String, String>();

  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  @OrderBy("posted DESC")
  private List<Comment> comments;
  
  /** */
  @Column(length = 36)
  private String providerId;
  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  @OrderBy("id DESC")
  private List<StreamItem> streamItems;

  /** */
  public Activity() {
    setPosted(new Date().getTime());
    setLastUpdated(new Date().getTime());
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void addLiker(String likerId) {
    this.likerIds.add(likerId);
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getLikerIds() {
    return likerIds;
  }

  public void setLikerIds(Set<String> likerIds) {
    this.likerIds = likerIds;
  }
  
  public Set<String> getMentionerIds() {
    return mentionerIds;
  }

  public void setMentionerIds(Set<String> mentionerIds) {
    this.mentionerIds = mentionerIds;
  }

  public Map<String, String> getTemplateParams() {
    return templateParams;
  }

  public void setTemplateParams(Map<String, String> templateParams) {
    this.templateParams = templateParams;
  }

  public List<Comment> getComments() {
    return comments;
  }

  public void setComments(List<Comment> comments) {
    this.comments = comments;
  }

  /**
   * Adds the comment item entity to this activity
   * @param item the stream item
   */
  public void addComment(Comment comment) {
    if (this.comments == null) {
      this.comments = new ArrayList<Comment>();
    }
    comment.setActivity(this);
    this.comments.add(comment);
  }
  
  public List<StreamItem> getStreamItems() {
    return streamItems;
  }

  public void setStreamItems(List<StreamItem> streamItems) {
    this.streamItems = streamItems;
  }

  /**
   * Adds the stream item entity to this activity
   * @param item the stream item
   */
  public void addStreamItem(StreamItem item) {
    if (this.streamItems == null) {
      this.streamItems = new ArrayList<StreamItem>();
    }
    item.setActivity(this);
    this.streamItems.add(item);
  }

  public Long getId() {
    return id;
  }

  public String getProviderId() {
    return providerId;
  }
  
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }
  
  @Override
  public String toString() {
    return new JSONObject(this).toString();
  }
}
