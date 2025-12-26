# 🛠️ Guías Operacionales - Ticketero API

> **Documentación completa para operación y mantenimiento en producción**

---

## 📁 **Contenido de este Directorio**

| Documento | Descripción | Audiencia |
|-----------|-------------|-----------|
| [`RUNBOOK.md`](RUNBOOK.md) | Procedimientos operacionales completos | DevOps/SRE |
| [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) | Diagnóstico y resolución de problemas | Ops/DevOps |
| [`SECURITY.md`](SECURITY.md) | Configuración y procedimientos de seguridad | Security/DevOps |

---

## 🎯 **Audiencias y Casos de Uso**

### **👨‍💻 Operadores (24/7)**
- **TROUBLESHOOTING.md** → Resolver incidentes
- **RUNBOOK.md** → Procedimientos de emergencia
- **SECURITY.md** → Respuesta a incidentes de seguridad

### **🔧 DevOps Engineers**
- **RUNBOOK.md** → Deployment y mantenimiento
- **TROUBLESHOOTING.md** → Diagnóstico avanzado
- **SECURITY.md** → Hardening y compliance

### **🛡️ Security Team**
- **SECURITY.md** → Configuración y monitoreo
- **RUNBOOK.md** → Procedimientos de backup
- **TROUBLESHOOTING.md** → Análisis forense

---

## 🚨 **Procedimientos de Emergencia**

### **Sistema Completamente Down**
1. **Diagnóstico rápido**: [`TROUBLESHOOTING.md#api-no-responde`](TROUBLESHOOTING.md#-api-no-responde)
2. **Procedimientos de recovery**: [`RUNBOOK.md#procedimientos-de-emergencia`](RUNBOOK.md#-procedimientos-de-emergencia)
3. **Escalación**: [`TROUBLESHOOTING.md#escalación`](TROUBLESHOOTING.md#-escalación)

### **Incidente de Seguridad**
1. **Respuesta inmediata**: [`SECURITY.md#incident-response`](SECURITY.md#-incident-response)
2. **Aislamiento**: [`SECURITY.md#procedimiento-de-respuesta`](SECURITY.md#procedimiento-de-respuesta)
3. **Forensics**: [`SECURITY.md#forensics-y-análisis`](SECURITY.md#forensics-y-análisis)

### **Performance Degradado**
1. **Diagnóstico**: [`TROUBLESHOOTING.md#response-time-alto`](TROUBLESHOOTING.md#-response-time-alto)
2. **Escalado**: [`RUNBOOK.md#escalado-y-performance`](RUNBOOK.md#-escalado-y-performance)
3. **Optimización**: [`TROUBLESHOOTING.md#optimización-de-performance`](TROUBLESHOOTING.md#optimización-de-performance)

---

## 📊 **Métricas y SLAs**

### **Objetivos de Nivel de Servicio**
- **Disponibilidad**: 99.9% (8.76 horas downtime/año)
- **Response Time**: < 200ms (p95)
- **Error Rate**: < 1%
- **Recovery Time**: < 15 minutos

### **Métricas Clave**
```bash
# Health check general
curl http://localhost:8080/actuator/health

# Métricas de performance
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds

# Métricas de negocio
curl -s http://localhost:8080/actuator/prometheus | grep tickets_created_total
```

---

## 🔄 **Procedimientos Rutinarios**

### **Diarios**
- [ ] Verificar health checks
- [ ] Revisar logs de errores
- [ ] Backup automático de BD
- [ ] Verificar espacio en disco

### **Semanales**
- [ ] Mantenimiento de BD (ANALYZE)
- [ ] Limpiar mensajes antiguos
- [ ] Verificar métricas de performance
- [ ] Revisar alertas y umbrales

### **Mensuales**
- [ ] Vacuum completo de BD
- [ ] Rotar logs de aplicación
- [ ] Actualizar imágenes base
- [ ] Revisar configuración de seguridad

---

## 🛡️ **Configuración de Seguridad**

### **Checklist de Seguridad**
- [ ] Usuarios no-root configurados
- [ ] Secrets rotados regularmente
- [ ] SSL/TLS habilitado
- [ ] Logs de auditoría activos
- [ ] Monitoreo de seguridad configurado
- [ ] Incident response plan actualizado

### **Vulnerabilidades Conocidas**
Ver [`SECURITY.md#vulnerabilidades-conocidas`](SECURITY.md#-vulnerabilidades-conocidas)

---

## 📞 **Contactos y Escalación**

### **Niveles de Soporte**

| Nivel | Responsabilidad | Horario | Contacto |
|-------|----------------|---------|----------|
| **L1** | Monitoreo básico | 24/7 | ops@ticketero.com |
| **L2** | DevOps/Infraestructura | 8-20h | devops@ticketero.com |
| **L3** | Desarrollo | 9-18h | dev@ticketero.com |
| **Security** | Incidentes de seguridad | 24/7 | security@ticketero.com |

### **Canales de Comunicación**
- **Slack**: #ops-alerts, #devops-support, #security-alerts
- **Email**: Para escalaciones formales
- **PagerDuty**: Para incidentes críticos (futuro)

---

## 🔗 **Enlaces Relacionados**

### **Documentación Técnica**
- **README Principal**: [`../../README.md`](../../README.md)
- **Arquitectura**: [`../ARCHITECTURE.md`](../ARCHITECTURE.md)
- **API Documentation**: [`../api/`](../api/)
- **ADRs**: [`../decisions/`](../decisions/)

### **Herramientas de Monitoreo**
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **RabbitMQ Management**: http://localhost:15672 (dev/dev123)

### **Scripts de Utilidad**
- **Health Check**: `/scripts/health-check.sh`
- **Backup**: `/scripts/backup-database.sh`
- **Cleanup**: `/scripts/cleanup.sh`
- **Security Updates**: `/scripts/security-updates.sh`

---

## 📋 **Templates y Checklists**

### **Incident Report Template**
```markdown
# Incident Report - [YYYY-MM-DD]

## Summary
- **Start Time**: 
- **End Time**: 
- **Duration**: 
- **Severity**: Critical/High/Medium/Low
- **Services Affected**: 

## Timeline
- **HH:MM** - Issue detected
- **HH:MM** - Investigation started
- **HH:MM** - Root cause identified
- **HH:MM** - Fix implemented
- **HH:MM** - Service restored

## Root Cause
[Detailed explanation]

## Resolution
[Steps taken to resolve]

## Lessons Learned
[What we learned and how to prevent]

## Action Items
- [ ] Item 1 - Owner - Due Date
- [ ] Item 2 - Owner - Due Date
```

### **Deployment Checklist**
- [ ] Backup realizado
- [ ] Variables verificadas
- [ ] Health checks pasando
- [ ] Smoke tests OK
- [ ] Rollback plan listo
- [ ] Stakeholders notificados

---

## 📈 **Mejora Continua**

### **Métricas de Operaciones**
- **MTTR** (Mean Time To Recovery): < 15 min
- **MTBF** (Mean Time Between Failures): > 30 días
- **Deployment Success Rate**: > 95%
- **Incident Response Time**: < 5 min

### **Revisiones Regulares**
- **Semanal**: Revisión de incidentes y métricas
- **Mensual**: Actualización de procedimientos
- **Trimestral**: Revisión completa de documentación
- **Anual**: Disaster recovery testing

---

**Mantenido por:** Equipo de DevOps  
**Última actualización:** 2024-11-25  
**Próxima revisión:** 2025-02-25