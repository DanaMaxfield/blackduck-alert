package com.synopsys.integration.alert.api.distribution.mock;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.synopsys.integration.alert.database.job.execution.JobExecutionEntity;
import com.synopsys.integration.alert.database.job.execution.JobExecutionRepository;
import com.synopsys.integration.alert.test.common.database.MockRepositoryContainer;

public class MockJobExecutionRepository extends MockRepositoryContainer<UUID, JobExecutionEntity> implements JobExecutionRepository {

    public MockJobExecutionRepository() {
        super(JobExecutionEntity::getExecutionId);
    }

    @Override
    public Page<JobExecutionEntity> findAllByStatusIn(Set<String> statuses, Pageable pageable) {
        Predicate<JobExecutionEntity> containsStatus = entity -> statuses.contains(entity.getStatus());
        List<JobExecutionEntity> entities = findAll()
            .stream()
            .filter(containsStatus)
            .collect(Collectors.toList());
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        List<List<JobExecutionEntity>> partitionedLists = ListUtils.partition(entities, pageSize);
        int totalPages = partitionedLists.size();
        if (partitionedLists.size() >= pageNumber) {
            return new PageImpl<>(partitionedLists.get(pageNumber), pageable, totalPages);
        } else {
            return new PageImpl<>(List.of());
        }
    }

    @Override
    public long countAllByStatusIn(Set<String> statuses) {
        Predicate<JobExecutionEntity> containsStatus = entity -> statuses.contains(entity.getStatus());
        return findAll()
            .stream()
            .filter(containsStatus)
            .count();
    }
}
