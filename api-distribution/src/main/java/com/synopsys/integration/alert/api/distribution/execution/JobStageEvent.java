package com.synopsys.integration.alert.api.distribution.execution;

import java.time.Instant;
import java.util.UUID;

import com.synopsys.integration.alert.api.event.AlertEvent;

public class JobStageEvent extends AlertEvent {
    private static final long serialVersionUID = 7484019815048606767L;
    private final UUID jobExecutionId;
    private final JobStage jobStage;
    private final Instant createdTimestamp;

    public JobStageEvent(String destination, UUID jobExecutionId, JobStage jobStage) {
        super(destination);
        this.jobExecutionId = jobExecutionId;
        this.jobStage = jobStage;
        this.createdTimestamp = Instant.now();
    }

    public UUID getJobExecutionId() {
        return jobExecutionId;
    }

    public JobStage getJobStage() {
        return jobStage;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }
}
