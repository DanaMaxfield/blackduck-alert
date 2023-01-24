package com.synopsys.integration.alert.common.persistence.accessor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobStageModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;

public interface JobExecutionAccessor {
    JobExecutionModel startJob(UUID jobConfigId, int totalNotificationCount);

    void endJobWithSuccess(UUID executionId, Instant endTime);

    void endJobWithFailure(UUID executionId, Instant endTime);

    void incrementNotificationCount(UUID jobExecutionId, int notificationCount);

    Optional<JobExecutionModel> getJobExecution(UUID jobExecutionId);

    AlertPagedModel<JobExecutionModel> getExecutingJobs(AlertPagedQueryDetails pagedQueryDetails);

    AlertPagedModel<JobExecutionModel> getCompletedJobs(AlertPagedQueryDetails pagedQueryDetails);

    List<JobStageModel> getJobStages(UUID jobExecutionId);

    Optional<JobStageModel> getJobStage(UUID jobExecutionId, String stageName);

    void startStage(UUID executionId, int stageId, Instant start);

    void endStage(UUID executionId, int stageId, Instant end);

    Long countJobsByStatus(AuditEntryStatus status);

    void purgeJob(UUID executionId);
}
