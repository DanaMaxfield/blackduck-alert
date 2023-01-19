package com.synopsys.integration.alert.component.diagnostic.model;

import java.util.List;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobDiagnosticModel extends AlertSerializableModel {
    private static final long serialVersionUID = -1256404107452935816L;
    private final List<JobStatusDiagnosticModel> jobStatuses;

    public JobDiagnosticModel(List<JobStatusDiagnosticModel> jobStatuses) {
        this.jobStatuses = jobStatuses;
    }

    public List<JobStatusDiagnosticModel> getJobStatuses() {
        return jobStatuses;
    }
}
