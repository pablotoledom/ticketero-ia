# 📡 Documentación de API - Ticketero

> **Documentación completa de la API REST para gestión de tickets bancarios**

---

## 📁 **Contenido de este Directorio**

| Archivo | Descripción | Audiencia |
|---------|-------------|-----------|
| [`openapi.yaml`](openapi.yaml) | Especificación OpenAPI 3.0 completa | Desarrolladores/Integradores |
| [`postman-collection.json`](postman-collection.json) | Collection de Postman con tests | QA/Desarrolladores |
| [`integration-guide.md`](integration-guide.md) | Guía de integración detallada | Desarrolladores Frontend |

---

## 🚀 **Quick Start**

### **1. Explorar API**
```bash
# Importar OpenAPI spec en tu herramienta favorita
# - Swagger UI: http://localhost:8080/swagger-ui.html
# - Postman: Import > openapi.yaml
# - Insomnia: Import > openapi.yaml
```

### **2. Testing con Postman**
```bash
# 1. Importar collection
# 2. Configurar baseUrl = http://localhost:8080
# 3. Ejecutar "🧪 Test Scenarios"
```

### **3. Integración**
Ver [`integration-guide.md`](integration-guide.md) para ejemplos en:
- JavaScript/Node.js
- Python
- Java/Spring Boot

---

## 🎯 **Endpoints Principales**

### **🎫 Gestión de Tickets**
- `POST /api/tickets` - Crear ticket
- `GET /api/tickets/{uuid}` - Consultar por UUID
- `GET /api/tickets/{numero}/position` - Posición en cola

### **👨💼 Administración**
- `GET /api/admin/dashboard` - Dashboard general
- `GET /api/admin/queues/{type}` - Estado de cola específica

### **🏥 Monitoreo**
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Métricas

---

## 📊 **Especificaciones Técnicas**

### **Formato de Datos**
- **Content-Type**: `application/json`
- **Charset**: UTF-8
- **Date Format**: ISO 8601

### **Códigos de Respuesta**
- `200` - OK (consultas)
- `201` - Created (recursos nuevos)
- `400` - Bad Request (validación)
- `404` - Not Found
- `500` - Internal Error

### **Rate Limiting**
- **Desarrollo**: Sin límites
- **Producción**: 100 req/min por IP

---

## 🔧 **Herramientas Recomendadas**

### **Exploración de API**
- **Swagger UI**: Interfaz web interactiva
- **Postman**: Testing y documentación
- **Insomnia**: Cliente REST alternativo

### **Generación de Código**
```bash
# Generar cliente JavaScript
npx @openapitools/openapi-generator-cli generate \
  -i openapi.yaml \
  -g javascript \
  -o ./generated-client

# Generar cliente Python
openapi-generator generate \
  -i openapi.yaml \
  -g python \
  -o ./python-client
```

---

## 🧪 **Testing Automatizado**

### **Postman Tests**
La collection incluye:
- ✅ Tests de validación de respuesta
- ✅ Tests de performance (< 1s)
- ✅ Tests de flujo completo
- ✅ Tests de casos de error

### **Ejecutar Tests**
```bash
# Con Newman (CLI)
npm install -g newman
newman run postman-collection.json \
  --environment postman-environment.json
```

---

## 📈 **Métricas de API**

### **Performance**
- **Tiempo de Respuesta**: < 200ms (p95)
- **Throughput**: > 100 req/s
- **Disponibilidad**: 99.9%

### **Monitoreo**
```bash
# Métricas en tiempo real
curl http://localhost:8080/actuator/prometheus | grep api_

# Health check
curl http://localhost:8080/actuator/health
```

---

## 🔗 **Enlaces Relacionados**

- **README Principal**: [`../../README.md`](../../README.md)
- **Arquitectura**: [`../ARCHITECTURE.md`](../ARCHITECTURE.md)
- **Deployment**: [`../DEPLOYMENT.md`](../DEPLOYMENT.md)
- **ADRs**: [`../decisions/`](../decisions/)

---

## 📞 **Soporte**

### **Documentación**
- **Swagger UI**: http://localhost:8080/swagger-ui.html (cuando esté disponible)
- **Postman Workspace**: [Link al workspace público]

### **Contacto**
- **Issues**: GitHub Issues
- **Email**: dev@ticketero.com
- **Slack**: #ticketero-api

---

**Mantenido por:** Equipo de API  
**Última actualización:** 2024-11-25  
**Versión API:** v1.0.0