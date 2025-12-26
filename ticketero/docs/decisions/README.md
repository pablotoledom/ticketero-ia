# 📋 Índice de Decisiones Arquitectónicas (ADRs)

> **Architecture Decision Records** - Registro de decisiones técnicas críticas del proyecto

---

## 📚 **ADRs Documentados**

| ADR | Título | Estado | Fecha | Impacto |
|-----|--------|--------|-------|---------|
| [ADR-001](ADR-001-database-postgresql.md) | Elección de PostgreSQL como Base de Datos Principal | ✅ Aceptado | 2024-11-25 | 🔴 Alto |
| [ADR-002](ADR-002-messaging-rabbitmq.md) | Implementación de RabbitMQ para Notificaciones Asíncronas | ✅ Aceptado | 2024-11-25 | 🟡 Medio |
| [ADR-003](ADR-003-architecture-monolith.md) | Arquitectura Monolítica vs Microservicios | ✅ Aceptado | 2024-11-25 | 🔴 Alto |
| [ADR-004](ADR-004-deployment-docker.md) | Estrategia de Deployment con Docker | ✅ Aceptado | 2024-11-25 | 🟡 Medio |
| [ADR-005](ADR-005-telegram-integration.md) | Integración con Telegram Bot API | ✅ Aceptado | 2024-11-25 | 🟡 Medio |

---

## 🏷️ **Clasificación por Categoría**

### **🗄️ Persistencia y Datos**
- [ADR-001: PostgreSQL como Base de Datos](ADR-001-database-postgresql.md)

### **📨 Mensajería y Comunicación**
- [ADR-002: RabbitMQ para Notificaciones](ADR-002-messaging-rabbitmq.md)
- [ADR-005: Integración con Telegram](ADR-005-telegram-integration.md)

### **🏗️ Arquitectura y Diseño**
- [ADR-003: Arquitectura Monolítica](ADR-003-architecture-monolith.md)

### **🚀 Deployment y Operaciones**
- [ADR-004: Estrategia Docker](ADR-004-deployment-docker.md)

---

## 📊 **Resumen de Decisiones**

### **Stack Tecnológico Seleccionado**
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │    RabbitMQ     │    │   Telegram      │
│   (ADR-001)     │    │   (ADR-002)     │    │   (ADR-005)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         ▲                       ▲                       ▲
         │                       │                       │
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Monolith (ADR-003)                    │
│                    Docker Container (ADR-004)                  │
└─────────────────────────────────────────────────────────────────┘
```

### **Principios de Decisión**
1. **Simplicidad sobre Complejidad** - Monolito vs Microservicios
2. **Confiabilidad sobre Performance** - PostgreSQL ACID
3. **Costo-Efectividad** - Telegram gratuito vs SMS pagado
4. **Portabilidad** - Docker para consistencia de ambientes
5. **Observabilidad** - RabbitMQ Management UI

---

## 🔄 **Proceso de ADRs**

### **Template Estándar**
```markdown
# ADR-XXX: [Título Descriptivo]

## Estado
[Propuesto | Aceptado | Rechazado | Deprecado | Superseded]

## Contexto
## Decisión  
## Consecuencias
## Alternativas Consideradas
## Referencias
```

### **Criterios de Revisión**
- **Impacto Alto**: Revisión cada 3 meses
- **Impacto Medio**: Revisión cada 6 meses  
- **Impacto Bajo**: Revisión anual

### **Estados Posibles**
- 🟢 **Propuesto** - En discusión
- ✅ **Aceptado** - Implementado y activo
- ❌ **Rechazado** - Descartado con justificación
- ⚠️ **Deprecado** - Ya no recomendado
- 🔄 **Superseded** - Reemplazado por ADR más reciente

---

## 📅 **Próximas Revisiones**

| ADR | Próxima Revisión | Responsable |
|-----|------------------|-------------|
| ADR-003 | 2025-02-25 | Tech Lead |
| ADR-001 | 2025-05-25 | DBA |
| ADR-002 | 2025-05-25 | DevOps |
| ADR-004 | 2025-05-25 | DevOps |
| ADR-005 | 2025-05-25 | Product Owner |

---

## 🔗 **Referencias**

- **Documentación Principal**: [`../ARCHITECTURE.md`](../ARCHITECTURE.md)
- **Estándares de Código**: [`../CODING-STANDARDS.md`](../CODING-STANDARDS.md)
- **Guía de Deployment**: [`../DEPLOYMENT.md`](../DEPLOYMENT.md)
- **ADR Template**: [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)

---

**Mantenido por:** Equipo de Arquitectura  
**Última actualización:** 2024-11-25