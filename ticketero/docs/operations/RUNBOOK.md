# 📖 RUNBOOK - Ticketero API

> **Guía operacional completa para administración en producción**

---

## 🎯 **Información General**

### **Servicios Principales**
- **Ticketero API**: Puerto 8080
- **PostgreSQL**: Puerto 5432
- **RabbitMQ**: Puerto 5672 (Management: 15672)
- **Prometheus**: Puerto 9090
- **Grafana**: Puerto 3000

### **Ambientes**
- **Desarrollo**: http://localhost:8080
- **Staging**: https://api-staging.ticketero.com
- **Producción**: https://api.ticketero.com

---

## 🚀 **Procedimientos de Startup**

### **Inicio Completo del Sistema**

```bash
# 1. Verificar prerequisitos
docker --version
docker compose --version

# 2. Configurar variables de entorno
source .env.prod

# 3. Levantar infraestructura
docker compose -f docker-compose.prod.yml up -d postgres rabbitmq

# 4. Esperar que servicios estén listos
./scripts/wait-for-services.sh

# 5. Levantar aplicación
docker compose -f docker-compose.prod.yml up -d api

# 6. Verificar health checks
./scripts/verify-health.sh
```

### **Orden de Inicio de Servicios**
1. **PostgreSQL** (30-60s startup)
2. **RabbitMQ** (15-30s startup)
3. **Ticketero API** (30-45s startup)
4. **Prometheus** (5-10s startup)
5. **Grafana** (10-15s startup)

### **Verificación Post-Startup**
```bash
# Health checks
curl -f http://localhost:8080/actuator/health || echo "API DOWN"
curl -f http://localhost:9090/-/healthy || echo "Prometheus DOWN"
curl -f http://localhost:3000/api/health || echo "Grafana DOWN"

# Functional test
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"nationalId":"99999999","branchOffice":"Test","queue":"CAJA"}' \
  || echo "API FUNCTIONAL TEST FAILED"
```

---

## 🔄 **Procedimientos de Deployment**

### **Deployment de Nueva Versión**

```bash
# 1. Backup de base de datos
./scripts/backup-database.sh

# 2. Pull nueva imagen
docker pull ticketero-api:${NEW_VERSION}

# 3. Rolling update (zero downtime)
docker compose -f docker-compose.prod.yml up -d --no-deps api

# 4. Verificar health check
timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 2; done'

# 5. Smoke test
./scripts/smoke-test.sh

# 6. Rollback si falla
if [ $? -ne 0 ]; then
  echo "Deployment failed, rolling back..."
  docker compose -f docker-compose.prod.yml up -d --no-deps api:${PREVIOUS_VERSION}
fi
```

### **Rollback de Versión**
```bash
# 1. Identificar versión anterior
PREVIOUS_VERSION=$(docker images ticketero-api --format "table {{.Tag}}" | sed -n '2p')

# 2. Rollback
docker compose -f docker-compose.prod.yml up -d --no-deps api:${PREVIOUS_VERSION}

# 3. Verificar
curl -f http://localhost:8080/actuator/health

# 4. Notificar
echo "Rollback completed to version: ${PREVIOUS_VERSION}"
```

---

## 🔍 **Monitoreo y Alertas**

### **Métricas Críticas**

#### **API Health**
```bash
# Health check endpoint
curl http://localhost:8080/actuator/health

# Respuesta esperada:
# {"status":"UP","components":{"db":{"status":"UP"},"rabbit":{"status":"UP"}}}
```

#### **Performance Metrics**
```bash
# Response time (debe ser < 200ms p95)
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds

# Memory usage (debe ser < 80%)
curl -s http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes

# Database connections (debe ser < 80% del pool)
curl -s http://localhost:8080/actuator/prometheus | grep hikaricp_connections_active
```

#### **Business Metrics**
```bash
# Tickets creados hoy
curl -s http://localhost:8080/actuator/prometheus | grep tickets_created_total

# Mensajes enviados
curl -s http://localhost:8080/actuator/prometheus | grep telegram_messages_sent_total

# Errores de Telegram
curl -s http://localhost:8080/actuator/prometheus | grep telegram_errors_total
```

### **Alertas Configuradas**

| Métrica | Umbral | Severidad | Acción |
|---------|--------|-----------|--------|
| API Response Time | > 500ms p95 | Warning | Investigar performance |
| API Error Rate | > 5% | Critical | Escalate to dev team |
| Memory Usage | > 85% | Warning | Consider scaling |
| Database Connections | > 80% | Warning | Check connection leaks |
| Telegram Errors | > 10% | Warning | Check Telegram API |
| Disk Space | > 90% | Critical | Clean logs/expand disk |

---

## 💾 **Backup y Recovery**

### **Backup de Base de Datos**

#### **Backup Diario (Automatizado)**
```bash
#!/bin/bash
# /scripts/backup-database.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/postgres"
BACKUP_FILE="ticketero_backup_${DATE}.sql"

# Crear backup
docker exec ticketero-postgres pg_dump \
  -U ticketero_user \
  -d ticketero \
  --no-owner \
  --no-privileges > "${BACKUP_DIR}/${BACKUP_FILE}"

# Comprimir
gzip "${BACKUP_DIR}/${BACKUP_FILE}"

# Limpiar backups antiguos (mantener 30 días)
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: ${BACKUP_FILE}.gz"
```

#### **Backup Manual**
```bash
# Backup completo
docker exec ticketero-postgres pg_dump \
  -U ticketero_user \
  -d ticketero \
  --clean --if-exists > backup_$(date +%Y%m%d).sql

# Backup solo datos
docker exec ticketero-postgres pg_dump \
  -U ticketero_user \
  -d ticketero \
  --data-only > data_backup_$(date +%Y%m%d).sql
```

### **Recovery de Base de Datos**

#### **Recovery Completo**
```bash
# 1. Detener aplicación
docker compose -f docker-compose.prod.yml stop api

# 2. Restaurar backup
docker exec -i ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero < backup_20241125.sql

# 3. Verificar integridad
docker exec ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero \
  -c "SELECT COUNT(*) FROM ticket;"

# 4. Reiniciar aplicación
docker compose -f docker-compose.prod.yml start api
```

#### **Recovery Point-in-Time**
```bash
# Si tienes WAL archiving habilitado
docker exec ticketero-postgres pg_basebackup \
  -U ticketero_user \
  -D /backup/base \
  -Ft -z -P
```

---

## 📊 **Mantenimiento Rutinario**

### **Tareas Diarias**
```bash
#!/bin/bash
# /scripts/daily-maintenance.sh

echo "=== Daily Maintenance $(date) ==="

# 1. Verificar health checks
curl -f http://localhost:8080/actuator/health || echo "ALERT: API DOWN"

# 2. Verificar espacio en disco
df -h | awk '$5 > 80 {print "ALERT: Disk usage high on " $6 ": " $5}'

# 3. Verificar logs de errores
ERROR_COUNT=$(docker logs ticketero-api --since 24h 2>&1 | grep -c ERROR)
if [ $ERROR_COUNT -gt 10 ]; then
  echo "ALERT: High error count in last 24h: $ERROR_COUNT"
fi

# 4. Backup automático
./backup-database.sh

# 5. Limpiar logs antiguos
docker logs ticketero-api --since 7d > /dev/null 2>&1
```

### **Tareas Semanales**
```bash
#!/bin/bash
# /scripts/weekly-maintenance.sh

echo "=== Weekly Maintenance $(date) ==="

# 1. Actualizar estadísticas de BD
docker exec ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero \
  -c "ANALYZE;"

# 2. Verificar fragmentación de índices
docker exec ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero \
  -c "REINDEX DATABASE ticketero;"

# 3. Limpiar mensajes antiguos (> 30 días)
docker exec ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero \
  -c "DELETE FROM mensaje WHERE created_at < NOW() - INTERVAL '30 days';"

# 4. Verificar tamaño de BD
docker exec ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero \
  -c "SELECT pg_size_pretty(pg_database_size('ticketero'));"
```

### **Tareas Mensuales**
```bash
#!/bin/bash
# /scripts/monthly-maintenance.sh

echo "=== Monthly Maintenance $(date) ==="

# 1. Vacuum completo de BD
docker exec ticketero-postgres psql \
  -U ticketero_user \
  -d ticketero \
  -c "VACUUM FULL;"

# 2. Rotar logs de aplicación
docker logs ticketero-api > /logs/app_$(date +%Y%m).log
docker compose -f docker-compose.prod.yml restart api

# 3. Actualizar imágenes base
docker pull postgres:16-alpine
docker pull rabbitmq:3.13-management-alpine

# 4. Verificar certificados SSL (si aplica)
openssl x509 -in /certs/ticketero.crt -noout -dates
```

---

## 🔧 **Escalado y Performance**

### **Escalado Horizontal**
```bash
# Escalar API a 3 instancias
docker compose -f docker-compose.prod.yml up -d --scale api=3

# Verificar load balancing
for i in {1..10}; do
  curl -s http://localhost:8080/actuator/info | jq -r '.hostname'
done
```

### **Escalado Vertical**
```yaml
# docker-compose.prod.yml
services:
  api:
    deploy:
      resources:
        limits:
          cpus: '2.0'      # Aumentar CPU
          memory: 4G       # Aumentar memoria
        reservations:
          cpus: '1.0'
          memory: 2G
```

### **Optimización de Performance**
```bash
# 1. Ajustar pool de conexiones BD
# En application-prod.yml:
# spring.datasource.hikari.maximum-pool-size=20

# 2. Ajustar JVM heap
# JAVA_OPTS="-Xmx2g -Xms1g"

# 3. Monitorear GC
docker exec ticketero-api jstat -gc 1 5s
```

---

## 🚨 **Procedimientos de Emergencia**

### **API No Responde**
```bash
# 1. Verificar contenedor
docker ps | grep ticketero-api

# 2. Ver logs
docker logs ticketero-api --tail 100

# 3. Restart rápido
docker compose -f docker-compose.prod.yml restart api

# 4. Si persiste, rollback
docker compose -f docker-compose.prod.yml up -d api:previous-version
```

### **Base de Datos Corrupta**
```bash
# 1. Detener aplicación
docker compose -f docker-compose.prod.yml stop api

# 2. Verificar integridad
docker exec ticketero-postgres pg_dump \
  -U ticketero_user \
  -d ticketero \
  --schema-only > /tmp/schema_check.sql

# 3. Restaurar desde backup más reciente
./scripts/restore-database.sh backup_latest.sql

# 4. Reiniciar aplicación
docker compose -f docker-compose.prod.yml start api
```

### **Telegram API Down**
```bash
# 1. Verificar conectividad
curl -f https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe

# 2. Ver mensajes pendientes en RabbitMQ
curl -u dev:dev123 http://localhost:15672/api/queues

# 3. Los mensajes se reintentarán automáticamente
# 4. Monitorear recovery
watch 'curl -s http://localhost:8080/actuator/prometheus | grep telegram_errors_total'
```

---

## 📞 **Contactos y Escalación**

### **Niveles de Soporte**

#### **Nivel 1 - Operaciones**
- **Responsabilidad**: Monitoreo, alertas, procedimientos básicos
- **Horario**: 24/7
- **Contacto**: ops@ticketero.com
- **Slack**: #ops-alerts

#### **Nivel 2 - DevOps**
- **Responsabilidad**: Deployment, infraestructura, performance
- **Horario**: 8:00-20:00
- **Contacto**: devops@ticketero.com
- **Slack**: #devops-support

#### **Nivel 3 - Desarrollo**
- **Responsabilidad**: Bugs de aplicación, lógica de negocio
- **Horario**: 9:00-18:00
- **Contacto**: dev@ticketero.com
- **Slack**: #dev-support

### **Matriz de Escalación**

| Severidad | Tiempo Respuesta | Escalación |
|-----------|------------------|------------|
| **Critical** | 15 minutos | Nivel 1 → Nivel 2 → Nivel 3 |
| **High** | 1 hora | Nivel 1 → Nivel 2 |
| **Medium** | 4 horas | Nivel 1 |
| **Low** | 24 horas | Nivel 1 |

---

## 📋 **Checklists**

### **Checklist de Deployment**
- [ ] Backup de BD realizado
- [ ] Variables de entorno verificadas
- [ ] Health checks pasando
- [ ] Smoke tests ejecutados
- [ ] Monitoreo funcionando
- [ ] Rollback plan preparado
- [ ] Stakeholders notificados

### **Checklist de Incident Response**
- [ ] Incident declarado en Slack
- [ ] Logs capturados
- [ ] Métricas revisadas
- [ ] Workaround implementado (si aplica)
- [ ] Root cause identificado
- [ ] Fix implementado
- [ ] Post-mortem programado

---

**Mantenido por:** Equipo de DevOps  
**Última actualización:** 2024-11-25  
**Próxima revisión:** 2025-02-25