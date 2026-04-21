package com.recruit.platform.candidate;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeAssetRepository extends JpaRepository<ResumeAsset, Long> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    Optional<ResumeAsset> findTopByCandidateIdOrderByUploadedAtDesc(Long candidateId);
}
