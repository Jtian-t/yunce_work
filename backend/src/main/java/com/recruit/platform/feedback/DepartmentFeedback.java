package com.recruit.platform.feedback;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.common.BaseEntity;
import com.recruit.platform.common.enums.FeedbackDecision;
import com.recruit.platform.user.User;
import com.recruit.platform.workflow.DepartmentAssignment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class DepartmentFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private DepartmentAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackDecision decision;

    @Column(length = 3000, nullable = false)
    private String feedback;

    private String rejectReason;

    private String nextStep;

    private String suggestedInterviewer;
}
