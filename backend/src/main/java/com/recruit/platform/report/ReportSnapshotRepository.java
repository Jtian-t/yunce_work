package com.recruit.platform.report;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, Long> {

    Optional<ReportSnapshot> findByReportDate(LocalDate reportDate);
}
