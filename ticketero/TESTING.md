# 🧪 TESTING - Ticketero API

> **Estrategia completa de testing para garantizar calidad y confiabilidad**

---

## 🎯 **Filosofía de Testing**

### **Principios**
1. **Test First** - Escribir tests antes o junto con el código
2. **Fast Feedback** - Tests rápidos para desarrollo ágil
3. **Reliable** - Tests determinísticos y estables
4. **Maintainable** - Tests fáciles de entender y mantener
5. **Comprehensive** - Cobertura adecuada sin over-testing

### **Pirámide de Testing**
```
        /\
       /  \
      / E2E \     ← Pocos, lentos, frágiles
     /______\
    /        \
   / Integration \  ← Algunos, medianos
  /______________\
 /                \
/   Unit Tests     \  ← Muchos, rápidos, estables
/____________________\
```

---

## 🏗️ **Estructura de Tests**

### **Organización de Directorios**
```
src/test/java/com/example/ticketero/
├── unit/                           # Tests unitarios
│   ├── service/
│   │   ├── TicketServiceTest.java
│   │   └── TelegramServiceTest.java
│   ├── controller/
│   │   └── TicketControllerTest.java
│   └── util/
│       └── ValidationUtilsTest.java
├── integration/                    # Tests de integración
│   ├── api/
│   │   └── TicketApiIntegrationTest.java
│   ├── repository/
│   │   └── TicketRepositoryTest.java
│   └── messaging/
│       └── RabbitMQIntegrationTest.java
├── e2e/                           # Tests end-to-end
│   ├── scenarios/
│   │   ├── TicketCreationE2ETest.java
│   │   └── NotificationFlowE2ETest.java
│   └── support/
│       ├── TestDataBuilder.java
│       └── TestContainersConfig.java
└── performance/                   # Tests de performance
    └── LoadTest.java
```

### **Configuración de Testing**
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  rabbitmq:
    host: localhost
    port: 5672
    username: test
    password: test

logging:
  level:
    com.example.ticketero: DEBUG
    org.springframework.test: INFO
```

---

## 🔬 **Tests Unitarios**

### **Configuración Base**
```java
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    
    @Mock
    private TicketRepository ticketRepository;
    
    @Mock
    private MensajeRepository mensajeRepository;
    
    @Mock
    private TelegramService telegramService;
    
    @InjectMocks
    private TicketService ticketService;
    
    @BeforeEach
    void setUp() {
        // Setup común para todos los tests
    }
}
```

### **Testing de Services**
```java
class TicketServiceTest {
    
    @Test
    @DisplayName("Crear ticket con datos válidos debe retornar respuesta exitosa")
    void createTicket_withValidData_shouldReturnSuccessResponse() {
        // Given
        TicketRequest request = TicketTestDataBuilder.aValidRequest()
            .withNationalId("12345678")
            .withQueue("CAJA")
            .build();
        
        Ticket savedTicket = TicketTestDataBuilder.aValidTicket()
            .withId(1L)
            .withNumero("C01")
            .build();
        
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        when(mensajeRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        
        // When
        TicketResponse response = ticketService.createTicket(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNumero()).isEqualTo("C01");
        assertThat(response.getQueue()).isEqualTo("CAJA");
        assertThat(response.getPosicionEnCola()).isEqualTo(1);
        
        verify(ticketRepository).save(argThat(ticket -> 
            ticket.getNationalId().equals("12345678") &&
            ticket.getQueue().equals("CAJA")
        ));
        verify(mensajeRepository).saveAll(argThat(mensajes -> 
            mensajes.size() == 3 // 3 tipos de mensajes
        ));
    }
    
    @Test
    @DisplayName("Crear ticket con ID nacional inválido debe lanzar excepción")
    void createTicket_withInvalidNationalId_shouldThrowValidationException() {
        // Given
        TicketRequest request = TicketTestDataBuilder.aValidRequest()
            .withNationalId("123") // Inválido
            .build();
        
        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ID nacional inválido");
        
        verify(ticketRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Error en base de datos debe lanzar excepción de servicio")
    void createTicket_withDatabaseError_shouldThrowServiceException() {
        // Given
        TicketRequest request = TicketTestDataBuilder.aValidRequest().build();
        when(ticketRepository.save(any())).thenThrow(new DataAccessException("DB Error") {});
        
        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(request))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Error al crear ticket");
    }
}
```

### **Testing de Controllers**
```java
@WebMvcTest(TicketController.class)
class TicketControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TicketService ticketService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("POST /api/tickets con datos válidos debe retornar 201")
    void createTicket_withValidData_shouldReturn201() throws Exception {
        // Given
        TicketRequest request = new TicketRequest("12345678", "1234567890", "Centro", "CAJA");
        TicketResponse expectedResponse = new TicketResponse(
            UUID.randomUUID(), "C01", "CAJA", 1, "15 minutos", "Ticket creado"
        );
        
        when(ticketService.createTicket(any(TicketRequest.class)))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C01"))
                .andExpect(jsonPath("$.queue").value("CAJA"))
                .andExpect(jsonPath("$.posicionEnCola").value(1))
                .andDo(print());
        
        verify(ticketService).createTicket(argThat(req -> 
            req.getNationalId().equals("12345678")
        ));
    }
    
    @Test
    @DisplayName("POST /api/tickets con datos inválidos debe retornar 400")
    void createTicket_withInvalidData_shouldReturn400() throws Exception {
        // Given
        TicketRequest invalidRequest = new TicketRequest("123", "", "", "INVALID");
        
        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists())
                .andDo(print());
        
        verify(ticketService, never()).createTicket(any());
    }
}
```

---

## 🔗 **Tests de Integración**

### **Configuración con TestContainers**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class TicketApiIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketero_test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withUser("test", "test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> "test");
    }
    
    @Test
    @DisplayName("Flujo completo: crear ticket y consultar por UUID")
    void completeFlow_createAndRetrieveTicket() {
        // Given
        TicketRequest request = new TicketRequest("12345678", "1234567890", "Centro", "CAJA");
        
        // When - Crear ticket
        ResponseEntity<TicketResponse> createResponse = restTemplate.postForEntity(
            "/api/tickets", request, TicketResponse.class);
        
        // Then - Verificar creación
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TicketResponse createdTicket = createResponse.getBody();
        assertThat(createdTicket).isNotNull();
        assertThat(createdTicket.getNumero()).startsWith("C");
        
        // When - Consultar ticket
        ResponseEntity<TicketDetailResponse> getResponse = restTemplate.getForEntity(
            "/api/tickets/" + createdTicket.getIdentificador(), TicketDetailResponse.class);
        
        // Then - Verificar consulta
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TicketDetailResponse retrievedTicket = getResponse.getBody();
        assertThat(retrievedTicket).isNotNull();
        assertThat(retrievedTicket.getIdentificador()).isEqualTo(createdTicket.getIdentificador());
        assertThat(retrievedTicket.getMensajes()).hasSize(3);
        
        // Verificar en base de datos
        Optional<Ticket> dbTicket = ticketRepository.findByCodigoReferencia(
            createdTicket.getIdentificador());
        assertThat(dbTicket).isPresent();
        assertThat(dbTicket.get().getNationalId()).isEqualTo("12345678");
    }
}
```

### **Testing de Repositories**
```java
@DataJpaTest
@Testcontainers
class TicketRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    @DisplayName("Buscar por código de referencia debe retornar ticket correcto")
    void findByCodigoReferencia_shouldReturnCorrectTicket() {
        // Given
        UUID codigoReferencia = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
            .codigoReferencia(codigoReferencia)
            .nationalId("12345678")
            .numero("C01")
            .queue("CAJA")
            .status("CREATED")
            .createdAt(LocalDateTime.now())
            .build();
        
        entityManager.persistAndFlush(ticket);
        
        // When
        Optional<Ticket> found = ticketRepository.findByCodigoReferencia(codigoReferencia);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNationalId()).isEqualTo("12345678");
        assertThat(found.get().getNumero()).isEqualTo("C01");
    }
    
    @Test
    @DisplayName("Contar tickets por cola debe retornar cantidad correcta")
    void countByQueue_shouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(createTicket("C01", "CAJA"));
        entityManager.persistAndFlush(createTicket("C02", "CAJA"));
        entityManager.persistAndFlush(createTicket("P01", "PLATAFORMA"));
        
        // When
        long cajaCount = ticketRepository.countByQueueAndStatus("CAJA", "CREATED");
        long plataformaCount = ticketRepository.countByQueueAndStatus("PLATAFORMA", "CREATED");
        
        // Then
        assertThat(cajaCount).isEqualTo(2);
        assertThat(plataformaCount).isEqualTo(1);
    }
}
```

---

## 🎭 **Tests End-to-End**

### **Configuración E2E**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@ActiveProfiles("e2e")
class TicketCreationE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");
    
    private RestTemplate restTemplate;
    private String baseUrl = "http://localhost:8080";
    
    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
    }
    
    @Test
    @DisplayName("Escenario completo: crear ticket y recibir notificaciones")
    void completeTicketScenario() throws InterruptedException {
        // Given
        TicketRequest request = new TicketRequest("12345678", "1234567890", "Centro", "CAJA");
        
        // When - Crear ticket
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
            baseUrl + "/api/tickets", request, TicketResponse.class);
        
        // Then - Verificar creación exitosa
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TicketResponse ticket = response.getBody();
        assertThat(ticket).isNotNull();
        
        // When - Esperar procesamiento de mensajes
        Thread.sleep(2000); // Esperar que se procesen los mensajes
        
        // Then - Verificar que los mensajes fueron creados
        ResponseEntity<TicketDetailResponse> detailResponse = restTemplate.getForEntity(
            baseUrl + "/api/tickets/" + ticket.getIdentificador(), TicketDetailResponse.class);
        
        TicketDetailResponse detail = detailResponse.getBody();
        assertThat(detail.getMensajes()).hasSize(3);
        assertThat(detail.getMensajes())
            .extracting(MensajeInfo::getPlantilla)
            .containsExactlyInAnyOrder("TICKET_CREATED", "TICKET_UPCOMING", "TICKET_ACTIVE");
        
        // Verificar que al menos el primer mensaje fue enviado
        assertThat(detail.getMensajes())
            .filteredOn(m -> m.getPlantilla().equals("TICKET_CREATED"))
            .extracting(MensajeInfo::getEstadoEnvio)
            .containsExactly("ENVIADO");
    }
}
```

---

## ⚡ **Tests de Performance**

### **Load Testing con JMeter/K6**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("performance")
class TicketApiLoadTest {
    
    private RestTemplate restTemplate = new RestTemplate();
    private String baseUrl = "http://localhost:8080";
    
    @Test
    @DisplayName("API debe manejar 100 requests concurrentes")
    void loadTest_100ConcurrentRequests() throws InterruptedException {
        int numberOfThreads = 100;
        int requestsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        TicketRequest request = new TicketRequest(
                            "1234567" + threadId, 
                            "123456789" + j, 
                            "Centro", 
                            "CAJA"
                        );
                        
                        try {
                            ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                                baseUrl + "/api/tickets", request, TicketResponse.class);
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            responseTimes.add(responseTime);
                            
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Análisis de resultados
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long p95ResponseTime = responseTimes.stream()
            .sorted()
            .skip((long) (responseTimes.size() * 0.95))
            .findFirst()
            .orElse(0L);
        
        System.out.println("=== Load Test Results ===");
        System.out.println("Total requests: " + (numberOfThreads * requestsPerThread));
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Average response time: " + averageResponseTime + "ms");
        System.out.println("P95 response time: " + p95ResponseTime + "ms");
        
        // Assertions
        assertThat(successCount.get()).isGreaterThan(numberOfThreads * requestsPerThread * 0.95); // 95% success rate
        assertThat(averageResponseTime).isLessThan(500); // Average < 500ms
        assertThat(p95ResponseTime).isLessThan(1000); // P95 < 1000ms
    }
}
```

---

## 🛠️ **Test Utilities y Helpers**

### **Test Data Builders**
```java
public class TicketTestDataBuilder {
    
    private String nationalId = "12345678";
    private String telefono = "1234567890";
    private String branchOffice = "Centro";
    private String queue = "CAJA";
    
    public static TicketTestDataBuilder aValidRequest() {
        return new TicketTestDataBuilder();
    }
    
    public TicketTestDataBuilder withNationalId(String nationalId) {
        this.nationalId = nationalId;
        return this;
    }
    
    public TicketTestDataBuilder withQueue(String queue) {
        this.queue = queue;
        return this;
    }
    
    public TicketRequest build() {
        return new TicketRequest(nationalId, telefono, branchOffice, queue);
    }
    
    public static TicketEntityBuilder aValidTicket() {
        return new TicketEntityBuilder();
    }
    
    public static class TicketEntityBuilder {
        private Long id = 1L;
        private UUID codigoReferencia = UUID.randomUUID();
        private String numero = "C01";
        private String nationalId = "12345678";
        private String queue = "CAJA";
        
        public TicketEntityBuilder withId(Long id) {
            this.id = id;
            return this;
        }
        
        public TicketEntityBuilder withNumero(String numero) {
            this.numero = numero;
            return this;
        }
        
        public Ticket build() {
            return Ticket.builder()
                .id(id)
                .codigoReferencia(codigoReferencia)
                .numero(numero)
                .nationalId(nationalId)
                .queue(queue)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();
        }
    }
}
```

### **Custom Matchers**
```java
public class TicketMatchers {
    
    public static Matcher<TicketResponse> hasValidTicketStructure() {
        return new TypeSafeMatcher<TicketResponse>() {
            @Override
            protected boolean matchesSafely(TicketResponse ticket) {
                return ticket.getIdentificador() != null &&
                       ticket.getNumero() != null &&
                       ticket.getQueue() != null &&
                       ticket.getPosicionEnCola() > 0 &&
                       ticket.getTiempoEstimado() != null;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("a valid ticket response structure");
            }
        };
    }
    
    public static Matcher<String> isValidTicketNumber() {
        return matchesPattern("^[A-Z]\\d{2}$");
    }
}
```

---

## 📊 **Métricas y Cobertura**

### **Configuración de JaCoCo**
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### **Objetivos de Cobertura**
| Componente | Objetivo | Crítico |
|------------|----------|---------|
| **Services** | 90%+ | Lógica de negocio |
| **Controllers** | 80%+ | Validación y mapeo |
| **Utilities** | 95%+ | Funciones puras |
| **Repositories** | N/A | Spring Data |
| **Configurations** | 70%+ | Setup crítico |

### **Comandos de Testing**
```bash
# Tests unitarios
./mvnw test

# Tests con cobertura
./mvnw test jacoco:report

# Solo tests de integración
./mvnw test -Dtest="**/*IntegrationTest"

# Solo tests E2E
./mvnw test -Dtest="**/*E2ETest" -Dspring.profiles.active=e2e

# Tests de performance
./mvnw test -Dtest="**/*LoadTest" -Dspring.profiles.active=performance

# Verificar cobertura mínima
./mvnw jacoco:check
```

---

## 🚀 **CI/CD Integration**

### **GitHub Actions**
```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: ./mvnw test jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
```

---

## 📋 **Best Practices**

### **Naming Conventions**
```java
// Test class naming
class TicketServiceTest { }           // Unit test
class TicketApiIntegrationTest { }    // Integration test
class TicketCreationE2ETest { }       // E2E test

// Test method naming
@Test
@DisplayName("Crear ticket con datos válidos debe retornar respuesta exitosa")
void createTicket_withValidData_shouldReturnSuccessResponse() { }

// Pattern: methodName_condition_expectedBehavior
```

### **Test Structure (AAA Pattern)**
```java
@Test
void testMethod() {
    // Arrange (Given)
    TicketRequest request = createValidRequest();
    when(repository.save(any())).thenReturn(savedTicket);
    
    // Act (When)
    TicketResponse response = service.createTicket(request);
    
    // Assert (Then)
    assertThat(response).isNotNull();
    assertThat(response.getNumero()).isEqualTo("C01");
    verify(repository).save(any());
}
```

### **Assertions Guidelines**
```java
// ✅ CORRECTO - Assertions específicas
assertThat(response.getNumero()).isEqualTo("C01");
assertThat(response.getPosicionEnCola()).isGreaterThan(0);

// ❌ EVITAR - Assertions genéricas
assertThat(response).isNotNull();
assertTrue(response.getNumero().startsWith("C"));
```

---

## 🔧 **Troubleshooting Tests**

### **Problemas Comunes**

#### **Tests Flaky**
```java
// ❌ PROBLEMA - Dependencia de tiempo
@Test
void testWithTiming() {
    service.processAsync();
    Thread.sleep(1000); // Flaky!
    assertThat(result).isNotNull();
}

// ✅ SOLUCIÓN - Usar Awaitility
@Test
void testWithAwaitility() {
    service.processAsync();
    await().atMost(5, SECONDS)
           .until(() -> result != null);
}
```

#### **Tests Lentos**
```java
// ❌ PROBLEMA - Levantar contexto completo
@SpringBootTest
class SlowTest { }

// ✅ SOLUCIÓN - Usar slices específicos
@WebMvcTest(TicketController.class)
class FastControllerTest { }

@DataJpaTest
class FastRepositoryTest { }
```

### **Debugging Tests**
```java
@Test
void debuggingTest() {
    // Logging para debugging
    log.debug("Request: {}", request);
    
    // Capturar argumentos
    ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
    verify(repository).save(ticketCaptor.capture());
    Ticket capturedTicket = ticketCaptor.getValue();
    
    // Assertions detalladas
    assertThat(capturedTicket)
        .extracting(Ticket::getNationalId, Ticket::getQueue)
        .containsExactly("12345678", "CAJA");
}
```

---

**Mantenido por:** Equipo de QA  
**Última actualización:** 2024-11-25  
**Próxima revisión:** 2025-02-25