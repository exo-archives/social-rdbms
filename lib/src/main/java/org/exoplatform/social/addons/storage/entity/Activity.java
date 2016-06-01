package org.exoplatform.social.addons.storage.entity;

import java.util.*;

import javax.persistence.*;

import org.json.JSONObject;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity(name = "SocActivity")
@ExoEntity
@Table(name = "SOC_ACTIVITIES")
@NamedQueries({
        @NamedQuery(
                name = "getActivityByComment",
                query = "select a from SocActivity a join a.comments Comment where Comment.id = :COMMENT_ID"
        ),
        @NamedQuery(name = "SocActivity.migratePosterId", query = "UPDATE SocActivity a SET a.posterId = :newId WHERE a.posterId = :oldId"),
        @NamedQuery(name = "SocActivity.migrateOwnerId", query = "UPDATE SocActivity a SET a.ownerId = :newId WHERE a.ownerId = :oldId")
})
public class Activity extends BaseActivity {

  private static final long serialVersionUID = -1489894321243127979L;

  @Id
  @SequenceGenerator(name="SEQ_SOC_ACTIVITIES_ID", sequenceName="SEQ_SOC_ACTIVITIES_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_SOC_ACTIVITIES_ID")
  @Column(name="ACTIVITY_ID")
  private Long id;
  
  @ElementCollection
  @CollectionTable(
    name = "SOC_ACTIVITY_LIKERS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @Column(name="LIKER_ID", nullable = false)
  private Set<String> likerIds = new HashSet<String>();

  @ElementCollection
  @JoinTable(
    name = "SOC_ACTIVITY_TEMPLATE_PARAMS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
  @Column(name="TEMPLATE_PARAM_VALUE", length = 1024)
  private Map<String, String> templateParams = new LinkedHashMap<String, String>();

  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  private List<Comment> comments;

  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  private List<Mention> mentions;

  /** */
  @Column(name="PROVIDER_ID", length = 36)
  private String providerId;
  
  /** */
  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
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

  public Set<String> getLikerIds() {
    return likerIds;
  }

  public void setLikerIds(Set<String> likerIds) {
    this.likerIds = likerIds;
  }

  public Set<String> getMentionerIds() {
    Set<String> result = new HashSet<String>();
    if (this.mentions!=null) {
      for (Mention mention : this.mentions) {
        result.add(mention.getMentionId());
      }
    }
    return result;
  }

  public void setMentionerIds(Set<String> mentionerIds) {
    if (this.mentions==null) {
      this.mentions = new ArrayList<Mention>();
    }
    this.mentions.clear();
    for (String mentionerId : mentionerIds) {
      addMention(mentionerId);
    }
  }

  private void addMention(String mentionerId) {
    if (this.mentions==null) {
      this.mentions = new ArrayList<Mention>();
    }
    Mention mention = new Mention();
    mention.setMentionId(mentionerId);
    mention.setActivity(this);
    this.mentions.add(mention);
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
  
  public void removeStreamItem(StreamItem item) {
    for (StreamItem it : this.getStreamItems()) {
      if (it.getOwnerId().equals(item.getOwnerId()) && it.getStreamType().equals(item.getStreamType())) {
        this.streamItems.remove(it);
        break;
      }
    }
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
