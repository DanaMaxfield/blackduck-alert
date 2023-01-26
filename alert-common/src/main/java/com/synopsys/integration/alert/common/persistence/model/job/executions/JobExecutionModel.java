package com.synopsys.integration.alert.common.persistence.model.job.executions;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;
import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;

public class JobExecutionModel extends AlertSerializableModel {

    private static final long serialVersionUID = 7623899926563701244L;
    private final UUID executionId;
    private final UUID jobConfigId;
    private final OffsetDateTime start;
    private final OffsetDateTime end;
    private final AuditEntryStatus status;
    private final int processedNotificationCount;
    private final int totalNotificationCount;
    private final boolean completionCounted;

    public JobExecutionModel(
        UUID executionId,
        UUID jobConfigId,
        OffsetDateTime start,
        OffsetDateTime end,
        AuditEntryStatus status,
        int processedNotificationCount,
        int totalNotificationCount,
        boolean completionCounted
    ) {
        this.executionId = executionId;
        this.jobConfigId = jobConfigId;
        this.start = start;
        this.end = end;
        this.status = status;
        this.processedNotificationCount = processedNotificationCount;
        this.totalNotificationCount = totalNotificationCount;
        this.completionCounted = completionCounted;
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

    public Optional<OffsetDateTime> getEnd() {
        return Optional.ofNullable(end);
    }

    public AuditEntryStatus getStatus() {
        return status;
    }

    public int getProcessedNotificationCount() {
        return processedNotificationCount;
    }

    public int getTotalNotificationCount() {
        return totalNotificationCount;
    }

    public boolean isCompletionCounted() {
        return completionCounted;
    }
}
