package com.synopsys.integration.alert.component.diagnostic.model;

import java.util.List;

public class JobDiagnosticModel {
    private final List<JobStatusDiagnosticModel> jobStatuses;

    public JobDiagnosticModel(List<JobStatusDiagnosticModel> jobStatuses) {
        this.jobStatuses = jobStatuses;
    }

    public List<JobStatusDiagnosticModel> getJobStatuses() {
        return jobStatuses;
    }
}
