package com.recruit.platform.common.enums;

import lombok.Getter;

@Getter
public enum CandidateStatus {
    NEW("NEW", "新建"),
    PENDING_DEPT_REVIEW("PENDING_DEPT_REVIEW", "待部门筛选"),
    IN_DEPT_REVIEW("IN_DEPT_REVIEW", "部门处理中"),
    PENDING_INTERVIEW("PENDING_INTERVIEW", "待安排面试"),
    INTERVIEWING("INTERVIEWING", "面试中"),
    INTERVIEW_PASSED("INTERVIEW_PASSED", "面试通过"),
    OFFER_PENDING("OFFER_PENDING", "待发 Offer"),
    OFFER_SENT("OFFER_SENT", "已发 Offer"),
    HIRED("HIRED", "已录用"),
    REJECTED("REJECTED", "已淘汰"),
    TIMEOUT("TIMEOUT", "超时");

    private final String code;
    private final String label;

    CandidateStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
