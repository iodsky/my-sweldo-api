package com.iodsky.mysweldo.philhealth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhilhealthRateRepository extends JpaRepository<PhilhealthRate, UUID>, JpaSpecificationExecutor<PhilhealthRate> {

    @Query("SELECT p FROM PhilhealthRate p WHERE p.effectiveDate <= :date AND p.deletedAt IS NULL ORDER BY p.effectiveDate DESC LIMIT 1")
    Optional<PhilhealthRate> findLatestByEffectiveDate(@Param("date") LocalDate date);

}
