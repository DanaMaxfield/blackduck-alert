package com.synopsys.integration.alert.api.distribution.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.google.gson.Gson;
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
import com.synopsys.integration.alert.database.api.DefaultJobCompletionStatusAccessor;
import com.synopsys.integration.alert.database.api.DefaultJobExecutionAccessor;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStageRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStageRepository;

class AuditSuccessEventListenerTest {
    private final Gson gson = new Gson();
    private final TaskExecutor taskExecutor = new SyncTaskExecutor();

    private ExecutingJobManager executingJobManager;
    private JobCompletionStatusAccessor jobCompletionStatusAccessor;
    private AuditSuccessHandler handler;

    @BeforeEach
    public void init() {
        JobCompletionStatusRepository jobCompletionStatusRepository = new MockJobCompletionStatusStatusRepository();
        JobCompletionStageRepository jobCompletionStageRepository = new MockJobCompletionStageRepository();
        JobExecutionRepository jobExecutionRepository = new MockJobExecutionRepository();
        JobExecutionStageRepository jobExecutionStageRepository = new MockJobExecutionStageRepository();

        jobCompletionStatusAccessor = new DefaultJobCompletionStatusAccessor(jobCompletionStatusRepository, jobCompletionStageRepository);
        JobExecutionAccessor jobExecutionAccessor = new DefaultJobExecutionAccessor(jobExecutionRepository, jobExecutionStageRepository);
        executingJobManager = new ExecutingJobManager(jobCompletionStatusAccessor, jobExecutionAccessor);
        handler = new AuditSuccessHandler(executingJobManager);
    }

    @Test
    void onMessageTest() {
        UUID jobId = UUID.randomUUID();
        Set<Long> notificationIds = Set.of(1L, 2L, 3L);
        JobExecutionModel executingJob = executingJobManager.startJob(jobId, notificationIds.size());
        UUID executingJobId = executingJob.getExecutionId();

        AuditSuccessEventListener listener = new AuditSuccessEventListener(gson, taskExecutor, handler);
        AuditSuccessEvent event = new AuditSuccessEvent(executingJobId, notificationIds);
        Message message = new Message(gson.toJson(event).getBytes());
        listener.onMessage(message);

        JobCompletionStatusModel statusModel = jobCompletionStatusAccessor.getJobExecutionStatus(jobId)
            .orElseThrow(() -> new AssertionError("Executing Job cannot be missing from the test."));
        assertEquals(AuditEntryStatus.SUCCESS.name(), statusModel.getLatestStatus());
        assertEquals(1, statusModel.getSuccessCount());
        assertEquals(0, statusModel.getFailureCount());
        assertEquals(0, statusModel.getAverageNotificationCount());
        assertTrue(executingJobManager.getExecutingJob(executingJobId).isPresent());
    }
}
