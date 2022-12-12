package com.synopsys.integration.alert.api.event.distribution;

import java.util.Set;
import java.util.UUID;

import com.synopsys.integration.alert.api.event.AlertEvent;

public class JobSubTaskEvent extends AlertEvent {
    private static final long serialVersionUID = 2328435266614582583L;
    private final UUID jobConfigId;
    private final UUID jobExecutionId;
    private final Set<Long> notificationIds;

    protected JobSubTaskEvent(String destination, UUID jobExecutionId, UUID jobConfigId, Set<Long> notificationIds) {
        super(destination);
        this.jobExecutionId = jobExecutionId;
        this.jobConfigId = jobConfigId;
        this.notificationIds = notificationIds;
    }

    public UUID getJobExecutionId() {
        return jobExecutionId;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public Set<Long> getNotificationIds() {
        return notificationIds;
    }
}
