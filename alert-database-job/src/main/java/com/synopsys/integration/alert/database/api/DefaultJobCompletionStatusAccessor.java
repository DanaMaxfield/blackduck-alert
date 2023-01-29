package com.synopsys.integration.alert.database.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStageModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.common.util.DateUtils;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStageEntity;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStageRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusEntity;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;

@Component
public class DefaultJobCompletionStatusAccessor implements JobCompletionStatusAccessor {

    private final JobCompletionStatusRepository jobCompletionStatusRepository;
    private final JobCompletionStageRepository jobCompletionStageRepository;

    @Autowired
    public DefaultJobCompletionStatusAccessor(
        JobCompletionStatusRepository jobCompletionStatusRepository,
        JobCompletionStageRepository jobCompletionStageRepository
    ) {
        this.jobCompletionStatusRepository = jobCompletionStatusRepository;
        this.jobCompletionStageRepository = jobCompletionStageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobCompletionStatusModel> getJobExecutionStatus(UUID jobConfigId) {
        return jobCompletionStatusRepository.findById(jobConfigId).map(this::convertToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertPagedModel<JobCompletionStatusModel> getJobExecutionStatus(AlertPagedQueryDetails pagedQueryDetails) {
        Sort sort = (pagedQueryDetails.getSortName().isPresent() && pagedQueryDetails.getSortOrder().isPresent()) ?
            Sort.by(pagedQueryDetails.getSortOrder().get(), pagedQueryDetails.getSortName().get()) :
            Sort.unsorted();
        PageRequest pageRequest = PageRequest.of(pagedQueryDetails.getOffset(), pagedQueryDetails.getLimit(), sort);

        Page<JobCompletionStatusEntity> entities;
        if (pagedQueryDetails.getSearchTerm().filter(StringUtils::isNotBlank).isPresent()) {
            entities = jobCompletionStatusRepository.findBySearchTerm(pagedQueryDetails.getSearchTerm().get(), pageRequest);
        } else {
            entities = jobCompletionStatusRepository.findAll(pageRequest);
        }
        List<JobCompletionStatusModel> pageContents = entities.map(this::convertToModel).getContent();
        return new AlertPagedModel<>(entities.getTotalPages(), pagedQueryDetails.getOffset(), pagedQueryDetails.getLimit(), pageContents);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveExecutionStatus(JobCompletionStatusModel latestData) {
        JobCompletionStatusModel updatedModel = createAggregatedStatusModel(latestData);
        JobCompletionStatusEntity jobExecutionStatus = convertFromModel(updatedModel);
        jobCompletionStatusRepository.save(jobExecutionStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobCompletionStageModel> getJobStageData(UUID jobConfigId) {
        return jobCompletionStageRepository.findAllByJobConfigId(jobConfigId)
            .stream()
            .map(this::convertToStageModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveJobStageData(JobCompletionStageModel jobCompletionStageModel) {
        JobCompletionStageModel updatedModel = createAggregatedStageModel(jobCompletionStageModel);
        jobCompletionStageRepository.save(convertFromStageModel(updatedModel));
    }

    private JobCompletionStatusModel convertToModel(JobCompletionStatusEntity entity) {
        return new JobCompletionStatusModel(
            entity.getJobConfigId(),
            entity.getLatestNotificationCount(),
            entity.getTotalNotificationCount(),
            entity.getSuccessCount(),
            entity.getFailureCount(),
            entity.getLatestStatus(),
            DateUtils.fromInstantUTC(entity.getLastRun().toInstant()),
            entity.getDurationNanos()
        );
    }

    private JobCompletionStatusEntity convertFromModel(JobCompletionStatusModel model) {
        return new JobCompletionStatusEntity(
            model.getJobConfigId(),
            model.getLatestNotificationCount(),
            model.getTotalNotificationCount(),
            model.getSuccessCount(),
            model.getFailureCount(),
            model.getLatestStatus(),
            DateUtils.fromInstantUTC(model.getLastRun().toInstant()),
            model.getDurationNanos()
        );
    }

    private JobCompletionStageModel convertToStageModel(JobCompletionStageEntity entity) {
        return new JobCompletionStageModel(entity.getJobConfigId(), entity.getStage(), entity.getDurationNano());
    }

    private JobCompletionStageEntity convertFromStageModel(JobCompletionStageModel model) {
        return new JobCompletionStageEntity(model.getJobConfigId(), model.getStageId(), model.getDurationNano());
    }

    private JobCompletionStatusModel createAggregatedStatusModel(JobCompletionStatusModel latestData) {
        UUID jobConfigId = latestData.getJobConfigId();
        JobCompletionStatusModel resultStatus;
        Optional<JobCompletionStatusModel> status = getJobExecutionStatus(jobConfigId);
        resultStatus = status
            .map(savedStatus -> updateCompletedJobStatus(latestData, savedStatus))
            .orElseGet(() -> createInitialCompletedJobStatus(latestData));

        return resultStatus;
    }

    private JobCompletionStatusModel updateCompletedJobStatus(JobCompletionStatusModel latestData, JobCompletionStatusModel savedStatus) {
        long successCount = savedStatus.getSuccessCount();
        long failureCount = savedStatus.getFailureCount();
        AuditEntryStatus jobStatus = AuditEntryStatus.valueOf(latestData.getLatestStatus());

        if (jobStatus == AuditEntryStatus.SUCCESS) {
            successCount = savedStatus.getSuccessCount() + latestData.getSuccessCount();
        }

        if (jobStatus == AuditEntryStatus.FAILURE) {
            failureCount = savedStatus.getFailureCount() + latestData.getFailureCount();
        }

        long totalNotificationCount = savedStatus.getTotalNotificationCount() + latestData.getTotalNotificationCount();

        long duration;

        if (0L == savedStatus.getDurationNanos()) {
            duration = latestData.getDurationNanos();
        } else {
            duration = calculateAverage(latestData.getDurationNanos(), savedStatus.getDurationNanos());
        }

        return new JobCompletionStatusModel(
            latestData.getJobConfigId(),
            latestData.getLatestNotificationCount(),
            totalNotificationCount,
            successCount,
            failureCount,
            jobStatus.name(),
            latestData.getLastRun(),
            duration
        );
    }

    private JobCompletionStatusModel createInitialCompletedJobStatus(JobCompletionStatusModel latestData) {
        long successCount = 0L;
        long failureCount = 0L;
        AuditEntryStatus jobStatus = AuditEntryStatus.valueOf(latestData.getLatestStatus());

        if (jobStatus == AuditEntryStatus.SUCCESS) {
            successCount = 1L;
        }

        if (jobStatus == AuditEntryStatus.FAILURE) {
            failureCount = 1L;
        }

        return new JobCompletionStatusModel(
            latestData.getJobConfigId(),
            latestData.getLatestNotificationCount(),
            latestData.getTotalNotificationCount(),
            successCount,
            failureCount,
            jobStatus.name(),
            latestData.getLastRun(),
            latestData.getDurationNanos()
        );
    }

    private JobCompletionStageModel createAggregatedStageModel(JobCompletionStageModel latestData) {
        UUID jobConfigId = latestData.getJobConfigId();
        Integer stageId = latestData.getStageId();

        Optional<JobCompletionStageEntity> currentStageData = jobCompletionStageRepository.findByJobConfigIdAndStage(jobConfigId, stageId);
        return currentStageData
            .map(savedStatus -> updateCompletedStageData(latestData, savedStatus))
            .orElse(latestData);
    }

    private JobCompletionStageModel updateCompletedStageData(JobCompletionStageModel latestData, JobCompletionStageEntity currentSavedData) {
        return new JobCompletionStageModel(
            currentSavedData.getJobConfigId(),
            currentSavedData.getStage(),
            calculateAverage(latestData.getDurationNano(), currentSavedData.getDurationNano())
        );
    }

    private Long calculateAverage(Long firstValue, Long secondValue) {
        if (firstValue == 0 && secondValue == 0) {
            return 0L;
        }
        return (firstValue + secondValue) / 2;
    }
}
