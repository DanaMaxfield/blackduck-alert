package com.synopsys.integration.alert.api.distribution.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionStatusDurations;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionStatusModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.util.DateUtils;

@Component
public class ExecutingJobManager {
    private final Map<UUID, ExecutingJob> executingJobMap = new ConcurrentHashMap<>();

    private final JobExecutionStatusAccessor completedJobsAccessor;

    public ExecutingJobManager(JobExecutionStatusAccessor completedJobsAccessor) {
        this.completedJobsAccessor = completedJobsAccessor;
    }

    public ExecutingJob startJob(UUID jobConfigId, int totalNotificationCount) {
        ExecutingJob job = ExecutingJob.startJob(jobConfigId, totalNotificationCount);
        executingJobMap.putIfAbsent(job.getExecutionId(), job);
        return job;
    }

    public void endJobWithSuccess(UUID executionId, Instant endTime) {
        Optional<ExecutingJob> executingJob = Optional.ofNullable(executingJobMap.getOrDefault(executionId, null));
        executingJob.ifPresent(execution -> {
            execution.jobSucceeded(endTime);
            completedJobsAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.SUCCESS));
            purgeJob(executionId);
        });
    }

    public void endJobWithFailure(UUID executionId, Instant endTime) {
        Optional<ExecutingJob> executingJob = Optional.ofNullable(executingJobMap.getOrDefault(executionId, null));
        executingJob.ifPresent(execution -> {
            execution.jobFailed(endTime);
            completedJobsAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.FAILURE));
            purgeJob(executionId);
        });
    }

    public void incrementNotificationCount(UUID jobExecutionId, int notificationCount) {
        Optional<ExecutingJob> executingJob = getExecutingJob(jobExecutionId);
        executingJob.ifPresent(execution -> execution.updateNotificationCount(notificationCount));
    }

    public Optional<ExecutingJob> getExecutingJob(UUID jobExecutionId) {
        return Optional.ofNullable(executingJobMap.getOrDefault(jobExecutionId, null));
    }

    public AlertPagedModel<ExecutingJob> getExecutingJobs(int pageNumber, int pageSize) {
        List<List<ExecutingJob>> pages = ListUtils.partition(new ArrayList<>(executingJobMap.values()), pageSize);
        List<ExecutingJob> pageOfData = List.of();
        if (!pages.isEmpty() && pageNumber < pages.size()) {
            pageOfData = pages.get(pageNumber);
        }
        return new AlertPagedModel<>(pages.size(), pageNumber, pageSize, pageOfData);
    }

    public void startStage(UUID executionId, JobStage stage, Instant start) {
        Optional<ExecutingJob> executingJob = Optional.ofNullable(executingJobMap.getOrDefault(executionId, null));
        executingJob.ifPresent(job -> {
            job.addStage(new ExecutingJobStage(executionId, stage, start));
        });
    }

    public void endStage(UUID executionId, JobStage stage, Instant end) {
        Optional<ExecutingJob> executingJob = Optional.ofNullable(executingJobMap.getOrDefault(executionId, null));
        executingJob
            .flatMap(job -> job.getStage(stage))
            .ifPresent(jobStage -> jobStage.endStage(end));
    }

    public void purgeJob(UUID executionId) {
        boolean remove = executingJobMap.containsKey(executionId);
        if (remove) {
            executingJobMap.remove(executionId);
        }
    }

    public AggregatedExecutionResults aggregateExecutingJobData() {
        Long pendingCount = countPendingJobs();
        Long successCount = countSuccessfulJobs();
        Long failedJobs = countFailedJobs();
        Long totalJobs = Long.valueOf(executingJobMap.size());

        return new AggregatedExecutionResults(totalJobs, pendingCount, successCount, failedJobs);
    }

    private Long countSuccessfulJobs() {
        return countJobsByStatus(AuditEntryStatus.SUCCESS);
    }

    private Long countFailedJobs() {
        return countJobsByStatus(AuditEntryStatus.FAILURE);
    }

    private Long countPendingJobs() {
        return countJobsByStatus(AuditEntryStatus.PENDING);
    }

    private Long countJobsByStatus(AuditEntryStatus entryStatus) {
        return executingJobMap.values().stream()
            .filter(executingJob -> executingJob.getStatus().equals(entryStatus))
            .count();
    }

    private JobExecutionStatusModel createStatusModel(ExecutingJob executingJob, AuditEntryStatus jobStatus) {
        UUID jobConfigId = executingJob.getJobConfigId();
        JobExecutionStatusModel resultStatus;
        Optional<JobExecutionStatusModel> status = completedJobsAccessor.getJobExecutionStatus(jobConfigId);
        resultStatus = status
            .map(currentStatus -> updateCompletedJobStatus(executingJob, jobStatus, currentStatus))
            .orElseGet(() -> createInitialCompletedJobStatus(executingJob, jobStatus));

        return resultStatus;
    }

    private JobExecutionStatusModel updateCompletedJobStatus(ExecutingJob executingJob, AuditEntryStatus jobStatus, JobExecutionStatusModel currentStatus) {
        JobExecutionStatusDurations currentDurations = currentStatus.getDurations();
        Long jobDuration = calculateNanoDuration(executingJob.getStart(), executingJob.getEnd().orElse(Instant.now()));
        Long processingStageDuration = calculateJobStageDuration(executingJob, JobStage.NOTIFICATION_PROCESSING);
        Long channelProcessingStageDuration = calculateJobStageDuration(executingJob, JobStage.CHANNEL_PROCESSING);
        Long issueCreationDuration = calculateJobStageDuration(executingJob, JobStage.ISSUE_CREATION);
        Long issueCommentingDuration = calculateJobStageDuration(executingJob, JobStage.ISSUE_COMMENTING);
        Long issueResolvingDuration = calculateJobStageDuration(executingJob, JobStage.ISSUE_RESOLVING);

        JobExecutionStatusDurations durations = new JobExecutionStatusDurations(
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

        return new JobExecutionStatusModel(
            executingJob.getJobConfigId(),
            calculateAverage(Integer.valueOf(executingJob.getProcessedNotificationCount()).longValue(), currentStatus.getNotificationCount()),
            successCount,
            failureCount,
            jobStatus.name(),
            DateUtils.fromInstantUTC(executingJob.getEnd().orElse(Instant.now())),
            durations
        );
    }

    private JobExecutionStatusModel createInitialCompletedJobStatus(ExecutingJob executingJob, AuditEntryStatus jobStatus) {
        long successCount = 0L;
        long failureCount = 0L;

        if (jobStatus == AuditEntryStatus.SUCCESS) {
            successCount = 1L;
        }

        if (jobStatus == AuditEntryStatus.FAILURE) {
            failureCount = 1L;
        }

        JobExecutionStatusDurations durations = new JobExecutionStatusDurations(
            calculateNanoDuration(executingJob.getStart(), executingJob.getEnd().orElse(Instant.now())),
            calculateJobStageDuration(executingJob, JobStage.NOTIFICATION_PROCESSING),
            calculateJobStageDuration(executingJob, JobStage.CHANNEL_PROCESSING),
            calculateJobStageDuration(executingJob, JobStage.ISSUE_CREATION),
            calculateJobStageDuration(executingJob, JobStage.ISSUE_COMMENTING),
            calculateJobStageDuration(executingJob, JobStage.ISSUE_RESOLVING)
        );
        return new JobExecutionStatusModel(
            executingJob.getJobConfigId(),
            Integer.valueOf(executingJob.getProcessedNotificationCount()).longValue(),
            successCount,
            failureCount,
            jobStatus.name(),
            DateUtils.fromInstantUTC(executingJob.getEnd().orElse(Instant.now())),
            durations
        );
    }

    private Long calculateAverage(Long firstValue, Long secondValue) {
        if (firstValue < 1 || secondValue < 1) {
            return 0L;
        }
        return (firstValue + secondValue) / 2;
    }

    private Long calculateJobStageDuration(ExecutingJob executingJob, JobStage stage) {
        return executingJob.getStage(stage)
            .filter(executingJobStage -> executingJobStage.getEnd().isPresent())
            .map(executedStage -> calculateNanoDuration(executedStage.getStart(), executedStage.getEnd().orElse(Instant.now())))
            .orElse(0L);
    }

    private Long calculateNanoDuration(Instant start, Instant end) {
        return Duration.between(start, end).toNanos();
    }

}
