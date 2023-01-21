package com.synopsys.integration.alert.database.job.execution;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.synopsys.integration.alert.database.BaseEntity;

@Entity
@Table(schema = "alert", name = "job_completion_durations")
public class JobCompletionStatusDurationsEntity extends BaseEntity {
    private static final long serialVersionUID = 8165889267435905900L;
    @Id
    @Column(name = "job_config_id")
    private UUID jobConfigId;

    @Column(name = "job_duration_nanoseconds")
    private Long jobDurationNanosec;

    @Column(name = "notification_processing_duration_nanoseconds")
    private Long notificationProcessingDuration;

    @Column(name = "channel_processing_duration_nanoseconds")
    private Long channelProcessingDuration;

    @Column(name = "issue_creation_duration_nanoseconds")
    private Long issueCreationDuration;

    @Column(name = "issue_commenting_duration_nanoseconds")
    private Long issueCommentingDuration;

    @Column(name = "issue_resolving_duration_nanoseconds")
    private Long issueTransitionDuration;

    public JobCompletionStatusDurationsEntity() {
        //default constructor for JPA
    }

    public JobCompletionStatusDurationsEntity(
        UUID jobConfigId,
        Long jobDurationNanosec,
        Long notificationProcessingDuration,
        Long channelProcessingDuration,
        Long issueCreationDuration,
        Long issueCommentingDuration,
        Long issueTransitionDuration
    ) {
        this.jobConfigId = jobConfigId;
        this.jobDurationNanosec = jobDurationNanosec;
        this.notificationProcessingDuration = notificationProcessingDuration;
        this.channelProcessingDuration = channelProcessingDuration;
        this.issueCreationDuration = issueCreationDuration;
        this.issueCommentingDuration = issueCommentingDuration;
        this.issueTransitionDuration = issueTransitionDuration;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public Long getJobDurationNanosec() {
        return jobDurationNanosec;
    }

    public Long getNotificationProcessingDuration() {
        return notificationProcessingDuration;
    }

    public Long getChannelProcessingDuration() {
        return channelProcessingDuration;
    }

    public Long getIssueCreationDuration() {
        return issueCreationDuration;
    }

    public Long getIssueCommentingDuration() {
        return issueCommentingDuration;
    }

    public Long getIssueTransitionDuration() {
        return issueTransitionDuration;
    }
}
