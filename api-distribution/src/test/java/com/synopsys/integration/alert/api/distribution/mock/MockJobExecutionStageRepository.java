package com.synopsys.integration.alert.api.distribution.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    public Page<JobExecutionStageEntity> findAllByExecutionId(UUID jobExecutionId, Pageable pageable) {
        Predicate<JobExecutionStageEntity> executionIdMatch = entity -> entity.getExecutionId().equals(jobExecutionId);
        List<JobExecutionStageEntity> entities = findAll().stream()
            .filter(executionIdMatch)
            .collect(Collectors.toList());
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        List<List<JobExecutionStageEntity>> partitionedLists = ListUtils.partition(entities, pageSize);
        int totalPages = partitionedLists.size();
        if (partitionedLists.size() >= pageNumber) {
            return new PageImpl<>(partitionedLists.get(pageNumber), pageable, totalPages);
        } else {
            return new PageImpl<>(List.of());
        }
    }

    @Override
    public Optional<JobExecutionStageEntity> findByExecutionIdAndStage(UUID jobExecutionId, String stage) {
        Predicate<JobExecutionStageEntity> executionIdMatch = entity -> entity.getExecutionId().equals(jobExecutionId);
        Predicate<JobExecutionStageEntity> stageNameMatch = entity -> entity.getStage().equals(stage);
        return findAll()
            .stream()
            .filter(executionIdMatch.and(stageNameMatch))
            .findFirst();
    }
}
