package com.synopsys.integration.alert.database.job.execution;

import java.io.Serializable;
import java.util.UUID;

import com.synopsys.integration.util.Stringable;

public class JobCompletionStagePK extends Stringable implements Serializable {
    private static final long serialVersionUID = 7653854949789467143L;
    private UUID jobConfigId;
    private int stage;

    public JobCompletionStagePK() {
        //JPA constructor
    }

    public JobCompletionStagePK(UUID jobConfigId, int stage) {
        this.jobConfigId = jobConfigId;
        this.stage = stage;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public int getStage() {
        return stage;
    }

    public void setJobConfigId(UUID jobConfigId) {
        this.jobConfigId = jobConfigId;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }
}
