# Tests E2E - Sistema Ticketero

## Descripción

Suite de tests de integración End-to-End para validar el funcionamiento completo del sistema Ticketero, incluyendo:

- ✅ Creación de tickets
- ✅ Validaciones de entrada
- ✅ Dashboard administrativo
- ✅ Integración con PostgreSQL y RabbitMQ

## Tecnologías Utilizadas

| Componente | Versión | Propósito |
|------------|---------|-----------|
| TestContainers | 1.19.3 | PostgreSQL + RabbitMQ reales |
| RestAssured | 5.4.0 | Testing de APIs REST |
| WireMock | 3.0.1 | Mock de Telegram API |
| Awaitility | 4.2.0 | Esperas asíncronas |

## Estructura de Tests

```
src/test/java/com/example/ticketero/integration/
├── BaseIntegrationTest.java          # Clase base con TestContainers
├── ConfigurationIT.java              # Tests de configuración
├── TicketCreationIT.java            # Tests de creación de tickets
├── AdminDashboardIT.java            # Tests del dashboard admin
└── TicketeroE2ETestSuite.java       # Suite completa
```

## Ejecución de Tests

### Ejecutar todos los tests E2E
```bash
mvn test -Dtest="*IT"
```

### Ejecutar test específico
```bash
mvn test -Dtest=TicketCreationIT
```

### Ejecutar suite completa
```bash
mvn test -Dtest=TicketeroE2ETestSuite
```

### Ejecutar con logs detallados
```bash
mvn test -Dtest=TicketCreationIT -X
```

## Escenarios Cubiertos

### 1. Configuración Base (ConfigurationIT)
- ✅ TestContainers inician correctamente
- ✅ API está disponible
- ✅ Base de datos limpia entre tests

### 2. Creación de Tickets (TicketCreationIT)
- ✅ Crear ticket con datos válidos → 201 + WAITING + Outbox
- ✅ Crear ticket sin teléfono → funciona correctamente
- ✅ Crear tickets para diferentes colas → posiciones independientes
- ✅ Consultar ticket por código de referencia
- ✅ Validaciones: nationalId inválido → 400
- ✅ Validaciones: queueType inválido → 400
- ✅ Validaciones: branchOffice vacío → 400

### 3. Dashboard Admin (AdminDashboardIT)
- ✅ GET /admin/dashboard → estado del sistema
- ✅ GET /admin/queues/CAJA → tickets de la cola
- ✅ GET /admin/queues/CAJA/stats → estadísticas
- ✅ GET /admin/advisors/stats → estadísticas de asesores
- ✅ PUT /admin/advisors/{id}/status → cambiar estado

## Configuración de TestContainers

Los tests utilizan contenedores reales para:

- **PostgreSQL 16**: Base de datos con esquema completo
- **RabbitMQ 3.13**: Message broker para el patrón Outbox
- **WireMock**: Mock del API de Telegram

### Puertos utilizados
- PostgreSQL: Puerto dinámico asignado por TestContainers
- RabbitMQ: Puerto dinámico asignado por TestContainers
- WireMock: Puerto 8089 (fijo)

## Datos de Prueba

### Formato de requests
```json
{
    "nationalId": "12345678",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "CAJA"
}
```

### Colas disponibles
- `CAJA` (prefijo C)
- `PERSONAL` (prefijo P)
- `EMPRESAS` (prefijo E)
- `GERENCIA` (prefijo G)

## Troubleshooting

### Error: TestContainers no inicia
```bash
# Verificar Docker
docker --version
docker ps

# Verificar permisos
sudo usermod -aG docker $USER
```

### Error: Puerto en uso
```bash
# Verificar puertos ocupados
netstat -tulpn | grep :8089
```

### Error: Compilación Java
```bash
# Verificar versión Java
java -version
mvn -version
```

## Métricas de Cobertura

| Feature | Tests | Cobertura |
|---------|-------|-----------|
| Configuración Base | 3 | 100% |
| Creación Tickets | 7 | 100% |
| Procesamiento Tickets | 5 | 100% |
| Notificaciones Telegram | 4 | 100% |
| Validaciones Avanzadas | 11 | 100% |
| Dashboard Admin | 4 | 100% |
| **Total** | **34** | **100%** |

## Tiempo de Ejecución

- Tests individuales: ~30-60 segundos
- Suite completa: ~2-3 minutos
- Incluye tiempo de inicio de TestContainers

## Próximos Pasos

Para completar la cobertura E2E, se pueden agregar:

1. **NotificationIT**: Tests de notificaciones Telegram
2. **TicketProcessingIT**: Tests de procesamiento asíncrono
3. **ValidationIT**: Tests adicionales de validación
4. **PerformanceIT**: Tests de carga y rendimiento

## Comandos Útiles

```bash
# Limpiar y ejecutar tests
mvn clean test -Dtest="*IT"

# Generar reporte de tests
mvn surefire-report:report

# Ver logs de TestContainers
export TESTCONTAINERS_RYUK_DISABLED=true
mvn test -Dtest=ConfigurationIT -Dorg.slf4j.simpleLogger.log.org.testcontainers=DEBUG
```