# Plan de Despliegue - Backend Reservas Lago

## Resumen del Stack

| Componente | Tecnología |
|------------|------------|
| Backend | Spring Boot 3.5.5 + Java 21 |
| Base de datos | PostgreSQL 16 |
| Contenedores | Docker (multi-stage build) |
| Migraciones | Flyway |
| Proxy reverso | Nginx (opcional) |

---

## 1. Preparación del Servidor Dedicado

```bash
# Instalar Docker y Docker Compose
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Crear estructura de directorios
mkdir -p /opt/lago-reservas/{logs,backups,data}
cd /opt/lago-reservas
```

---

## 2. Variables de Entorno Requeridas

Crear archivo `.env` en el servidor con las siguientes variables:

```env
# ===== BASE DE DATOS =====
DATABASE_PASSWORD=<contraseña_segura>
DATABASE_USERNAME=lago_user

# ===== ADMIN INICIAL =====
ADMIN_USERNAME=admin@tudominio.com
ADMIN_PASSWORD=<contraseña_admin_segura>

# ===== JWT (OBLIGATORIO) =====
# Generar con: openssl rand -base64 64
JWT_SECRET=<clave_secreta_64_caracteres_minimo>
JWT_EXPIRATION=86400000

# ===== CAPACIDAD =====
DEFAULT_CAPACITY=50

# ===== CORS - DOMINIOS DEL FRONTEND =====
ALLOWED_ORIGINS=https://tudominio.com,https://www.tudominio.com

# ===== EMAIL (opcional) =====
APP_MAIL_ENABLED=true
MAIL_HOST=smtp.tuproveedor.com
MAIL_PORT=587
MAIL_USERNAME=correo@tudominio.com
MAIL_PASSWORD=<contraseña_email>

# ===== WHATSAPP TWILIO (opcional) =====
TWILIO_ACCOUNT_SID=<tu_sid>
TWILIO_AUTH_TOKEN=<tu_token>
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
```

> **IMPORTANTE para Frontend:** La variable `ALLOWED_ORIGINS` debe contener los dominios exactos desde donde se harán las peticiones. Sin esto, las llamadas al API fallarán por CORS.

---

## 3. Archivos a Subir al Servidor

```
/opt/lago-reservas/
├── docker-compose.prod.yml    # Orquestación de contenedores
├── .env                       # Variables de entorno (crear en servidor)
└── Dockerfile                 # (incluido en el build)
```

### Subir archivos:

```bash
scp docker-compose.prod.yml Dockerfile usuario@servidor:/opt/lago-reservas/
```

---

## 4. Despliegue

```bash
# En el servidor
cd /opt/lago-reservas

# Construir y levantar servicios
docker compose -f docker-compose.prod.yml up -d --build

# Verificar que estén corriendo
docker compose -f docker-compose.prod.yml ps

# Ver logs en tiempo real
docker compose -f docker-compose.prod.yml logs -f backend
```

---

## 5. Verificación del Despliegue

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Respuesta esperada:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Probar endpoint público

```bash
curl http://localhost:8080/api/availability
```

---

## 6. URLs del API en Producción

| Endpoint | Método | Descripción | Autenticación |
|----------|--------|-------------|---------------|
| `/api/auth/login` | POST | Obtener JWT token | No |
| `/api/availability` | GET | Consultar disponibilidad | No |
| `/api/reservations` | POST | Crear reserva | No |
| `/api/admin/reservations` | GET | Listar reservas | JWT (Admin) |
| `/api/admin/reservations/{id}/confirm` | POST | Confirmar reserva | JWT (Admin) |
| `/api/admin/reservations/{id}/cancel` | POST | Cancelar reserva | JWT (Admin) |
| `/api/admin/reservations/export` | GET | Exportar a Excel | JWT (Admin) |
| `/actuator/health` | GET | Estado del servicio | No |

> **Nota:** En producción, Swagger UI (`/docs`) está **deshabilitado** por seguridad.

---

## 7. Configuración SSL con Nginx

### Opción A: Nginx incluido en Docker Compose

El `docker-compose.prod.yml` incluye un servicio nginx opcional. Descomentar y configurar certificados.

### Opción B: Nginx externo

```nginx
server {
    listen 80;
    server_name api.tudominio.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.tudominio.com;

    ssl_certificate /etc/letsencrypt/live/api.tudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.tudominio.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Obtener certificado SSL gratuito:

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.tudominio.com
```

---

## 8. Backups Automáticos

### Script de backup (`/opt/lago-reservas/backup.sh`):

```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/opt/lago-reservas/backups

# Crear backup
docker exec lago-postgres pg_dump -U lago_user lago_db > $BACKUP_DIR/backup_$DATE.sql

# Comprimir
gzip $BACKUP_DIR/backup_$DATE.sql

# Eliminar backups mayores a 7 días
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completado: backup_$DATE.sql.gz"
```

### Programar en cron (diario a las 3am):

```bash
chmod +x /opt/lago-reservas/backup.sh
crontab -e
# Agregar línea:
0 3 * * * /opt/lago-reservas/backup.sh
```

---

## 9. Comandos de Mantenimiento

| Acción | Comando |
|--------|---------|
| Ver logs | `docker compose -f docker-compose.prod.yml logs -f backend` |
| Reiniciar backend | `docker compose -f docker-compose.prod.yml restart backend` |
| Reiniciar todo | `docker compose -f docker-compose.prod.yml restart` |
| Parar servicios | `docker compose -f docker-compose.prod.yml down` |
| Actualizar imagen | `docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d` |
| Ver uso de recursos | `docker stats` |
| Backup manual | `docker exec lago-postgres pg_dump -U lago_user lago_db > backup.sql` |
| Acceder a DB | `docker exec -it lago-postgres psql -U lago_user lago_db` |

---

## 10. Checklist Pre-Despliegue

- [ ] Servidor con Docker instalado
- [ ] Puertos abiertos en firewall (80, 443)
- [ ] Generar `JWT_SECRET` seguro (mínimo 64 caracteres)
- [ ] Configurar contraseña segura para base de datos
- [ ] Configurar credenciales de admin
- [ ] Agregar dominios del frontend en `ALLOWED_ORIGINS`
- [ ] Certificado SSL configurado
- [ ] DNS apuntando al servidor
- [ ] Backups automáticos configurados
- [ ] Health check respondiendo OK

---

## 11. Información para el Frontend

### URL Base del API

```
Producción: https://api.tudominio.com
Desarrollo: http://localhost:8080
```

### Headers requeridos

```javascript
// Para endpoints públicos
headers: {
  'Content-Type': 'application/json'
}

// Para endpoints de admin
headers: {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <jwt_token>'
}
```

### Obtener token JWT

```javascript
const response = await fetch('https://api.tudominio.com/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'admin@tudominio.com',
    password: 'tu_contraseña'
  })
});

const { token } = await response.json();
// Guardar token para usar en requests autenticados
```

### Ejemplo: Consultar disponibilidad

```javascript
const response = await fetch('https://api.tudominio.com/api/availability?month=2025-01');
const availability = await response.json();
```

### Ejemplo: Crear reserva

```javascript
const response = await fetch('https://api.tudominio.com/api/reservations', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    visitDate: '2025-01-15',
    firstName: 'Juan',
    lastName: 'Pérez',
    dni: '12345678',
    phone: '+5493512345678',
    email: 'juan@email.com',
    circuit: 'A',
    visitorType: 'INDIVIDUAL',
    adults18Plus: 2,
    children2To17: 1,
    babiesLessThan2: 0,
    reducedMobility: 0,
    acceptedPolicies: true
  })
});
```

---

## 12. Troubleshooting

### El backend no inicia

```bash
# Ver logs detallados
docker compose -f docker-compose.prod.yml logs backend

# Verificar que PostgreSQL esté listo
docker compose -f docker-compose.prod.yml logs postgres
```

### Error de conexión a base de datos

```bash
# Verificar que el contenedor de postgres esté corriendo
docker ps | grep postgres

# Probar conexión manual
docker exec -it lago-postgres psql -U lago_user -d lago_db -c "SELECT 1"
```

### Error CORS desde el frontend

Verificar que `ALLOWED_ORIGINS` en `.env` incluya el dominio exacto del frontend (incluyendo protocolo https://).

### Puerto 8080 ocupado

```bash
# Ver qué proceso usa el puerto
sudo lsof -i :8080

# Cambiar puerto en docker-compose.prod.yml
ports:
  - "8081:8080"  # Usar 8081 externamente
```

---

## Contacto

Para problemas con el despliegue, contactar al equipo de backend.

---

*Documento generado el: Diciembre 2024*
