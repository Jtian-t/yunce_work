package com.recruit.platform.agent;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.common.BaseEntity;
import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AgentJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentJobType jobType;

    @Column(nullable = false, length = 10000)
    private String requestPayloadJson;

    @Column(nullable = false, unique = true)
    private String callbackToken;

    private OffsetDateTime requestedAt;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @Column(length = 2000)
    private String lastError;
}
