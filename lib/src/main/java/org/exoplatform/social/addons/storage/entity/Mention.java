package org.exoplatform.social.addons.storage.entity;

import javax.persistence.*;

/**
 * Created by bdechateauvieux on 7/7/15.
 */
@Entity
@Table(name="SOC_MENTIONS")
public class Mention {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="MENTION_ID")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ACTIVITY_ID")
    private Activity activity;

    @Column(name="MENTIONER_ID")
    private String mentionId;

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

    public String getMentionId() {
        return mentionId;
    }

    public void setMentionId(String mentionId) {
        this.mentionId = mentionId;
    }
}
