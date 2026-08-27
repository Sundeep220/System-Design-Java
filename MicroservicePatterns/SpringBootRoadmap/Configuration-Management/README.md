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
```
