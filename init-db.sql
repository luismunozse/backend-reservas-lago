-- Script de inicialización de la base de datos para producción
-- Este script se ejecuta automáticamente cuando se crea el contenedor de PostgreSQL

-- Crear usuario de la aplicación si no existe
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'lago_user') THEN
        CREATE USER lago_user WITH PASSWORD 'lago_user_password';
    END IF;
END
$$;

-- Crear base de datos si no existe
SELECT 'CREATE DATABASE lago_prod OWNER lago_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'lago_prod')\gexec

-- Conceder privilegios
GRANT ALL PRIVILEGES ON DATABASE lago_prod TO lago_user;

-- Configuraciones adicionales para la base de datos
ALTER DATABASE lago_prod SET timezone TO 'America/Argentina/Cordoba';
ALTER DATABASE lago_prod SET log_statement TO 'all';
ALTER DATABASE lago_prod SET log_min_duration_statement TO 1000;

-- Comentarios
COMMENT ON DATABASE lago_prod IS 'Base de datos de producción para el sistema de reservas del Lago Escondido';


