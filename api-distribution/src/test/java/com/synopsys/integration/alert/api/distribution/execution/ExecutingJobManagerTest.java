package com.synopsys.integration.alert.api.distribution.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStageRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStatusDurationRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStatusStatusRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobExecutionRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobExecutionStageRepository;
import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobStageModel;
import com.synopsys.integration.alert.database.api.DefaultJobCompletionStatusAccessor;
import com.synopsys.integration.alert.database.api.DefaultJobExecutionAccessor;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStageRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusDurationRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStageRepository;

class ExecutingJobManagerTest {

    private ExecutingJobManager createManager() {
        JobCompletionStatusDurationRepository durationsRepository = new MockJobCompletionStatusDurationRepository();
        JobCompletionStatusRepository jobCompletionStatusRepository = new MockJobCompletionStatusStatusRepository(durationsRepository);
        JobCompletionStageRepository jobCompletionStageRepository = new MockJobCompletionStageRepository();
        JobCompletionStatusAccessor completionStatusAccessor = new DefaultJobCompletionStatusAccessor(
            jobCompletionStatusRepository,
            durationsRepository,
            jobCompletionStageRepository
        );
        JobExecutionRepository jobExecutionRepository = new MockJobExecutionRepository();
        JobExecutionStageRepository jobExecutionStageRepository = new MockJobExecutionStageRepository();
        JobExecutionAccessor jobExecutionAccessor = new DefaultJobExecutionAccessor(jobExecutionRepository, jobExecutionStageRepository);

        return new ExecutingJobManager(completionStatusAccessor, jobExecutionAccessor);
    }

    @Test
    void createExecutingJobTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 0);
        assertNotNull(executingJob);
        assertEquals(jobConfigId, executingJob.getJobConfigId());
    }

    @Test
    void removeExecutingJobTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 0);
        jobManager.endJobWithSuccess(jobConfigId, Instant.now());
        jobManager.purgeJob(executingJob.getExecutionId());
        Optional<JobExecutionModel> savedJob = jobManager.getExecutingJob(executingJob.getExecutionId());
        assertTrue(savedJob.isEmpty());
    }

    @Test
    void executingJobPendingTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 1);
        AggregatedExecutionResults results = jobManager.aggregateExecutingJobData();
        assertNotNull(executingJob);
        assertEquals(jobConfigId, executingJob.getJobConfigId());
        assertEquals(AuditEntryStatus.PENDING, executingJob.getStatus());
        assertNotNull(executingJob.getStart());
        assertTrue(executingJob.getEnd().isEmpty());

        assertEquals(1, results.getPendingJobs());
        assertEquals(0, results.getSuccessFulJobs());
        assertEquals(0, results.getFailedJobs());
        assertEquals(1, results.getTotalJobsInSystem());
    }

    @Test
    void executingJobSucceededTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 1);
        jobManager.endJobWithSuccess(executingJob.getExecutionId(), Instant.now());
        JobExecutionModel savedJob = jobManager.getExecutingJob(executingJob.getExecutionId()).orElseThrow(() -> new AssertionError("Job with execution ID not found."));
        AggregatedExecutionResults results = jobManager.aggregateExecutingJobData();
        assertEquals(jobConfigId, savedJob.getJobConfigId());
        assertEquals(AuditEntryStatus.SUCCESS, savedJob.getStatus());
        assertNotNull(savedJob.getStart());
        assertNotNull(savedJob.getEnd().orElseThrow(() -> new AssertionError("End time should be present for a completed job.")));

        assertEquals(0, results.getPendingJobs());
        assertEquals(1, results.getSuccessFulJobs());
        assertEquals(0, results.getFailedJobs());
        assertEquals(1, results.getTotalJobsInSystem());
    }

    @Test
    void executingJobFailedTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 1);
        jobManager.endJobWithFailure(executingJob.getExecutionId(), Instant.now());
        JobExecutionModel savedJob = jobManager.getExecutingJob(executingJob.getExecutionId()).orElseThrow(() -> new AssertionError("Job with execution ID not found."));
        AggregatedExecutionResults results = jobManager.aggregateExecutingJobData();
        assertEquals(jobConfigId, savedJob.getJobConfigId());
        assertEquals(AuditEntryStatus.FAILURE, savedJob.getStatus());
        assertNotNull(savedJob.getStart());
        assertNotNull(savedJob.getEnd().orElseThrow(() -> new AssertionError("End time should be present for a completed job.")));

        assertEquals(0, results.getPendingJobs());
        assertEquals(0, results.getSuccessFulJobs());
        assertEquals(1, results.getFailedJobs());
        assertEquals(1, results.getTotalJobsInSystem());
    }

    @Test
    void addStageTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 1);
        jobManager.startStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, Instant.now());
        jobManager.endStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, Instant.now());
        JobStageModel storedStage = jobManager.getStages(executingJob.getExecutionId())
            .stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("Job Stage is missing when it should be present."));
        assertEquals(executingJob.getExecutionId(), storedStage.getExecutionId());
        assertEquals(JobStage.NOTIFICATION_PROCESSING.name(), storedStage.getStageId());
        assertNotNull(storedStage.getStart());
        assertNotNull(storedStage.getEnd());
    }

    @Test
    void addSameStageTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        Instant firstStageStart = Instant.now();
        Instant firstStageEnd = Instant.now();
        Instant secondStageStart = Instant.now();
        Instant secondStageEnd = Instant.now();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 1);
        jobManager.startStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, firstStageStart);
        jobManager.endStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, firstStageEnd);

        jobManager.startStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, secondStageStart);
        jobManager.endStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, secondStageEnd);

        List<JobStageModel> storedStages = jobManager.getStages(executingJob.getExecutionId());
        assertEquals(1, storedStages.size());
        JobStageModel storedStage = storedStages.get(0);
        assertEquals(executingJob.getExecutionId(), storedStage.getExecutionId());
        assertEquals(JobStage.NOTIFICATION_PROCESSING.name(), storedStage.getStageId());
        assertNotNull(storedStage.getStart());
        assertNotNull(storedStage.getEnd());
        assertTrue(firstStageStart.isBefore(storedStage.getStart().toInstant()));
        assertTrue(firstStageEnd.isBefore(storedStage.getEnd().map(OffsetDateTime::toInstant).orElseThrow(() -> new AssertionError("End time should be present."))));
    }

    @Test
    void multipleStagesTest() {
        ExecutingJobManager jobManager = createManager();

        UUID jobConfigId = UUID.randomUUID();
        JobExecutionModel executingJob = jobManager.startJob(jobConfigId, 1);
        Instant firstStageStart = Instant.now();
        Instant firstStageEnd = Instant.now();
        Instant secondStageStart = Instant.now();
        Instant secondStageEnd = Instant.now();
        jobManager.startStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, firstStageStart);
        jobManager.endStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING, firstStageEnd);

        jobManager.startStage(executingJob.getExecutionId(), JobStage.CHANNEL_PROCESSING, secondStageStart);
        jobManager.endStage(executingJob.getExecutionId(), JobStage.CHANNEL_PROCESSING, secondStageEnd);

        List<JobStageModel> storedStages = jobManager.getStages(executingJob.getExecutionId());
        assertEquals(2, storedStages.size());
        JobStageModel storedStage = storedStages
            .stream()
            .filter(model -> model.getStageId() == JobStage.NOTIFICATION_PROCESSING.getStageId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("stage expected but not found"));
        assertEquals(executingJob.getExecutionId(), storedStage.getExecutionId());
        assertEquals(JobStage.NOTIFICATION_PROCESSING.getStageId(), storedStage.getStageId());
        assertNotNull(storedStage.getStart());
        assertNotNull(storedStage.getEnd());
        assertEquals(firstStageStart, storedStage.getStart().toInstant());
        assertEquals(firstStageEnd, storedStage.getEnd().map(OffsetDateTime::toInstant).orElseThrow(() -> new AssertionError("End time should be present.")));

        storedStage = storedStages
            .stream()
            .filter(model -> model.getStageId() == JobStage.CHANNEL_PROCESSING.getStageId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("stage expected but not found"));
        assertEquals(executingJob.getExecutionId(), storedStage.getExecutionId());
        assertEquals(JobStage.CHANNEL_PROCESSING.name(), storedStage.getStageId());
        assertNotNull(storedStage.getStart());
        assertNotNull(storedStage.getEnd());
        assertEquals(secondStageStart, storedStage.getStart().toInstant());
        assertEquals(secondStageEnd, storedStage.getEnd().map(OffsetDateTime::toInstant).orElseThrow(() -> new AssertionError("End time should be present.")));
    }
}
