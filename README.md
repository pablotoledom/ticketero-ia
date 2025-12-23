# Ticketero Digital - Sistema de Gestión de Tickets

Sistema digital de gestión de tickets para atención en sucursales bancarias con notificaciones en tiempo real vía Telegram.

## Stack Tecnológico

- **Java 21** - Lenguaje principal
- **Spring Boot 3.2** - Framework de aplicación
- **PostgreSQL** - Base de datos
- **Telegram Bot API** - Notificaciones
- **WebSocket** - Dashboard en tiempo real

## Arquitectura

**Monolito Modular** - Enfoque pragmático sin sobre-ingeniería, optimizado para simplicidad operacional y mantenimiento.

### Módulos Principales
- `ticket/` - Gestión de tickets
- `queue/` - Manejo de colas por tipo de atención
- `notification/` - Servicio de notificaciones Telegram
- `executive/` - Gestión de ejecutivos y asignación automática
- `monitoring/` - Dashboard y métricas en tiempo real
- `audit/` - Trazabilidad completa de eventos

## Documentación

- [`docs/project-requirements.md`](docs/project-requirements.md) - Requerimientos funcionales y no funcionales
- [`docs/technical-architecture.md`](docs/technical-architecture.md) - Arquitectura técnica detallada
- [`docs/aws-architecture-diagram.md`](docs/aws-architecture-diagram.md) - Diagrama de arquitectura AWS con servicios cloud

## Funcionalidades Clave

### Para Clientes
- ✅ Creación de ticket digital con RUT/ID
- ✅ Notificaciones automáticas vía Telegram (3 mensajes)
- ✅ Consulta de estado y posición en cola
- ✅ Movilidad durante la espera

### Para Ejecutivos
- ✅ Asignación automática de tickets
- ✅ Balanceo de carga inteligente
- ✅ Notificación de nuevo cliente asignado

### Para Supervisores
- ✅ Dashboard en tiempo real (actualización cada 5s)
- ✅ Métricas de rendimiento por cola
- ✅ Alertas de situaciones críticas
- ✅ Auditoría completa de eventos

## Tipos de Cola

| Cola | Tiempo Promedio | Prioridad |
|------|----------------|-----------|
| Caja | 5 min | Baja |
| Personal Banker | 15 min | Media |
| Empresas | 20 min | Media |
| Gerencia | 30 min | Máxima |

## Escalabilidad

- **Fase Piloto**: 500-800 tickets/día, 1 sucursal
- **Fase Expansión**: 2,500-3,000 tickets/día, 5 sucursales  
- **Fase Nacional**: 25,000+ tickets/día, 50+ sucursales

## Próximos Pasos

1. Implementar MVP (Ticket + Queue básico)
2. Integrar Telegram Bot
3. Desarrollar dashboard mínimo viable
4. Implementar auditoría y métricas
5. Optimizar según métricas reales

---

**Proyecto de Capacitación** - Ciclo Completo de Desarrollo de Software