# Configuración de Certificados SSL

## ¿Qué son los certificados SSL?

Los certificados SSL permiten que tu API use HTTPS (conexión segura) en lugar de HTTP.

## Opción 1: Certificados Let's Encrypt (GRATIS y RECOMENDADO)

### Para nginx DENTRO de Docker:

```bash
# 1. Crear directorio SSL
mkdir -p ssl

# 2. Instalar certbot en el servidor (fuera de Docker)
sudo apt update
sudo apt install certbot

# 3. Obtener certificado (reemplaza tu-dominio.com)
sudo certbot certonly --standalone -d api.tu-dominio.com

# 4. Copiar certificados al directorio ssl/
sudo cp /etc/letsencrypt/live/api.tu-dominio.com/fullchain.pem ssl/
sudo cp /etc/letsencrypt/live/api.tu-dominio.com/privkey.pem ssl/
sudo chmod 644 ssl/*.pem

# 5. Levantar Docker con nginx
docker compose -f docker-compose.prod.yml up -d
```

### Renovación automática (certificados expiran cada 90 días):

```bash
# Agregar a cron para renovar automáticamente
sudo crontab -e

# Agregar esta línea (renueva diariamente a las 2am):
0 2 * * * certbot renew --quiet && cp /etc/letsencrypt/live/api.tu-dominio.com/*.pem /opt/lago-reservas/ssl/ && docker compose -f /opt/lago-reservas/docker-compose.prod.yml restart nginx
```

---

## Opción 2: Sin nginx en Docker (nginx externo)

Si prefieres usar nginx instalado directamente en el servidor (sin Docker):

### 1. Comentar servicio nginx en docker-compose.prod.yml:

```yaml
# Comentar o eliminar estas líneas (79-93):
#  nginx:
#    image: nginx:alpine
#    ...
```

### 2. Instalar nginx en el servidor:

```bash
sudo apt update
sudo apt install nginx certbot python3-certbot-nginx
```

### 3. Crear configuración nginx externa:

```bash
sudo nano /etc/nginx/sites-available/lago-reservas
```

Contenido:

```nginx
server {
    listen 80;
    server_name api.tu-dominio.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 4. Activar y obtener certificado:

```bash
# Activar sitio
sudo ln -s /etc/nginx/sites-available/lago-reservas /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# Obtener certificado (certbot configurará HTTPS automáticamente)
sudo certbot --nginx -d api.tu-dominio.com

# Renovación automática ya está configurada por certbot
```

---

## Opción 3: Certificado autofirmado (SOLO PARA PRUEBAS)

**⚠️ NO usar en producción - Los navegadores mostrarán advertencias de seguridad**

```bash
# Crear directorio
mkdir -p ssl

# Generar certificado autofirmado
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ssl/privkey.pem \
  -out ssl/fullchain.pem \
  -subj "/CN=localhost"

# Levantar Docker
docker compose -f docker-compose.prod.yml up -d
```

---

## Opción 4: Desarrollo sin HTTPS

Para desarrollo local, puedes desactivar nginx temporalmente:

### 1. Comentar servicio nginx en docker-compose.prod.yml

### 2. Exponer puerto 8080 directamente:

```yaml
services:
  app:
    ports:
      - "8080:8080"  # Acceso directo sin nginx
```

### 3. Usar HTTP en el frontend:

```
API_URL=http://localhost:8080
```

**⚠️ Esto es SOLO para desarrollo local, NUNCA en producción**

---

## Verificación

Una vez configurado SSL, verifica:

```bash
# Debe retornar 200 OK
curl -I https://api.tu-dominio.com/actuator/health

# Verificar certificado
openssl s_client -connect api.tu-dominio.com:443 -servername api.tu-dominio.com
```

---

## Recomendación

- **Producción:** Opción 1 (Let's Encrypt con Docker) o Opción 2 (nginx externo)
- **Desarrollo local:** Opción 4 (sin HTTPS)
- **Testing:** Opción 3 (autofirmado)

---

## Configuración DNS

No olvides apuntar tu dominio al servidor:

```
Tipo: A
Nombre: api
Valor: <IP_DEL_SERVIDOR>
TTL: 3600
```

Espera 5-10 minutos para que se propague.
