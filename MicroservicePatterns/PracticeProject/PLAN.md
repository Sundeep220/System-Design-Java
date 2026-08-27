# FlowForge -- Phase 1: Implementation Plan

Covers all concepts from the docs we've read (Phases 5-17 of the roadmap).
Each step builds on the previous one. No skipping.

**Stack:** Java 25 (LTS) + Spring Boot 4.0.x (Spring Framework 7, Hibernate 7, Jakarta EE 11)

---

## Project Setup

```text
Source:       start.spring.io
Group:        com.flowforge
Artifact:     flowforge
Java:         25
Spring Boot:  4.0.x (latest)
Packaging:    Jar

Dependencies:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Validation (Bean Validation 3.1)
  - Lombok
  - Spring Boot DevTools
```

---

## Java 25 + Spring Boot 4 Features We'll Use

These are the new features available to us. We'll incorporate them as we build.

### Java 25 Features (LTS -- released Sep 2025)

```text
Feature                                     Where We'll Use It
---------------------------------------------------------------------------------
Module Import Declarations (JEP 511)        import module java.base; in source files
                                            (reduces import clutter)

Flexible Constructor Bodies (JEP 513)       Validate constructor args BEFORE calling
                                            super() in entity classes

Scoped Values (JEP 506)                     Pass request context (tenantId, userId)
                                            across threads without ThreadLocal
                                            (cleaner than MDC for some cases)

Compact Object Headers (JEP 519)            Free performance gain -- JVM uses 64-bit
                                            headers instead of 96-128 bits (automatic)

Structured Concurrency (Preview, JEP 505)   Later phases -- run parallel tasks as a
                                            single unit (e.g., fan-out workflow steps)

Stable Values (Preview, JEP 502)            Lazy-initialized constants that JVM treats
                                            as final (config values, mapper instances)
```

### Spring Boot 4 / Spring Framework 7 Features

```text
Feature                                     Where We'll Use It
---------------------------------------------------------------------------------
Jackson 3 (default in Boot 4)               JSON serialization uses tools.jackson
                                            package. JsonMapper replaces ObjectMapper.
                                            ObjectMapper still works but is immutable.
                                            Use JsonMapperBuilderCustomizer for config.

API Versioning (built-in)                   spring.mvc.apiversion.* properties
                                            Declarative versioning on @GetMapping etc.
                                            No more manual URI/header versioning code.
                                            -> Step 4 (CRUD Controller)

HTTP Service Clients                        @HttpExchange interfaces auto-configured.
                                            Spring generates implementation.
                                            -> Later phases (inter-service calls)

Built-in @Retryable (Spring Framework 7)    No more spring-retry dependency!
                                            @Retryable + @EnableResilience in core.
                                            @ConcurrencyLimit for throttling.
                                            -> Step 12 (AOP) and later phases

JSpecify Null Safety                        @Nullable / @NonNull across entire Spring
                                            portfolio. IDE warnings for null misuse.
                                            -> Used throughout

Modularized Auto-Configuration              Boot 4 splits autoconfigure into smaller
                                            JARs. Faster startup, smaller uber JARs.
                                            -> Understand in Step 1

OpenTelemetry Starter                       spring-boot-starter-opentelemetry
                                            Metrics + Traces out of the box.
                                            -> Later phases (Observability)

RestTestClient                              New testing API for REST endpoints.
                                            -> When we add tests

Hibernate 7.1                               JPA 3.2, improved batch processing,
                                            better query validation.
                                            -> Step 2 (Entities)

Jakarta EE 11                               Servlet 6.1, Bean Validation 3.1,
                                            JPA 3.2. Package: jakarta.* (not javax.*)
                                            -> Used throughout

Virtual Threads (JDK HttpClient)            spring.threads.virtual.enabled=true
                                            Auto-configures virtual threads for HTTP.
                                            -> Step 13 (Configuration)
```

### Key Differences from Spring Boot 3.x Code

```text
What Changed                          Boot 3.x                    Boot 4.x
----------------------------------------------------------------------------------
Jackson package                       com.fasterxml.jackson       tools.jackson
Mapper class                          ObjectMapper (mutable)      JsonMapper (preferred)
Mapper customizer                     Jackson2ObjectMapper...     JsonMapperBuilderCustomizer
Retry                                 spring-retry dependency     Built-in @Retryable
API versioning                        Manual (URI/header)         spring.mvc.apiversion.*
Null safety                           @Nullable (Spring)          @Nullable (JSpecify)
Auto-config module                    One big jar                 Many small jars
Bean Validation                       jakarta.validation 3.0      jakarta.validation 3.1
Hibernate                             6.x                         7.1
Security                              Spring Security 6           Spring Security 7
```

---

## Steps

### Step 1: Create Project from start.spring.io
- Generate with Java 25 + Spring Boot 4.0.x and dependencies above
- Extract into PracticeProject/
- Verify it compiles and runs
- Note: Boot 4 uses modularized auto-config JARs (smaller, faster startup)
- Note: Jackson 3 is the default (tools.jackson package, JsonMapper)
- **Concepts:** Spring Boot Internals, Starters, Auto-Configuration, Boot 4 modularity

Status: `DONE`

---

### Step 2: Entities + Repository
- Create Workflow entity (id, name, description, status, maxRetries, timeoutSeconds, createdAt, updatedAt)
- Create WorkflowStep entity (id, workflow, name, type, stepOrder, config)
- Create WorkflowStatus enum (DRAFT, ACTIVE, PAUSED, ARCHIVED)
- Use Flexible Constructor Bodies (Java 25) -- validate args before super()
- Create JPA repositories
- Configure PostgreSQL in application.yml
- Note: Hibernate 7.1 + JPA 3.2 (jakarta.persistence.*)
- **Concepts:** Spring Core (beans, DI, component scanning), JPA basics, Java 25 constructors

Status: `DONE`

---

### Step 3: DTOs + Mapper
- WorkflowCreateRequest (record, with validation annotations)
- WorkflowUpdateRequest (record, full replace)
- WorkflowPatchRequest (partial update)
- WorkflowSummaryResponse (for lists)
- WorkflowDetailResponse (for single resource)
- WorkflowMapper (manual mapping)
- PageResponse wrapper
- Use Jackson 3 annotations (tools.jackson.annotation.*)
- Use Bean Validation 3.1 (jakarta.validation.*)
- **Concepts:** DTOs & API Contracts, Records, Jackson 3, Separation of entity vs API

Status: `DONE`

---

### Step 4: CRUD Controller
- POST   /api/v1/workflows        -> create
- GET    /api/v1/workflows         -> list (paginated)
- GET    /api/v1/workflows/{id}    -> get detail
- PUT    /api/v1/workflows/{id}    -> full update
- DELETE /api/v1/workflows/{id}    -> delete
- Proper status codes (201, 200, 204, 404)
- ResponseEntity with Location header on create
- Use Spring Boot 4 API Versioning (spring.mvc.apiversion.*) for declarative version support
- **Concepts:** REST API Design, Spring MVC, HTTP methods, Status codes, URI design, Boot 4 API versioning

Status: `DONE`

---

### Step 5: Global Exception Handler
- FlowForgeException base class (errorCode, httpStatus)
- ResourceNotFoundException (404)
- ConflictException (409)
- InvalidStateTransitionException (422)
- ApiError response record (timestamp, status, error, message, path, traceId, details)
- @RestControllerAdvice with handlers for:
  - MethodArgumentNotValidException (400)
  - HttpMessageNotReadableException (400)
  - ResourceNotFoundException (404)
  - ConflictException (409)
  - DataIntegrityViolationException (409)
  - Generic Exception (500 -- no stack trace to client)
- **Concepts:** Exception Handling, @ControllerAdvice, Consistent error responses

Status: `DONE`

---

### Step 6: Validation
- @NotBlank, @Size, @Min, @Max, @Email on create/update DTOs
- @Valid on @RequestBody in controller
- @Validated on service class for method-level validation
- @Validated(OnCreate.class) vs @Validated(OnUpdate.class) with groups
- Custom validator: @ValidWorkflowName (no special characters)
- **Concepts:** Bean Validation, @Valid vs @Validated, Custom validators, Validation groups

Status: `DONE`

---

### Step 7: PATCH Endpoint (Partial Update)
- PATCH /api/v1/workflows/{id}
- WorkflowPatchRequest with Optional<> fields
- Only update fields that are present in the request
- Return 200 with updated resource
- **Concepts:** REST PATCH, JSON Merge Patch, Partial updates

Status: `DONE`

---

### Step 8: Pagination
- Offset pagination: GET /api/v1/workflows?page=0&size=20
- Use Spring Data Pageable, Page, PageRequest
- Custom PageResponse wrapper
- Cursor pagination: GET /api/v1/workflows?cursor=xxx&limit=20
- Compare Page vs Slice
- **Concepts:** Pagination strategies, Offset vs Cursor, Performance

Status: `DONE`

---

### Step 9: Filtering + Sorting
- Static filters: GET /api/v1/workflows?status=ACTIVE
- Dynamic filters using JPA Specifications
- Multi-column sorting: ?sort=createdAt,desc&sort=name,asc
- Whitelist allowed sort fields (reject arbitrary columns)
- Combine with pagination
- **Concepts:** JPA Specifications, Criteria API, Dynamic queries, Database indexes

Status: `PENDING`

---

### Step 10: Searching
- GET /api/v1/workflows?search=payment
- LIKE / ILIKE search on name and description
- Combine with filtering, sorting, and pagination in one endpoint
- **Concepts:** Database search, Dynamic querying, Query optimization

Status: `PENDING`

---

### Step 11: Filter + Interceptor
- RequestTimingFilter: log method, URI, status, duration for every request
- Add X-Request-Id header via filter
- RateLimitInterceptor: simple in-memory rate limiter (preHandle)
- Register interceptor via WebMvcConfigurer
- **Concepts:** Spring MVC Filters vs Interceptors, Filter chain, OncePerRequestFilter

Status: `PENDING`

---

### Step 12: Custom AOP Aspect + Built-in Resilience
- @LogExecution annotation + aspect: log method entry/exit with duration
- Apply to all service methods via pointcut
- Demonstrate self-invocation trap (and fix)
- Verify proxy creation with AopUtils.isAopProxy()
- Use Spring Framework 7's built-in @Retryable (no spring-retry dependency needed)
- Use @ConcurrencyLimit for throttling
- Add @EnableResilience to activate
- **Concepts:** AOP, CGLIB Proxies, Pointcuts, @Around, Self-invocation, Built-in retry

Status: `PENDING`

---

### Step 13: Configuration Management
- application.yml (shared defaults)
- application-dev.yml (H2 or local Postgres, DEBUG logging, show-sql)
- application-prod.yml (env vars for secrets, INFO logging, validate DDL)
- @ConfigurationProperties for FlowForge-specific config (execution.maxRetries, etc.)
- Enable virtual threads: spring.threads.virtual.enabled=true
- Configure Jackson 3 via spring.jackson.* properties or JsonMapperBuilderCustomizer
- Demonstrate property override: yml < env var < CLI arg
- **Concepts:** Profiles, @ConfigurationProperties, @Value, Secrets, Precedence, Virtual threads

Status: `PENDING`

---

## Concept Coverage Matrix

```text
Doc Topic                          Practiced In Step
-------------------------------------------------------
REST API Design                    4, 7, 8, 9, 10
DTOs & API Contracts               3
Spring Core (IoC, DI, Lifecycle)   2, 3, 4 (used throughout)
Spring Boot Internals              1
Configuration Management           13
Spring MVC                         4, 11
Exception Handling & Validation    5, 6
AOP & Proxies                      12

Java 25 Feature                    Where Used
-------------------------------------------------------
Flexible Constructor Bodies        2 (entity constructors)
Module Import Declarations         Throughout (cleaner imports)
Scoped Values                      11 (request context)
Compact Object Headers             Automatic (JVM-level)

Spring Boot 4 Feature              Where Used
-------------------------------------------------------
Jackson 3 (tools.jackson)          3, 13 (DTOs, config)
API Versioning (built-in)          4 (controller)
Built-in @Retryable                12 (AOP + resilience)
JSpecify Null Safety               Throughout
Modularized Auto-Config            1 (project setup)
Virtual Threads                    13 (configuration)
Hibernate 7.1 / JPA 3.2           2 (entities)
Bean Validation 3.1                3, 6 (DTOs, validation)
```

---

## How to Work Through This

```text
1. Complete one step fully before moving to the next
2. Test every endpoint with Postman / curl after each step
3. Commit after each step (one commit per step)
4. If something breaks, fix it before moving on
5. After Phase 1, we move to Phase 2 (Database, JPA, Transactions, Caching, etc.)
```
