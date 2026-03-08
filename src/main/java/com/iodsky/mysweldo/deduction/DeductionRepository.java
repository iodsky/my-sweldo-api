package com.iodsky.mysweldo.deduction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeductionRepository extends JpaRepository<Deduction, String> {
    Optional<Deduction> findByCode(String code);
}
