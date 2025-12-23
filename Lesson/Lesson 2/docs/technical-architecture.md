# **Arquitectura Técnica - Sistema de Gestión de Tickets**

**Proyecto:** Ticketero Digital  
**Stack:** Java 21 + Spring Boot 3.2 + PostgreSQL  
**Enfoque:** Pragmático, sin sobre-ingeniería  
**Fecha:** Diciembre 2025

## **1. Módulos y Componentes Principales**

### **Core Business Modules**
- **Ticket Management**: Creación, asignación y seguimiento de tickets
- **Queue Management**: Gestión de colas por tipo de atención con prioridades
- **Notification Service**: Envío de mensajes vía Telegram con reintentos
- **Executive Management**: Control de disponibilidad y asignación automática
- **Monitoring Dashboard**: Panel en tiempo real para supervisores

### **Supporting Components**
- **Audit Service**: Trazabilidad completa de eventos
- **Calculation Engine**: Estimación de tiempos y posiciones
- **Configuration Service**: Parámetros de negocio (tiempos promedio, prioridades)

**Justificación**: Cada módulo tiene una responsabilidad específica y clara, evitando acoplamiento innecesario mientras mantiene cohesión funcional.

## **2. Arquitectura Recomendada: Monolito Modular**

### **Decisión: Monolito Modular con Spring Boot**

**Justificación**:
- **Simplicidad operacional**: Un solo artefacto para desplegar
- **Transacciones ACID**: Consistencia garantizada entre tickets, colas y notificaciones
- **Latencia mínima**: Comunicación interna sin overhead de red
- **Escalabilidad suficiente**: 25K tickets/día es manejable con un monolito bien diseñado
- **Mantenimiento**: Equipo pequeño, debugging más simple

### **Estructura Modular Interna**
```
com.ticketero
├── ticket/          # Gestión de tickets
├── queue/           # Manejo de colas
├── notification/    # Servicio de notificaciones
├── executive/       # Gestión de ejecutivos
├── monitoring/      # Dashboard y métricas
├── audit/           # Auditoría y logs
└── shared/          # Componentes compartidos
```

## **3. Estructura del Proyecto Spring Boot 3.2**

```
ticketero-system/
├── src/main/java/com/ticketero/
│   ├── TicketeroApplication.java
│   ├── config/
│   │   ├── DatabaseConfig.java
│   │   ├── TelegramConfig.java
│   │   └── WebSocketConfig.java
│   ├── ticket/
│   │   ├── domain/
│   │   ├── application/
│   │   ├── infrastructure/
│   │   └── web/
│   ├── queue/
│   ├── notification/
│   ├── executive/
│   ├── monitoring/
│   └── shared/
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/
│   └── static/dashboard/
└── src/test/
```

**Enfoque**: Arquitectura hexagonal ligera por módulo, sin over-engineering.

## **4. Diseño del Dominio**

### **Agregados Principales**

**Ticket Aggregate**
```java
@Entity
public class Ticket {
    private TicketId id;
    private CustomerId customerId;
    private QueueType queueType;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private ExecutiveId assignedExecutive;
    private Integer position;
    private Duration estimatedWaitTime;
}
```

**Queue Aggregate**
```java
@Entity
public class Queue {
    private QueueType type;
    private List<TicketId> waitingTickets;
    private QueueConfiguration config;
    private QueueMetrics metrics;
}
```

**Executive Aggregate**
```java
@Entity
public class Executive {
    private ExecutiveId id;
    private String name;
    private Set<QueueType> supportedQueues;
    private ExecutiveStatus status;
    private TicketId currentTicket;
}
```

### **Value Objects Clave**
- `TicketId`, `CustomerId`, `ExecutiveId`
- `QueueType` (enum: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)
- `TicketStatus` (enum: WAITING, ASSIGNED, IN_PROGRESS, COMPLETED)

### **DTOs Mínimos**
- `CreateTicketRequest/Response`
- `TicketStatusResponse`
- `DashboardMetricsResponse`

## **5. Estrategia de Persistencia PostgreSQL**

### **Migración: Flyway**
**Justificación**: Más simple que Liquibase, perfecto para este caso de uso.

### **Esquema de Base de Datos**
```sql
-- V1__initial_schema.sql
CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    assigned_executive_id UUID,
    position INTEGER,
    estimated_wait_minutes INTEGER
);

CREATE TABLE executives (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    supported_queues TEXT[], -- PostgreSQL array
    status VARCHAR(20) NOT NULL,
    current_ticket_id UUID
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

### **Índices Estratégicos**
```sql
CREATE INDEX idx_tickets_queue_status ON tickets(queue_type, status);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_audit_events_entity ON audit_events(entity_id, created_at);
```

## **6. Integraciones Externas**

### **Telegram Bot API**
- **Cliente HTTP**: WebClient de Spring WebFlux
- **Patrón**: Circuit Breaker con Resilience4j
- **Reintentos**: 3 intentos con backoff exponencial
- **Fallback**: Cola de mensajes pendientes en BD

### **WebSocket para Dashboard**
- **Tecnología**: Spring WebSocket + STOMP
- **Frecuencia**: Actualización cada 5 segundos
- **Payload**: Solo métricas agregadas, no datos sensibles

## **7. Buenas Prácticas Java 21 + Spring Boot 3.2**

### **Java 21 Features**
- **Virtual Threads**: Para operaciones I/O (Telegram API)
- **Pattern Matching**: En switch expressions para estados
- **Records**: Para DTOs y Value Objects
- **Text Blocks**: Para queries SQL complejas

### **Spring Boot 3.2**
- **Native Compilation**: Para startup más rápido
- **Observability**: Micrometer + Prometheus
- **Configuration Properties**: `@ConfigurationProperties` con records
- **Testing**: TestContainers para tests de integración

### **Configuración Ejemplo**
```java
@ConfigurationProperties("ticketero")
public record TicketeroConfig(
    TelegramConfig telegram,
    QueueConfig queues,
    MonitoringConfig monitoring
) {
    public record TelegramConfig(String botToken, String baseUrl) {}
    public record QueueConfig(Map<QueueType, QueueSettings> settings) {}
}
```

## **8. Deployment y Orquestación**

### **Containerización**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/ticketero-system.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app.jar"]
```

### **IaC Recomendado: Docker Compose (Fase Piloto)**
```yaml
services:
  app:
    image: ticketero-system:latest
    environment:
      - SPRING_PROFILES_ACTIVE=production
    depends_on:
      - postgres
  
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ticketero
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

### **Escalabilidad Futura**
- **Kubernetes**: Solo cuando llegues a 10+ sucursales
- **Helm Charts**: Para gestión de configuraciones por ambiente
- **Horizontal Pod Autoscaler**: Basado en CPU y memoria

## **9. Observabilidad y Monitoreo**

### **Métricas Clave**
- Tickets creados/minuto
- Tiempo promedio de procesamiento
- Tasa de éxito de notificaciones Telegram
- Latencia de asignación automática

### **Stack Recomendado**
- **Logs**: Logback con formato JSON
- **Métricas**: Micrometer + Prometheus
- **Alertas**: Grafana con umbrales críticos
- **Health Checks**: Spring Actuator

## **10. Consideraciones de Seguridad**

### **Datos Sensibles**
- Encriptación AES-256 para RUT/teléfonos
- Variables de entorno para secrets
- HTTPS obligatorio en producción

### **Acceso**
- Spring Security con JWT para dashboard
- Rate limiting para APIs públicas
- CORS configurado específicamente

---

## **Próximos Pasos Sugeridos**

1. Crear MVP con módulo Ticket + Queue básico
2. Integrar Telegram Bot para notificaciones
3. Desarrollar dashboard mínimo viable
4. Implementar auditoría y métricas
5. Optimizar y escalar según métricas reales

---

**Preparado por:** Arquitecto de Software Senior  
**Enfoque:** Pragmático, orientado a productividad  
**Principio:** Simple, mantenible, fácilmente desplegable