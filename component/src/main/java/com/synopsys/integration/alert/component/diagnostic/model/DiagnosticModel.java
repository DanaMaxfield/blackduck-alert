/*
 * component
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.component.diagnostic.model;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;
import com.synopsys.integration.alert.api.common.model.Obfuscated;

public class DiagnosticModel extends AlertSerializableModel implements Obfuscated<DiagnosticModel> {
    private static final long serialVersionUID = 6714869824373312126L;

    private NotificationDiagnosticModel notificationDiagnosticModel;
    private AuditDiagnosticModel auditDiagnosticModel;
    private SystemDiagnosticModel systemDiagnosticModel;
    private RabbitMQDiagnosticModel rabbitMQDiagnosticModel;
    private JobDiagnosticModel jobDiagnosticModel;
    private JobExecutionsDiagnosticModel executingJobsDiagnosticModel;
    private String requestTimestamp;

    public DiagnosticModel() {
        // For serialization
    }

    public DiagnosticModel(
        String requestTimestamp,
        NotificationDiagnosticModel notificationDiagnosticModel,
        AuditDiagnosticModel auditDiagnosticModel,
        SystemDiagnosticModel systemDiagnosticModel,
        RabbitMQDiagnosticModel rabbitMQDiagnosticModel,
        JobDiagnosticModel jobDiagnosticModel,
        JobExecutionsDiagnosticModel executingJobsDiagnosticModel
    ) {
        this.requestTimestamp = requestTimestamp;
        this.notificationDiagnosticModel = notificationDiagnosticModel;
        this.auditDiagnosticModel = auditDiagnosticModel;
        this.systemDiagnosticModel = systemDiagnosticModel;
        this.rabbitMQDiagnosticModel = rabbitMQDiagnosticModel;
        this.jobDiagnosticModel = jobDiagnosticModel;
        this.executingJobsDiagnosticModel = executingJobsDiagnosticModel;
    }

    public NotificationDiagnosticModel getNotificationDiagnosticModel() {
        return notificationDiagnosticModel;
    }

    public AuditDiagnosticModel getAuditDiagnosticModel() {
        return auditDiagnosticModel;
    }

    public SystemDiagnosticModel getSystemDiagnosticModel() {
        return systemDiagnosticModel;
    }

    public RabbitMQDiagnosticModel getRabbitMQDiagnosticModel() {
        return rabbitMQDiagnosticModel;
    }

    public String getRequestTimestamp() {
        return requestTimestamp;
    }

    public JobDiagnosticModel getJobDiagnosticModel() {
        return jobDiagnosticModel;
    }

    public JobExecutionsDiagnosticModel getExecutingJobsDiagnosticModel() {
        return executingJobsDiagnosticModel;
    }

    @Override
    public DiagnosticModel obfuscate() {
        return new DiagnosticModel(
            requestTimestamp,
            notificationDiagnosticModel,
            auditDiagnosticModel,
            systemDiagnosticModel,
            rabbitMQDiagnosticModel,
            jobDiagnosticModel,
            executingJobsDiagnosticModel
        );
    }
}
