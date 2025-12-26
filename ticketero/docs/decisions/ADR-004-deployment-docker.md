# ADR-004: Estrategia de Deployment con Docker

## Estado
**Aceptado** - 2024-11-25

## Contexto

Necesidad de definir estrategia de deployment que soporte:
- Desarrollo local consistente entre desarrolladores
- Deployment en múltiples ambientes (dev, staging, prod)
- Portabilidad entre diferentes infraestructuras
- Facilidad de rollback y versionado
- Integración con CI/CD pipelines

## Decisión

**Adoptar containerización con Docker** como estrategia principal de deployment, usando:
- **Multi-stage Dockerfile** para optimización de imagen
- **Docker Compose** para orquestación local
- **Container Registry** para distribución de imágenes

### Arquitectura de Deployment

```
[Source Code] → [Docker Build] → [Container Registry] → [Target Environment]
                      ↓
              [Multi-stage Build]
              ├── dependencies (cached)
              ├── build (Maven)
              └── runtime (JRE only)
```

## Justificación Técnica

1. **Consistencia**: "Works on my machine" → "Works everywhere"
2. **Portabilidad**: Mismo container en dev, staging y prod
3. **Aislamiento**: Dependencias encapsuladas en el container
4. **Escalabilidad**: Fácil scaling horizontal con orchestrators
5. **Rollback**: Versionado de imágenes para rollback rápido

## Implementación

### Multi-stage Dockerfile
```dockerfile
# Stage 1: Dependencies (cacheable)
FROM maven:3.9-eclipse-temurin-21-alpine AS dependencies
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Stage 2: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY --from=dependencies /root/.m2 /root/.m2
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose para Desarrollo
```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    depends_on:
      - postgres
      - rabbitmq
  
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    
  rabbitmq:
    image: rabbitmq:3.13-management-alpine
```

## Consecuencias

### ✅ Positivas
- **Reproducibilidad**: Builds consistentes en cualquier ambiente
- **Aislamiento**: No hay conflictos de dependencias del host
- **Portabilidad**: Funciona en cualquier plataforma con Docker
- **Versionado**: Imágenes taggeadas para control de versiones
- **Rollback**: Cambio rápido entre versiones de imagen
- **CI/CD**: Integración natural con pipelines automatizados

### ⚠️ Negativas
- **Overhead**: Capa adicional de abstracción
- **Tamaño**: Imágenes pueden ser grandes sin optimización
- **Complejidad**: Curva de aprendizaje para el equipo
- **Debugging**: Más complejo debuggear dentro del container

### 🔄 Mitigaciones
- **Multi-stage builds** para reducir tamaño de imagen final
- **Layer caching** para builds incrementales rápidos
- **Health checks** integrados en el container
- **Volume mounts** para desarrollo local con hot reload

## Optimizaciones Implementadas

### 1. **Tamaño de Imagen**
```dockerfile
# ✅ Imagen final: ~200MB (JRE + app)
FROM eclipse-temurin:21-jre-alpine  # Base pequeña

# ❌ Evitado: ~800MB (JDK completo)
FROM eclipse-temurin:21-jdk-alpine
```

### 2. **Build Caching**
```dockerfile
# Copiar pom.xml primero para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B  # Esta capa se cachea

# Copiar código después
COPY src ./src
RUN mvn clean package  # Solo se ejecuta si src/ cambió
```

### 3. **Seguridad**
```dockerfile
# Usuario no-root
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
USER spring:spring

# Health check integrado
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

## Alternativas Consideradas

### JAR Deployment Tradicional
```bash
# ❌ Rechazado
java -jar ticketero-0.0.1-SNAPSHOT.jar
```
**Problemas:**
- Dependencias del sistema host (Java version, etc.)
- Configuración manual de ambiente
- Dificultad para replicar problemas

### VM Images (AMI, etc.)
**❌ Rechazado por:**
- Tiempo de boot lento (minutos vs segundos)
- Tamaño grande (GBs vs MBs)
- Actualizaciones complejas (nueva AMI vs nueva imagen)

### Serverless (AWS Lambda)
**❌ Rechazado por:**
- Cold start latency
- Limitaciones de runtime (15 min max)
- Vendor lock-in

## Estrategia de Versionado

### Tagging Strategy
```bash
# Semantic versioning
docker tag ticketero-api:latest ticketero-api:1.0.0
docker tag ticketero-api:latest ticketero-api:1.0
docker tag ticketero-api:latest ticketero-api:1

# Environment tags
docker tag ticketero-api:1.0.0 ticketero-api:dev
docker tag ticketero-api:1.0.0 ticketero-api:staging
docker tag ticketero-api:1.0.0 ticketero-api:prod
```

### Rollback Strategy
```bash
# Rollback rápido cambiando tag
docker service update --image ticketero-api:1.0.0 ticketero_app
```

## Métricas de Éxito

- ✅ **Build Time**: < 5 minutos (con cache)
- ✅ **Image Size**: < 300MB imagen final
- ✅ **Startup Time**: < 30 segundos
- ✅ **Deployment Time**: < 2 minutos end-to-end
- ✅ **Rollback Time**: < 1 minuto

## Monitoreo

### Container Metrics
- CPU/Memory usage
- Container restart count
- Health check status
- Image pull time

### Build Metrics
- Build success rate
- Build duration
- Cache hit ratio
- Image size trends

## Referencias

- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Multi-stage Builds](https://docs.docker.com/develop/dev-best-practices/#use-multi-stage-builds)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [DEPLOYMENT.md](../DEPLOYMENT.md)

---

**Autor:** Equipo de DevOps  
**Revisado por:** Tech Lead  
**Próxima revisión:** 2025-05-25