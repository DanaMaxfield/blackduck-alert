package com.synopsys.integration.alert.database.job.execution;

import java.io.Serializable;
import java.util.UUID;

public class JobExecutionStagePK implements Serializable {
    private UUID executionId;
    private String stage;

    public JobExecutionStagePK() {
        //JPA constructor
    }

    public JobExecutionStagePK(UUID executionId, String stage) {
        this.executionId = executionId;
        this.stage = stage;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getStage() {
        return stage;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
