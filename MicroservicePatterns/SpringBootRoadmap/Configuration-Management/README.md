# Phase 14 -- Configuration Management

How Spring Boot loads, resolves, and binds configuration. Short topic but critical for production.

---

## 1. Configuration Sources and Priority

Spring Boot loads configuration from multiple sources. **Higher priority overrides lower:**

```text
Priority (highest to lowest):
-----------------------------------------------------------
1.  Command-line arguments          --server.port=9090
2.  SPRING_APPLICATION_JSON         (inline JSON in env var)
3.  ServletConfig/ServletContext     (web.xml params)
4.  JNDI attributes
5.  Java System properties           -Dserver.port=9090
6.  OS environment variables         SERVER_PORT=9090
7.  Profile-specific files           application-prod.yml
8.  application.yml / .properties    (inside or outside JAR)
9.  @PropertySource annotations
10. Default properties               (SpringApplication.setDefaultProperties)
```

### Practical implications

```text
application.yml sets:       server.port=8080
Environment variable sets:  SERVER_PORT=9090
Command-line sets:          --server.port=7070

Result: port = 7070  (command-line wins)
```

### Environment variable naming

```text
Property:              spring.datasource.url
Env var equivalent:    SPRING_DATASOURCE_URL

Rule: dots -> underscores, lowercase -> uppercase
Hyphens also become underscores: spring.jpa.show-sql -> SPRING_JPA_SHOW_SQL
```

---

## 2. application.yml vs application.properties

```properties
# application.properties (flat)
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/flowforge
spring.datasource.username=admin
spring.datasource.hikari.maximum-pool-size=20
```

```yaml
# application.yml (hierarchical, more readable)
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flowforge
    username: admin
    hikari:
      maximum-pool-size: 20
```

**Recommendation:** Use YAML for readability. Both work identically.

---

## 3. Profiles

Profiles let you have different configs for different environments.

```yaml
# application.yml (shared/default config)
server:
  port: 8080
spring:
  jpa:
    open-in-view: false

# application-dev.yml (development overrides)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flowforge_dev
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
logging:
  level:
    com.flowforge: DEBUG
    org.hibernate.SQL: DEBUG

# application-prod.yml (production overrides)
spring:
  datasource:
    url: ${DATABASE_URL}          # from env var
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate          # never auto-modify schema in prod
logging:
  level:
    com.flowforge: INFO
```

### Activating profiles

```text
1. application.yml:           spring.profiles.active=dev
2. Command line:              --spring.profiles.active=prod
3. Environment variable:      SPRING_PROFILES_ACTIVE=prod
4. JVM system property:       -Dspring.profiles.active=prod
5. Programmatically:          SpringApplication.setAdditionalProfiles("dev")
```

### Profile-specific beans

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        // H2 for local development
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() {
        // PostgreSQL for production
    }
}

@Configuration
@Profile("!prod")   // any profile EXCEPT prod
public class NonProdConfig {
    @Bean
    public MockEmailService emailService() {
        return new MockEmailService(); // don't send real emails in dev/test
    }
}
```

---

## 4. @Value -- Simple Property Injection

```java
@Service
public class NotificationService {

    @Value("${app.notification.enabled:true}")  // default = true
    private boolean enabled;

    @Value("${app.notification.max-retries:3}")
    private int maxRetries;

    @Value("${app.notification.sender-email}")  // no default = fail if missing
    private String senderEmail;

    @Value("${APP_SECRET_KEY}")  // from environment variable
    private String secretKey;

    @Value("#{${app.feature-flags}}")  // SpEL: parse as Map
    private Map<String, Boolean> featureFlags;
}
```

### Problems with @Value

```text
1. Scattered:    properties used across 20 classes, hard to find all of them
2. No type safety: typo in property name -> runtime failure
3. No grouping:  related properties not bundled together
4. No validation: can't enforce @NotNull, @Min on config values
```

---

## 5. @ConfigurationProperties -- Type-Safe Configuration (RECOMMENDED)

```yaml
# application.yml
flowforge:
  execution:
    max-retries: 3
    timeout-seconds: 300
    retry-delay-ms: 2000
    allowed-statuses:
      - PENDING
      - RUNNING
    notification:
      enabled: true
      channels:
        - email
        - slack
```

```java
@ConfigurationProperties(prefix = "flowforge.execution")
@Validated                    // enables validation
public class ExecutionProperties {

    @Min(0) @Max(10)
    private int maxRetries = 3;         // default value

    @Min(1)
    private int timeoutSeconds = 300;

    @Min(100)
    private long retryDelayMs = 2000;

    private List<String> allowedStatuses = List.of("PENDING", "RUNNING");

    @Valid                               // validate nested object
    private NotificationProperties notification = new NotificationProperties();

    // getters and setters

    public static class NotificationProperties {
        private boolean enabled = true;
        private List<String> channels = List.of("email");
        // getters and setters
    }
}
```

### Enable and use

```java
@Configuration
@EnableConfigurationProperties(ExecutionProperties.class)
public class AppConfig { }

// Or on the main class:
@SpringBootApplication
@ConfigurationProperties(prefix = "flowforge.execution")  // if using records

// Inject anywhere
@Service
@RequiredArgsConstructor
public class ExecutionService {
    private final ExecutionProperties props;

    public void execute(Execution exec) {
        if (exec.getAttempt() > props.getMaxRetries()) {
            markFailed(exec);
            return;
        }
        // ...
    }
}
```

### Immutable configuration with records (Spring Boot 3+)

```java
@ConfigurationProperties(prefix = "flowforge.execution")
public record ExecutionProperties(
    @DefaultValue("3") @Min(0) @Max(10) int maxRetries,
    @DefaultValue("300") @Min(1) int timeoutSeconds,
    @DefaultValue("2000") @Min(100) long retryDelayMs,
    @DefaultValue({"PENDING", "RUNNING"}) List<String> allowedStatuses
) {}
```

### @Value vs @ConfigurationProperties

```text
Feature                @Value              @ConfigurationProperties
-----------------------------------------------------------------------
Type safety            No                  Yes (compile-time binding)
Grouping               No                  Yes (nested objects)
Validation             Manual              @Validated + Bean Validation
Relaxed binding        Limited             Full (kebab-case, camelCase, etc.)
Metadata (IDE hints)   No                  Yes (spring-configuration-metadata.json)
Use case               1-2 simple values   Structured config groups
```

---

## 6. Secrets Management

### NEVER do this

```yaml
# application.yml committed to Git
spring:
  datasource:
    password: MyS3cretP@ssw0rd    # EXPOSED IN VERSION CONTROL
```

### Do this instead

```text
Option 1: Environment variables
  DATABASE_PASSWORD=MyS3cretP@ssw0rd

  spring:
    datasource:
      password: ${DATABASE_PASSWORD}

Option 2: Kubernetes Secrets
  kubectl create secret generic db-creds --from-literal=password=MyS3cretP@ssw0rd
  -> mounted as env var or file

Option 3: Vault / AWS Secrets Manager / Azure Key Vault
  spring:
    cloud:
      vault:
        host: vault.internal
        token: ${VAULT_TOKEN}

Option 4: Spring Cloud Config Server
  Centralized config server, encrypted values
```

### .env file for local development

```text
# .env (add to .gitignore!)
DATABASE_URL=jdbc:postgresql://localhost:5432/flowforge_dev
DATABASE_PASSWORD=localdev123
REDIS_HOST=localhost
KAFKA_BOOTSTRAP=localhost:9092
```

---

## 7. Configuration Precedence in Practice

```text
For FlowForge, recommended setup:

application.yml              -> shared defaults (port, JPA settings, logging format)
application-dev.yml          -> local dev (H2/local Postgres, DEBUG logging, show-sql)
application-test.yml         -> test (Testcontainers, in-memory, disable Kafka)
application-prod.yml         -> prod (env vars for secrets, INFO logging, validate DDL)

Secrets:                     -> ALWAYS from environment variables / Vault
                                NEVER in yml files
```

---

## 8. Refreshing Configuration at Runtime

### Spring Cloud Config + @RefreshScope

```java
@RestController
@RefreshScope                    // re-creates bean when /actuator/refresh is called
public class FeatureFlagController {

    @Value("${app.feature.new-ui-enabled:false}")
    private boolean newUiEnabled;

    @GetMapping("/features/new-ui")
    public boolean isNewUiEnabled() {
        return newUiEnabled;
    }
}

// POST /actuator/refresh -> re-reads config -> re-creates @RefreshScope beans
```

### When this matters

```text
Scenario: You want to toggle a feature flag without restarting 50 pods.

Without @RefreshScope:  restart all pods (downtime risk)
With @RefreshScope:     POST /actuator/refresh on each pod (or use Spring Cloud Bus)
```

---

## 9. Interview Questions

```text
Q: What is the property resolution order in Spring Boot?
A: Command-line args > env vars > profile-specific yml > application.yml > defaults.
   Higher sources override lower ones.

Q: How do you handle secrets in production?
A: Environment variables, Kubernetes Secrets, or Vault.
   NEVER commit secrets to application.yml in version control.

Q: @Value vs @ConfigurationProperties?
A: @Value for simple one-off values. @ConfigurationProperties for grouped,
   validated, type-safe config with IDE support.

Q: What is relaxed binding?
A: Spring Boot matches property names flexibly:
   app.max-retries = app.maxRetries = APP_MAX_RETRIES = app.max_retries
   All resolve to the same property.

Q: How do profiles work?
A: Profiles activate environment-specific config. application-{profile}.yml
   overrides application.yml. Activated via SPRING_PROFILES_ACTIVE env var.

Q: @ConfigurationProperties with records — how does it work?
A: Records use constructor binding automatically (Spring Boot 3+/4).
   Each record component maps to a config property. @DefaultValue provides defaults.
   Immutable, concise, and type-safe.

Q: What are virtual threads and when should I enable them?
A: Virtual threads (JDK 21+) are lightweight threads managed by the JVM.
   Enabled with spring.threads.virtual.enabled=true. Use for I/O-bound apps
   (DB queries, REST calls). They allow millions of concurrent tasks without
   platform thread exhaustion. Pair with @ConcurrencyLimit to protect resources.
```

---

# FlowForge Implementation

How we implemented configuration management in FlowForge.

---

## 10. Profile Architecture

```text
FlowForge uses a TWO-DIMENSIONAL profile strategy:

  Dimension 1: Environment    dev  |  prod
  Dimension 2: Database       postgres  |  h2

  Combination examples:
    dev,postgres    → local development with PostgreSQL (DEFAULT)
    dev,h2          → local development with H2 in-memory
    prod            → production (includes its own datasource from env vars)

  Default: spring.profiles.default=dev,postgres
  (used when no profiles are explicitly set)
```

### File Structure

```text
src/main/resources/
├── application.yaml              ← shared defaults (always loaded)
├── application-dev.yaml          ← development overrides
├── application-prod.yaml         ← production overrides
├── application-postgres.yaml     ← PostgreSQL datasource
├── application-h2.yaml           ← H2 datasource
└── schema-postgres.sql           ← GIN index for full-text search
```

### What Goes Where

```text
FILE                      WHAT IT CONFIGURES                             WHY
─────────────────────────────────────────────────────────────────────────────────────
application.yaml          App name, JPA open-in-view, virtual threads,   Always needed,
                          Jackson 3, server shutdown, FlowForge props    environment-agnostic

application-dev.yaml      DEBUG logging, show-sql, format_sql,           Dev-only settings
                          ddl-auto: create-drop                          that would be harmful in prod

application-prod.yaml     Datasource from env vars (no defaults!),       Prod-only security
                          validate DDL, no show-sql, WARN logging,       and performance settings
                          sql.init: never

application-postgres.yaml PostgreSQL datasource URL/credentials,          Database-specific,
                          defer-datasource-initialization, schema init   independent of environment

application-h2.yaml       H2 in-memory URL, H2 console enabled           Database-specific,
                                                                          no Postgres needed
```

### application.yaml (Shared Defaults)

```yaml
# --- Shared defaults (all profiles) ---
spring:
  application:
    name: flowforge

  profiles:
    default: dev,postgres          # default when nothing is explicitly set

  jpa:
    open-in-view: false            # prevent lazy-loading in view layer (best practice)

  threads:
    virtual:
      enabled: true                # enable virtual threads (Java 21+)

  jackson:
    serialization:
      write-dates-as-timestamps: false  # ISO 8601 date strings

server:
  shutdown: graceful               # drain in-flight requests before stopping

# --- Custom properties (bound via @ConfigurationProperties) ---
flowforge:
  execution:
    default-max-retries: 3
    default-timeout-seconds: 60
  rate-limit:
    max-requests: 50
    window-seconds: 60
```

```text
KEY DECISIONS:

  1. spring.profiles.default=dev,postgres
     When you run `mvn spring-boot:run` with no arguments, you get dev+postgres.
     This is safe — dev settings are only for local machines.

  2. spring.jpa.open-in-view=false
     Open-in-view keeps the Hibernate session open during view rendering.
     This can cause N+1 queries and lazy-loading surprises.
     Always disable it and use DTOs/explicit fetching.

  3. server.shutdown=graceful
     On SIGTERM, Spring waits for in-flight requests to finish before shutting down.
     Without this, requests get terminated mid-flight (data corruption risk).

  4. spring.threads.virtual.enabled=true
     Switches Tomcat from platform thread pool to virtual threads.
     Each request gets its own virtual thread — millions possible.
     Critical for I/O-bound apps (DB queries, REST calls to other services).
```

### application-dev.yaml

```yaml
spring:
  jpa:
    show-sql: true                   # print SQL to console
    hibernate:
      ddl-auto: create-drop          # recreate schema on each restart
    properties:
      hibernate:
        format_sql: true             # pretty-print SQL

logging:
  level:
    com.flowforge: DEBUG             # our code at DEBUG
    org.springframework.web: DEBUG   # Spring MVC at DEBUG
    org.hibernate.SQL: DEBUG         # Hibernate SQL at DEBUG
```

```text
WHY create-drop in dev?

  ddl-auto options:
    none         →  do nothing (production default for external DB)
    validate     →  verify schema matches entities (fail if mismatch)
    update       →  alter tables to match entities (DANGEROUS in prod)
    create       →  drop and recreate on startup
    create-drop  →  drop and recreate on startup, drop on shutdown

  In development: create-drop is fine — fresh schema every restart, no migration headaches.
  In production: validate — never let Hibernate touch the schema. Use Flyway/Liquibase.
```

### application-prod.yaml

```yaml
spring:
  datasource:
    url: ${FLOWFORGE_DB_URL}             # REQUIRED — no default!
    username: ${FLOWFORGE_DB_USERNAME}    # REQUIRED — no default!
    password: ${FLOWFORGE_DB_PASSWORD}    # REQUIRED — no default!
    driver-class-name: org.postgresql.Driver

  jpa:
    show-sql: false                       # no SQL logging in prod
    hibernate:
      ddl-auto: validate                  # verify schema, never modify
    defer-datasource-initialization: false
    properties:
      hibernate:
        format_sql: false

  sql:
    init:
      mode: never                         # don't run schema scripts

  h2:
    console:
      enabled: false                      # no H2 console in prod

server:
  port: ${PORT:8080}                      # port from env var

logging:
  level:
    root: WARN
    com.flowforge: INFO
    org.springframework.web: WARN
```

```text
KEY PRODUCTION DECISIONS:

  1. NO DEFAULT VALUES for datasource
     ${FLOWFORGE_DB_URL} with no :default means the app FAILS TO START
     if the env var is missing. This is intentional — fail fast.
     You never want production silently connecting to localhost:5432.

  2. ddl-auto: validate
     Hibernate checks that entities match the database schema.
     If there's a mismatch, the app fails to start.
     Schema changes are managed by migration tools (Flyway/Liquibase).

  3. sql.init.mode: never
     The schema-postgres.sql (GIN index for full-text search) is NOT run.
     In production, this index is created by a migration, not by Spring.

  4. WARN-level logging
     Only warnings and errors are logged. No DEBUG spam.
     This keeps log volume manageable and costs down (CloudWatch/Datadog).
```

### application-postgres.yaml (Dev Convenience)

```yaml
spring:
  datasource:
    url: ${FLOWFORGE_DB_URL:jdbc:postgresql://localhost:5432/flowforge}
    username: ${FLOWFORGE_DB_USERNAME:postgres}
    password: ${FLOWFORGE_DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    defer-datasource-initialization: true   # run SQL init AFTER Hibernate

  sql:
    init:
      mode: always                          # run schema-postgres.sql
      platform: postgres                    # use schema-postgres.sql (not schema.sql)

  h2:
    console:
      enabled: false
```

```text
NOTE: This profile has DEFAULT VALUES for the datasource credentials.
  url: ${FLOWFORGE_DB_URL:jdbc:postgresql://localhost:5432/flowforge}
                          ↑ fallback if env var is not set

  This is fine for dev — developers can run without setting env vars.
  In production, the 'prod' profile overrides with NO defaults.
```

---

## 11. @ConfigurationProperties with Records

### FlowForgeProperties.java

```java
@ConfigurationProperties(prefix = "flowforge")
public record FlowForgeProperties(
        @DefaultValue Execution execution,    // defaults to new Execution() with defaults
        @DefaultValue RateLimit rateLimit      // defaults to new RateLimit() with defaults
) {
    public record Execution(
            @DefaultValue("3") int defaultMaxRetries,
            @DefaultValue("60") int defaultTimeoutSeconds
    ) {}

    public record RateLimit(
            @DefaultValue("50") int maxRequests,
            @DefaultValue("60") int windowSeconds
    ) {}
}
```

### How It Works

```text
BINDING FLOW:

  application.yaml:                    Java record:
  ─────────────────                    ─────────────
  flowforge:                           FlowForgeProperties(
    execution:                           Execution(
      default-max-retries: 3    →          defaultMaxRetries = 3,
      default-timeout-seconds: 60 →        defaultTimeoutSeconds = 60
    rate-limit:                          ),
      max-requests: 50          →        RateLimit(
      window-seconds: 60        →          maxRequests = 50,
                                           windowSeconds = 60
                                         )
                                       )

  Spring uses RELAXED BINDING:
    default-max-retries  →  defaultMaxRetries   (kebab-case → camelCase)
    FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES     (env var → same property)
    flowforge.execution.defaultMaxRetries       (dot notation → same property)

  All three resolve to the same record field.
```

### @DefaultValue Explained

```text
@DefaultValue on a NESTED RECORD:
  @DefaultValue Execution execution
  → if 'flowforge.execution' is entirely missing from YAML,
    create an Execution with ITS defaults (3 and 60)
  → without @DefaultValue, a missing section = null → NullPointerException

@DefaultValue on a PRIMITIVE:
  @DefaultValue("3") int defaultMaxRetries
  → if 'flowforge.execution.default-max-retries' is missing, use 3
  → the value in YAML overrides this default
  → env var / CLI further overrides the YAML value
```

### Enabling @ConfigurationProperties

```java
@SpringBootApplication
@EnableConfigurationProperties(FlowForgeProperties.class)  // ← register the record
public class FlowforgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowforgeApplication.class, args);
    }
}
```

```text
Three ways to enable:

  1. @EnableConfigurationProperties(FlowForgeProperties.class)  ← explicit (we use this)
  2. @ConfigurationPropertiesScan                                ← auto-discover all
  3. @Bean FlowForgeProperties                                   ← manual bean (rare)

  Option 1 is most common — explicit about which properties classes exist.
  Option 2 is convenient for projects with many properties classes.
```

### Using FlowForgeProperties

```java
// In RateLimitInterceptor — replaces hardcoded constants
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int maxRequests;
    private final long windowMs;

    public RateLimitInterceptor(FlowForgeProperties properties) {
        this.maxRequests = properties.rateLimit().maxRequests();   // from YAML
        this.windowMs = properties.rateLimit().windowSeconds() * 1_000L;
    }
    // ...
}
```

```text
BEFORE (hardcoded):
  private static final int MAX_REQUESTS_PER_WINDOW = 50;  // change = recompile + redeploy
  private static final long WINDOW_MS = 60_000;

AFTER (@ConfigurationProperties):
  this.maxRequests = properties.rateLimit().maxRequests();  // change = edit YAML + restart
  this.windowMs = properties.rateLimit().windowSeconds() * 1_000L;

  Or override at runtime: --flowforge.rate-limit.max-requests=100
  No recompile, no redeploy — just a restart (or @RefreshScope for zero-downtime).
```

### Debug Endpoint — Verify Properties

```java
@GetMapping("/config")
public ResponseEntity<Map<String, Object>> configInfo() {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("activeProfiles", environment.getActiveProfiles());
    config.put("flowforge.execution.defaultMaxRetries", properties.execution().defaultMaxRetries());
    config.put("flowforge.execution.defaultTimeoutSeconds", properties.execution().defaultTimeoutSeconds());
    config.put("flowforge.rateLimit.maxRequests", properties.rateLimit().maxRequests());
    config.put("flowforge.rateLimit.windowSeconds", properties.rateLimit().windowSeconds());
    config.put("spring.threads.virtual.enabled", environment.getProperty("spring.threads.virtual.enabled"));
    config.put("server.port", environment.getProperty("server.port", "8080"));
    config.put("currentThread", Thread.currentThread().toString());
    return ResponseEntity.ok(config);
}
```

```text
GET /api/v1/debug/config response:

{
  "activeProfiles": ["dev", "postgres"],
  "flowforge.execution.defaultMaxRetries": 3,
  "flowforge.execution.defaultTimeoutSeconds": 60,
  "flowforge.rateLimit.maxRequests": 50,
  "flowforge.rateLimit.windowSeconds": 60,
  "spring.threads.virtual.enabled": "true",
  "server.port": "8080",
  "currentThread": "VirtualThread[#51]/runnable@ForkJoinPool-1-worker-1"
}

"currentThread" containing "VirtualThread" proves virtual threads are active.
```

---

## 12. Virtual Threads

### What They Are

```text
PLATFORM THREADS (traditional):
  - 1:1 mapping to OS threads
  - Expensive to create (~1MB stack each)
  - Limited by OS (thousands, not millions)
  - Thread pool is the bottleneck

  Tomcat default: 200 platform threads
  → max 200 concurrent requests
  → thread pool exhaustion → 503 errors

VIRTUAL THREADS (Java 21+):
  - Many-to-few mapping (millions of virtual threads → few OS threads)
  - Cheap to create (~few KB each)
  - JVM scheduler manages them
  - No thread pool bottleneck

  With virtual threads: each request gets its own virtual thread
  → millions of concurrent requests possible
  → blocking I/O (DB, HTTP calls) no longer wastes OS threads
```

### How To Enable

```yaml
spring:
  threads:
    virtual:
      enabled: true    # one line — that's it
```

```text
What this does in Spring Boot 4:

  1. Tomcat uses virtual threads for request handling
     (instead of a fixed platform thread pool)

  2. @Async tasks run on virtual threads

  3. Spring MVC handlers run on virtual threads

  You can verify by checking Thread.currentThread().toString()
  in any controller or service method:

    Platform thread:  Thread[tomcat-handler-1,5,main]
    Virtual thread:   VirtualThread[#51]/runnable@ForkJoinPool-1-worker-1
```

### Why Pair With @ConcurrencyLimit

```text
WITHOUT virtual threads + @ConcurrencyLimit:
  200 platform threads → 200 concurrent DB queries max → pool OK

WITH virtual threads, WITHOUT @ConcurrencyLimit:
  10,000 requests → 10,000 virtual threads → 10,000 concurrent DB queries
  → Hikari pool (default 10 connections) overwhelmed → timeout → failure

WITH virtual threads + @ConcurrencyLimit:
  10,000 requests → 10,000 virtual threads →
  @ConcurrencyLimit(5) on findAll() → only 5 concurrent DB queries
  → Hikari pool handles 5 easily → remaining threads wait their turn
  → no connection exhaustion

Virtual threads removed the thread pool as the natural bottleneck.
@ConcurrencyLimit provides an explicit, controlled bottleneck where you need one.
```

---

## 13. Jackson 3 Configuration (Spring Boot 4)

### What Changed

```text
Spring Boot 4 upgraded from Jackson 2 to Jackson 3.

  Jackson 2                       Jackson 3
  ─────────────────────────────────────────────
  com.fasterxml.jackson.*         tools.jackson.*
  ObjectMapper                    JsonMapper
  Jackson2ObjectMapperBuilder     JsonMapperBuilder
  Jackson2ObjectMapperCustomizer  JsonMapperBuilderCustomizer

  Spring Boot 4 auto-configures a JsonMapper bean.
  Configure it via spring.jackson.* properties (same namespace, new engine).
```

### Our Configuration

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false   # "2024-01-15T10:30:00Z" not 1705312200000
```

```text
WHY write-dates-as-timestamps: false?

  With timestamps (default Jackson):
    { "createdAt": 1705312200000 }          ← what does this mean?

  With ISO 8601 (Spring Boot default):
    { "createdAt": "2024-01-15T10:30:00Z" } ← human-readable, timezone-aware

  Spring Boot sets this to false by default, but we document it explicitly
  in our YAML to make the configuration visible and intentional.
```

### Other Useful Jackson Properties

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false    # ISO 8601 date strings
      indent-output: false                 # compact JSON (save bandwidth)
    deserialization:
      fail-on-unknown-properties: false    # ignore unknown JSON fields
    default-property-inclusion: always     # include null fields in response
    time-zone: UTC                         # default timezone for dates
```

### Programmatic Configuration (Alternative)

```java
// If you need more control than spring.jackson.* properties:
@Bean
public JsonMapperBuilderCustomizer flowForgeJacksonCustomizer() {
    return builder -> builder
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .defaultPropertyInclusion(JsonInclude.Include.NON_NULL);
}
```

```text
Use spring.jackson.* properties for simple settings.
Use JsonMapperBuilderCustomizer for complex/conditional configuration.
Both approaches configure the same auto-configured JsonMapper bean.
```

---

## 14. Property Override Precedence — Demonstrated

```text
We use flowforge.execution.default-max-retries to show override precedence:

SOURCE                              VALUE    WINS?
────────────────────────────────────────────────────────
application.yaml                    3        ← lowest priority
application-dev.yaml                (not set)
Environment variable                10       ← overrides YAML
  FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES=10
CLI argument                        99       ← overrides everything
  --flowforge.execution.default-max-retries=99
```

### How To Test

```text
1. Default (from YAML):
   mvn spring-boot:run
   GET /api/v1/debug/config → defaultMaxRetries: 3

2. Override via env var:
   set FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES=10    (Windows)
   export FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES=10 (Linux/Mac)
   mvn spring-boot:run
   GET /api/v1/debug/config → defaultMaxRetries: 10

3. Override via CLI:
   mvn spring-boot:run -Dspring-boot.run.arguments="--flowforge.execution.default-max-retries=99"
   GET /api/v1/debug/config → defaultMaxRetries: 99

4. Both env var AND CLI (CLI wins):
   FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES=10
   --flowforge.execution.default-max-retries=99
   GET /api/v1/debug/config → defaultMaxRetries: 99
```

### Environment Variable Naming Rules

```text
Property:                          flowforge.execution.default-max-retries
Env var:                           FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES

Rules:
  1. Dots (.) → underscores (_)
  2. Hyphens (-) → underscores (_)
  3. Lowercase → UPPERCASE
  4. Remove any trailing [index] brackets

  spring.datasource.url          →  SPRING_DATASOURCE_URL
  flowforge.rate-limit.max-requests → FLOWFORGE_RATELIMIT_MAXREQUESTS
                                     or FLOWFORGE_RATE_LIMIT_MAX_REQUESTS
                                     (Spring Boot supports both — relaxed binding)
```

---

## 15. FlowForge Config — File Structure Summary

```text
flowforge/src/main/
├── java/com/flowforge/flowforge/
│   ├── FlowforgeApplication.java         # @EnableConfigurationProperties(FlowForgeProperties.class)
│   ├── config/
│   │   ├── FlowForgeProperties.java      # @ConfigurationProperties record (flowforge.*)
│   │   ├── WebConfig.java                # WebMvcConfigurer (registers interceptors)
│   │   └── DataSeeder.java              # @Profile("!prod") — seeds dev data
│   ├── interceptor/
│   │   └── RateLimitInterceptor.java     # Uses FlowForgeProperties (not hardcoded)
│   └── controller/
│       └── DebugController.java          # GET /api/v1/debug/config (property inspection)
└── resources/
    ├── application.yaml                   # Shared defaults + flowforge.* properties
    ├── application-dev.yaml               # DEBUG logging, show-sql, create-drop
    ├── application-prod.yaml              # Env vars, INFO logging, validate
    ├── application-postgres.yaml          # PostgreSQL datasource
    ├── application-h2.yaml                # H2 datasource
    └── schema-postgres.sql                # GIN index for full-text search
```

---

## 16. Configuration Management Summary

```text
1. PROFILES
   - Two-dimensional: environment (dev/prod) × database (postgres/h2)
   - Default: dev,postgres (when nothing is set)
   - Production: --spring.profiles.active=prod (self-contained)
   - Beans can be profile-conditional: @Profile("!prod")

2. @CONFIGURATIONPROPERTIES
   - Type-safe, grouped, validated configuration
   - Records = immutable + concise (no getters/setters)
   - @DefaultValue for fallback values
   - @EnableConfigurationProperties to register
   - Inject as a constructor parameter anywhere

3. PROPERTY PRECEDENCE
   - YAML < profile YAML < env var < CLI arg
   - Higher sources override lower ones
   - Use /api/v1/debug/config to inspect resolved values

4. VIRTUAL THREADS
   - spring.threads.virtual.enabled=true
   - Millions of concurrent requests without thread pool exhaustion
   - Pair with @ConcurrencyLimit to protect downstream resources

5. JACKSON 3 (Spring Boot 4)
   - Auto-configured via spring.jackson.* properties
   - Or programmatically via JsonMapperBuilderCustomizer
   - ObjectMapper → JsonMapper, different package (tools.jackson.*)

6. SECRETS
   - NEVER in YAML files committed to Git
   - Use env vars: ${FLOWFORGE_DB_PASSWORD}
   - Prod profile has NO defaults — fails fast if env vars missing
```
