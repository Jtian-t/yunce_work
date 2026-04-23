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
    @Column(columnDefinition = "longtext")
    private String summary;

    @Column
    private Integer overallScore;

    @Lob
    @Column(columnDefinition = "longtext")
    private String dimensionScoresJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String strengths;

    @Lob
    @Column(columnDefinition = "longtext")
    private String risks;

    @Lob
    @Column(columnDefinition = "longtext")
    private String recommendedAction;

    @Lob
    @Column(columnDefinition = "longtext")
    private String rawReasoningDigest;

    @Lob
    @Column(columnDefinition = "longtext")
    private String parseReportJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String decisionReportJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String skillsJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String projectsJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String experiencesJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String educationsJson;

    @Lob
    @Column(columnDefinition = "longtext")
    private String rawBlocksJson;

    @Column(length = 255)
    private String parsedName;

    @Column(length = 255)
    private String parsedPhone;

    @Column(length = 255)
    private String parsedEmail;

    @Column(length = 255)
    private String parsedLocation;

    @Lob
    @Column(columnDefinition = "text")
    private String parsedEducation;

    @Lob
    @Column(columnDefinition = "text")
    private String parsedExperience;

    @Lob
    @Column(columnDefinition = "longtext")
    private String parsedSkillsSummary;

    @Lob
    @Column(columnDefinition = "longtext")
    private String parsedProjectSummary;
}
