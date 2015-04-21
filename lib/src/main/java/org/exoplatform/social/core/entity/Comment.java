package org.exoplatform.social.core.entity;

import javax.persistence.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity
@Table(name = "SOC_COMMENTS")
public class Comment extends BaseActivity{
  @Id
  @GeneratedValue
  @Column(name="COMMENT_ID")
  private Long id;

  @ElementCollection
  @CollectionTable(
    name = "SOC_COMMENT_LIKERS",
    joinColumns=@JoinColumn(name = "COMMENT_ID")
  )
  @Column(name="LIKER_ID")
  private List<String> likerIds;

  @ElementCollection
  @JoinTable(
    name = "SOC_COMMENT_TEMPLATE_PARAMS",
    joinColumns=@JoinColumn(name = "COMMENT_ID")
  )
  @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
  @Column(name="TEMPLATE_PARAM_VALUE")
  private Map<String, String> templateParams;

  public Comment() {
    setPosted(new Date());
    setLastUpdated(new Date());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public List<String> getLikerIds() {
    return likerIds;
  }

  public void setLikerIds(List<String> likerIds) {
    this.likerIds = likerIds;
  }

  public Map<String, String> getTemplateParams() {
    return templateParams;
  }

  public void setTemplateParams(Map<String, String> templateParams) {
    this.templateParams = templateParams;
  }
}
