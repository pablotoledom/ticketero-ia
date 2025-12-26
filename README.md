# 🎫 Sistema Ticketero - Gestión de Colas Bancarias

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![AWS CDK](https://img.shields.io/badge/AWS%20CDK-2.170-orange.svg)](https://aws.amazon.com/cdk/)

> **Sistema completo de gestión de tickets bancarios con notificaciones automáticas vía Telegram**

---

## 🚀 **Quick Start (< 5 minutos)**

```bash
# 1. Clonar proyecto
git clone <repository-url>
cd ticketero-ia

# 2. Configurar variables
cd ticketero
cp .env.example .env
# Editar .env con tu TELEGRAM_BOT_TOKEN

# 3. Levantar servicios
docker compose up -d

# 4. Verificar
curl http://localhost:8080/actuator/health
```

**🎯 ¡Listo!** API corriendo en http://localhost:8080

---

## 📋 **Descripción**

Sistema que digitaliza el proceso de emisión de tickets en sucursales bancarias, enviando **3 notificaciones automáticas** vía Telegram:

1. **Confirmación inmediata** → "Tu ticket P01 está listo, tiempo estimado: 15 min"
2. **Pre-aviso (~30s)** → "Faltan 3 turnos para ti"  
3. **Turno activo (~60s)** → "¡Es tu turno P01!"

### **Características Principales**
- ✅ API REST con Spring Boot 3.2 + Java 21
- ✅ Base de datos PostgreSQL con migraciones Flyway
- ✅ Mensajería asíncrona con RabbitMQ
- ✅ Notificaciones reales vía Telegram Bot API
- ✅ Containerización completa con Docker
- ✅ Infraestructura como código con AWS CDK
- ✅ Monitoreo con Prometheus + Grafana

---

## 🏗️ **Arquitectura**

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Cliente   │────▶│  REST API   │────▶│ PostgreSQL  │
│  (Postman)  │     │ Spring Boot │     │   Tickets   │
└─────────────┘     └──────┬──────┘     └─────────────┘
                          │
                    ┌─────▼─────┐
                    │ Scheduler │
                    │ Mensajes  │
                    └─────┬─────┘
                          │
                    ┌─────▼─────┐     ┌─────────────┐
                    │ RabbitMQ  │────▶│  Telegram   │
                    │   Queue   │     │    Bot      │
                    └───────────┘     └─────────────┘
```

**Stack Tecnológico:**
- **Backend:** Java 21, Spring Boot 3.2, Spring Data JPA
- **Base de Datos:** PostgreSQL 16, Flyway migrations
- **Mensajería:** RabbitMQ 3.13
- **Notificaciones:** Telegram Bot API
- **Containerización:** Docker + Docker Compose
- **Infraestructura:** AWS CDK (Java)
- **Monitoreo:** Prometheus, Grafana

---

## 📁 **Estructura del Proyecto**

```
ticketero-ia/
├── ticketero/                   # 🎯 Aplicación Principal
│   ├── src/main/java/          # Código fuente Java
│   ├── docs/                   # Documentación técnica
│   ├── scripts/                # Scripts de utilidad
│   ├── docker-compose.yml      # Orquestación local
│   ├── Dockerfile              # Imagen optimizada
│   └── README.md               # Documentación de la API
├── ticketero-infra/            # 🏗️ Infraestructura AWS
│   ├── src/main/java/          # Código CDK
│   └── cdk.json                # Configuración CDK
└── docs/                       # 📚 Documentación global
    ├── ARCHITECTURE.md         # Diseño del sistema
    ├── DEPLOYMENT.md           # Guía de deployment
    └── CODING-STANDARDS.md     # Estándares de código
```

---

## ⚡ **Requisitos Previos**

### **Para Desarrollo Local**
- ☕ **Java 21+** ([OpenJDK](https://openjdk.java.net/projects/jdk/21/))
- 📦 **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- 🐳 **Docker + Docker Compose** ([Install](https://docs.docker.com/get-docker/))
- 🤖 **Telegram Bot Token** ([Crear bot](https://core.telegram.org/bots#creating-a-new-bot))

### **Para Deployment AWS**
- ☁️ **AWS CLI configurado** ([Setup](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html))
- 🛠️ **AWS CDK CLI** (`npm install -g aws-cdk`)
- 🔑 **Credenciales AWS** con permisos de deployment

---

## 🛠️ **Instalación y Configuración**

### **1. Configuración de Variables**

```bash
cd ticketero
cp .env.example .env
```

Editar `.env`:
```bash
# Telegram Configuration
TELEGRAM_BOT_TOKEN=123456789:ABCDEF...  # Tu bot token
TELEGRAM_CHAT_ID=123456789              # Tu chat ID

# Database (Docker Compose maneja esto)
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123

# RabbitMQ (Docker Compose maneja esto)
RABBITMQ_HOST=localhost
RABBITMQ_USERNAME=dev
RABBITMQ_PASSWORD=dev123
```

### **2. Obtener Token de Telegram**

```bash
# 1. Crear bot con @BotFather en Telegram
# 2. Enviar /newbot y seguir instrucciones
# 3. Copiar el token generado
# 4. Obtener tu chat ID enviando mensaje al bot y visitando:
#    https://api.telegram.org/bot<TOKEN>/getUpdates
```

### **3. Levantar Infraestructura**

```bash
# Opción A: Todo con Docker Compose (Recomendado)
docker compose up -d

# Opción B: Solo infraestructura + app local
docker compose up -d postgres rabbitmq
./mvnw spring-boot:run
```

---

## 🎮 **Uso**

### **API Endpoints**

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/tickets` | Crear nuevo ticket |
| `GET` | `/api/tickets/{uuid}` | Consultar ticket por código |
| `GET` | `/api/tickets/{numero}/position` | Obtener posición en cola |
| `GET` | `/api/admin/dashboard` | Dashboard administrativo |

### **Crear Ticket (Ejemplo)**

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

### **Flujo de Notificaciones**

1. **Inmediata:** "🎫 Tu ticket C01 está listo. Posición: 1, Tiempo estimado: 15 min"
2. **Pre-aviso (30s):** "⏰ Faltan 3 turnos para ti. Ticket: C01"
3. **Turno activo (60s):** "🔔 ¡Es tu turno! Ticket: C01 - Dirígete a ventanilla"

---

## 🧪 **Testing**

### **Tests Unitarios**
```bash
cd ticketero
./mvnw test
```

### **Tests de Integración**
```bash
./mvnw test -Dspring.profiles.active=test
```

### **Test Manual de API**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Crear ticket de prueba
./test-api.sh
```

### **Cobertura de Código**
```bash
./mvnw jacoco:report
open target/site/jacoco/index.html
```

---

## 🚀 **Deployment**

### **Desarrollo Local**
```bash
# Ver guía completa
cat ticketero/docs/DEPLOYMENT.md

# Quick start
docker compose up -d
```

### **Producción con Docker**
```bash
# Configurar variables de producción
cp .env.prod.example .env.prod
# Editar .env.prod

# Deploy
docker compose -f docker-compose.prod.yml up -d --build
```

### **AWS con CDK**
```bash
cd ticketero-infra

# Configurar CDK (primera vez)
cdk bootstrap

# Deploy infraestructura
cdk deploy TicketeroStack

# Ver outputs
cdk outputs
```

**Servicios desplegados en AWS:**
- 🖥️ **ECS Fargate** - Aplicación containerizada
- 🗄️ **RDS PostgreSQL** - Base de datos gestionada
- 📨 **Amazon MQ** - RabbitMQ gestionado
- 🔍 **CloudWatch** - Logs y métricas
- 🌐 **Application Load Balancer** - Balanceador de carga

---

## 🔍 **Monitoreo**

### **Health Checks**
```bash
# API Health
curl http://localhost:8080/actuator/health

# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus
```

### **Dashboards**
- **Grafana:** http://localhost:3000 (admin/admin123)
- **RabbitMQ:** http://localhost:15672 (dev/dev123)
- **Prometheus:** http://localhost:9090

### **Logs**
```bash
# Logs de aplicación
docker compose logs -f api

# Logs de base de datos
docker compose logs -f postgres

# Logs de RabbitMQ
docker compose logs -f rabbitmq
```

---

## 🚨 **Troubleshooting**

### **Problemas Comunes**

| Problema | Solución |
|----------|----------|
| API no inicia | Verificar variables en `.env` |
| Telegram no envía | Validar `TELEGRAM_BOT_TOKEN` |
| BD no conecta | `docker compose restart postgres` |
| Puerto ocupado | Cambiar puerto en `docker-compose.yml` |

### **Comandos de Diagnóstico**
```bash
# Estado de servicios
docker compose ps

# Logs detallados
docker compose logs -f

# Reiniciar todo
docker compose down && docker compose up -d

# Limpiar volúmenes (⚠️ elimina datos)
docker compose down -v
```

### **Soporte**
- 📖 **Documentación:** `ticketero/docs/`
- 🐛 **Issues:** GitHub Issues
- 💬 **Chat:** Slack #ticketero-support

---

## 🤝 **Contribución**

### **Workflow de Desarrollo**
```bash
# 1. Fork y clone
git clone <your-fork>
cd ticketero-ia

# 2. Crear rama
git checkout -b feature/nueva-funcionalidad

# 3. Desarrollar
# Ver ticketero/docs/CODING-STANDARDS.md

# 4. Tests
cd ticketero && ./mvnw test

# 5. Commit
git commit -m "feat: agregar nueva funcionalidad"

# 6. Push y PR
git push origin feature/nueva-funcionalidad
```

### **Estándares**
- 📏 **Código:** Ver `ticketero/docs/CODING-STANDARDS.md`
- 🏗️ **Arquitectura:** Ver `ticketero/docs/ARCHITECTURE.md`
- 🚀 **Deployment:** Ver `ticketero/docs/DEPLOYMENT.md`

---

## 📚 **Documentación Detallada**

| Documento | Descripción | Audiencia | Tiempo |
|-----------|-------------|-----------|--------|
| [`ticketero/README.md`](ticketero/README.md) | Documentación técnica de la API | Desarrolladores | 15 min |
| [`ticketero/docs/ARCHITECTURE.md`](ticketero/docs/ARCHITECTURE.md) | Diseño del sistema y decisiones | Arquitectos | 45 min |
| [`ticketero/docs/api/`](ticketero/docs/api/) | Documentación completa de API | Frontend/Integradores | 20 min |
| [`ticketero/docs/operations/`](ticketero/docs/operations/) | Guías operacionales (RUNBOOK, etc.) | DevOps/SRE | 60 min |
| [`ticketero/docs/decisions/`](ticketero/docs/decisions/) | Architecture Decision Records | Arquitectos | 30 min |
| [`ticketero/CONTRIBUTING.md`](ticketero/CONTRIBUTING.md) | Guía de contribución | Desarrolladores | 30 min |
| [`ticketero/TESTING.md`](ticketero/TESTING.md) | Estrategia de testing | QA/Desarrolladores | 25 min |
| [`ticketero/CHANGELOG.md`](ticketero/CHANGELOG.md) | Historial de versiones | Todos | 10 min |
| [`ticketero-infra/README.md`](ticketero-infra/README.md) | Infraestructura CDK | DevOps/SRE | 15 min |

### **📖 Índice Completo**
Ver [`ticketero/docs/INDEX.md`](ticketero/docs/INDEX.md) para navegación completa por audiencia y flujos de lectura recomendados.

---

## 📊 **Métricas del Proyecto**

- ⚡ **Tiempo de setup:** < 5 minutos
- 🎯 **Onboarding:** < 2 horas
- 🧪 **Cobertura de tests:** 80%+
- 🚀 **Tiempo de deployment:** < 10 minutos
- 📱 **Latencia de notificaciones:** < 5 segundos

---

## 📄 **Licencia**

Este proyecto es para fines educativos como parte del programa de capacitación en desarrollo Java.

**Desarrollado con ❤️ para aprender Spring Boot, AWS y mejores prácticas de desarrollo**

---

## 🏷️ **Versión**

**v1.0.0** - Sistema completo funcional con notificaciones Telegram

**Próximas versiones:**
- v1.1.0 - Dashboard web administrativo
- v1.2.0 - Integración WhatsApp Business
- v2.0.0 - Microservicios y Kubernetes

---

**🎯 ¿Listo para empezar? Ejecuta `docker compose up -d` y comienza a crear tickets!**