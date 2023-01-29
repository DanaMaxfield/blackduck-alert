package com.synopsys.integration.alert.api.distribution.execution;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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

    //private final Map<UUID, AtomicInteger> remainingEvents = new ConcurrentHashMap<>();

    @Autowired
    public ExecutingJobManager(JobCompletionStatusAccessor completedJobStatusAccessor, JobExecutionAccessor jobExecutionAccessor) {
        this.completedJobStatusAccessor = completedJobStatusAccessor;
        this.jobExecutionAccessor = jobExecutionAccessor;
    }

    public JobExecutionModel startJob(UUID jobConfigId, int totalNotificationCount) {
        JobExecutionModel execution = jobExecutionAccessor.startJob(jobConfigId, totalNotificationCount);
        Optional<JobCompletionStatusModel> existingStatus = completedJobStatusAccessor.getJobExecutionStatus(execution.getJobConfigId());
        if (existingStatus.isEmpty()) {
            completedJobStatusAccessor.saveExecutionStatus(createEmptyStatusModel(execution));
            completedJobStatusAccessor.saveJobStageData(createEmptyStatusStageModel(jobConfigId, JobStage.NOTIFICATION_PROCESSING));
            completedJobStatusAccessor.saveJobStageData(createEmptyStatusStageModel(jobConfigId, JobStage.CHANNEL_PROCESSING));
        }
        return execution;
    }

    public void endJobWithSuccess(UUID executionId, Instant endTime, int notificationCount) {
        jobExecutionAccessor.endJobWithSuccess(executionId, endTime);
        Optional<JobExecutionModel> executingJob = jobExecutionAccessor.getJobExecution(executionId);
        executingJob.ifPresent(execution -> completedJobStatusAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.SUCCESS, notificationCount)));
    }

    public void endJobWithFailure(UUID executionId, Instant endTime, int notificationCount) {
        jobExecutionAccessor.endJobWithFailure(executionId, endTime);
        Optional<JobExecutionModel> executingJob = jobExecutionAccessor.getJobExecution(executionId);
        executingJob.ifPresent(execution -> completedJobStatusAccessor.saveExecutionStatus(createStatusModel(execution, AuditEntryStatus.FAILURE, notificationCount)));
    }

    public void incrementNotificationCount(UUID jobExecutionId, int notificationCount) {
        jobExecutionAccessor.incrementNotificationCount(jobExecutionId, notificationCount);
    }

    public void incrementJobEventCount(UUID jobExecutionId, int eventCount) {
        //        jobExecutionAccessor.getJobExecution(jobExecutionId)
        //            .map(JobExecutionModel::getJobConfigId)
        //            .ifPresent(jobConfigId -> {
        //                AtomicInteger remainingEventCount = remainingEvents.computeIfAbsent(jobConfigId, ignored -> new AtomicInteger(0));
        //                remainingEventCount.addAndGet(eventCount);
        //            });
        jobExecutionAccessor.incrementJobEventCount(jobExecutionId, eventCount);
    }

    public void decrementJobEventCount(UUID jobExecutionId) {
        //        jobExecutionAccessor.getJobExecution(jobExecutionId)
        //            .map(JobExecutionModel::getJobConfigId)
        //            .ifPresent(jobConfigId -> {
        //                AtomicInteger remainingEventCount = remainingEvents.computeIfAbsent(jobConfigId, ignored -> new AtomicInteger(0));
        //                remainingEventCount.decrementAndGet();
        //            });
        jobExecutionAccessor.decrementJobEventCount(jobExecutionId);
    }

    public boolean hasRemainingEvents(UUID jobExecutionId) {
        //        return jobExecutionAccessor.getJobExecution(jobExecutionId)
        //            .map(JobExecutionModel::getJobConfigId)
        //            .filter(remainingEvents::containsKey)
        //            .map(remainingEvents::get)
        //            .stream()
        //            .allMatch(atomicInteger -> atomicInteger.get() > 0);
        return jobExecutionAccessor.hasRemainingEvents(jobExecutionId);
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

    public void purgeOldCompletedJobs() {
        jobExecutionAccessor.purgeOldCompletedJobs();
    }

    public void purgeAllJobs() {
        jobExecutionAccessor.purgeAllJobs();
    }

    private JobCompletionStatusModel createEmptyStatusModel(JobExecutionModel executingJob) {
        return new JobCompletionStatusModel(executingJob.getJobConfigId(), 0L, 0L, 0L, 0L, AuditEntryStatus.PENDING.name(), OffsetDateTime.now(), 0L);
    }

    private JobCompletionStageModel createEmptyStatusStageModel(UUID jobConfigId, JobStage jobStage) {
        return new JobCompletionStageModel(jobConfigId, jobStage.getStageId(), 0L);
    }

    private JobCompletionStatusModel createStatusModel(JobExecutionModel executingJob, AuditEntryStatus jobStatus, int notificationCount) {
        UUID jobConfigId = executingJob.getJobConfigId();
        long successCount = jobExecutionAccessor.countJobExecutionsByStatus(jobConfigId, AuditEntryStatus.SUCCESS);
        long failureCount = jobExecutionAccessor.countJobExecutionsByStatus(jobConfigId, AuditEntryStatus.FAILURE);
        jobExecutionAccessor.markAllExecutionsForJobAggregated(jobConfigId);
        OffsetDateTime start = executingJob.getStart();
        OffsetDateTime end = executingJob.getEnd().orElse(OffsetDateTime.now());

        return new JobCompletionStatusModel(
            executingJob.getJobConfigId(),
            Integer.valueOf(executingJob.getProcessedNotificationCount()).longValue(),
            Integer.valueOf(notificationCount).longValue(),
            successCount,
            failureCount,
            jobStatus.name(),
            DateUtils.fromInstantUTC(end.toInstant()),
            calculateDuration(start, end)
        );
    }

    private Long calculateJobStageDuration(JobExecutionModel executionModel, JobStage jobStage) {
        Optional<JobStageModel> jobStageModel = jobExecutionAccessor.getJobStage(executionModel.getExecutionId(), jobStage.getStageId());
        return jobStageModel
            .map(this::calculateJobStageDuration)
            .orElse(0L);
    }

    private Long calculateJobStageDuration(JobStageModel stageModel) {
        return calculateDuration(
            stageModel.getStart(),
            stageModel.getEnd().orElse(DateUtils.createCurrentDateTimestamp())
        );
    }

    private Long calculateDuration(OffsetDateTime start, OffsetDateTime end) {
        return Duration.between(
            start.toInstant(),
            end.toInstant()
        ).toNanos();
    }
}
