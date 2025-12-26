# 🔧 TROUBLESHOOTING - Ticketero API

> **Guía completa para diagnóstico y resolución de problemas**

---

## 🚨 **Problemas Críticos**

### **🔴 API No Responde**

#### **Síntomas**
- Health check falla: `curl http://localhost:8080/actuator/health`
- Timeout en requests
- Error 502/503 en load balancer

#### **Diagnóstico**
```bash
# 1. Verificar contenedor
docker ps | grep ticketero-api
# Estado esperado: Up X minutes

# 2. Ver logs recientes
docker logs ticketero-api --tail 50 --timestamps

# 3. Verificar recursos
docker stats ticketero-api --no-stream

# 4. Verificar conectividad de red
docker exec ticketero-api nc -zv postgres 5432
docker exec ticketero-api nc -zv rabbitmq 5672
```

#### **Soluciones**

**Solución 1: Restart Simple**
```bash
docker compose -f docker-compose.prod.yml restart api
# Tiempo esperado: 30-45 segundos
```

**Solución 2: Recrear Contenedor**
```bash
docker compose -f docker-compose.prod.yml up -d --force-recreate api
```

**Solución 3: Rollback**
```bash
# Si el problema persiste
PREVIOUS_VERSION=$(docker images ticketero-api --format "{{.Tag}}" | sed -n '2p')
docker compose -f docker-compose.prod.yml up -d api:${PREVIOUS_VERSION}
```

---

### **🔴 Base de Datos No Conecta**

#### **Síntomas**
- Error en logs: `Connection refused` o `Connection timeout`
- Health check DB: DOWN
- API arranca pero falla en primer request

#### **Diagnóstico**
```bash
# 1. Verificar contenedor PostgreSQL
docker ps | grep postgres

# 2. Ver logs de PostgreSQL
docker logs ticketero-postgres --tail 50

# 3. Verificar conectividad
docker exec ticketero-api nc -zv postgres 5432

# 4. Test de conexión manual
docker exec -it ticketero-postgres psql -U ticketero_user -d ticketero -c "SELECT 1;"
```

#### **Soluciones**

**Solución 1: Restart PostgreSQL**
```bash
docker compose -f docker-compose.prod.yml restart postgres
# Esperar 30-60 segundos para startup completo
```

**Solución 2: Verificar Configuración**
```bash
# Verificar variables de entorno
docker exec ticketero-api env | grep DATABASE

# Verificar archivo de configuración
docker exec ticketero-postgres cat /var/lib/postgresql/data/postgresql.conf | grep listen_addresses
```

**Solución 3: Recovery de BD**
```bash
# Si hay corrupción de datos
docker compose -f docker-compose.prod.yml stop api
./scripts/restore-database.sh backup_latest.sql
docker compose -f docker-compose.prod.yml start api
```

---

### **🔴 RabbitMQ No Procesa Mensajes**

#### **Síntomas**
- Mensajes se acumulan en colas
- Notificaciones Telegram no se envían
- Error en logs: `Connection refused` a RabbitMQ

#### **Diagnóstico**
```bash
# 1. Verificar contenedor RabbitMQ
docker ps | grep rabbitmq

# 2. Ver estado de colas
curl -u dev:dev123 http://localhost:15672/api/queues | jq '.[].messages'

# 3. Verificar conectividad
docker exec ticketero-api nc -zv rabbitmq 5672

# 4. Ver logs de RabbitMQ
docker logs ticketero-rabbitmq --tail 50
```

#### **Soluciones**

**Solución 1: Restart RabbitMQ**
```bash
docker compose -f docker-compose.prod.yml restart rabbitmq
# Esperar 15-30 segundos
```

**Solución 2: Purgar Colas**
```bash
# Si hay demasiados mensajes acumulados
curl -u dev:dev123 -X DELETE http://localhost:15672/api/queues/%2F/telegram-notifications/contents
```

**Solución 3: Recrear Colas**
```bash
# En caso de corrupción
docker exec ticketero-rabbitmq rabbitmqctl delete_queue telegram-notifications
docker compose -f docker-compose.prod.yml restart api  # Recreará las colas
```

---

## ⚠️ **Problemas de Performance**

### **🟡 Response Time Alto**

#### **Síntomas**
- API responde > 500ms
- Timeout en algunos requests
- Usuarios reportan lentitud

#### **Diagnóstico**
```bash
# 1. Verificar métricas de response time
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds

# 2. Verificar uso de CPU/Memory
docker stats ticketero-api --no-stream

# 3. Verificar conexiones de BD
curl -s http://localhost:8080/actuator/prometheus | grep hikaricp_connections

# 4. Verificar queries lentas en PostgreSQL
docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;"
```

#### **Soluciones**

**Solución 1: Optimizar Pool de Conexiones**
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
```

**Solución 2: Ajustar JVM**
```bash
# Aumentar heap size
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"
```

**Solución 3: Escalado Horizontal**
```bash
docker compose -f docker-compose.prod.yml up -d --scale api=3
```

---

### **🟡 Memory Leak**

#### **Síntomas**
- Uso de memoria crece constantemente
- OutOfMemoryError en logs
- Contenedor se reinicia frecuentemente

#### **Diagnóstico**
```bash
# 1. Monitorear memoria en tiempo real
watch 'docker stats ticketero-api --no-stream'

# 2. Generar heap dump
docker exec ticketero-api jcmd 1 GC.run_finalization
docker exec ticketero-api jcmd 1 VM.gc

# 3. Verificar métricas JVM
curl -s http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes
```

#### **Soluciones**

**Solución 1: Restart Programado**
```bash
# Restart temporal mientras se investiga
docker compose -f docker-compose.prod.yml restart api
```

**Solución 2: Ajustar GC**
```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError"
```

---

## 🟠 **Problemas de Integración**

### **🟡 Telegram API Falla**

#### **Síntomas**
- Mensajes no se envían
- Error 429 (Rate Limit) en logs
- Error 401 (Unauthorized)

#### **Diagnóstico**
```bash
# 1. Test manual de Telegram API
curl -X GET "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe"

# 2. Verificar rate limiting
curl -s http://localhost:8080/actuator/prometheus | grep telegram_errors_total

# 3. Ver logs específicos de Telegram
docker logs ticketero-api 2>&1 | grep -i telegram | tail -20
```

#### **Soluciones**

**Solución 1: Verificar Token**
```bash
# Verificar que el token es válido
curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe"
# Respuesta esperada: {"ok":true,"result":{"id":...}}
```

**Solución 2: Rate Limiting**
```bash
# Si hay error 429, esperar y reintentar
# Los mensajes se reintentarán automáticamente
# Verificar configuración de rate limit en application.yml
```

**Solución 3: Fallback Manual**
```bash
# Enviar mensajes pendientes manualmente
curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
  -d "chat_id=${CHAT_ID}&text=Test message"
```

---

## 🔍 **Comandos de Diagnóstico**

### **Logs y Debugging**

```bash
# Ver logs en tiempo real
docker logs -f ticketero-api

# Buscar errores específicos
docker logs ticketero-api 2>&1 | grep -i error | tail -20

# Logs con timestamp
docker logs ticketero-api --timestamps --since 1h

# Logs de todos los servicios
docker compose -f docker-compose.prod.yml logs --tail 50

# Filtrar por servicio
docker compose -f docker-compose.prod.yml logs api --tail 100
```

### **Estado del Sistema**

```bash
# Estado de contenedores
docker compose -f docker-compose.prod.yml ps

# Uso de recursos
docker stats --no-stream

# Espacio en disco
df -h

# Procesos del sistema
top -p $(docker inspect -f '{{.State.Pid}}' ticketero-api)
```

### **Conectividad de Red**

```bash
# Test de conectividad entre servicios
docker exec ticketero-api nc -zv postgres 5432
docker exec ticketero-api nc -zv rabbitmq 5672
docker exec ticketero-api nc -zv rabbitmq 15672

# DNS resolution
docker exec ticketero-api nslookup postgres
docker exec ticketero-api nslookup rabbitmq

# Test de conectividad externa
docker exec ticketero-api curl -I https://api.telegram.org
```

---

## 📊 **Métricas y Monitoreo**

### **Health Checks Detallados**

```bash
# Health check completo
curl -s http://localhost:8080/actuator/health | jq '.'

# Health check específico de BD
curl -s http://localhost:8080/actuator/health/db | jq '.'

# Health check de RabbitMQ
curl -s http://localhost:8080/actuator/health/rabbit | jq '.'

# Info de la aplicación
curl -s http://localhost:8080/actuator/info | jq '.'
```

### **Métricas de Performance**

```bash
# Response times
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds_sum

# Throughput
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds_count

# Error rate
curl -s http://localhost:8080/actuator/prometheus | grep 'http_server_requests_seconds_count.*status="[45]'

# JVM metrics
curl -s http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes
curl -s http://localhost:8080/actuator/prometheus | grep jvm_gc_pause_seconds
```

### **Business Metrics**

```bash
# Tickets creados
curl -s http://localhost:8080/actuator/prometheus | grep tickets_created_total

# Mensajes enviados
curl -s http://localhost:8080/actuator/prometheus | grep telegram_messages_sent_total

# Errores de negocio
curl -s http://localhost:8080/actuator/prometheus | grep business_errors_total
```

---

## 🛠️ **Scripts de Utilidad**

### **Script de Health Check Completo**

```bash
#!/bin/bash
# /scripts/health-check.sh

echo "=== Health Check $(date) ==="

# API Health
if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
  echo "✅ API: UP"
else
  echo "❌ API: DOWN"
  exit 1
fi

# Database
if docker exec ticketero-postgres pg_isready -U ticketero_user > /dev/null; then
  echo "✅ Database: UP"
else
  echo "❌ Database: DOWN"
  exit 1
fi

# RabbitMQ
if curl -f -s -u dev:dev123 http://localhost:15672/api/overview > /dev/null; then
  echo "✅ RabbitMQ: UP"
else
  echo "❌ RabbitMQ: DOWN"
  exit 1
fi

# Functional test
RESPONSE=$(curl -s -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"nationalId":"99999999","branchOffice":"HealthCheck","queue":"CAJA"}')

if echo "$RESPONSE" | grep -q "identificador"; then
  echo "✅ Functional Test: PASS"
else
  echo "❌ Functional Test: FAIL"
  echo "Response: $RESPONSE"
  exit 1
fi

echo "✅ All systems operational"
```

### **Script de Limpieza**

```bash
#!/bin/bash
# /scripts/cleanup.sh

echo "=== Cleanup $(date) ==="

# Limpiar logs antiguos
docker system prune -f --filter "until=168h"

# Limpiar imágenes no utilizadas
docker image prune -f --filter "until=168h"

# Limpiar datos de test antiguos
docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
DELETE FROM ticket 
WHERE branch_office = 'HealthCheck' 
AND created_at < NOW() - INTERVAL '1 day';"

echo "Cleanup completed"
```

---

## 📋 **Checklist de Troubleshooting**

### **Problema Reportado**
- [ ] Severidad identificada (Critical/High/Medium/Low)
- [ ] Síntomas documentados
- [ ] Tiempo de inicio del problema
- [ ] Usuarios afectados estimados

### **Diagnóstico Inicial**
- [ ] Health checks ejecutados
- [ ] Logs revisados (últimos 30 min)
- [ ] Métricas verificadas
- [ ] Conectividad de red probada

### **Investigación**
- [ ] Root cause identificado
- [ ] Impacto evaluado
- [ ] Workaround implementado (si aplica)
- [ ] Timeline de resolución estimado

### **Resolución**
- [ ] Fix implementado
- [ ] Verificación post-fix realizada
- [ ] Monitoreo adicional configurado
- [ ] Documentación actualizada

### **Post-Mortem**
- [ ] Incident report creado
- [ ] Lessons learned documentadas
- [ ] Mejoras preventivas identificadas
- [ ] Stakeholders notificados

---

## 📞 **Escalación**

### **Cuándo Escalar**

| Situación | Tiempo | Acción |
|-----------|--------|--------|
| API completamente down | Inmediato | Escalar a DevOps |
| Performance degradado | 30 min | Escalar a DevOps |
| Errores de aplicación | 1 hora | Escalar a Development |
| Problemas de infraestructura | 15 min | Escalar a DevOps |

### **Información para Escalación**
- Descripción del problema
- Síntomas observados
- Pasos de troubleshooting realizados
- Logs relevantes
- Métricas afectadas
- Impacto en usuarios

---

**Mantenido por:** Equipo de DevOps  
**Última actualización:** 2024-11-25  
**Próxima revisión:** 2025-02-25