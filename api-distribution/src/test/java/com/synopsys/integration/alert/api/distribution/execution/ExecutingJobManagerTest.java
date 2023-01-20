package com.synopsys.integration.alert.api.distribution.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.alert.api.distribution.mock.MockJobExecutionStatusDurationsRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobExecutionStatusRepository;
import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionStatusAccessor;
import com.synopsys.integration.alert.database.api.DefaultJobExecutionStatusAccessor;
import com.synopsys.integration.alert.database.job.execution.JobExecutionDurationsRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionRepository;

class ExecutingJobManagerTest {

    private JobExecutionStatusAccessor createAccessor() {
        JobExecutionDurationsRepository durationsRepository = new MockJobExecutionStatusDurationsRepository();
        JobExecutionRepository jobExecutionRepository = new MockJobExecutionStatusRepository(durationsRepository);
        return new DefaultJobExecutionStatusAccessor(jobExecutionRepository, durationsRepository);
    }

    @Test
    void createExecutingJobTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 0);
        assertNotNull(executingJob);
        assertEquals(jobConfigId, executingJob.getJobConfigId());
    }

    @Test
    void removeExecutingJobTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 0);
        jobManager.endJobWithSuccess(jobConfigId, Instant.now());
        ExecutingJob savedJob = jobManager.getExecutingJob(executingJob.getExecutionId()).orElse(null);
        jobManager.purgeJob(executingJob.getExecutionId());
        assertNotNull(savedJob);
        assertEquals(executingJob.getExecutionId(), savedJob.getExecutionId());
        assertTrue(jobManager.getExecutingJob(savedJob.getExecutionId()).isEmpty());
    }

    @Test
    void executingJobPendingTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
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
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
        ExecutingJob savedJob = jobManager.getExecutingJob(executingJob.getExecutionId()).orElseThrow(() -> new AssertionError("Job with execution ID not found."));
        savedJob.jobSucceeded(Instant.now());
        AggregatedExecutionResults results = jobManager.aggregateExecutingJobData();
        assertEquals(jobConfigId, savedJob.getJobConfigId());
        assertEquals(AuditEntryStatus.SUCCESS, savedJob.getStatus());
        assertNotNull(executingJob.getStart());
        assertNotNull(executingJob.getEnd().orElseThrow(() -> new AssertionError("End time should be present for a completed job.")));

        assertEquals(0, results.getPendingJobs());
        assertEquals(1, results.getSuccessFulJobs());
        assertEquals(0, results.getFailedJobs());
        assertEquals(1, results.getTotalJobsInSystem());
    }

    @Test
    void executingJobFailedTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
        ExecutingJob savedJob = jobManager.getExecutingJob(executingJob.getExecutionId()).orElseThrow(() -> new AssertionError("Job with execution ID not found."));
        savedJob.jobFailed(Instant.now());
        AggregatedExecutionResults results = jobManager.aggregateExecutingJobData();
        assertEquals(jobConfigId, savedJob.getJobConfigId());
        assertEquals(AuditEntryStatus.FAILURE, executingJob.getStatus());
        assertNotNull(executingJob.getStart());
        assertNotNull(executingJob.getEnd().orElseThrow(() -> new AssertionError("End time should be present for a completed job.")));

        assertEquals(0, results.getPendingJobs());
        assertEquals(0, results.getSuccessFulJobs());
        assertEquals(1, results.getFailedJobs());
        assertEquals(1, results.getTotalJobsInSystem());
    }

    @Test
    void addStageTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
        ExecutingJobStage executingJobStage = ExecutingJobStage.createStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING);
        executingJob.addStage(executingJobStage);
        executingJobStage.endStage();
        ExecutingJobStage storedStage = executingJob.getStage(JobStage.NOTIFICATION_PROCESSING)
            .orElseThrow(() -> new AssertionError("Job Stage is missing when it should be present."));
        assertEquals(executingJobStage, storedStage);
        assertEquals(executingJob.getExecutionId(), executingJobStage.getExecutionId());
        assertEquals(JobStage.NOTIFICATION_PROCESSING, executingJobStage.getStage());
        assertNotNull(executingJobStage.getStart());
        assertNotNull(executingJobStage.getEnd());
    }

    @Test
    void stageMissingTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
        ExecutingJobStage executingJobStage = ExecutingJobStage.createStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING);
        executingJob.addStage(executingJobStage);
        executingJobStage.endStage();
        Optional<ExecutingJobStage> missingStage = executingJob.getStage(JobStage.CHANNEL_PROCESSING);
        assertTrue(missingStage.isEmpty());
    }

    @Test
    void addSameStageTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
        ExecutingJobStage firstStage = ExecutingJobStage.createStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING);
        executingJob.addStage(firstStage);
        firstStage.endStage();

        ExecutingJobStage secondStage = ExecutingJobStage.createStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING);
        executingJob.addStage(secondStage);
        secondStage.endStage();

        ExecutingJobStage storedStage = executingJob.getStage(JobStage.NOTIFICATION_PROCESSING)
            .orElseThrow(() -> new AssertionError("Job Stage is missing when it should be present."));
        assertEquals(firstStage, storedStage);
        assertEquals(1, executingJob.getStages().size());
    }

    @Test
    void multipleStagesTest() {
        JobExecutionStatusAccessor accessor = createAccessor();
        ExecutingJobManager jobManager = new ExecutingJobManager(accessor);
        UUID jobConfigId = UUID.randomUUID();
        ExecutingJob executingJob = jobManager.startJob(jobConfigId, 1);
        ExecutingJobStage mappingStage = ExecutingJobStage.createStage(executingJob.getExecutionId(), JobStage.NOTIFICATION_PROCESSING);
        executingJob.addStage(mappingStage);
        mappingStage.endStage();
        ExecutingJobStage processingStage = ExecutingJobStage.createStage(executingJob.getExecutionId(), JobStage.CHANNEL_PROCESSING);
        executingJob.addStage(processingStage);
        processingStage.endStage();
        assertTrue(executingJob.getStage(JobStage.NOTIFICATION_PROCESSING).isPresent());
        assertTrue(executingJob.getStage(JobStage.CHANNEL_PROCESSING).isPresent());
        assertEquals(2, executingJob.getStages().size());
    }

}
