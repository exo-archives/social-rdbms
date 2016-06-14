package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

/**
 * Created by bdechateauvieux on 7/7/15.
 */
@Entity(name = "SocMention")
@ExoEntity
@Table(name="SOC_MENTIONS")
@NamedQueries({
        @NamedQuery(name = "SocMention.migrateMentionId",
                query = "UPDATE SocMention m SET m.mentionId = :newId WHERE m.mentionId = :oldId"),
        @NamedQuery(name = "SocMention.selectMentionByOldId",
                query = "SELECT m FROM SocMention m WHERE m.mentionId LIKE :oldId"),
})
public class MentionEntity {

  @Id
  @SequenceGenerator(name="SEQ_SOC_MENTIONS_ID", sequenceName="SEQ_SOC_MENTIONS_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_SOC_MENTIONS_ID")
  @Column(name="MENTION_ID")
  private Long id;

  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="ACTIVITY_ID", nullable = false)
  private ActivityEntity activity;

  @Column(name="MENTIONER_ID", nullable = false)
  private String mentionId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ActivityEntity getActivity() {
    return activity;
  }

  public void setActivity(ActivityEntity activity) {
    this.activity = activity;
  }

  public String getMentionId() {
    return mentionId;
  }

  public void setMentionId(String mentionId) {
    this.mentionId = mentionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MentionEntity that = (MentionEntity) o;

    if (!activity.equals(that.activity)) return false;
    return mentionId.equals(that.mentionId);

  }

  @Override
  public int hashCode() {
    int result = activity.hashCode();
    result = 31 * result + mentionId.hashCode();
    return result;
  }
}
