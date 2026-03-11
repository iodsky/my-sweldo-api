package com.iodsky.mysweldo.philhealth;

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
public class PhilhealthRateService {

    private final PhilhealthRateRepository repository;

    @Transactional
    public PhilhealthRate createPhilhealthRateTable(PhilhealthRateRequest request) {
        // Check if configuration already exists for this effective date
        if (repository.findLatestByEffectiveDate(request.getEffectiveDate()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "PhilHealth rate table already exists for effective date: " + request.getEffectiveDate()
            );
        }

        PhilhealthRate rateTable = PhilhealthRate.builder()
                .premiumRate(request.getPremiumRate())
                .maxSalaryCap(request.getMaxSalaryCap())
                .minSalaryFloor(request.getMinSalaryFloor())
                .fixedContribution(request.getFixedContribution())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return repository.save(rateTable);
    }

    public Page<PhilhealthRate> getAllPhilhealthRateTables(int page, int limit, LocalDate effectiveDate) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "effectiveDate"));

        if (effectiveDate != null) {
            return repository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.lessThanOrEqualTo(root.get("effectiveDate"), effectiveDate),
                            cb.isNull(root.get("deletedAt"))
                    ),
                    pageable
            );
        }

        return repository.findAll(
                (root, query, cb) -> cb.isNull(root.get("deletedAt")),
                pageable
        );
    }

    public PhilhealthRate getPhilhealthRateTableById(UUID id) {
        return repository.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "PhilHealth rate table not found with ID: " + id
                ));
    }

    public PhilhealthRate getLatestPhilhealthRateTable(LocalDate date) {
        return repository.findLatestByEffectiveDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No PhilHealth rate table found for date: " + date
                ));
    }

    @Transactional
    public PhilhealthRate updatePhilhealthRateTable(UUID id, PhilhealthRateRequest request) {
        PhilhealthRate rateTable = getPhilhealthRateTableById(id);

        rateTable.setPremiumRate(request.getPremiumRate());
        rateTable.setMaxSalaryCap(request.getMaxSalaryCap());
        rateTable.setEffectiveDate(request.getEffectiveDate());

        return repository.save(rateTable);
    }

    @Transactional
    public void deletePhilhealthRateTable(UUID id) {
        PhilhealthRate rateTable = getPhilhealthRateTableById(id);
        rateTable.setDeletedAt(Instant.now());
        repository.save(rateTable);
    }
}
