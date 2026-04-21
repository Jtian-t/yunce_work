package com.recruit.platform.workflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, Long> {

    List<WorkflowEvent> findByCandidateIdOrderByOccurredAtAsc(Long candidateId);
}
