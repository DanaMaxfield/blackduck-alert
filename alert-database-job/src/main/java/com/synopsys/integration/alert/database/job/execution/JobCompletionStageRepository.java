package com.synopsys.integration.alert.database.job.execution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCompletionStageRepository extends JpaRepository<JobCompletionStageEntity, JobCompletionStagePK> {

    List<JobCompletionStageEntity> findAllByJobConfigId(UUID jobConfigId);

    Optional<JobCompletionStageEntity> findByJobConfigIdAndStage(UUID jobConfigId, int stageId);
}
