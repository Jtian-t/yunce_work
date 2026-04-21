package com.recruit.platform.workflow;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class WorkflowEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String actorName;

    private Long actorId;

    @Column(nullable = false)
    private String sourceAction;

    @Column(nullable = false)
    private String statusCode;

    @Column(nullable = false)
    private String statusLabel;

    @Column(length = 2000)
    private String note;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;
}
