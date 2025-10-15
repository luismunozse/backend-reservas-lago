package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
