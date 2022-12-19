/*
 * api-processor
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.processor.api.distribute;

import java.util.UUID;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;
import com.synopsys.integration.alert.common.persistence.model.job.DistributionJobModel;

public final class ProcessedNotificationDetails extends AlertSerializableModel {
    private final UUID jobId;
    private final String channelName;
    private final String jobName;
    private final UUID jobExecutionId;

    public static ProcessedNotificationDetails fromDistributionJob(UUID jobExecutionId, DistributionJobModel distributionJobModel) {
        return new ProcessedNotificationDetails(
            distributionJobModel.getJobId(),
            distributionJobModel.getChannelDescriptorName(),
            distributionJobModel.getName(),
            jobExecutionId
        );
    }

    public ProcessedNotificationDetails(final UUID jobId, final String channelName, final String jobName, final UUID jobExecutionId) {
        this.jobId = jobId;
        this.channelName = channelName;
        this.jobName = jobName;
        this.jobExecutionId = jobExecutionId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getJobExecutionId() {
        return jobExecutionId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getJobName() {
        return jobName;
    }
}
