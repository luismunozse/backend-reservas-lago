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
}
