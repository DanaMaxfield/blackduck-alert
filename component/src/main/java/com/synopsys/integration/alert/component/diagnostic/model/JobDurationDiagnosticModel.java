package com.synopsys.integration.alert.component.diagnostic.model;

import java.util.List;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobDurationDiagnosticModel extends AlertSerializableModel {

    private static final long serialVersionUID = 7429493522403387353L;
    private final String jobDuration;
    private final List<JobStageStatusDiagnosticModel> stageDurations;

    public JobDurationDiagnosticModel(String jobDuration, List<JobStageStatusDiagnosticModel> stageDurations) {
        this.jobDuration = jobDuration;
        this.stageDurations = stageDurations;
    }

    public String getJobDuration() {
        return jobDuration;
    }

    public List<JobStageStatusDiagnosticModel> getStageDurations() {
        return stageDurations;
    }
}
