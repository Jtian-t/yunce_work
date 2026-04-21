package com.recruit.platform.agent;

import com.recruit.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AgentResult extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private AgentJob job;

    @Column(length = 4000)
    private String summary;

    @Column
    private Integer overallScore;

    @Column(length = 4000)
    private String dimensionScoresJson;

    @Column(length = 4000)
    private String strengths;

    @Column(length = 4000)
    private String risks;

    @Column(length = 1000)
    private String recommendedAction;

    @Column(length = 10000)
    private String rawReasoningDigest;

    @Column(length = 255)
    private String parsedName;

    @Column(length = 255)
    private String parsedPhone;

    @Column(length = 255)
    private String parsedEmail;

    @Column(length = 1000)
    private String parsedEducation;

    @Column(length = 1000)
    private String parsedExperience;

    @Column(length = 5000)
    private String parsedSkillsSummary;

    @Column(length = 5000)
    private String parsedProjectSummary;
}
