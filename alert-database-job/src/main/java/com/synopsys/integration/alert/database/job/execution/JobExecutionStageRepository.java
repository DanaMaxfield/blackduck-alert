package com.synopsys.integration.alert.database.job.execution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionStageRepository extends JpaRepository<JobExecutionStageEntity, JobExecutionStagePK> {
    List<JobExecutionStageEntity> findAllByExecutionId(UUID jobExecutionId);

    Optional<JobExecutionStageEntity> findByExecutionIdAndStage(UUID jobExecutionId, int stageId);
}
