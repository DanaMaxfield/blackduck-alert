package com.synopsys.integration.alert.database.job.execution;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionStageRepository extends JpaRepository<JobExecutionStageEntity, UUID> {
    Page<JobExecutionStageEntity> findAllByExecutionId(UUID jobExecutionId, Pageable pageable);
}
