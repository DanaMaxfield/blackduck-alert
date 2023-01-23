package com.synopsys.integration.alert.database.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusDurations;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusDurationRepository;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusDurationsEntity;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusEntity;
import com.synopsys.integration.alert.database.job.execution.JobCompletionStatusRepository;

@Component
public class DefaultJobCompletionStatusAccessor implements JobCompletionStatusAccessor {

    private final JobCompletionStatusRepository jobCompletionStatusRepository;
    private final JobCompletionStatusDurationRepository jobCompletionStatusDurationRepository;

    @Autowired
    public DefaultJobCompletionStatusAccessor(
        JobCompletionStatusRepository jobCompletionStatusRepository,
        JobCompletionStatusDurationRepository jobCompletionStatusDurationRepository
    ) {
        this.jobCompletionStatusRepository = jobCompletionStatusRepository;
        this.jobCompletionStatusDurationRepository = jobCompletionStatusDurationRepository;
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
    public void saveExecutionStatus(JobCompletionStatusModel statusModel) {
        JobCompletionStatusDurationsEntity durations = convertDurationFromModel(statusModel.getJobConfigId(), statusModel.getDurations());
        JobCompletionStatusEntity jobExecutionStatus = convertFromModel(statusModel);
        jobCompletionStatusRepository.save(jobExecutionStatus);
        jobCompletionStatusDurationRepository.save(durations);
    }

    private JobCompletionStatusModel convertToModel(JobCompletionStatusEntity entity) {
        JobCompletionStatusDurations durations = convertDurationToModel(jobCompletionStatusDurationRepository.findById(entity.getJobConfigId())
            .orElseGet(() -> createEmptyDurations(entity.getJobConfigId())));
        return new JobCompletionStatusModel(
            entity.getJobConfigId(),
            entity.getNotificationCount(),
            entity.getSuccessCount(),
            entity.getFailureCount(),
            entity.getLatestStatus(),
            entity.getLastRun(),
            durations
        );
    }

    private JobCompletionStatusDurations convertDurationToModel(JobCompletionStatusDurationsEntity entity) {
        return new JobCompletionStatusDurations(
            entity.getJobDurationNanosec(),
            entity.getNotificationProcessingDuration(),
            entity.getChannelProcessingDuration(),
            entity.getIssueCreationDuration(),
            entity.getIssueCommentingDuration(),
            entity.getIssueTransitionDuration()
        );
    }

    private JobCompletionStatusEntity convertFromModel(JobCompletionStatusModel model) {
        return new JobCompletionStatusEntity(
            model.getJobConfigId(),
            model.getNotificationCount(),
            model.getSuccessCount(),
            model.getFailureCount(),
            model.getLatestStatus(),
            model.getLastRun()
        );
    }

    private JobCompletionStatusDurationsEntity convertDurationFromModel(UUID jobConfigId, JobCompletionStatusDurations model) {
        return new JobCompletionStatusDurationsEntity(
            jobConfigId,
            model.getJobDurationMillisec(),
            model.getNotificationProcessingDuration().orElse(null),
            model.getChannelProcessingDuration().orElse(null),
            model.getIssueCreationDuration().orElse(null),
            model.getIssueCommentingDuration().orElse(null),
            model.getIssueTransitionDuration().orElse(null)
        );
    }

    private JobCompletionStatusDurationsEntity createEmptyDurations(UUID jobConfigId) {
        return new JobCompletionStatusDurationsEntity(
            jobConfigId,
            0L,
            null,
            null,
            null,
            null,
            null
        );
    }
}
