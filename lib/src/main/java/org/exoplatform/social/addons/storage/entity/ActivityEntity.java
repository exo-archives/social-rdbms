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
public class ActivityEntity extends BaseActivity {

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
  private Set<LikerEntity> likers = new HashSet<LikerEntity>();

  @ElementCollection
  @JoinTable(
    name = "SOC_ACTIVITY_TEMPLATE_PARAMS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
  @Column(name="TEMPLATE_PARAM_VALUE")
  private Map<String, String> templateParams = new LinkedHashMap<String, String>();

  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  private List<CommentEntity> comments;

  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  private Set<MentionEntity> mentions;

  /** */
  @Column(name="PROVIDER_ID")
  private String providerId;
  
  /** */
  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="activity", fetch=FetchType.LAZY)
  private List<StreamItemEntity> streamItems;

  /** */
  public ActivityEntity() {
    setPosted(new Date());
    setUpdatedDate(new Date());
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void addLiker(String likerId) {
    LikerEntity liker = new LikerEntity(likerId);
    if (!this.likers.contains(liker)) {
      this.likers.add(liker);
    }
  }

  public Set<LikerEntity> getLikers() {
    return likers;
  }

  public Set<String> getLikerIds() {
    Set<String> ids = new HashSet<String>();
    for (LikerEntity liker : likers) {
      ids.add(liker.getLikerId());      
    }
    return ids;
  }

  public void setLikerIds(Set<String> likerIds) {
    if (likerIds == null || likerIds.isEmpty()) {
      this.likers.clear();
    } else {
      //clean
      Iterator<LikerEntity> itor = likers.iterator();
      while (itor.hasNext()) {
        LikerEntity liker = itor.next();
        if (!likerIds.contains(liker.getLikerId())) {
          itor.remove();
        }
      }
      //add new
      for (String id : likerIds) {        
        addLiker(id);
      }
    }
  }

  public Set<String> getMentionerIds() {
    Set<String> result = new HashSet<String>();
    if (this.mentions!=null) {
      for (MentionEntity mention : this.mentions) {
        result.add(mention.getMentionId());
      }
    }
    return result;
  }

  public void setMentionerIds(Set<String> mentionerIds) {
    if (this.mentions==null) {
      this.mentions = new HashSet<>();
    }

    Set<String> mentionToAdd = new HashSet<>(mentionerIds);
    Set<MentionEntity> mentioned = new HashSet<>(this.mentions);
    for (MentionEntity m : mentioned) {
      if (!mentionerIds.contains(m.getMentionId())) {
        this.mentions.remove(m);
      } else {
        mentionToAdd.remove(m.getMentionId());
      }
    }

    for (String mentionerId : mentionToAdd) {
      addMention(mentionerId);
    }
  }

  private void addMention(String mentionerId) {
    if (this.mentions==null) {
      this.mentions = new HashSet<>();
    }
    MentionEntity mention = new MentionEntity();
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

  public List<CommentEntity> getComments() {
    return comments;
  }

  public void setComments(List<CommentEntity> comments) {
    this.comments = comments;
  }

  /**
   * Adds the comment item entity to this activity
   * @param item the stream item
   */
  public void addComment(CommentEntity comment) {
    if (this.comments == null) {
      this.comments = new ArrayList<CommentEntity>();
    }
    comment.setActivity(this);
    this.comments.add(comment);
  }
  
  public List<StreamItemEntity> getStreamItems() {
    return streamItems;
  }

  public void setStreamItems(List<StreamItemEntity> streamItems) {
    this.streamItems = streamItems;
  }

  /**
   * Adds the stream item entity to this activity
   * @param item the stream item
   */
  public void addStreamItem(StreamItemEntity item) {
    if (this.streamItems == null) {
      this.streamItems = new ArrayList<StreamItemEntity>();
    }
    item.setActivity(this);
    this.streamItems.add(item);
  }
  
  public void removeStreamItem(StreamItemEntity item) {
    for (StreamItemEntity it : this.getStreamItems()) {
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
