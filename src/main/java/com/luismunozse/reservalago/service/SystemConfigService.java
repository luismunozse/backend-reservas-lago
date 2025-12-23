package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.SystemConfig;
import com.luismunozse.reservalago.repo.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    public static final String EDUCATIONAL_RESERVATIONS_ENABLED = "educational_reservations_enabled";
    public static final String DEFAULT_CAPACITY = "default_capacity";

    public boolean isEducationalReservationsEnabled() {
        return configRepository.findByConfigKey(EDUCATIONAL_RESERVATIONS_ENABLED)
                .map(config -> "true".equalsIgnoreCase(config.getConfigValue()))
                .orElse(true); // Por defecto habilitado
    }

    @Transactional
    public void setEducationalReservationsEnabled(boolean enabled) {
        SystemConfig config = configRepository.findByConfigKey(EDUCATIONAL_RESERVATIONS_ENABLED)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(EDUCATIONAL_RESERVATIONS_ENABLED);
                    return newConfig;
                });
        config.setConfigValue(String.valueOf(enabled));
        configRepository.save(config);
    }

    public int getDefaultCapacity() {
        return configRepository.findByConfigKey(DEFAULT_CAPACITY)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        return 30; // fallback defensivo
                    }
                })
                .orElse(30); // por defecto 30
    }


    @Transactional
    public void setDefaultCapacity(Integer capacity) {
        if (capacity == null || capacity < 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor o igual a 0");
        }

        SystemConfig config = configRepository.findByConfigKey(DEFAULT_CAPACITY)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(DEFAULT_CAPACITY);
                    return newConfig;
                });

        config.setConfigValue(String.valueOf(capacity));
        configRepository.save(config);
    }

}
