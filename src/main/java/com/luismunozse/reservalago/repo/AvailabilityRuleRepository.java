package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule,Long> {
    Optional<AvailabilityRule> findByDay(LocalDate day);
}
