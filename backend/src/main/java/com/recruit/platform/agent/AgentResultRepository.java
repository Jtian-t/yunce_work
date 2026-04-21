package com.recruit.platform.agent;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentResultRepository extends JpaRepository<AgentResult, Long> {

    Optional<AgentResult> findByJobId(Long jobId);
}
