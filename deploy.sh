#!/bin/bash

# ============================================
# SCRIPT DE DESPLIEGUE PARA PRODUCCIÓN
# ============================================

set -e  # Salir si cualquier comando falla

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    error "Este script debe ejecutarse desde el directorio raíz del proyecto backend"
fi

log "🚀 Iniciando despliegue del backend de Lago Escondido..."

# Verificar que Docker esté instalado y ejecutándose
if ! command -v docker &> /dev/null; then
    error "Docker no está instalado. Por favor instálalo primero."
fi

if ! docker info &> /dev/null; then
    error "Docker no está ejecutándose. Por favor inicia Docker."
fi

# Verificar que Docker Compose esté disponible
if ! command -v docker-compose &> /dev/null; then
    error "Docker Compose no está instalado. Por favor instálalo primero."
fi

# Verificar archivo de variables de entorno
if [ ! -f ".env.prod" ]; then
    warning "Archivo .env.prod no encontrado. Creando desde ejemplo..."
    if [ -f "env.prod.example" ]; then
        cp env.prod.example .env.prod
        warning "Por favor edita .env.prod con tus valores reales antes de continuar"
        exit 1
    else
        error "Archivo env.prod.example no encontrado"
    fi
fi

# Cargar variables de entorno
log "📋 Cargando variables de entorno..."
source .env.prod

# Verificar variables críticas
if [ -z "$DATABASE_PASSWORD" ] || [ -z "$ADMIN_PASSWORD" ]; then
    error "Variables críticas DATABASE_PASSWORD y ADMIN_PASSWORD deben estar configuradas en .env.prod"
fi

# Detener contenedores existentes
log "🛑 Deteniendo contenedores existentes..."
docker-compose -f docker-compose.prod.yml down --remove-orphans || true

# Limpiar imágenes antiguas (opcional)
log "🧹 Limpiando imágenes antiguas..."
docker image prune -f || true

# Construir nueva imagen
log "🔨 Construyendo nueva imagen de la aplicación..."
docker-compose -f docker-compose.prod.yml build --no-cache

# Iniciar servicios
log "🚀 Iniciando servicios..."
docker-compose -f docker-compose.prod.yml up -d

# Esperar a que los servicios estén listos
log "⏳ Esperando a que los servicios estén listos..."
sleep 30

# Verificar estado de los servicios
log "🔍 Verificando estado de los servicios..."

# Verificar PostgreSQL
if docker-compose -f docker-compose.prod.yml exec postgres pg_isready -U lago_user -d lago_prod; then
    success "PostgreSQL está listo"
else
    error "PostgreSQL no está respondiendo"
fi

# Verificar aplicación Spring Boot
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    success "Aplicación Spring Boot está lista"
else
    error "Aplicación Spring Boot no está respondiendo"
fi

# Verificar logs de la aplicación
log "📊 Mostrando logs de la aplicación..."
docker-compose -f docker-compose.prod.yml logs --tail=20 app

# Mostrar información del despliegue
log "🎉 Despliegue completado exitosamente!"
echo ""
echo "📍 Información del despliegue:"
echo "   • API: http://localhost:8080/api"
echo "   • Health Check: http://localhost:8080/actuator/health"
echo "   • Base de datos: PostgreSQL en puerto 5432"
echo ""
echo "🔧 Comandos útiles:"
echo "   • Ver logs: docker-compose -f docker-compose.prod.yml logs -f"
echo "   • Detener: docker-compose -f docker-compose.prod.yml down"
echo "   • Reiniciar: docker-compose -f docker-compose.prod.yml restart"
echo "   • Estado: docker-compose -f docker-compose.prod.yml ps"
echo ""
success "¡El backend está listo para producción! 🚀"


