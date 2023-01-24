package com.synopsys.integration.alert.api.distribution.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.synopsys.integration.alert.database.job.execution.JobCompletionStageEntity;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStagePK;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStageRepository;
import com.synopsys.integration.alert.test.common.database.MockRepositoryContainer;

public class MockJobCompletionStageRepository extends MockRepositoryContainer<JobCompletionStagePK, JobCompletionStageEntity> implements JobCompletionStageRepository {

    public MockJobCompletionStageRepository() {
        super(MockJobCompletionStageRepository::createPrimaryKey);
    }

    private static JobCompletionStagePK createPrimaryKey(JobCompletionStageEntity entity) {
        return new JobCompletionStagePK(entity.getJobConfigId(), entity.getStage());
    }

    @Override
    public List<JobCompletionStageEntity> findAllByJobConfigId(UUID jobConfigId) {
        return null;
    }

    @Override
    public Optional<JobCompletionStageEntity> findByJobConfigIdAndStage(UUID jobConfigId, int stageId) {
        return Optional.empty();
    }
}
