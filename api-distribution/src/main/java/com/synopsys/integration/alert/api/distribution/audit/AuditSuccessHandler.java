package com.synopsys.integration.alert.api.distribution.audit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.event.AlertEventHandler;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionStatusAccessor;

@Component
public class AuditSuccessHandler implements AlertEventHandler<AuditSuccessEvent> {
    private final ExecutingJobManager executingJobManager;
    private final JobExecutionStatusAccessor jobExecutionStatusAccessor;

    public AuditSuccessHandler(ExecutingJobManager executingJobManager, JobExecutionStatusAccessor jobExecutionStatusAccessor) {
        this.executingJobManager = executingJobManager;
        this.jobExecutionStatusAccessor = jobExecutionStatusAccessor;
    }

    @Override
    public void handle(AuditSuccessEvent event) {
        UUID jobExecutionId = event.getJobExecutionId();
        executingJobManager.getExecutingJob(jobExecutionId)
            .ifPresent(executingJob -> {
                executingJobManager.endJobWithSuccess(jobExecutionId, event.getCreatedTimestamp().toInstant());
            });
    }
}
