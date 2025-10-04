# 🚀 Backend Lago Escondido - Guía de Producción

Este documento explica cómo desplegar el backend del sistema de reservas del Lago Escondido en un entorno de producción.

## 📋 Prerrequisitos

- Docker y Docker Compose instalados
- Servidor Linux (Ubuntu 20.04+ recomendado)
- Certificados SSL (para HTTPS)
- Dominio configurado
- Servicio de email configurado

## 🔧 Configuración Inicial

### 1. Clonar el repositorio
```bash
git clone <tu-repositorio>
cd backend-reservas-lago
```

### 2. Configurar variables de entorno
```bash
# Copiar archivo de ejemplo
cp env.prod.example .env.prod

# Editar con tus valores reales
nano .env.prod
```

### 3. Variables críticas a configurar:

#### Base de Datos
```bash
DATABASE_PASSWORD=tu_password_super_seguro
DATABASE_USERNAME=lago_user
```

#### Administración
```bash
ADMIN_USERNAME=admin
ADMIN_PASSWORD=tu_password_admin_muy_seguro
```

#### Email (Gmail ejemplo)
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu_app_password_de_gmail
APP_MAIL_FROM=reservas@lago-escondido.com
```

#### CORS
```bash
ALLOWED_ORIGINS=https://lago-escondido.com,https://www.lago-escondido.com
```

## 🚀 Despliegue

### Despliegue automático (recomendado)
```bash
./deploy.sh
```

### Despliegue manual
```bash
# 1. Construir imagen
docker-compose -f docker-compose.prod.yml build

# 2. Iniciar servicios
docker-compose -f docker-compose.prod.yml up -d

# 3. Verificar estado
docker-compose -f docker-compose.prod.yml ps
```

## 🔍 Verificación

### Health Checks
```bash
# Verificar aplicación
curl http://localhost:8080/actuator/health

# Verificar base de datos
docker-compose -f docker-compose.prod.yml exec postgres pg_isready -U lago_user -d lago_prod
```

### Logs
```bash
# Ver logs de la aplicación
docker-compose -f docker-compose.prod.yml logs -f app

# Ver logs de la base de datos
docker-compose -f docker-compose.prod.yml logs -f postgres
```

## 📊 Monitoreo

### Endpoints de monitoreo disponibles:
- `GET /actuator/health` - Estado de salud
- `GET /actuator/info` - Información de la aplicación
- `GET /actuator/metrics` - Métricas de la aplicación

### Logs importantes:
- Aplicación: `/var/log/lago-escondido/application.log`
- Nginx: `/var/log/nginx/lago-access.log`

## 🔒 Seguridad

### Configuraciones implementadas:
- ✅ Usuario no-root en contenedores
- ✅ Headers de seguridad en Nginx
- ✅ Rate limiting en API
- ✅ CORS configurado
- ✅ SSL/TLS habilitado
- ✅ Passwords seguros requeridos

### Recomendaciones adicionales:
- Usar un firewall (UFW)
- Configurar fail2ban
- Mantener certificados SSL actualizados
- Monitorear logs de seguridad

## 🛠️ Mantenimiento

### Actualizaciones
```bash
# 1. Hacer backup de la base de datos
docker-compose -f docker-compose.prod.yml exec postgres pg_dump -U lago_user lago_prod > backup.sql

# 2. Actualizar código
git pull origin main

# 3. Redesplegar
./deploy.sh
```

### Backup de base de datos
```bash
# Backup completo
docker-compose -f docker-compose.prod.yml exec postgres pg_dump -U lago_user lago_prod > backup_$(date +%Y%m%d_%H%M%S).sql

# Restaurar backup
docker-compose -f docker-compose.prod.yml exec -T postgres psql -U lago_user lago_prod < backup.sql
```

### Limpieza de logs
```bash
# Limpiar logs antiguos (más de 30 días)
find /var/log/lago-escondido -name "*.log" -mtime +30 -delete
```

## 🔧 Configuración de Email

### Gmail (desarrollo/pruebas)
1. Habilitar verificación en 2 pasos
2. Generar "App Password"
3. Usar el App Password como `MAIL_PASSWORD`

### Servicios profesionales (recomendado para producción)
- **SendGrid**: `MAIL_HOST=smtp.sendgrid.net`, `MAIL_PORT=587`
- **Mailgun**: `MAIL_HOST=smtp.mailgun.org`, `MAIL_PORT=587`
- **Amazon SES**: Configurar según región

## 🌐 Configuración de Dominio

### DNS
```
A    lago-escondido.com      -> IP_DEL_SERVIDOR
A    www.lago-escondido.com  -> IP_DEL_SERVIDOR
CNAME api.lago-escondido.com -> lago-escondido.com
```

### Certificados SSL
```bash
# Usar Let's Encrypt (gratuito)
sudo certbot --nginx -d lago-escondido.com -d www.lago-escondido.com
```

## 📞 Soporte

### Comandos de diagnóstico
```bash
# Estado de contenedores
docker-compose -f docker-compose.prod.yml ps

# Uso de recursos
docker stats

# Espacio en disco
df -h

# Memoria
free -h
```

### Logs de errores comunes
```bash
# Error de conexión a BD
docker-compose -f docker-compose.prod.yml logs postgres | grep ERROR

# Error de email
docker-compose -f docker-compose.prod.yml logs app | grep -i mail

# Error de memoria
docker-compose -f docker-compose.prod.yml logs app | grep -i memory
```

## 🚨 Troubleshooting

### Problema: Aplicación no inicia
```bash
# Verificar logs
docker-compose -f docker-compose.prod.yml logs app

# Verificar variables de entorno
docker-compose -f docker-compose.prod.yml config
```

### Problema: Base de datos no conecta
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose -f docker-compose.prod.yml exec postgres pg_isready

# Verificar credenciales
docker-compose -f docker-compose.prod.yml exec postgres psql -U lago_user -d lago_prod -c "SELECT 1;"
```

### Problema: Emails no se envían
```bash
# Verificar configuración de email
docker-compose -f docker-compose.prod.yml logs app | grep -i mail

# Probar conectividad SMTP
telnet smtp.gmail.com 587
```

---

## 📝 Notas Importantes

1. **NUNCA** subas el archivo `.env.prod` al repositorio
2. **SIEMPRE** haz backup antes de actualizaciones
3. **MONITORA** los logs regularmente
4. **ACTUALIZA** las dependencias periódicamente
5. **TESTEA** en un entorno de staging antes de producción

¡Tu backend está listo para producción! 🎉
