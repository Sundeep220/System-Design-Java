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
- Normalization (1NF → 2NF → 3NF → BCNF → 4NF → 5NF) and denormalization trade-offs
- Indexes: B-tree internals, composite indexes, covering indexes, partial indexes
- Query plans: EXPLAIN ANALYZE, scan types (Seq Scan, Index Scan, Index Only Scan)
- Joins: INNER, LEFT, RIGHT, FULL OUTER, CROSS, self-join
- Locking: shared vs exclusive, row-level, table-level, deadlocks

Practice in FlowForge:

```text
Constraint tests: src/test/java/.../phase2/Step18_DatabaseFundamentalsTest.java
Run: mvn test -Dtest=Step18_DatabaseFundamentalsTest  (requires Docker)

SCENARIO 18.1: Schema Review & Constraints
  ✅ Review all existing tables — both have UUID PK via BaseEntity
  ✅ NOT NULL constraints — already on workflow.name, step.name via @Column(nullable=false)
  ✅ CHECK constraint: workflow.status IN ('DRAFT','ACTIVE','PAUSED','ARCHIVED') — schema-postgres.sql
  ✅ CHECK constraint: max_retries >= 0, timeout_seconds > 0, step_order >= 0 — schema-postgres.sql
  ✅ UNIQUE constraint on workflow.name — schema-postgres.sql + @Column(unique=true)
  ✅ FK: workflow_steps.workflow_id → workflows.id with CascadeType.ALL + orphanRemoval
  ✅ GlobalExceptionHandler: specific error messages for each constraint violation
  ✅ Integration tests: duplicate name, invalid status, negative retries, zero timeout,
     negative step_order, invalid FK, valid persist, cascade delete

SCENARIO 18.2: Normalization Audit
  ✅ Analyzed: schema IS in 3NF — documented in schema-postgres.sql comment block
  ✅ No repeating groups (1NF), no partial dependencies (2NF — single-column PKs),
     no transitive dependencies (3NF)
  ✅ Denormalization not needed yet — JOIN + COUNT is fast with proper indexes

SCENARIO 18.3: Index Design
  ✅ Composite index: idx_workflow_status_name ON workflows(status, name) — schema-postgres.sql
  ✅ FK index: idx_step_workflow_id ON workflow_steps(workflow_id) — schema-postgres.sql
  ✅ Covering index: idx_workflow_status_cover ON workflows(status) INCLUDE(name, id)
  ✅ Cursor pagination index: idx_workflow_created_at ON workflows(created_at DESC, id DESC)
  ✅ FTS GIN index: idx_workflows_fts (already existed)
  □ Run EXPLAIN ANALYZE queries from practice-queries.sql to verify index usage
    (run manually in psql/DataGrip after starting the app with test data)

SCENARIO 18.4: Join Practice
  ✅ INNER JOIN, LEFT JOIN, self-join, aggregate JOIN — all in practice-queries.sql
  ✅ EXPLAIN ANALYZE on joins — added to practice-queries.sql
  ✅ Test data generator: 100 workflows + 0-5 steps each — in practice-queries.sql
  □ Run practice-queries.sql manually in psql/DataGrip to observe results

SCENARIO 18.5: Locking Observation
  ✅ FOR UPDATE, NOWAIT, SKIP LOCKED, deadlock, pg_locks — all in practice-queries.sql
  □ Run locking experiments manually in two psql sessions to observe blocking
```

**Concepts:** Relational modeling, normalization, indexes, query plans, locking
**Documentation:** `SpringBootRoadmap/Database-Fundamentals/README.md`

Status: `DONE` ✅

---

## Step 19 — JPA / Hibernate Core

**Goal:** Master entity lifecycle, persistence context, and EntityManager operations.

Topics:
- @Entity, @Id, @GeneratedValue (UUID/SEQUENCE/IDENTITY), @Column mapping
- EntityManager deep dive: persist, find, merge, remove, flush, clear, detach, refresh
- EntityManager proxy (SharedEntityManagerCreator), @PersistenceContext vs @Autowired
- find() vs getReference() (eager vs lazy proxy)
- Persistence Context: identity map, dirty checking, write-behind, snapshot comparison
- Entity states: Transient → Managed → Detached → Removed
- Query methods: JPQL, native SQL, Criteria API, named queries
- Session vs EntityManager, Spring Data JPA repository mapping
- Why save() is NOT needed with @Transactional but IS needed without it

Practice in FlowForge:

```text
All scenarios implemented as integration tests (Testcontainers + PostgreSQL).
Test class: src/test/java/.../phase2/Step19_JpaHibernateCoreTest.java
Run: mvn test -Dtest=Step19_JpaHibernateCoreTest  (requires Docker)

SCENARIO 19.1: Entity Mapping Verification
  ✅ workflowHasUuidPk() — UUID PK assigned on persist
  ✅ stepHasUuidPkAndFk() — WorkflowStep has UUID PK + FK to Workflow
  ✅ timestampsAreSet() — @PrePersist sets createdAt/updatedAt
  ✅ SQL logging: show-sql, format_sql, hibernate.orm.jdbc.bind TRACE

SCENARIO 19.2: Persistence Context & Dirty Checking
  ✅ dirtyCheckingTriggersUpdate() — modify MANAGED entity without save(), verify UPDATE
  ✅ noOpChangeNoUpdate() — setting same value → Hibernate skips UPDATE
  □ Experiment: add @DynamicUpdate to Workflow, compare UPDATE SQL
    (only name column vs all columns — try it yourself, then remove)

SCENARIO 19.3: Entity States
  ✅ entityStateTransitions() — find()→MANAGED, detach()→DETACHED, merge()→new copy
  ✅ transientState() — new entity is TRANSIENT until persist()

SCENARIO 19.4: persist vs merge vs save()
  ✅ persistOnlyInserts() — one INSERT, no SELECT
  ✅ mergeDetachedSelectsAndUpdates() — SELECT + UPDATE in logs
  ✅ saveOnManagedIsUnnecessary() — save() on managed = redundant merge()

SCENARIO 19.5: find() vs getReference()
  ✅ getReferenceReturnsProxy() — proxy class, no SELECT until property access
  ✅ getReferenceForFkAvoidSelect() — INSERT step without SELECT for workflow
  ✅ findForFkExtraSelect() — SELECT for workflow + INSERT for step

SCENARIO 19.6: Batch Insert with flush/clear
  ✅ batchInsertWithFlushClear() — flush/clear every 50, keeps context small
  ✅ batchInsertWithoutFlushClear() — all entities stay in context

SCENARIO 19.7: EntityManager Query Methods
  ✅ allQueryMethodsReturnManagedEntities() — JPQL + native + Criteria side-by-side
  ✅ getSingleResultNoResults() — 0 results → NoResultException
  ✅ getSingleResultMultipleResults() — 2+ results → NonUniqueResultException

SCENARIO 19.8: Bulk Update (bypassing entity lifecycle)
  ✅ bulkUpdateCausesStaleness() — STALE entity after bulk UPDATE, fixed by refresh()
  ✅ bulkUpdateReturnsCount() — one SQL, no entity loading
```

**Concepts:** Entity lifecycle, persistence context, dirty checking, flush/clear, EntityManager operations
**Documentation:** `SpringBootRoadmap/JPA-Hibernate/README.md`

Status: `DONE` ✅

---

## Step 20 — Hibernate Performance

**Goal:** Detect and fix N+1 queries, understand lazy loading and caches.

Topics:
- Lazy vs eager loading, Hibernate proxies (PersistentBag, HibernateProxy)
- N+1 problem: detection (SQL logging, hibernate.generate_statistics) and 4 fixes
- JOIN FETCH, @EntityGraph, @BatchSize, SUBSELECT
- LazyInitializationException (already hit in FlowForge Step 4)
- open-in-view: true vs false
- First-level cache (persistence context), second-level cache
- DTO projections for read-only queries

Practice in FlowForge:

```text
All scenarios implemented as integration tests (Testcontainers + PostgreSQL).
Test class: src/test/java/.../phase2/Step20_HibernatePerformanceTest.java
Run: mvn test -Dtest=Step20_HibernatePerformanceTest  (requires Docker)

SCENARIO 20.1: N+1 Detection
  ✅ findAllTriggersNPlus1() — seeds 10 workflows with 3 steps, uses Statistics
     to count queries. Confirms N+1: 1 + N queries instead of 1.

SCENARIO 20.2: Fix with JOIN FETCH
  ✅ findAllWithSteps() added to WorkflowRepository — LEFT JOIN FETCH
  ✅ joinFetchSingleQuery() — verifies exactly 1 query
  ✅ joinFetchIncludesWorkflowsWithoutSteps() — LEFT JOIN returns empty ones too
  ⚠️ Note: JOIN FETCH + Pageable → Hibernate paginates IN MEMORY (common bug)

SCENARIO 20.3: Fix with @EntityGraph
  ✅ findAllWithStepsEntityGraph() added to WorkflowRepository — @EntityGraph
  ✅ entityGraphSingleQuery() — verifies 1 query (LEFT JOIN generated)
  ✅ entityGraphIncludesEmpty() — includes workflows with 0 steps

SCENARIO 20.4: Fix with @BatchSize
  ✅ batchSizeReducesQueries() — documents the concept and expected improvement
     To enable: add @BatchSize(size = 25) on Workflow.steps field
     Or global: spring.jpa.properties.hibernate.default_batch_fetch_size=25
     Expected: 1 + ceil(N/batchSize) queries instead of 1 + N

SCENARIO 20.5: Fix with SUBSELECT
  ✅ subselectConcept() — documents all 4 N+1 fix approaches side-by-side
     To enable: add @Fetch(FetchMode.SUBSELECT) on Workflow.steps
     Expected: exactly 2 queries regardless of N

SCENARIO 20.6: LazyInitializationException
  ✅ accessingLazyCollectionOutsideTransaction() — proves LazyInitException
     with open-in-view=false (already set in application.yaml)
  ✅ fixWithJoinFetch() — findByIdWithSteps works outside TX
  ✅ fixWithTransactional() — lazy load works inside @Transactional

SCENARIO 20.7: DTO Projections
  ✅ WorkflowProjection interface — getId(), getName(), getStatus()
  ✅ WorkflowIdNameStatus record — class-based DTO with constructor expression
  ✅ findAllProjectedBy() — interface projection (auto-proxy)
  ✅ findAllDtoProjection() — @Query constructor expression
  ✅ interfaceProjection() — verifies only 3 columns selected
  ✅ classDtoProjection() — verifies records returned, no entities
  ✅ projectionsNotManaged() — DTOs have zero persistence context overhead
  ✅ entityVsProjection() — side-by-side: entity (all cols, managed) vs projection
```

**Concepts:** N+1, lazy loading, fetch strategies, L1/L2 cache, projections
**Documentation:** `SpringBootRoadmap/Hibernate-Performance/README.md`

Status: `DONE` ✅

---

## Step 21 — Transactions

**Goal:** Master ACID, isolation levels, @Transactional behavior, and propagation.

Topics:
- ACID properties (Atomicity, Consistency, Isolation, Durability)
- Isolation levels: READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE
- Read phenomena: dirty read, non-repeatable read, phantom read
- @Transactional: every parameter (propagation, isolation, readOnly, timeout, rollbackFor, noRollbackFor, transactionManager, label)
- Propagation: REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER
- Self-invocation trap (proxy bypass)
- @Transactional + @Async interaction
- @Transactional + @Retryable ordering
- Programmatic transactions (TransactionTemplate)
- Transaction events (@TransactionalEventListener)

Practice in FlowForge:

```text
SCENARIO 21.1: readOnly Optimization
  □ Add @Transactional(readOnly = true) on ALL read service methods
    - findById, findAll, search, count
  □ Enable SQL logging → verify no UPDATE or flush on read methods
  □ Test: try wf.setName("Hack") inside a readOnly method
    → Verify: change is SILENTLY LOST (no UPDATE generated)
  □ Test: try a @Query UPDATE inside readOnly
    → Verify: PostgreSQL throws ERROR (cannot execute UPDATE in read-only TX)
  □ Measure: compare query time with/without readOnly on 1000 reads

SCENARIO 21.2: rollbackFor Configuration
  □ Add @Transactional(rollbackFor = Exception.class) on all write methods
  □ Test default behavior: throw IOException (checked) inside @Transactional
    → Without rollbackFor: TX COMMITS (change persists!) — verify this bug
    → With rollbackFor = Exception.class: TX rolls back ✅
  □ Test: throw RuntimeException → verify rollback (default behavior)
  □ Test noRollbackFor:
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NotificationException.class)
    → Throw NotificationException → TX commits (exception excluded from rollback)

SCENARIO 21.3: Propagation — REQUIRED vs REQUIRES_NEW
  □ Create AuditService:
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(String action, UUID entityId) {
        auditRepo.save(new AuditEntry(action, entityId, Instant.now()));
    }
  □ In WorkflowService.create():
    - Save workflow
    - Call auditService.logAudit("CREATE", wf.getId())
    - Throw RuntimeException AFTER audit
    - Verify: workflow is ROLLED BACK, but audit entry PERSISTS
      (REQUIRES_NEW committed independently)
  □ Change audit to REQUIRED → both roll back together
  □ Document the difference with SQL logs

SCENARIO 21.4: Self-Invocation Trap
  □ Create a method in WorkflowService:
    public void outerMethod(UUID id) {
        this.innerMethod(id);  // self-invocation!
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void innerMethod(UUID id) { ... }
  □ Call outerMethod → observe: innerMethod does NOT get its own TX
    (proxy bypassed — REQUIRES_NEW ignored)
  □ Fix: inject WorkflowService into itself with @Lazy:
    @Lazy @Autowired private WorkflowService self;
    self.innerMethod(id);  // goes through proxy ✅
  □ Alternative fix: extract innerMethod to a separate service bean

SCENARIO 21.5: Transaction Timeout
  □ Add timeout on a slow endpoint:
    @Transactional(timeout = 3)
    public List<Workflow> heavySearch(String query) {
        Thread.sleep(5000);  // simulate slow operation
        return repo.findAll();
    }
  □ Call the endpoint → expect TransactionTimedOutException
  □ Handle in GlobalExceptionHandler → return 504 Gateway Timeout

SCENARIO 21.6: @TransactionalEventListener
  □ Create WorkflowCreatedEvent record
  □ Publish in create method: eventPublisher.publishEvent(new WorkflowCreatedEvent(wf.getId()))
  □ Create listener:
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowCreated(WorkflowCreatedEvent event) {
        log.info("Workflow committed: {}", event.id());
    }
  □ Test: create workflow → verify listener fires AFTER commit
  □ Test: create workflow but throw exception → verify listener does NOT fire
  □ Add AFTER_ROLLBACK listener → verify it fires on failure

SCENARIO 21.7: Programmatic Transactions
  □ Inject TransactionTemplate
  □ Create an endpoint that uses TransactionTemplate instead of @Transactional:
    txTemplate.execute(status -> {
        repo.save(workflow);
        if (condition) { status.setRollbackOnly(); }
        return workflow;
    });
  □ Test: rollback works without proxy (no self-invocation trap)
  □ Compare: @Transactional vs TransactionTemplate — when to use each

SCENARIO 21.8: @Transactional + @Async
  □ Create an async method:
    @Async @Transactional
    public void processAsync(UUID id) { ... }
  □ Call from a @Transactional method that throws after the async call
  □ Verify: async method's changes PERSIST even though caller rolled back
    (different thread = different TX)
  □ Document this behavior — it's a common production surprise
```

**Concepts:** ACID, isolation, propagation, rollback, self-invocation, events, programmatic TX
**Documentation:** `SpringBootRoadmap/Transactions/README.md`

Status: `PLANNED`

---

## Step 22 — Concurrency Control

**Goal:** Implement optimistic and pessimistic locking.

Topics:
- Lost update problem (two concurrent reads → both write → second overwrites first)
- Optimistic locking: @Version, OptimisticLockException, retry logic
- Pessimistic locking: @Lock, LockModeType, FOR UPDATE, NOWAIT, SKIP LOCKED
- When to use optimistic vs pessimistic (decision flowchart)
- API-level optimistic locking (ETag / If-Match headers)

Practice in FlowForge:

```text
SCENARIO 22.1: Demonstrate the Lost Update Problem
  □ WITHOUT any locking:
    - Thread 1: read workflow (version: name="Deploy")
    - Thread 2: read same workflow (version: name="Deploy")
    - Thread 1: update name to "Pipeline A" → save
    - Thread 2: update name to "Pipeline B" → save
    - Result: "Pipeline B" wins, "Pipeline A" is LOST
  □ Create a test/debug endpoint that simulates this with two threads
  □ Document: show that Thread 1's change was silently overwritten

SCENARIO 22.2: Optimistic Locking with @Version
  □ Add to Workflow entity:
    @Version
    private Long version;
  □ Hibernate auto-manages this:
    - INSERT → version = 0
    - UPDATE → SET ... WHERE id = ? AND version = 0; version = 1
    - If version mismatch → OptimisticLockException
  □ Run the same lost-update test from 22.1
    → Now Thread 2 gets OptimisticLockException ✅
  □ Handle in GlobalExceptionHandler:
    @ExceptionHandler(OptimisticLockException.class)
    → return 409 Conflict with body: "Entity was modified by another user"

SCENARIO 22.3: Auto-Retry on Optimistic Lock Failure
  □ Add @Retryable on update method:
    @Retryable(retryFor = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public Workflow update(UUID id, UpdateRequest req) { ... }
  □ Test: concurrent updates → first attempt fails, retry re-reads and succeeds
  □ Add @Recover method for when all retries exhausted → return 409

SCENARIO 22.4: ETag / If-Match (API-Level Optimistic Locking)
  □ GET /workflows/{id} response:
    - Add header: ETag: "{version}"
    - Client stores this ETag
  □ PUT /workflows/{id} request:
    - Client sends: If-Match: "{version}"
    - Server checks: if request version != current version → 412 Precondition Failed
    - If match → proceed with update
  □ Test with curl:
    curl -H "If-Match: 0" -X PUT ... → 200 OK (version was 0)
    curl -H "If-Match: 0" -X PUT ... → 412 (version is now 1)
  □ Test without If-Match header → 428 Precondition Required (optional)

SCENARIO 22.5: Pessimistic Locking — SELECT FOR UPDATE
  □ Add to repository:
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Workflow w WHERE w.id = :id")
    Optional<Workflow> findByIdForUpdate(@Param("id") UUID id);
  □ Create "publish workflow" endpoint:
    - findByIdForUpdate → blocks other transactions from reading/writing
    - Validate state (must be DRAFT)
    - Set status = ACTIVE
    - Save
  □ Test: two concurrent publish requests for same workflow
    → One blocks, other waits → only one succeeds

SCENARIO 22.6: NOWAIT and SKIP LOCKED
  □ NOWAIT: try to lock → if already locked → immediately fail (no wait)
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    → Throws PessimisticLockException immediately
  □ SKIP LOCKED: skip rows that are locked → process only unlocked rows
    @Query(value = "SELECT * FROM workflows WHERE status = 'PENDING' FOR UPDATE SKIP LOCKED LIMIT 10", nativeQuery = true)
    → Use for: job queue pattern (multiple workers pulling from same table)
  □ Create a job processor endpoint:
    - SELECT ... FOR UPDATE SKIP LOCKED LIMIT 5
    - Process 5 unlocked workflows
    - Concurrent calls process DIFFERENT workflows (no contention)

SCENARIO 22.7: Decision — When to Use What
  □ Create a cheat sheet in the codebase (or README):
    - Low contention (most updates succeed) → Optimistic (@Version)
    - High contention (frequent conflicts) → Pessimistic (FOR UPDATE)
    - API clients (browser/mobile) → ETag/If-Match
    - Job queue / worker pattern → SKIP LOCKED
    - Must-not-fail critical operation → FOR UPDATE NOWAIT (fail fast)
```

**Concepts:** @Version, SELECT FOR UPDATE, ETag, NOWAIT, SKIP LOCKED, conflict resolution
**Documentation:** `SpringBootRoadmap/Concurrency-Control/README.md`

Status: `PLANNED`

---

## Step 23 — Connection Pooling Tuning

**Goal:** Configure and monitor HikariCP properly.

Topics:
- Why pools exist (avoid TCP handshake + TLS + auth per query → 11x speedup)
- Pool size formula: connections = (cores × 2) + effective_spindle_count
- Why bigger ≠ better (context switching, lock contention)
- connectionTimeout, idleTimeout, maxLifetime, minimumIdle
- Connection leak detection
- Virtual threads + pool interaction
- @ConcurrencyLimit as pool protector
- Monitoring with Actuator metrics

Practice in FlowForge:

```text
SCENARIO 23.1: Baseline Configuration
  □ Add to application.yaml:
    spring.datasource.hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000      # 30s wait for connection
      idle-timeout: 600000           # 10min idle before eviction
      max-lifetime: 1800000          # 30min max connection age
      pool-name: FlowForge-Pool
  □ Start app → verify pool starts with 5 connections (minimumIdle)

SCENARIO 23.2: Leak Detection
  □ Enable: spring.datasource.hikari.leak-detection-threshold: 5000  # 5 seconds
  □ Create a buggy endpoint that holds a connection too long:
    @Transactional
    public void leakyMethod() {
        repo.findAll();
        Thread.sleep(10000);  // holds connection for 10s
    }
  □ Call the endpoint → observe WARNING in logs:
    "Connection leak detection triggered for ..."
  □ Fix: reduce transaction scope or remove unnecessary sleep

SCENARIO 23.3: Pool Exhaustion
  □ Set maximum-pool-size: 3 (intentionally small)
  □ Send 10 concurrent requests (each with a 2-second sleep in @Transactional)
  □ Observe: first 3 succeed, next 7 WAIT → some timeout
    → SQLTransientConnectionException: "Connection is not available, request timed out"
  □ Add @ConcurrencyLimit(3) on the method → excess requests fail FAST
    instead of waiting for pool timeout
  □ Reset pool size to 10

SCENARIO 23.4: Monitoring with Actuator
  □ Enable: management.endpoints.web.exposure.include: health,metrics
  □ Call GET /actuator/metrics/hikaricp.connections.active
  □ Call GET /actuator/metrics/hikaricp.connections.idle
  □ Call GET /actuator/metrics/hikaricp.connections.pending
  □ Call GET /actuator/metrics/hikaricp.connections.timeout
  □ Under load: observe active connections increase, idle decrease
  □ Document: what each metric means and when to alert

SCENARIO 23.5: Pool Size Experiment
  □ Create a load test endpoint: GET /debug/pool-test
    → runs 100 queries sequentially
  □ Test with pool size 2 → measure total time
  □ Test with pool size 5 → measure total time
  □ Test with pool size 20 → measure total time
  □ Document: diminishing returns after (cores × 2) + 1
```

**Concepts:** HikariCP tuning, pool sizing, leak detection, monitoring
**Documentation:** `SpringBootRoadmap/Connection-Pooling/README.md`
**Deep dive:** `SpringBootRoadmap/Resilience/HIKARI-CONNECTION-POOL.md`

Status: `PLANNED`

---

## Step 24 — Caching

**Goal:** Implement caching with Spring Cache + Redis.

Topics:
- Cache strategies: cache-aside, read-through, write-through, write-behind, write-around
- TTL, eviction policies (LRU, LFU, noeviction)
- Spring: @Cacheable, @CachePut, @CacheEvict, @Caching, @EnableCaching
- condition vs unless, sync = true for stampede prevention
- Local (Caffeine) vs distributed (Redis) vs multi-tier (L1 + L2)
- Redis data structures: strings, hashes, lists, sets, sorted sets
- Cache problems: stampede, penetration, avalanche, hot keys
- Cache anti-patterns
- Cache invalidation: delete vs update, DB-first vs cache-first

Practice in FlowForge:

```text
SCENARIO 24.1: Basic Cache-Aside with Caffeine (Local Cache)
  □ Add dependency: spring-boot-starter-cache + com.github.ben-manes.caffeine:caffeine
  □ Add @EnableCaching on main app class
  □ Configure Caffeine in application.yaml:
    spring.cache.type: caffeine
    spring.cache.caffeine.spec: maximumSize=500,expireAfterWrite=5m
  □ Add @Cacheable("workflows") on findById
  □ Test: call GET /workflows/{id} twice
    → First: SQL log shows SELECT (cache miss)
    → Second: no SQL (cache hit)
  □ Add @CacheEvict("workflows", key = "#id") on update and delete
  □ Test: update workflow → next GET hits DB again (cache evicted)

SCENARIO 24.2: Prevent Caching Nulls
  □ Call GET /workflows/{non-existent-id}
    → Without unless: null is cached! Next call returns null from cache.
  □ Fix: @Cacheable(value = "workflows", key = "#id", unless = "#result == null")
  □ Verify: null is NOT cached, every call for non-existent ID hits DB

SCENARIO 24.3: Cache Stampede Prevention
  □ Add sync = true:
    @Cacheable(value = "workflows", key = "#id", sync = true)
  □ Simulate: 10 concurrent requests for same uncached ID
    → Without sync: all 10 hit DB simultaneously
    → With sync: only 1 hits DB, other 9 wait and get cached result
  □ Check SQL logs: only 1 SELECT instead of 10

SCENARIO 24.4: Switch to Redis (Distributed Cache)
  □ Add dependency: spring-boot-starter-data-redis
  □ Configure:
    spring.cache.type: redis
    spring.data.redis.host: localhost
    spring.data.redis.port: 6379
  □ Configure JSON serialization (not Java serialization!):
    - Create RedisCacheConfiguration bean with GenericJackson2JsonRedisSerializer
  □ Configure per-cache TTL:
    "workflows" → 5 min TTL
    "workflowList" → 1 min TTL (list changes more often)
  □ Test: call endpoint → check Redis with redis-cli:
    redis-cli KEYS "*workflow*"
    redis-cli GET "workflows::uuid-here"
  □ Verify: data is JSON, not Java-serialized bytes

SCENARIO 24.5: @CachePut for Write-Through
  □ On create method:
    @CachePut(value = "workflows", key = "#result.id")
    public Workflow create(CreateRequest req) { ... }
  □ Test: create workflow → immediately GET by ID
    → No DB hit (cache was populated by @CachePut)
  □ Compare: @Cacheable skips method if cached. @CachePut always runs method.

SCENARIO 24.6: @CacheEvict — allEntries and beforeInvocation
  □ On a "reindex" endpoint:
    @CacheEvict(value = "workflows", allEntries = true)
    public void reindex() { ... }
  □ Test: evict all → all subsequent GETs hit DB
  □ Test beforeInvocation = true vs false:
    → false (default): if method throws, cache is NOT evicted
    → true: cache evicted even if method throws

SCENARIO 24.7: Combining Cache Operations
  □ Use @Caching for multiple cache actions:
    @Caching(
        evict = {
            @CacheEvict(value = "workflows", key = "#id"),
            @CacheEvict(value = "workflowList", allEntries = true)
        }
    )
    public void delete(UUID id) { ... }
  □ Test: delete → both individual and list caches are evicted

SCENARIO 24.8: Self-Invocation Trap (same as @Transactional)
  □ Verify: calling @Cacheable method from within the same class
    bypasses cache proxy → method always executes
  □ Fix: extract to separate service bean

SCENARIO 24.9: Cache Hit Rate Monitoring
  □ Enable Micrometer metrics:
    management.endpoints.web.exposure.include: metrics
  □ Check: GET /actuator/metrics/cache.gets?tag=result:hit
  □ Check: GET /actuator/metrics/cache.gets?tag=result:miss
  □ Calculate hit rate: hits / (hits + misses) × 100%
  □ Target: > 90% for frequently accessed data
```

**Concepts:** Cache strategies, TTL, @Cacheable, Redis, Caffeine, stampede/avalanche
**Documentation:** `SpringBootRoadmap/Caching/README.md`

Status: `PLANNED`

---

## Step 25 — Distributed Locking

**Goal:** Implement distributed locks with Redisson.

Topics:
- Why synchronized/ReentrantLock fails in multi-instance deployments
- Redis SET NX EX for basic locking, safe release with Lua scripts
- Lease / TTL / watchdog auto-renewal
- Fencing tokens for correctness
- Redisson: RLock, fair locks, read/write locks, semaphores
- Deadlock risks and prevention
- Alternatives: DB constraints, idempotency keys, @Version

Practice in FlowForge:

```text
SCENARIO 25.1: The Problem — Synchronized Fails
  □ Add a "publish workflow" endpoint with synchronized:
    public synchronized void publish(UUID id) { ... }
  □ Document: this works for 1 instance but fails for 2+ instances
    (each JVM has its own lock — not shared)

SCENARIO 25.2: Basic Redis Lock (Manual)
  □ Implement with RedisTemplate:
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent("lock:publish:" + id, instanceId, 10, TimeUnit.SECONDS);
  □ Release with Lua (check-and-delete atomically):
    if redis.call('get', KEYS[1]) == ARGV[1] then
        return redis.call('del', KEYS[1])
    end
  □ Test: acquire lock → release → verify key deleted in Redis
  □ Test: acquire lock → DON'T release → verify TTL expires after 10s

SCENARIO 25.3: Redisson RLock (Production-Grade)
  □ Add dependency: org.redisson:redisson-spring-boot-starter
  □ Configure Redisson in application.yaml
  □ Implement publish with RLock:
    RLock lock = redissonClient.getLock("lock:publish:" + id);
    boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
    // 5s wait, 30s lease
    try {
        if (!acquired) throw new LockAcquireException();
        // critical section
    } finally {
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
  □ Test: two concurrent publish requests
    → First acquires lock, second waits up to 5s
    → If first finishes within 5s → second proceeds
    → If first takes > 5s → second fails (could not acquire lock)

SCENARIO 25.4: Watchdog Auto-Renewal
  □ Use lock() without lease time:
    lock.lock();  // no lease → Redisson starts watchdog
    // Watchdog extends TTL every 10s (default: lockWatchdogTimeout/3)
    // If JVM crashes → watchdog dies → lock auto-releases after 30s
  □ Test: acquire lock → sleep 60s → verify lock is still held (watchdog renewed)
  □ Compare: with tryLock(5, 10, SECONDS) → no watchdog → lock expires after 10s

SCENARIO 25.5: Fair Lock (FIFO Ordering)
  □ RLock fairLock = redissonClient.getFairLock("lock:publish:" + id);
    // Requests are served in FIFO order (first come, first served)
    // Regular RLock: no ordering guarantee — any waiting thread can acquire
  □ Test: 5 concurrent requests → verify they complete in order

SCENARIO 25.6: Lock vs DB Unique Constraint
  □ Scenario: prevent duplicate workflow publish
  □ Approach A: distributed lock (lock:publish:{id})
  □ Approach B: unique constraint on (workflow_id, status='ACTIVE') + catch exception
  □ Approach C: idempotency key table + check before publish
  □ Compare: which is simpler? Which handles failures better?
  □ Document trade-offs for each approach

SCENARIO 25.7: Debug Endpoint
  □ GET /debug/locks → show all active locks:
    - Iterate Redisson keys matching "lock:*"
    - Show: key, TTL remaining, holder instance
  □ DELETE /debug/locks/{key} → force-release a stuck lock (admin only)
```

**Concepts:** Distributed locks, Redisson, fencing tokens, idempotency
**Documentation:** `SpringBootRoadmap/Distributed-Locking/README.md`

Status: `PLANNED`

---

## Step 26 — Auditing

**Goal:** Implement audit tables to track who changed what and when.

Topics:
- Why auditing: compliance, debugging, accountability
- Spring Data JPA auditing: @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy
- @EnableJpaAuditing with AuditorAware implementation
- Hibernate Envers: automatic history tables, @Audited annotation
- Custom audit tables (manual approach): audit_log table with entity type, entity ID, action, old/new JSON
- CDC (Change Data Capture) with Debezium for external audit

Practice in FlowForge:

```text
SCENARIO 26.1: Basic JPA Auditing (@CreatedDate / @LastModifiedDate)
  □ Create a base class:
    @MappedSuperclass
    @EntityListeners(AuditingEntityListener.class)
    public abstract class Auditable {
        @CreatedDate
        @Column(updatable = false)
        private Instant createdAt;

        @LastModifiedDate
        private Instant updatedAt;
    }
  □ Make Workflow extend Auditable
  □ Add @EnableJpaAuditing on main class
  □ Test: create workflow → createdAt and updatedAt are auto-set
  □ Test: update workflow → updatedAt changes, createdAt stays the same

SCENARIO 26.2: @CreatedBy / @LastModifiedBy
  □ Create AuditorAware<String> implementation:
    @Component
    public class SpringSecurityAuditorAware implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // For now: return a hardcoded user or extract from request header
            return Optional.of("system");
        }
    }
  □ Add to Auditable:
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String modifiedBy;
  □ Add @EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
  □ Test: create → createdBy = "system"
  □ Test: update → modifiedBy = "system" (or different user)

SCENARIO 26.3: Custom Audit Log Table
  □ Create AuditEntry entity:
    @Entity
    public class AuditEntry {
        @Id @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;
        private String entityType;     // "Workflow", "Step"
        private UUID entityId;
        private String action;         // "CREATE", "UPDATE", "DELETE"
        private String changedBy;
        @Column(columnDefinition = "JSONB")
        private String oldValue;       // JSON snapshot of old state
        @Column(columnDefinition = "JSONB")
        private String newValue;       // JSON snapshot of new state
        private Instant changedAt;
    }
  □ Create AuditService:
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChange(String entityType, UUID entityId, String action,
                          Object oldValue, Object newValue) {
        // Serialize to JSON, save AuditEntry
    }
    // REQUIRES_NEW: audit persists even if outer TX rolls back
  □ Call auditService from WorkflowService on create, update, delete

SCENARIO 26.4: Audit with @TransactionalEventListener
  □ Instead of calling auditService directly, publish events:
    eventPublisher.publishEvent(new AuditEvent("Workflow", id, "UPDATE", old, new));
  □ Listen with @TransactionalEventListener(phase = AFTER_COMMIT):
    @Transactional(propagation = REQUIRES_NEW)
    public void onAudit(AuditEvent event) {
        auditRepo.save(new AuditEntry(...));
    }
  □ Benefit: audit only fires on successful commits (no phantom audit entries)

SCENARIO 26.5: Query Audit Trail
  □ GET /audit/{entityType}/{entityId}
    → Returns all audit entries for that entity, ordered by changedAt DESC
  □ GET /audit/recent?limit=50
    → Returns latest 50 changes across all entities
  □ GET /audit/{entityType}/{entityId}/diff/{auditId}
    → Returns the diff (old vs new values) for a specific change

SCENARIO 26.6: Hibernate Envers (Optional — Advanced)
  □ Add dependency: spring-boot-starter-data-jpa-envers (or hibernate-envers)
  □ Add @Audited on Workflow entity
  □ Hibernate auto-creates workflows_AUD table (history)
  □ Every INSERT/UPDATE/DELETE creates a row in workflows_AUD
  □ Query history:
    AuditReader reader = AuditReaderFactory.get(entityManager);
    List<Number> revisions = reader.getRevisions(Workflow.class, id);
    Workflow oldVersion = reader.find(Workflow.class, id, revisions.get(0));
  □ Compare: custom audit table vs Envers
    - Custom: more flexible, JSON-based, easier to query
    - Envers: automatic, mirrors entity structure, revision-based
```

**Concepts:** @CreatedDate, @LastModifiedDate, AuditorAware, audit tables, Envers, event-driven audit
**Documentation:** `SpringBootRoadmap/Auditing/README.md`

Status: `PLANNED`

---

---

---

# Phase 3 — Resilience, Messaging, Scheduling & API Design

## Step 27 — Resilience4j Integration

**Goal:** Add production-grade fault tolerance to FlowForge service calls.

Topics:
- Circuit Breaker: state machine (CLOSED → OPEN → HALF_OPEN), sliding windows
- Retry: exponential backoff + jitter, retryable vs non-retryable exceptions
- Bulkhead: semaphore vs thread pool, isolating downstream calls
- Rate Limiter: protecting downstream services from overload
- TimeLimiter: timeouts for async operations
- Fallback: graceful degradation strategies
- Combining patterns: decorator order matters
- Monitoring: Actuator endpoints, Prometheus/Micrometer metrics
- Testing: forcing circuit state, WireMock for failure simulation

Practice in FlowForge:

```text
SCENARIO 27.1: Simulate an External Service
  □ Create ExternalValidationService interface:
    - Simulates calling an external service to validate workflow configs
    - POST /api/external/validate → returns valid/invalid with random latency
  □ Create ExternalValidationClient:
    - Uses RestClient to call the validation endpoint
    - This is the target we'll wrap with Resilience4j

SCENARIO 27.2: Circuit Breaker
  □ Add dependencies: resilience4j-spring-boot3, spring-boot-starter-aop
  □ Configure in application.yml:
    resilience4j.circuitbreaker.instances.externalValidation:
      slidingWindowType: COUNT_BASED
      slidingWindowSize: 10
      minimumNumberOfCalls: 5
      failureRateThreshold: 50
      waitDurationInOpenState: 30s
      permittedNumberOfCallsInHalfOpenState: 3
      recordExceptions:
        - java.io.IOException
        - java.net.ConnectException
        - org.springframework.web.client.HttpServerErrorException
      ignoreExceptions:
        - com.flowforge.exception.BusinessException
  □ Add @CircuitBreaker(name = "externalValidation", fallbackMethod = "validateFallback")
  □ Implement fallback: return "validation skipped, pending manual review"
  □ Test: make external service return 500 repeatedly → observe CB open
  □ Test: verify fallback is invoked when CB is open
  □ Test: after wait duration → CB transitions to HALF_OPEN → test calls → CLOSED

SCENARIO 27.3: Retry with Backoff
  □ Configure retry:
    resilience4j.retry.instances.externalValidation:
      maxAttempts: 3
      waitDuration: 1s
      enableExponentialBackoff: true
      exponentialBackoffMultiplier: 2
      enableRandomizedWait: true
      randomizedWaitFactor: 0.5
      retryExceptions:
        - java.io.IOException
        - java.util.concurrent.TimeoutException
      ignoreExceptions:
        - com.flowforge.exception.BusinessException
  □ Add @Retry(name = "externalValidation") on the client method
  □ Test: simulate transient failure → succeeds on 2nd or 3rd attempt
  □ Test: simulate permanent failure → all retries exhausted → fallback
  □ Observe logs: retry attempt #1, #2, #3 with increasing delays

SCENARIO 27.4: Bulkhead
  □ Configure bulkhead:
    resilience4j.bulkhead.instances.externalValidation:
      maxConcurrentCalls: 5
      maxWaitDuration: 500ms
  □ Add @Bulkhead(name = "externalValidation")
  □ Test: send 10 concurrent validation requests
    → First 5 proceed, next 5 wait up to 500ms
    → If still full → BulkheadFullException → fallback
  □ Observe: other FlowForge operations (CRUD) unaffected by slow validation

SCENARIO 27.5: Rate Limiter
  □ Configure rate limiter:
    resilience4j.ratelimiter.instances.externalValidation:
      limitForPeriod: 10
      limitRefreshPeriod: 1s
      timeoutDuration: 0s
  □ Add @RateLimiter(name = "externalValidation")
  □ Test: send 15 requests in 1 second → first 10 succeed, next 5 rejected
  □ Handle RequestNotPermitted in fallback

SCENARIO 27.6: Combining All Patterns
  □ Stack annotations on the client method:
    @CircuitBreaker(name = "externalValidation", fallbackMethod = "fallback")
    @Retry(name = "externalValidation")
    @Bulkhead(name = "externalValidation")
    @RateLimiter(name = "externalValidation")
  □ Verify execution order: Retry → CB → RateLimiter → Bulkhead → call
  □ Test scenarios:
    a. Transient failure → retry succeeds → no CB impact
    b. Sustained failure → retries exhausted → CB records failures → CB opens
    c. CB open → calls rejected instantly → fallback → no retries
    d. Rate limit hit → rejected before reaching CB
    e. Bulkhead full → rejected before making call

SCENARIO 27.7: Monitoring
  □ Add resilience4j-micrometer dependency
  □ Expose actuator endpoints:
    management.endpoints.web.exposure.include: circuitbreakers,retries,bulkheads,ratelimiters
  □ Call GET /actuator/circuitbreakers/externalValidation → verify state
  □ Simulate failures → observe state transition in /actuator/circuitbreakerevents
  □ Verify metrics:
    - resilience4j_circuitbreaker_state
    - resilience4j_circuitbreaker_failure_rate
    - resilience4j_retry_calls_total
    - resilience4j_bulkhead_available_concurrent_calls

SCENARIO 27.8: Testing with State Control
  □ Inject CircuitBreakerRegistry in test
  □ Test: cb.transitionToOpenState() → verify fallback invoked
  □ Test: cb.transitionToHalfOpenState() → verify limited calls pass
  □ Test: cb.reset() → clean state between tests
  □ WireMock test: stub external service → 500 → verify CB opens
  □ WireMock test: stub with 5s delay → verify timeout + retry
```

**Concepts:** Circuit Breaker, Retry, Bulkhead, Rate Limiter, fallback, decorator order, monitoring
**Documentation:** `SpringBootRoadmap/Resilience/01-04` (all 4 files)

Status: `PLANNED`

---

## Step 28 — Kafka Integration

**Goal:** Add event-driven messaging to FlowForge using Kafka.

Topics:
- Kafka fundamentals: topics, partitions, offsets, consumer groups, brokers
- Producer: KafkaTemplate, batching, acks, idempotent producer
- Consumer: @KafkaListener, poll loop, rebalancing, offset management
- Serialization: JSON serializer/deserializer, schema design
- Error handling: DefaultErrorHandler, DLT (Dead Letter Topic), retry topic
- Idempotent consumer: preventing duplicate event processing
- Transactional Outbox pattern: reliable event publishing
- Spring Kafka testing: @EmbeddedKafka, Testcontainers
- Monitoring: consumer lag, JMX metrics

Practice in FlowForge:

```text
SCENARIO 28.1: Setup Kafka with Docker Compose
  □ Add docker-compose.yml with:
    - Zookeeper (or KRaft single-node)
    - Kafka broker (port 9092)
    - Kafka UI (optional, for visual inspection)
  □ Add dependency: spring-kafka
  □ Configure in application.yml:
    spring.kafka.bootstrap-servers: localhost:9092
    spring.kafka.producer.key-serializer: StringSerializer
    spring.kafka.producer.value-serializer: JsonSerializer
    spring.kafka.consumer.key-deserializer: StringDeserializer
    spring.kafka.consumer.value-deserializer: JsonDeserializer
    spring.kafka.consumer.group-id: flowforge-group
    spring.kafka.consumer.auto-offset-reset: earliest

SCENARIO 28.2: Publish Events on Workflow State Changes
  □ Create WorkflowEvent record:
    { eventId, eventType, workflowId, workflowName, status, timestamp, changedBy }
  □ Create WorkflowEventPublisher:
    @Component with KafkaTemplate<String, WorkflowEvent>
    publish(topic = "workflow-events", key = workflowId, value = event)
  □ Call publisher from WorkflowService on create, update, delete, status change
  □ Test: create a workflow → check Kafka UI → message visible on topic
  □ Key: use workflowId as the Kafka key (ensures ordering per workflow)

SCENARIO 28.3: Consume Events
  □ Create WorkflowEventConsumer:
    @KafkaListener(topics = "workflow-events", groupId = "flowforge-analytics")
    public void consume(WorkflowEvent event) {
        log.info("Received event: {} for workflow {}", event.eventType(), event.workflowId());
        // Simulate analytics processing
    }
  □ Test: publish event → consumer logs it
  □ Add consumer for a second group:
    @KafkaListener(topics = "workflow-events", groupId = "flowforge-notifications")
    → Both consumers receive the same event (different groups)

SCENARIO 28.4: Error Handling + Dead Letter Topic
  □ Configure DefaultErrorHandler with DeadLetterPublishingRecoverer:
    - 3 retries with backoff
    - After 3 failures → send to "workflow-events.DLT"
  □ Create a consumer that throws on specific events (simulate poison message)
  □ Verify: message retried 3 times → sent to DLT
  □ Create DLT consumer: @KafkaListener(topics = "workflow-events.DLT")
    → Log the failed event for manual investigation
  □ Test: verify original topic consumer continues processing other messages

SCENARIO 28.5: Idempotent Consumer
  □ Create processed_events table: (event_id UUID PRIMARY KEY, processed_at TIMESTAMP)
  □ In consumer:
    1. Check if event_id exists in processed_events
    2. If yes → skip (already processed)
    3. If no → process → insert event_id into processed_events
    4. Wrap in @Transactional
  □ Test: send same event twice → processed only once
  □ Test: consumer crash after process but before commit → event redelivered → idempotency check → skip

SCENARIO 28.6: Transactional Outbox Pattern
  □ Create outbox_events table:
    (id UUID PK, aggregate_type, aggregate_id, event_type, payload JSONB,
     created_at TIMESTAMP, published BOOLEAN DEFAULT FALSE)
  □ In WorkflowService.create():
    @Transactional → save workflow + save OutboxEvent in SAME transaction
  □ Create OutboxPublisher scheduled job:
    @Scheduled(fixedDelay = 1000)
    → SELECT from outbox_events WHERE published = false ORDER BY created_at LIMIT 50
    → Publish each to Kafka
    → Mark as published = true
  □ Benefit: event is guaranteed to be published (same TX as data change)
  □ Test: create workflow → outbox entry created → publisher sends to Kafka

SCENARIO 28.7: Testing with @EmbeddedKafka
  □ Create integration test with @EmbeddedKafka:
    @SpringBootTest
    @EmbeddedKafka(partitions = 1, topics = "workflow-events")
    class WorkflowEventTest {
        @Autowired KafkaTemplate<String, WorkflowEvent> template;
        @Autowired KafkaConsumer consumer;

        @Test void shouldPublishAndConsumeEvent() { ... }
    }
  □ Test: publish → consume → verify event content
  □ Test: error handling → DLT delivery
  □ Alternative: Testcontainers with real Kafka container

SCENARIO 28.8: Consumer Lag Monitoring
  □ Add Micrometer Kafka metrics:
    spring.kafka.listener.observation-enabled: true
  □ Check: GET /actuator/metrics/kafka.consumer.fetch.manager.records.lag
  □ Simulate slow consumer → observe lag increasing
  □ Document: what lag means, when to alert, how to fix
```

**Concepts:** Kafka producer/consumer, event design, DLT, idempotent consumer, outbox, testing
**Documentation:** `SpringBootRoadmap/Kafka-Deep-Dive/01-05`, `SpringBootRoadmap/Messaging/README.md`, `SpringBootRoadmap/Reliable-Messaging/README.md`

Status: `PLANNED`

---

## Step 29 — Job Scheduling

**Goal:** Implement scheduled jobs, thread pool configuration, and distributed scheduling.

Topics:
- @EnableScheduling, @Scheduled: fixedRate, fixedDelay, cron
- Thread pool configuration (the single-thread trap)
- Error handling in scheduled tasks
- Externalized cron configuration
- Dynamic scheduling (SchedulingConfigurer, TaskScheduler API)
- ShedLock for distributed scheduling (multi-instance safety)
- Quartz basics (awareness, not full implementation)
- Monitoring scheduled jobs
- Idempotent jobs

Practice in FlowForge:

```text
SCENARIO 29.1: Basic Scheduled Jobs
  □ Add @EnableScheduling on main class
  □ Create ScheduledMaintenanceService:
    @Scheduled(cron = "${flowforge.scheduler.cleanup-cron:0 0 2 * * *}", zone = "UTC")
    public void cleanupDraftWorkflows() {
        // Delete DRAFT workflows older than 30 days
        int deleted = workflowRepo.deleteByStatusAndCreatedAtBefore(
            WorkflowStatus.DRAFT, Instant.now().minus(30, ChronoUnit.DAYS));
        log.info("Cleaned up {} draft workflows", deleted);
    }
  □ Create StatsCollectorJob:
    @Scheduled(fixedDelayString = "${flowforge.scheduler.stats-interval:60000}")
    public void collectStats() {
        // Count workflows by status, log summary
        Map<WorkflowStatus, Long> counts = workflowRepo.countByStatus();
        log.info("Workflow stats: {}", counts);
    }
  □ Externalize all schedules in application.yml
  □ Test: verify jobs execute at configured intervals (check logs)

SCENARIO 29.2: Thread Pool Configuration
  □ DEMONSTRATE the problem: add 3 scheduled jobs with fixedRate = 1000
    Each sleeps for 3 seconds → all run on 1 thread → each runs every 9s
  □ Fix: create ThreadPoolTaskScheduler bean:
    poolSize = 5, threadNamePrefix = "ff-scheduler-"
    errorHandler, waitForTasksToCompleteOnShutdown = true
  □ Verify: all 3 jobs now run concurrently, each at their own rate
  □ Check thread names in logs: ff-scheduler-1, ff-scheduler-2, ff-scheduler-3

SCENARIO 29.3: Error Handling
  □ Add global error handler on TaskScheduler:
    scheduler.setErrorHandler(t -> {
        log.error("Scheduled task failed", t);
        meterRegistry.counter("scheduler.error").increment();
    });
  □ Add per-job try-catch with metrics:
    Timer.Sample sample = Timer.start(meterRegistry);
    try { ... meterRegistry.counter("scheduler.cleanup.success").increment(); }
    catch (Exception e) { meterRegistry.counter("scheduler.cleanup.failure").increment(); }
    finally { sample.stop(meterRegistry.timer("scheduler.cleanup.duration")); }
  □ Test: force a job to throw → verify error is logged, next run still happens
  □ Check metrics: GET /actuator/metrics/scheduler.cleanup.success

SCENARIO 29.4: Conditional Scheduling
  □ Add @ConditionalOnProperty(name = "flowforge.scheduler.cleanup.enabled", havingValue = "true")
  □ Test: set enabled=false → cleanup job does NOT register
  □ Add @Profile("!test") → jobs disabled in test profile
  □ Test: verify scheduled jobs don't fire during integration tests

SCENARIO 29.5: Dynamic Scheduling
  □ Create ScheduleConfig entity: (job_name VARCHAR PK, cron_expression VARCHAR, enabled BOOLEAN)
  □ Implement SchedulingConfigurer:
    addTriggerTask(cleanupTask, context -> {
        String cron = scheduleConfigRepo.findCronByJobName("cleanup")
            .orElse("0 0 2 * * *");
        return new CronTrigger(cron).nextExecution(context);
    });
  □ Create admin endpoint: PATCH /admin/schedules/{jobName} { "cron": "0 */5 * * * *" }
  □ Test: change cron via API → job frequency changes WITHOUT restart
  □ Test: set enabled=false → job stops executing

SCENARIO 29.6: ShedLock (Distributed Scheduling)
  □ Add dependency: shedlock-spring + shedlock-provider-jdbc-template
  □ Create shedlock table:
    CREATE TABLE shedlock (name VARCHAR(64), lock_until TIMESTAMP, locked_at TIMESTAMP,
                           locked_by VARCHAR(255), PRIMARY KEY (name));
  □ Add @EnableSchedulerLock(defaultLockAtMostFor = "10m")
  □ Annotate jobs:
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "cleanup-drafts", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void cleanupDraftWorkflows() { ... }
  □ Test: start 2 instances of FlowForge → verify only ONE executes the job
  □ Inspect shedlock table: see lock_until, locked_by values
  □ Test: kill the instance holding the lock → other instance picks up after lockAtMostFor

SCENARIO 29.7: Job Monitoring Dashboard
  □ Create GET /admin/scheduler/status endpoint:
    - List all registered scheduled tasks (method name, trigger type, cron/rate)
    - Last run time, next run time, last status (success/failure)
    - Use ScheduledTaskHolder to inspect registered tasks
  □ Dead man's switch concept: track last_success_time per job
    If now - last_success_time > 2 × expected_interval → alert
```

**Concepts:** @Scheduled, cron, thread pool, ShedLock, dynamic scheduling, monitoring
**Documentation:** `SpringBootRoadmap/Job-Scheduling/README.md`, `SpringBootRoadmap/Distributed-Scheduling/README.md`

Status: `PLANNED`

---

## Step 30 — Async Processing

**Goal:** Add async task execution for non-blocking operations.

Topics:
- @EnableAsync, @Async annotation
- Executor configuration (thread pool sizing)
- Return types: void, Future, CompletableFuture
- Exception handling in async methods
- Context propagation (MDC, security context)
- @Async + @Transactional interaction (different threads = different TX)
- Self-invocation trap (same as @Transactional, @Cacheable)
- Virtual threads with async (Spring Boot 3.2+)

Practice in FlowForge:

```text
SCENARIO 30.1: Basic @Async Setup
  □ Add @EnableAsync on main class
  □ Configure executor:
    @Bean
    public TaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ff-async-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
  □ Create NotificationService:
    @Async
    public void sendWorkflowNotification(UUID workflowId, String action) {
        log.info("Sending notification for workflow {} action {}", workflowId, action);
        Thread.sleep(2000);  // simulate slow email/webhook
        log.info("Notification sent");
    }
  □ Call from WorkflowService.create() → verify: create returns immediately,
    notification runs on separate thread (check thread name in logs)

SCENARIO 30.2: CompletableFuture Return
  □ Create:
    @Async
    public CompletableFuture<ValidationResult> validateAsync(WorkflowConfig config) {
        // Expensive validation
        return CompletableFuture.completedFuture(result);
    }
  □ Call from controller: CompletableFuture<ValidationResult> future = service.validateAsync(config);
  □ Combine multiple async calls:
    CompletableFuture.allOf(validate1, validate2, validate3).join();
  □ Test: 3 validations that each take 2s → total time ~2s (parallel), not 6s (sequential)

SCENARIO 30.3: Exception Handling
  □ For void @Async methods: create AsyncExceptionHandler implements AsyncUncaughtExceptionHandler
    → Log error, increment metrics
  □ Configure: implement AsyncConfigurer.getAsyncUncaughtExceptionHandler()
  □ For CompletableFuture: use .exceptionally() or .handle() on the caller side
  □ Test: async method throws → verify exception is caught and logged (not silently swallowed)

SCENARIO 30.4: @Async + @Transactional
  □ Create:
    @Async @Transactional
    public void processWorkflowAsync(UUID id) {
        Workflow wf = repo.findById(id).orElseThrow();
        wf.setStatus(WorkflowStatus.ACTIVE);
        // This runs in its OWN transaction on the async thread
    }
  □ Call from a @Transactional method that throws AFTER the async call
  □ Verify: caller's TX rolls back, but async method's TX commits
    (different thread = different TX = independent)
  □ Document this behavior — it surprises many developers

SCENARIO 30.5: MDC Context Propagation
  □ Problem: @Async runs on different thread → MDC (requestId, userId) is LOST
  □ Create TaskDecorator:
    public class MdcTaskDecorator implements TaskDecorator {
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                MDC.setContextMap(contextMap);
                try { runnable.run(); }
                finally { MDC.clear(); }
            };
        }
    }
  □ Register: executor.setTaskDecorator(new MdcTaskDecorator());
  □ Test: set MDC requestId in filter → verify it appears in async thread logs

SCENARIO 30.6: Self-Invocation Trap
  □ Create a method that calls @Async internally:
    public void process() { this.asyncStep(); }  // bypasses proxy!
    @Async public void asyncStep() { ... }
  □ Verify: asyncStep runs on the SAME thread (not async)
  □ Fix: inject self or extract to separate service
```

**Concepts:** @Async, executor config, CompletableFuture, context propagation, exception handling
**Documentation:** `SpringBootRoadmap/Async-Processing/README.md`

Status: `PLANNED`

---

## Step 31 — API Design Refinement

**Goal:** Apply REST API design best practices to the existing FlowForge API.

Topics:
- Review existing endpoints against REST best practices
- Idempotency key support for POST endpoints
- ETag / If-Match for conditional requests
- HATEOAS links (optional)
- API documentation with Springdoc OpenAPI
- Rate limiting headers (X-RateLimit-*)
- Consistent error response format audit
- Response envelope vs flat response decision

Practice in FlowForge:

```text
SCENARIO 31.1: Idempotency Key for POST /workflows
  □ Create idempotency_keys table: (key VARCHAR PK, response JSONB, created_at TIMESTAMP)
  □ Accept Idempotency-Key header on POST endpoints
  □ Logic:
    1. If key exists → return stored response (no duplicate create)
    2. If key absent → process → store key + response
  □ TTL: clean up keys older than 24 hours (scheduled job from Step 29)
  □ Test: send same POST twice with same Idempotency-Key → same response, 1 workflow created
  □ Test: send POST without Idempotency-Key → works normally (optional header)

SCENARIO 31.2: ETag / Conditional Requests
  □ GET /workflows/{id} response:
    Add ETag header: ETag: "{version}"  (from @Version field added in Step 22)
  □ PUT /workflows/{id} request:
    Accept If-Match header
    If If-Match != current version → 412 Precondition Failed
    If match → proceed with update
  □ GET with If-None-Match:
    If ETag matches → 304 Not Modified (no body, save bandwidth)
  □ Test: GET → save ETag → PUT with If-Match → success
  □ Test: concurrent update → second PUT gets 412

SCENARIO 31.3: Rate Limit Headers
  □ Enhance existing RateLimitInterceptor:
    Add response headers:
      X-RateLimit-Limit: 100
      X-RateLimit-Remaining: 73
      X-RateLimit-Reset: 1721209200
    On limit exceeded:
      429 Too Many Requests + Retry-After header
  □ Test: send requests → observe headers counting down
  □ Test: exceed limit → 429 with Retry-After

SCENARIO 31.4: Springdoc OpenAPI Documentation
  □ Add dependency: springdoc-openapi-starter-webmvc-ui
  □ Add @Tag on controllers
  □ Add @Operation on key endpoints
  □ Add @Schema on DTOs
  □ Access: GET /swagger-ui.html → interactive API documentation
  □ Access: GET /v3/api-docs → JSON OpenAPI spec
  □ Group APIs: "Public API" vs "Admin API"

SCENARIO 31.5: API Audit Checklist
  □ Review all endpoints against the API Design checklist:
    ✅ Resources are nouns (not verbs)
    ✅ Plural nouns for collections (/workflows, not /workflow)
    ✅ Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)
    ✅ Correct status codes (201 for create, 204 for delete, 422 for business error)
    ✅ Pagination on all list endpoints
    ✅ Filtering via query parameters
    ✅ Sorting support
    ✅ Consistent error response format
    ✅ Timestamps in ISO 8601 UTC
    ✅ IDs are UUIDs (not sequential)
    ✅ Versioned URL (/api/v1/)
  □ Fix any gaps found during the audit
```

**Concepts:** Idempotency, ETag, rate limit headers, OpenAPI, REST best practices
**Documentation:** `SpringBootRoadmap/API-Design/README.md`

Status: `PLANNED`

---

## Phase 3 Concept Coverage Matrix

```text
Doc Topic                                 Practiced In Step
-------------------------------------------------------------------
Resilience4j (Circuit Breaker, Retry,     27
  Bulkhead, Rate Limiter, Fallback)
Kafka (Producer, Consumer, Spring Kafka)  28
Event-Driven Architecture (DLT, Outbox)   28
Job Scheduling (@Scheduled, ShedLock)     29
Async Processing (@Async, Executors)      30
API Design Best Practices                 31
```

---

## How to Work Through This

```text
1. Complete one step fully before moving to the next
2. Test every endpoint with Postman / curl after each step
3. Commit after each step (one commit per step)
4. If something breaks, fix it before moving on
5. Phase 1 (Steps 1-13): REST API + Spring Core
6. Phase 2 (Steps 18-26): Database, JPA, Transactions, Caching, Locking, Auditing
7. Phase 3 (Steps 27-31): Resilience, Messaging, Scheduling, Async, API Design
8. Each SCENARIO within a step is a mini-task:
   □ Read the documentation first
   □ Implement the scenario
   □ Test it (endpoint + SQL logs)
   □ Check the box
   □ Commit
```

---

## Future Enhancements

### Database Migrations (Flyway or Liquibase)

```text
Currently using: ddl-auto: create-drop + schema-postgres.sql (no versioning)
TODO: Replace with a proper migration tool before any production-like deployment.

  □ Choose: Flyway (simpler, SQL files) or Liquibase (YAML/XML, rollback support)
  □ Add Spring Boot starter dependency
  □ Convert existing schema to V1 baseline migration
  □ Move constraints + indexes from schema-postgres.sql into versioned migrations
  □ Set ddl-auto: validate (Hibernate only validates, never modifies schema)
  □ Each future schema change = new migration file with version number
```

### Auditing (Spring Data JPA + Custom Audit Tables)

```text
  □ Create Auditable base class: @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy
  □ Implement AuditorAware<String> for current user resolution
  □ @EnableJpaAuditing on main class
  □ Create AuditEntry entity (entityType, entityId, action, oldValue JSONB, newValue JSONB)
  □ AuditService with @Transactional(propagation = REQUIRES_NEW) — persists even on rollback
  □ Event-driven audit with @TransactionalEventListener (fires only on commit)
  □ Audit query endpoints: GET /audit/{entityType}/{entityId}, GET /audit/recent
  □ Optional: Hibernate Envers for automatic history tables (@Audited)

  Documentation: SpringBootRoadmap/Auditing/README.md
```

### Connection Pooling Tuning (HikariCP)

```text
  □ Configure HikariCP: maximum-pool-size, minimum-idle, connection-timeout, idle-timeout, max-lifetime
  □ Enable leak-detection-threshold in dev profile
  □ Simulate pool exhaustion with small pool + concurrent requests
  □ Add @ConcurrencyLimit as pool protector
  □ Enable Actuator metrics: hikaricp.connections.active/idle/pending/timeout
  □ Pool size experiment: compare performance with pool size 2 vs 5 vs 20
  □ Document: diminishing returns after (cores × 2) + 1

  Documentation: SpringBootRoadmap/Connection-Pooling/README.md
  Deep dive: SpringBootRoadmap/Resilience/HIKARI-CONNECTION-POOL.md
```
