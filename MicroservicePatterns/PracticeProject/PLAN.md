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
SCENARIO 18.1: Schema Review & Constraints
  □ Review all existing tables — verify every table has a proper PK
  □ Add NOT NULL constraints on workflow.name, step.name
  □ Add CHECK constraint: workflow.status IN ('DRAFT','ACTIVE','ARCHIVED')
  □ Add UNIQUE constraint on workflow.name (no duplicate workflow names)
  □ Verify FK: steps.workflow_id → workflows.id with ON DELETE CASCADE
  □ Test: try inserting a step with non-existent workflow_id → expect FK violation

SCENARIO 18.2: Normalization Audit
  □ Analyze current schema against 1NF-3NF rules
  □ Identify any repeating groups or partial dependencies
  □ Document: "Is our schema in 3NF? Why or why not?"
  □ If not 3NF: propose and apply the fix
  □ Think: would denormalization help any read-heavy queries? Where?

SCENARIO 18.3: Index Design
  □ Add composite index: CREATE INDEX idx_workflow_status_name ON workflows(status, name)
  □ Add index on steps.workflow_id (FK index for join performance)
  □ Run: EXPLAIN ANALYZE SELECT * FROM workflows WHERE status = 'ACTIVE'
    → Verify Index Scan is used (not Seq Scan)
  □ Run: EXPLAIN ANALYZE SELECT * FROM workflows WHERE name = 'Deploy'
    → Observe: does the composite index help? (leftmost prefix rule)
  □ Run: EXPLAIN ANALYZE SELECT * FROM workflows WHERE name = 'Deploy' AND status = 'ACTIVE'
    → Compare with the previous query plan
  □ Add a covering index: CREATE INDEX idx_workflow_cover ON workflows(status) INCLUDE (name, id)
  □ Run EXPLAIN ANALYZE on SELECT id, name FROM workflows WHERE status = 'ACTIVE'
    → Verify "Index Only Scan" (no heap fetch)
  □ Document: query plan screenshots/output before and after each index

SCENARIO 18.4: Join Practice
  □ Write INNER JOIN: workflows + steps (only workflows that have steps)
  □ Write LEFT JOIN: all workflows + their steps (including workflows with 0 steps)
  □ Write a self-join: find workflows created on the same day
  □ Run EXPLAIN ANALYZE on each join → document join strategy (Nested Loop, Hash Join, Merge Join)
  □ Add test data: 100 workflows with 0-5 steps each → re-run EXPLAIN

SCENARIO 18.5: Locking Observation
  □ Open two psql sessions (or two DataGrip connections)
  □ Session 1: BEGIN; SELECT * FROM workflows WHERE id = ? FOR UPDATE;
  □ Session 2: BEGIN; UPDATE workflows SET name = 'Test' WHERE id = ?;
    → Observe: Session 2 WAITS (blocked by Session 1's row lock)
  □ Session 1: COMMIT; → Session 2 proceeds
  □ Create a deadlock scenario:
    Session 1: UPDATE workflows SET name = 'A' WHERE id = 1;
    Session 2: UPDATE workflows SET name = 'B' WHERE id = 2;
    Session 1: UPDATE workflows SET name = 'A' WHERE id = 2;  -- waits
    Session 2: UPDATE workflows SET name = 'B' WHERE id = 1;  -- DEADLOCK!
  □ Observe: PostgreSQL detects deadlock and kills one transaction
```

**Concepts:** Relational modeling, normalization, indexes, query plans, locking
**Documentation:** `SpringBootRoadmap/Database-Fundamentals/README.md`

Status: `PLANNED`

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
SCENARIO 19.1: Entity Mapping Verification
  □ Verify @Entity on Workflow and Step
  □ Verify @Id + @GeneratedValue(strategy = GenerationType.UUID) on both
  □ Add @Column(nullable = false) on name fields
  □ Enable SQL logging: spring.jpa.show-sql=true, format_sql=true
  □ Observe the exact DDL generated by Hibernate on startup

SCENARIO 19.2: Persistence Context & Dirty Checking
  □ Create a test/debug endpoint: GET /debug/dirty-checking/{id}
    - Load workflow by ID (MANAGED)
    - Change the name with setName() — do NOT call save()
    - Return the workflow
    - Check logs: UPDATE should appear at transaction commit
  □ Verify: the DB has the new name (dirty checking worked)
  □ Add @DynamicUpdate to Workflow entity
    - Change only the name field
    - Check logs: UPDATE should only include name column, not all columns
    - Remove @DynamicUpdate and compare: now ALL columns are in UPDATE

SCENARIO 19.3: Entity States
  □ Create endpoint: GET /debug/entity-states/{id}
    - Inject EntityManager with @PersistenceContext
    - find() → log em.contains(entity) → expect true (MANAGED)
    - em.detach(entity) → log em.contains(entity) → expect false (DETACHED)
    - entity.setName("Test") → change is NOT tracked (no UPDATE on commit)
    - em.merge(entity) → returns NEW managed copy
    - log: original == merged → expect false (different objects!)
  □ Document each state transition with the log output

SCENARIO 19.4: persist vs merge vs save()
  □ Test persist():
    - new Workflow() → em.persist(wf) → check wf has UUID after persist
    - Verify: only ONE SELECT-less INSERT in logs
  □ Test merge() on detached:
    - Load workflow in one TX → modify outside TX → merge in new TX
    - Verify: SELECT + UPDATE in logs (merge re-loads from DB)
  □ Test save() on managed (the pitfall):
    - @Transactional method: findById → setName → repo.save(wf)
    - Check logs: unnecessary extra operations
    - Remove save() → same result, fewer queries

SCENARIO 19.5: find() vs getReference()
  □ Create endpoint: POST /debug/step-with-reference/{workflowId}
    - Use em.getReference(Workflow.class, workflowId) to set FK
    - em.persist(new Step) with the proxy reference
    - Verify: NO SELECT for workflow, just INSERT for step
  □ Compare with find():
    - Use em.find(Workflow.class, workflowId) to set FK
    - Verify: SELECT for workflow + INSERT for step (extra query)

SCENARIO 19.6: Batch Insert with flush/clear
  □ Create endpoint: POST /debug/batch-insert?count=5000
    - Loop: em.persist(new Workflow("WF-" + i))
    - Every 500: em.flush() + em.clear()
    - Log time taken
  □ Compare: without flush/clear (same 5000 inserts)
    - Watch memory usage — expect OOM or high heap without clear
  □ Compare: with Spring Data saveAll() — observe differences

SCENARIO 19.7: EntityManager Query Methods
  □ Write a JPQL query: SELECT w FROM Workflow w WHERE w.status = :status
  □ Write the same as native SQL: SELECT * FROM workflows WHERE status = ?
  □ Write the same with Criteria API (type-safe)
  □ Compare: which returns MANAGED entities? (JPQL + Criteria yes, native depends)
  □ Test getSingleResult() with 0 results → catch NoResultException
  □ Test getSingleResult() with 2 results → catch NonUniqueResultException

SCENARIO 19.8: Bulk Update (bypassing entity lifecycle)
  □ Create endpoint: PUT /debug/archive-old
    - em.createQuery("UPDATE Workflow w SET w.status = 'ARCHIVED' WHERE w.createdAt < :date")
    - executeUpdate() → returns count of affected rows
    - Verify: no entities loaded, no dirty checking, one SQL statement
    - ⚠️ Test: load a workflow BEFORE bulk update, then check its status
      → It's STALE! (persistence context has old state)
    - Fix: em.clear() after bulk update, or em.refresh(entity)
```

**Concepts:** Entity lifecycle, persistence context, dirty checking, flush/clear, EntityManager operations
**Documentation:** `SpringBootRoadmap/JPA-Hibernate/README.md`

Status: `PLANNED`

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
SCENARIO 20.1: N+1 Detection
  □ Enable statistics:
    spring.jpa.properties.hibernate.generate_statistics=true
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true
  □ Call GET /workflows (findAll) → observe SQL logs
    - Count queries: 1 SELECT for workflows + N SELECTs for steps
    - Log output: "N+1 detected: 1 + {count} queries for {count} workflows"
  □ Add 50 workflows with 3 steps each → call findAll
    - Expected: 51 queries (1 + 50) — unacceptable!

SCENARIO 20.2: Fix with JOIN FETCH
  □ Add to WorkflowRepository:
    @Query("SELECT DISTINCT w FROM Workflow w JOIN FETCH w.steps")
    List<Workflow> findAllWithSteps();
  □ Call the new method → observe: 1 query only
  □ Compare query count: 51 → 1
  □ ⚠️ Test pagination: JOIN FETCH + Pageable → HHH90003004 warning!
    Hibernate applies limit IN MEMORY (loads all, then paginates)
    This is a common production bug. Document it.

SCENARIO 20.3: Fix with @EntityGraph
  □ Add to WorkflowRepository:
    @EntityGraph(attributePaths = {"steps"})
    @Override
    List<Workflow> findAll();
  □ Verify: 1 query (LEFT JOIN generated)
  □ Compare: @EntityGraph generates LEFT JOIN, JOIN FETCH generates INNER JOIN
  □ Test: workflow with 0 steps — @EntityGraph returns it, JOIN FETCH might not

SCENARIO 20.4: Fix with @BatchSize
  □ Add @BatchSize(size = 25) on Workflow.steps field
  □ Call findAll with 50 workflows → observe:
    - Query 1: SELECT workflows (50 rows)
    - Query 2: SELECT steps WHERE workflow_id IN (25 IDs)
    - Query 3: SELECT steps WHERE workflow_id IN (25 IDs)
    - Total: 3 queries instead of 51
  □ Also try global setting:
    spring.jpa.properties.hibernate.default_batch_fetch_size=25

SCENARIO 20.5: Fix with SUBSELECT
  □ Add @Fetch(FetchMode.SUBSELECT) on Workflow.steps field
  □ Call findAll → observe:
    - Query 1: SELECT workflows
    - Query 2: SELECT steps WHERE workflow_id IN (SELECT id FROM workflows)
    - Total: exactly 2 queries
  □ Compare all 4 approaches: query count, data transferred, memory usage

SCENARIO 20.6: LazyInitializationException
  □ Set spring.jpa.open-in-view=false (if not already)
  □ Create a service method WITHOUT @Transactional:
    public Workflow getWorkflow(UUID id) {
        return repo.findById(id).orElseThrow();
    }
  □ In controller: call wf.getSteps() → LazyInitializationException!
  □ Fix 1: change service method to use findByIdWithSteps (JOIN FETCH)
  □ Fix 2: add @Transactional and call wf.getSteps().size() to force load
  □ Fix 3: return a DTO instead of the entity

SCENARIO 20.7: DTO Projections
  □ Create interface projection:
    public interface WorkflowSummary {
        UUID getId();
        String getName();
        String getStatus();
    }
  □ Add to repository: List<WorkflowSummary> findAllProjectedBy();
  □ Compare queries: entity findAll vs projection findAll
    - Entity: SELECT * (all columns) + lazy proxies
    - Projection: SELECT id, name, status (only needed columns)
  □ Add a class-based DTO projection with @Query + constructor expression:
    @Query("SELECT new com.flowforge.dto.WorkflowDTO(w.id, w.name, w.status) FROM Workflow w")
  □ Benchmark: entity vs interface projection vs class projection
```

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

## How to Work Through This

```text
1. Complete one step fully before moving to the next
2. Test every endpoint with Postman / curl after each step
3. Commit after each step (one commit per step)
4. If something breaks, fix it before moving on
5. Phase 1 (Steps 1-13): REST API + Spring Core
6. Phase 2 (Steps 18-26): Database, JPA, Transactions, Caching, Locking, Auditing
7. Each SCENARIO within a step is a mini-task:
   □ Read the documentation first
   □ Implement the scenario
   □ Test it (endpoint + SQL logs)
   □ Check the box
   □ Commit
```
