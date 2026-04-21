package com.recruit.platform.interview;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.common.BaseEntity;
import com.recruit.platform.common.enums.InterviewResult;
import com.recruit.platform.user.User;
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
public class InterviewEvaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_plan_id", nullable = false)
    private InterviewPlan interviewPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewResult result;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 3000, nullable = false)
    private String evaluation;

    @Column(length = 2000)
    private String strengths;

    @Column(length = 2000)
    private String weaknesses;

    @Column(length = 1000)
    private String suggestion;
}
