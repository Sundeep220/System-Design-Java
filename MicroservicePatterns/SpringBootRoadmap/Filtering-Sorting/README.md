# Filtering & Sorting -- Deep Dive

How to let clients filter and sort data in a Spring Boot REST API
using JPA Specifications and the Criteria API.

---

## The Problem

Without filtering and sorting, the only way to get specific data is:

```text
GET /api/v1/workflows               -> returns ALL workflows (maybe 50,000)
GET /api/v1/workflows/some-uuid      -> returns ONE workflow

There's no middle ground. Client can't say:
  "Give me only ACTIVE workflows, sorted by newest first"
```

With filtering and sorting:

```text
GET /api/v1/workflows?status=ACTIVE&sort=createdAt,desc
  -> returns only ACTIVE workflows, newest first

GET /api/v1/workflows?status=DRAFT&minRetries=3&sort=name,asc
  -> returns DRAFT workflows with 3+ retries, alphabetical

GET /api/v1/workflows?createdAfter=2025-01-01T00:00:00Z&sort=maxRetries,desc&sort=name,asc
  -> created after Jan 1st, sorted by retries descending then name ascending
```

---

## Static vs Dynamic Filtering

### Static filtering -- hardcoded query methods

```java
// One method per filter combination
List<Workflow> findByStatus(WorkflowStatus status);
List<Workflow> findByStatusAndMaxRetriesGreaterThanEqual(WorkflowStatus status, int retries);
List<Workflow> findByCreatedAtAfter(Instant after);
List<Workflow> findByStatusAndCreatedAtBetween(WorkflowStatus status, Instant from, Instant to);
```

```text
Problem: combinatorial explosion

If you have 5 filter fields, each optional, that's:
  2^5 = 32 possible combinations

You'd need 32 repository methods. Unmanageable.
Each new filter field DOUBLES the number of methods.
```

### Dynamic filtering -- built at runtime

```java
// ONE method handles ALL filter combinations
Specification<Workflow> spec = (root, query, cb) -> null;  // start empty (match all)

if (status != null) {
    spec = spec.and(hasStatus(status));        // add status filter
}
if (minRetries != null) {
    spec = spec.and(minRetries(minRetries));   // add retries filter
}
if (createdAfter != null) {
    spec = spec.and(createdAfter(createdAfter)); // add date filter
}

repository.findAll(spec, pageable);  // ONE query, handles ANY combination
```

```text
This is what JPA Specifications give you:
  - Build queries dynamically based on which parameters are present
  - Compose filters with .and() / .or()
  - One repository method handles all combinations
  - Type-safe (compile-time checks)
```

---

# JPA Specifications -- How They Work

## The Interface

```java
// This is the entire Specification interface (from Spring Data JPA):
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
```

It's a **functional interface** (one abstract method), so you can write it as a lambda.

## The Three Parameters

Every Specification receives three objects:

### 1. `Root<T>` -- represents the entity (the FROM clause)

```java
root.get("status")       // -> references the "status" column
root.get("maxRetries")   // -> references the "max_retries" column
root.get("createdAt")    // -> references the "created_at" column

// Think of it as: SELECT * FROM workflows w
//                                          ^ this is "root"
// root.get("status") means w.status
```

### 2. `CriteriaQuery<?>` -- represents the overall query

```java
// Rarely used directly in Specifications.
// Useful for:
query.distinct(true);     // SELECT DISTINCT ...
query.orderBy(...)        // ORDER BY ... (usually handled by Sort instead)

// You'll almost never touch this in basic filtering.
```

### 3. `CriteriaBuilder` (cb) -- builds the WHERE conditions

```java
cb.equal(root.get("status"), WorkflowStatus.ACTIVE)
// -> WHERE status = 'ACTIVE'

cb.greaterThanOrEqualTo(root.get("maxRetries"), 3)
// -> WHERE max_retries >= 3

cb.lessThanOrEqualTo(root.get("createdAt"), someInstant)
// -> WHERE created_at <= '2025-08-29T...'

cb.like(cb.lower(root.get("name")), "%payment%")
// -> WHERE LOWER(name) LIKE '%payment%'

cb.between(root.get("maxRetries"), 1, 5)
// -> WHERE max_retries BETWEEN 1 AND 5

cb.isNull(root.get("description"))
// -> WHERE description IS NULL

cb.isNotNull(root.get("description"))
// -> WHERE description IS NOT NULL

cb.in(root.get("status")).value(ACTIVE).value(DRAFT)
// -> WHERE status IN ('ACTIVE', 'DRAFT')
```

## Putting It Together

```java
// "Give me all workflows with status = ACTIVE"
Specification<Workflow> spec = (root, query, cb) ->
    cb.equal(root.get("status"), WorkflowStatus.ACTIVE);

// In plain SQL this becomes:
// SELECT * FROM workflows WHERE status = 'ACTIVE'
```

---

## FlowForge Implementation -- Step by Step

### 1. Repository: Enable Specifications

```java
// Just add JpaSpecificationExecutor to your repository.
// That's it. No other changes needed.

public interface WorkflowRepository extends JpaRepository<Workflow, UUID>,
        JpaSpecificationExecutor<Workflow> {
    // findAll(Specification, Pageable) is now available automatically
}
```

```text
What JpaSpecificationExecutor adds:

  Optional<T> findOne(Specification<T> spec);
  List<T>     findAll(Specification<T> spec);
  Page<T>     findAll(Specification<T> spec, Pageable pageable);   <-- this is the one
  List<T>     findAll(Specification<T> spec, Sort sort);
  long        count(Specification<T> spec);
  boolean     exists(Specification<T> spec);
```

### 2. Specification Class: Reusable Filter Builders

```java
public final class WorkflowSpecification {

    private WorkflowSpecification() {}   // utility class, no instances

    // Each method returns a Specification (a lambda)
    // They don't execute anything -- they describe a WHERE condition

    public static Specification<Workflow> hasStatus(WorkflowStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
        // -> WHERE status = ?
    }

    public static Specification<Workflow> minRetries(int minRetries) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxRetries"), minRetries);
        // -> WHERE max_retries >= ?
    }

    public static Specification<Workflow> maxRetries(int maxRetries) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("maxRetries"), maxRetries);
        // -> WHERE max_retries <= ?
    }

    public static Specification<Workflow> createdAfter(Instant after) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), after);
        // -> WHERE created_at >= ?
    }

    public static Specification<Workflow> createdBefore(Instant before) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), before);
        // -> WHERE created_at <= ?
    }

    public static Specification<Workflow> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        // -> WHERE LOWER(name) LIKE '%payment%'
    }
}
```

```text
KEY INSIGHT: These methods don't run queries.
They return Specification objects (lambdas) that DESCRIBE a condition.
The actual SQL is only generated when you call repository.findAll(spec).
This is why you can compose them freely with .and() / .or().
```

### 3. Service: Compose Specifications Dynamically

```java
public Page<Workflow> findAll(WorkflowFilterRequest filter, Pageable pageable) {
    sortValidator.validate(pageable.getSort());

    // Start with an empty spec (matches everything)
    // In Spring Boot 4.x, Specification.where(null) throws IllegalArgumentException
    // Use a lambda returning null predicate instead (JPA treats null predicate as "no condition")
    Specification<Workflow> spec = (root, query, cb) -> null;

    // Conditionally chain filters -- only if the parameter is present
    if (filter.status() != null) {
        spec = spec.and(WorkflowSpecification.hasStatus(filter.status()));
    }
    if (filter.minRetries() != null) {
        spec = spec.and(WorkflowSpecification.minRetries(filter.minRetries()));
    }
    if (filter.maxRetries() != null) {
        spec = spec.and(WorkflowSpecification.maxRetries(filter.maxRetries()));
    }
    if (filter.createdAfter() != null) {
        spec = spec.and(WorkflowSpecification.createdAfter(filter.createdAfter()));
    }
    if (filter.createdBefore() != null) {
        spec = spec.and(WorkflowSpecification.createdBefore(filter.createdBefore()));
    }

    return workflowRepository.findAll(spec, pageable);
}
```

### How Specification composition works

```text
Example: GET /api/v1/workflows?status=ACTIVE&minRetries=3&sort=createdAt,desc

Step 1: spec = (root, query, cb) -> null
        -> WHERE true  (matches everything)

Step 2: spec = spec.and(hasStatus(ACTIVE))
        -> WHERE true AND status = 'ACTIVE'
        -> WHERE status = 'ACTIVE'

Step 3: spec = spec.and(minRetries(3))
        -> WHERE status = 'ACTIVE' AND max_retries >= 3

Final SQL generated:
  SELECT w.* FROM workflows w
  WHERE w.status = 'ACTIVE'
    AND w.max_retries >= 3
  ORDER BY w.created_at DESC
  LIMIT 20 OFFSET 0

  + COUNT query for pagination totals:
  SELECT COUNT(*) FROM workflows w
  WHERE w.status = 'ACTIVE'
    AND w.max_retries >= 3
```

```text
Example: GET /api/v1/workflows?page=0&size=10  (NO filters)

Step 1: spec = (root, query, cb) -> null
        -> WHERE true  (matches everything)

No if-blocks execute (all filter params are null).

Final SQL:
  SELECT w.* FROM workflows w
  ORDER BY ...
  LIMIT 10 OFFSET 0

  The same code handles "no filters" and "many filters" gracefully.
```

### 4. Controller: Accept Filter Query Parameters

```java
@GetMapping
public ResponseEntity<PageResponse<WorkflowSummaryResponse>> list(
        Pageable pageable,
        @RequestParam(required = false) WorkflowStatus status,
        @RequestParam(required = false) Integer minRetries,
        @RequestParam(required = false) Integer maxRetries,
        @RequestParam(required = false) Instant createdAfter,
        @RequestParam(required = false) Instant createdBefore) {

    WorkflowFilterRequest filter = new WorkflowFilterRequest(
            status, minRetries, maxRetries, createdAfter, createdBefore);

    Page<WorkflowSummaryResponse> page = workflowService.findAll(filter, pageable)
            .map(workflowMapper::toSummary);
    return ResponseEntity.ok(workflowMapper.toPageResponse(page));
}
```

```text
How Spring resolves these parameters:

  GET /api/v1/workflows?status=ACTIVE&minRetries=3&page=0&size=20&sort=name,asc

  Spring MVC automatically:
  1. Resolves Pageable from page=0, size=20, sort=name,asc
  2. Converts "ACTIVE" string to WorkflowStatus.ACTIVE enum
  3. Converts "3" string to Integer 3
  4. Leaves missing params (maxRetries, createdAfter, createdBefore) as null

  @RequestParam(required = false) means the param is OPTIONAL.
  If not provided, the value is null.
```

### 5. Filter DTO: Clean Parameter Passing

```java
public record WorkflowFilterRequest(
        WorkflowStatus status,
        Integer minRetries,      // Integer not int -- null means "not specified"
        Integer maxRetries,
        Instant createdAfter,
        Instant createdBefore
) {}
```

```text
Why use a record DTO instead of passing 5 separate params to the service?

Without DTO:
  service.findAll(status, minRetries, maxRetries, createdAfter, createdBefore, pageable)
  -> 6 parameters, hard to read, easy to mix up order

With DTO:
  service.findAll(filter, pageable)
  -> 2 parameters, clean, extensible (add new filters without changing method signature)
```

---

# Sorting -- Deep Dive

## How Spring Data Sorting Works

### Client sends sort in the URL

```text
?sort=createdAt,desc              -> single sort, descending
?sort=name,asc                    -> single sort, ascending
?sort=name                        -> single sort, ascending (default direction)
?sort=createdAt,desc&sort=name,asc -> multi-column sort
```

### Spring auto-resolves to Sort object

```java
// Spring MVC sees ?sort=createdAt,desc&sort=name,asc and creates:
Pageable pageable = PageRequest.of(
    0,              // page
    20,             // size
    Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("name")
    )
);
```

### JPA translates to ORDER BY

```sql
SELECT * FROM workflows
ORDER BY created_at DESC, name ASC
LIMIT 20 OFFSET 0
```

## Multi-Column Sorting Explained

```text
Why would you sort by multiple columns?

Data:
  name          created_at
  ----          ----------
  Payment       2025-08-27
  Order         2025-08-28
  Payment       2025-08-29
  Billing       2025-08-28

Single sort: ?sort=name,asc

  Billing       2025-08-28
  Order         2025-08-28
  Payment       2025-08-27   <-- which Payment comes first?
  Payment       2025-08-29   <-- database decides (non-deterministic!)

Multi sort: ?sort=name,asc&sort=createdAt,desc

  Billing       2025-08-28
  Order         2025-08-28
  Payment       2025-08-29   <-- newest Payment first (deterministic!)
  Payment       2025-08-27

The second sort column breaks ties in the first.
This makes the ordering DETERMINISTIC and REPEATABLE.
Without a tiebreaker, the same query can return different row orders each time.
```

## The Sort Whitelist Problem

### Why whitelist sort fields?

```text
Without a whitelist, the client can sort by ANY column:

  ?sort=password,asc              -> might leak data ordering info
  ?sort=some_internal_column,asc  -> error (column doesn't exist in entity)
  ?sort=steps.config,asc          -> might navigate relationships unexpectedly

Worse: arbitrary sort fields can bypass indexes and cause full table scans.

  ?sort=LOWER(name),asc           -> SQL injection? No (Spring parameterizes)
                                     but still causes expensive unindexed sort.
```

### SortValidator: whitelist implementation

```java
@Component
public class SortValidator {

    // ONLY these fields can be used in ?sort=
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "status",
            "maxRetries",
            "timeoutSeconds",
            "createdAt",
            "updatedAt"
    );

    public void validate(Sort sort) {
        if (sort.isUnsorted()) {
            return;   // no sort requested, that's fine
        }
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortFieldException(order.getProperty(), ALLOWED_SORT_FIELDS);
            }
        }
    }
}
```

```text
What happens when a client sends ?sort=password,asc:

  1. Spring parses it into Sort.Order("password", ASC)
  2. SortValidator.validate() iterates the orders
  3. "password" is NOT in ALLOWED_SORT_FIELDS
  4. Throws InvalidSortFieldException (extends FlowForgeException)
  5. GlobalExceptionHandler catches it, returns 400:

  {
    "status": 400,
    "error": "Bad Request",
    "errorCode": "INVALID_SORT_FIELD",
    "message": "Invalid sort field: 'password'. Allowed fields: [name, status, ...]"
  }
```

---

# The Criteria API -- Under the Hood

JPA Specifications are built on the **JPA Criteria API**. Here's the mapping:

## SQL to Criteria API mapping

```text
SQL                                     Criteria API
----------------------------------------------------------------------
SELECT * FROM workflows                 root (Root<Workflow>)
WHERE status = 'ACTIVE'                 cb.equal(root.get("status"), ACTIVE)
  AND max_retries >= 3                  cb.greaterThanOrEqualTo(root.get("maxRetries"), 3)
  AND created_at >= '2025-01-01'        cb.greaterThanOrEqualTo(root.get("createdAt"), instant)
  AND LOWER(name) LIKE '%pay%'          cb.like(cb.lower(root.get("name")), "%pay%")
ORDER BY created_at DESC                Sort.by(Sort.Order.desc("createdAt"))
LIMIT 20 OFFSET 40                     PageRequest.of(2, 20)
```

## Common CriteriaBuilder Methods

```text
Method                                  SQL Equivalent
----------------------------------------------------------------------
cb.equal(x, value)                      x = value
cb.notEqual(x, value)                   x != value
cb.greaterThan(x, value)                x > value
cb.greaterThanOrEqualTo(x, value)       x >= value
cb.lessThan(x, value)                   x < value
cb.lessThanOrEqualTo(x, value)          x <= value
cb.between(x, low, high)               x BETWEEN low AND high
cb.like(x, pattern)                     x LIKE pattern
cb.isNull(x)                            x IS NULL
cb.isNotNull(x)                         x IS NOT NULL
cb.in(x).value(a).value(b)             x IN (a, b)
cb.and(pred1, pred2)                    pred1 AND pred2
cb.or(pred1, pred2)                     pred1 OR pred2
cb.not(pred)                            NOT pred
cb.lower(x)                             LOWER(x)
cb.upper(x)                             UPPER(x)
cb.count(x)                             COUNT(x)
cb.sum(x)                               SUM(x)
```

## root.get() -- Accessing Entity Fields

```text
root.get("name")         -> workflows.name         (String)
root.get("status")       -> workflows.status        (WorkflowStatus)
root.get("maxRetries")   -> workflows.max_retries   (int)
root.get("createdAt")    -> workflows.created_at    (Instant)

NOTE: You use the JAVA FIELD NAME, not the database column name.
  root.get("maxRetries")   CORRECT (Java field)
  root.get("max_retries")  WRONG (database column)

Hibernate translates the Java field name to the column name using the
@Column(name = "max_retries") annotation on the entity.
```

---

# Specification Composition: .and() / .or()

## AND composition (all conditions must match)

```java
// status = ACTIVE AND max_retries >= 3
Specification<Workflow> spec = Specification
    .where(WorkflowSpecification.hasStatus(ACTIVE))
    .and(WorkflowSpecification.minRetries(3));

// SQL: WHERE status = 'ACTIVE' AND max_retries >= 3
```

## OR composition (any condition can match)

```java
// status = ACTIVE OR status = DRAFT
Specification<Workflow> spec = Specification
    .where(WorkflowSpecification.hasStatus(ACTIVE))
    .or(WorkflowSpecification.hasStatus(DRAFT));

// SQL: WHERE status = 'ACTIVE' OR status = 'DRAFT'
```

## Complex composition

```java
// (status = ACTIVE OR status = DRAFT) AND max_retries >= 3
Specification<Workflow> statusSpec = Specification
    .where(WorkflowSpecification.hasStatus(ACTIVE))
    .or(WorkflowSpecification.hasStatus(DRAFT));

Specification<Workflow> fullSpec = statusSpec
    .and(WorkflowSpecification.minRetries(3));

// SQL: WHERE (status = 'ACTIVE' OR status = 'DRAFT') AND max_retries >= 3
```

## Negation

```java
Specification<Workflow> notDraft = Specification
    .not(WorkflowSpecification.hasStatus(DRAFT));

// SQL: WHERE NOT (status = 'DRAFT')
```

---

# Database Indexes for Filtering & Sorting

## Why indexes matter

```text
Without index:

  SELECT * FROM workflows WHERE status = 'ACTIVE'
  -> Full table scan: reads EVERY row, checks if status = 'ACTIVE'
  -> 1,000,000 rows = 1,000,000 comparisons
  -> Slow (seconds)

With index on status:

  SELECT * FROM workflows WHERE status = 'ACTIVE'
  -> Index lookup: jumps directly to 'ACTIVE' entries
  -> 50,000 ACTIVE rows = 50,000 reads (skips the other 950,000)
  -> Fast (milliseconds)
```

## Index recommendations for our filters

```sql
-- Single-column indexes for individual filters
CREATE INDEX idx_workflows_status ON workflows (status);
CREATE INDEX idx_workflows_created_at ON workflows (created_at);
CREATE INDEX idx_workflows_max_retries ON workflows (max_retries);

-- Composite index for common filter + sort combination
CREATE INDEX idx_workflows_status_created_at ON workflows (status, created_at DESC);

-- This composite index accelerates:
--   WHERE status = 'ACTIVE' ORDER BY created_at DESC
-- Because the DB can use ONE index for both filter AND sort.
```

## When indexes hurt

```text
Indexes speed up reads but slow down writes.

Every INSERT/UPDATE/DELETE must also update the index.
  - 0-2 indexes:  negligible overhead
  - 3-5 indexes:  slight overhead
  - 10+ indexes:  significant write performance impact

Rule of thumb:
  - Index columns that appear in WHERE clauses
  - Index columns that appear in ORDER BY clauses
  - Don't index columns that are rarely filtered/sorted
  - Don't index columns with very low cardinality
    (e.g., a boolean "active" column with only true/false
     -- index doesn't help much if 50% of rows match)
```

---

# Alternatives to Specifications

## 1. Spring Data Query Methods (static)

```java
Page<Workflow> findByStatus(WorkflowStatus status, Pageable pageable);
Page<Workflow> findByStatusAndMaxRetriesGreaterThanEqual(
    WorkflowStatus status, int retries, Pageable pageable);
```

```text
+ Simple, readable, no boilerplate
+ Spring generates SQL from method name
- Combinatorial explosion with multiple optional filters
- Method names become absurdly long
- Not dynamic -- each combination needs its own method
```

## 2. @Query with JPQL (semi-dynamic)

```java
@Query("""
    SELECT w FROM Workflow w
    WHERE (:status IS NULL OR w.status = :status)
      AND (:minRetries IS NULL OR w.maxRetries >= :minRetries)
    """)
Page<Workflow> findFiltered(
    @Param("status") WorkflowStatus status,
    @Param("minRetries") Integer minRetries,
    Pageable pageable);
```

```text
+ One query handles multiple optional filters
+ Readable SQL
- "IS NULL OR" trick can confuse the query optimizer
- Performance can suffer (optimizer may not use indexes well)
- Harder to compose and extend
- All filters must be defined upfront in the query
```

## 3. JPA Specifications (dynamic) -- what we use

```java
Specification<Workflow> spec = (root, query, cb) -> null;
if (status != null) spec = spec.and(hasStatus(status));
if (minRetries != null) spec = spec.and(minRetries(minRetries));
repository.findAll(spec, pageable);
```

```text
+ Fully dynamic -- any combination of filters
+ Composable with .and() / .or()
+ Each filter is a reusable building block
+ Clean separation of concerns
- More boilerplate than query methods
- Lambda syntax can be confusing at first
- Uses string field names (typo = runtime error, not compile error)
```

## 4. QueryDSL (type-safe dynamic)

```java
QWorkflow w = QWorkflow.workflow;
BooleanBuilder builder = new BooleanBuilder();
if (status != null) builder.and(w.status.eq(status));
if (minRetries != null) builder.and(w.maxRetries.goe(minRetries));
repository.findAll(builder, pageable);
```

```text
+ Fully type-safe (w.status instead of root.get("status"))
+ Compile-time error if field doesn't exist
+ Generates Q-classes from your entities
- Extra dependency and annotation processing setup
- Q-classes must be regenerated when entities change
- Less common in the ecosystem
```

## When to use which

```text
Situation                              Best Approach
----------------------------------------------------------------------
1-2 fixed filters, never changes       Query Methods
Complex but fixed query                @Query JPQL
Multiple optional filters              JPA Specifications  <-- our case
Need compile-time type safety          QueryDSL
Very complex dynamic queries           Native SQL + custom repo
```

---

# Full Request Flow

```text
Client request:
  GET /api/v1/workflows?status=ACTIVE&minRetries=3&page=0&size=20&sort=createdAt,desc

1. DispatcherServlet routes to WorkflowController.list()

2. Spring MVC resolves parameters:
     Pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")))
     status = WorkflowStatus.ACTIVE
     minRetries = 3
     maxRetries = null
     createdAfter = null
     createdBefore = null

3. Controller creates WorkflowFilterRequest(ACTIVE, 3, null, null, null)

4. Service calls sortValidator.validate(sort)
     -> "createdAt" is in whitelist -> OK

5. Service builds Specification:
     spec = where(null)
           .and(hasStatus(ACTIVE))        // status present
           .and(minRetries(3))            // minRetries present
                                          // maxRetries null -> skip
                                          // createdAfter null -> skip
                                          // createdBefore null -> skip

6. Service calls repository.findAll(spec, pageable)

7. Spring Data JPA translates to SQL:
     SELECT w.* FROM workflows w
     WHERE w.status = 'ACTIVE'
       AND w.max_retries >= 3
     ORDER BY w.created_at DESC
     LIMIT 20 OFFSET 0

     SELECT COUNT(*) FROM workflows w
     WHERE w.status = 'ACTIVE'
       AND w.max_retries >= 3

8. Results mapped through workflowMapper::toSummary

9. Wrapped in PageResponse and returned as JSON:
     {
       "content": [...],
       "page": 0,
       "size": 20,
       "totalElements": 12,
       "totalPages": 1,
       "first": true,
       "last": true
     }
```

---

# Curl Examples

```bash
# No filters -- returns all workflows
curl "http://localhost:8080/api/v1/workflows?page=0&size=20"

# Filter by status
curl "http://localhost:8080/api/v1/workflows?status=ACTIVE"

# Filter by status + minimum retries
curl "http://localhost:8080/api/v1/workflows?status=ACTIVE&minRetries=3"

# Filter by date range
curl "http://localhost:8080/api/v1/workflows?createdAfter=2025-01-01T00:00:00Z&createdBefore=2025-12-31T23:59:59Z"

# Sort by name ascending
curl "http://localhost:8080/api/v1/workflows?sort=name,asc"

# Sort by multiple columns
curl "http://localhost:8080/api/v1/workflows?sort=status,asc&sort=createdAt,desc"

# Everything combined: filter + sort + pagination
curl "http://localhost:8080/api/v1/workflows?status=ACTIVE&minRetries=2&sort=createdAt,desc&page=0&size=10"

# Invalid sort field -- returns 400
curl "http://localhost:8080/api/v1/workflows?sort=password,asc"
# -> {"errorCode": "INVALID_SORT_FIELD", "message": "Invalid sort field: 'password'. ..."}
```

---

# Spring Boot 4.x Breaking Change: Specification.where(null)

## The problem

In Spring Boot 3.x / Spring Data 3.x, this was valid:

```java
Specification<Workflow> spec = Specification.where(null);
repository.findAll(spec, pageable);  // works fine
```

In **Spring Boot 4.x / Spring Data 4.x**, this throws:

```text
java.lang.IllegalArgumentException: Specification must not be null
```

Spring Data 4.x added null-safety checks. Passing `null` as a `Specification`
(or wrapping `null` with `Specification.where(null)`) is now rejected at runtime.

## The fix

Instead of `Specification.where(null)`, use a lambda that returns a **null predicate**:

```java
// BROKEN in Spring Boot 4.x:
Specification<Workflow> spec = Specification.where(null);

// ALSO BROKEN -- casting doesn't help:
Specification<Workflow> spec = Specification.where((Specification<Workflow>) null);

// WORKS -- a Specification that returns null predicate means "no condition":
Specification<Workflow> spec = (root, query, cb) -> null;
```

```text
Why does returning null from the lambda work?

  A Specification object that EXISTS but returns a null Predicate is valid.
  JPA interprets a null Predicate as "no WHERE condition" (match all rows).

  The difference:
  - Specification.where(null)     -> null Specification OBJECT  -> rejected
  - (root, query, cb) -> null     -> valid Specification OBJECT that returns null Predicate -> accepted

  It's the difference between:
  - "I have no filter"            -> Spring says "give me a filter!"
  - "I have a filter that matches everything" -> Spring says "OK, here are all rows"
```

## How .and() works with null predicates

```java
Specification<Workflow> spec = (root, query, cb) -> null;  // matches everything

spec = spec.and(WorkflowSpecification.hasStatus(ACTIVE));
// Internally: cb.and(null, statusPredicate)
// JPA treats null AND X as just X
// Result: WHERE status = 'ACTIVE'

spec = spec.and(WorkflowSpecification.minRetries(3));
// Result: WHERE status = 'ACTIVE' AND max_retries >= 3
```

```text
The null predicate acts as an identity element for AND/OR composition:
  null AND X  =  X
  null OR  X  =  X

This is by JPA Criteria API design -- it gracefully ignores null predicates.
```

---

# Summary

```text
1. FILTERING
   - Use JPA Specifications for dynamic, composable filters
   - Each filter is a static method returning Specification<T>
   - Compose with .and() / .or() based on which params are present
   - Start with (root, query, cb) -> null as the empty spec (Spring Boot 4.x)
   - The Criteria API (root, query, cb) builds WHERE conditions

2. SORTING
   - Spring auto-resolves ?sort=field,direction from URL
   - Multi-column: ?sort=a,desc&sort=b,asc
   - ALWAYS whitelist allowed sort fields (security + performance)
   - Reject unknown fields with a clear error

3. KEY CLASSES
   - Specification<T>        -- describes one WHERE condition
   - JpaSpecificationExecutor<T> -- adds findAll(spec, pageable) to repo
   - CriteriaBuilder         -- builds predicates (equal, like, between...)
   - Root<T>                 -- references entity fields
   - Sort / Sort.Order       -- represents ORDER BY
   - Pageable / PageRequest  -- combines page + size + sort

4. INDEXES
   - Index columns used in WHERE and ORDER BY
   - Composite indexes for filter+sort combos
   - Don't over-index (writes get slower)
```
