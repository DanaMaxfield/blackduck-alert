package com.synopsys.integration.alert.api.distribution.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJob;
import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStatusDurationRepository;
import com.synopsys.integration.alert.api.distribution.mock.MockJobCompletionStatusStatusRepository;
import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionStatusModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.database.api.DefaultJobCompletionStatusAccessor;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusDurationRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;

class AuditSuccessHandlerTest {
    private ExecutingJobManager executingJobManager;
    private JobCompletionStatusAccessor jobCompletionStatusAccessor;

    @BeforeEach
    public void init() {
        JobCompletionStatusDurationRepository jobCompletionStatusDurationRepository = new MockJobCompletionStatusDurationRepository();
        JobCompletionStatusRepository jobCompletionStatusRepository = new MockJobCompletionStatusStatusRepository(jobCompletionStatusDurationRepository);

        jobCompletionStatusAccessor = new DefaultJobCompletionStatusAccessor(jobCompletionStatusRepository, jobCompletionStatusDurationRepository);
        executingJobManager = new ExecutingJobManager(jobCompletionStatusAccessor);
    }

    @Test
    void handleEventTest() {
        UUID jobId = UUID.randomUUID();
        ExecutingJob executingJob = executingJobManager.startJob(jobId, 0);
        UUID jobExecutionId = executingJob.getExecutionId();
        AuditSuccessHandler handler = new AuditSuccessHandler(executingJobManager);
        AuditSuccessEvent event = new AuditSuccessEvent(jobExecutionId, Set.of());
        handler.handle(event);
        JobExecutionStatusModel statusModel = jobCompletionStatusAccessor.getJobExecutionStatus(jobId)
            .orElseThrow(() -> new AssertionError("Executing Job cannot be missing from the test."));
        assertEquals(AuditEntryStatus.SUCCESS.name(), statusModel.getLatestStatus());
        assertEquals(1, statusModel.getSuccessCount());
        assertEquals(0, statusModel.getFailureCount());
        assertEquals(0, statusModel.getNotificationCount());
        assertTrue(executingJobManager.getExecutingJob(jobExecutionId).isEmpty());
    }

    @Test
    void handleEventAuditMissingTest() {
        UUID jobExecutionId = UUID.randomUUID();
        Set<Long> notificationIds = Set.of(1L, 2L, 3L);
        AlertPagedQueryDetails pagedQueryDetails = new AlertPagedQueryDetails(1, 10);
        AuditSuccessHandler handler = new AuditSuccessHandler(executingJobManager);
        AuditSuccessEvent event = new AuditSuccessEvent(jobExecutionId, notificationIds);
        handler.handle(event);
        Optional<ExecutingJob> executingJob = executingJobManager.getExecutingJob(jobExecutionId);
        assertTrue(executingJob.isEmpty());
        assertTrue(jobCompletionStatusAccessor.getJobExecutionStatus(pagedQueryDetails).getModels().isEmpty());
        assertTrue(executingJobManager.getExecutingJob(jobExecutionId).isEmpty());
    }
}
