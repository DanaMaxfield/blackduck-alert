package com.synopsys.integration.alert.database.job.execution;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.synopsys.integration.alert.database.BaseEntity;

@Entity
@Table(schema = "alert", name = "job_execution")
public class JobExecutionEntity extends BaseEntity {
    private static final long serialVersionUID = 1678337364473767550L;
    @Id
    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "job_config_id")
    private UUID jobConfigId;
    @Column(name = "start_time")
    private OffsetDateTime start;
    @Column(name = "end_time")
    private OffsetDateTime end;

    @Column(name = "status")
    private String status;
    @Column(name = "processed_notification_count")
    private int processedNotificationCount;

    @Column(name = "total_notification_count")
    private int totalNotificationCount;

    public JobExecutionEntity() {
        // default constructor
    }

    public JobExecutionEntity(
        UUID executionId,
        UUID jobConfigId,
        OffsetDateTime start,
        OffsetDateTime end,
        String status,
        int processedNotificationCount,
        int totalNotificationCount
    ) {
        this.executionId = executionId;
        this.jobConfigId = jobConfigId;
        this.start = start;
        this.end = end;
        this.status = status;
        this.processedNotificationCount = processedNotificationCount;
        this.totalNotificationCount = totalNotificationCount;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public String getStatus() {
        return status;
    }

    public int getProcessedNotificationCount() {
        return processedNotificationCount;
    }

    public int getTotalNotificationCount() {
        return totalNotificationCount;
    }
}
