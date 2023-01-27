/*
 * component
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.component.diagnostic.database;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synopsys.integration.alert.api.distribution.execution.ExecutingJobManager;
import com.synopsys.integration.alert.api.distribution.execution.JobStage;
import com.synopsys.integration.alert.common.enumeration.AuditEntryStatus;
import com.synopsys.integration.alert.common.persistence.accessor.DiagnosticAccessor;
import com.synopsys.integration.alert.common.persistence.accessor.JobCompletionStatusAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.DistributionJobModel;
import com.synopsys.integration.alert.common.persistence.model.job.DistributionJobModelData;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStageModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobExecutionModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobStageModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;
import com.synopsys.integration.alert.common.util.DateUtils;
import com.synopsys.integration.alert.component.diagnostic.model.AuditDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.DiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobDurationDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobExecutionDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobExecutionStageDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobExecutionsDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobStageStatusDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.JobStatusDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.NotificationDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.RabbitMQDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.model.SystemDiagnosticModel;
import com.synopsys.integration.alert.component.diagnostic.utility.RabbitMQDiagnosticUtility;
import com.synopsys.integration.alert.database.api.StaticJobAccessor;
import com.synopsys.integration.alert.database.audit.AuditEntryRepository;
import com.synopsys.integration.alert.database.notification.NotificationContentRepository;

@Component
public class DefaultDiagnosticAccessor implements DiagnosticAccessor {
    private final NotificationContentRepository notificationContentRepository;
    private final AuditEntryRepository auditEntryRepository;
    private final RabbitMQDiagnosticUtility rabbitMQDiagnosticUtility;

    private final StaticJobAccessor jobAccessor;
    private final ExecutingJobManager executingJobManager;
    private final JobCompletionStatusAccessor jobCompletionStatusAccessor;

    @Autowired
    public DefaultDiagnosticAccessor(
        NotificationContentRepository notificationContentRepository,
        AuditEntryRepository auditEntryRepository,
        RabbitMQDiagnosticUtility rabbitMQDiagnosticUtility,
        StaticJobAccessor staticJobAccessor,
        ExecutingJobManager executingJobManager,
        JobCompletionStatusAccessor jobCompletionStatusAccessor
    ) {
        this.notificationContentRepository = notificationContentRepository;
        this.auditEntryRepository = auditEntryRepository;
        this.rabbitMQDiagnosticUtility = rabbitMQDiagnosticUtility;
        this.jobAccessor = staticJobAccessor;
        this.executingJobManager = executingJobManager;
        this.jobCompletionStatusAccessor = jobCompletionStatusAccessor;
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosticModel getDiagnosticInfo() {
        NotificationDiagnosticModel notificationDiagnosticModel = getNotificationDiagnosticInfo();
        AuditDiagnosticModel auditDiagnosticModel = getAuditDiagnosticInfo();
        SystemDiagnosticModel systemDiagnosticModel = getSystemInfo();
        RabbitMQDiagnosticModel rabbitMQDiagnosticModel = rabbitMQDiagnosticUtility.getRabbitMQDiagnostics();
        JobDiagnosticModel jobDiagnosticModel = getJobDiagnosticModel();
        JobExecutionsDiagnosticModel jobExecutionsDiagnosticModel = getExecutingJobDiagnosticModel();
        return new DiagnosticModel(
            LocalDateTime.now().toString(),
            notificationDiagnosticModel,
            auditDiagnosticModel,
            systemDiagnosticModel,
            rabbitMQDiagnosticModel,
            jobDiagnosticModel,
            jobExecutionsDiagnosticModel
        );
    }

    private NotificationDiagnosticModel getNotificationDiagnosticInfo() {
        long numberOfNotifications = notificationContentRepository.count();
        long numberOfNotificationsProcessed = notificationContentRepository.countByProcessed(true);
        long numberOfNotificationsUnprocessed = notificationContentRepository.countByProcessed(false);
        return new NotificationDiagnosticModel(numberOfNotifications, numberOfNotificationsProcessed, numberOfNotificationsUnprocessed);
    }

    private AuditDiagnosticModel getAuditDiagnosticInfo() {
        long numberOfAuditEntriesSuccessful = auditEntryRepository.countByStatus(AuditEntryStatus.SUCCESS.name());
        long numberOfAuditEntriesFailed = auditEntryRepository.countByStatus(AuditEntryStatus.FAILURE.name());
        long numberOfAuditEntriesPending = auditEntryRepository.countByStatus(AuditEntryStatus.PENDING.name());
        return new AuditDiagnosticModel(
            numberOfAuditEntriesSuccessful,
            numberOfAuditEntriesFailed,
            numberOfAuditEntriesPending,
            auditEntryRepository.getAverageAuditEntryCompletionTime().orElse(AuditDiagnosticModel.NO_AUDIT_CONTENT_MESSAGE)
        );
    }

    private SystemDiagnosticModel getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        return new SystemDiagnosticModel(runtime.availableProcessors(), runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory());
    }

    private JobExecutionsDiagnosticModel getExecutingJobDiagnosticModel() {
        List<JobExecutionDiagnosticModel> jobExecutions = getExecutionData();
        return new JobExecutionsDiagnosticModel(
            jobExecutions
        );
    }

    private List<JobExecutionDiagnosticModel> getExecutionData() {
        List<JobExecutionDiagnosticModel> jobExecutions = new LinkedList<>();
        int pageSize = 100;
        int pageNumber = 0;
        AlertPagedModel<JobExecutionModel> page = executingJobManager.getExecutingJobs(pageNumber, pageSize);
        while (pageNumber < page.getTotalPages()) {
            jobExecutions.addAll(page.getModels().stream()
                .map(this::convertExecutionData)
                .collect(Collectors.toList()));
            pageNumber++;
            page = executingJobManager.getExecutingJobs(pageNumber, pageSize);
        }

        return jobExecutions;
    }

    private JobDiagnosticModel getJobDiagnosticModel() {
        List<JobStatusDiagnosticModel> jobStatusData = new LinkedList<>();
        int pageNumber = 0;
        int pageSize = 100;
        AlertPagedModel<JobCompletionStatusModel> page = jobCompletionStatusAccessor.getJobExecutionStatus(new AlertPagedQueryDetails(pageNumber, pageSize));
        while (pageNumber < page.getTotalPages()) {
            jobStatusData.addAll(page.getModels().stream()
                .map(this::convertJobStatusData)
                .collect(Collectors.toList()));
            pageNumber++;
            page = jobCompletionStatusAccessor.getJobExecutionStatus(new AlertPagedQueryDetails(pageNumber, pageSize));
        }

        return new JobDiagnosticModel(jobStatusData);
    }

    private JobExecutionDiagnosticModel convertExecutionData(JobExecutionModel job) {
        List<JobStageModel> jobStages = executingJobManager.getStages(job.getExecutionId());
        List<JobExecutionStageDiagnosticModel> stageData = new LinkedList<>(jobStages
            .stream()
            .map(this::convertJobStageData)
            .collect(Collectors.toList()));

        Optional<DistributionJobModel> distributionJobModel = jobAccessor.getJobById(job.getJobConfigId());
        String jobName = distributionJobModel.map(DistributionJobModelData::getName).orElse(String.format("Unknown Job (%s)", job.getJobConfigId()));
        String channelName = distributionJobModel.map(DistributionJobModel::getChannelDescriptorName).orElse("Unknown Channel");
        String start = DateUtils.formatDateAsJsonString(job.getStart());
        String end = job.getEnd().map(DateUtils::formatDateAsJsonString).orElse("");

        return new JobExecutionDiagnosticModel(jobName, channelName, start, end, job.getStatus(), job.getProcessedNotificationCount(), job.getTotalNotificationCount(), stageData);
    }

    private String getJobName(UUID jobConfigId) {
        Optional<DistributionJobModel> distributionJobModel = jobAccessor.getJobById(jobConfigId);
        return distributionJobModel.map(DistributionJobModelData::getName).orElse(String.format("Unknown Job (%s)", jobConfigId));
    }

    private JobExecutionStageDiagnosticModel convertJobStageData(JobStageModel executingJobStage) {
        String start = DateUtils.formatDateAsJsonString(executingJobStage.getStart());
        String end = executingJobStage.getEnd().map(DateUtils::formatDateAsJsonString).orElse("");
        return new JobExecutionStageDiagnosticModel(
            JobStage.findByStageId(executingJobStage.getStageId()),
            start,
            end
        );
    }

    private JobStatusDiagnosticModel convertJobStatusData(JobCompletionStatusModel jobCompletionStatusModel) {
        JobDurationDiagnosticModel jobDurationDiagnosticModel = new JobDurationDiagnosticModel(
            DateUtils.formatDurationFromNanos(jobCompletionStatusModel.getDurationNanos()),
            convertStageData(jobCompletionStatusAccessor.getJobStageData(jobCompletionStatusModel.getJobConfigId()))
        );
        String jobName = getJobName(jobCompletionStatusModel.getJobConfigId());
        return new JobStatusDiagnosticModel(
            jobCompletionStatusModel.getJobConfigId(),
            jobName,
            jobCompletionStatusModel.getLatestNotificationCount(),
            jobCompletionStatusModel.getAverageNotificationCount(),
            jobCompletionStatusModel.getSuccessCount(),
            jobCompletionStatusModel.getFailureCount(),
            jobCompletionStatusModel.getLatestStatus(),
            DateUtils.formatDateAsJsonString(jobCompletionStatusModel.getLastRun()),
            jobDurationDiagnosticModel
        );

    }

    private List<JobStageStatusDiagnosticModel> convertStageData(List<JobCompletionStageModel> jobDurationModel) {
        return jobDurationModel.stream()
            .map(stage -> new JobStageStatusDiagnosticModel(JobStage.findByStageId(stage.getStageId()).name(), DateUtils.formatDurationFromNanos(stage.getDurationNano())))
            .collect(Collectors.toList());
    }
}
