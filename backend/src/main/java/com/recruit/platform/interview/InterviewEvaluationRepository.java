package com.recruit.platform.interview;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewEvaluationRepository extends JpaRepository<InterviewEvaluation, Long> {

    @EntityGraph(attributePaths = {"interviewer", "interviewPlan"})
    List<InterviewEvaluation> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
}
