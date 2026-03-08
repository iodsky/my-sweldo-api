package com.iodsky.mysweldo.contribution;

import org.springframework.stereotype.Component;

@Component
public class ContributionMapper {

    public ContributionDto toDto(Contribution entity) {
        if (entity == null) {
            return null;
        }

        return ContributionDto.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
