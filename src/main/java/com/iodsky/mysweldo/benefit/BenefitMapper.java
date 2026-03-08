package com.iodsky.mysweldo.benefit;

import org.springframework.stereotype.Component;

@Component
public class BenefitMapper {

    public BenefitDto toDto(Benefit entity) {
        if (entity == null) {
            return null;
        }

        return BenefitDto.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
