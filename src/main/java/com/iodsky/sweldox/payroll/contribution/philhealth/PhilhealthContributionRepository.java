package com.iodsky.sweldox.payroll.contribution.philhealth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhilhealthContributionRepository extends JpaRepository<PhilhealthContribution, UUID>, JpaSpecificationExecutor<PhilhealthContribution> {

    @Query("SELECT p FROM PhilhealthContribution p WHERE p.effectiveDate <= :date AND p.deletedAt IS NULL ORDER BY p.effectiveDate DESC LIMIT 1")
    Optional<PhilhealthContribution> findLatestByEffectiveDate(@Param("date") LocalDate date);

}
