package com.recruit.platform.candidate;

import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.Department;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.interview.InterviewPlanRepository;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.storage.ResumeStorageService;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import com.recruit.platform.workflow.DepartmentAssignmentRepository;
import com.recruit.platform.workflow.WorkflowEvent;
import com.recruit.platform.workflow.WorkflowEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ResumeAssetRepository resumeAssetRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ResumeStorageService resumeStorageService;
    private final WorkflowEventRepository workflowEventRepository;
    private final DepartmentAssignmentRepository departmentAssignmentRepository;
    private final InterviewPlanRepository interviewPlanRepository;

    public List<CandidateResponse> list(String query, String status, String department) {
        return candidateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(candidate -> query == null || query.isBlank() || matchesQuery(candidate, query))
                .filter(candidate -> status == null || status.isBlank() || candidate.getStatus().name().equalsIgnoreCase(status))
                .filter(candidate -> department == null || department.isBlank() ||
                        (candidate.getDepartment() != null && candidate.getDepartment().getName().equalsIgnoreCase(department)))
                .map(this::toResponse)
                .toList();
    }

    public CandidateDetailResponse get(Long id) {
        Candidate candidate = getEntity(id);
        return toDetailResponse(candidate);
    }

    @Transactional
    public CandidateDetailResponse create(CandidateUpsertRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = new Candidate();
        apply(candidate, request);
        candidate.setStatus(CandidateStatus.NEW);
        candidate.setNextAction(request.nextAction() == null || request.nextAction().isBlank() ? "等待分发部门" : request.nextAction());
        Candidate saved = candidateRepository.save(candidate);
        createWorkflowEvent(saved, "CANDIDATE_CREATED", "创建候选人", "创建候选人台账");
        return toDetailResponse(saved);
    }

    @Transactional
    public CandidateDetailResponse update(Long id, CandidateUpsertRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = getEntity(id);
        apply(candidate, request);
        candidate.setNextAction(request.nextAction() == null || request.nextAction().isBlank() ? candidate.getNextAction() : request.nextAction());
        createWorkflowEvent(candidate, "CANDIDATE_UPDATED", "更新候选人", "更新候选人基础信息");
        return toDetailResponse(candidateRepository.save(candidate));
    }

    public List<TimelineEventResponse> timeline(Long id) {
        getEntity(id);
        return workflowEventRepository.findByCandidateIdOrderByOccurredAtAsc(id).stream()
                .map(event -> new TimelineEventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getActorName(),
                        event.getSourceAction(),
                        event.getStatusCode(),
                        event.getStatusLabel(),
                        event.getNote(),
                        event.getOccurredAt()
                ))
                .toList();
    }

    @Transactional
    public ResumeAssetResponse uploadResume(Long candidateId, MultipartFile file) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = getEntity(candidateId);
        User user = currentUserService.getRequiredUser();
        String objectKey = "candidate-" + candidateId + "/" + System.currentTimeMillis() + "-" + sanitize(file.getOriginalFilename());
        resumeStorageService.store(objectKey, file);

        ResumeAsset asset = new ResumeAsset();
        asset.setCandidate(candidate);
        asset.setObjectKey(objectKey);
        asset.setOriginalFileName(file.getOriginalFilename());
        asset.setContentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType());
        asset.setFileSize(file.getSize());
        asset.setUploadedBy(user);
        asset.setUploadedAt(OffsetDateTime.now());
        ResumeAsset saved = resumeAssetRepository.save(asset);
        createWorkflowEvent(candidate, "RESUME_UPLOADED", "上传简历", "上传候选人简历附件");
        return toResumeResponse(saved);
    }

    public ResponseEntity<Resource> downloadResume(Long candidateId) {
        assertResumeAccess(candidateId);
        ResumeAsset asset = resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return buildResumeResponse(asset, false);
    }

    public ResponseEntity<Resource> previewResume(Long candidateId) {
        assertResumeAccess(candidateId);
        ResumeAsset asset = resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return buildResumeResponse(asset, true);
    }

    private ResponseEntity<Resource> buildResumeResponse(ResumeAsset asset, boolean inline) {
        Resource resource = resumeStorageService.loadAsResource(asset.getObjectKey());
        ContentDisposition contentDisposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(asset.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(resource);
    }

    public Candidate getEntity(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidate not found"));
    }

    public ResumeAsset getLatestResume(Long candidateId) {
        return resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId).orElse(null);
    }

    private void apply(Candidate candidate, CandidateUpsertRequest request) {
        candidate.setName(request.name());
        candidate.setPosition(request.position());
        candidate.setSource(request.source());
        candidate.setSubmittedDate(request.submittedDate());
        candidate.setPhone(request.phone());
        candidate.setEmail(request.email());
        candidate.setLocation(request.location());
        candidate.setExperience(request.experience());
        candidate.setEducation(request.education());
        candidate.setSkillsSummary(request.skillsSummary());
        candidate.setProjectSummary(request.projectSummary());
        candidate.setJdSummary(request.jdSummary());
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found"));
            candidate.setDepartment(department);
        } else {
            candidate.setDepartment(null);
        }
        if (request.ownerId() != null) {
            User owner = userRepository.findById(request.ownerId())
                    .orElseThrow(() -> new NotFoundException("Owner not found"));
            candidate.setOwner(owner);
        }
    }

    private CandidateResponse toResponse(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getName(),
                candidate.getPosition(),
                candidate.getDepartment() == null ? null : candidate.getDepartment().getName(),
                candidate.getStatus().getCode(),
                candidate.getStatus().getLabel(),
                candidate.getOwner() == null ? null : candidate.getOwner().getDisplayName(),
                candidate.getSource(),
                candidate.getSubmittedDate(),
                candidate.getUpdatedAt(),
                candidate.getNextAction()
        );
    }

    private CandidateDetailResponse toDetailResponse(Candidate candidate) {
        ResumeAsset asset = getLatestResume(candidate.getId());
        return new CandidateDetailResponse(
                candidate.getId(),
                candidate.getName(),
                candidate.getPosition(),
                candidate.getDepartment() == null ? null : candidate.getDepartment().getName(),
                candidate.getStatus().getCode(),
                candidate.getStatus().getLabel(),
                candidate.getOwner() == null ? null : candidate.getOwner().getDisplayName(),
                candidate.getSource(),
                candidate.getSubmittedDate(),
                candidate.getUpdatedAt(),
                candidate.getNextAction(),
                candidate.getPhone(),
                candidate.getEmail(),
                candidate.getLocation(),
                candidate.getExperience(),
                candidate.getEducation(),
                candidate.getSkillsSummary(),
                candidate.getProjectSummary(),
                candidate.getJdSummary(),
                asset == null ? null : toResumeResponse(asset)
        );
    }

    private ResumeAssetResponse toResumeResponse(ResumeAsset asset) {
        return new ResumeAssetResponse(
                asset.getId(),
                asset.getOriginalFileName(),
                asset.getContentType(),
                asset.getFileSize(),
                asset.getUploadedAt(),
                asset.getUploadedBy().getDisplayName()
        );
    }

    private void createWorkflowEvent(Candidate candidate, String eventType, String sourceAction, String note) {
        User actor = currentUserService.getRequiredUser();
        recordWorkflowEvent(candidate, eventType, sourceAction, note, actor);
    }

    @Transactional
    public void recordWorkflowEvent(Candidate candidate, String eventType, String sourceAction, String note) {
        recordWorkflowEvent(candidate, eventType, sourceAction, note, currentUserService.getRequiredUser());
    }

    private void recordWorkflowEvent(Candidate candidate, String eventType, String sourceAction, String note, User actor) {
        WorkflowEvent event = new WorkflowEvent();
        event.setCandidate(candidate);
        event.setEventType(eventType);
        event.setActorId(actor.getId());
        event.setActorName(actor.getDisplayName());
        event.setSourceAction(sourceAction);
        event.setStatusCode(candidate.getStatus().getCode());
        event.setStatusLabel(candidate.getStatus().getLabel());
        event.setNote(note);
        event.setOccurredAt(OffsetDateTime.now());
        workflowEventRepository.save(event);
    }

    private void assertResumeAccess(Long candidateId) {
        User user = currentUserService.getRequiredUser();
        if (currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            return;
        }
        boolean assignmentAccess = departmentAssignmentRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .anyMatch(assignment -> assignment.getReviewer().getId().equals(user.getId()));
        if (assignmentAccess) {
            return;
        }
        boolean interviewerAccess = interviewPlanRepository.existsByCandidateIdAndInterviewerId(candidateId, user.getId());
        if (interviewerAccess) {
            return;
        }
        throw new ForbiddenException("You do not have permission to access this resume");
    }

    private boolean matchesQuery(Candidate candidate, String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return candidate.getName().toLowerCase(Locale.ROOT).contains(normalized)
                || candidate.getPosition().toLowerCase(Locale.ROOT).contains(normalized)
                || (candidate.getDepartment() != null &&
                candidate.getDepartment().getName().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private String sanitize(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "resume.bin";
        }
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
