package com.synopsys.integration.alert.api.distribution.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.event.AlertEventHandler;
import com.synopsys.integration.alert.common.persistence.accessor.ProcessingFailedAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.util.DateUtils;

@Component
public class AuditFailedHandler implements AlertEventHandler<AuditFailedEvent> {
    private final ProcessingFailedAccessor processingFailedAccessor;
    private final ExecutingJobManager executingJobManager;

    @Autowired
    public AuditFailedHandler(
        ProcessingFailedAccessor processingFailedAccessor,
        ExecutingJobManager executingJobManager
    ) {
        this.processingFailedAccessor = processingFailedAccessor;
        this.executingJobManager = executingJobManager;
    }

    @Override
    public void handle(AuditFailedEvent event) {
        UUID jobExecutionId = event.getJobExecutionId();
        Optional<JobExecutionModel> executingJobOptional = executingJobManager.getExecutingJob(jobExecutionId);
        if (executingJobOptional.isPresent()) {
            JobExecutionModel executingJob = executingJobOptional.get();
            UUID jobConfigId = executingJob.getJobConfigId();
            if (event.getStackTrace().isPresent()) {
                processingFailedAccessor.setAuditFailure(
                    jobConfigId,
                    event.getNotificationIds(),
                    DateUtils.fromInstantUTC(event.getCreatedTimestamp()),
                    event.getErrorMessage(),
                    event.getStackTrace().orElse("NO STACK TRACE")
                );
            } else {
                processingFailedAccessor.setAuditFailure(jobConfigId, event.getNotificationIds(), DateUtils.fromInstantUTC(event.getCreatedTimestamp()), event.getErrorMessage());
            }
            executingJobManager.endJobWithFailure(jobExecutionId, event.getCreatedTimestamp(), event.getNotificationIds().size());
        }
        executingJobManager.decrementJobEventCount(jobExecutionId);
    }
}
