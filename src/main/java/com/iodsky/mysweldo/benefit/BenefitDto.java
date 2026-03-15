package com.iodsky.mysweldo.benefit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenefitDto {
    private String code;
    private String description;
    private Boolean taxable;
    private BigDecimal nonTaxablelimit;
    private Instant createdAt;
    private Instant updatedAt;
}
