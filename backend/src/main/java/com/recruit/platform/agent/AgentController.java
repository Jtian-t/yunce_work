package com.recruit.platform.agent;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/api/candidates/{candidateId}/analysis-jobs")
    AgentJobResponse createJob(@PathVariable Long candidateId, @RequestBody CreateAnalysisJobRequest request) {
        return agentService.createAnalysisJob(candidateId, request);
    }

    @GetMapping("/api/candidates/{candidateId}/analysis-jobs/latest")
    AgentJobResponse latest(@PathVariable Long candidateId) {
        return agentService.latestAnalysis(candidateId);
    }

    @PostMapping("/api/candidates/{candidateId}/parse-jobs")
    AgentJobResponse createParseJob(@PathVariable Long candidateId, @RequestBody(required = false) CreateParseJobRequest request) {
        return agentService.createParseJob(candidateId, request == null ? new CreateParseJobRequest(null) : request);
    }

    @GetMapping("/api/candidates/{candidateId}/parse-jobs/latest")
    AgentJobResponse latestParse(@PathVariable Long candidateId) {
        return agentService.latestParse(candidateId);
    }

    @PostMapping("/api/candidates/{candidateId}/parsed-profile/apply")
    void applyParsedProfile(@PathVariable Long candidateId, @RequestBody(required = false) ApplyParsedProfileRequest request) {
        agentService.applyParsedProfile(candidateId, request == null ? new ApplyParsedProfileRequest(null) : request);
    }

    @PutMapping("/api/candidates/{candidateId}/parsed-profile/manual-fields")
    void saveManualParsedFields(@PathVariable Long candidateId, @Valid @RequestBody ParsedProfileManualFieldsRequest request) {
        agentService.saveManualParsedFields(candidateId, request);
    }

    @PostMapping("/api/candidates/{candidateId}/decision-jobs")
    AgentJobResponse createDecisionJob(@PathVariable Long candidateId, @RequestBody(required = false) CreateDecisionJobRequest request) {
        return agentService.createDecisionJob(candidateId, request == null ? new CreateDecisionJobRequest(null) : request);
    }

    @GetMapping("/api/candidates/{candidateId}/decision-jobs/latest")
    AgentJobResponse latestDecision(@PathVariable Long candidateId) {
        return agentService.latestDecision(candidateId);
    }

    @GetMapping("/api/candidates/{candidateId}/decision-jobs")
    java.util.List<AgentJobResponse> decisionHistory(@PathVariable Long candidateId) {
        return agentService.decisionHistory(candidateId);
    }

    @PostMapping("/api/internal/agent/jobs/{jobId}/result")
    AgentJobResponse callback(
            @PathVariable Long jobId,
            @RequestHeader("X-Agent-Token") String callbackToken,
            @Valid @RequestBody AgentCallbackRequest request
    ) {
        return agentService.handleAnalysisCallback(jobId, callbackToken, request);
    }

    @PostMapping("/api/internal/agent/parse-jobs/{jobId}/result")
    AgentJobResponse parseCallback(
            @PathVariable Long jobId,
            @RequestHeader("X-Agent-Token") String callbackToken,
            @Valid @RequestBody AgentCallbackRequest request
    ) {
        return agentService.handleParseCallback(jobId, callbackToken, request);
    }

    @PostMapping("/api/internal/agent/decision-jobs/{jobId}/result")
    AgentJobResponse decisionCallback(
            @PathVariable Long jobId,
            @RequestHeader("X-Agent-Token") String callbackToken,
            @Valid @RequestBody AgentCallbackRequest request
    ) {
        return agentService.handleDecisionCallback(jobId, callbackToken, request);
    }
}
