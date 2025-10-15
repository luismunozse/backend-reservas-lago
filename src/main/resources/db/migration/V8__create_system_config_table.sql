-- Tabla para almacenar configuraciones del sistema
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Insertar configuración por defecto para reservas educativas (habilitadas por defecto)
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES ('educational_reservations_enabled', 'true', NOW());
