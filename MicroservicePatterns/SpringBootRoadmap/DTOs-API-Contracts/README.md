# Phase 11 -- DTOs and API Contracts

Why you should **never** expose JPA entities directly through REST APIs, and how to design clean request/response contracts.

---

## 1. The Problem: Entity = API Contract

```java
// BAD: Entity exposed directly
@Entity
public class Workflow {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;

    @ManyToOne(fetch = LAZY)
    private User createdBy;           // <-- lazy proxy, serialization bomb

    @OneToMany(mappedBy = "workflow")
    private List<Execution> executions; // <-- N+1, huge payload

    @Column(name = "internal_score")
    private double internalScore;      // <-- internal field leaked to client

    private String passwordHash;       // <-- security disaster
    private Instant deletedAt;         // <-- soft-delete detail leaked
}

@GetMapping("/workflows/{id}")
public Workflow get(@PathVariable Long id) {
    return workflowRepository.findById(id).orElseThrow();
    // Returns ALL fields including passwordHash, internalScore
    // Triggers lazy loading -> N+1 or LazyInitializationException
    // Any DB column rename = API breaking change
}
```

### What goes wrong

```text
1. SECURITY:       Internal fields (passwordHash, internalScore) exposed
2. PERFORMANCE:    Lazy-loaded collections cause N+1 or exceptions
3. COUPLING:       DB schema change = API breaking change
4. FLEXIBILITY:    Can't return different shapes for list vs detail
5. VALIDATION:     Entity validation != request validation
6. EVOLUTION:      Can't version the API independently of the DB
7. CIRCULAR REF:   Workflow -> Execution -> Workflow -> infinite JSON loop
```

---

## 2. The Solution: Separate DTOs

```text
         Request DTOs              Entity               Response DTOs
         (what client sends)       (DB model)            (what client gets)

         WorkflowCreateRequest                           WorkflowResponse
         WorkflowUpdateRequest  -> Workflow ->           WorkflowSummaryResponse
         WorkflowPatchRequest                            WorkflowDetailResponse
```

### Core Principle

```text
Entity:   models the DATABASE (persistence concern)
DTO:      models the API CONTRACT (presentation concern)

They evolve independently.
You can change your DB schema without breaking clients.
You can change your API response without touching the DB.
```

---

## 3. Request DTOs

### Create Request

```java
// Only the fields needed to CREATE a workflow
public record WorkflowCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be <= 100 characters")
    String name,

    @Size(max = 500, message = "Description must be <= 500 characters")
    String description,

    @NotNull(message = "Max retries is required")
    @Min(value = 0, message = "Max retries must be >= 0")
    @Max(value = 10, message = "Max retries must be <= 10")
    Integer maxRetries,

    @Min(value = 1) @Max(value = 3600)
    Integer timeoutSeconds
) {}
```

### Update Request (full replace -- PUT)

```java
// ALL fields required -- full replacement
public record WorkflowUpdateRequest(
    @NotBlank String name,
    String description,          // nullable = description can be removed
    @NotNull @Min(0) @Max(10) Integer maxRetries,
    @NotNull @Min(1) @Max(3600) Integer timeoutSeconds
) {}
```

### Patch Request (partial update -- PATCH)

```java
// Uses Optional to distinguish "not sent" from "sent as null"
public record WorkflowPatchRequest(
    Optional<@Size(max = 100) String> name,
    Optional<@Size(max = 500) String> description,
    Optional<@Min(0) @Max(10) Integer> maxRetries,
    Optional<@Min(1) @Max(3600) Integer> timeoutSeconds
) {}
```

### Key design rules for Request DTOs

```text
1. NO id field          -- ID comes from path parameter, not body
2. NO createdAt         -- server sets timestamps
3. NO createdBy         -- server derives from auth context
4. NO version           -- used internally for optimistic locking
5. Validation on DTO    -- @NotBlank, @Min, @Max, @Email, etc.
6. Use records          -- immutable, concise, perfect for DTOs
```

---

## 4. Response DTOs

### Summary Response (for lists)

```java
// Lightweight -- used in GET /workflows (paginated list)
public record WorkflowSummaryResponse(
    Long id,
    String name,
    String status,
    Instant createdAt,
    String createdBy          // just the name, not the full User object
) {}
```

### Detail Response (for single resource)

```java
// Full detail -- used in GET /workflows/{id}
public record WorkflowDetailResponse(
    Long id,
    String name,
    String description,
    String status,
    Integer maxRetries,
    Integer timeoutSeconds,
    Instant createdAt,
    Instant updatedAt,
    String createdBy,
    Long currentVersionId,
    int totalExecutions,       // computed field
    int failedExecutions       // computed field
) {}
```

### Why separate Summary vs Detail?

```text
GET /workflows          -> returns WorkflowSummaryResponse[]
                           List page: 50 items, each ~200 bytes = 10 KB

GET /workflows/{id}     -> returns WorkflowDetailResponse
                           Detail page: 1 item with full info = 1 KB

If you used DetailResponse for the list:
  50 items * 1 KB = 50 KB + N+1 queries for executions count
  Client receives data it doesn't need for a list view
```

---

## 5. Mapping: Entity <-> DTO

### Manual Mapping (recommended for learning)

```java
@Component
public class WorkflowMapper {

    public WorkflowSummaryResponse toSummary(Workflow entity) {
        return new WorkflowSummaryResponse(
            entity.getId(),
            entity.getName(),
            entity.getStatus().name(),
            entity.getCreatedAt(),
            entity.getCreatedBy().getFullName()   // only extract what's needed
        );
    }

    public WorkflowDetailResponse toDetail(Workflow entity) {
        return new WorkflowDetailResponse(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getStatus().name(),
            entity.getMaxRetries(),
            entity.getTimeoutSeconds(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCreatedBy().getFullName(),
            entity.getCurrentVersion().getId(),
            entity.getExecutions().size(),          // or use a COUNT query
            (int) entity.getExecutions().stream()
                .filter(e -> e.getStatus() == FAILED).count()
        );
    }

    public Workflow toEntity(WorkflowCreateRequest request, User currentUser) {
        Workflow workflow = new Workflow();
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setMaxRetries(request.maxRetries());
        workflow.setTimeoutSeconds(request.timeoutSeconds());
        workflow.setCreatedBy(currentUser);
        workflow.setStatus(WorkflowStatus.DRAFT);
        workflow.setCreatedAt(Instant.now());
        return workflow;
    }

    public void updateEntity(Workflow entity, WorkflowUpdateRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setMaxRetries(request.maxRetries());
        entity.setTimeoutSeconds(request.timeoutSeconds());
        entity.setUpdatedAt(Instant.now());
    }

    public void patchEntity(Workflow entity, WorkflowPatchRequest request) {
        request.name().ifPresent(entity::setName);
        request.description().ifPresent(entity::setDescription);
        request.maxRetries().ifPresent(entity::setMaxRetries);
        request.timeoutSeconds().ifPresent(entity::setTimeoutSeconds);
        entity.setUpdatedAt(Instant.now());
    }
}
```

### MapStruct (production, compile-time code generation)

```java
@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    @Mapping(target = "createdBy", source = "createdBy.fullName")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    WorkflowSummaryResponse toSummary(Workflow entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Workflow toEntity(WorkflowCreateRequest request);
}
```

### Comparison of mapping approaches

```text
Approach       Pros                          Cons
------------------------------------------------------------------
Manual         Full control, no magic        Verbose, boilerplate
MapStruct      Compile-time, fast, safe      Learning curve, annotations
ModelMapper    Runtime, less code             Reflection, slower, fragile
BeanUtils      Simple copy                   No type safety, no control
```

**Recommendation:** Start manual (understand the mapping), move to MapStruct when boilerplate becomes painful.

---

## 6. Projections (JPA/Spring Data)

Instead of fetching the full entity and mapping, fetch only what you need from the DB.

### Interface Projection

```java
// Spring Data creates a proxy that returns only these fields
public interface WorkflowSummaryProjection {
    Long getId();
    String getName();
    String getStatus();
    Instant getCreatedAt();
}

// Repository -- SELECT id, name, status, created_at only (no full entity load)
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Page<WorkflowSummaryProjection> findAllProjectedBy(Pageable pageable);
}
```

### Class-Based Projection (using record)

```java
public record WorkflowSummaryProjection(
    Long id,
    String name,
    String status,
    Instant createdAt
) {}

// JPQL constructor expression
@Query("""
    SELECT new com.flowforge.dto.WorkflowSummaryProjection(
        w.id, w.name, w.status, w.createdAt)
    FROM Workflow w
    """)
Page<WorkflowSummaryProjection> findSummaries(Pageable pageable);
```

### When to use Projections

```text
Full Entity + Mapper:
  - Need to call entity methods
  - Need lazy-loaded associations
  - Need to modify and save

Projection:
  - Read-only list views
  - Performance critical (avoids loading unused columns)
  - Large tables with many columns
  - Reporting / dashboards
```

---

## 7. Nested DTOs and Relationships

### Flattening relationships

```java
// Entity has a relationship
@Entity
public class Execution {
    @Id private Long id;
    @ManyToOne private Workflow workflow;   // full object
    private ExecutionStatus status;
}

// Response DTO flattens it
public record ExecutionResponse(
    Long id,
    Long workflowId,         // just the ID, not the full Workflow
    String workflowName,     // just the name
    String status,
    Instant startedAt,
    Instant completedAt
) {}
```

### Nested DTOs (when detail is needed)

```java
public record WorkflowDetailResponse(
    Long id,
    String name,
    List<StepSummaryResponse> steps,       // nested list of step summaries
    ExecutionStatsResponse executionStats   // nested stats object
) {}

public record StepSummaryResponse(
    Long id,
    String name,
    String type,
    int order
) {}

public record ExecutionStatsResponse(
    int total,
    int succeeded,
    int failed,
    int running
) {}
```

### Rule: avoid deep nesting

```text
BAD (3+ levels deep):
{
  "workflow": {
    "versions": [{
      "steps": [{
        "executions": [{
          "attempts": [{ ... }]
        }]
      }]
    }]
  }
}

GOOD (max 2 levels, use links for deeper data):
{
  "id": 1,
  "name": "Payment Flow",
  "steps": [
    { "id": 10, "name": "Validate" },
    { "id": 11, "name": "Process" }
  ],
  "links": {
    "versions": "/api/v1/workflows/1/versions",
    "executions": "/api/v1/workflows/1/executions"
  }
}
```

---

## 8. API Contract Versioning with DTOs

DTOs make versioning easy because entity stays the same:

```java
// V1 response
public record WorkflowResponseV1(
    Long id,
    String name,              // full name in one field
    String status
) {}

// V2 response -- breaking change handled in DTO, not entity
public record WorkflowResponseV2(
    Long id,
    String displayName,       // renamed field
    String shortName,         // new field
    String status,
    Map<String, String> metadata  // new field
) {}

// Same entity serves both
@Component
public class WorkflowMapper {
    public WorkflowResponseV1 toV1(Workflow entity) {
        return new WorkflowResponseV1(entity.getId(), entity.getName(), entity.getStatus().name());
    }

    public WorkflowResponseV2 toV2(Workflow entity) {
        return new WorkflowResponseV2(
            entity.getId(),
            entity.getDisplayName(),
            entity.getShortName(),
            entity.getStatus().name(),
            entity.getMetadata()
        );
    }
}
```

---

## 9. Common DTO Patterns

### Envelope/Wrapper Response

```java
// Standard API response wrapper
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error,
    Map<String, String> metadata
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error, null);
    }
}

// Usage:  { "success": true, "data": { ... }, "error": null }
```

### Page Response (standardized pagination)

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T, E> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
```

---

## 10. Package Structure

```text
com.flowforge
  |
  +-- controller/
  |     WorkflowController.java
  |
  +-- service/
  |     WorkflowService.java
  |
  +-- repository/
  |     WorkflowRepository.java
  |
  +-- entity/
  |     Workflow.java
  |
  +-- dto/
  |   +-- request/
  |   |     WorkflowCreateRequest.java
  |   |     WorkflowUpdateRequest.java
  |   |     WorkflowPatchRequest.java
  |   |
  |   +-- response/
  |   |     WorkflowSummaryResponse.java
  |   |     WorkflowDetailResponse.java
  |   |     PageResponse.java
  |   |     ApiResponse.java
  |   |     ApiError.java
  |   |
  |   +-- projection/
  |         WorkflowSummaryProjection.java
  |
  +-- mapper/
        WorkflowMapper.java
```

---

## 11. FlowForge Application

```text
Workflow CRUD:
  WorkflowCreateRequest  ->  Workflow entity  ->  WorkflowSummaryResponse (list)
  WorkflowUpdateRequest  ->                   ->  WorkflowDetailResponse (detail)
  WorkflowPatchRequest   ->

Execution:
  ExecutionCreateRequest ->  Execution entity  ->  ExecutionResponse
                                               ->  ExecutionSummaryResponse (list)

Step:
  StepCreateRequest      ->  WorkflowStep      ->  StepResponse
```

### Checklist

```text
[x] Request DTOs for every write endpoint (no entity exposure)
[x] Separate Summary vs Detail response DTOs
[x] Validation annotations on request DTOs, not entities
[x] Mapper layer between entity and DTO
[x] Projections for read-heavy list endpoints
[x] Flat relationships (IDs/names, not nested entities)
[x] Records for all DTOs (immutable, clean)
[x] No internal fields in response DTOs
[x] PageResponse wrapper for all paginated endpoints
```
