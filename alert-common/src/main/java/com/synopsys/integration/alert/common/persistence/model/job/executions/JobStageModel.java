package com.synopsys.integration.alert.common.persistence.model.job.executions;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.lang.Nullable;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobStageModel extends AlertSerializableModel {

    private static final long serialVersionUID = -3731652313267969148L;
    private final UUID executionId;
    private final String name;
    private final OffsetDateTime start;
    private final OffsetDateTime end;

    public JobStageModel(UUID executionId, String name, OffsetDateTime start, @Nullable OffsetDateTime end) {
        this.executionId = executionId;
        this.name = name;
        this.start = start;
        this.end = end;
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

    public Optional<OffsetDateTime> getEnd() {
        return Optional.ofNullable(end);
    }
}
