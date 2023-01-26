package com.synopsys.integration.alert.database.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobExecutionAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobStageModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.common.util.DateUtils;
import com.synopsys.integration.alert.database.job.execution.JobExecutionEntity;
import com.synopsys.integration.alert.database.job.execution.JobExecutionRepository;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStageEntity;
import com.synopsys.integration.alert.database.job.execution.JobExecutionStageRepository;

@Component
public class DefaultJobExecutionAccessor implements JobExecutionAccessor {

    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionStageRepository jobExecutionStageRepository;

    @Autowired
    public DefaultJobExecutionAccessor(JobExecutionRepository jobExecutionRepository, JobExecutionStageRepository jobExecutionStageRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutionStageRepository = jobExecutionStageRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public JobExecutionModel startJob(UUID jobConfigId, int totalNotificationCount) {
        JobExecutionEntity entity = new JobExecutionEntity(
            UUID.randomUUID(),
            jobConfigId,
            DateUtils.createCurrentDateTimestamp(),
            null,
            AuditEntryStatus.PENDING.name(),
            0,
            totalNotificationCount,
            false
        );
        entity = jobExecutionRepository.save(entity);
        return convertToExecutionModel(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void endJobWithSuccess(UUID executionId, Instant endTime) {
        endJobWithStatus(executionId, endTime, AuditEntryStatus.SUCCESS);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void endJobWithFailure(UUID executionId, Instant endTime) {
        endJobWithStatus(executionId, endTime, AuditEntryStatus.FAILURE);
    }

    private void endJobWithStatus(UUID executionId, Instant endTime, AuditEntryStatus status) {
        Optional<JobExecutionEntity> searchedEntity = jobExecutionRepository.findById(executionId);
        if (searchedEntity.isPresent()) {
            OffsetDateTime end = DateUtils.fromInstantUTC(endTime);
            JobExecutionEntity savedEntity = searchedEntity.get();
            JobExecutionEntity updatedEntity = new JobExecutionEntity(
                savedEntity.getExecutionId(),
                savedEntity.getJobConfigId(),
                savedEntity.getStart(),
                end,
                status.name(),
                savedEntity.getProcessedNotificationCount(),
                savedEntity.getTotalNotificationCount(),
                savedEntity.isCompletionCounted()
            );
            jobExecutionRepository.save(updatedEntity);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void incrementNotificationCount(UUID jobExecutionId, int notificationCount) {
        Optional<JobExecutionEntity> searchedEntity = jobExecutionRepository.findById(jobExecutionId);
        if (searchedEntity.isPresent()) {
            JobExecutionEntity entity = searchedEntity.get();
            int incrementedValue = entity.getProcessedNotificationCount() + notificationCount;
            jobExecutionRepository.save(new JobExecutionEntity(
                entity.getExecutionId(),
                entity.getJobConfigId(),
                entity.getStart(),
                entity.getEnd(),
                entity.getStatus(),
                incrementedValue,
                entity.getTotalNotificationCount(),
                entity.isCompletionCounted()
            ));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobExecutionModel> getJobExecution(UUID jobExecutionId) {
        return jobExecutionRepository.findById(jobExecutionId).map(this::convertToExecutionModel);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertPagedModel<JobExecutionModel> getExecutingJobs(AlertPagedQueryDetails pagedQueryDetails) {
        PageRequest pageRequest = PageRequest.of(pagedQueryDetails.getOffset(), pagedQueryDetails.getLimit());
        Page<JobExecutionEntity> page = jobExecutionRepository.findAllByStatusIn(Set.of(AuditEntryStatus.PENDING.name()), pageRequest);
        List<JobExecutionModel> data = page
            .stream()
            .map(this::convertToExecutionModel)
            .collect(Collectors.toList());
        return new AlertPagedModel<>(page.getTotalPages(), page.getNumber(), page.getNumberOfElements(), data);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertPagedModel<JobExecutionModel> getCompletedJobs(AlertPagedQueryDetails pagedQueryDetails) {
        PageRequest pageRequest = PageRequest.of(pagedQueryDetails.getOffset(), pagedQueryDetails.getLimit());
        Page<JobExecutionEntity> page = jobExecutionRepository.findAllByStatusIn(Set.of(AuditEntryStatus.FAILURE.name(), AuditEntryStatus.SUCCESS.name()), pageRequest);
        List<JobExecutionModel> data = page
            .stream()
            .map(this::convertToExecutionModel)
            .collect(Collectors.toList());
        return new AlertPagedModel<>(page.getTotalPages(), page.getNumber(), page.getNumberOfElements(), data);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countJobExecutionsByStatus(UUID jobConfigId, AuditEntryStatus status) {
        return jobExecutionRepository.countAllByCompletionCountedFalseAndJobConfigIdAndStatusIn(jobConfigId, Set.of(status.name()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void markAllExecutionsForJobAggregated(UUID jobConfigId) {
        jobExecutionRepository.updateCompletionCountedTrueForJobConfig(jobConfigId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobStageModel> getJobStage(UUID jobExecutionId, int stageId) {
        return jobExecutionStageRepository.findByExecutionIdAndStage(jobExecutionId, stageId).map(this::convertToStageModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobStageModel> getJobStages(UUID jobExecutionId) {
        List<JobExecutionStageEntity> stages = jobExecutionStageRepository.findAllByExecutionId(jobExecutionId);
        return stages
            .stream()
            .map(this::convertToStageModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void startStage(UUID executionId, int stageId, Instant start) {
        Optional<JobExecutionStageEntity> stageData = jobExecutionStageRepository.findByExecutionIdAndStage(executionId, stageId);
        if (stageData.isEmpty()) {
            jobExecutionStageRepository.save(new JobExecutionStageEntity(executionId, stageId, DateUtils.fromInstantUTC(start), null));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void endStage(UUID executionId, int stageId, Instant end) {
        Optional<JobExecutionStageEntity> stageData = jobExecutionStageRepository.findByExecutionIdAndStage(executionId, stageId);
        if (stageData.isPresent()) {
            JobExecutionStageEntity stage = stageData.get();
            JobExecutionStageEntity updatedStage = new JobExecutionStageEntity(
                stage.getExecutionId(),
                stage.getStage(),
                stage.getStart(),
                DateUtils.fromInstantUTC(end)
            );
            jobExecutionStageRepository.save(updatedStage);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void purgeJob(UUID executionId) {
        jobExecutionRepository.deleteById(executionId);
    }

    private JobExecutionModel convertToExecutionModel(JobExecutionEntity entity) {
        return new JobExecutionModel(
            entity.getExecutionId(),
            entity.getJobConfigId(),
            entity.getStart(),
            entity.getEnd(),
            AuditEntryStatus.valueOf(entity.getStatus()),
            entity.getProcessedNotificationCount(),
            entity.getTotalNotificationCount(),
            entity.isCompletionCounted()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void purgeOldCompletedJobs() {
        jobExecutionRepository.purgeCompletedAggregatedJobs(DateUtils.createCurrentDateTimestamp(), Set.of(AuditEntryStatus.SUCCESS.name(), AuditEntryStatus.FAILURE.name()));
    }

    private JobStageModel convertToStageModel(JobExecutionStageEntity entity) {
        return new JobStageModel(entity.getExecutionId(), entity.getStage(), entity.getStart(), entity.getEnd());
    }
}
