package com.synopsys.integration.alert.component.diagnostic.model;

import java.util.Optional;

public class JobDurationDiagnosticModel {

    private final String jobDuration;
    private final String notificationProcessingDuration;
    private final String channelProcessingDuration;
    private final String issueCreationDuration;
    private final String issueCommentingDuration;
    private final String issueTransitionDuration;

    public JobDurationDiagnosticModel(
        String jobDuration,
        String notificationProcessingDuration,
        String channelProcessingDuration,
        String issueCreationDuration,
        String issueCommentingDuration,
        String issueTransitionDuration
    ) {
        this.jobDuration = jobDuration;
        this.notificationProcessingDuration = notificationProcessingDuration;
        this.channelProcessingDuration = channelProcessingDuration;
        this.issueCreationDuration = issueCreationDuration;
        this.issueCommentingDuration = issueCommentingDuration;
        this.issueTransitionDuration = issueTransitionDuration;
    }

    public String getJobDuration() {
        return jobDuration;
    }

    public Optional<String> getNotificationProcessingDuration() {
        return Optional.ofNullable(notificationProcessingDuration);
    }

    public Optional<String> getChannelProcessingDuration() {
        return Optional.ofNullable(channelProcessingDuration);
    }

    public Optional<String> getIssueCreationDuration() {
        return Optional.ofNullable(issueCreationDuration);
    }

    public Optional<String> getIssueCommentingDuration() {
        return Optional.ofNullable(issueCommentingDuration);
    }

    public Optional<String> getIssueTransitionDuration() {
        return Optional.ofNullable(issueTransitionDuration);
    }
}
