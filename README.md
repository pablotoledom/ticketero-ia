# 🎫 Sistema Ticketero - API REST

Sistema de gestión de tickets con notificaciones en tiempo real desarrollado con Spring Boot 3.2.11 y Java 21.

## 🚀 Características

- ✅ API REST completa para gestión de tickets
- ✅ Base de datos PostgreSQL con Hibernate
- ✅ Diferentes tipos de cola (General y Preferencial)
- ✅ Estados de ticket (Waiting, In Progress, Completed, Cancelled)
- ✅ Estimación de tiempo de espera
- ✅ Asignación de asesores y módulos
- ✅ Estadísticas del sistema
- ✅ Dockerizado y listo para producción

## 🛠️ Stack Tecnológico

- **Backend**: Spring Boot 3.2.11
- **Lenguaje**: Java 21
- **Base de datos**: PostgreSQL 16
- **ORM**: Hibernate/JPA
- **Migraciones**: Flyway (opcional)
- **Contenedores**: Docker & Docker Compose
- **Build**: Maven

## 📋 Prerrequisitos

- Java 21+
- Maven 3.8+
- PostgreSQL 16+ (o Docker)
- Docker & Docker Compose (opcional)

## 🚀 Instalación y Ejecución

### Opción 1: Con Docker Compose (Recomendado)

```bash
# Clonar el repositorio
git clone <repository-url>
cd ticketero-ia

# Ejecutar con Docker Compose
docker-compose up -d

# La API estará disponible en http://localhost:8080
```

### Opción 2: Ejecución Local

```bash
# 1. Configurar PostgreSQL
createdb ticketero

# 2. Configurar variables de entorno
export DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
export DATABASE_USERNAME=dev
export DATABASE_PASSWORD=dev123

# 3. Compilar y ejecutar
mvn clean compile
mvn spring-boot:run

# La API estará disponible en http://localhost:8080
```

## 📚 Documentación de API

Ver [API-DOCUMENTATION.md](./API-DOCUMENTATION.md) para detalles completos de todos los endpoints.

### Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/tickets` | Crear nuevo ticket |
| GET | `/api/tickets/{codigo}/status` | Consultar estado |
| PUT | `/api/tickets/{codigo}/status` | Actualizar estado |
| GET | `/api/tickets/waiting` | Tickets en espera |
| GET | `/api/tickets/stats` | Estadísticas |
| DELETE | `/api/tickets/{codigo}` | Cancelar ticket |
| GET | `/api/tickets/health` | Health check |

## 🧪 Pruebas

```bash
# Ejecutar script de pruebas automáticas
./test-api.sh

# O probar manualmente
curl http://localhost:8080/api/tickets/health
```

## 📁 Estructura del Proyecto

```
ticketero-ia/
├── src/main/java/com/example/ticketero/
│   ├── controller/          # Controladores REST
│   ├── model/
│   │   ├── entity/         # Entidades JPA
│   │   ├── dto/            # DTOs
│   │   └── enums/          # Enumeraciones
│   ├── repository/         # Repositorios JPA
│   └── TicketeroApplication.java
├── src/main/resources/
│   ├── db/migration/       # Migraciones Flyway
│   └── application.yml     # Configuración
├── docs/                   # Documentación técnica
├── docker-compose.yml      # Configuración Docker
├── Dockerfile             # Imagen Docker
├── test-api.sh           # Script de pruebas
└── README.md
```

## 🔧 Configuración

### Variables de Entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `DATABASE_URL` | URL de PostgreSQL | `jdbc:postgresql://localhost:5432/ticketero` |
| `DATABASE_USERNAME` | Usuario de BD | `dev` |
| `DATABASE_PASSWORD` | Contraseña de BD | `dev123` |
| `TELEGRAM_BOT_TOKEN` | Token del bot (futuro) | - |

### Perfiles de Spring

- `default`: Desarrollo local
- `docker`: Contenedores Docker
- `prod`: Producción (futuro)

## 📊 Modelo de Datos

### Entidad Ticket

```sql
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia VARCHAR(20) UNIQUE NOT NULL,
    numero VARCHAR(10) NOT NULL,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    branch_office VARCHAR(50) NOT NULL,
    queue_type VARCHAR(20) CHECK (queue_type IN ('PREFERENCIAL', 'GENERAL')),
    status VARCHAR(20) DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    position_in_queue INTEGER DEFAULT 0,
    estimated_wait_minutes INTEGER DEFAULT 0,
    assigned_advisor VARCHAR(100),
    assigned_module_number INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🚀 Próximas Funcionalidades

- [ ] Integración con Telegram Bot
- [ ] Notificaciones en tiempo real (WebSocket)
- [ ] Dashboard web para administradores
- [ ] Métricas avanzadas con Micrometer
- [ ] Autenticación y autorización
- [ ] Tests unitarios e integración
- [ ] CI/CD con GitHub Actions

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👥 Autores

- **Desarrollador Principal** - Implementación inicial

## 🙏 Agradecimientos

- Spring Boot Team por el excelente framework
- PostgreSQL por la robusta base de datos
- Docker por la containerización