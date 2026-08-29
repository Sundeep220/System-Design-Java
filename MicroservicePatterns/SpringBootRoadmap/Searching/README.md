# Searching -- Deep Dive

How to implement search functionality in a Spring Boot REST API.
This document covers basic LIKE-based search. Full-text search (PostgreSQL, Elasticsearch)
will be added later when we migrate from H2 to PostgreSQL.

---

## The Problem

Filtering lets you narrow by exact values:

```text
?status=ACTIVE          -> exact match on a known field
?minRetries=3           -> numeric range on a known field
```

But filtering can't handle free-text queries:

```text
"Show me anything related to payments"
"Find workflows about order processing"
"Which workflows mention 'retry' in their description?"
```

Search solves this -- the user types a keyword, and we look for it
across multiple text fields.

---

## Search vs Filter -- Key Difference

```text
FILTER:
  - User picks a specific field and value
  - Exact or range match
  - ?status=ACTIVE  -> status column = 'ACTIVE'
  - Structured, predictable

SEARCH:
  - User types free text
  - Substring/fuzzy match across MULTIPLE fields
  - ?search=payment -> name LIKE '%payment%' OR description LIKE '%payment%'
  - Unstructured, flexible
```

```text
In a real API, you combine both:

  GET /api/v1/workflows?search=payment&status=ACTIVE&sort=createdAt,desc

  Meaning: "Find ACTIVE workflows related to payments, newest first"

  Search narrows by keyword (OR across fields)
  Filter narrows by exact value (AND with search)
  Sort orders the results
  Pagination limits the page size
```

---

# Basic LIKE Search (What We Implemented)

## How SQL LIKE Works

```sql
-- Exact substring match
SELECT * FROM workflows WHERE name LIKE '%payment%'
-- Matches: "Payment Pipeline", "Refund Payment Flow", "payment-v2"

-- % is a wildcard that matches any sequence of characters
-- %payment% means: anything, then "payment", then anything
```

```text
LIKE patterns:
  'payment%'    -> starts with "payment"    (prefix search)
  '%payment'    -> ends with "payment"      (suffix search)
  '%payment%'   -> contains "payment"       (substring search)  <-- what we use
  'pay_ent'     -> _ matches exactly one character
```

## Case Sensitivity

```text
Problem: LIKE is case-sensitive in most databases.

  WHERE name LIKE '%payment%'
  -> matches "payment pipeline"
  -> does NOT match "Payment Pipeline"  (capital P)

Solution: Convert both sides to lowercase.

  WHERE LOWER(name) LIKE '%payment%'
  -> matches "Payment Pipeline"  (LOWER("Payment Pipeline") = "payment pipeline")
  -> matches "PAYMENT"           (LOWER("PAYMENT") = "payment")
  -> matches "payment"           (already lowercase)
```

```text
Alternative: ILIKE (PostgreSQL only)

  WHERE name ILIKE '%payment%'
  -> case-insensitive LIKE, same result as LOWER() + LIKE
  -> cleaner syntax but PostgreSQL-specific
  -> not available in H2 (our current database)

We use LOWER() + LIKE because it works on ALL databases (H2, PostgreSQL, MySQL, etc.)
```

## Multi-Field Search

```text
Single field search:
  WHERE LOWER(name) LIKE '%payment%'
  -> only finds workflows with "payment" in the name
  -> misses: description = "Processes payments and refunds"

Multi-field search (what we want):
  WHERE LOWER(name) LIKE '%payment%'
     OR LOWER(description) LIKE '%payment%'
  -> finds "payment" in name OR description
  -> much more useful for the user
```

---

## FlowForge Implementation

### 1. Specification: The Search Method

```java
public static Specification<Workflow> search(String keyword) {
    String pattern = "%" + keyword.toLowerCase() + "%";
    return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("name")), pattern),
            cb.like(cb.lower(root.get("description")), pattern)
    );
}
```

```text
Breaking it down:

  1. keyword = "payment"
  2. pattern = "%payment%"
  3. cb.lower(root.get("name"))         -> LOWER(w.name)
  4. cb.like(..., pattern)               -> LOWER(w.name) LIKE '%payment%'
  5. Same for description
  6. cb.or(nameMatch, descriptionMatch)  -> name LIKE ... OR description LIKE ...

Generated SQL:
  WHERE LOWER(w.name) LIKE '%payment%'
     OR LOWER(w.description) LIKE '%payment%'
```

### 2. Why cb.or() Inside the Specification?

```text
Search is fundamentally different from filters:

  FILTERS use AND -- all conditions must be true:
    status = ACTIVE AND maxRetries >= 3
    (narrowing: each filter reduces results)

  SEARCH uses OR -- any field can match:
    name LIKE '%payment%' OR description LIKE '%payment%'
    (broadening: each field increases chances of a match)

But search as a WHOLE is AND'd with filters:
    (name LIKE '%payment%' OR description LIKE '%payment%')
    AND status = 'ACTIVE'
    AND max_retries >= 3

The OR is INSIDE the search spec.
The AND is BETWEEN search and filters.
```

### 3. Service: Composing Search with Filters

```java
public Page<Workflow> findAll(WorkflowFilterRequest filter, Pageable pageable) {
    sortValidator.validate(pageable.getSort());

    Specification<Workflow> spec = (root, query, cb) -> null;

    // Search -- OR across name and description (added as AND to the overall spec)
    if (filter.search() != null && !filter.search().isBlank()) {
        spec = spec.and(WorkflowSpecification.search(filter.search()));
    }
    // Filters -- each one AND'd
    if (filter.status() != null) {
        spec = spec.and(WorkflowSpecification.hasStatus(filter.status()));
    }
    // ... more filters ...

    return workflowRepository.findAll(spec, pageable);
}
```

```text
Example: ?search=payment&status=ACTIVE&minRetries=3

  spec = no-op (match all)
         .and( search("payment") )       -> (name LIKE '%payment%' OR desc LIKE '%payment%')
         .and( hasStatus(ACTIVE) )       -> AND status = 'ACTIVE'
         .and( minRetries(3) )           -> AND max_retries >= 3

  Final SQL:
    WHERE (LOWER(name) LIKE '%payment%' OR LOWER(description) LIKE '%payment%')
      AND status = 'ACTIVE'
      AND max_retries >= 3
    ORDER BY ...
    LIMIT 20 OFFSET 0
```

### 4. Controller: The search Query Parameter

```java
@GetMapping
public ResponseEntity<PageResponse<WorkflowSummaryResponse>> list(
        Pageable pageable,
        @RequestParam(required = false) String search,      // <-- new
        @RequestParam(required = false) WorkflowStatus status,
        @RequestParam(required = false) Integer minRetries,
        // ... other filters ...
) {
    WorkflowFilterRequest filter = new WorkflowFilterRequest(
            search, status, minRetries, ...);
    // ...
}
```

```text
How it handles edge cases:

  ?search=payment     -> search = "payment"  -> applies search spec
  ?search=            -> search = ""          -> isBlank() = true -> skipped
  ?search=   (spaces) -> search = "   "       -> isBlank() = true -> skipped
  (no param)          -> search = null         -> null check -> skipped

All three "empty" cases result in no search filter, returning all results.
```

### 5. Filter DTO: Adding Search

```java
public record WorkflowFilterRequest(
        String search,            // <-- new, first field
        WorkflowStatus status,
        Integer minRetries,
        Integer maxRetries,
        Instant createdAfter,
        Instant createdBefore
) {}
```

```text
Why search is a String, not an enum or typed value:

  Filters are typed:  status = WorkflowStatus.ACTIVE  (known values)
  Search is free text: search = "anything the user types"

  The user doesn't know field names or valid values.
  They just type words and expect results.
```

---

# Performance Considerations

## LIKE '%keyword%' is Slow on Large Tables

```text
Problem:
  WHERE LOWER(name) LIKE '%payment%'

  The database CANNOT use a B-tree index for leading-wildcard LIKE.
  '%payment%' starts with %, so the DB must scan EVERY row.

  1,000 rows:      fine (milliseconds)
  100,000 rows:    noticeable (tens of ms)
  10,000,000 rows: painful (seconds)

This is called a FULL TABLE SCAN.
```

## Why B-tree Indexes Don't Help

```text
B-tree indexes are sorted like a phone book:

  Payment Pipeline
  Process Orders
  Refund Handler

Finding "Pay..." is fast -- jump to the P section (prefix search).
Finding "...ment..." is impossible -- you'd have to read every entry.

  LIKE 'payment%'    -> B-tree CAN help (prefix match)
  LIKE '%payment%'   -> B-tree CANNOT help (substring match)
```

## Solutions for Large-Scale Search (Future)

```text
When LIKE becomes too slow, upgrade to:

1. PostgreSQL Full-Text Search (GIN index + tsvector)
   - Tokenizes text into searchable terms
   - Supports stemming ("running" matches "run")
   - Relevance ranking with ts_rank()
   - Works within PostgreSQL, no external service
   - Will be added when we migrate from H2 to PostgreSQL

2. Trigram Index (PostgreSQL pg_trgm extension)
   - CREATE INDEX idx_name_trgm ON workflows USING GIN (name gin_trgm_ops);
   - Makes LIKE '%keyword%' use the index
   - Supports fuzzy matching with similarity()

3. Elasticsearch / OpenSearch
   - Dedicated search engine running alongside the database
   - Best for: fuzzy matching, typo tolerance, synonyms, faceted search
   - Data is synced from PostgreSQL to Elasticsearch
   - Most powerful but adds infrastructure complexity
```

---

# Curl Examples

```bash
# Basic search
curl "http://localhost:8080/api/v1/workflows?search=order"

# Case-insensitive
curl "http://localhost:8080/api/v1/workflows?search=ORDER"

# Partial match
curl "http://localhost:8080/api/v1/workflows?search=pay"

# Search + filter
curl "http://localhost:8080/api/v1/workflows?search=order&status=ACTIVE"

# Search + filter + sort + pagination
curl "http://localhost:8080/api/v1/workflows?search=pipe&status=DRAFT&sort=name,asc&page=0&size=5"

# Everything combined
curl "http://localhost:8080/api/v1/workflows?search=work&status=ACTIVE&minRetries=2&sort=createdAt,desc&page=0&size=10"
```

---

# Summary

```text
1. BASIC SEARCH (current implementation)
   - Single ?search= parameter
   - Case-insensitive LIKE across name and description
   - Uses LOWER() + LIKE for database portability
   - OR logic inside search, AND logic with filters
   - Blank/null search is ignored (returns all)

2. HOW IT COMPOSES WITH FILTERS
   - Search: (name LIKE '%x%' OR desc LIKE '%x%')    -> OR inside
   - Filters: AND status = 'ACTIVE' AND retries >= 3  -> AND between
   - Result: (search OR) AND filter AND filter

3. LIMITATIONS
   - No typo tolerance ("paymet" won't match "payment")
   - No relevance ranking (all matches weighted equally)
   - No stemming ("running" won't match "run")
   - Performance degrades on large tables (full table scan)

4. FUTURE UPGRADES (when we switch to PostgreSQL)
   - PostgreSQL Full-Text Search with tsvector + ts_rank
   - Trigram indexes for fast substring search
   - Elasticsearch for advanced search features
```
