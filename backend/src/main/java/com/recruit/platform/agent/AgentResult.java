package com.recruit.platform.agent;

import com.recruit.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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

    @Lob
    private String summary;

    @Column
    private Integer overallScore;

    @Lob
    private String dimensionScoresJson;

    @Lob
    private String strengths;

    @Lob
    private String risks;

    @Lob
    private String recommendedAction;

    @Lob
    private String rawReasoningDigest;

    @Lob
    private String parseReportJson;

    @Lob
    private String decisionReportJson;

    @Lob
    private String skillsJson;

    @Lob
    private String projectsJson;

    @Lob
    private String experiencesJson;

    @Lob
    private String educationsJson;

    @Lob
    private String rawBlocksJson;

    @Column(length = 255)
    private String parsedName;

    @Column(length = 255)
    private String parsedPhone;

    @Column(length = 255)
    private String parsedEmail;

    @Column(length = 255)
    private String parsedLocation;

    @Column(length = 1000)
    private String parsedEducation;

    @Column(length = 1000)
    private String parsedExperience;

    @Column(length = 5000)
    private String parsedSkillsSummary;

    @Column(length = 5000)
    private String parsedProjectSummary;
}
