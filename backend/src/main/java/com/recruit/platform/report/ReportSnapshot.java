package com.recruit.platform.report;

import com.recruit.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ReportSnapshot extends BaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate reportDate;

    @Lob
    @Column(nullable = false)
    private String payloadJson;
}
