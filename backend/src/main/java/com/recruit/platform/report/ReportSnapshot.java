package com.recruit.platform.report;

import com.recruit.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ReportSnapshot extends BaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(nullable = false, length = 20000)
    private String payloadJson;
}
