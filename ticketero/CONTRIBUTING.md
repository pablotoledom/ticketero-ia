# 🤝 CONTRIBUTING - Ticketero API

> **Guía completa para contribuir al proyecto de gestión de tickets bancarios**

---

## 🎯 **Bienvenido Contribuidor**

¡Gracias por tu interés en contribuir al proyecto Ticketero! Esta guía te ayudará a configurar tu entorno de desarrollo y seguir nuestras mejores prácticas.

### **Tipos de Contribuciones**
- 🐛 **Bug fixes** - Corrección de errores
- ✨ **Features** - Nuevas funcionalidades
- 📚 **Documentation** - Mejoras en documentación
- 🧪 **Tests** - Nuevos tests o mejoras
- 🔧 **Refactoring** - Mejoras de código
- 🚀 **Performance** - Optimizaciones

---

## 🛠️ **Setup de Entorno de Desarrollo**

### **Prerequisitos**
- ☕ **Java 21+** ([OpenJDK](https://openjdk.java.net/projects/jdk/21/))
- 📦 **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- 🐳 **Docker + Docker Compose** ([Install](https://docs.docker.com/get-docker/))
- 🔧 **Git** ([Install](https://git-scm.com/downloads))
- 💻 **IDE** (IntelliJ IDEA, VS Code, Eclipse)

### **Configuración Inicial**

#### **1. Fork y Clone**
```bash
# 1. Fork el repositorio en GitHub
# 2. Clone tu fork
git clone https://github.com/TU_USERNAME/ticketero-ia.git
cd ticketero-ia

# 3. Agregar upstream remote
git remote add upstream https://github.com/ORIGINAL_OWNER/ticketero-ia.git

# 4. Verificar remotes
git remote -v
```

#### **2. Setup del Proyecto**
```bash
# Navegar al directorio de la aplicación
cd ticketero

# Copiar configuración de desarrollo
cp .env.example .env

# Editar .env con tus credenciales de Telegram
nano .env
```

#### **3. Levantar Infraestructura**
```bash
# Levantar PostgreSQL y RabbitMQ
docker compose up -d postgres rabbitmq

# Verificar que estén corriendo
docker compose ps
```

#### **4. Ejecutar Aplicación**
```bash
# Compilar y ejecutar
./mvnw spring-boot:run

# Verificar que funciona
curl http://localhost:8080/actuator/health
```

#### **5. Ejecutar Tests**
```bash
# Tests unitarios
./mvnw test

# Tests con cobertura
./mvnw test jacoco:report

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

---

## 🔄 **Workflow de Desarrollo**

### **Branching Strategy**

```
main
├── develop
│   ├── feature/nueva-funcionalidad
│   ├── bugfix/corregir-error
│   └── hotfix/fix-critico
└── release/v1.1.0
```

#### **Tipos de Branches**
- **`main`** - Código de producción estable
- **`develop`** - Integración de features
- **`feature/*`** - Nuevas funcionalidades
- **`bugfix/*`** - Corrección de bugs
- **`hotfix/*`** - Fixes críticos para producción
- **`release/*`** - Preparación de releases

### **Proceso de Contribución**

#### **1. Crear Branch**
```bash
# Actualizar develop
git checkout develop
git pull upstream develop

# Crear nueva branch
git checkout -b feature/nombre-descriptivo

# Ejemplos de nombres:
# feature/add-sms-notifications
# bugfix/fix-telegram-timeout
# docs/update-api-documentation
```

#### **2. Desarrollo**
```bash
# Hacer cambios siguiendo coding standards
# Ver docs/CODING-STANDARDS.md

# Commits frecuentes con mensajes descriptivos
git add .
git commit -m "feat: add SMS notification service"

# Push a tu fork
git push origin feature/nombre-descriptivo
```

#### **3. Testing**
```bash
# Ejecutar todos los tests
./mvnw test

# Verificar cobertura (debe ser > 80%)
./mvnw jacoco:report

# Tests de integración
./mvnw test -Dspring.profiles.active=test

# Linting y formato
./mvnw spotless:check
```

#### **4. Pull Request**
1. **Crear PR** desde tu branch hacia `develop`
2. **Completar template** de PR (ver abajo)
3. **Asignar reviewers** (mínimo 2)
4. **Esperar aprobación** y merge

---

## 📝 **Estándares de Código**

### **Convenciones de Naming**
```java
// Clases: PascalCase
public class TicketService { }

// Métodos y variables: camelCase
public void createTicket() { }
private String ticketNumber;

// Constantes: UPPER_SNAKE_CASE
public static final String DEFAULT_QUEUE = "GENERAL";

// Packages: lowercase
package com.example.ticketero.service;
```

### **Estructura de Clases**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    
    // 1. Constantes
    private static final String DEFAULT_STATUS = "CREATED";
    
    // 2. Dependencies (final fields)
    private final TicketRepository ticketRepository;
    private final TelegramService telegramService;
    
    // 3. Public methods
    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        // Implementation
    }
    
    // 4. Private methods
    private void validateRequest(TicketRequest request) {
        // Implementation
    }
}
```

### **Manejo de Excepciones**
```java
// ✅ CORRECTO - Excepciones específicas
@Service
public class TicketService {
    
    public TicketResponse createTicket(TicketRequest request) {
        try {
            return processTicket(request);
        } catch (ValidationException e) {
            log.warn("Validation failed for request: {}", request, e);
            throw new BadRequestException("Invalid ticket data", e);
        } catch (Exception e) {
            log.error("Unexpected error creating ticket", e);
            throw new InternalServerException("Failed to create ticket", e);
        }
    }
}
```

### **Logging**
```java
@Slf4j
public class TicketService {
    
    public TicketResponse createTicket(TicketRequest request) {
        log.info("Creating ticket for nationalId: {}", 
                maskSensitiveData(request.getNationalId()));
        
        try {
            TicketResponse response = processTicket(request);
            log.info("Ticket created successfully: {}", response.getIdentificador());
            return response;
        } catch (Exception e) {
            log.error("Failed to create ticket for nationalId: {}", 
                     maskSensitiveData(request.getNationalId()), e);
            throw e;
        }
    }
}
```

---

## 🧪 **Testing Guidelines**

### **Estructura de Tests**
```
src/test/java/
├── unit/                    # Tests unitarios
│   ├── service/
│   ├── controller/
│   └── util/
├── integration/             # Tests de integración
│   ├── api/
│   └── repository/
└── e2e/                     # Tests end-to-end
    └── scenarios/
```

### **Tests Unitarios**
```java
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    
    @Mock
    private TicketRepository ticketRepository;
    
    @Mock
    private TelegramService telegramService;
    
    @InjectMocks
    private TicketService ticketService;
    
    @Test
    void createTicket_withValidData_shouldReturnResponse() {
        // Given
        TicketRequest request = TicketRequest.builder()
            .nationalId("12345678")
            .branchOffice("Centro")
            .queue("CAJA")
            .build();
        
        Ticket savedTicket = Ticket.builder()
            .id(1L)
            .codigoReferencia(UUID.randomUUID())
            .numero("C01")
            .build();
        
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        
        // When
        TicketResponse response = ticketService.createTicket(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNumero()).isEqualTo("C01");
        verify(ticketRepository).save(any(Ticket.class));
        verify(telegramService, never()).sendMessage(anyString(), anyString());
    }
    
    @Test
    void createTicket_withInvalidData_shouldThrowException() {
        // Given
        TicketRequest request = TicketRequest.builder()
            .nationalId("123") // Invalid
            .build();
        
        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid national ID");
    }
}
```

### **Tests de Integración**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TicketControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketero_test")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void createTicket_shouldReturnCreatedTicket() {
        // Given
        TicketRequest request = new TicketRequest("12345678", "Centro", "CAJA");
        
        // When
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
            "/api/tickets", request, TicketResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getNumero()).startsWith("C");
    }
}
```

### **Cobertura de Tests**
- **Objetivo mínimo**: 80%
- **Services**: 90%+ (lógica de negocio crítica)
- **Controllers**: 70%+ (validación y mapeo)
- **Repositories**: No requerido (Spring Data)

---

## 📋 **Commit Guidelines**

### **Conventional Commits**
Seguimos la especificación [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### **Tipos de Commit**
- **feat**: Nueva funcionalidad
- **fix**: Corrección de bug
- **docs**: Cambios en documentación
- **style**: Cambios de formato (no afectan lógica)
- **refactor**: Refactoring de código
- **test**: Agregar o modificar tests
- **chore**: Tareas de mantenimiento

#### **Ejemplos**
```bash
# Feature
git commit -m "feat(api): add SMS notification service"

# Bug fix
git commit -m "fix(telegram): handle rate limit errors properly"

# Documentation
git commit -m "docs(readme): update installation instructions"

# Breaking change
git commit -m "feat(api)!: change ticket creation response format

BREAKING CHANGE: The ticket creation endpoint now returns a different response structure"
```

---

## 🔍 **Code Review Process**

### **Antes de Crear PR**
- [ ] Tests pasan localmente
- [ ] Cobertura > 80%
- [ ] Código sigue estándares
- [ ] Documentación actualizada
- [ ] Commits siguen convenciones

### **Template de Pull Request**
```markdown
## Descripción
Breve descripción de los cambios realizados.

## Tipo de Cambio
- [ ] Bug fix (cambio que corrige un issue)
- [ ] Nueva feature (cambio que agrega funcionalidad)
- [ ] Breaking change (fix o feature que causa cambios incompatibles)
- [ ] Documentación

## Testing
- [ ] Tests unitarios agregados/actualizados
- [ ] Tests de integración agregados/actualizados
- [ ] Tests manuales realizados

## Checklist
- [ ] Mi código sigue los estándares del proyecto
- [ ] He realizado self-review de mi código
- [ ] He comentado código complejo
- [ ] He actualizado la documentación
- [ ] Mis cambios no generan warnings
- [ ] Tests nuevos y existentes pasan

## Screenshots (si aplica)
[Agregar screenshots de cambios en UI]

## Notas Adicionales
[Cualquier información adicional para reviewers]
```

### **Criterios de Aprobación**
- **2 aprobaciones** mínimo
- **Todos los tests** deben pasar
- **No conflictos** con branch destino
- **Documentación** actualizada si es necesario

---

## 🚀 **Release Process**

### **Versionado Semántico**
Seguimos [Semantic Versioning](https://semver.org/):
- **MAJOR**: Cambios incompatibles
- **MINOR**: Nueva funcionalidad compatible
- **PATCH**: Bug fixes compatibles

### **Proceso de Release**
```bash
# 1. Crear branch de release
git checkout develop
git pull upstream develop
git checkout -b release/v1.1.0

# 2. Actualizar versión
./mvnw versions:set -DnewVersion=1.1.0

# 3. Actualizar CHANGELOG.md
# 4. Commit cambios
git commit -m "chore: prepare release v1.1.0"

# 5. Merge a main
git checkout main
git merge release/v1.1.0

# 6. Tag release
git tag -a v1.1.0 -m "Release v1.1.0"
git push upstream main --tags

# 7. Merge back to develop
git checkout develop
git merge main
```

---

## 🛠️ **Herramientas de Desarrollo**

### **IDE Configuration**

#### **IntelliJ IDEA**
```xml
<!-- .idea/codeStyles/Project.xml -->
<code_scheme name="Project" version="173">
  <JavaCodeStyleSettings>
    <option name="IMPORT_LAYOUT_TABLE">
      <value>
        <package name="java" withSubpackages="true" static="false"/>
        <package name="javax" withSubpackages="true" static="false"/>
        <emptyLine/>
        <package name="org" withSubpackages="true" static="false"/>
        <package name="com" withSubpackages="true" static="false"/>
        <emptyLine/>
        <package name="" withSubpackages="true" static="false"/>
      </value>
    </option>
  </JavaCodeStyleSettings>
</code_scheme>
```

#### **VS Code**
```json
// .vscode/settings.json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
  "java.saveActions.organizeImports": true,
  "editor.formatOnSave": true
}
```

### **Git Hooks**
```bash
#!/bin/sh
# .git/hooks/pre-commit

echo "Running pre-commit checks..."

# Run tests
./mvnw test
if [ $? -ne 0 ]; then
  echo "Tests failed. Commit aborted."
  exit 1
fi

# Check code formatting
./mvnw spotless:check
if [ $? -ne 0 ]; then
  echo "Code formatting issues found. Run './mvnw spotless:apply' to fix."
  exit 1
fi

echo "Pre-commit checks passed!"
```

---

## 📞 **Soporte y Comunicación**

### **Canales de Comunicación**
- **GitHub Issues**: Para bugs y feature requests
- **GitHub Discussions**: Para preguntas y discusiones
- **Slack**: #ticketero-dev (para contributors activos)
- **Email**: dev@ticketero.com

### **Reportar Bugs**
Usar el template de issue:
```markdown
**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Environment:**
- OS: [e.g. macOS, Ubuntu]
- Java version: [e.g. 21]
- Maven version: [e.g. 3.9.5]

**Additional context**
Add any other context about the problem here.
```

### **Solicitar Features**
```markdown
**Is your feature request related to a problem?**
A clear and concise description of what the problem is.

**Describe the solution you'd like**
A clear and concise description of what you want to happen.

**Describe alternatives you've considered**
A clear and concise description of any alternative solutions.

**Additional context**
Add any other context or screenshots about the feature request here.
```

---

## 🏆 **Reconocimiento**

### **Contributors**
Todos los contributors son reconocidos en:
- **README.md** - Lista de contributors
- **CHANGELOG.md** - Créditos por release
- **GitHub Contributors** - Automático

### **Tipos de Contribución**
Reconocemos diferentes tipos de contribución usando [All Contributors](https://allcontributors.org/):
- 💻 Code
- 📖 Documentation
- 🐛 Bug reports
- 💡 Ideas
- 🤔 Answering Questions
- ⚠️ Tests
- 🚇 Infrastructure

---

## 📋 **Checklist del Contributor**

### **Primera Contribución**
- [ ] Fork del repositorio creado
- [ ] Entorno de desarrollo configurado
- [ ] Tests ejecutándose correctamente
- [ ] Estándares de código entendidos
- [ ] Proceso de PR comprendido

### **Antes de Cada PR**
- [ ] Branch actualizada con develop
- [ ] Cambios testeados localmente
- [ ] Documentación actualizada
- [ ] Commits siguen convenciones
- [ ] PR template completado

### **Después del Merge**
- [ ] Branch local eliminada
- [ ] Fork sincronizado
- [ ] Issue cerrado (si aplica)
- [ ] Documentación verificada

---

**¡Gracias por contribuir al proyecto Ticketero! 🎉**

---

**Mantenido por:** Equipo de Desarrollo  
**Última actualización:** 2024-11-25  
**Próxima revisión:** 2025-02-25