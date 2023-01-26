package com.synopsys.integration.alert.database.job.execution;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {
    Page<JobExecutionEntity> findAllByStatusIn(Set<String> statuses, Pageable pageable);

    long countAllByStatusIn(Set<String> statuses);

    long countAllByCompletionCountedFalseAndJobConfigIdAndStatusIn(UUID jobConfigId, Set<String> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JobExecutionEntity entity"
        + " SET entity.completionCounted = true"
        + " WHERE entity.jobConfigId = :jobConfigId"
    )
    void updateCompletionCountedTrueForJobConfig(@Param("jobConfigId") UUID jobConfigId);
}
