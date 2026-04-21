package com.recruit.platform.workflow;

import com.recruit.platform.common.enums.AssignmentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentAssignmentRepository extends JpaRepository<DepartmentAssignment, Long> {

    @EntityGraph(attributePaths = {"candidate", "candidate.department", "reviewer", "department"})
    List<DepartmentAssignment> findByReviewerIdAndStatusOrderByDueAtAsc(Long reviewerId, AssignmentStatus status);

    @EntityGraph(attributePaths = {"candidate", "reviewer", "department"})
    List<DepartmentAssignment> findByStatusAndDueAtBefore(AssignmentStatus status, OffsetDateTime dueAt);

    @EntityGraph(attributePaths = {"candidate", "reviewer", "department"})
    Optional<DepartmentAssignment> findTopByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    @EntityGraph(attributePaths = {"candidate", "reviewer", "department"})
    List<DepartmentAssignment> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
}
