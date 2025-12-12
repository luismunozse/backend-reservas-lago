-- ============================================
-- Script de inicialización de la base de datos para producción
-- ============================================
-- Este script se ejecuta automáticamente cuando se crea el contenedor de PostgreSQL
--
-- IMPORTANTE: Este script NO crea el usuario ni la base de datos.
-- Eso se hace automáticamente por PostgreSQL usando las variables de entorno:
--   - POSTGRES_DB (nombre de la base de datos)
--   - POSTGRES_USER (nombre del usuario)
--   - POSTGRES_PASSWORD (contraseña del usuario)
--
-- Este script solo configura parámetros adicionales de la base de datos
-- ============================================

-- Configuraciones adicionales para la base de datos
ALTER DATABASE lago_prod SET timezone TO 'America/Argentina/Cordoba';

-- Logging: Solo registrar queries que tarden más de 1 segundo
ALTER DATABASE lago_prod SET log_min_duration_statement TO 1000;

-- Logging: No registrar todos los statements (reduce carga de I/O)
ALTER DATABASE lago_prod SET log_statement TO 'ddl';

-- Comentario descriptivo
COMMENT ON DATABASE lago_prod IS 'Base de datos de producción para el sistema de reservas del Lago Escondido';

-- ============================================
-- NOTA: PostgreSQL ya creó automáticamente:
-- ============================================
-- ✓ Base de datos: lago_prod (desde POSTGRES_DB)
-- ✓ Usuario: lago_user (desde POSTGRES_USER)
-- ✓ Password: ******* (desde POSTGRES_PASSWORD)
-- ✓ Privilegios: lago_user es owner de lago_prod
--
-- Las migraciones de Flyway crearán las tablas automáticamente
-- al iniciar la aplicación Spring Boot
-- ============================================



