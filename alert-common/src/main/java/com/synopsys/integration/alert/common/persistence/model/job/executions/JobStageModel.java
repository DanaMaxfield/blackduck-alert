package com.synopsys.integration.alert.common.persistence.model.job.executions;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.lang.Nullable;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobStageModel extends AlertSerializableModel {

    private static final long serialVersionUID = -3731652313267969148L;
    private final UUID executionId;
    private final int stageId;
    private final OffsetDateTime start;
    private final OffsetDateTime end;

    public JobStageModel(UUID executionId, int stageId, OffsetDateTime start, @Nullable OffsetDateTime end) {
        this.executionId = executionId;
        this.stageId = stageId;
        this.start = start;
        this.end = end;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public int getStageId() {
        return stageId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public Optional<OffsetDateTime> getEnd() {
        return Optional.ofNullable(end);
    }
}
