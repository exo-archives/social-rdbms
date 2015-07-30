package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity
@ExoEntity
@Table(name = "SOC_COMMENTS")
@NamedQuery(
  name = "getActivityByComment",
  query = "select a from Activity a join a.comments Comment where Comment.id = :COMMENT_ID"
)
public class Comment extends BaseActivity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name="COMMENT_ID")
  private Long id;

  @ElementCollection
  @JoinTable(
    name = "SOC_COMMENT_TEMPLATE_PARAMS",
    joinColumns=@JoinColumn(name = "COMMENT_ID")
  )
  @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
  @Column(name="TEMPLATE_PARAM_VALUE", length = 1024)
  private Map<String, String> templateParams;

  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="ACTIVITY_ID")
  private Activity activity;

  public Comment() {
    setPosted(new Date().getTime());
    setLastUpdated(new Date().getTime());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Map<String, String> getTemplateParams() {
    return templateParams;
  }

  public void setTemplateParams(Map<String, String> templateParams) {
    this.templateParams = templateParams;
  }

  public Activity getActivity() {
    return activity;
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  @Override
  public String toString() {
    return "Comment[id = " + id + ",owner = " + getOwnerId() + ",title = " + getTitle() + "]";
  }
}
