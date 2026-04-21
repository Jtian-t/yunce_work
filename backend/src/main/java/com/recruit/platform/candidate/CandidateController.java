package com.recruit.platform.candidate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;
    private final CandidateAdvanceService candidateAdvanceService;

    @GetMapping
    java.util.List<CandidateResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department
    ) {
        return candidateService.list(q, status, department);
    }

    @PostMapping
    CandidateDetailResponse create(@Valid @RequestBody CandidateUpsertRequest request) {
        return candidateService.create(request);
    }

    @GetMapping("/{id}")
    CandidateDetailResponse get(@PathVariable Long id) {
        return candidateService.get(id);
    }

    @PutMapping("/{id}")
    CandidateDetailResponse update(@PathVariable Long id, @Valid @RequestBody CandidateUpsertRequest request) {
        return candidateService.update(id, request);
    }

    @GetMapping("/{id}/timeline")
    java.util.List<TimelineEventResponse> timeline(@PathVariable Long id) {
        return candidateService.timeline(id);
    }

    @PostMapping("/{id}/resume")
    ResumeAssetResponse uploadResume(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return candidateService.uploadResume(id, file);
    }

    @GetMapping("/{id}/resume/download")
    ResponseEntity<org.springframework.core.io.Resource> downloadResume(@PathVariable Long id) {
        return candidateService.downloadResume(id);
    }

    @GetMapping("/{id}/resume/preview")
    ResponseEntity<org.springframework.core.io.Resource> previewResume(@PathVariable Long id) {
        return candidateService.previewResume(id);
    }

    @PostMapping("/{id}/advance")
    CandidateDetailResponse advance(@PathVariable Long id, @Valid @RequestBody AdvanceCandidateRequest request) {
        return candidateAdvanceService.advance(id, request);
    }
}
