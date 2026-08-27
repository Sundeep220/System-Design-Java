# Phase 13 -- Spring Boot Internals

Spring Boot is not a separate framework. It is an **opinionated auto-configuration layer** on top of Spring Framework. Understanding what happens between `main()` and "your controller serving requests" is what separates SDE1 from SDE2.

---

## 1. What Spring Boot Actually Does

```text
Without Spring Boot (plain Spring):
  1. Create web.xml
  2. Configure DispatcherServlet manually
  3. Add Jackson dependency + configure ObjectMapper
  4. Add HikariCP dependency + configure DataSource
  5. Add Hibernate dependency + configure EntityManagerFactory
  6. Add Tomcat dependency + configure embedded server
  7. Wire everything together in XML or Java config
  = 200+ lines of boilerplate before writing one controller

With Spring Boot:
  1. Add spring-boot-starter-web
  2. Write @RestController
  3. Run main()
  = Everything auto-configured
```

Spring Boot's job:

```text
1. Detect what's on the classpath
2. Auto-configure sensible defaults
3. Let you override anything
```

---

## 2. @SpringBootApplication -- The Entry Point

```java
@SpringBootApplication
public class FlowForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowForgeApplication.class, args);
    }
}
```

`@SpringBootApplication` is a composed annotation:

```java
@SpringBootConfiguration     // = @Configuration (this class is a config source)
@EnableAutoConfiguration     // = turn on auto-configuration magic
@ComponentScan               // = scan this package + sub-packages for beans
public @interface SpringBootApplication { }
```

So this single annotation does three things:

```text
1. @Configuration:           Makes the main class a bean definition source
2. @EnableAutoConfiguration: Triggers classpath-based auto-configuration
3. @ComponentScan:           Scans for @Component, @Service, @Repository, etc.
```

---

## 3. SpringApplication.run() -- Complete Startup Sequence

```text
SpringApplication.run(FlowForgeApplication.class, args)
    |
    |-- 1. Create SpringApplication instance
    |       |-- detect web application type (SERVLET, REACTIVE, NONE)
    |       |-- load SpringApplicationRunListeners (from spring.factories / META-INF)
    |       |-- load ApplicationContextInitializers
    |
    |-- 2. Run listeners: starting()
    |
    |-- 3. Prepare Environment
    |       |-- create Environment (StandardServletEnvironment)
    |       |-- load properties: application.yml, env vars, system props, CLI args
    |       |-- resolve profiles (dev, prod, etc.)
    |       |-- bind properties to @ConfigurationProperties beans
    |
    |-- 4. Print Banner (the Spring logo)
    |
    |-- 5. Create ApplicationContext
    |       |-- AnnotationConfigServletWebServerApplicationContext (for web apps)
    |
    |-- 6. Prepare Context
    |       |-- register main class as a bean definition
    |       |-- run ApplicationContextInitializers
    |
    |-- 7. Refresh Context (THIS IS THE BIG ONE)
    |       |-- Component Scan: find all @Component classes
    |       |-- Process @Configuration classes
    |       |-- Process @EnableAutoConfiguration
    |       |     |-- load auto-configuration classes from META-INF/spring/
    |       |     |-- evaluate @Conditional annotations
    |       |     |-- register matching auto-configurations as beans
    |       |-- Create all singleton beans (dependency order)
    |       |-- Run BeanPostProcessors (create proxies for @Transactional, etc.)
    |       |-- Initialize embedded web server (Tomcat/Jetty/Undertow)
    |       |-- Register DispatcherServlet
    |       |-- Register Filters (Security, etc.)
    |
    |-- 8. Run listeners: started()
    |
    |-- 9. Call ApplicationRunner / CommandLineRunner beans
    |
    |-- 10. Run listeners: ready()
    |
    |-- APPLICATION IS READY TO SERVE REQUESTS
```

### Timing

```text
Typical startup: 2-8 seconds

Where time is spent:
  Component scanning:        10-20%
  Auto-configuration:        10-15%
  Bean creation:             20-30%
  Hibernate initialization:  20-40%  (schema validation, entity scanning)
  Embedded server start:     5-10%
```

---

## 4. Auto-Configuration -- How It Works

### The mechanism

```text
1. You add a dependency:  spring-boot-starter-data-jpa
2. This brings classes:   HikariDataSource, EntityManagerFactory, etc.
3. Spring Boot scans:     META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
4. Finds:                 DataSourceAutoConfiguration, HibernateJpaAutoConfiguration, etc.
5. Evaluates conditions:  @ConditionalOnClass(DataSource.class) -> true (HikariCP on classpath)
6. Registers beans:       DataSource, EntityManagerFactory, TransactionManager
```

### Example: DataSourceAutoConfiguration (simplified)

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)              // only if DataSource class exists
@ConditionalOnMissingBean(DataSource.class)         // only if user hasn't defined their own
@EnableConfigurationProperties(DataSourceProperties.class)  // bind spring.datasource.*
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource", name = "url")
    public DataSource dataSource(DataSourceProperties props) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        ds.setMaximumPoolSize(10);   // sensible default
        return ds;
    }
}
```

### The key conditional annotations

```text
@ConditionalOnClass          -- class exists on classpath
@ConditionalOnMissingClass   -- class does NOT exist
@ConditionalOnBean           -- specific bean already exists
@ConditionalOnMissingBean    -- no bean of this type exists (USER OVERRIDE POINT)
@ConditionalOnProperty       -- config property has specific value
@ConditionalOnWebApplication -- running as a web app
@ConditionalOnResource       -- specific resource file exists
```

### Override order

```text
                          Highest priority
                              |
    User-defined @Bean    <---+  (your @Configuration class)
                              |
    @ConditionalOnMissingBean checks for user beans first
                              |
    Auto-configuration    <---+  (only if user didn't define it)
                              |
                          Lowest priority

This is why you can always override auto-configuration:
just define your own @Bean of the same type.
```

### Example: overriding auto-configured ObjectMapper

```java
// Spring Boot auto-configures an ObjectMapper
// But if you define your own, auto-configuration backs off

@Configuration
public class JacksonConfig {

    @Bean   // this takes precedence over auto-configured ObjectMapper
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
```

---

## 5. Starters -- Dependency Groups

A starter is just a POM/Gradle file that bundles related dependencies:

```text
spring-boot-starter-web
    |-- spring-boot-starter          (core + auto-config + logging)
    |-- spring-web                   (Spring MVC)
    |-- spring-webmvc                (DispatcherServlet)
    |-- spring-boot-starter-tomcat   (embedded Tomcat)
    |-- spring-boot-starter-json     (Jackson)

spring-boot-starter-data-jpa
    |-- spring-boot-starter          (core)
    |-- spring-data-jpa              (JPA repositories)
    |-- hibernate-core               (JPA implementation)
    |-- spring-boot-starter-jdbc     (HikariCP + JDBC)

spring-boot-starter-security
    |-- spring-security-core
    |-- spring-security-config
    |-- spring-security-web
```

### What a starter triggers

```text
Add starter to pom.xml
    -> classes appear on classpath
    -> auto-configuration detects them (@ConditionalOnClass)
    -> beans are created with sensible defaults
    -> you configure via application.yml

Example:
  Add spring-boot-starter-data-redis
  -> RedisAutoConfiguration detects Lettuce/Jedis on classpath
  -> creates RedisConnectionFactory bean
  -> creates RedisTemplate bean
  -> creates StringRedisTemplate bean
  -> you just set spring.data.redis.host=localhost
```

---

## 6. Embedded Server

Spring Boot embeds the web server inside the application:

```text
Traditional:
  Build WAR file -> Deploy to external Tomcat -> Tomcat runs your app

Spring Boot:
  Build JAR file -> java -jar app.jar -> Tomcat starts INSIDE your app
```

### How the embedded server starts

```text
During context refresh:
  1. ServletWebServerFactory bean is created (TomcatServletWebServerFactory)
  2. Factory creates a Tomcat instance
  3. DispatcherServlet is registered as a servlet
  4. Filters are registered (Security, CORS, etc.)
  5. Tomcat starts listening on port 8080
```

### Switching servers

```xml
<!-- Use Jetty instead of Tomcat -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

### Server configuration

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200          # max worker threads
      min-spare: 10     # min idle threads
    max-connections: 8192
    accept-count: 100   # queue when all threads busy
    connection-timeout: 20000  # ms
  shutdown: graceful     # finish in-flight requests before stopping
  
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # max wait for graceful shutdown
```

---

## 7. Application Startup Events

Spring Boot fires events in this order:

```text
1. ApplicationStartingEvent          -- before anything (logging not ready)
2. ApplicationEnvironmentPreparedEvent -- Environment ready, context not created
3. ApplicationContextInitializedEvent  -- context created, beans not loaded
4. ApplicationPreparedEvent           -- beans loaded, not refreshed
5. ContextRefreshedEvent              -- context refreshed, all beans ready
6. ApplicationStartedEvent           -- app started, runners not called
7. ApplicationReadyEvent             -- FULLY READY (runners completed)

On failure:
   ApplicationFailedEvent            -- startup failed
```

### Listening to events

```java
@Component
public class StartupListener {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("FlowForge is ready to serve requests!");
        // warm up caches, verify external connections, etc.
    }
}
```

### ApplicationRunner vs CommandLineRunner

```java
// Runs after ALL beans are initialized, just before ApplicationReadyEvent
@Component
public class DatabaseSeeder implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("seed")) {
            seedDatabase();
        }
    }
}

@Component
@Order(1)  // control execution order
public class HealthChecker implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // verify external dependencies are reachable
        verifyDatabaseConnection();
        verifyRedisConnection();
        verifyKafkaConnection();
    }
}
```

---

## 8. Debug Auto-Configuration

### See what was auto-configured

```yaml
# application.yml
debug: true
# or
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
```

This prints:
```text
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'javax.sql.DataSource'
      - @ConditionalOnMissingBean did not find any beans of type 'DataSource'

   HibernateJpaAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'EntityManager', 'SessionFactory'

Negative matches:
-----------------
   RedisAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'RedisConnectionFactory'

   MongoAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'MongoClient'
```

### Exclude specific auto-configurations

```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,    // don't auto-configure DB
    SecurityAutoConfiguration.class       // don't auto-configure security
})
public class FlowForgeApplication { }
```

Or in application.yml:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 9. Spring Boot Actuator (Preview)

Actuator exposes operational endpoints:

```text
/actuator/health      -- is the app healthy?
/actuator/info        -- build info, git commit
/actuator/metrics     -- JVM, HTTP, custom metrics
/actuator/beans       -- all beans in the context
/actuator/env         -- all configuration properties
/actuator/conditions  -- auto-configuration conditions report
/actuator/mappings    -- all @RequestMapping endpoints
/actuator/loggers     -- view and change log levels at runtime
```

### Startup performance

```yaml
# See exact bean creation times
spring:
  main:
    lazy-initialization: true   # defer bean creation until needed (faster startup)
```

```text
WARNING: lazy-initialization hides errors until first request.
         Use only in development for faster feedback.
         In production, you WANT startup to fail fast.
```

---

## 10. Fat JAR / Uber JAR

```text
mvn package -> target/flowforge-0.0.1-SNAPSHOT.jar

This JAR contains:
  flowforge-0.0.1-SNAPSHOT.jar
    |-- BOOT-INF/
    |     |-- classes/         (your compiled code)
    |     |-- lib/             (ALL dependency JARs: Spring, Hibernate, Tomcat, etc.)
    |
    |-- META-INF/
    |     |-- MANIFEST.MF      (Main-Class: JarLauncher, Start-Class: FlowForgeApplication)
    |
    |-- org/springframework/boot/loader/
          |-- JarLauncher.class  (Spring Boot's custom classloader)
```

```text
java -jar flowforge-0.0.1-SNAPSHOT.jar

1. JVM calls JarLauncher.main()
2. JarLauncher creates a custom ClassLoader
3. ClassLoader can read nested JARs (BOOT-INF/lib/*.jar)
4. JarLauncher calls FlowForgeApplication.main()
5. SpringApplication.run() starts
```

### Why a custom classloader?

```text
Normal Java:  java -jar app.jar  ->  can't load JARs inside JARs
Spring Boot:  JarLauncher creates a classloader that CAN read nested JARs
              This is why you can have 200+ dependency JARs inside one JAR
```

---

## 11. Creating Your Own Auto-Configuration

For shared libraries across FlowForge services:

```java
// In a shared library module: flowforge-common

@AutoConfiguration
@ConditionalOnClass(IdempotencyService.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyService idempotencyService(
            IdempotencyProperties props,
            IdempotencyRepository repo) {
        return new IdempotencyService(repo, props.getTtl());
    }
}

@ConfigurationProperties(prefix = "flowforge.idempotency")
public class IdempotencyProperties {
    private Duration ttl = Duration.ofHours(24);
    // getters/setters
}
```

Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```text
com.flowforge.common.IdempotencyAutoConfiguration
```

Now any FlowForge service that depends on `flowforge-common` automatically gets `IdempotencyService`.

---

## 12. Interview Deep Dive Questions

```text
Q: What does @SpringBootApplication do?
A: It combines @Configuration + @EnableAutoConfiguration + @ComponentScan.
   It makes the class a config source, enables auto-configuration based on
   classpath, and scans the current package tree for beans.

Q: How does auto-configuration work?
A: Spring Boot reads auto-configuration classes from META-INF/spring/ files.
   Each class has @Conditional annotations. If conditions match (class on
   classpath, no user-defined bean, property set), the auto-configuration
   creates beans with sensible defaults.

Q: How do you override auto-configuration?
A: Define your own @Bean of the same type. @ConditionalOnMissingBean in
   auto-configuration backs off when it finds your bean.

Q: What is a starter?
A: A Maven/Gradle dependency that bundles related libraries. Adding a starter
   puts classes on the classpath, which triggers auto-configuration.

Q: How does the embedded server work?
A: Spring Boot creates a ServletWebServerFactory bean, which creates a
   Tomcat/Jetty/Undertow instance. DispatcherServlet and filters are
   registered. The server starts inside the application process.

Q: What is the fat JAR and how does it work?
A: Spring Boot packages your code + all dependencies into one JAR.
   JarLauncher uses a custom ClassLoader to read nested JARs.
   This makes deployment simple: just java -jar app.jar.

Q: What is the startup sequence?
A: Create SpringApplication -> Prepare Environment -> Create Context ->
   Component Scan -> Auto-Configure -> Create Beans -> Start Server ->
   Run Runners -> Application Ready.

Q: How would you debug slow startup?
A: Enable debug=true to see conditions report. Use spring.main.lazy-initialization
   for dev. Check Hibernate auto-ddl (disable in prod). Profile with
   ApplicationStartup (BufferingApplicationStartup).
```
