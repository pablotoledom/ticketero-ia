# ADR-003: Arquitectura Monolítica vs Microservicios

## Estado
**Aceptado** - 2024-11-25

## Contexto

Decisión sobre la arquitectura del sistema de ticketero:
- Proyecto de capacitación con timeline limitado
- Equipo pequeño (2-3 desarrolladores)
- Funcionalidad bien definida y acotada
- Necesidad de deployment simple y rápido
- Posible evolución futura a mayor escala

## Decisión

**Implementar arquitectura monolítica modular** con Spring Boot como aplicación única.

### Estructura Seleccionada

```
ticketero-api (Single JAR)
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # Data access
├── scheduler/      # Async processing
└── config/         # Configuration
```

## Justificación Técnica

1. **Simplicidad**: Una sola aplicación para desarrollar, testear y desplegar
2. **Velocidad**: Desarrollo más rápido para MVP
3. **Debugging**: Más fácil debuggear en un solo proceso
4. **Transacciones**: ACID transactions simples dentro del monolito
5. **Deployment**: Un solo artefacto (JAR) para desplegar

## Consecuencias

### ✅ Positivas
- **Time to Market**: Desarrollo y deployment más rápido
- **Simplicidad Operacional**: Un solo servicio a monitorear
- **Consistencia de Datos**: Transacciones ACID nativas
- **Testing**: Tests de integración más simples
- **Debugging**: Stack traces completos en un solo lugar

### ⚠️ Negativas
- **Escalabilidad**: Toda la aplicación escala como una unidad
- **Tecnología**: Stack único para todos los componentes
- **Deployment**: Cambios pequeños requieren redeploy completo
- **Fault Isolation**: Fallo en un componente afecta toda la app

### 🔄 Mitigaciones
- **Modularidad**: Separación clara de responsabilidades por capas
- **Interfaces**: Preparar para futura extracción de servicios
- **Monitoring**: Métricas granulares por componente
- **Circuit Breakers**: Para llamadas externas (Telegram)

## Alternativas Consideradas

### Microservicios
```
[ticket-service] ← → [notification-service] ← → [telegram-service]
       ↓                      ↓                        ↓
[ticket-db]           [message-queue]           [external-api]
```

**❌ Rechazado por:**
- **Complejidad**: Service discovery, distributed tracing, etc.
- **Overhead**: Network latency entre servicios
- **Data Consistency**: Distributed transactions complejas
- **Operational**: Múltiples servicios a monitorear y desplegar

**✅ Ventajas (para futuro):**
- Escalabilidad independiente por servicio
- Tecnologías específicas por dominio
- Fault isolation mejorado
- Teams independientes

### Serverless (AWS Lambda)
**❌ Rechazado por:**
- **Cold Starts**: Latencia impredecible
- **Vendor Lock-in**: Dependencia de AWS
- **Debugging**: Más complejo en entorno distribuido
- **Costo**: Para volumen bajo puede ser más caro

## Estrategia de Evolución

### Fase 1: Monolito Modular (Actual)
```java
@Service
public class TicketService {
    // Business logic centralizada
}

@Service  
public class NotificationService {
    // Preparado para extracción futura
}
```

### Fase 2: Extracción Gradual (Futuro)
1. **Notification Service** → Primer candidato a extraer
2. **Admin Dashboard** → Servicio independiente
3. **Analytics Service** → Para reportes y métricas

### Criterios para Migración
- **Volumen**: > 1000 tickets/día por sucursal
- **Team Size**: > 5 desarrolladores
- **Scaling Needs**: Componentes con diferentes patrones de carga

## Patrones Implementados

### 1. **Layered Architecture**
```java
@RestController  // Presentation Layer
public class TicketController {
    private final TicketService service;
}

@Service  // Business Layer
public class TicketService {
    private final TicketRepository repository;
}

@Repository  // Data Layer
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
```

### 2. **Domain Separation**
```
com.example.ticketero/
├── ticket/          # Ticket domain
├── notification/    # Notification domain  
├── admin/          # Admin domain
└── shared/         # Shared utilities
```

## Métricas de Éxito

- ✅ **Development Velocity**: Features entregadas en < 1 semana
- ✅ **Deployment Time**: < 5 minutos end-to-end
- ✅ **MTTR**: < 15 minutos para resolver incidentes
- ✅ **Code Coverage**: > 80% para business logic

## Monitoreo de Decisión

### Señales para Reconsiderar
- **Performance**: Response time > 500ms p95
- **Scaling**: CPU/Memory > 80% sustained
- **Team**: > 5 desarrolladores trabajando en paralelo
- **Features**: Conflictos frecuentes en deployment

### Métricas de Transición
- **Service Boundaries**: Identificar dominios independientes
- **Data Coupling**: Medir dependencias entre módulos
- **Team Velocity**: Impacto de coordinación en desarrollo

## Referencias

- [Monolith First - Martin Fowler](https://martinfowler.com/bliki/MonolithFirst.html)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [ARCHITECTURE.md - Componentes](../ARCHITECTURE.md#componentes-principales)

---

**Autor:** Equipo de Arquitectura  
**Revisado por:** Tech Lead  
**Próxima revisión:** 2025-02-25 (3 meses)