package com.synopsys.integration.alert.api.event.distribution;

import java.util.Set;
import java.util.UUID;

import com.synopsys.integration.alert.api.event.AlertEvent;

public class JobSubTaskEvent extends AlertEvent {
    private static final long serialVersionUID = 2328435266614582583L;
    private final UUID parentEventId;
    private final UUID jobExecutionId;
    private final Set<Long> notificationIds;

    protected JobSubTaskEvent(String destination, UUID parentEventId, UUID jobExecutionId, Set<Long> notificationIds) {
        super(destination);
        this.parentEventId = parentEventId;
        this.jobExecutionId = jobExecutionId;
        this.notificationIds = notificationIds;
    }

    public UUID getParentEventId() {
        return parentEventId;
    }

    public UUID getJobExecutionId() {
        return jobExecutionId;
    }

    public Set<Long> getNotificationIds() {
        return notificationIds;
    }
}
