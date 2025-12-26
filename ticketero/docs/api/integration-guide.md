# 🔌 Guía de Integración - Ticketero API

> **Guía completa para integrar con la API de gestión de tickets bancarios**

---

## 📋 **Información General**

### **Base URL**
```
Desarrollo:  http://localhost:8080
Staging:     https://api-staging.ticketero.com
Producción:  https://api.ticketero.com
```

### **Formato de Datos**
- **Content-Type**: `application/json`
- **Charset**: UTF-8
- **Date Format**: ISO 8601 (`2024-11-25T10:30:00Z`)

### **Códigos de Respuesta**
| Código | Descripción | Uso |
|--------|-------------|-----|
| `200` | OK | Consultas exitosas |
| `201` | Created | Recursos creados |
| `400` | Bad Request | Datos inválidos |
| `404` | Not Found | Recurso no encontrado |
| `500` | Internal Error | Error del servidor |

---

## 🚀 **Quick Start**

### **1. Verificar Conectividad**
```bash
curl -X GET http://localhost:8080/actuator/health
```

**Respuesta esperada:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "rabbit": {"status": "UP"}
  }
}
```

### **2. Crear Primer Ticket**
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678",
    "telefono": "1234567890",
    "branchOffice": "Sucursal Centro",
    "queue": "CAJA"
  }'
```

**Respuesta:**
```json
{
  "identificador": "550e8400-e29b-41d4-a716-446655440000",
  "numero": "C01",
  "queue": "CAJA",
  "posicionEnCola": 1,
  "tiempoEstimado": "15 minutos",
  "mensaje": "Ticket creado exitosamente"
}
```

### **3. Consultar Ticket**
```bash
curl -X GET http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000
```

---

## 📚 **Endpoints Principales**

### **🎫 Crear Ticket**

**Endpoint:** `POST /api/tickets`

**Request Body:**
```json
{
  "nationalId": "12345678",      // Requerido: 8-12 dígitos
  "telefono": "1234567890",      // Opcional: 9-15 dígitos
  "branchOffice": "Sucursal Centro", // Requerido: nombre sucursal
  "queue": "CAJA"                // Requerido: CAJA|PLATAFORMA|EJECUTIVO
}
```

**Response (201):**
```json
{
  "identificador": "uuid-v4",
  "numero": "C01",
  "queue": "CAJA",
  "posicionEnCola": 1,
  "tiempoEstimado": "15 minutos",
  "mensaje": "Ticket creado exitosamente"
}
```

**Validaciones:**
- `nationalId`: Regex `^[0-9]{8,12}$`
- `telefono`: Regex `^[0-9]{9,15}$` (opcional)
- `branchOffice`: No vacío, máximo 100 caracteres
- `queue`: Enum válido

---

### **🔍 Consultar Ticket**

**Endpoint:** `GET /api/tickets/{uuid}`

**Path Parameters:**
- `uuid`: UUID v4 del ticket

**Response (200):**
```json
{
  "identificador": "550e8400-e29b-41d4-a716-446655440000",
  "numero": "C01",
  "nationalId": "12345678",
  "telefono": "1234567890",
  "branchOffice": "Sucursal Centro",
  "queue": "CAJA",
  "status": "CREATED",
  "posicionEnCola": 1,
  "tiempoEstimado": "15 minutos",
  "createdAt": "2024-11-25T10:30:00Z",
  "mensajes": [
    {
      "plantilla": "TICKET_CREATED",
      "estadoEnvio": "ENVIADO",
      "fechaProgramada": "2024-11-25T10:30:00Z",
      "fechaEnvio": "2024-11-25T10:30:05Z"
    }
  ]
}
```

---

### **📍 Posición en Cola**

**Endpoint:** `GET /api/tickets/{numero}/position`

**Path Parameters:**
- `numero`: Número del ticket (ej: C01, P02)

**Response (200):**
```json
{
  "numero": "C01",
  "posicionActual": 3,
  "tiempoEstimado": "12 minutos",
  "enAtencion": "C02"
}
```

---

## 🛠️ **Integración por Tecnología**

### **JavaScript/Node.js**

```javascript
const axios = require('axios');

class TicketeroClient {
  constructor(baseUrl = 'http://localhost:8080') {
    this.baseUrl = baseUrl;
    this.client = axios.create({
      baseURL: baseUrl,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  async createTicket(ticketData) {
    try {
      const response = await this.client.post('/api/tickets', ticketData);
      return response.data;
    } catch (error) {
      throw new Error(`Error creating ticket: ${error.response?.data?.mensaje || error.message}`);
    }
  }

  async getTicket(uuid) {
    try {
      const response = await this.client.get(`/api/tickets/${uuid}`);
      return response.data;
    } catch (error) {
      if (error.response?.status === 404) {
        return null;
      }
      throw new Error(`Error fetching ticket: ${error.response?.data?.mensaje || error.message}`);
    }
  }

  async getPosition(numero) {
    try {
      const response = await this.client.get(`/api/tickets/${numero}/position`);
      return response.data;
    } catch (error) {
      throw new Error(`Error fetching position: ${error.response?.data?.mensaje || error.message}`);
    }
  }
}

// Uso
const client = new TicketeroClient();

async function example() {
  // Crear ticket
  const ticket = await client.createTicket({
    nationalId: '12345678',
    telefono: '1234567890',
    branchOffice: 'Sucursal Centro',
    queue: 'CAJA'
  });
  
  console.log('Ticket creado:', ticket);
  
  // Consultar ticket
  const details = await client.getTicket(ticket.identificador);
  console.log('Detalles:', details);
  
  // Obtener posición
  const position = await client.getPosition(ticket.numero);
  console.log('Posición:', position);
}
```

### **Python**

```python
import requests
from typing import Optional, Dict, Any

class TicketeroClient:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json'
        })
    
    def create_ticket(self, ticket_data: Dict[str, Any]) -> Dict[str, Any]:
        """Crear un nuevo ticket"""
        response = self.session.post(
            f"{self.base_url}/api/tickets",
            json=ticket_data
        )
        response.raise_for_status()
        return response.json()
    
    def get_ticket(self, uuid: str) -> Optional[Dict[str, Any]]:
        """Obtener ticket por UUID"""
        response = self.session.get(f"{self.base_url}/api/tickets/{uuid}")
        
        if response.status_code == 404:
            return None
        
        response.raise_for_status()
        return response.json()
    
    def get_position(self, numero: str) -> Dict[str, Any]:
        """Obtener posición en cola"""
        response = self.session.get(f"{self.base_url}/api/tickets/{numero}/position")
        response.raise_for_status()
        return response.json()

# Uso
client = TicketeroClient()

# Crear ticket
ticket = client.create_ticket({
    'nationalId': '12345678',
    'telefono': '1234567890',
    'branchOffice': 'Sucursal Centro',
    'queue': 'CAJA'
})

print(f"Ticket creado: {ticket}")

# Consultar detalles
details = client.get_ticket(ticket['identificador'])
print(f"Detalles: {details}")
```

### **Java/Spring Boot**

```java
@Service
@RequiredArgsConstructor
public class TicketeroClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${ticketero.api.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public TicketResponse createTicket(TicketRequest request) {
        String url = baseUrl + "/api/tickets";
        
        try {
            return restTemplate.postForObject(url, request, TicketResponse.class);
        } catch (HttpClientErrorException e) {
            throw new TicketeroException("Error creating ticket: " + e.getResponseBodyAsString());
        }
    }
    
    public Optional<TicketDetailResponse> getTicket(String uuid) {
        String url = baseUrl + "/api/tickets/" + uuid;
        
        try {
            TicketDetailResponse response = restTemplate.getForObject(url, TicketDetailResponse.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
    
    public PositionResponse getPosition(String numero) {
        String url = baseUrl + "/api/tickets/" + numero + "/position";
        return restTemplate.getForObject(url, PositionResponse.class);
    }
}
```

---

## 🔒 **Seguridad y Autenticación**

### **Desarrollo Local**
- No requiere autenticación
- Todas las APIs son públicas

### **Producción** (Futuro)
```bash
# Header de autorización
Authorization: Bearer <jwt-token>

# Ejemplo
curl -X POST http://api.ticketero.com/api/tickets \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{"nationalId":"12345678",...}'
```

---

## 📊 **Rate Limiting**

### **Límites Actuales**
- **Desarrollo**: Sin límites
- **Producción**: 100 requests/minuto por IP

### **Headers de Rate Limit**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
```

### **Respuesta 429 (Too Many Requests)**
```json
{
  "mensaje": "Rate limit exceeded. Try again in 60 seconds.",
  "codigo": 429,
  "timestamp": "2024-11-25T10:30:00Z"
}
```

---

## 🚨 **Manejo de Errores**

### **Estructura de Error Estándar**
```json
{
  "mensaje": "Descripción del error",
  "codigo": 400,
  "timestamp": "2024-11-25T10:30:00Z"
}
```

### **Errores Comunes**

#### **400 - Validación**
```json
{
  "mensaje": "nationalId: ID nacional inválido, queue: Valor no válido",
  "codigo": 400,
  "timestamp": "2024-11-25T10:30:00Z"
}
```

#### **404 - No Encontrado**
```json
{
  "mensaje": "Ticket no encontrado",
  "codigo": 404,
  "timestamp": "2024-11-25T10:30:00Z"
}
```

#### **500 - Error Interno**
```json
{
  "mensaje": "Error interno del servidor",
  "codigo": 500,
  "timestamp": "2024-11-25T10:30:00Z"
}
```

### **Estrategias de Retry**

```javascript
async function createTicketWithRetry(ticketData, maxRetries = 3) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await client.createTicket(ticketData);
    } catch (error) {
      if (error.response?.status === 500 && attempt < maxRetries) {
        // Retry en errores 5xx
        await sleep(1000 * attempt); // Backoff exponencial
        continue;
      }
      throw error;
    }
  }
}
```

---

## 🧪 **Testing**

### **Postman Collection**
1. Importar [`postman-collection.json`](postman-collection.json)
2. Configurar variable `baseUrl`
3. Ejecutar tests automatizados

### **Datos de Prueba**
```json
{
  "nationalId": "12345678",
  "telefono": "1234567890",
  "branchOffice": "Sucursal Test",
  "queue": "CAJA"
}
```

### **Ambiente de Testing**
```bash
# Variables de entorno para testing
export TICKETERO_BASE_URL=http://localhost:8080
export TICKETERO_TIMEOUT=5000
```

---

## 📈 **Monitoreo y Observabilidad**

### **Health Check**
```bash
curl http://localhost:8080/actuator/health
```

### **Métricas**
```bash
# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus

# Métricas clave:
# - tickets_created_total
# - tickets_processing_duration
# - api_requests_total
```

### **Logs**
```bash
# Logs de aplicación
docker logs ticketero-api

# Buscar errores
docker logs ticketero-api 2>&1 | grep ERROR
```

---

## 🔄 **Versionado de API**

### **Versión Actual: v1**
- Todas las URLs incluyen `/api/` (v1 implícito)
- Breaking changes resultarán en v2

### **Compatibilidad**
- **Backward Compatible**: Nuevos campos opcionales
- **Breaking Changes**: Nuevas versiones de API

---

## 📞 **Soporte**

### **Documentación**
- **OpenAPI Spec**: [`openapi.yaml`](openapi.yaml)
- **Postman Collection**: [`postman-collection.json`](postman-collection.json)
- **Arquitectura**: [`../ARCHITECTURE.md`](../ARCHITECTURE.md)

### **Contacto**
- **Email**: dev@ticketero.com
- **Slack**: #ticketero-api
- **Issues**: GitHub Issues

### **SLA**
- **Disponibilidad**: 99.9%
- **Tiempo de Respuesta**: < 200ms (p95)
- **Soporte**: Horario laboral (9-18h)

---

**Última actualización:** 2024-11-25  
**Versión de la guía:** 1.0