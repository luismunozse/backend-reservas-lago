# Arquitectura del Backend - Sistema de Reservas Lago Escondido

## 📋 Índice

1. [Stack Tecnológico](#stack-tecnológico)
2. [Arquitectura de Capas](#arquitectura-de-capas)
3. [Decisiones de Diseño](#decisiones-de-diseño)
4. [Entornos: Dev vs Prod](#entornos-dev-vs-prod)
5. [Seguridad](#seguridad)
6. [Base de Datos](#base-de-datos)
7. [Sistema de Notificaciones](#sistema-de-notificaciones)
8. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## Stack Tecnológico

### Backend
- **Framework:** Spring Boot 3.5.5
- **Java:** 21 (LTS) con Eclipse Temurin
- **Build:** Maven 3.9.6

### Base de Datos
- **Motor:** PostgreSQL 16
- **Migraciones:** Flyway (7 migraciones aplicadas)
- **ORM:** JPA/Hibernate (validación de esquema)

### Contenedorización
- **Docker:** Multi-stage build
- **Compose:** Orquestación de servicios
- **Proxy:** Nginx (solo producción)

### Librerías Clave
| Librería | Versión | Propósito |
|----------|---------|-----------|
| Spring Security | 3.5.5 | Autenticación/Autorización |
| JJWT | 0.12.3 | Tokens JWT |
| Twilio | 10.1.0 | WhatsApp API |
| Apache POI | 5.2.5 | Exportación Excel |
| SpringDoc OpenAPI | 2.8.11 | Documentación Swagger |
| Flyway | Incluido | Migraciones DB |

---

## Arquitectura de Capas

### Estructura del Proyecto

```
src/main/java/com.luismunozse.reservalago/
│
├── 📁 config/                    [Configuración]
│   ├── CorsConfig.java           → CORS origins permitidos
│   ├── JwtAuthenticationFilter.java → Interceptor de requests
│   ├── OpenApiConfig.java        → Swagger/OpenAPI setup
│   └── SecurityConfig.java       → Spring Security + JWT
│
├── 📁 controller/                [Capa de presentación]
│   ├── PublicController.java    → API pública (sin auth)
│   ├── AuthController.java      → Login JWT
│   ├── AdminController.java     → Endpoints admin (requiere JWT)
│   ├── UserController.java      → CRUD usuarios
│   └── ApiExceptionHandler.java → Manejo centralizado de errores
│
├── 📁 service/                   [Lógica de negocio]
│   ├── ReservationService.java  → Lógica principal de reservas
│   ├── AvailabilityService.java → Cálculo de disponibilidad
│   ├── WhatsAppService.java     → Integración Twilio
│   ├── UserService.java         → Gestión usuarios admin
│   ├── JwtService.java          → Generación/validación JWT
│   ├── ReservationMapper.java   → DTOs ↔ Entities
│   └── ReservationExcelExporter.java → Exportación XLSX
│
├── 📁 repo/                      [Capa de datos]
│   ├── ReservationRepository.java
│   ├── AvailabilityRuleRepository.java
│   ├── SystemConfigRepository.java
│   └── UserRepository.java
│
├── 📁 model/                     [Entidades JPA]
│   ├── Reservation.java          → Reserva principal
│   ├── ReservationVisitor.java   → Visitantes adicionales
│   ├── AvailabilityRule.java     → Reglas de capacidad por día
│   ├── User.java                 → Usuarios admin
│   ├── SystemConfig.java         → Configuración dinámica
│   └── [Enums]                   → Circuit, ReservationStatus, etc.
│
└── 📁 dto/                       [Objetos de transferencia]
    ├── CreateReservationRequest.java
    ├── AdminReservationDTO.java
    ├── LoginRequest.java/Response.java
    └── ...
```

### Flujo de una Request

```
1. HTTP Request
   ↓
2. JwtAuthenticationFilter (si es endpoint protegido)
   ↓
3. Controller (validación de inputs)
   ↓
4. Service (lógica de negocio)
   ↓
5. Repository (acceso a datos)
   ↓
6. PostgreSQL
   ↓
7. Service (mapeo a DTOs)
   ↓
8. Controller (HTTP Response)
```

---

## Decisiones de Diseño

### 1. ¿Por qué 2 Docker Compose files?

**Problema:** Configuraciones de desarrollo vs producción son muy diferentes.

**Solución:** Separar en archivos específicos.

| Aspecto | Desarrollo | Producción |
|---------|------------|------------|
| Seguridad | Relajada (debugging) | Estricta (SSL, rate limiting) |
| Logs | Verbose (DEBUG) | Conciso (INFO/WARN) |
| Swagger | Habilitado | Deshabilitado |
| Proxy | Ninguno | Nginx + SSL |
| CORS | Permisivo (localhost) | Solo dominios específicos |

**Alternativa descartada:** Un solo archivo con overrides → Genera confusión y errores.

---

### 2. ¿Por qué bases de datos con nombres diferentes?

- **Dev:** `lago` (simple, fácil de recordar)
- **Prod:** `lago_prod` (distingue claramente el entorno)

**Razón:** Evitar conexiones accidentales a producción desde entornos locales.

**Trade-off:** Requiere documentación clara (este archivo).

---

### 3. ¿Por qué Flyway en lugar de Hibernate DDL-Auto?

**Hibernate DDL-Auto problems:**
- No versionado (no se puede auditar cambios)
- Riesgoso en producción (puede borrar datos)
- No permite migraciones complejas

**Flyway beneficios:**
✅ Versionado (V4, V5, V6, ...)
✅ Rollback manual si es necesario
✅ Migraciones como código
✅ Auditable (Git history)

**Configuración actual:** `ddl-auto: validate` (solo valida, no modifica esquema).

---

### 4. ¿Por qué JWT en lugar de sesiones?

**Problema:** Backend stateless, frontend separado (posiblemente en otro servidor).

**JWT beneficios:**
✅ Stateless (no requiere almacenamiento en servidor)
✅ Escalable (múltiples instancias del backend)
✅ Compatible con SPA (React, Next.js)
✅ CORS-friendly

**Configuración:**
- Secret: Variable de entorno obligatoria en prod
- Expiración: 24 horas (configurable)
- Algoritmo: HS256

---

### 5. ¿Por qué WhatsApp en lugar de Email?

**Contexto:** Público argentino prefiere WhatsApp.

**Implementado:**
✅ WhatsApp (Twilio API)

**No implementado:**
❌ Email (mencionado en documentación antigua pero sin código)

**Futuro:** Agregar email como notificación secundaria.

---

### 6. ¿Por qué Multi-stage Docker build?

**Problema:** Imagen con Maven completo = ~800MB.

**Solución:**
- **Stage 1 (build):** Maven + JDK → Compila JAR
- **Stage 2 (runtime):** Solo JRE Alpine → Ejecuta JAR

**Resultado:** Imagen final ~250MB (3x más pequeña).

**Beneficio adicional:**
- BuildKit cache → Builds incrementales rápidos
- Usuario no-root → Mejor seguridad

---

## Entornos: Dev vs Prod

### Desarrollo (`docker-compose.dev.yml`)

**Objetivo:** Facilitar debugging y pruebas rápidas.

**Características:**
```yaml
Profile: dev
DB: lago (postgres/postgres)
CORS: localhost:3000,3002,127.0.0.1:*
Swagger: http://localhost:8080/docs
WhatsApp: Opcional (configurable)
Logs: DEBUG nivel
Actuator: Todos los endpoints expuestos
SSL: No (HTTP plano)
```

**Cuándo usar:**
- Desarrollo local en tu máquina
- Testing de features nuevas
- Debugging con logs detallados
- Probar integración con frontend local

---

### Producción (`docker-compose.prod.yml`)

**Objetivo:** Máxima seguridad y performance.

**Características:**
```yaml
Profile: prod
DB: lago_prod (lago_user con password seguro)
CORS: Solo dominios HTTPS específicos
Swagger: Deshabilitado
WhatsApp: Habilitado por defecto
Logs: INFO/WARN a archivo rotativo
Actuator: Solo health, info, metrics
SSL: HTTPS con Nginx
Networks: Aisladas (lago-network)
Java: Optimizado (-Xmx1g -XX:+UseG1GC)
Rate limiting: Nginx (10 req/s API, 5 req/min login)
```

**Cuándo usar:**
- Servidor en la nube (VPS, AWS, etc.)
- Deploy final con dominio real
- Producción accesible al público

---

## Seguridad

### Implementaciones de Seguridad

#### 1. Autenticación JWT
```
Login → Validar credenciales → Generar JWT → Cliente guarda token
Request protegido → Enviar JWT en header → Validar token → Permitir/Denegar
```

**Headers esperados:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 2. CORS Restrictivo (Producción)
```java
@Configuration
public class CorsConfig {
    // Solo permite orígenes específicos
    allowedOrigins: process.env.ALLOWED_ORIGINS
}
```

#### 3. Rate Limiting (Nginx en Prod)
```nginx
# API general: 10 requests/segundo
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

# Login: 5 intentos/minuto
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;
```

#### 4. Headers de Seguridad
```nginx
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
```

#### 5. Usuario no-root en Docker
```dockerfile
USER appuser  # UID 1001, no privilegios de root
```

---

### Secretos y Variables Sensibles

#### ✅ Protegido (en .gitignore):
- `.env` (credenciales Twilio dev)
- `.env.prod` (todas las credenciales producción)
- `run-dev.ps1` / `run-dev.sh`
- `ssl/` (certificados)

#### ⚠️ Verificar:
- Historial de Git: Credenciales nunca commiteadas ✅
- Variables hardcodeadas: Solo en dev, nunca en prod ✅

#### 🔐 Generación de secretos:
```bash
# JWT_SECRET (64+ caracteres)
openssl rand -base64 64

# DATABASE_PASSWORD
openssl rand -base64 32

# ADMIN_PASSWORD
openssl rand -base64 24
```

---

## Base de Datos

### Migraciones Flyway

Ubicación: `src/main/resources/db/migration/`

| Archivo | Descripción |
|---------|-------------|
| V4__init.sql | Tablas principales (reservations, availability_rules) |
| V5__unique_reservation_per_day.sql | Constraint: 1 reserva por DNI por día |
| V6__create_users_table.sql | Tabla de usuarios admin |
| V7__add_user_names.sql | Columnas first_name, last_name |
| V8__create_system_config_table.sql | Configuración dinámica |
| V9__create_reservation_visitors.sql | Visitantes adicionales |
| V10__drop_allergies_column.sql | Eliminar columna obsoleta |

**Orden de ejecución:** Automático al iniciar Spring Boot (Flyway).

**Estado:** Todas las migraciones aplicadas ✅

---

### Modelo de Datos Principal

```
Reservation (Reserva principal)
  ├── visitDate: LocalDate
  ├── firstName, lastName, dni, phone, email
  ├── circuit: Enum (A, B, C, D)
  ├── visitorType: Enum (INDIVIDUAL, EDUCATIONAL, EVENT)
  ├── adults18Plus, children2To17, babiesLessThan2
  ├── reducedMobility: int
  ├── status: Enum (PENDING, CONFIRMED, CANCELLED)
  ├── howHeard: Enum (fuente de la reserva)
  └── visitors: List<ReservationVisitor> (visitantes adicionales)

AvailabilityRule (Capacidad por día)
  ├── ruleDate: LocalDate
  └── capacity: int (override de DEFAULT_CAPACITY)

User (Administradores)
  ├── email: String (username)
  ├── password: BCrypt hash
  ├── firstName, lastName
  └── enabled: boolean

SystemConfig (Configuración dinámica)
  ├── configKey: String (ej: "educational_reservations_enabled")
  └── configValue: String
```

---

### Nombres de DB en Diferentes Contextos

| Contexto | Nombre DB | Usuario | Host |
|----------|-----------|---------|------|
| **Dev local (sin Docker)** | `lago` | `postgres` | `localhost:5432` |
| **Dev Docker (compose.dev)** | `lago` | `postgres` | `db:5432` |
| **Prod Docker (compose.prod)** | `lago_prod` | `lago_user` | `postgres:5432` |

**Importante:** `application-dev.yml` y `application-prod.yml` usan variables de entorno que se configuran en docker-compose.

---

## Sistema de Notificaciones

### WhatsApp (Twilio)

**Estado:** ✅ Implementado y funcional

**Flujo:**
```
Admin confirma reserva
  ↓
WhatsAppService.sendConfirmation()
  ↓
Twilio API
  ↓
WhatsApp del cliente
```

**Configuración:**
```yaml
WHATSAPP_ENABLED: true/false
TWILIO_ACCOUNT_SID: ACxxxxxxxxx
TWILIO_AUTH_TOKEN: xxxxxxxxx
TWILIO_WHATSAPP_FROM: whatsapp:+14155238886
```

**Características:**
- Normalización automática de números argentinos (+549 handling)
- Plantillas de mensaje en español
- Retry handling (manejo de errores Twilio)

**Endpoints que envían WhatsApp:**
- `POST /api/admin/reservations/{id}/confirm`
- `POST /api/admin/reservations/{id}/cancel`

---

### Email

**Estado:** ❌ NO implementado

**Razón:** Prioridad inicial fue WhatsApp (más usado en Argentina).

**Próximos pasos (si se implementa):**
1. Agregar dependencia `spring-boot-starter-mail`
2. Crear servicio `EmailService`
3. Plantillas HTML con Thymeleaf
4. Configurar SMTP (Gmail, SendGrid, etc.)

---

## Preguntas Frecuentes

### ¿Por qué hay un archivo `docker-compose.simple.yml.bak`?

Era la versión original (`docker-compose.yml`), pero era redundante con `docker-compose.dev.yml`. Se renombró a `.bak` para evitar confusión pero mantener referencia histórica.

**Puedes eliminarlo:** `rm docker-compose.simple.yml.bak`

---

### ¿Cómo agregar un nuevo endpoint?

1. **Controller:** Agregar método con `@GetMapping/@PostMapping`
2. **Service:** Lógica de negocio
3. **Repository:** Query si es necesario (JPA Query Methods)
4. **DTO:** Si requiere request/response personalizado
5. **Security:** Actualizar `SecurityConfig.java` si es público

---

### ¿Cómo agregar una nueva migración?

1. Crear archivo: `src/main/resources/db/migration/V11__descripcion.sql`
2. Escribir SQL (CREATE, ALTER, INSERT, etc.)
3. Reiniciar aplicación → Flyway ejecuta automáticamente
4. Verificar: `SELECT * FROM flyway_schema_history;`

**Regla:** Nunca modificar migraciones ya aplicadas en producción.

---

### ¿Cómo cambiar el puerto del backend?

**Desarrollo:**
- Editar `docker-compose.dev.yml` línea 38: `ports: - "8081:8080"`

**Producción:**
- Nginx sigue en 80/443
- Backend interno sigue en 8080 (no expuesto)

---

### ¿Cómo escalar horizontalmente?

Spring Boot es stateless (JWT, sin sesiones), así que:

1. **Load balancer:** Nginx, HAProxy, AWS ALB
2. **Múltiples instancias:** `docker compose up --scale app=3`
3. **DB compartida:** Todas apuntan al mismo PostgreSQL
4. **CORS:** Asegurar que `ALLOWED_ORIGINS` incluya dominio del LB

---

### ¿Cómo monitorear en producción?

**Actuator endpoints disponibles:**
- `/actuator/health` → Estado del servicio
- `/actuator/info` → Información de la app
- `/actuator/metrics` → Métricas (memoria, requests, etc.)

**Herramientas recomendadas:**
- **Logs:** Docker logs + rotación automática
- **Métricas:** Prometheus + Grafana
- **Alertas:** Uptime Robot, Datadog, New Relic
- **APM:** Spring Boot Admin, Elastic APM

---

### ¿Cómo hacer rollback de una migración?

Flyway **NO hace rollback automático**. Opciones:

1. **Restaurar backup de DB:**
   ```bash
   docker exec -i lago-postgres psql -U lago_user lago_prod < backup.sql
   ```

2. **Crear migración inversa:**
   ```sql
   -- V12__rollback_v11.sql
   ALTER TABLE reservations DROP COLUMN nueva_columna;
   ```

---

### ¿Por qué init-db.sql ya no crea usuario?

**Antes:** Script SQL creaba usuario con password hardcodeado.

**Ahora:** PostgreSQL crea automáticamente desde variables de entorno:
```yaml
POSTGRES_USER: lago_user
POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
POSTGRES_DB: lago_prod
```

**Beneficio:** Password nunca en código fuente, solo en `.env.prod`.

---

## 📚 Documentación Relacionada

| Documento | Propósito |
|-----------|-----------|
| [README.md](README.md) | Endpoints API, features, setup general |
| [README-DOCKER.md](README-DOCKER.md) | Guía de docker-compose files |
| [PLAN_DESPLIEGUE.md](PLAN_DESPLIEGUE.md) | Despliegue completo paso a paso |
| [SSL-SETUP.md](SSL-SETUP.md) | Configurar certificados SSL |
| [env.prod.example](env.prod.example) | Plantilla de variables de entorno |
| [POSTMAN_README.md](POSTMAN_README.md) | Colección para testing |
| **ARQUITECTURA.md** (este archivo) | Decisiones técnicas y arquitectura |

---

**Mantenido por:** Backend Team
**Última actualización:** Diciembre 2024
**Versión:** 1.0.0
