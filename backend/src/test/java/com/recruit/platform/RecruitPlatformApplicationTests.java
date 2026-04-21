package com.recruit.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.agent.AgentJobRepository;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.user.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RecruitPlatformApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentJobRepository agentJobRepository;

    @Test
    void candidateWorkflowShouldCompleteHappyPath() throws Exception {
        String hrToken = login("hr", "Password123!");
        String deptLeadToken = login("techlead", "Password123!");
        String interviewerToken = login("interviewer", "Password123!");
        Long techDepartmentId = departmentRepository.findByCode("TECH").orElseThrow().getId();
        Long techLeadId = userRepository.findByUsername("techlead").orElseThrow().getId();
        Long interviewerId = userRepository.findByUsername("interviewer").orElseThrow().getId();

        Long candidateId = createCandidate(hrToken, techDepartmentId, techLeadId, "吴昊");
        uploadResume(hrToken, candidateId);

        String assignmentPayload = """
                {
                  "departmentId": %d,
                  "reviewerId": %d
                }
                """.formatted(techDepartmentId, techLeadId);
        MvcResult assignmentResult = mockMvc.perform(post("/api/candidates/{id}/assignments", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andReturn();
        Long assignmentId = readJson(assignmentResult).path("id").asLong();

        mockMvc.perform(get("/api/department/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(deptLeadToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidateId").exists());

        String feedbackPayload = """
                {
                  "assignmentId": %d,
                  "decision": "PASS",
                  "feedback": "技术栈匹配，建议进入面试环节",
                  "nextStep": "安排一面",
                  "suggestedInterviewer": "刘洋"
                }
                """.formatted(assignmentId);
        mockMvc.perform(post("/api/feedbacks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(deptLeadToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("PASS"));

        mockMvc.perform(get("/api/candidates/{id}", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("PENDING_INTERVIEW"));

        String interviewPayload = """
                {
                  "candidateId": %d,
                  "interviewerId": %d,
                  "roundLabel": "一面",
                  "scheduledAt": "%s",
                  "endsAt": "%s"
                }
                """.formatted(
                candidateId,
                interviewerId,
                OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS),
                OffsetDateTime.now().plusDays(1).plusHours(1).truncatedTo(ChronoUnit.SECONDS)
        );
        MvcResult interviewResult = mockMvc.perform(post("/api/interviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interviewPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andReturn();
        Long interviewId = readJson(interviewResult).path("id").asLong();

        String evaluationPayload = """
                {
                  "result": "PASS",
                  "score": 88,
                  "evaluation": "沟通顺畅，基础扎实",
                  "strengths": "Spring Boot, React",
                  "weaknesses": "缺少大型系统经验",
                  "suggestion": "推进下一轮"
                }
                """;
        mockMvc.perform(post("/api/interviews/{id}/evaluations", interviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(interviewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("PASS"));

        mockMvc.perform(get("/api/candidates/{id}", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("INTERVIEW_PASSED"));

        mockMvc.perform(get("/api/candidates/{id}/timeline", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void agentJobShouldPersistResultAndExposeLatest() throws Exception {
        String hrToken = login("hr", "Password123!");
        Long techDepartmentId = departmentRepository.findByCode("TECH").orElseThrow().getId();
        Long techLeadId = userRepository.findByUsername("techlead").orElseThrow().getId();
        Long candidateId = createCandidate(hrToken, techDepartmentId, techLeadId, "孙丽");
        uploadResume(hrToken, candidateId);

        mockMvc.perform(post("/api/candidates/{id}/analysis-jobs", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jdSummary\":\"需要 3 年以上 Java 后端经验\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        var job = agentJobRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId).orElseThrow();
        String callbackPayload = """
                {
                  "succeeded": true,
                  "summary": "候选人技术基础良好，适合继续推进。",
                  "overallScore": 84,
                  "dimensionScores": {
                    "technical": 86,
                    "communication": 80
                  },
                  "strengths": "Java、Spring Boot、协作能力",
                  "risks": "大型项目经验较少",
                  "recommendedAction": "安排技术面试",
                  "rawReasoningDigest": "结构化结论已生成"
                }
                """;
        mockMvc.perform(post("/api/internal/agent/jobs/{jobId}/result", job.getId())
                        .header("X-Agent-Token", job.getCallbackToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.overallScore").value(84));

        mockMvc.perform(get("/api/candidates/{id}/analysis-jobs/latest", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.summary").value("候选人技术基础良好，适合继续推进。"));
    }

    @Test
    void dashboardAndReportEndpointsShouldReturnData() throws Exception {
        String hrToken = login("hr", "Password123!");

        mockMvc.perform(get("/api/dashboard/overview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingFeedbackCount").exists());

        mockMvc.perform(get("/api/reports/daily")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.statusBreakdown").isArray());
    }

    @Test
    void parseJobLookupAndAdvanceEndpointsShouldWork() throws Exception {
        String hrToken = login("hr", "Password123!");
        Long techDepartmentId = departmentRepository.findByCode("TECH").orElseThrow().getId();
        Long techLeadId = userRepository.findByUsername("techlead").orElseThrow().getId();
        Long interviewerId = userRepository.findByUsername("interviewer").orElseThrow().getId();
        Long candidateId = createCandidate(hrToken, techDepartmentId, techLeadId, "李四");
        uploadResume(hrToken, candidateId);

        mockMvc.perform(get("/api/lookups/departments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        mockMvc.perform(get("/api/lookups/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .param("role", "INTERVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").exists());

        mockMvc.perform(post("/api/candidates/{id}/parse-jobs", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hint\":\"请提取联系方式和技能\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("PARSE"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/candidates/{id}/parse-jobs/latest", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("PARSE"))
                .andExpect(jsonPath("$.result.parsedCandidateDraft.projectSummary").exists());

        String schedulePayload = """
                {
                  "action": "SCHEDULE_INTERVIEW",
                  "interviewerId": %d,
                  "roundLabel": "一面",
                  "scheduledAt": "%s",
                  "endsAt": "%s"
                }
                """.formatted(
                interviewerId,
                OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS),
                OffsetDateTime.now().plusDays(2).plusHours(1).truncatedTo(ChronoUnit.SECONDS)
        );
        mockMvc.perform(post("/api/candidates/{id}/advance", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedulePayload))
                .andExpect(status().isBadRequest());

        String assignPayload = """
                {
                  "action": "ASSIGN_TO_DEPARTMENT",
                  "departmentId": %d,
                  "reviewerId": %d
                }
                """.formatted(techDepartmentId, techLeadId);
        mockMvc.perform(post("/api/candidates/{id}/advance", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("IN_DEPT_REVIEW"));

        String moveToPoolPayload = """
                {
                  "action": "MOVE_TO_POOL"
                }
                """;
        mockMvc.perform(post("/api/candidates/{id}/advance", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveToPoolPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("NEW"))
                .andExpect(jsonPath("$.department").isEmpty());

        mockMvc.perform(get("/api/candidates/{id}/resume/preview", candidateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("inline")));
    }

    private Long createCandidate(String token, Long departmentId, Long ownerId, String name) throws Exception {
        String payload = """
                {
                  "name": "%s",
                  "position": "Java 工程师",
                  "departmentId": %d,
                  "ownerId": %d,
                  "source": "BOSS 直聘",
                  "submittedDate": "%s",
                  "nextAction": "等待分发部门",
                  "phone": "13812345678",
                  "email": "candidate_%s@example.com",
                  "location": "上海",
                  "experience": "4 年",
                  "education": "本科",
                  "skillsSummary": "Java, Spring Boot, MySQL",
                  "projectSummary": "有中后台系统建设经验"
                }
                """.formatted(name, departmentId, ownerId, LocalDate.now(), name);
        MvcResult result = mockMvc.perform(post("/api/candidates")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("id").asLong();
    }

    private void uploadResume(String token, Long candidateId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "mock resume".getBytes()
        );
        mockMvc.perform(multipart("/api/candidates/{id}/resume", candidateId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFileName").value("resume.pdf"));
    }

    private String login(String username, String password) throws Exception {
        String payload = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = readJson(result).path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
