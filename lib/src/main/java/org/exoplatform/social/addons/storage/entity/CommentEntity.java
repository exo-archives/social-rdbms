package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity(name = "SocComment")
@ExoEntity
@Table(name = "SOC_COMMENTS")
@NamedQueries({
        @NamedQuery(name = "SocComment.migratePosterId", query = "UPDATE SocComment c SET c.posterId = :newId WHERE c.posterId = :oldId"),
        @NamedQuery(name = "SocComment.migrateOwnerId", query = "UPDATE SocComment c SET c.ownerId = :newId WHERE c.ownerId = :oldId")
})
public class CommentEntity extends BaseActivity {

  private static final long serialVersionUID = 8385677663290737181L;

  @Id
  @SequenceGenerator(name="SEQ_SOC_COMMENTS_ID", sequenceName="SEQ_SOC_COMMENTS_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_SOC_COMMENTS_ID")
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
  @JoinColumn(name="ACTIVITY_ID", nullable = false)
  private ActivityEntity activity;

  public CommentEntity() {
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

  public ActivityEntity getActivity() {
    return activity;
  }

  public void setActivity(ActivityEntity activity) {
    this.activity = activity;
  }

  @Override
  public String toString() {
    return "Comment[id = " + id + ",owner = " + getOwnerId() + ",title = " + getTitle() + "]";
  }
}
