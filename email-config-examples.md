# Configuración de Email para Lago Escondido

## 🚀 Configuraciones Recomendadas

### 1. **Desarrollo Local (MailHog)**
```yaml
# application.yml - Ya configurado
spring:
  mail:
    host: localhost
    port: 1025
    username: ""
    password: ""
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
```

**Para usar MailHog:**
```bash
# Instalar MailHog
go install github.com/mailhog/MailHog@latest

# Ejecutar MailHog
MailHog
```

**Acceder a MailHog:** `http://localhost:8025`

### 2. **Gmail SMTP (Recomendado para desarrollo)**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: tu-email@gmail.com
    password: tu-app-password  # NO tu contraseña normal
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Pasos para configurar Gmail:**
1. Activar verificación en 2 pasos
2. Generar una "Contraseña de aplicación"
3. Usar esa contraseña en `MAIL_PASSWORD`

### 3. **Variables de Entorno**
```bash
# Gmail
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=tu-email@gmail.com
export MAIL_PASSWORD=tu-app-password
export MAIL_SMTP_AUTH=true
export MAIL_SMTP_STARTTLS=true

# Configuración de la aplicación
export APP_MAIL_FROM=tu-email@gmail.com
export APP_MAIL_FROM_NAME="Lago Escondido"
export APP_MAIL_ENABLED=true
```

### 4. **Producción (SendGrid)**
```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: tu-sendgrid-api-key
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### 5. **Producción (Amazon SES)**
```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: tu-ses-smtp-username
    password: tu-ses-smtp-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## 📧 Templates Disponibles

- **Confirmación de Reserva:** `reservation-confirmation.html`
- **Cancelación de Reserva:** `reservation-cancellation.html`

## 🔧 Comandos Útiles

```bash
# Reiniciar backend con nueva configuración
cd backend-reservas-lago
./mvnw clean compile spring-boot:run

# Ver logs de email
tail -f logs/application.log | grep -i mail

# Probar email manualmente
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "visitDate": "2025-10-20",
    "firstName": "Test",
    "lastName": "Email",
    "dni": "12345678",
    "phone": "+54 9 351 000-0000",
    "email": "test@example.com",
    "circuit": "A",
    "visitorType": "INDIVIDUAL",
    "adults14Plus": 1,
    "minors": 0,
    "reducedMobility": 0,
    "allergies": false,
    "comment": "Test email",
    "originLocation": "Córdoba, AR",
    "howHeard": "WEBSITE",
    "acceptedPolicies": true
  }'
```

## 🚨 Troubleshooting

### Error: "Authentication failed"
- Verificar credenciales
- Para Gmail: usar contraseña de aplicación
- Verificar que 2FA esté activado

### Error: "Connection refused"
- Verificar host y puerto
- Verificar firewall
- Para Gmail: verificar configuración de seguridad

### Email no llega
- Revisar carpeta de spam
- Verificar que `APP_MAIL_ENABLED=true`
- Revisar logs del servidor
