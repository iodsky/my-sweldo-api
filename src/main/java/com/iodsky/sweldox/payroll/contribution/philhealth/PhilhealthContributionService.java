package com.iodsky.sweldox.payroll.contribution.philhealth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhilhealthContributionService {

    private final PhilhealthContributionRepository philhealthContributionRepository;

    @Transactional
    public PhilhealthContribution createPhilhealthContribution(PhilhealthContributionRequest request) {
        // Check if configuration already exists for this effective date
        if (philhealthContributionRepository.findLatestByEffectiveDate(request.getEffectiveDate()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "PhilHealth contribution configuration already exists for effective date: " + request.getEffectiveDate()
            );
        }

        PhilhealthContribution contribution = PhilhealthContribution.builder()
                .premiumRate(request.getPremiumRate())
                .maxSalaryCap(request.getMaxSalaryCap())
                .minSalaryFloor(request.getMinSalaryFloor())
                .fixedContribution(request.getFixedContribution())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return philhealthContributionRepository.save(contribution);
    }

    public Page<PhilhealthContribution> getAllPhilhealthContributions(int page, int limit, LocalDate effectiveDate) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "effectiveDate"));

        if (effectiveDate != null) {
            return philhealthContributionRepository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.lessThanOrEqualTo(root.get("effectiveDate"), effectiveDate),
                            cb.isNull(root.get("deletedAt"))
                    ),
                    pageable
            );
        }

        return philhealthContributionRepository.findAll(
                (root, query, cb) -> cb.isNull(root.get("deletedAt")),
                pageable
        );
    }

    public PhilhealthContribution getPhilhealthContributionById(UUID id) {
        return philhealthContributionRepository.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "PhilHealth contribution configuration not found with ID: " + id
                ));
    }

    public PhilhealthContribution getLatestPhilhealthContribution(LocalDate date) {
        return philhealthContributionRepository.findLatestByEffectiveDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No PhilHealth contribution configuration found for date: " + date
                ));
    }

    @Transactional
    public PhilhealthContribution updatePhilhealthContribution(UUID id, PhilhealthContributionRequest request) {
        PhilhealthContribution contribution = getPhilhealthContributionById(id);

        contribution.setPremiumRate(request.getPremiumRate());
        contribution.setMaxSalaryCap(request.getMaxSalaryCap());
        contribution.setEffectiveDate(request.getEffectiveDate());

        return philhealthContributionRepository.save(contribution);
    }

    @Transactional
    public void deletePhilhealthContribution(UUID id) {
        PhilhealthContribution contribution = getPhilhealthContributionById(id);
        contribution.setDeletedAt(Instant.now());
        philhealthContributionRepository.save(contribution);
    }
}
