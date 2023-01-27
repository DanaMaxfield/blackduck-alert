package com.synopsys.integration.alert.startup.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;

@Component
@Order(80)
public class ExecutingJobCleanup extends StartupComponent {

    private final ExecutingJobManager executingJobManager;

    @Autowired
    public ExecutingJobCleanup(ExecutingJobManager executingJobManager) {
        this.executingJobManager = executingJobManager;
    }

    @Override
    protected void initialize() {
        executingJobManager.purgeAllJobs();
    }
}
