package com.synopsys.integration.alert.database.job.execution;

import java.io.Serializable;
import java.util.UUID;

import com.synopsys.integration.util.Stringable;

public class JobExecutionStagePK extends Stringable implements Serializable {
    private static final long serialVersionUID = -4271124930513120155L;
    private UUID executionId;
    private int stage;

    public JobExecutionStagePK() {
        //JPA constructor
    }

    public JobExecutionStagePK(UUID executionId, int stage) {
        this.executionId = executionId;
        this.stage = stage;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public int getStage() {
        return stage;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }
}
