package com.recruit.platform.feedback;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/api/feedbacks")
    FeedbackResponse submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        return feedbackService.submit(request);
    }

    @GetMapping("/api/candidates/{candidateId}/feedbacks")
    List<FeedbackResponse> listByCandidate(@PathVariable Long candidateId) {
        return feedbackService.listByCandidate(candidateId);
    }
}
