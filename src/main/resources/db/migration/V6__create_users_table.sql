-- Tabla de usuarios para autenticación
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insertar usuario admin por defecto
-- Contraseña: admin123 (hasheada con BCrypt)
INSERT INTO users (email, password, role, enabled) 
VALUES (
    'admin@lago-escondido.com', 
    '$2a$10$l0uX5phdoKE780yFXa5BmeV9r/RGM5Vui67zMKqUe5tJxi7ykO986', 
    'ADMIN', 
    true
) ON CONFLICT (email) DO NOTHING;

-- Índice para búsquedas rápidas por email
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

