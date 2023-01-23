package com.synopsys.integration.alert.common.persistence.model.job.executions;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobExecutionModel extends AlertSerializableModel {

    private static final long serialVersionUID = 7623899926563701244L;
    private final UUID executionId;
    private final UUID jobConfigId;
    private final OffsetDateTime start;
    private final OffsetDateTime end;
    private final String status;
    private final int processedNotificationCount;
    private final int totalNotificationCount;

    public JobExecutionModel(
        UUID executionId,
        UUID jobConfigId,
        OffsetDateTime start,
        OffsetDateTime end,
        String status,
        int processedNotificationCount,
        int totalNotificationCount
    ) {
        this.executionId = executionId;
        this.jobConfigId = jobConfigId;
        this.start = start;
        this.end = end;
        this.status = status;
        this.processedNotificationCount = processedNotificationCount;
        this.totalNotificationCount = totalNotificationCount;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public String getStatus() {
        return status;
    }

    public int getProcessedNotificationCount() {
        return processedNotificationCount;
    }

    public int getTotalNotificationCount() {
        return totalNotificationCount;
    }
}
