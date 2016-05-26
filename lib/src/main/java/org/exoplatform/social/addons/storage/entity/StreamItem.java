package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

/**
 * Created by bdechateauvieux on 3/26/15.
 */
@Entity
@ExoEntity
@Table(name = "SOC_STREAM_ITEMS")
@NamedQueries({
        @NamedQuery(name = "SocStreamItem.migrateOwner", query = "UPDATE StreamItem s SET s.ownerId = :newId WHERE s.ownerId = :oldId"),
        @NamedQuery(name = "getStreamByActivityId", query = "select s from StreamItem s join s.activity A where A.id = :activityId")
})
public class StreamItem {

  @Id
  @SequenceGenerator(name="SEQ_SOC_STREAM_ITEMS_ID", sequenceName="SEQ_SOC_STREAM_ITEMS_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_SOC_STREAM_ITEMS_ID")
  @Column(name = "STREAM_ITEM_ID")
  private Long id;

  @OneToOne
  @JoinColumn(name = "ACTIVITY_ID", nullable = false)
  private Activity activity;

  @Column(name = "ACTIVITY_ID", insertable=false, updatable=false)
  private Long activityId;
  
  /**
   * This is id's Identity owner of ActivityStream or SpaceStream
   */
  @Column(name="OWNER_ID", length = 36, nullable = false)
  private String ownerId;
  
  /** */
  @Column(name="LAST_UPDATED", nullable = false)
  private Long lastUpdated;

  @Enumerated
  @Column(name="STREAM_TYPE", nullable = false)
  private StreamType streamType;

  public StreamItem() {
  }

  public StreamItem(StreamType streamType) {
    this.streamType = streamType;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Activity getActivity() {
    return activity;
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public StreamType getStreamType() {
    return streamType;
  }

  public void setStreamType(StreamType streamType) {
    this.streamType = streamType;
  }
  
  public Long getLastUpdated() {
    return lastUpdated;
  }
  
  public void setLastUpdated(Long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Long getActivityId() {
    return activityId;
  }

  public void setActivityId(Long activityId) {
    this.activityId = activityId;
  }
  
}
