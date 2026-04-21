package com.recruit.platform.feedback;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentFeedbackRepository extends JpaRepository<DepartmentFeedback, Long> {

    @EntityGraph(attributePaths = {"reviewer", "assignment"})
    List<DepartmentFeedback> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
}
