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

Status: `DONE`

---

### Step 10: Searching
- GET /api/v1/workflows?search=payment
- LIKE / ILIKE search on name and description
- Combine with filtering, sorting, and pagination in one endpoint
- **Concepts:** Database search, Dynamic querying, Query optimization

Status: `DONE`

---

### Step 11: Filter + Interceptor
- RequestTimingFilter: log method, URI, status, duration for every request
- Add X-Request-Id header via filter
- RateLimitInterceptor: simple in-memory rate limiter (preHandle)
- Register interceptor via WebMvcConfigurer
- **Concepts:** Spring MVC Filters vs Interceptors, Filter chain, OncePerRequestFilter

Status: `DONE`

---

### Step 12: Custom AOP Aspect + Built-in Resilience
- @LogExecution annotation + aspect: log method entry/exit with duration
- Apply to all service methods via pointcut
- Demonstrate self-invocation trap (and fix)
- Verify proxy creation with AopUtils.isAopProxy()
- Use Spring Framework 7's built-in @Retryable (org.springframework.resilience.annotation)
- Use @ConcurrencyLimit for throttling
- Add @EnableResilientMethods to activate (note: PLAN originally said @EnableResilience — actual name is @EnableResilientMethods)
- @Retryable on create() for transient DB failures (TransientDataAccessException)
- @ConcurrencyLimit(5) on findAll() to throttle concurrent queries
- simulateTransientFailure() demo method with retry (GET /api/v1/debug/retry)
- **Concepts:** AOP, CGLIB Proxies, Pointcuts, @Around, Self-invocation, Built-in retry, Concurrency limiting

Status: `DONE`

---

### Step 13: Configuration Management
- application.yaml (shared defaults: app name, JPA, virtual threads, Jackson 3, FlowForge properties)
- application-dev.yaml (DEBUG logging, show-sql, format_sql, create-drop DDL)
- application-prod.yaml (env vars for secrets — no defaults, INFO logging, validate DDL, sql.init: never)
- application-postgres.yaml (datasource with env var defaults, schema init)
- application-h2.yaml (in-memory datasource, H2 console)
- @ConfigurationProperties(prefix="flowforge") with record binding (FlowForgeProperties)
  - flowforge.execution.default-max-retries / default-timeout-seconds
  - flowforge.rate-limit.max-requests / window-seconds
- RateLimitInterceptor now reads from FlowForgeProperties (no more hardcoded constants)
- Enable virtual threads: spring.threads.virtual.enabled=true
- Configure Jackson 3 via spring.jackson.serialization.write-dates-as-timestamps=false
- Graceful shutdown: server.shutdown=graceful
- Default profiles: dev,postgres (via spring.profiles.default)
- Property override demo: GET /api/v1/debug/config shows resolved values
  - Override via env var: FLOWFORGE_EXECUTION_DEFAULT_MAX_RETRIES=10
  - Override via CLI: --flowforge.execution.default-max-retries=10
- **Concepts:** Profiles, @ConfigurationProperties, Records, Env vars, Secrets, Precedence, Virtual threads, Jackson 3

Status: `DONE`

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
Built-in @Retryable                12 (AOP + resilience — @Retryable, @ConcurrencyLimit, @EnableResilientMethods)
JSpecify Null Safety               Throughout
Modularized Auto-Config            1 (project setup)
Virtual Threads                    13 (configuration)
Hibernate 7.1 / JPA 3.2           2 (entities)
Bean Validation 3.1                3, 6 (DTOs, validation)
```

---

---

# Phase 2 — Database, JPA, Transactions, Caching & Locking

## Step 18 — Database Fundamentals

**Goal:** Understand the database layer before touching JPA.

Topics:
- Relational modeling, primary keys, foreign keys, constraints
- Normalization (1NF, 2NF, 3NF) and denormalization trade-offs
- Indexes: B-tree, composite indexes, covering indexes
- Query plans: EXPLAIN ANALYZE, scan types, join types
- Joins: INNER, LEFT, RIGHT, FULL OUTER, CROSS, self-join
- Locking: shared vs exclusive, row-level, deadlocks

Practice in FlowForge:
- Add composite index on `(status, name)` for workflow filtering
- Run EXPLAIN ANALYZE on existing queries to verify index usage
- Add CHECK constraint on workflow status column
- Document query plan differences with and without indexes

**Concepts:** Relational modeling, normalization, indexes, query plans, locking
**Documentation:** `SpringBootRoadmap/Database-Fundamentals/README.md`

Status: `PLANNED`

---

## Step 19 — JPA / Hibernate Core

**Goal:** Master entity lifecycle, persistence context, and EntityManager operations.

Topics:
- @Entity, @Id, @GeneratedValue, @Column mapping
- EntityManager: persist, find, merge, remove, flush, clear, detach
- Persistence Context: identity map, dirty checking, write-behind
- Entity states: Transient → Managed → Detached → Removed
- Session vs EntityManager, Spring Data JPA repository mapping

Practice in FlowForge:
- Verify dirty checking: modify a managed entity without calling save()
- Add @DynamicUpdate to Workflow entity (verify smaller UPDATE SQL)
- Add debug endpoint to show entity state (managed/detached) via `em.contains()`
- Batch insert 1000 workflows with flush/clear every 100 (avoid OOM)

**Concepts:** Entity lifecycle, persistence context, dirty checking, flush/clear
**Documentation:** `SpringBootRoadmap/JPA-Hibernate/README.md`

Status: `PLANNED`

---

## Step 20 — Hibernate Performance

**Goal:** Detect and fix N+1 queries, understand lazy loading and caches.

Topics:
- Lazy vs eager loading, Hibernate proxies
- N+1 problem: detection (SQL logging) and fixes
- JOIN FETCH, @EntityGraph, @BatchSize, SUBSELECT
- LazyInitializationException (already hit in FlowForge Step 4)
- open-in-view: true vs false
- First-level cache (persistence context), second-level cache
- DTO projections for read-only queries

Practice in FlowForge:
- Enable hibernate.generate_statistics in dev profile
- Add @EntityGraph on findAll for workflows with steps
- Set global default_batch_fetch_size: 25
- Add DTO projection (WorkflowSummary interface) for list endpoint
- Compare query counts: entity query vs DTO projection

**Concepts:** N+1, lazy loading, fetch strategies, L1/L2 cache, projections
**Documentation:** `SpringBootRoadmap/Hibernate-Performance/README.md`

Status: `PLANNED`

---

## Step 21 — Transactions

**Goal:** Master ACID, isolation levels, @Transactional behavior, and propagation.

Topics:
- ACID properties (Atomicity, Consistency, Isolation, Durability)
- Isolation levels: READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE
- Read phenomena: dirty read, non-repeatable read, phantom read
- @Transactional: proxy chain, readOnly, timeout, rollbackFor
- Propagation: REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, MANDATORY, NEVER
- Self-invocation trap (proxy bypass)
- Transaction problems: long-running TX, lost updates, deadlocks

Practice in FlowForge:
- Add @Transactional(readOnly = true) on all read service methods
- Add @Transactional(rollbackFor = Exception.class) on write methods
- Create a REQUIRES_NEW audit logging method (persists even on rollback)
- Add transaction timeout on report/search methods
- Demonstrate self-invocation trap with a test

**Concepts:** ACID, isolation, propagation, rollback, self-invocation
**Documentation:** `SpringBootRoadmap/Transactions/README.md`

Status: `PLANNED`

---

## Step 22 — Concurrency Control

**Goal:** Implement optimistic and pessimistic locking.

Topics:
- Lost update problem
- Optimistic locking: @Version, OptimisticLockException handling
- Pessimistic locking: @Lock, LockModeType, FOR UPDATE, NOWAIT, SKIP LOCKED
- When to use optimistic vs pessimistic
- API-level optimistic locking (ETag / If-Match headers)

Practice in FlowForge:
- Add @Version to Workflow entity
- Handle OptimisticLockException → return 409 Conflict in GlobalExceptionHandler
- Add ETag header to GET /workflows/{id} response
- Add If-Match header support to PUT /workflows/{id}
- Create a pessimistic lock example for a "publish workflow" operation
- @Retryable on update to auto-retry optimistic lock failures

**Concepts:** @Version, SELECT FOR UPDATE, ETag, conflict resolution
**Documentation:** `SpringBootRoadmap/Concurrency-Control/README.md`

Status: `PLANNED`

---

## Step 23 — Connection Pooling Tuning

**Goal:** Configure and monitor HikariCP properly.

Topics:
- Pool size formula: (cores × 2) + 1
- connectionTimeout, idleTimeout, maxLifetime
- Connection leak detection
- Virtual threads + pool interaction
- @ConcurrencyLimit as pool protector
- Monitoring with Actuator metrics

Practice in FlowForge:
- Tune HikariCP settings in application.yaml
- Enable leak-detection-threshold in dev profile
- Add Actuator endpoint for pool metrics (hikaricp.*)
- Load test with 100 concurrent requests → observe pool behavior
- Compare performance: pool size 5 vs 10 vs 50

**Concepts:** HikariCP tuning, pool sizing, leak detection, monitoring
**Documentation:** `SpringBootRoadmap/Connection-Pooling/README.md`
**Deep dive:** `SpringBootRoadmap/Resilience/HIKARI-CONNECTION-POOL.md`

Status: `PLANNED`

---

## Step 24 — Caching

**Goal:** Implement caching with Spring Cache + Redis.

Topics:
- Cache strategies: cache-aside, read-through, write-through, write-behind
- TTL, eviction policies (LRU, LFU)
- Spring: @Cacheable, @CachePut, @CacheEvict, @EnableCaching
- Redis data structures: strings, hashes, sets, sorted sets
- Cache problems: stampede, penetration, avalanche, hot keys
- Cache invalidation strategies

Practice in FlowForge:
- Add Redis dependency and @EnableCaching
- Add @Cacheable on findById (cache individual workflows)
- Add @CacheEvict on update and delete
- Add @CachePut on create
- Configure TTL in application.yaml
- Implement cache-aside with Redis for workflow search results
- Add jitter to TTL to prevent cache avalanche

**Concepts:** Cache strategies, TTL, @Cacheable, Redis, stampede/avalanche
**Documentation:** `SpringBootRoadmap/Caching/README.md`

Status: `PLANNED`

---

## Step 25 — Distributed Locking

**Goal:** Implement distributed locks with Redisson.

Topics:
- Why JVM locks fail in multi-instance deployments
- Redis SET NX EX for basic locking
- Lease / TTL / watchdog auto-renewal
- Fencing tokens for correctness
- Redisson: RLock, fair locks, read/write locks, semaphores
- Deadlock risks and prevention
- Alternatives: DB constraints, idempotency keys, @Version

Practice in FlowForge:
- Add Redisson dependency
- Implement distributed lock for "publish workflow" (prevent double-publish)
- Use tryLock with timeout (not blocking lock)
- Compare: distributed lock vs database unique constraint for idempotency
- Add debug endpoint to show lock status

**Concepts:** Distributed locks, Redisson, fencing tokens, idempotency
**Documentation:** `SpringBootRoadmap/Distributed-Locking/README.md`

Status: `PLANNED`

---

## How to Work Through This

```text
1. Complete one step fully before moving to the next
2. Test every endpoint with Postman / curl after each step
3. Commit after each step (one commit per step)
4. If something breaks, fix it before moving on
5. Phase 1 (Steps 1-13): REST API + Spring Core
6. Phase 2 (Steps 18-25): Database, JPA, Transactions, Caching, Locking
```
