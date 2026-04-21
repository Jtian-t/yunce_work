package com.recruit.platform.agent;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
