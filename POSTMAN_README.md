# 📋 Guía de Uso - Colección Postman

## 🚀 Configuración Inicial

### 1. Importar la colección
- Abrir Postman
- Click en **Import**
- Seleccionar `postman_collection.json`
- Seleccionar `postman_environment_local.json`

### 2. Configurar el environment
- Seleccionar el environment **"Local"** en el dropdown superior derecho
- Verificar que `baseUrl` sea `http://localhost:8080`

### 3. Obtener el token de autenticación
1. Ir a **Auth → Login (JWT)**
2. Ejecutar el request
3. Copiar el token de la respuesta
4. Pegarlo en la variable `authToken` del environment

---

## 📊 **EXPORTACIÓN EXCEL - Casos de Prueba**

### **Carpeta: Admin → Exportación Excel**

Esta carpeta contiene **10 casos de prueba** para probar la funcionalidad de exportación a Excel con diferentes filtros.

#### **1. Exportar - Todas las reservas**
```
GET /api/admin/reservations/export
```
- Exporta **TODAS** las reservas del sistema
- ⚠️ **CUIDADO:** Si hay más de 10,000 registros, retornará error
- Útil para: Backup completo, análisis general

#### **2. Exportar - Por fecha específica**
```
GET /api/admin/reservations/export?date=2025-12-15
```
- Exporta solo las reservas del **15 de diciembre de 2025**
- Formato de fecha: `YYYY-MM-DD`
- Útil para: Reportes diarios, control de aforo

#### **3. Exportar - Por mes**
```
GET /api/admin/reservations/export?month=2025-12
```
- Exporta todas las reservas de **diciembre 2025**
- Formato: `YYYY-MM`
- Útil para: Reportes mensuales, estadísticas

#### **4. Exportar - Por año**
```
GET /api/admin/reservations/export?year=2025
```
- Exporta todas las reservas del **año 2025**
- Formato: `YYYY`
- Útil para: Reportes anuales, cierre de gestión

#### **5. Exportar - Por DNI**
```
GET /api/admin/reservations/export?dni=12345678
```
- Busca el DNI en **titulares Y visitantes**
- Normaliza automáticamente (quita puntos y guiones)
- Útil para: Historial de un visitante específico

#### **6. Exportar - Por nombre** ⭐ NUEVO
```
GET /api/admin/reservations/export?name=García
```
- Busca en **nombre y apellido** de titulares Y visitantes
- **Case-insensitive** (no importa mayúsculas/minúsculas)
- Busca **coincidencias parciales** ("Gar" encuentra "García")
- Útil para: Buscar reservas de una familia o grupo

#### **7. Exportar - Por estado**
```
GET /api/admin/reservations/export?status=CONFIRMED
```
- Valores válidos:
  - `PENDING` - Pendientes de confirmación
  - `CONFIRMED` - Confirmadas
  - `CANCELLED` - Canceladas
- Útil para: Control de reservas pendientes, lista de confirmados

#### **8. Exportar - Por tipo de visitante**
```
GET /api/admin/reservations/export?visitorType=EDUCATIONAL_INSTITUTION
```
- Valores válidos:
  - `INDIVIDUAL` - Visitantes particulares
  - `EDUCATIONAL_INSTITUTION` - Instituciones educativas
  - `EVENT` - Eventos especiales
- Útil para: Reportes por segmento

#### **9. Exportar - Combinación de filtros**
```
GET /api/admin/reservations/export?date=2025-12-15&status=CONFIRMED&name=García&dni=12345678
```
- Combina **MÚLTIPLES filtros**
- Los resultados deben cumplir **TODOS** los criterios (AND, no OR)
- Útil para: Búsquedas específicas

#### **10. Exportar - Filtros completos**
```
GET /api/admin/reservations/export?month=2025-12&status=PENDING&visitorType=INDIVIDUAL&dni=30111222&name=Juan
```
- Ejemplo con **TODOS los filtros disponibles**
- Útil para: Casos de prueba exhaustivos

---

## 📝 **Formato del Archivo Excel**

### Características del archivo generado:

| Característica | Detalle |
|----------------|---------|
| **Formato** | XLSX (Excel 2007+) |
| **Fecha de visita** | DD/MM/YYYY (ej: 15/12/2025) |
| **Fecha de creación** | DD/MM/YYYY (sin horario) |
| **Rol** | "Titular" o "Visitante" |
| **Colores** | Titulares en verde claro, Visitantes en amarillo |
| **Columnas** | 16 columnas de información |
| **Visitantes** | Se muestran debajo del titular |

### Columnas incluidas:
1. Rol
2. Fecha de visita
3. Estado
4. Nombre
5. Apellido
6. DNI
7. Email
8. Teléfono
9. Tipo de visitante
10. Circuito
11. Procedencia
12. Adultos 18+
13. Menores 2-17
14. Bebés <2
15. Movilidad reducida
16. Creada

---

## ⚠️ **Limitaciones y Seguridad**

### Límite de registros
- **Máximo:** 10,000 registros por exportación
- Si excedes el límite, recibirás:
  ```json
  {
    "status": 400,
    "message": "Demasiados registros (15234). Límite: 10,000. Aplique más filtros."
  }
  ```
- **Solución:** Aplicar más filtros (fecha, mes, estado, etc.)

### Autenticación
- Todos los endpoints requieren **JWT Bearer Token**
- Token válido por **24 horas**
- Si expira, ejecutar nuevamente **Auth → Login (JWT)**

---

## 🎯 **Casos de Uso Reales**

### **Caso 1: Reporte diario para coordinación**
```
GET /api/admin/reservations/export?date=2025-12-15&status=CONFIRMED
```
→ Lista de confirmados para mañana

### **Caso 2: Buscar todas las reservas de una familia**
```
GET /api/admin/reservations/export?name=Fernández
```
→ Todas las reservas con "Fernández" en nombre/apellido

### **Caso 3: Exportar pendientes de instituciones educativas**
```
GET /api/admin/reservations/export?status=PENDING&visitorType=EDUCATIONAL_INSTITUTION
```
→ Instituciones que esperan confirmación

### **Caso 4: Historial completo de un visitante**
```
GET /api/admin/reservations/export?dni=30111222
```
→ Todas las visitas (pasadas y futuras) de esa persona

### **Caso 5: Reporte mensual**
```
GET /api/admin/reservations/export?month=2025-12
```
→ Todas las reservas de diciembre para análisis

---

## 🐛 **Solución de Problemas**

### Error 401 - No autenticado
- **Causa:** Token inválido o expirado
- **Solución:** Ejecutar **Auth → Login (JWT)** y actualizar el token

### Error 403 - Sin permisos
- **Causa:** El usuario no tiene rol ADMIN
- **Solución:** Verificar que el usuario sea administrador

### Error 400 - Demasiados registros
- **Causa:** Más de 10,000 registros coinciden con los filtros
- **Solución:** Agregar más filtros (fecha, estado, etc.)

### No se descarga el archivo
- **Causa:** Postman no guarda archivos binarios por defecto
- **Solución:**
  1. Hacer click en **Send and Download**
  2. O copiar la URL y abrirla en el navegador

---

## 📚 **Parámetros de Query - Referencia Rápida**

| Parámetro | Tipo | Formato | Ejemplo | Descripción |
|-----------|------|---------|---------|-------------|
| `date` | String | YYYY-MM-DD | `2025-12-15` | Fecha exacta de visita |
| `month` | String | YYYY-MM | `2025-12` | Mes completo |
| `year` | Integer | YYYY | `2025` | Año completo |
| `status` | Enum | - | `CONFIRMED` | PENDING, CONFIRMED, CANCELLED |
| `visitorType` | Enum | - | `INDIVIDUAL` | INDIVIDUAL, EDUCATIONAL_INSTITUTION, EVENT |
| `dni` | String | Sin puntos | `12345678` | DNI del titular o visitante |
| `name` | String | - | `García` | Nombre o apellido (case-insensitive) |

---

## ✅ **Verificación Post-Exportación**

Después de exportar, verifica:

1. ✅ **Nombre del archivo** sigue el patrón:
   - Con fecha: `reservas_2025-12-15.xlsx`
   - Con mes: `reservas_2025-12.xlsx`
   - Con año: `reservas_2025.xlsx`
   - Sin filtros: `reservas.xlsx`

2. ✅ **Formato de fechas** en el Excel:
   - Fecha de visita: `15/12/2025`
   - Fecha de creación: `09/12/2025`

3. ✅ **Texto "Visitante"** (no "Acompañante")

4. ✅ **Colores:**
   - Titulares: Fondo verde claro
   - Visitantes: Fondo amarillo

5. ✅ **Cantidad de registros** coincide con los filtros aplicados

---

## 🔄 **Actualización de la Colección**

**Última actualización:** 09/12/2025

**Cambios recientes:**
- ✅ Agregado parámetro `name` para búsqueda por nombre/apellido
- ✅ Optimización de consultas en base de datos
- ✅ Límite de seguridad de 10,000 registros
- ✅ Formato de fechas cambiado a DD/MM/YYYY
- ✅ Texto "Visitante" en lugar de "Acompañante"
- ✅ 10 casos de prueba documentados

---

## 📞 **Soporte**

Si encuentras algún problema:
1. Verificar que el backend esté corriendo en `http://localhost:8080`
2. Verificar que tengas un token válido
3. Revisar los logs del backend para más detalles
4. Consultar la documentación Swagger en `http://localhost:8080/swagger-ui.html`

---

**Desarrollado por:** Luis Muñoz
**Proyecto:** Sistema de Reservas Lago Escondido
