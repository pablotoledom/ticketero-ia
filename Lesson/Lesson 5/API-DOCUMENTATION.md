# API REST - Sistema Ticketero

## Base URL
```
http://localhost:8080/api/tickets
```

## Endpoints Disponibles

### 1. Crear Ticket
**POST** `/api/tickets`

**Request Body:**
```json
{
  "nationalId": "12345678",
  "telefono": "555-0001",
  "branchOffice": "Sucursal Centro",
  "queueType": "GENERAL"
}
```

**Response:**
```json
{
  "id": 1,
  "codigoReferencia": "REF5AB5473F",
  "numero": "G001",
  "nationalId": "12345678",
  "telefono": "555-0001",
  "branchOffice": "Sucursal Centro",
  "queueType": "GENERAL",
  "status": "WAITING",
  "positionInQueue": 1,
  "estimatedWaitMinutes": 15,
  "createdAt": "2025-12-23T22:16:55.272792247",
  "updatedAt": "2025-12-23T22:16:55.272809567"
}
```

### 2. Consultar Estado de Ticket
**GET** `/api/tickets/{codigoReferencia}/status`

**Response:**
```json
{
  "codigoReferencia": "REF5AB5473F",
  "numero": "G001",
  "queueType": "GENERAL",
  "status": "WAITING",
  "positionInQueue": 3,
  "estimatedWaitMinutes": 45,
  "createdAt": "2025-12-23T22:06:55.272792247",
  "updatedAt": "2025-12-23T22:16:55.272809567"
}
```

### 3. Actualizar Estado de Ticket
**PUT** `/api/tickets/{codigoReferencia}/status`

**Request Body:**
```json
{
  "status": "IN_PROGRESS",
  "assignedAdvisor": "Ana García",
  "assignedModuleNumber": 1
}
```

### 4. Obtener Tickets en Espera
**GET** `/api/tickets/waiting?branchOffice=Sucursal Centro&queueType=GENERAL`

### 5. Estadísticas del Sistema
**GET** `/api/tickets/stats`

**Response:**
```json
{
  "totalTickets": 25,
  "waitingTickets": 8,
  "inProgressTickets": 3,
  "completedTickets": 14,
  "averageWaitTime": 22,
  "timestamp": "2025-12-23T22:16:55.272809567"
}
```

### 6. Cancelar Ticket
**DELETE** `/api/tickets/{codigoReferencia}`

### 7. Health Check
**GET** `/api/tickets/health`

## Estados de Ticket
- `WAITING`: En espera
- `IN_PROGRESS`: En atención
- `COMPLETED`: Completado
- `CANCELLED`: Cancelado

## Tipos de Cola
- `GENERAL`: Cola general
- `PREFERENCIAL`: Cola preferencial

## Códigos de Respuesta HTTP
- `200 OK`: Operación exitosa
- `400 Bad Request`: Datos inválidos
- `404 Not Found`: Ticket no encontrado
- `500 Internal Server Error`: Error del servidor