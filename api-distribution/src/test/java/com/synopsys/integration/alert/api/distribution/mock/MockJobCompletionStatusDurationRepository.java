package com.synopsys.integration.alert.api.distribution.mock;

import java.util.UUID;

import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusDurationRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusDurationsEntity;
import com.synopsys.integration.alert.test.common.database.MockRepositoryContainer;

public class MockJobCompletionStatusDurationRepository extends MockRepositoryContainer<UUID, JobCompletionStatusDurationsEntity> implements JobCompletionStatusDurationRepository {
    public MockJobCompletionStatusDurationRepository() {
        super(JobCompletionStatusDurationsEntity::getJobConfigId);
    }
}
