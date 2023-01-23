package com.synopsys.integration.alert.common.persistence.model.job.executions;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.lang.Nullable;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobStageModel extends AlertSerializableModel {

    private static final long serialVersionUID = -3731652313267969148L;

    private final UUID id;
    private final UUID executionId;
    private final String name;
    private final OffsetDateTime start;
    private final OffsetDateTime end;

    public JobStageModel(UUID id, UUID executionId, String name, OffsetDateTime start, @Nullable OffsetDateTime end) {
        this.id = id;
        this.executionId = executionId;
        this.name = name;
        this.start = start;
        this.end = end;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getName() {
        return name;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }
}
