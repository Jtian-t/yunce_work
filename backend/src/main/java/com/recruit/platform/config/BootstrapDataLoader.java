package com.recruit.platform.config;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.CandidateRepository;
import com.recruit.platform.common.enums.AssignmentStatus;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.Department;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.interview.InterviewPlan;
import com.recruit.platform.interview.InterviewPlanRepository;
import com.recruit.platform.common.enums.InterviewPlanStatus;
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

            User admin = user("admin", "平台管理员", "admin@company.com", null, Set.of(RoleType.ADMIN));
            User hr = user("hr", "李明", "hr@company.com", tech, Set.of(RoleType.HR));
            User techLead = user("techlead", "王总", "techlead@company.com", tech, Set.of(RoleType.DEPARTMENT_LEAD));
            User productLead = user("productlead", "张华", "productlead@company.com", product, Set.of(RoleType.DEPARTMENT_LEAD));
            User techInterviewer = user("interviewer", "刘洋", "interviewer@company.com", tech, Set.of(RoleType.INTERVIEWER));
            User techInterviewer2 = user("interviewer2", "周敏", "interviewer2@company.com", tech, Set.of(RoleType.INTERVIEWER));
            User productInterviewer = user("productinterviewer", "宋佳", "productinterviewer@company.com", product, Set.of(RoleType.INTERVIEWER));
            User designInterviewer = user("designinterviewer", "许然", "designinterviewer@company.com", design, Set.of(RoleType.INTERVIEWER));

            Candidate pendingCandidate = candidate("张伟", "前端工程师", techLead, tech, CandidateStatus.IN_DEPT_REVIEW, "等待部门反馈");
            Candidate interviewCandidate = candidate("王芳", "产品经理", productLead, product, CandidateStatus.INTERVIEWING, "等待面试结果");
            Candidate timeoutCandidate = candidate("赵强", "Java 工程师", techLead, tech, CandidateStatus.TIMEOUT, "处理超时反馈");
            Candidate hiredCandidate = candidate("陈静", "UI 设计师", hr, design, CandidateStatus.HIRED, "已完成录用");

            DepartmentAssignment openAssignment = new DepartmentAssignment();
            openAssignment.setCandidate(pendingCandidate);
            openAssignment.setDepartment(tech);
            openAssignment.setReviewer(techLead);
            openAssignment.setAssignedBy(hr);
            openAssignment.setStatus(AssignmentStatus.OPEN);
            openAssignment.setDueAt(OffsetDateTime.now().plusDays(1));
            assignmentRepository.save(openAssignment);

            DepartmentAssignment overdueAssignment = new DepartmentAssignment();
            overdueAssignment.setCandidate(timeoutCandidate);
            overdueAssignment.setDepartment(tech);
            overdueAssignment.setReviewer(techLead);
            overdueAssignment.setAssignedBy(hr);
            overdueAssignment.setStatus(AssignmentStatus.OPEN);
            overdueAssignment.setDueAt(OffsetDateTime.now().minusHours(5));
            assignmentRepository.save(overdueAssignment);

            InterviewPlan interviewPlan = new InterviewPlan();
            interviewPlan.setCandidate(interviewCandidate);
            interviewPlan.setInterviewer(techInterviewer);
            interviewPlan.setCreatedBy(hr);
            interviewPlan.setOrganizer(hr);
            interviewPlan.setInterviewStageCode("ROUND_1");
            interviewPlan.setInterviewStageLabel("一面");
            interviewPlan.setRoundLabel("一面");
            interviewPlan.setScheduledAt(OffsetDateTime.now().plusHours(4));
            interviewPlan.setEndsAt(OffsetDateTime.now().plusHours(5));
            interviewPlan.setMeetingType(com.recruit.platform.common.enums.InterviewMeetingType.TENCENT_MEETING);
            interviewPlan.setMeetingUrl("https://meeting.tencent.com/mock");
            interviewPlan.setDepartment(tech);
            interviewPlan.setStatus(InterviewPlanStatus.PLANNED);
            interviewPlanRepository.save(interviewPlan);

            InterviewPlan interviewPlan2 = new InterviewPlan();
            interviewPlan2.setCandidate(interviewCandidate);
            interviewPlan2.setInterviewer(techInterviewer2);
            interviewPlan2.setCreatedBy(hr);
            interviewPlan2.setOrganizer(hr);
            interviewPlan2.setInterviewStageCode("ROUND_2");
            interviewPlan2.setInterviewStageLabel("二面");
            interviewPlan2.setRoundLabel("二面");
            interviewPlan2.setScheduledAt(OffsetDateTime.now().plusDays(1).plusHours(2));
            interviewPlan2.setEndsAt(OffsetDateTime.now().plusDays(1).plusHours(3));
            interviewPlan2.setMeetingType(com.recruit.platform.common.enums.InterviewMeetingType.TENCENT_MEETING);
            interviewPlan2.setMeetingUrl("https://meeting.tencent.com/mock-2");
            interviewPlan2.setDepartment(tech);
            interviewPlan2.setStatus(InterviewPlanStatus.PLANNED);
            interviewPlanRepository.save(interviewPlan2);
        };
    }

    private Department department(String code, String name) {
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        return departmentRepository.save(department);
    }

    private User user(String username, String displayName, String email, Department department, Set<RoleType> roles) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setDepartment(department);
        user.setRoles(roles);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
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
}
