package com.synopsys.integration.alert.api.distribution.audit;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.synopsys.integration.alert.api.event.AlertEvent;

public class AuditEvent extends AlertEvent {
    private static final long serialVersionUID = 8821840075948290969L;
    private final UUID jobExecutionId;
    private final Set<Long> notificationIds;
    private final Instant createdTimestamp;

    public AuditEvent(String destination, UUID jobExecutionId, Set<Long> notificationIds) {
        this(destination, jobExecutionId, notificationIds, Instant.now());
    }

    public AuditEvent(String destination, UUID jobExecutionId, Set<Long> notificationIds, Instant createdTimestamp) {
        super(destination);
        this.jobExecutionId = jobExecutionId;
        this.notificationIds = notificationIds;
        this.createdTimestamp = createdTimestamp;
    }

    public UUID getJobExecutionId() {
        return jobExecutionId;
    }

    public Set<Long> getNotificationIds() {
        return notificationIds;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }
}
