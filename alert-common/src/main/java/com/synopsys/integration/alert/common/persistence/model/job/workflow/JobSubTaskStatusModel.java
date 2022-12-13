package com.synopsys.integration.alert.common.persistence.model.job.workflow;

import java.util.UUID;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobSubTaskStatusModel extends AlertSerializableModel {
    private static final long serialVersionUID = -9192758155312295976L;
    private final UUID jobExecutionId;
    private final UUID jobId;
    private final Long remainingTaskCount;
    private final UUID notificationCorrelationId;

    public JobSubTaskStatusModel(UUID jobExecutionId, UUID jobId, Long remainingTaskCount, UUID notificationCorrelationId) {
        this.jobExecutionId = jobExecutionId;
        this.jobId = jobId;
        this.remainingTaskCount = remainingTaskCount;
        this.notificationCorrelationId = notificationCorrelationId;
    }

    public UUID getJobExecutionId() {
        return jobExecutionId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public Long getRemainingTaskCount() {
        return remainingTaskCount;
    }

    public UUID getNotificationCorrelationId() {
        return notificationCorrelationId;
    }
}
