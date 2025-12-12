# Guía de Docker Compose

Este proyecto tiene **2 archivos Docker Compose** activos. Usa el correcto según tu entorno.

---

## 📁 Archivos Docker Compose

| Archivo | Propósito | Cuándo usar |
|---------|-----------|-------------|
| [docker-compose.dev.yml](docker-compose.dev.yml) | **Desarrollo** | Trabajo local, debugging, pruebas |
| [docker-compose.prod.yml](docker-compose.prod.yml) | **Producción** | Servidor en la nube, deploy final |
| `docker-compose.simple.yml.bak` | ⚠️ **OBSOLETO** | No usar - Versión antigua simplificada |

---

## 🛠️ Desarrollo Local

### Comandos:

```bash
# Levantar servicios de desarrollo
docker compose -f docker-compose.dev.yml up -d

# Ver logs en tiempo real
docker compose -f docker-compose.dev.yml logs -f

# Reiniciar solo el backend
docker compose -f docker-compose.dev.yml restart app

# Parar todo
docker compose -f docker-compose.dev.yml down

# Parar y eliminar volúmenes (reset completo de DB)
docker compose -f docker-compose.dev.yml down -v
```

### Características de desarrollo:

✅ **Swagger UI habilitado:** http://localhost:8080/docs
✅ **Base de datos:** `lago` (postgres/postgres)
✅ **Logs detallados:** DEBUG nivel en consola
✅ **CORS permisivo:** localhost:3000, 3002, 127.0.0.1
✅ **WhatsApp:** Configurado pero requiere credenciales Twilio en `.env`
✅ **Actuator:** Todos los endpoints expuestos
✅ **Healthcheck:** DB con reintentos automáticos

### Variables de entorno:

Crear archivo `.env` en la raíz del backend (ya en .gitignore):

```env
TWILIO_ACCOUNT_SID=tu_sid
TWILIO_AUTH_TOKEN=tu_token
```

Si no usas WhatsApp, edita [docker-compose.dev.yml](docker-compose.dev.yml) línea 33:
```yaml
WHATSAPP_ENABLED: "false"
```

---

## 🚀 Producción

### Pre-requisitos:

1. **Certificados SSL:** Ver [SSL-SETUP.md](SSL-SETUP.md)
2. **Variables de entorno:** Crear `.env.prod` basado en [env.prod.example](env.prod.example)
3. **DNS configurado:** Apuntando al servidor

### Comandos:

```bash
# Primera vez: Construir y levantar
docker compose -f docker-compose.prod.yml up -d --build

# Actualizar aplicación (rebuild)
docker compose -f docker-compose.prod.yml up -d --build app

# Ver logs
docker compose -f docker-compose.prod.yml logs -f app

# Ver logs de nginx
docker compose -f docker-compose.prod.yml logs -f nginx

# Reiniciar todo
docker compose -f docker-compose.prod.yml restart

# Parar (sin eliminar datos)
docker compose -f docker-compose.prod.yml down
```

### Características de producción:

🔒 **Nginx reverse proxy:** Puertos 80/443 con SSL
🔒 **Swagger DESHABILITADO:** Solo API disponible
🔒 **Base de datos:** `lago_prod` (lago_user con password seguro)
🔒 **Logs a archivo:** `/var/log/lago-escondido/application.log`
🔒 **Java optimizado:** 1GB heap, G1GC, container support
🔒 **Healthchecks:** DB + App + Nginx
🔒 **Networks aisladas:** `lago-network` (bridge)
🔒 **Volúmenes persistentes:** `postgres_data` + `app_logs`

### Variables de entorno requeridas (.env.prod):

```env
# Obligatorias
DATABASE_PASSWORD=...
JWT_SECRET=...
ADMIN_USERNAME=...
ADMIN_PASSWORD=...

# Opcionales
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
ALLOWED_ORIGINS=https://tu-dominio.com
```

Ver plantilla completa: [env.prod.example](env.prod.example)

---

## 🔄 Diferencias Clave

| Feature | Desarrollo | Producción |
|---------|------------|------------|
| **Base de datos** | `lago` | `lago_prod` |
| **Imagen PostgreSQL** | `postgres:16` | `postgres:16-alpine` |
| **Usuario DB** | `postgres` | `lago_user` |
| **Swagger UI** | ✅ Habilitado | ❌ Deshabilitado |
| **Logs** | DEBUG en consola | INFO en archivo |
| **CORS** | localhost:* | Solo dominios permitidos |
| **SSL/HTTPS** | ❌ No | ✅ Sí (nginx) |
| **Container names** | `lago-*-dev` | `lago-*-prod` |
| **Java heap** | Default | 1GB optimizado |
| **Nginx** | ❌ No | ✅ Sí (puerto 80/443) |
| **Networks** | Default bridge | `lago-network` |
| **Init script** | ❌ No | ✅ `init-db.sql` |
| **Rate limiting** | ❌ No | ✅ Sí (nginx) |

---

## 🐳 Verificación Post-Deploy

### Desarrollo:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/docs

# API pública
curl http://localhost:8080/api/availability
```

### Producción:

```bash
# Health check (HTTPS)
curl https://api.tu-dominio.com/actuator/health

# API pública
curl https://api.tu-dominio.com/api/availability

# Verificar certificado SSL
curl -vI https://api.tu-dominio.com 2>&1 | grep "SSL connection"
```

---

## 🗑️ Archivo Obsoleto

**`docker-compose.simple.yml.bak`** es la versión antigua (antes `docker-compose.yml`).

**¿Por qué se removió?**
- Era redundante con `docker-compose.dev.yml`
- No tenía healthchecks
- No tenía configuración de WhatsApp
- No tenía variables CORS específicas
- Generaba confusión sobre cuál usar

**¿Se puede eliminar?**
Sí, pero se mantiene como `.bak` por si necesitas referencia histórica.

```bash
# Eliminar si estás seguro de no necesitarlo
rm docker-compose.simple.yml.bak
```

---

## 📚 Documentación Relacionada

- **Despliegue completo:** [PLAN_DESPLIEGUE.md](PLAN_DESPLIEGUE.md)
- **Configurar SSL:** [SSL-SETUP.md](SSL-SETUP.md)
- **Variables de entorno:** [env.prod.example](env.prod.example)
- **Arquitectura general:** [ARQUITECTURA.md](ARQUITECTURA.md)
- **Endpoints API:** [README.md](README.md)
- **Colección Postman:** [POSTMAN_README.md](POSTMAN_README.md)

---

## ⚡ Quick Start

### Desarrollo:
```bash
docker compose -f docker-compose.dev.yml up -d
```

### Producción:
```bash
# 1. Configurar variables
cp env.prod.example .env.prod
nano .env.prod

# 2. Configurar SSL (ver SSL-SETUP.md)
mkdir ssl

# 3. Levantar servicios
docker compose -f docker-compose.prod.yml up -d --build
```

---

**¿Dudas?** Revisa [PLAN_DESPLIEGUE.md](PLAN_DESPLIEGUE.md) o [ARQUITECTURA.md](ARQUITECTURA.md)
