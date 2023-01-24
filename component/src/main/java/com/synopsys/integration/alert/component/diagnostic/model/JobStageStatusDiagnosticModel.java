package com.synopsys.integration.alert.component.diagnostic.model;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

public class JobStageStatusDiagnosticModel extends AlertSerializableModel {
    private static final long serialVersionUID = 650211937480509070L;
    private final String name;
    private final String duration;

    public JobStageStatusDiagnosticModel(String name, String duration) {
        this.name = name;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public String getDuration() {
        return duration;
    }
}
