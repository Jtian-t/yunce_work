package com.recruit.platform.config;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.CandidateRepository;
import com.recruit.platform.common.enums.AssignmentStatus;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.EmploymentStatus;
import com.recruit.platform.common.enums.InterviewMeetingType;
import com.recruit.platform.common.enums.InterviewPlanStatus;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.Department;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.interview.InterviewPlan;
import com.recruit.platform.interview.InterviewPlanRepository;
import com.recruit.platform.notification.NotificationService;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import com.recruit.platform.workflow.DepartmentAssignment;
import com.recruit.platform.workflow.DepartmentAssignmentRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BootstrapDataLoader {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final DepartmentAssignmentRepository assignmentRepository;
    private final InterviewPlanRepository interviewPlanRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner loadSeedData() {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            Department tech = department("TECH", "技术部");
            Department product = department("PRODUCT", "产品部");
            Department design = department("DESIGN", "设计部");

            User admin = user("admin", "平台管理员", "admin@company.com", null, Set.of(RoleType.ADMIN), false, EmploymentStatus.ACTIVE, 0);
            User hr = user("hr", "李明", "hr@company.com", tech, Set.of(RoleType.HR), false, EmploymentStatus.ACTIVE, 1);

            User techLead = user("techlead", "王聪", "techlead@company.com", tech, Set.of(RoleType.DEPARTMENT_LEAD), false, EmploymentStatus.ACTIVE, 10);
            User productLead = user("productlead", "张华", "productlead@company.com", product, Set.of(RoleType.DEPARTMENT_LEAD), false, EmploymentStatus.ACTIVE, 10);
            User designLead = user("designlead", "许岚", "designlead@company.com", design, Set.of(RoleType.DEPARTMENT_LEAD), false, EmploymentStatus.ACTIVE, 10);

            User techInterviewer = user("interviewer", "刘洋", "interviewer@company.com", tech, Set.of(RoleType.INTERVIEWER), true, EmploymentStatus.ACTIVE, 20);
            User techInterviewer2 = user("interviewer2", "周敏", "interviewer2@company.com", tech, Set.of(RoleType.INTERVIEWER), true, EmploymentStatus.ACTIVE, 21);
            User productInterviewer = user("productinterviewer", "宋佳", "productinterviewer@company.com", product, Set.of(RoleType.INTERVIEWER), true, EmploymentStatus.ACTIVE, 20);
            User productInterviewer2 = user("productinterviewer2", "赵悦", "productinterviewer2@company.com", product, Set.of(RoleType.INTERVIEWER), true, EmploymentStatus.ACTIVE, 21);
            User designInterviewer = user("designinterviewer", "许可", "designinterviewer@company.com", design, Set.of(RoleType.INTERVIEWER), true, EmploymentStatus.ACTIVE, 20);
            User designInterviewer2 = user("designinterviewer2", "林朵", "designinterviewer2@company.com", design, Set.of(RoleType.INTERVIEWER), true, EmploymentStatus.ACTIVE, 21);
            user("productstaff", "产品专员", "productstaff@company.com", product, Set.of(RoleType.INTERVIEWER), false, EmploymentStatus.ACTIVE, 30);

            Candidate pendingCandidate = candidate("张伟", "前端工程师", techLead, tech, CandidateStatus.IN_DEPT_REVIEW, "等待部门反馈");
            Candidate productCandidate = candidate("王芳", "产品经理", productLead, product, CandidateStatus.INTERVIEWING, "等待一面结果");
            Candidate techCandidate = candidate("金天祥", "Java开发", techLead, tech, CandidateStatus.INTERVIEWING, "等待一面结果");
            Candidate designCandidate = candidate("陈静", "UI 设计师", hr, design, CandidateStatus.HIRED, "已完成录用");

            assignment(pendingCandidate, tech, techLead, hr, OffsetDateTime.now().plusDays(1));
            assignment(techCandidate, tech, techLead, hr, OffsetDateTime.now().plusHours(10));
            assignment(productCandidate, product, productLead, hr, OffsetDateTime.now().plusHours(12));

            interview(productCandidate, productInterviewer, hr, product, "一面", "ROUND_1", "一面",
                    OffsetDateTime.now().plusHours(4), OffsetDateTime.now().plusHours(5), "https://meeting.tencent.com/product-1");
            interview(productCandidate, productInterviewer2, hr, product, "二面", "ROUND_2", "二面",
                    OffsetDateTime.now().plusDays(1).plusHours(2), OffsetDateTime.now().plusDays(1).plusHours(3), "https://meeting.tencent.com/product-2");
            interview(techCandidate, techInterviewer, hr, tech, "一面", "ROUND_1", "一面",
                    OffsetDateTime.now().plusHours(3), OffsetDateTime.now().plusHours(4), "https://meeting.tencent.com/tech-1");

            notificationService.create(productInterviewer, NotificationType.INTERVIEW_ASSIGNED,
                    "收到新的面试安排",
                    productCandidate.getName() + " 的一面已安排给您。",
                    java.util.Map.of(
                            "candidateId", productCandidate.getId(),
                            "candidateName", productCandidate.getName(),
                            "departmentId", product.getId(),
                            "departmentName", product.getName(),
                            "interviewerId", productInterviewer.getId(),
                            "interviewerName", productInterviewer.getDisplayName(),
                            "interviewStageCode", "ROUND_1",
                            "interviewStageLabel", "一面",
                            "meetingUrl", "https://meeting.tencent.com/product-1"
                    ));

            // Keep admin referenced so seed data remains obvious in local demos.
            if (admin.getId() == null || designLead.getId() == null || designInterviewer.getId() == null || designInterviewer2.getId() == null || designCandidate.getId() == null) {
                throw new IllegalStateException("Seed data bootstrap failed");
            }
        };
    }

    private Department department(String code, String name) {
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        return departmentRepository.save(department);
    }

    private User user(String username, String displayName, String email, Department department, Set<RoleType> roles,
                      boolean canInterview, EmploymentStatus employmentStatus, int displayOrder) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setDepartment(department);
        user.setRoles(roles);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setCanInterview(canInterview);
        user.setEmploymentStatus(employmentStatus);
        user.setDisplayOrder(displayOrder);
        return userRepository.save(user);
    }

    private Candidate candidate(String name, String position, User owner, Department department, CandidateStatus status, String nextAction) {
        Candidate candidate = new Candidate();
        candidate.setName(name);
        candidate.setPosition(position);
        candidate.setOwner(owner);
        candidate.setDepartment(department);
        candidate.setStatus(status);
        candidate.setSource("系统导入");
        candidate.setSubmittedDate(LocalDate.now());
        candidate.setNextAction(nextAction);
        candidate.setPhone("13800000000");
        candidate.setEmail(name.toLowerCase().replace(" ", "") + "@mail.com");
        candidate.setExperience("3 年");
        candidate.setEducation("本科");
        candidate.setLocation("上海");
        candidate.setSkillsSummary("Java, Spring Boot, React");
        candidate.setProjectSummary("具备招聘协同、管理后台等项目经验");
        return candidateRepository.save(candidate);
    }

    private void assignment(Candidate candidate, Department department, User reviewer, User assignedBy, OffsetDateTime dueAt) {
        DepartmentAssignment assignment = new DepartmentAssignment();
        assignment.setCandidate(candidate);
        assignment.setDepartment(department);
        assignment.setReviewer(reviewer);
        assignment.setAssignedBy(assignedBy);
        assignment.setStatus(AssignmentStatus.OPEN);
        assignment.setDueAt(dueAt);
        assignmentRepository.save(assignment);
    }

    private void interview(Candidate candidate, User interviewer, User organizer, Department department, String roundLabel,
                           String stageCode, String stageLabel, OffsetDateTime scheduledAt, OffsetDateTime endsAt, String meetingUrl) {
        InterviewPlan interviewPlan = new InterviewPlan();
        interviewPlan.setCandidate(candidate);
        interviewPlan.setInterviewer(interviewer);
        interviewPlan.setCreatedBy(organizer);
        interviewPlan.setOrganizer(organizer);
        interviewPlan.setRoundLabel(roundLabel);
        interviewPlan.setInterviewStageCode(stageCode);
        interviewPlan.setInterviewStageLabel(stageLabel);
        interviewPlan.setScheduledAt(scheduledAt);
        interviewPlan.setEndsAt(endsAt);
        interviewPlan.setMeetingType(InterviewMeetingType.TENCENT_MEETING);
        interviewPlan.setMeetingUrl(meetingUrl);
        interviewPlan.setDepartment(department);
        interviewPlan.setStatus(InterviewPlanStatus.PLANNED);
        interviewPlanRepository.save(interviewPlan);
    }
}
