package com.synopsys.integration.alert.api.distribution.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.synopsys.integration.alert.database.job.execution.JobExecutionStageEntity;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStagePK;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStageRepository;
import com.synopsys.integration.alert.test.common.database.MockRepositoryContainer;

public class MockJobExecutionStageRepository extends MockRepositoryContainer<JobExecutionStagePK, JobExecutionStageEntity> implements JobExecutionStageRepository {

    public MockJobExecutionStageRepository() {
        super(MockJobExecutionStageRepository::createPrimaryKey);
    }

    private static JobExecutionStagePK createPrimaryKey(JobExecutionStageEntity entity) {
        return new JobExecutionStagePK(entity.getExecutionId(), entity.getStage());
    }

    @Override
    public List<JobExecutionStageEntity> findAllByExecutionId(UUID jobExecutionId) {
        Predicate<JobExecutionStageEntity> executionIdMatch = entity -> entity.getExecutionId().equals(jobExecutionId);
        return findAll().stream()
            .filter(executionIdMatch)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<JobExecutionStageEntity> findByExecutionIdAndStage(UUID jobExecutionId, int stageId) {
        Predicate<JobExecutionStageEntity> executionIdMatch = entity -> entity.getExecutionId().equals(jobExecutionId);
        Predicate<JobExecutionStageEntity> stageNameMatch = entity -> entity.getStage() == stageId;
        return findAll()
            .stream()
            .filter(executionIdMatch.and(stageNameMatch))
            .findFirst();
    }
}
