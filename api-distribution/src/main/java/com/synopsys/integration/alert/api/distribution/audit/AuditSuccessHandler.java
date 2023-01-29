package com.synopsys.integration.alert.api.distribution.audit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.event.AlertEventHandler;

@Component
public class AuditSuccessHandler implements AlertEventHandler<AuditSuccessEvent> {
    private final ExecutingJobManager executingJobManager;

    public AuditSuccessHandler(ExecutingJobManager executingJobManager) {
        this.executingJobManager = executingJobManager;
    }

    @Override
    public void handle(AuditSuccessEvent event) {
        UUID jobExecutionId = event.getJobExecutionId();
        executingJobManager.decrementJobEventCount(jobExecutionId);
        executingJobManager.getExecutingJob(jobExecutionId)
            .ifPresent(executingJob -> {
                if (!executingJobManager.hasRemainingEvents(jobExecutionId)) {
                    executingJobManager.endJobWithSuccess(jobExecutionId, event.getCreatedTimestamp(), event.getNotificationIds().size());
                }
            });
    }
}
