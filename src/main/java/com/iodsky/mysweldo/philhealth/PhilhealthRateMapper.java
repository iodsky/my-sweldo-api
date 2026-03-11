package com.iodsky.mysweldo.philhealth;

import org.springframework.stereotype.Component;

@Component
public class PhilhealthRateMapper {

    public PhilhealthRateDto toDto(PhilhealthRate entity) {
        if (entity == null) {
            return null;
        }

        return PhilhealthRateDto.builder()
                .id(entity.getId())
                .premiumRate(entity.getPremiumRate())
                .maxSalaryCap(entity.getMaxSalaryCap())
                .minSalaryFloor(entity.getMinSalaryFloor())
                .fixedContribution(entity.getFixedContribution())
                .effectiveDate(entity.getEffectiveDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
