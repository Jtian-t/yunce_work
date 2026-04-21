package com.recruit.platform.interview;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/api/interviews")
    InterviewPlanResponse create(@Valid @RequestBody CreateInterviewPlanRequest request) {
        return interviewService.createPlan(request);
    }

    @PostMapping("/api/interviews/{id}/evaluations")
    InterviewEvaluationResponse submitEvaluation(
            @PathVariable Long id,
            @Valid @RequestBody SubmitInterviewEvaluationRequest request
    ) {
        return interviewService.submitEvaluation(id, request);
    }

    @GetMapping("/api/candidates/{candidateId}/interviews")
    List<InterviewPlanResponse> listByCandidate(@PathVariable Long candidateId) {
        return interviewService.listByCandidate(candidateId);
    }
}
