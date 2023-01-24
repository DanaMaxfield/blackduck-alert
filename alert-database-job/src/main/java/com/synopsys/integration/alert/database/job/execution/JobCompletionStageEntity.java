package com.synopsys.integration.alert.database.job.execution;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import com.synopsys.integration.alert.database.BaseEntity;

@Entity
@IdClass(JobCompletionStagePK.class)
@Table(schema = "alert", name = "job_execution_stage")
public class JobCompletionStageEntity extends BaseEntity {
    private static final long serialVersionUID = -5033287979526840338L;
    @Id
    @Column(name = "job_config_id")
    private UUID jobConfigId;
    @Id
    @Column(name = "stage_id")
    private int stage;
    @Column(name = "duration_nanosecond")
    private Long durationNano;

    public JobCompletionStageEntity() {
        // default constructor for JPA
    }

    public JobCompletionStageEntity(UUID jobConfigId, int stage, Long durationNano) {
        this.jobConfigId = jobConfigId;
        this.stage = stage;
        this.durationNano = durationNano;
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public int getStage() {
        return stage;
    }

    public Long getDurationNano() {
        return durationNano;
    }
}
