package com.iodsky.mysweldo.benefit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenefitDto {
    private String code;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
