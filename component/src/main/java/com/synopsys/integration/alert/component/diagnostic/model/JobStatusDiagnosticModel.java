package com.synopsys.integration.alert.component.diagnostic.model;

import java.util.UUID;

public class JobStatusDiagnosticModel {
    private final UUID jobConfigId;
    private final String jobName;
    private final Long notificationCount;
    private final Long successCount;
    private final Long failureCount;
    private final String latestStatus;
    private final String lastRun;

    private final JobDurationDiagnosticModel durations;

    public JobStatusDiagnosticModel(
        UUID jobConfigId,
        String jobName,
        Long notificationCount,
        Long successCount,
        Long failureCount,
        String latestStatus,
        String lastRun,
        JobDurationDiagnosticModel durations
    ) {
        this.jobConfigId = jobConfigId;
        this.jobName = jobName;
        this.notificationCount = notificationCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.latestStatus = latestStatus;
        this.lastRun = lastRun;
        this.durations = durations;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public String getJobName() {
        return jobName;
    }

    public Long getNotificationCount() {
        return notificationCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public String getLastRun() {
        return lastRun;
    }

    public JobDurationDiagnosticModel getDurations() {
        return durations;
    }
}
