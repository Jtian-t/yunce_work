package com.recruit.platform.workflow;

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
public class DepartmentTaskController {

    private final WorkflowService workflowService;

    @PostMapping("/api/candidates/{candidateId}/assignments")
    AssignmentResponse assign(@PathVariable Long candidateId, @Valid @RequestBody AssignmentRequest request) {
        return workflowService.assign(candidateId, request);
    }

    @GetMapping("/api/department/tasks")
    List<AssignmentResponse> pendingTasks() {
        return workflowService.pendingTasks();
    }

    @GetMapping("/api/department/tasks/completed")
    List<AssignmentResponse> completedTasks() {
        return workflowService.completedTasks();
    }

    @PostMapping("/api/department/tasks/{assignmentId}/remind")
    AssignmentResponse remind(@PathVariable Long assignmentId) {
        return workflowService.remind(assignmentId);
    }
}
