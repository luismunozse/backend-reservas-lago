-- Agregar campos first_name y last_name a la tabla users
ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name VARCHAR(100);

-- Actualizar el usuario admin existente con valores por defecto
UPDATE users
SET first_name = 'Admin',
    last_name = 'Sistema'
WHERE email = 'admin@lago-escondido.com';

-- Hacer los campos NOT NULL después de actualizar los datos existentes
ALTER TABLE users
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

-- Crear índice para búsquedas por nombre
CREATE INDEX IF NOT EXISTS idx_users_names ON users(first_name, last_name);

