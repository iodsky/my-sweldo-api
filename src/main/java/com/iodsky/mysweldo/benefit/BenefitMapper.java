package com.iodsky.mysweldo.benefit;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BenefitMapper {

    public BenefitDto toDto(Benefit entity) {
        if (entity == null) {
            return null;
        }

        return BenefitDto.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .taxable(entity.isTaxable())
                .nonTaxablelimit(entity.getNonTaxableLimit() != null ? entity.getNonTaxableLimit() : BigDecimal.ZERO)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
