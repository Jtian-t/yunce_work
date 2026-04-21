package com.recruit.platform.interview;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewPlanRepository extends JpaRepository<InterviewPlan, Long> {

    @EntityGraph(attributePaths = {"candidate", "interviewer", "organizer", "department"})
    List<InterviewPlan> findByCandidateIdOrderByScheduledAtAsc(Long candidateId);

    @EntityGraph(attributePaths = {"interviewer", "candidate", "organizer", "department"})
    Optional<InterviewPlan> findById(Long id);

    @EntityGraph(attributePaths = {"candidate", "interviewer", "organizer", "department"})
    List<InterviewPlan> findByInterviewerIdOrderByScheduledAtAsc(Long interviewerId);

    @EntityGraph(attributePaths = {"candidate", "interviewer", "organizer", "department"})
    List<InterviewPlan> findAllByOrderByScheduledAtAsc();

    boolean existsByCandidateIdAndInterviewerId(Long candidateId, Long interviewerId);
}
