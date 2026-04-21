package com.recruit.platform.candidate;

import com.recruit.platform.common.BaseEntity;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.department.Department;
import com.recruit.platform.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Candidate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private LocalDate submittedDate;

    @Column(nullable = false)
    private String nextAction;

    private String phone;

    private String email;

    private String location;

    private String experience;

    private String education;

    @Column(length = 5000)
    private String skillsSummary;

    @Column(length = 5000)
    private String projectSummary;
}
