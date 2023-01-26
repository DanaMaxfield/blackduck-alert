package com.synopsys.integration.alert.component.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.task.ScheduledTask;
import com.synopsys.integration.alert.component.scheduling.workflow.PurgeOldExecutionsTask;

class PurgeOldExecutionTaskTest {

    @Test
    void testGetTaskName() {
        PurgeOldExecutionsTask task = new PurgeOldExecutionsTask(null, null, null);
        assertEquals(ScheduledTask.computeTaskName(task.getClass()), task.getTaskName());
    }

    @Test
    void testCronExpressionName() {
        PurgeOldExecutionsTask task = new PurgeOldExecutionsTask(null, null, null);
        assertEquals(PurgeOldExecutionsTask.CRON_FORMAT, task.scheduleCronExpression());
    }

    @Test
    void testRunExecution() {
        ExecutingJobManager executingJobManager = Mockito.mock(ExecutingJobManager.class);
        PurgeOldExecutionsTask task = new PurgeOldExecutionsTask(null, null, executingJobManager);
        task.runTask();
        Mockito.verify(executingJobManager).purgeOldCompletedJobs();
    }
}
