package com.synopsys.integration.alert.api.distribution.execution;

import java.util.Arrays;

public enum JobStage {
    NOTIFICATION_PROCESSING(0),
    CHANNEL_PROCESSING(1),
    ISSUE_CREATION(2),
    ISSUE_COMMENTING(3),
    ISSUE_RESOLVING(4);

    private final int stageId;

    JobStage(int stageId) {
        this.stageId = stageId;
    }

    public int getStageId() {
        return stageId;
    }

    public static JobStage findByStageId(int stageId) {
        return Arrays.stream(JobStage.values())
            .filter(jobStage -> jobStage.getStageId() == stageId)
            .findFirst()
            .orElse(null);
    }
}
