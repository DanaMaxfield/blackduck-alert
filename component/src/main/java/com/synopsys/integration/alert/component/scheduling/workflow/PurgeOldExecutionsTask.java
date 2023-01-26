package com.synopsys.integration.alert.component.scheduling.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.task.StartupScheduledTask;
import com.synopsys.integration.alert.api.task.TaskManager;

@Component
public class PurgeOldExecutionsTask extends StartupScheduledTask {

    public static final String CRON_FORMAT = "0 0 0/1 1/1 * ?";

    private final ExecutingJobManager executingJobManager;

    @Autowired
    public PurgeOldExecutionsTask(TaskScheduler taskScheduler, TaskManager taskManager, ExecutingJobManager executingJobManager) {
        super(taskScheduler, taskManager);
        this.executingJobManager = executingJobManager;
    }

    @Override
    public String scheduleCronExpression() {
        return CRON_FORMAT;
    }

    @Override
    public void runTask() {
        executingJobManager.purgeOldCompletedJobs();
    }
}
