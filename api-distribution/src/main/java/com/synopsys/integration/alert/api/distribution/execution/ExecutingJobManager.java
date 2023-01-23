package com.synopsys.integration.alert.api.distribution.execution;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusDurations;
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
        executingJob.ifPresent(execution -> {
            completedJobStatusAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.SUCCESS));
        });
    }

    public void endJobWithFailure(UUID executionId, Instant endTime) {
        jobExecutionAccessor.endJobWithFailure(executionId, endTime);
        Optional<JobExecutionModel> executingJob = jobExecutionAccessor.getJobExecution(executionId);
        executingJob.ifPresent(execution -> {
            completedJobStatusAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.FAILURE));
        });
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

    public AlertPagedModel<JobStageModel> getStages(UUID executionId, AlertPagedQueryDetails pagedQueryDetails) {
        return jobExecutionAccessor.getJobStages(executionId, pagedQueryDetails);
    }

    public void startStage(UUID executionId, JobStage stage, Instant start) {
        jobExecutionAccessor.startStage(executionId, stage.name(), start);
    }

    public void endStage(UUID executionId, JobStage stage, Instant end) {
        jobExecutionAccessor.endStage(executionId, stage.name(), end);
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
        UUID jobConfigId = executingJob.getJobConfigId();
        JobCompletionStatusModel resultStatus;
        Optional<JobCompletionStatusModel> status = completedJobStatusAccessor.getJobExecutionStatus(jobConfigId);
        resultStatus = status
            .map(currentStatus -> updateCompletedJobStatus(executingJob, jobStatus, currentStatus))
            .orElseGet(() -> createInitialCompletedJobStatus(executingJob, jobStatus));

        return resultStatus;
    }

    private JobCompletionStatusModel updateCompletedJobStatus(JobExecutionModel executingJob, AuditEntryStatus jobStatus, JobCompletionStatusModel currentStatus) {
        JobCompletionStatusDurations currentDurations = currentStatus.getDurations();
        Long jobDuration = calculateNanoDuration(executingJob.getStart().toInstant(), executingJob.getEnd().map(OffsetDateTime::toInstant).orElse(Instant.now()));
        Long processingStageDuration = calculateJobStageDuration(executingJob, JobStage.NOTIFICATION_PROCESSING);
        Long channelProcessingStageDuration = calculateJobStageDuration(executingJob, JobStage.CHANNEL_PROCESSING);
        Long issueCreationDuration = calculateJobStageDuration(executingJob, JobStage.ISSUE_CREATION);
        Long issueCommentingDuration = calculateJobStageDuration(executingJob, JobStage.ISSUE_COMMENTING);
        Long issueResolvingDuration = calculateJobStageDuration(executingJob, JobStage.ISSUE_RESOLVING);

        JobCompletionStatusDurations durations = new JobCompletionStatusDurations(
            calculateAverage(currentDurations.getJobDurationMillisec(), jobDuration),
            calculateAverage(currentDurations.getNotificationProcessingDuration().orElse(0L), processingStageDuration),
            calculateAverage(currentDurations.getChannelProcessingDuration().orElse(0L), channelProcessingStageDuration),
            calculateAverage(currentDurations.getIssueCreationDuration().orElse(0L), issueCreationDuration),
            calculateAverage(currentDurations.getIssueCommentingDuration().orElse(0L), issueCommentingDuration),
            calculateAverage(currentDurations.getIssueTransitionDuration().orElse(0L), issueResolvingDuration)
        );

        long successCount = 0L;
        long failureCount = 0L;

        if (jobStatus == AuditEntryStatus.SUCCESS) {
            successCount = currentStatus.getSuccessCount() + 1L;
        }

        if (jobStatus == AuditEntryStatus.FAILURE) {
            failureCount = currentStatus.getFailureCount() + 1L;
        }

        return new JobCompletionStatusModel(
            executingJob.getJobConfigId(),
            calculateAverage(Integer.valueOf(executingJob.getProcessedNotificationCount()).longValue(), currentStatus.getNotificationCount()),
            successCount,
            failureCount,
            jobStatus.name(),
            DateUtils.fromInstantUTC(executingJob.getEnd().map(OffsetDateTime::toInstant).orElse(Instant.now())),
            durations
        );
    }

    private JobCompletionStatusModel createInitialCompletedJobStatus(JobExecutionModel executingJob, AuditEntryStatus jobStatus) {
        long successCount = 0L;
        long failureCount = 0L;

        if (jobStatus == AuditEntryStatus.SUCCESS) {
            successCount = 1L;
        }

        if (jobStatus == AuditEntryStatus.FAILURE) {
            failureCount = 1L;
        }

        JobCompletionStatusDurations durations = new JobCompletionStatusDurations(
            calculateNanoDuration(executingJob.getStart().toInstant(), executingJob.getEnd().map(OffsetDateTime::toInstant).orElse(Instant.now())),
            calculateJobStageDuration(executingJob, JobStage.NOTIFICATION_PROCESSING),
            calculateJobStageDuration(executingJob, JobStage.CHANNEL_PROCESSING),
            calculateJobStageDuration(executingJob, JobStage.ISSUE_CREATION),
            calculateJobStageDuration(executingJob, JobStage.ISSUE_COMMENTING),
            calculateJobStageDuration(executingJob, JobStage.ISSUE_RESOLVING)
        );
        return new JobCompletionStatusModel(
            executingJob.getJobConfigId(),
            Integer.valueOf(executingJob.getProcessedNotificationCount()).longValue(),
            successCount,
            failureCount,
            jobStatus.name(),
            DateUtils.fromInstantUTC(executingJob.getEnd().map(OffsetDateTime::toInstant).orElse(Instant.now())),
            durations
        );
    }

    private Long calculateAverage(Long firstValue, Long secondValue) {
        if (firstValue < 1 || secondValue < 1) {
            return 0L;
        }
        return (firstValue + secondValue) / 2;
    }

    private Long calculateJobStageDuration(JobExecutionModel executionModel, JobStage jobStage) {
        Optional<JobStageModel> jobStageModel = jobExecutionAccessor.getJobStage(executionModel.getExecutionId(), jobStage.name());
        return jobStageModel
            .map(this::calculateJobStageDuration)
            .orElse(0L);
    }

    private Long calculateJobStageDuration(JobStageModel stageModel) {
        return calculateNanoDuration(stageModel.getStart().toInstant(), stageModel.getEnd().orElse(DateUtils.createCurrentDateTimestamp()).toInstant());
    }

    private Long calculateNanoDuration(Instant start, Instant end) {
        return Duration.between(start, end).toNanos();
    }

}
