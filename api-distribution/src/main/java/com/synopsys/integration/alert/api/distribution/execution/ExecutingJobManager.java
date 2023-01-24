package com.synopsys.integration.alert.api.distribution.execution;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStageModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobStageModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.common.util.DateUtils;

@Component
public class ExecutingJobManager {

    private final JobCompletionStatusAccessor completedJobStatusAccessor;
    private final JobExecutionAccessor jobExecutionAccessor;

    public ExecutingJobManager(JobCompletionStatusAccessor completedJobStatusAccessor, JobExecutionAccessor jobExecutionAccessor) {
        this.completedJobStatusAccessor = completedJobStatusAccessor;
        this.jobExecutionAccessor = jobExecutionAccessor;
    }

    public JobExecutionModel startJob(UUID jobConfigId, int totalNotificationCount) {
        return jobExecutionAccessor.startJob(jobConfigId, totalNotificationCount);
    }

    public void endJobWithSuccess(UUID executionId, Instant endTime) {
        jobExecutionAccessor.endJobWithSuccess(executionId, endTime);
        Optional<JobExecutionModel> executingJob = jobExecutionAccessor.getJobExecution(executionId);
        executingJob.ifPresent(execution -> completedJobStatusAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.SUCCESS)));
    }

    public void endJobWithFailure(UUID executionId, Instant endTime) {
        jobExecutionAccessor.endJobWithFailure(executionId, endTime);
        Optional<JobExecutionModel> executingJob = jobExecutionAccessor.getJobExecution(executionId);
        executingJob.ifPresent(execution -> completedJobStatusAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.FAILURE)));
    }

    public void incrementNotificationCount(UUID jobExecutionId, int notificationCount) {
        jobExecutionAccessor.incrementNotificationCount(jobExecutionId, notificationCount);
    }

    public Optional<JobExecutionModel> getExecutingJob(UUID jobExecutionId) {
        return jobExecutionAccessor.getJobExecution(jobExecutionId);
    }

    public AlertPagedModel<JobExecutionModel> getExecutingJobs(int pageNumber, int pageSize) {
        return jobExecutionAccessor.getExecutingJobs(new AlertPagedQueryDetails(pageNumber, pageSize));
    }

    public List<JobStageModel> getStages(UUID executionId) {
        return jobExecutionAccessor.getJobStages(executionId);
    }

    public void startStage(UUID executionId, JobStage stage, Instant start) {
        jobExecutionAccessor.startStage(executionId, stage.getStageId(), start);
    }

    public void endStage(UUID executionId, JobStage stage, Instant end) {
        jobExecutionAccessor.endStage(executionId, stage.getStageId(), end);
        Optional<JobExecutionModel> executingJob = jobExecutionAccessor.getJobExecution(executionId);
        executingJob.ifPresent(job -> {
            Long stageDuration = calculateJobStageDuration(job, stage);
            completedJobStatusAccessor.saveJobStageData(new JobCompletionStageModel(job.getJobConfigId(), stage.getStageId(), stageDuration));
        });
    }

    public void purgeJob(UUID executionId) {
        jobExecutionAccessor.purgeJob(executionId);
    }

    public AggregatedExecutionResults aggregateExecutingJobData() {
        Long pendingCount = countJobsByStatus(AuditEntryStatus.PENDING);
        Long successCount = countJobsByStatus(AuditEntryStatus.SUCCESS);
        Long failedJobs = countJobsByStatus(AuditEntryStatus.FAILURE);
        long totalJobs = pendingCount + successCount + failedJobs;

        return new AggregatedExecutionResults(totalJobs, pendingCount, successCount, failedJobs);
    }

    private Long countJobsByStatus(AuditEntryStatus status) {
        return jobExecutionAccessor.countJobsByStatus(status);
    }

    private JobCompletionStatusModel createStatusModel(JobExecutionModel executingJob, AuditEntryStatus jobStatus) {
        long successCount = 0L;
        long failureCount = 0L;

        if (jobStatus == AuditEntryStatus.SUCCESS) {
            successCount = 1L;
        }

        if (jobStatus == AuditEntryStatus.FAILURE) {
            failureCount = 1L;
        }

        return new JobCompletionStatusModel(
            executingJob.getJobConfigId(),
            Integer.valueOf(executingJob.getProcessedNotificationCount()).longValue(),
            successCount,
            failureCount,
            jobStatus.name(),
            DateUtils.fromInstantUTC(executingJob.getEnd().map(OffsetDateTime::toInstant).orElse(Instant.now()))
        );
    }

    private Long calculateJobStageDuration(JobExecutionModel executionModel, JobStage jobStage) {
        Optional<JobStageModel> jobStageModel = jobExecutionAccessor.getJobStage(executionModel.getExecutionId(), jobStage.name());
        return jobStageModel
            .map(this::calculateJobStageDuration)
            .orElse(0L);
    }

    private Long calculateJobStageDuration(JobStageModel stageModel) {
        return Duration.between(
            stageModel.getStart().toInstant(),
            stageModel.getEnd().orElse(DateUtils.createCurrentDateTimestamp()).toInstant()
        ).toNanos();
    }
}
