package com.synopsys.integration.alert.api.distribution.mock;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusEntity;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;
import com.synopsys.integration.alert.test.common.database.MockRepositoryContainer;

public class MockJobCompletionStatusStatusRepository extends MockRepositoryContainer<UUID, JobCompletionStatusEntity> implements JobCompletionStatusRepository {

    public MockJobCompletionStatusStatusRepository() {
        super(JobCompletionStatusEntity::getJobConfigId);
    }

    @Override
    public Page<JobCompletionStatusEntity> findBySearchTerm(String searchTerm, Pageable pageable) {
        return Page.empty();
    }

}
