# **PROMPT 7: Document – README, decisiones, notas técnicas**

## **Contexto**

Technical Writer senior con background en ingeniería de software, responsable de cerrar el ciclo de desarrollo asegurando mantenibilidad, trazabilidad y transferencia de conocimiento.

**Características del proyecto:**

* API REST con PostgreSQL y RabbitMQ  
* Dockerfile funcional existente  
* 3 tipos de notificaciones vía Telegram  
* Ambientes: desarrollo y producción

**IMPORTANTE:** Después de completar CADA paso, debes DETENERTE y solicitar una **revisión exhaustiva** antes de continuar.

---

## **Documentos de Entrada**

**Directorios del proyecto:**

1. `ticketero` \- Directorio con codigo fuente del software
2. `ticketero-infra` \- Directorio de Infraestructura como código

**Documentación actual del proyecto:**

1. `ticketero/docs/ARQUITECTURA.md` \- Stack tecnológico 
2. `ticketero/docs/CODING-STANDARDS.md` \- Estándares de Código 
3. `ticketero/docs/DEPLOYMENT.md` \- Guía de Deployment API 

4. **Sistema testeado:** Suite de tests E2E pasando (PROMPT 6\)

---
## **Metodología de Trabajo**

### **Principio:**

**"Analizar directorios → Generar documentación → Sintetizar → Validar → Confirmar → Continuar"**

---

## **PASO 1: Análisis de Estructura del Proyecto**

**ROL LLM:** Analista de Documentación

### **Tareas:**

1. **Analizar estructura de directorios:**
   - `ticketero/` - Código fuente
   - `ticketero-infra/` - Infraestructura CDK
   - Identificar archivos clave (pom.xml, Dockerfile, docker-compose.yml)

2. **Revisar documentación existente:**
   - Leer `ticketero/docs/ARCHITECTURE.md`
   - Leer `ticketero/docs/CODING-STANDARDS.md` 
   - Leer `ticketero/docs/DEPLOYMENT.md`

3. **Identificar gaps de documentación:**
   - README principal faltante o incompleto
   - Documentación de API endpoints
   - Guías de desarrollo local
   - Troubleshooting común

**ENTREGABLE:** Reporte de análisis con gaps identificados

**🛑 CHECKPOINT:** Solicitar revisión antes de continuar

---

## **PASO 2: Generación de README Principal**

**ROL LLM:** Technical Writer

### **Estructura del README:**

```markdown
# Sistema Ticketero

## Descripción
## Arquitectura
## Requisitos Previos
## Instalación y Configuración
## Uso
## API Endpoints
## Testing
## Deployment
## Troubleshooting
## Contribución
## Licencia
```

### **Criterios de Calidad:**
- Lenguaje claro y conciso
- Ejemplos prácticos ejecutables
- Enlaces a documentación detallada
- Badges de estado (build, tests, coverage)

**ENTREGABLE:** `ticketero/README.md` completo

**🛑 CHECKPOINT:** Validar README con stakeholders

---

## **PASO 3: Documentación de Decisiones Arquitectónicas (ADRs)**

**ROL LLM:** Arquitecto de Software

### **ADRs a documentar:**

1. **ADR-001:** Elección de PostgreSQL vs otras bases de datos
2. **ADR-002:** Implementación de RabbitMQ para notificaciones
3. **ADR-003:** Arquitectura de microservicios vs monolito
4. **ADR-004:** Estrategia de deployment con Docker
5. **ADR-005:** Integración con Telegram API

### **Template ADR:**
```markdown
# ADR-XXX: [Título]

## Estado
[Propuesto | Aceptado | Rechazado | Deprecado]

## Contexto
## Decisión
## Consecuencias
## Alternativas Consideradas
```

**ENTREGABLE:** `ticketero/docs/decisions/` con ADRs

**🛑 CHECKPOINT:** Revisar decisiones con equipo técnico

---

## **PASO 4: Documentación de API**

**ROL LLM:** API Documentation Specialist

### **Generar:**

1. **OpenAPI/Swagger specification**
   - Endpoints documentados
   - Modelos de datos
   - Códigos de respuesta
   - Ejemplos de uso

2. **Postman Collection**
   - Requests configurados
   - Variables de entorno
   - Tests automatizados

3. **Guía de integración**
   - Autenticación
   - Rate limiting
   - Manejo de errores

**ENTREGABLE:** `ticketero/docs/api/` completa

**🛑 CHECKPOINT:** Validar con desarrolladores frontend

---

## **PASO 5: Guías Operacionales**

**ROL LLM:** DevOps Technical Writer

### **Documentos a crear:**

1. **RUNBOOK.md**
   - Procedimientos de operación
   - Monitoreo y alertas
   - Backup y recovery
   - Escalado

2. **TROUBLESHOOTING.md**
   - Problemas comunes
   - Logs importantes
   - Comandos de diagnóstico
   - Contactos de soporte

3. **SECURITY.md**
   - Configuración de seguridad
   - Vulnerabilidades conocidas
   - Procedimientos de incident response

**ENTREGABLE:** `ticketero/docs/operations/`

**🛑 CHECKPOINT:** Revisar con equipo de operaciones

---

## **PASO 6: Documentación de Desarrollo**

**ROL LLM:** Developer Experience Engineer

### **Crear:**

1. **CONTRIBUTING.md**
   - Setup de entorno de desarrollo
   - Workflow de Git
   - Estándares de código
   - Proceso de review

2. **TESTING.md**
   - Estrategia de testing
   - Cómo ejecutar tests
   - Cobertura esperada
   - Mocking y fixtures

3. **CHANGELOG.md**
   - Historial de versiones
   - Breaking changes
   - Nuevas features
   - Bug fixes

**ENTREGABLE:** Documentación de desarrollo completa

**🛑 CHECKPOINT:** Validar con equipo de desarrollo

---

## **PASO 7: Síntesis y Validación Final**

**ROL LLM:** Quality Assurance Documentation

### **Actividades:**

1. **Revisión de consistencia**
   - Enlaces internos funcionando
   - Información actualizada
   - Formato homogéneo

2. **Validación práctica**
   - Seguir guías paso a paso
   - Verificar comandos
   - Probar ejemplos

3. **Índice de documentación**
   - Crear `docs/INDEX.md`
   - Organizar por audiencia
   - Mapear flujos de lectura

**ENTREGABLE:** Documentación completa y validada

---

## **Criterios de Aceptación**

### **✅ Documentación Completa:**
- [ ] README principal informativo y ejecutable
- [ ] ADRs de decisiones críticas documentadas
- [ ] API completamente documentada con ejemplos
- [ ] Guías operacionales para producción
- [ ] Documentación de desarrollo actualizada

### **✅ Calidad:**
- [ ] Lenguaje claro y técnicamente preciso
- [ ] Ejemplos ejecutables y verificados
- [ ] Enlaces y referencias actualizadas
- [ ] Formato consistente (Markdown)

### **✅ Mantenibilidad:**
- [ ] Estructura escalable de documentación
- [ ] Proceso de actualización definido
- [ ] Responsabilidades asignadas
- [ ] Versionado de documentación

---

## **Entregables Finales**

```
ticketero/
├── README.md                    # Documentación principal
├── CHANGELOG.md                 # Historial de cambios
├── CONTRIBUTING.md              # Guía de contribución
└── docs/
    ├── INDEX.md                 # Índice de documentación
    ├── ARCHITECTURE.md          # (Existente - actualizar)
    ├── CODING-STANDARDS.md      # (Existente - actualizar)
    ├── DEPLOYMENT.md            # (Existente - actualizar)
    ├── TESTING.md               # Estrategia de testing
    ├── SECURITY.md              # Documentación de seguridad
    ├── api/
    │   ├── openapi.yaml         # Especificación OpenAPI
    │   ├── postman-collection.json
    │   └── integration-guide.md
    ├── decisions/
    │   ├── ADR-001-database.md
    │   ├── ADR-002-messaging.md
    │   └── ...
    └── operations/
        ├── RUNBOOK.md
        ├── TROUBLESHOOTING.md
        └── monitoring.md
```

---

## **Notas Técnicas**

- **Formato:** Markdown con sintaxis GitHub Flavored
- **Versionado:** Seguir semantic versioning para docs
- **Automatización:** Considerar docs-as-code con CI/CD
- **Métricas:** Tracking de uso y feedback de documentación

**🎯 OBJETIVO:** Documentación que permita onboarding de nuevos desarrolladores en < 2 horas