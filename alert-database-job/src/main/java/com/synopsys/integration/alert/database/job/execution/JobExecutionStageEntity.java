package com.synopsys.integration.alert.database.job.execution;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.synopsys.integration.alert.database.BaseEntity;

@Entity
@Table(schema = "alert", name = "job_execution_stage")
public class JobExecutionStageEntity extends BaseEntity {
    private static final long serialVersionUID = -5052694730138984687L;
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "execution_id")
    private UUID executionId;
    @Column(name = "stage_id")
    private String stage;
    @Column(name = "start_time")
    private OffsetDateTime start;
    @Column(name = "end_time")
    private OffsetDateTime end;

    public JobExecutionStageEntity() {
        // default constructor for JPA
    }

    public JobExecutionStageEntity(UUID executionId, String stage, OffsetDateTime start, OffsetDateTime end) {
        this.id = UUID.randomUUID();
        this.executionId = executionId;
        this.stage = stage;
        this.start = start;
        this.end = end;
    }

    public JobExecutionStageEntity(UUID id, UUID executionId, String stage, OffsetDateTime start, OffsetDateTime end) {
        this.id = id;
        this.executionId = executionId;
        this.stage = stage;
        this.start = start;
        this.end = end;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getStage() {
        return stage;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }
}
