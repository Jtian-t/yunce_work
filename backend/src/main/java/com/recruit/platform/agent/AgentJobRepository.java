package com.recruit.platform.agent;

import com.recruit.platform.common.enums.AgentJobType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentJobRepository extends JpaRepository<AgentJob, Long> {

    @EntityGraph(attributePaths = {"candidate"})
    Optional<AgentJob> findTopByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    @EntityGraph(attributePaths = {"candidate"})
    Optional<AgentJob> findTopByCandidateIdAndJobTypeOrderByCreatedAtDesc(Long candidateId, AgentJobType jobType);

    @EntityGraph(attributePaths = {"candidate"})
    java.util.List<AgentJob> findByCandidateIdAndJobTypeOrderByCreatedAtDesc(Long candidateId, AgentJobType jobType);
}
