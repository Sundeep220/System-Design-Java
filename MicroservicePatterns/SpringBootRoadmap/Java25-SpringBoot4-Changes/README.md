# Java 25 + Spring Boot 4 -- Major Changes & New Features

We are using **Java 25 (LTS)** and **Spring Boot 4.0.x (Spring Framework 7)**. This covers every major change with code snippets so you know what's different as you go deeper into each roadmap topic.

---

## 1. Jackson 3 (Default in Spring Boot 4)

The biggest breaking change. Jackson moved from `com.fasterxml.jackson` to `tools.jackson`.

### Package changes

```java
// Spring Boot 3 (Jackson 2)
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;

// Spring Boot 4 (Jackson 3)
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.core.JacksonException;

// EXCEPTION: annotations stay in the OLD package (shared between Jackson 2 and 3)
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
// These do NOT change!
```

### ObjectMapper -> JsonMapper (immutable)

```java
// Spring Boot 3 -- mutable ObjectMapper
ObjectMapper mapper = new ObjectMapper();
mapper.enable(SerializationFeature.INDENT_OUTPUT);   // can change state anytime
mapper.registerModule(new JavaTimeModule());          // mutable, NOT thread-safe

// Spring Boot 4 -- immutable JsonMapper (builder pattern)
JsonMapper mapper = JsonMapper.builder()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .addModule(new JavaTimeModule())
    .build();   // configuration LOCKED after build(), thread-safe
```

### Spring Boot auto-configures JsonMapper

```java
// Spring Boot 4 auto-configures a JsonMapper bean. Just inject it:
@Service
public class WorkflowExporter {
    private final JsonMapper jsonMapper;  // auto-configured by Spring Boot 4

    public WorkflowExporter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String toJson(Workflow workflow) {
        return jsonMapper.writeValueAsString(workflow);
        // Note: JacksonException is now a RuntimeException, not IOException!
    }
}
```

### Customizing JsonMapper

```java
// Spring Boot 3
@Bean
Jackson2ObjectMapperBuilderCustomizer customizer() {
    return builder -> builder.featuresToEnable(SerializationFeature.INDENT_OUTPUT);
}

// Spring Boot 4
@Bean
JsonMapperBuilderCustomizer customizer() {
    return builder -> builder
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
}
```

### Exception handling change (SILENT RUNTIME BREAK)

```java
// Spring Boot 3 (Jackson 2): JsonProcessingException extends IOException
try {
    return objectMapper.readValue(json, MyDto.class);
} catch (IOException e) {   // catches both IO errors AND Jackson parse errors
    log.error("Failed", e);
}

// Spring Boot 4 (Jackson 3): JacksonException extends RuntimeException
try {
    return jsonMapper.readValue(json, MyDto.class);
} catch (IOException e) {         // ONLY catches real IO errors now
    log.error("IO failure", e);
} catch (JacksonException e) {    // MUST explicitly catch Jackson errors
    log.error("Parse failure", e);
}
// WARNING: old catch(IOException) silently stops catching parse errors!
```

### Renamed Spring Boot classes

```text
Spring Boot 3                           Spring Boot 4
--------------------------------------------------------------------
Jackson2ObjectMapperBuilderCustomizer   JsonMapperBuilderCustomizer
@JsonComponent                          @JacksonComponent
@JsonMixin                              @JacksonMixin
JsonObjectSerializer                    ObjectValueSerializer
JsonObjectDeserializer                  ObjectValueDeserializer
```

### Properties changes

```yaml
# Spring Boot 3
spring:
  jackson:
    read:
      FAIL_ON_UNKNOWN_PROPERTIES: false
    write:
      WRITE_DATES_AS_TIMESTAMPS: false

# Spring Boot 4 -- moved under spring.jackson.json.*
spring:
  jackson:
    json:
      read:
        FAIL_ON_UNKNOWN_PROPERTIES: false
      write:
        WRITE_DATES_AS_TIMESTAMPS: false
```

---

## 2. Built-in API Versioning (Spring Framework 7)

No more manual URI/header versioning. Spring Boot 4 has first-class support.

### Configuration via application.yml

```yaml
# Choose ONE strategy:

# Option 1: Header-based
spring:
  mvc:
    apiversion:
      use:
        header: X-API-Version
      supported: 1.0, 2.0
      default: 1.0

# Option 2: Path segment (e.g., /api/v1/workflows)
spring:
  mvc:
    apiversion:
      use:
        path-segment: 1          # index of the path segment with version
      supported: 1.0, 2.0
      default: 1.0

# Option 3: Query parameter (e.g., /workflows?version=2.0)
spring:
  mvc:
    apiversion:
      use:
        query-parameter: version
      supported: 1.0, 2.0
      default: 1.0
```

### Or configure via Java

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("X-API-Version")     // or .usePathSegment(1)
            .addSupportedVersions("1.0", "2.0")
            .setDefaultVersion("1.0");
    }
}
```

### Controller: use `version` attribute on mappings

```java
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    // v1.0 response
    @GetMapping(value = "/{id}", version = "1.0")
    public WorkflowResponseV1 getWorkflowV1(@PathVariable Long id) {
        return workflowService.findByIdV1(id);
    }

    // v2.0 response (new fields, different structure)
    @GetMapping(value = "/{id}", version = "2.0")
    public WorkflowResponseV2 getWorkflowV2(@PathVariable Long id) {
        return workflowService.findByIdV2(id);
    }

    // "1.0+" means version 1.0 and all later versions (unless a more specific match exists)
    @PostMapping(version = "1.0+")
    public WorkflowResponseV1 createWorkflow(@Valid @RequestBody WorkflowCreateRequest req) {
        return workflowService.create(req);
    }
}

// Client sends:
// curl -H "X-API-Version: 2.0" http://localhost:8080/api/workflows/42
// -> routes to getWorkflowV2()
```

### Deprecation support

```java
// Mark old versions as deprecated -- Spring sends deprecation headers to clients
@GetMapping(value = "/{id}", version = "1.0")
@Deprecated
public WorkflowResponseV1 getWorkflowV1(@PathVariable Long id) {
    return workflowService.findByIdV1(id);
}
```

---

## 3. Built-in @Retryable & @ConcurrencyLimit (Spring Framework 7)

Previously required `spring-retry` dependency. Now in `spring-core` and `spring-context`.

### Enable

```java
@Configuration
@EnableResilientMethods   // activates @Retryable and @ConcurrencyLimit
public class AppConfig { }
```

### @Retryable -- declarative retry

```java
@Service
public class NotificationService {

    // Default: 3 retries, 1 second delay
    @Retryable
    public void sendNotification(String message) {
        restClient.post()
            .uri("https://notification-service/send")
            .body(message)
            .retrieve()
            .toBodilessEntity();
    }

    // Custom retry config
    @Retryable(
        maxRetries = 5,
        delayString = "100ms",        // initial delay
        multiplier = 2,               // exponential backoff (100, 200, 400, 800, 1000)
        maxDelayString = "1000ms",    // cap at 1 second
        jitterString = "100ms",       // random jitter to avoid thundering herd
        includes = {HttpServerErrorException.class}  // only retry on 5xx
    )
    public String callExternalApi(Long workflowId) {
        return restClient.get()
            .uri("/external/workflows/{id}", workflowId)
            .retrieve()
            .body(String.class);
    }
}
```

### RetryTemplate -- programmatic retry

```java
@Service
public class WorkflowExecutor {

    public void executeWithRetry(Workflow workflow) {
        RetryPolicy policy = RetryPolicy.builder()
            .maxRetries(5)
            .delay(Duration.ofMillis(200))
            .multiplier(2)
            .maxDelay(Duration.ofSeconds(2))
            .jitter(Duration.ofMillis(50))
            .includes(TimeoutException.class, IOException.class)
            .build();

        RetryTemplate retryTemplate = new RetryTemplate(policy);

        retryTemplate.invoke(() -> {
            executeStep(workflow);
            return null;
        });
    }
}
```

### @ConcurrencyLimit -- throttle concurrent access

```java
@Service
public class ExternalApiService {

    // Only 10 threads can call this method simultaneously
    // Others BLOCK until a slot opens (like a semaphore)
    @ConcurrencyLimit(10)
    public String callRateLimitedApi(String request) {
        return restClient.post()
            .uri("https://external-api/process")
            .body(request)
            .retrieve()
            .body(String.class);
    }

    // REJECT policy: immediately throws if limit reached (instead of blocking)
    @ConcurrencyLimit(limit = 5, policy = ThrottlePolicy.REJECT)
    public String callCriticalApi(String request) {
        return restClient.post()
            .uri("https://critical-api/process")
            .body(request)
            .retrieve()
            .body(String.class);
    }

    // Lock to single thread (mutual exclusion)
    @ConcurrencyLimit(1)
    public void updateSharedResource() {
        // only one thread at a time
    }
}
```

### Works with reactive return types too

```java
@Retryable(maxRetries = 3, delayString = "500ms")
public Mono<String> callReactiveApi() {
    return webClient.get()
        .uri("/api/data")
        .retrieve()
        .bodyToMono(String.class);
    // Spring automatically decorates the reactive pipeline with Reactor's retry
}
```

---

## 4. HTTP Service Clients (Spring Boot 4 Auto-Configuration)

Define an interface, Spring generates the implementation. No boilerplate RestClient setup.

```java
// Define the interface
@HttpExchange("/api/v1/notifications")
public interface NotificationServiceClient {

    @PostExchange
    void send(@RequestBody NotificationRequest request);

    @GetExchange("/{id}")
    NotificationResponse getById(@PathVariable Long id);

    @GetExchange
    List<NotificationResponse> findAll(
        @RequestParam("status") String status);
}

// Spring Boot 4 auto-configures the implementation.
// Just inject and use:
@Service
public class WorkflowNotifier {
    private final NotificationServiceClient client;  // auto-configured!

    public void notifyComplete(Long workflowId) {
        client.send(new NotificationRequest(workflowId, "Workflow completed"));
    }
}
```

```yaml
# Configure the base URL
spring:
  http:
    client:
      notification-service:
        url: http://notification-service:8080
```

---

## 5. JSpecify Null Safety (Across Entire Spring Portfolio)

Spring Boot 4 annotated the entire codebase with JSpecify `@Nullable` / `@NonNull`.

```java
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@Service
public class WorkflowService {

    // IDE now warns if you pass null to a @NonNull parameter
    public Workflow findById(@NonNull Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow", id));
    }

    // Explicitly nullable return -- IDE warns callers to null-check
    public @Nullable Workflow findBySlug(@NonNull String slug) {
        return repository.findBySlug(slug).orElse(null);
    }
}
```

```text
What this means in practice:
  - Your IDE (IntelliJ) shows warnings when you pass null to @NonNull params
  - Spring's own methods now declare nullability -- no more guessing
  - Consistent across Spring Framework, Data, Security, Boot
  - Replaces Spring's old @Nullable annotation
```

---

## 6. Java 25 Features

### Module Import Declarations (JEP 511)

```java
// Before: many individual imports
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Java 25: one module import covers all java.util, java.io, java.time, etc.
import module java.base;
```

### Flexible Constructor Bodies (JEP 513)

```java
// Before Java 25: couldn't validate before super()
public class WorkflowStep extends BaseEntity {
    private final String name;

    public WorkflowStep(String name, Long workflowId) {
        super(workflowId);            // HAD to be first line
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name required");
            // Too late! super() already ran with potentially bad data
        }
        this.name = name;
    }
}

// Java 25: validate BEFORE super()
public class WorkflowStep extends BaseEntity {
    private final String name;

    public WorkflowStep(String name, Long workflowId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name required");
            // Fails BEFORE super() -- no half-constructed object
        }
        this.name = name;             // can initialize fields before super()
        super(workflowId);            // super() no longer required to be first
    }
}
```

### Scoped Values (JEP 506) -- replacement for ThreadLocal

```java
// ThreadLocal (old way) -- mutable, easy to leak, problems with virtual threads
private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

public void handleRequest() {
    TENANT_ID.set("tenant-123");
    try {
        processRequest();
    } finally {
        TENANT_ID.remove();  // must manually clean up!
    }
}

// Scoped Values (Java 25) -- immutable, auto-cleanup, virtual thread friendly
private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

public void handleRequest() {
    ScopedValue.runWhere(TENANT_ID, "tenant-123", () -> {
        processRequest();
        // TENANT_ID is available here and in all called methods
        // automatically cleaned up when lambda exits
        // no risk of leaking to other requests
    });
}

// Reading the value anywhere in the call chain:
public void processRequest() {
    String tenantId = TENANT_ID.get();  // "tenant-123"
}
```

### Compact Object Headers (JEP 519) -- automatic performance

```text
No code changes needed. The JVM automatically uses 64-bit object headers
instead of 96-128 bits. This means:
  - Every Java object uses 4-8 bytes less memory
  - Significant savings with millions of objects (entity caches, collections)
  - Free performance upgrade just by using Java 25
  - Enable with: -XX:+UseCompactObjectHeaders (default in Java 25)
```

### Structured Concurrency (Preview, JEP 505)

```java
// Run multiple tasks as a single unit -- if one fails, cancel all
public WorkflowResult executeParallelSteps(List<Step> steps) throws Exception {
    try (var scope = StructuredTaskScope.open()) {
        // Fork all steps to run concurrently
        List<StructuredTaskScope.Subtask<StepResult>> subtasks = steps.stream()
            .map(step -> scope.fork(() -> executeStep(step)))
            .toList();

        scope.join();  // wait for ALL to complete (or fail fast)

        return new WorkflowResult(
            subtasks.stream()
                .map(StructuredTaskScope.Subtask::get)
                .toList()
        );
    }
    // If any step fails, all others are cancelled automatically
    // No orphaned threads, no resource leaks
}
```

---

## 7. Other Spring Boot 4 Changes

### Virtual Threads

```yaml
# Enable virtual threads (Java 21+)
spring:
  threads:
    virtual:
      enabled: true
# JDK HttpClient is auto-configured to use virtual threads
# Each request gets its own virtual thread -- no thread pool bottleneck
```

### OpenTelemetry Starter

```xml
<!-- One starter for metrics + traces via OTLP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

### RestTestClient (for testing)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowControllerTest {

    @Autowired
    RestTestClient restTestClient;

    @Test
    void shouldCreateWorkflow() {
        restTestClient.post()
            .uri("/api/v1/workflows")
            .body(new WorkflowCreateRequest("Test", "desc", 3, 300))
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().exists("Location")
            .expectBody(WorkflowResponse.class)
            .value(wf -> assertThat(wf.name()).isEqualTo("Test"));
    }
}
```

### Dependency Versions

```text
Spring Framework:   7.0
Spring Security:    7.0
Spring Data:        2025.1
Hibernate:          7.1 (JPA 3.2)
Micrometer:         1.16
Reactor:            2025.0
Jakarta EE:         11 (Servlet 6.1, Bean Validation 3.1)
Flyway:             11.x
```

---

## Quick Reference: Boot 3.x vs Boot 4.x

```text
What Changed                          Boot 3.x                        Boot 4.x
-------------------------------------------------------------------------------------------
Java baseline                         17+                             17+ (first-class 25)
Jackson                               2.x (com.fasterxml.jackson)     3.x (tools.jackson)
Mapper                                ObjectMapper (mutable)          JsonMapper (immutable)
Mapper customizer                     Jackson2ObjectMapper...         JsonMapperBuilderCustomizer
@JsonComponent                        @JsonComponent                  @JacksonComponent
Jackson exceptions                    extends IOException             extends RuntimeException
API versioning                        Manual (URI/header)             Built-in (spring.mvc.apiversion.*)
Retry                                 spring-retry dependency         Built-in @Retryable
Concurrency throttle                  Manual                          Built-in @ConcurrencyLimit
Enable resilience                     @EnableRetry                    @EnableResilientMethods
Null safety                           @Nullable (Spring)              @Nullable (JSpecify)
HTTP service clients                  Manual setup                    Auto-configured @HttpExchange
Auto-config module                    One big jar                     Many small jars
Bean Validation                       3.0                             3.1
Hibernate                             6.x                             7.1
Security                              Spring Security 6               Spring Security 7
Observability                         Micrometer + manual OTel        spring-boot-starter-opentelemetry
Testing REST                          MockMvc / WebTestClient         + RestTestClient
ThreadLocal alternative               ThreadLocal                     ScopedValue (Java 25)
Constructor flexibility               super() must be first           Flexible (Java 25 JEP 513)
```
