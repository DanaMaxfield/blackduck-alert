package com.synopsys.integration.alert.common.persistence.model.job.executions;

import java.util.UUID;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobCompletionStageModel extends AlertSerializableModel {

    private static final long serialVersionUID = -8116973682156003231L;
    private final UUID jobConfigId;
    private final Integer stageId;
    private final Long durationNano;

    public JobCompletionStageModel(UUID jobConfigId, Integer stageId, Long durationNano) {
        this.jobConfigId = jobConfigId;
        this.stageId = stageId;
        this.durationNano = durationNano;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public Integer getStageId() {
        return stageId;
    }

    public Long getDurationNano() {
        return durationNano;
    }

}
