# Pagination -- Deep Dive

Pagination is the technique of splitting a large dataset into smaller chunks (pages)
and returning only one chunk per request instead of the entire collection.

---

## Why Paginate?

```text
Without pagination:
  GET /api/v1/workflows
  -> Returns 500,000 rows
  -> Serializes all to JSON
  -> 200MB response
  -> Client crashes / times out
  -> DB holds connection for 30s
  -> Other requests queue up

With pagination:
  GET /api/v1/workflows?page=0&size=20
  -> Returns 20 rows
  -> 2KB response
  -> 5ms
  -> Client renders instantly
```

Every production API that returns a list **must** paginate. No exceptions.

---

## The Five Pagination Strategies

```text
Strategy            How It Works                  Best For
--------------------------------------------------------------------
1. Offset           OFFSET + LIMIT                Simple admin UIs
2. Page-number      page=2&size=20 (maps to       Most REST APIs
                    offset internally)
3. Cursor           cursor=abc123&limit=20         Feeds, infinite scroll
4. Keyset           WHERE id > 5000 LIMIT 20       High-performance APIs
5. Seek             Variant of keyset with         Time-series data
                    compound keys
```

---

# 1. Offset Pagination

The simplest approach. You tell the database: "skip N rows, give me M rows."

## How it works

```text
Client request:    GET /api/v1/workflows?page=2&size=20

Calculation:
  page = 2
  size = 20
  OFFSET = page * size = 2 * 20 = 40

SQL generated:
  SELECT * FROM workflows
  ORDER BY created_at DESC
  LIMIT 20 OFFSET 40

  -> Skip first 40 rows, return next 20
```

## What the database actually does

```text
Page 0:  rows  1-20    OFFSET 0   LIMIT 20   (scans 20 rows)
Page 1:  rows 21-40    OFFSET 20  LIMIT 20   (scans 40 rows, discards 20)
Page 2:  rows 41-60    OFFSET 40  LIMIT 20   (scans 60 rows, discards 40)
Page 99: rows 1981-2000 OFFSET 1980 LIMIT 20 (scans 2000 rows, discards 1980)

The database reads ALL rows up to OFFSET + LIMIT, then throws away the first OFFSET rows.
```

## Spring Data Implementation (What we built in FlowForge)

### Controller

```java
@GetMapping
public ResponseEntity<PageResponse<WorkflowSummaryResponse>> list(Pageable pageable) {
    Page<WorkflowSummaryResponse> page = workflowService.findAll(pageable)
            .map(workflowMapper::toSummary);
    return ResponseEntity.ok(workflowMapper.toPageResponse(page));
}

// Spring auto-resolves Pageable from query params:
//   GET /api/v1/workflows?page=0&size=20&sort=createdAt,desc
//
// pageable.getPageNumber() -> 0
// pageable.getPageSize()   -> 20
// pageable.getSort()       -> Sort by createdAt DESC
```

### Pageable query parameters (auto-resolved by Spring)

```text
Parameter   Type     Default   Description
─────────   ──────   ───────   ────────────────────────────────────────────
page        int      0         Zero-based page index (0 = first page)
size        int      20        Number of items per page
sort        String   (none)    Sort column + direction, repeatable for composite

Examples:
  (no params)                              → page=0, size=20, Sort.unsorted()
  ?page=2                                  → page=2, size=20, Sort.unsorted()
  ?size=5                                  → page=0, size=5,  Sort.unsorted()
  ?page=1&size=10                          → page=1, size=10, Sort.unsorted()
  ?sort=name,asc                           → page=0, size=20, ORDER BY name ASC
  ?sort=name,desc                          → page=0, size=20, ORDER BY name DESC
  ?sort=status,asc&sort=createdAt,desc     → ORDER BY status ASC, created_at DESC
  ?page=0&size=5&sort=name,asc             → all three combined
```

### Configuring defaults and limits (application.yaml)

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20     # size when client omits ?size=
        max-page-size: 100        # cap — ?size=9999 becomes 100
        one-indexed-parameters: false  # false = page starts at 0 (default)
                                       # true  = page starts at 1
        page-parameter: page      # query param name for page number
        size-parameter: size      # query param name for page size
      sort:
        sort-parameter: sort      # query param name for sorting
```

```text
What each config does:

default-page-size: 20
  → GET /api/v1/workflows           → size = 20
  → GET /api/v1/workflows?size=5    → size = 5

max-page-size: 100
  → GET /api/v1/workflows?size=50   → size = 50 (within limit)
  → GET /api/v1/workflows?size=999  → size = 100 (clamped silently)

one-indexed-parameters: false (default)
  → page=0 = first page
  → page=1 = second page

one-indexed-parameters: true
  → page=1 = first page (more natural for non-developers)
  → page=2 = second page
```

### Service

```java
@Service
@Transactional(readOnly = true)
public class WorkflowService {

    public Page<Workflow> findAll(Pageable pageable) {
        return workflowRepository.findAll(pageable);
    }
}
```

### Repository

```java
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    // findAll(Pageable) is inherited from JpaRepository
    // Spring Data generates TWO queries:
    //   1. SELECT * FROM workflows ORDER BY ... LIMIT ? OFFSET ?
    //   2. SELECT COUNT(*) FROM workflows   <-- for totalElements
}
```

### PageResponse DTO

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
```

### Mapper

```java
public <T> PageResponse<T> toPageResponse(Page<T> page) {
    return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
    );
}
```

### JSON Response

```json
{
  "content": [
    { "id": "...", "name": "Payment Flow", "status": "ACTIVE", ... },
    { "id": "...", "name": "Order Flow", "status": "DRAFT", ... }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 143,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### Curl Examples

```bash
# Page 0 (first page), 20 items
curl "http://localhost:8080/api/v1/workflows?page=0&size=20"

# Page 3, 10 items per page, sorted by name ascending
curl "http://localhost:8080/api/v1/workflows?page=3&size=10&sort=name,asc"

# Page 0, 5 items, sorted by createdAt descending (newest first)
curl "http://localhost:8080/api/v1/workflows?page=0&size=5&sort=createdAt,desc"
```

## Advantages

```text
+ Simple to implement
+ Client can jump to any page (page=50)
+ Easy to show "Page 3 of 8" UI
+ totalElements and totalPages available
+ Works with any ORM / framework
+ Users can bookmark specific pages
```

## Disadvantages

```text
- Deep page performance: OFFSET 100000 scans 100,000 rows then discards them
- COUNT(*) query is expensive on large tables (runs a full table scan)
- Inconsistent results when data changes between page requests:

  Time 1: User fetches page 1 (rows 1-20)
  Time 2: Someone inserts a new row at position 5
  Time 3: User fetches page 2 (rows 21-40)
  -> Row 20 from page 1 appears AGAIN on page 2 (it shifted to position 21)
  -> User sees a duplicate

  Or the reverse: a row gets deleted and the user MISSES a row entirely.

- Two SQL queries per request (data + count)
```

## The Deep Page Problem -- Visualized

```text
Table has 10,000,000 rows.

Page 0:     OFFSET 0        -> scans 20 rows       -> fast (5ms)
Page 10:    OFFSET 200      -> scans 220 rows      -> fast (6ms)
Page 100:   OFFSET 2000     -> scans 2020 rows     -> okay (15ms)
Page 1000:  OFFSET 20000    -> scans 20020 rows    -> slow (80ms)
Page 10000: OFFSET 200000   -> scans 200020 rows   -> very slow (800ms)
Page 50000: OFFSET 1000000  -> scans 1000020 rows  -> unacceptable (5s+)

The database does NOT skip rows. It reads them all and discards.
Even with an index, it must walk the index tree to the OFFSET position.
```

## When to Use Offset Pagination

```text
USE when:
  - Dataset is small to medium (< 100K rows)
  - Users need to jump to arbitrary pages
  - You need to show "Page X of Y"
  - Admin dashboards, backoffice tools
  - The data doesn't change frequently

AVOID when:
  - Dataset is very large (> 1M rows)
  - Users will paginate deeply (page > 1000)
  - Data changes frequently between requests (feeds, live data)
  - Mobile infinite scroll UIs
```

---

# 2. Page-Number Pagination

This is actually the same as offset pagination -- just a different way of expressing it.

```text
?page=3&size=20    ->    OFFSET = 3 * 20 = 60, LIMIT = 20

Page-number pagination IS offset pagination.
Spring Data's Pageable uses page-number style (page=0, page=1, page=2...).
```

The only difference is the API contract:
- Some APIs use `?offset=60&limit=20` (raw offset)
- Some APIs use `?page=3&size=20` (page number -- Spring's default)

They generate the same SQL. Same advantages. Same disadvantages.

---

# 3. Cursor Pagination

Instead of "skip N rows," you say: "give me rows AFTER this marker."

## How it works

```text
First request (no cursor):
  GET /api/v1/workflows?limit=20

  -> Returns 20 rows
  -> Response includes a cursor pointing to the LAST item returned

Second request (with cursor):
  GET /api/v1/workflows?limit=20&cursor=eyJpZCI6IjU1NWUyIn0=

  -> Decodes cursor: {"id": "555e2..."}
  -> SQL: WHERE id > '555e2...' ORDER BY id LIMIT 20
  -> Returns next 20 rows
  -> New cursor in response

Third request:
  GET /api/v1/workflows?limit=20&cursor=eyJpZCI6Ijc3N2U0In0=
  -> And so on...
```

## The cursor is an opaque token

```text
What the client sees:     cursor=eyJpZCI6IjU1NWUyIn0=
What it actually is:      Base64 encoded {"id": "555e2..."}

The client should NEVER parse or construct cursors.
The server creates them, the client just passes them back.
This lets you change the cursor format without breaking clients.
```

## Spring Data Implementation

### CursorPageResponse DTO

```java
public record CursorPageResponse<T>(
        List<T> content,
        int size,
        boolean hasNext,
        String nextCursor     // null if no more pages
) {}
```

### Repository (custom query)

```java
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    // For cursor pagination: get rows after the cursor ID
    @Query("""
        SELECT w FROM Workflow w
        WHERE w.id > :cursorId
        ORDER BY w.id ASC
        """)
    List<Workflow> findAllAfterId(@Param("cursorId") UUID cursorId, Pageable pageable);

    // For the first page (no cursor yet)
    @Query("SELECT w FROM Workflow w ORDER BY w.id ASC")
    List<Workflow> findAllOrderById(Pageable pageable);
}
```

### Service

```java
@Service
@Transactional(readOnly = true)
public class WorkflowService {

    public CursorPageResponse<WorkflowSummaryResponse> findAllWithCursor(
            String cursor, int limit) {

        // Request limit+1 to check if there's a next page
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<Workflow> workflows;
        if (cursor == null) {
            workflows = workflowRepository.findAllOrderById(pageable);
        } else {
            UUID cursorId = decodeCursor(cursor);
            workflows = workflowRepository.findAllAfterId(cursorId, pageable);
        }

        boolean hasNext = workflows.size() > limit;
        if (hasNext) {
            workflows = workflows.subList(0, limit);  // trim the extra row
        }

        List<WorkflowSummaryResponse> content = workflows.stream()
                .map(workflowMapper::toSummary)
                .toList();

        String nextCursor = hasNext
                ? encodeCursor(workflows.get(workflows.size() - 1).getId())
                : null;

        return new CursorPageResponse<>(content, limit, hasNext, nextCursor);
    }

    private String encodeCursor(UUID id) {
        return Base64.getUrlEncoder()
                .encodeToString(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    private UUID decodeCursor(String cursor) {
        String decoded = new String(
                Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        return UUID.fromString(decoded);
    }
}
```

### Controller

```java
@GetMapping("/cursor")
public ResponseEntity<CursorPageResponse<WorkflowSummaryResponse>> listWithCursor(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(workflowService.findAllWithCursor(cursor, limit));
}
```

### JSON Response

```json
{
  "content": [
    { "id": "111e2...", "name": "Payment Flow", ... },
    { "id": "222e3...", "name": "Order Flow", ... },
    { "id": "333e4...", "name": "Notification Flow", ... }
  ],
  "size": 20,
  "hasNext": true,
  "nextCursor": "eyJpZCI6IjMzM2U0Li4uIn0="
}
```

### The "limit + 1" trick

```text
Why do we request limit + 1 rows?

If client asks for 20 items:
  - We query for 21
  - If we get 21 back -> there IS a next page (hasNext = true)
  - We return only 20 to the client, trim the 21st
  - If we get 20 or fewer -> no next page (hasNext = false)

This avoids a separate COUNT(*) query entirely!
```

## SQL Comparison

```sql
-- Offset: page 500 of a 10M row table
SELECT * FROM workflows
ORDER BY id
LIMIT 20 OFFSET 10000;
-- Scans 10020 rows, discards 10000

-- Cursor: same position
SELECT * FROM workflows
WHERE id > '555e2...'
ORDER BY id
LIMIT 21;
-- Index seek directly to '555e2...', reads only 21 rows
-- O(log n) index lookup, not O(n) scan
```

## Advantages

```text
+ Consistent performance: page 1 and page 50000 are equally fast
+ No COUNT(*) query needed (uses limit+1 trick)
+ No duplicate/missing row problem:
    Even if rows are inserted/deleted between requests,
    cursor-based queries won't skip or repeat rows
    because they use WHERE id > cursor, not OFFSET
+ Perfect for real-time feeds (Twitter, Instagram, chat)
+ Works well with infinite scroll UIs
```

## Disadvantages

```text
- Cannot jump to arbitrary pages (no "go to page 50")
- Cannot show "Page X of Y" (no totalElements)
- Client must traverse sequentially (page 1 -> 2 -> 3 -> ...)
- More complex to implement than offset
- Cursor must be opaque and tamper-resistant
- Sorting is limited: you can only cursor-paginate on columns
  that have a unique, ordered value (usually id or created_at + id)
```

## FlowForge Implementation — Compound Cursor Pagination

In FlowForge, we used a **compound cursor** with `createdAt + id` as the key pair.
This is the keyset technique applied inside a cursor-based API.

### Why compound key? Why not just `id`?

```text
If you sort by createdAt DESC (newest first), id alone won't work:
  - UUIDs are random — they don't reflect insertion order
  - You'd get rows in wrong order if you use WHERE id > cursor

Compound key (createdAt, id) solves this:
  - createdAt gives the sort order you want (newest first)
  - id is the tiebreaker when two rows have the same createdAt
  - Together they form a unique, stable cursor position
```

### How we route offset vs cursor on the same endpoint

```java
// Offset pagination (default) — no mode param
@GetMapping
public ResponseEntity<PageResponse<WorkflowSummaryResponse>> list(Pageable pageable) { ... }

// Cursor pagination — triggered by mode=cursor param
@GetMapping(params = "mode=cursor")
public ResponseEntity<CursorPageResponse<WorkflowSummaryResponse>> listWithCursor(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int limit) { ... }
```

```text
GET /api/v1/workflows                        → offset (Page)
GET /api/v1/workflows?page=2&size=10         → offset (Page)
GET /api/v1/workflows?mode=cursor&limit=5    → cursor (CursorPageResponse)
```

Spring's `params = "mode=cursor"` acts as a discriminator — same path,
different handler based on query parameter.

### Repository — compound keyset queries

```java
// First page: no cursor, just ORDER BY
@Query("SELECT w FROM Workflow w ORDER BY w.createdAt DESC, w.id DESC")
List<Workflow> findFirstPage(Pageable pageable);

// Next pages: compound WHERE for keyset seek
@Query("""
    SELECT w FROM Workflow w
    WHERE w.createdAt < :cursorTime
       OR (w.createdAt = :cursorTime AND w.id < :cursorId)
    ORDER BY w.createdAt DESC, w.id DESC
    """)
List<Workflow> findAfterCursor(Instant cursorTime, UUID cursorId, Pageable pageable);
```

```text
The compound WHERE clause:
  WHERE createdAt < cursorTime                         → rows with an earlier timestamp
     OR (createdAt = cursorTime AND id < cursorId)     → same timestamp, tiebreak by id

This handles the edge case where multiple rows share the same createdAt.
Without the tiebreaker, rows would be skipped.
```

### Service — cursor encoding/decoding + limit clamping

```java
public CursorPageResponse<WorkflowSummaryResponse> findAllWithCursor(String cursor, int limit) {
    int fetchSize = Math.min(Math.max(limit, 1), 100);  // clamp: 1 <= limit <= 100
    Pageable pageable = PageRequest.of(0, fetchSize + 1);  // limit+1 trick

    List<Workflow> workflows;
    if (cursor == null || cursor.isBlank()) {
        workflows = workflowRepository.findFirstPage(pageable);
    } else {
        CursorData cursorData = decodeCursor(cursor);
        workflows = workflowRepository.findAfterCursor(cursorData.createdAt(), cursorData.id(), pageable);
    }

    boolean hasNext = workflows.size() > fetchSize;
    List<Workflow> page = hasNext ? workflows.subList(0, fetchSize) : workflows;
    String nextCursor = hasNext ? encodeCursor(page.getLast()) : null;

    List<WorkflowSummaryResponse> content = page.stream()
            .map(workflowMapper::toSummary).toList();

    return new CursorPageResponse<>(content, content.size(), hasNext, nextCursor);
}

// Cursor = Base64(createdAt|id) — opaque to the client
private String encodeCursor(Workflow workflow) {
    String raw = workflow.getCreatedAt().toString() + "|" + workflow.getId().toString();
    return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
}

private CursorData decodeCursor(String cursor) {
    String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    String[] parts = decoded.split("\\|", 2);
    return new CursorData(Instant.parse(parts[0]), UUID.fromString(parts[1]));
}

private record CursorData(Instant createdAt, UUID id) {}
```

```text
Limit clamping:
  Client sends limit=-5   → clamped to 1
  Client sends limit=999  → clamped to 100
  Client sends limit=20   → used as-is

Cursor format (Base64 URL-safe, no padding):
  Raw:     2026-08-27T00:30:00Z|550e8400-e29b-41d4-a716-446655440000
  Encoded: MjAyNi0wOC0yN1QwMDozMDowMFp8NTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAw
```

## When to Use Cursor Pagination

```text
USE when:
  - Dataset is very large (> 100K rows)
  - Users scroll sequentially (feeds, timelines, chat history)
  - Data changes frequently (new items inserted often)
  - Mobile apps with infinite scroll
  - Performance is critical (no deep-page penalty)
  - Real-time or near-real-time data

AVOID when:
  - Users need to jump to arbitrary pages
  - You need to display "Page X of Y"
  - Admin dashboards where random page access is needed
  - Reports that need total counts
```

---

# 4. Keyset Pagination

Keyset is the SQL technique behind cursor pagination. The cursor holds the keyset values.

## How it works

```text
Instead of:   OFFSET 10000 LIMIT 20         (slow: scans 10020 rows)
Use:          WHERE id > 5000 LIMIT 20       (fast: index seek)

The "keyset" is the set of column values that defines your position.
For simple cases, it's just the id.
For compound sorting, it's multiple columns.
```

## Simple keyset (single column)

```sql
-- First page
SELECT * FROM workflows
ORDER BY id ASC
LIMIT 20;
-- Last row has id = 20

-- Second page
SELECT * FROM workflows
WHERE id > 20
ORDER BY id ASC
LIMIT 20;
-- Last row has id = 40

-- Third page
SELECT * FROM workflows
WHERE id > 40
ORDER BY id ASC
LIMIT 20;
```

## Compound keyset (multiple columns)

When sorting by a non-unique column (like `created_at`), you need a tiebreaker:

```text
Problem: Two workflows have created_at = '2025-08-27 10:00:00'
         If you use WHERE created_at > '2025-08-27 10:00:00'
         you might skip one of them.

Solution: Compound keyset = (created_at, id)
         WHERE (created_at, id) > ('2025-08-27 10:00:00', 'abc123')
```

```sql
-- Compound keyset pagination (sorted by created_at DESC, id DESC)
-- First page
SELECT * FROM workflows
ORDER BY created_at DESC, id DESC
LIMIT 20;
-- Last row: created_at = '2025-08-25', id = 'xyz789'

-- Second page (rows that come AFTER the last row in our sort order)
SELECT * FROM workflows
WHERE (created_at < '2025-08-25')
   OR (created_at = '2025-08-25' AND id < 'xyz789')
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

```text
Why the compound WHERE clause?

Sort order: created_at DESC, id DESC

"After" means either:
  1. created_at is strictly less (an earlier date)
  OR
  2. created_at is the same, but id is less (tiebreaker)

This guarantees no skipped rows, even if created_at has duplicates.
```

### Spring Data implementation

```java
@Query("""
    SELECT w FROM Workflow w
    WHERE w.createdAt < :createdAt
       OR (w.createdAt = :createdAt AND w.id < :id)
    ORDER BY w.createdAt DESC, w.id DESC
    """)
List<Workflow> findAfterKeyset(
    @Param("createdAt") Instant createdAt,
    @Param("id") UUID id,
    Pageable pageable
);
```

## Index requirement

```sql
-- For keyset pagination to be fast, you need a matching composite index:
CREATE INDEX idx_workflows_created_at_id ON workflows (created_at DESC, id DESC);

-- Without this index, the database falls back to a full table scan + sort
-- and you lose all the performance benefit.
```

## Advantages

```text
+ Fastest pagination strategy for large datasets
+ O(log n) index seek -- constant time regardless of page depth
+ No COUNT(*) needed
+ No duplicate/missing rows
+ Database-level optimization (uses B-tree index efficiently)
```

## Disadvantages

```text
- Cannot jump to arbitrary pages
- Requires a unique, ordered column (or compound key) for correctness
- Compound keyset queries are complex to write
- Sort order is fixed (you can't easily let the client change sort columns)
- Need matching database indexes
- More complex cursor encoding/decoding
```

## When to Use Keyset Pagination

```text
USE when:
  - Very large datasets (millions+ rows)
  - Performance is the top priority
  - You can accept sequential-only traversal
  - Sort order is predictable (usually created_at + id)
  - You have proper indexes in place

AVOID when:
  - Users need flexible sorting (different columns per request)
  - Random page access is required
  - Dataset is small (offset is simpler and fast enough)
```

---

# 5. Seek Pagination

Seek pagination is a variant of keyset that uses a named position value
instead of the generic "keyset" concept. Often used for time-series data.

```text
GET /api/v1/events?after=2025-08-27T10:30:00Z&limit=50

SQL:
  SELECT * FROM events
  WHERE event_time > '2025-08-27T10:30:00Z'
  ORDER BY event_time ASC
  LIMIT 50

Next request uses the last event_time from the response:
  GET /api/v1/events?after=2025-08-27T10:35:42Z&limit=50
```

The difference from keyset is mostly semantic:
- **Keyset**: generic technique, cursor encodes position
- **Seek**: domain-specific, the "seek" parameter has business meaning (`after`, `since`, `from`)

Same performance characteristics. Same SQL. Just a different API design.

---

# Page vs Slice in Spring Data

Spring Data gives you two return types for paginated queries.

## Page<T>

```java
public Page<Workflow> findAll(Pageable pageable) {
    return workflowRepository.findAll(pageable);
}
```

```text
Page<T> runs TWO queries:
  1. SELECT * FROM workflows ORDER BY ... LIMIT 20 OFFSET 40
  2. SELECT COUNT(*) FROM workflows

Returns:
  content          -> the actual rows
  totalElements    -> total rows in the table (from COUNT)
  totalPages       -> totalElements / pageSize
  number           -> current page number
  size             -> page size
  first / last     -> booleans
```

## Slice<T>

```java
public Slice<Workflow> findAll(Pageable pageable) {
    return workflowRepository.findAll(pageable);  // if return type is Slice
}
```

```text
Slice<T> runs ONE query (with limit+1 trick):
  SELECT * FROM workflows ORDER BY ... LIMIT 21 OFFSET 40
  (requests pageSize + 1)

Returns:
  content          -> the actual rows (trimmed to pageSize)
  hasNext          -> true if the extra row existed
  number           -> current page number
  size             -> page size
  first            -> boolean

Does NOT return:
  totalElements    -> UNKNOWN
  totalPages       -> UNKNOWN
```

## When to use which

```text
Use Page when:
  - You need to display "Showing page 3 of 12"
  - You need totalElements for a UI counter
  - Dataset is small enough that COUNT(*) is cheap

Use Slice when:
  - You just need "Load More" / "Next Page" functionality
  - Dataset is large and COUNT(*) is expensive
  - Infinite scroll UIs
  - Performance matters more than knowing the total

COUNT(*) cost on large tables:
  10K rows   -> COUNT is ~2ms    (negligible)
  100K rows  -> COUNT is ~20ms   (acceptable)
  1M rows    -> COUNT is ~200ms  (noticeable)
  10M rows   -> COUNT is ~2s     (unacceptable)
  100M rows  -> COUNT is ~20s    (completely broken)

  The cost comes from scanning the entire index to count rows.
  There's no shortcut -- the DB doesn't cache this number because
  concurrent transactions may see different row counts.
```

---

# Comparison Table

```text
Feature                 Offset          Cursor          Keyset
---------------------------------------------------------------------
Jump to page N          YES             NO              NO
"Page X of Y"          YES             NO              NO
totalElements           YES (COUNT)     NO              NO
Deep-page performance   BAD (O(n))      GOOD (O(log n)) GOOD (O(log n))
Data consistency        POOR            GOOD            GOOD
(between page fetches)  (dup/miss)      (stable)        (stable)
Flexible sorting        YES             LIMITED         LIMITED
Implementation          SIMPLE          MODERATE        COMPLEX
SQL queries per req     2 (data+count)  1               1
Best for                Admin UIs       Feeds/scroll    High-perf APIs
Mobile/infinite scroll  NO              YES             YES
Bookmarkable pages      YES             NO              NO
```

---

# Decision Flowchart

```text
Do you need to jump to arbitrary pages (page 1, page 50, page 200)?
  │
  ├── YES ──> Is the dataset small (< 100K rows)?
  │             ├── YES ──> OFFSET PAGINATION ✓
  │             └── NO  ──> OFFSET with capped max page
  │                         (reject page > 100, force cursor for deep access)
  │
  └── NO ──> Is it a feed/timeline/infinite scroll?
               ├── YES ──> CURSOR PAGINATION ✓
               └── NO  ──> Is sorting always the same (e.g., by date)?
                             ├── YES ──> KEYSET PAGINATION ✓
                             └── NO  ──> CURSOR PAGINATION ✓
                                         (more flexible than keyset)
```

---

# Common Mistakes

### 1. Not setting a max page size

```java
// BAD: client can request ?size=1000000
@GetMapping
public Page<Workflow> list(Pageable pageable) {
    return workflowService.findAll(pageable);
}

// GOOD: cap the page size
// In application.yml:
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

### 2. Offset pagination on huge tables without a cap

```java
// BAD: allows page=999999 on a 50M row table
// This will scan millions of rows

// GOOD: reject deep pages
if (pageable.getOffset() > 10000) {
    throw new BadRequestException(
        "Offset too large. Use cursor pagination for deep access.");
}
```

### 3. No ORDER BY clause

```text
Without ORDER BY, the database returns rows in arbitrary order.
Page 1 might return row X, and page 2 might ALSO return row X.
ALWAYS include ORDER BY when paginating.

In Spring Data, if no sort is specified, rows come in insertion order
(which is NOT guaranteed to be stable across queries).
```

#### How Spring auto-resolves sort from query params

Spring's `PageableHandlerMethodArgumentResolver` parses `page`, `size`, and
`sort` from query params automatically — no manual parsing needed.

```text
URL param                          Pageable.getSort() resolves to
───────────────────────────────    ────────────────────────────────
(no sort param)                   Sort.unsorted()  → NO ORDER BY
sort=name,asc                     Sort.by(Order.asc("name"))
sort=name,desc                    Sort.by(Order.desc("name"))
sort=status,asc&sort=name,desc    Sort.by(Order.asc("status"), Order.desc("name"))
sort=createdAt,desc&sort=id,desc  Sort.by(Order.desc("createdAt"), Order.desc("id"))
```

Composite sorting: repeat the `sort` param for each column. Spring chains
them in order.

```text
GET /api/v1/workflows?page=0&size=5&sort=status,asc&sort=name,desc

Generates:
  SELECT * FROM workflows
  ORDER BY status ASC, name DESC
  LIMIT 5 OFFSET 0
```

#### The danger of Sort.unsorted()

```text
GET /api/v1/workflows?page=0&size=5    (no sort= param)

Generates:
  SELECT * FROM workflows
  LIMIT 5 OFFSET 0
  -- NO ORDER BY!

The database returns rows in arbitrary order.
  - Page 1 might show row X
  - Page 2 might ALSO show row X (duplicate)
  - Some rows might be skipped entirely
```

#### Fix: default sort fallback in the controller

```java
@GetMapping
public ResponseEntity<PageResponse<WorkflowSummaryResponse>> list(Pageable pageable) {
    if (pageable.getSort().isUnsorted()) {
        pageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
    Page<WorkflowSummaryResponse> page = workflowService.findAll(pageable)
            .map(workflowMapper::toSummary);
    return ResponseEntity.ok(workflowMapper.toPageResponse(page));
}
```

Now `GET /api/v1/workflows?page=0&size=5` without `sort=` defaults to
`ORDER BY created_at DESC` — newest first, stable ordering.

### 4. Using COUNT(*) when you don't need it

```text
If your UI just shows a "Load More" button, you don't need totalElements.
Use Slice<T> instead of Page<T> to skip the COUNT query.

Saves ~200ms per request on a 1M row table.
```

### 5. Cursor pagination with mutable sort columns

```text
BAD:  Cursor on "name" column
      -> If a row's name changes between page requests,
         the cursor breaks (row moves position)

GOOD: Cursor on (created_at, id) or (id)
      -> Immutable columns that never change
      -> Cursor is always valid
```

---

# Real-World Usage Patterns

```text
Application             Pagination Type       Why
------------------------------------------------------------------------
Google Search            Offset (capped)       Users click page 1-10
                                               (Google limits to ~30 pages)

Twitter/X feed           Cursor                Infinite scroll, real-time
                                               inserts, huge dataset

GitHub issues list       Offset + cursor       Web UI uses offset (page links)
                                               API uses cursor (Link headers)

Admin dashboard          Offset                Small dataset, need page jumps
(internal tool)                                and total count

Elasticsearch results    Offset + scroll       search_after for deep pages
                                               (keyset-like)

Chat message history     Cursor/keyset         Load older messages on scroll
                                               WHERE created_at < cursor

Analytics time-series    Seek                  ?after=2025-08-01&limit=1000
                                               Natural time-based access

E-commerce products      Offset                Users browse page by page
                                               Typically < 10K products per
                                               category
```

---

# Performance Benchmarks (Approximate)

```text
Table: 10 million rows, indexed on (created_at, id)

Request                              Offset          Cursor/Keyset
-------------------------------------------------------------------
Page 1 (first 20 rows)               3ms             3ms
Page 10 (rows 181-200)               4ms             3ms
Page 100 (rows 1981-2000)            8ms             3ms
Page 1,000 (rows 19981-20000)        45ms            3ms
Page 10,000 (rows 199981-200000)     400ms           3ms
Page 100,000 (rows 1999981-2000000)  4,000ms         3ms
Page 500,000 (rows 9999981-10M)      20,000ms        3ms

COUNT(*) on 10M rows:                ~2,000ms        not needed

Offset degrades linearly with page depth.
Cursor/keyset stays constant because of index seek.
```

---

# Summary

```text
1. OFFSET / PAGE-NUMBER
   -> Use for small datasets, admin UIs, when you need "Page X of Y"
   -> Simple to implement with Spring Data (Pageable, Page)
   -> Degrades badly on deep pages

2. CURSOR
   -> Use for feeds, infinite scroll, large datasets
   -> Opaque token, sequential access only
   -> Consistent performance at any depth

3. KEYSET
   -> The SQL technique that powers cursor pagination
   -> Best raw performance, but most complex to implement
   -> Requires careful index design

4. SEEK
   -> Domain-specific variant of keyset (e.g., ?after=timestamp)
   -> Natural fit for time-series data

5. In Spring Data:
   -> Page<T>  = offset + COUNT (two queries)
   -> Slice<T> = offset + limit+1 trick (one query, no total)

Start with offset pagination (simple, covers most cases).
Switch to cursor when you hit performance issues or need infinite scroll.
```
