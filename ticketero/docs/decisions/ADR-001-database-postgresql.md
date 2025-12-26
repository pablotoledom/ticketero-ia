# ADR-001: Elección de PostgreSQL como Base de Datos Principal

## Estado
**Aceptado** - 2024-11-25

## Contexto

El sistema de ticketero requiere una base de datos que soporte:
- Transacciones ACID para consistencia de datos
- Relaciones entre entidades (tickets ↔ mensajes)
- Consultas complejas para posición en cola
- Escalabilidad para múltiples sucursales
- Soporte nativo en Spring Boot

## Decisión

**Seleccionar PostgreSQL 16 como base de datos principal** para el sistema de ticketero.

### Justificación Técnica

1. **Transaccionalidad ACID**: Crítica para consistencia entre tickets y mensajes
2. **Modelo Relacional**: Ideal para relaciones 1:N (ticket → mensajes)
3. **Performance**: Índices optimizados para consultas de posición en cola
4. **Ecosistema Spring**: Soporte nativo con Spring Data JPA
5. **Operaciones**: Experiencia del equipo y herramientas maduras

## Consecuencias

### ✅ Positivas
- **Consistencia garantizada** con transacciones ACID
- **Queries complejas** con SQL estándar
- **Migraciones controladas** con Flyway
- **Monitoreo maduro** con herramientas existentes
- **Backup/Recovery** procedimientos establecidos

### ⚠️ Negativas
- **Escalabilidad horizontal** más compleja que NoSQL
- **Overhead relacional** para operaciones simples
- **Dependencia de esquema** requiere migraciones

### 🔄 Mitigaciones
- Usar índices optimizados para queries frecuentes
- Connection pooling para performance
- Read replicas para escalabilidad de lectura (futuro)

## Alternativas Consideradas

### MongoDB (NoSQL)
- ❌ **Rechazado**: Transacciones ACID limitadas
- ❌ **Complejidad**: Relaciones 1:N menos naturales
- ✅ **Ventaja**: Escalabilidad horizontal

### MySQL
- ❌ **Rechazado**: Funcionalidades JSON menos maduras
- ❌ **Licencia**: Consideraciones comerciales
- ✅ **Ventaja**: Amplia adopción

### H2 (In-Memory)
- ❌ **Rechazado**: Solo para testing
- ❌ **Persistencia**: Datos se pierden al reiniciar
- ✅ **Ventaja**: Setup simple para desarrollo

## Implementación

```yaml
# docker-compose.yml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: ticketero
    POSTGRES_USER: dev
    POSTGRES_PASSWORD: dev123
```

```sql
-- Esquema optimizado
CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID UNIQUE NOT NULL,
    -- ... otros campos
);

CREATE INDEX idx_ticket_codigo_ref ON ticket(codigo_referencia);
```

## Métricas de Éxito

- ✅ **Tiempo de respuesta**: < 100ms para consultas de tickets
- ✅ **Consistencia**: 0 inconsistencias entre tickets y mensajes
- ✅ **Disponibilidad**: > 99.9% uptime
- ✅ **Backup**: Recovery time < 15 minutos

## Referencias

- [PostgreSQL Documentation](https://www.postgresql.org/docs/16/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [ARCHITECTURE.md - Modelo de Datos](../ARCHITECTURE.md#modelo-de-datos)

---

**Autor:** Equipo de Arquitectura  
**Revisado por:** Tech Lead  
**Próxima revisión:** 2025-05-25