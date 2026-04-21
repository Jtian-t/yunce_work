package com.recruit.platform.candidate;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    @EntityGraph(attributePaths = {"department", "owner"})
    List<Candidate> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"department", "owner"})
    Optional<Candidate> findById(Long id);
}
