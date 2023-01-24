package com.synopsys.integration.alert.api.distribution.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStageRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStatusStatusRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobExecutionRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobExecutionStageRepository;
import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.database.api.DefaultJobCompletionStatusAccessor;
import com.synopsys.integration.alert.database.api.DefaultJobExecutionAccessor;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStageRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStageRepository;

class AuditSuccessHandlerTest {
    private ExecutingJobManager executingJobManager;
    private JobCompletionStatusAccessor jobCompletionStatusAccessor;
    private JobExecutionAccessor jobExecutionAccessor;

    @BeforeEach
    public void init() {
        JobCompletionStatusRepository jobCompletionStatusRepository = new MockJobCompletionStatusStatusRepository();
        JobCompletionStageRepository jobCompletionStageRepository = new MockJobCompletionStageRepository();
        JobExecutionRepository jobExecutionRepository = new MockJobExecutionRepository();
        JobExecutionStageRepository jobExecutionStageRepository = new MockJobExecutionStageRepository();
        jobCompletionStatusAccessor = new DefaultJobCompletionStatusAccessor(jobCompletionStatusRepository, jobCompletionStageRepository);
        jobExecutionAccessor = new DefaultJobExecutionAccessor(jobExecutionRepository, jobExecutionStageRepository);
        executingJobManager = new ExecutingJobManager(jobCompletionStatusAccessor, jobExecutionAccessor);
    }

    @Test
    void handleEventTest() {
        UUID jobId = UUID.randomUUID();
        JobExecutionModel executingJob = executingJobManager.startJob(jobId, 0);
        UUID jobExecutionId = executingJob.getExecutionId();
        AuditSuccessHandler handler = new AuditSuccessHandler(executingJobManager);
        AuditSuccessEvent event = new AuditSuccessEvent(jobExecutionId, Set.of());
        handler.handle(event);
        JobCompletionStatusModel statusModel = jobCompletionStatusAccessor.getJobExecutionStatus(jobId)
            .orElseThrow(() -> new AssertionError("Executing Job cannot be missing from the test."));
        assertEquals(AuditEntryStatus.SUCCESS.name(), statusModel.getLatestStatus());
        assertEquals(1, statusModel.getSuccessCount());
        assertEquals(0, statusModel.getFailureCount());
        assertEquals(0, statusModel.getNotificationCount());
        assertTrue(executingJobManager.getExecutingJob(jobExecutionId).isPresent());
    }

    @Test
    void handleEventAuditMissingTest() {
        UUID jobExecutionId = UUID.randomUUID();
        Set<Long> notificationIds = Set.of(1L, 2L, 3L);
        AlertPagedQueryDetails pagedQueryDetails = new AlertPagedQueryDetails(1, 10);
        AuditSuccessHandler handler = new AuditSuccessHandler(executingJobManager);
        AuditSuccessEvent event = new AuditSuccessEvent(jobExecutionId, notificationIds);
        handler.handle(event);
        Optional<JobExecutionModel> executingJob = executingJobManager.getExecutingJob(jobExecutionId);
        assertTrue(executingJob.isEmpty());
        assertTrue(jobCompletionStatusAccessor.getJobExecutionStatus(pagedQueryDetails).getModels().isEmpty());
        assertTrue(executingJobManager.getExecutingJob(jobExecutionId).isEmpty());
    }
}
