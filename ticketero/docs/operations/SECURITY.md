# 🔒 SECURITY - Ticketero API

> **Guía completa de seguridad para el sistema de gestión de tickets**

---

## 🎯 **Postura de Seguridad**

### **Principios de Seguridad**
1. **Defense in Depth** - Múltiples capas de seguridad
2. **Least Privilege** - Acceso mínimo necesario
3. **Zero Trust** - Verificar siempre, nunca confiar
4. **Security by Design** - Seguridad desde el diseño
5. **Continuous Monitoring** - Monitoreo constante

### **Clasificación de Datos**
- **Público**: Documentación, APIs públicas
- **Interno**: Logs, métricas, configuraciones
- **Confidencial**: IDs nacionales, teléfonos
- **Restringido**: Tokens, passwords, keys

---

## 🛡️ **Configuración de Seguridad**

### **Contenedores Docker**

#### **Usuario No-Root**
```dockerfile
# Dockerfile - Usuario no privilegiado
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
USER spring:spring
```

#### **Configuración de Seguridad**
```yaml
# docker-compose.prod.yml
services:
  api:
    security_opt:
      - no-new-privileges:true
    read_only: true
    tmpfs:
      - /tmp
      - /var/log
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
```

### **PostgreSQL**

#### **Configuración Segura**
```bash
# postgresql.conf
ssl = on
ssl_cert_file = '/certs/server.crt'
ssl_key_file = '/certs/server.key'
ssl_ca_file = '/certs/ca.crt'

# Logging de conexiones
log_connections = on
log_disconnections = on
log_statement = 'mod'  # Log modificaciones
```

#### **Usuarios y Permisos**
```sql
-- Crear usuario de aplicación con permisos mínimos
CREATE USER ticketero_app WITH PASSWORD 'strong_password_here';

-- Permisos específicos por tabla
GRANT SELECT, INSERT, UPDATE ON ticket TO ticketero_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON mensaje TO ticketero_app;
GRANT USAGE ON SEQUENCE ticket_id_seq TO ticketero_app;
GRANT USAGE ON SEQUENCE mensaje_id_seq TO ticketero_app;

-- NO otorgar permisos de DDL
REVOKE CREATE ON SCHEMA public FROM ticketero_app;
```

### **RabbitMQ**

#### **Configuración Segura**
```bash
# rabbitmq.conf
ssl_options.cacertfile = /certs/ca.crt
ssl_options.certfile = /certs/server.crt
ssl_options.keyfile = /certs/server.key
ssl_options.verify = verify_peer
ssl_options.fail_if_no_peer_cert = true

# Disable guest user
loopback_users.guest = false
```

#### **Usuarios y VHosts**
```bash
# Crear usuario específico para la aplicación
rabbitmqctl add_user ticketero_app secure_password
rabbitmqctl set_permissions -p / ticketero_app ".*" ".*" ".*"

# Eliminar usuario guest
rabbitmqctl delete_user guest
```

---

## 🔐 **Gestión de Secretos**

### **Variables de Entorno Seguras**

#### **Desarrollo**
```bash
# .env (NO commitear)
DATABASE_PASSWORD=dev123
RABBITMQ_PASSWORD=dev123
TELEGRAM_BOT_TOKEN=123456789:ABCDEF...
```

#### **Producción**
```bash
# Usar Docker Secrets o Vault
docker secret create db_password db_password.txt
docker secret create telegram_token telegram_token.txt

# docker-compose.prod.yml
services:
  api:
    secrets:
      - db_password
      - telegram_token
    environment:
      DATABASE_PASSWORD_FILE: /run/secrets/db_password
      TELEGRAM_BOT_TOKEN_FILE: /run/secrets/telegram_token
```

### **Rotación de Secretos**

#### **Passwords de Base de Datos**
```bash
#!/bin/bash
# /scripts/rotate-db-password.sh

NEW_PASSWORD=$(openssl rand -base64 32)

# 1. Cambiar password en PostgreSQL
docker exec ticketero-postgres psql -U postgres -c "
ALTER USER ticketero_user PASSWORD '$NEW_PASSWORD';"

# 2. Actualizar secret
echo "$NEW_PASSWORD" | docker secret create db_password_new -

# 3. Actualizar servicio
docker service update --secret-rm db_password --secret-add db_password_new ticketero_api

# 4. Limpiar secret anterior
docker secret rm db_password
```

#### **Tokens de Telegram**
```bash
# Regenerar token en @BotFather
# 1. /revoke en @BotFather
# 2. Obtener nuevo token
# 3. Actualizar configuración
# 4. Restart aplicación
```

---

## 🔍 **Monitoreo de Seguridad**

### **Logs de Seguridad**

#### **Configuración de Logging**
```yaml
# application-prod.yml
logging:
  level:
    org.springframework.security: INFO
    org.springframework.web.filter.CommonsRequestLoggingFilter: INFO
    com.example.ticketero.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n"
```

#### **Eventos a Monitorear**
- Intentos de acceso no autorizados
- Fallos de autenticación repetidos
- Acceso a endpoints sensibles
- Cambios en configuración de seguridad
- Errores de validación de datos

### **Métricas de Seguridad**

```bash
# Requests fallidos por validación
curl -s http://localhost:8080/actuator/prometheus | grep 'http_server_requests_seconds_count.*status="400"'

# Errores de autenticación (cuando se implemente)
curl -s http://localhost:8080/actuator/prometheus | grep 'authentication_failures_total'

# Acceso a endpoints administrativos
curl -s http://localhost:8080/actuator/prometheus | grep 'admin_endpoint_access_total'
```

### **Alertas de Seguridad**

| Evento | Umbral | Acción |
|--------|--------|--------|
| Requests 400 | > 100/min | Investigar posible ataque |
| Requests 401 | > 50/min | Posible brute force |
| Requests 403 | > 20/min | Acceso no autorizado |
| Errores de validación | > 200/min | Posible injection attack |
| Conexiones DB fallidas | > 10/min | Investigar credenciales |

---

## 🚨 **Vulnerabilidades Conocidas**

### **Mitigaciones Implementadas**

#### **SQL Injection**
```java
// ✅ CORRECTO - Usar JPA/Hibernate con parámetros
@Query("SELECT t FROM Ticket t WHERE t.nationalId = :nationalId")
Optional<Ticket> findByNationalId(@Param("nationalId") String nationalId);

// ❌ NUNCA - Concatenación de strings
// String sql = "SELECT * FROM ticket WHERE national_id = '" + nationalId + "'";
```

#### **XSS (Cross-Site Scripting)**
```java
// ✅ CORRECTO - Validación de entrada
@Pattern(regexp = "^[0-9]{8,12}$", message = "ID nacional inválido")
private String nationalId;

// ✅ CORRECTO - Escape de salida (automático con Spring Boot)
@RestController
public class TicketController {
    // Spring Boot escapa automáticamente JSON responses
}
```

#### **CSRF (Cross-Site Request Forgery)**
```java
// ✅ CORRECTO - CSRF protection habilitada
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .build();
    }
}
```

### **Vulnerabilidades Pendientes**

#### **Rate Limiting**
```yaml
# TODO: Implementar rate limiting
# application-prod.yml
bucket4j:
  enabled: true
  filters:
    - cache-name: rate-limit-buckets
      url: /api/.*
      rate-limits:
        - bandwidths:
            - capacity: 100
              time: 1
              unit: minutes
```

#### **Input Validation Avanzada**
```java
// TODO: Validadores custom más estrictos
@ValidNationalId  // Custom validator
@ValidPhoneNumber  // Custom validator
```

---

## 🔒 **Hardening del Sistema**

### **Sistema Operativo**

#### **Updates de Seguridad**
```bash
#!/bin/bash
# /scripts/security-updates.sh

# Actualizar imágenes base
docker pull postgres:16-alpine
docker pull rabbitmq:3.13-management-alpine
docker pull eclipse-temurin:21-jre-alpine

# Verificar vulnerabilidades
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image ticketero-api:latest
```

#### **Configuración de Red**
```yaml
# docker-compose.prod.yml
networks:
  ticketero-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
    driver_opts:
      com.docker.network.bridge.enable_icc: "false"
```

### **Aplicación**

#### **Headers de Seguridad**
```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
                .and())
            .build();
    }
}
```

#### **Validación Estricta**
```java
@RestController
@Validated
public class TicketController {
    
    @PostMapping("/api/tickets")
    public ResponseEntity<TicketResponse> crearTicket(
        @Valid @RequestBody TicketRequest request,
        HttpServletRequest httpRequest
    ) {
        // Log request para auditoría
        log.info("Ticket creation request from IP: {}", 
                getClientIpAddress(httpRequest));
        
        return ResponseEntity.ok(ticketService.crearTicket(request));
    }
}
```

---

## 🔐 **Compliance y Auditoría**

### **GDPR/Protección de Datos**

#### **Datos Personales Identificados**
- **ID Nacional**: Pseudonimizado en logs
- **Teléfono**: Enmascarado en logs
- **Ubicación**: Solo nombre de sucursal

#### **Implementación de Privacidad**
```java
@Component
public class DataMaskingService {
    
    public String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() < 4) {
            return "****";
        }
        return nationalId.substring(0, 2) + "****" + 
               nationalId.substring(nationalId.length() - 2);
    }
    
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
```

### **Auditoría de Acceso**

#### **Logging de Auditoría**
```java
@Component
@Slf4j
public class AuditLogger {
    
    public void logTicketCreation(String nationalId, String branchOffice, String clientIp) {
        log.info("AUDIT: Ticket created - NationalId: {}, Branch: {}, IP: {}", 
                dataMaskingService.maskNationalId(nationalId), 
                branchOffice, 
                clientIp);
    }
    
    public void logTicketAccess(String ticketId, String clientIp) {
        log.info("AUDIT: Ticket accessed - ID: {}, IP: {}", ticketId, clientIp);
    }
}
```

### **Retención de Datos**

#### **Política de Retención**
```sql
-- Limpiar datos antiguos (ejecutar mensualmente)
DELETE FROM mensaje 
WHERE created_at < NOW() - INTERVAL '90 days';

DELETE FROM ticket 
WHERE created_at < NOW() - INTERVAL '1 year' 
AND status = 'COMPLETED';
```

---

## 🚨 **Incident Response**

### **Procedimiento de Respuesta**

#### **Detección de Incidente**
1. **Alertas automáticas** (Prometheus/Grafana)
2. **Reportes de usuarios**
3. **Monitoreo proactivo**

#### **Clasificación de Incidentes**

| Severidad | Descripción | Tiempo Respuesta |
|-----------|-------------|------------------|
| **Critical** | Breach de datos, sistema comprometido | 15 minutos |
| **High** | Vulnerabilidad explotable | 1 hora |
| **Medium** | Configuración insegura | 4 horas |
| **Low** | Mejora de seguridad | 24 horas |

#### **Pasos de Respuesta**
```bash
#!/bin/bash
# /scripts/security-incident-response.sh

echo "=== Security Incident Response ==="

# 1. Aislar sistema afectado
docker compose -f docker-compose.prod.yml stop api

# 2. Preservar evidencia
docker logs ticketero-api > /security/incident-$(date +%Y%m%d-%H%M%S).log
docker exec ticketero-postgres pg_dump -U ticketero_user ticketero > /security/db-snapshot-$(date +%Y%m%d-%H%M%S).sql

# 3. Notificar stakeholders
curl -X POST https://hooks.slack.com/services/... \
  -d '{"text":"🚨 Security incident detected - System isolated"}'

# 4. Iniciar investigación
echo "Evidence preserved in /security/"
echo "System isolated - awaiting investigation"
```

### **Forensics y Análisis**

#### **Logs Críticos**
```bash
# Accesos sospechosos
docker logs ticketero-api 2>&1 | grep -E "(401|403|429)" | tail -100

# Patrones de ataque
docker logs ticketero-api 2>&1 | grep -E "(SELECT|UNION|DROP|INSERT)" | tail -50

# IPs sospechosas
docker logs ticketero-api 2>&1 | grep -oE "\b([0-9]{1,3}\.){3}[0-9]{1,3}\b" | sort | uniq -c | sort -nr
```

#### **Análisis de Base de Datos**
```sql
-- Verificar integridad de datos
SELECT COUNT(*) FROM ticket WHERE created_at > NOW() - INTERVAL '24 hours';

-- Buscar patrones anómalos
SELECT national_id, COUNT(*) 
FROM ticket 
WHERE created_at > NOW() - INTERVAL '1 hour'
GROUP BY national_id 
HAVING COUNT(*) > 10;
```

---

## 📋 **Security Checklist**

### **Deployment Security**
- [ ] Secrets rotados
- [ ] Usuarios no-root configurados
- [ ] SSL/TLS habilitado
- [ ] Firewall configurado
- [ ] Logs de auditoría habilitados
- [ ] Backups encriptados
- [ ] Monitoreo de seguridad activo

### **Application Security**
- [ ] Input validation implementada
- [ ] Output encoding habilitado
- [ ] SQL injection mitigado
- [ ] XSS protection habilitado
- [ ] CSRF protection configurado
- [ ] Rate limiting implementado
- [ ] Error handling seguro

### **Infrastructure Security**
- [ ] Containers hardened
- [ ] Network segmentation
- [ ] Access controls configurados
- [ ] Vulnerability scanning
- [ ] Security updates aplicados
- [ ] Incident response plan
- [ ] Backup and recovery tested

---

## 📞 **Contactos de Seguridad**

### **Equipo de Seguridad**
- **Security Officer**: security@ticketero.com
- **Incident Response**: incident@ticketero.com
- **Slack**: #security-alerts

### **Escalación de Incidentes**
1. **L1 - Operations**: Detección y contención inicial
2. **L2 - Security Team**: Investigación y análisis
3. **L3 - Management**: Decisiones de negocio y comunicación

### **Reportar Vulnerabilidades**
- **Email**: security@ticketero.com
- **PGP Key**: [Link to public key]
- **Bug Bounty**: [Link to program]

---

**Mantenido por:** Equipo de Seguridad  
**Última actualización:** 2024-11-25  
**Próxima revisión:** 2025-01-25 (Mensual)