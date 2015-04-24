package org.exoplatform.social.core.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.json.JSONObject;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity
@Table(name = "SOC_ACTIVITIES")
@EntityListeners({Activity.ActivityEntityListener.class})
@NamedQueries({
  @NamedQuery(
    name = "getActivitiesByLikerId",
    query = "select a from Activity a join a.likerIds likers where likers = :likerId"
  )
})
public class Activity extends BaseActivity {
  @Id
//    @GeneratedValue
//    @GeneratedValue(generator="system-uuid")
//    @GenericGenerator(name="system-uuid", strategy = "uuid2")
  @Column(name="ACTIVITY_ID", length=36)
//    private Long id;
  private String id;

  private String type;

  @ElementCollection
  @CollectionTable(
    name = "SOC_ACTIVITY_LIKERS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )

  @Column(name="LIKER_ID")
  private Set<String> likerIds = new HashSet<String>();

  @ElementCollection
  @JoinTable(
    name = "SOC_ACTIVITY_TEMPLATE_PARAMS",
    joinColumns=@JoinColumn(name = "ACTIVITY_ID")
  )
  @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
  @Column(name="TEMPLATE_PARAM_VALUE")
  private Map<String, String> templateParams;

  @OneToMany(cascade=CascadeType.ALL, mappedBy="activity", fetch=FetchType.LAZY)
  @OrderBy("posted DESC")
  private List<Comment> comments;

  public Activity() {
    setPosted(new Date());
    setLastUpdated(new Date());
  }

  public void setId(String id) {
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

  public void addComment(Comment comment) {
    if (this.comments == null) {
      this.comments = new ArrayList<Comment>();
    }
    comment.setActivity(this);
    this.comments.add(comment);
  }

  public String getId() {
    return id;
  }

  public static class ActivityEntityListener {
    @PrePersist
    public void onPrePersist(Activity activity) {
      if(activity.getId() == null) {
        activity.setId(UUID.randomUUID().toString());
      }
    }
  }

  @Override
  public String toString() {
    return new JSONObject(this).toString();
  }
}
