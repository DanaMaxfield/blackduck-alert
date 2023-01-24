package com.synopsys.integration.alert.database.job.execution;

import java.io.Serializable;
import java.util.UUID;

public class JobCompletionStagePK implements Serializable {
    private UUID executionId;
    private int stage;

    public JobCompletionStagePK() {
        //JPA constructor
    }

    public JobCompletionStagePK(UUID executionId, int stage) {
        this.executionId = executionId;
        this.stage = stage;
    }

    public UUID getJobConfigId() {
        return executionId;
    }

    public int getStage() {
        return stage;
    }

    public void setJobConfigId(UUID executionId) {
        this.executionId = executionId;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }
}
