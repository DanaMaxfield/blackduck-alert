package com.synopsys.integration.alert.common.persistence.accessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStageModel;
import com.synopsys.integration.alert.common.persistence.model.job.executions.JobCompletionStatusModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedModel;
import com.synopsys.integration.alert.common.rest.model.AlertPagedQueryDetails;

public interface JobCompletionStatusAccessor {
    Optional<JobCompletionStatusModel> getJobExecutionStatus(UUID jobConfigId);

    AlertPagedModel<JobCompletionStatusModel> getJobExecutionStatus(AlertPagedQueryDetails pagedQueryDetails);

    void saveExecutionStatus(JobCompletionStatusModel statusModel);

    List<JobCompletionStageModel> getJobStageData(UUID jobConfigId);

    void saveJobStageData(JobCompletionStageModel jobCompletionStageModel);
}
