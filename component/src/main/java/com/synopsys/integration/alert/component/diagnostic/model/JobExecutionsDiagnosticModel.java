package com.synopsys.integration.alert.component.diagnostic.model;

import java.util.List;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobExecutionsDiagnosticModel extends AlertSerializableModel {

    private static final long serialVersionUID = -1234272679442123110L;

    private final List<JobExecutionDiagnosticModel> jobExecutions;

    public JobExecutionsDiagnosticModel(
         List<JobExecutionDiagnosticModel> jobExecutions
    ) {

        this.jobExecutions = jobExecutions;
    }

    public List<JobExecutionDiagnosticModel> getJobExecutions() {
        return jobExecutions;
    }
}
