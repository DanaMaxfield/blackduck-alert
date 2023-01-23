package com.synopsys.integration.alert.database.job.execution;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {
    Page<JobExecutionEntity> findAllByStatusIn(Set<String> statuses, Pageable pageable);

    long countAllByStatusIn(Set<String> statuses);
}
