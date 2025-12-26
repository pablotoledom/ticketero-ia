# 🎫 Ticketero API - Aplicación Principal

> **API REST para gestión de tickets bancarios con notificaciones automáticas vía Telegram**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Coverage](https://img.shields.io/badge/coverage-85%25-green.svg)](#)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)

## 🏗️ Arquitectura Técnica

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Cliente   │────▶│ Controller  │────▶│ PostgreSQL  │
│  (Postman)  │     │   Layer     │     │  Database   │
└─────────────┘     └──────┬──────┘     └─────────────┘
                          │
                    ┌─────▼─────┐
                    │  Service  │
                    │   Layer   │
                    └─────┬─────┘
                          │
                    ┌─────▼─────┐     ┌─────────────┐
                    │ Scheduler │────▶│  Telegram   │
                    │ @Scheduled│     │  Bot API    │
                    └───────────┘     └─────────────┘
```

## ✨ Características Técnicas

- **Arquitectura en Capas**: Controller → Service → Repository
- **Programación Reactiva**: Scheduler para mensajes asíncronos
- **Notificaciones Telegram**: 3 tipos (creado, próximo, activo)
- **Transaccionalidad**: @Transactional para consistencia de datos
- **Métricas**: Actuator + Prometheus para monitoreo
- **Containerización**: Docker multi-stage optimizado

## 🛠️ Stack Tecnológico

| Componente | Tecnología | Versión | Propósito |
|------------|-------------|---------|----------|
| **Runtime** | Java | 21 (LTS) | Lenguaje principal |
| **Framework** | Spring Boot | 3.2.11 | Framework web |
| **ORM** | Spring Data JPA | 3.2+ | Persistencia |
| **Database** | PostgreSQL | 16 | Base de datos relacional |
| **Messaging** | RabbitMQ | 3.13 | Cola de mensajes |
| **Migrations** | Flyway | 10+ | Migraciones de BD |
| **Monitoring** | Micrometer + Prometheus | - | Métricas |
| **Testing** | JUnit 5 + Mockito | 5.10+ | Testing framework |
| **Build** | Maven | 3.9+ | Herramienta de build |
| **Container** | Docker | 24+ | Containerización |

## 🚀 Quick Start para Desarrolladores

### 1. **Setup Local**
```bash
# Clonar y configurar
git clone <repository-url>
cd ticketero
cp .env.example .env
# Editar .env con TELEGRAM_BOT_TOKEN

# Levantar infraestructura
docker compose up -d postgres rabbitmq

# Ejecutar aplicación
./mvnw spring-boot:run
```

### 2. **Verificar Setup**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Crear ticket de prueba
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"nationalId":"12345678","telefono":"1234567890","branchOffice":"Centro","queue":"CAJA"}'
```

### 3. **Desarrollo**
```bash
# Tests
./mvnw test

# Build
./mvnw clean package

# Docker build
docker build -t ticketero-api .
```

## API Endpoints

### Tickets

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tickets` | Create new ticket |
| GET | `/api/tickets/{uuid}` | Get ticket by reference code |
| GET | `/api/tickets/{numero}/position` | Get queue position |

### Admin Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/dashboard` | System overview |
| GET | `/api/admin/queues/{type}` | Queue status |
| GET | `/api/admin/advisors` | List advisors |
| PUT | `/api/admin/advisors/{id}/status` | Update advisor status |

## Project Structure

```
src/main/java/com/example/ticketero/
├── config/          # Spring configurations
├── consumer/        # RabbitMQ message consumers
├── controller/      # REST API controllers
├── exception/       # Custom exceptions
├── model/
│   ├── dto/         # Data Transfer Objects
│   ├── entity/      # JPA entities
│   └── enums/       # Enumeration types
├── repository/      # Spring Data repositories
├── service/         # Business logic services
└── util/            # Utility classes
```

## Key Patterns

### Outbox Pattern

Ensures transactional consistency between database and message broker:

```java
@Transactional
public TicketResponse crearTicket(TicketCreateRequest request) {
    Ticket ticket = ticketRepository.saveAndFlush(ticket);
    outboxMessageRepository.save(outboxMessage);  // Same transaction
    return response;
}
```

### Manual ACK with RabbitMQ

Prevents message loss with manual acknowledgment:

```java
@RabbitListener(queues = "caja-queue", ackMode = "MANUAL")
public void process(Message msg, Channel channel, long deliveryTag) {
    try {
        processTicket(msg);
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, true);
    }
}
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/ticketero` |
| `DATABASE_USERNAME` | Database user | `dev` |
| `DATABASE_PASSWORD` | Database password | `dev123` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token | - |
| `TELEGRAM_CHAT_ID` | Telegram chat ID | - |

## Development

### Run Tests

```bash
./mvnw test
```

### Build JAR

```bash
./mvnw clean package -DskipTests
```

### Run Locally

```bash
./mvnw spring-boot:run
```

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

### RabbitMQ Management

Open http://localhost:15672 (user: dev, password: dev123)

## 📚 Documentación Técnica

| Documento | Descripción | Audiencia |
|-----------|-------------|----------|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Diseño del sistema y decisiones | Arquitectos/Desarrolladores |
| [`docs/CODING-STANDARDS.md`](docs/CODING-STANDARDS.md) | Estándares y convenciones | Desarrolladores |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | Guía de deployment | DevOps/SRE |
| [API Documentation](#api-endpoints) | Endpoints y ejemplos | Frontend/Integradores |

## 🔗 Enlaces Útiles

- **README Principal**: [`../README.md`](../README.md) - Visión general del proyecto
- **Infraestructura CDK**: [`../ticketero-infra/`](../ticketero-infra/) - Código de infraestructura
- **Prometheus Métricas**: http://localhost:8080/actuator/prometheus
- **RabbitMQ Management**: http://localhost:15672 (dev/dev123)
- **Grafana Dashboard**: http://localhost:3000 (admin/admin123)

## 🏆 Objetivos de Calidad

- **Cobertura de Tests**: > 80%
- **Tiempo de Respuesta**: < 200ms (p95)
- **Disponibilidad**: > 99.9%
- **Tiempo de Build**: < 2 minutos
- **Tiempo de Startup**: < 30 segundos
