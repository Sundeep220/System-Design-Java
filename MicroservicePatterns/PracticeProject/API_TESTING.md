# FlowForge -- API Testing Guide

Base URL: `http://localhost:8080`

---

## Step 4: CRUD Endpoints

### 1. Create a Workflow

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "Order Processing",
  "description": "Handles end-to-end order fulfillment",
  "maxRetries": 3,
  "timeoutSeconds": 60
}
```

**Expected:** `201 Created`
- Response body: full workflow detail with `id`, `status: DRAFT`, `steps: []`
- `Location` header: `/api/v1/workflows/{id}`

---

### 2. Create a Second Workflow

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "Payment Pipeline",
  "description": "Processes payments and refunds",
  "maxRetries": 5,
  "timeoutSeconds": 120
}
```

**Expected:** `201 Created`

---

### 3. List Workflows (Paginated)

```
GET /api/v1/workflows?page=0&size=10
```

**Expected:** `200 OK`
- `content`: array of summary objects (id, name, status, createdAt, updatedAt)
- `page: 0`, `size: 10`, `totalElements: 2`, `totalPages: 1`
- `first: true`, `last: true`

---

### 4. Get Workflow by ID

```
GET /api/v1/workflows/{id}
```

Replace `{id}` with a UUID from the create response.

**Expected:** `200 OK`
- Full detail: includes `description`, `maxRetries`, `timeoutSeconds`, `steps`

---

### 5. Full Update (PUT)

```
PUT /api/v1/workflows/{id}
Content-Type: application/json
```

```json
{
  "name": "Order Processing v2",
  "description": "Updated description",
  "maxRetries": 5,
  "timeoutSeconds": 90
}
```

**Expected:** `200 OK`
- All fields replaced with new values
- `updatedAt` changed, `createdAt` unchanged

---

### 6. Delete a Workflow

```
DELETE /api/v1/workflows/{id}
```

**Expected:** `204 No Content` (empty body)

Verify: `GET /api/v1/workflows/{id}` should now return `404`.

---

### 7. Error Cases

#### 7a. Get non-existent workflow

```
GET /api/v1/workflows/00000000-0000-0000-0000-000000000000
```

**Expected:** `404 Not Found`

#### 7b. Blank name (validation failure)

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "",
  "description": "test",
  "maxRetries": 3,
  "timeoutSeconds": 60
}
```

**Expected:** `400 Bad Request`

#### 7c. Invalid values (validation failure)

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "Bad Workflow",
  "description": "test",
  "maxRetries": -1,
  "timeoutSeconds": 0
}
```

**Expected:** `400 Bad Request` (maxRetries < 0, timeoutSeconds < 1)

#### 7d. Missing required fields

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{}
```

**Expected:** `400 Bad Request`

---

## H2 Console

While using H2, you can inspect the database directly:

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:flowforge
Username: sa
Password: (leave blank)
```

---

## Step 5: Global Exception Handler

All errors now return a consistent `ApiError` JSON structure.

### Expected error response format

```json
{
  "timestamp": "2026-08-27T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/v1/workflows",
  "details": [
    {
      "field": "name",
      "message": "Name is required",
      "rejectedValue": ""
    }
  ]
}
```

### 8. Validation error — structured field details

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "",
  "description": "test",
  "maxRetries": -1,
  "timeoutSeconds": 0
}
```

**Expected:** `400 Bad Request`
- `errorCode`: `VALIDATION_FAILED`
- `details`: array with entries for `name`, `maxRetries`, `timeoutSeconds`

### 9. Not found — structured error

```
GET /api/v1/workflows/00000000-0000-0000-0000-000000000000
```

**Expected:** `404 Not Found`
- `errorCode`: `RESOURCE_NOT_FOUND`
- `message`: `Workflow not found with id: 00000000-...`
- `details`: `null`

### 10. Malformed JSON

```
POST /api/v1/workflows
Content-Type: application/json
```

```
{ this is not valid json }
```

**Expected:** `400 Bad Request`
- `errorCode`: `MALFORMED_REQUEST`
- `message`: `Request body is missing or malformed`

### 11. Missing request body entirely

```
POST /api/v1/workflows
Content-Type: application/json
```

(send with empty body)

**Expected:** `400 Bad Request`
- `errorCode`: `MALFORMED_REQUEST`

---

## Step 6: Custom Validation

### 12. Custom validator — special characters in name

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "Order@Processing#v1!",
  "description": "Has special chars",
  "maxRetries": 3,
  "timeoutSeconds": 60
}
```

**Expected:** `400 Bad Request`
- `errorCode`: `VALIDATION_FAILED`
- `details` includes: `"Workflow name must contain only letters, numbers, spaces, hyphens, and underscores"`

### 13. Valid names that should pass

These should all return `201 Created`:

```json
{ "name": "Order Processing",     "maxRetries": 3, "timeoutSeconds": 60 }
{ "name": "payment-pipeline",     "maxRetries": 3, "timeoutSeconds": 60 }
{ "name": "step_2_retry",         "maxRetries": 3, "timeoutSeconds": 60 }
{ "name": "My Workflow 2026",     "maxRetries": 3, "timeoutSeconds": 60 }
```

### 14. Multiple validation errors at once

```
POST /api/v1/workflows
Content-Type: application/json
```

```json
{
  "name": "",
  "description": "test",
  "maxRetries": 99,
  "timeoutSeconds": -5
}
```

**Expected:** `400 Bad Request`
- `details` array should contain multiple entries:
  - `name`: "Name is required"
  - `maxRetries`: "maxRetries must be <= 10"
  - `timeoutSeconds`: "timeoutSeconds must be >= 1"

---

## Step 7: PATCH Endpoint (Partial Update)

### 15. Patch only the name

First create a workflow, then patch it:

```
PATCH /api/v1/workflows/{id}
Content-Type: application/json
```

```json
{
  "name": "Renamed Workflow"
}
```

**Expected:** `200 OK`
- `name` changed to `"Renamed Workflow"`
- All other fields (`description`, `maxRetries`, `timeoutSeconds`) unchanged
- `updatedAt` changed

### 16. Patch only maxRetries

```
PATCH /api/v1/workflows/{id}
Content-Type: application/json
```

```json
{
  "maxRetries": 7
}
```

**Expected:** `200 OK`
- Only `maxRetries` changed to `7`
- Everything else unchanged

### 17. Patch multiple fields at once

```
PATCH /api/v1/workflows/{id}
Content-Type: application/json
```

```json
{
  "description": "Updated via patch",
  "timeoutSeconds": 300
}
```

**Expected:** `200 OK`
- `description` and `timeoutSeconds` updated
- `name` and `maxRetries` unchanged

### 18. Patch with empty body

```
PATCH /api/v1/workflows/{id}
Content-Type: application/json
```

```json
{}
```

**Expected:** `200 OK`
- No fields changed (all Optionals are empty)
- This is valid — a no-op patch

### 19. Patch with invalid name

```
PATCH /api/v1/workflows/{id}
Content-Type: application/json
```

```json
{
  "name": "Bad@Name!"
}
```

**Expected:** `400 Bad Request`
- `errorCode`: `VALIDATION_FAILED`
- `details` includes workflow name validation error

### 20. Patch non-existent workflow

```
PATCH /api/v1/workflows/00000000-0000-0000-0000-000000000000
Content-Type: application/json
```

```json
{
  "name": "Doesn't matter"
}
```

**Expected:** `404 Not Found`

### PUT vs PATCH comparison

| | PUT | PATCH |
|---|---|---|
| Body | All fields required | Only fields you want to change |
| Missing fields | Overwritten with new values | Left unchanged |
| Use case | Full replacement | Partial update |

---

## Step 8: Pagination (Offset + Cursor)

### Offset Pagination (already implemented)

#### 21. Default pagination

```
GET /api/v1/workflows
```

**Expected:** `200 OK` with `PageResponse` — defaults to `page=0`, `size=20`

#### 22. Custom page and size

```
GET /api/v1/workflows?page=1&size=5
```

**Expected:** `200 OK` — second page with 5 items

#### 23. Sorting with offset pagination

```
GET /api/v1/workflows?page=0&size=10&sort=name,asc
```

**Expected:** `200 OK` — sorted by name ascending

### Cursor Pagination

#### 24. First page (no cursor)

```
GET /api/v1/workflows?mode=cursor&limit=3
```

**Expected:** `200 OK`
```json
{
  "content": [ ... 3 items ... ],
  "size": 3,
  "hasNext": true,
  "nextCursor": "MjAyNi0wOC0yN1QwMDozMDowMFp8YWJjMTIz..."
}
```

#### 25. Next page (with cursor)

Copy `nextCursor` from previous response:

```
GET /api/v1/workflows?mode=cursor&limit=3&cursor=MjAyNi0wOC0yN1QwMDozMDowMFp8YWJjMTIz...
```

**Expected:** `200 OK` — next 3 items, different from first page

#### 26. Last page (hasNext = false)

Keep following `nextCursor` until:

**Expected:** `200 OK`
```json
{
  "content": [ ... remaining items ... ],
  "size": 2,
  "hasNext": false,
  "nextCursor": null
}
```

#### 27. Invalid cursor

```
GET /api/v1/workflows?mode=cursor&cursor=not-a-valid-cursor
```

**Expected:** `400 Bad Request`
- `errorCode`: `INVALID_PARAMETER`
- `message`: `"Invalid cursor: not-a-valid-cursor"`

#### 28. Limit clamping

```
GET /api/v1/workflows?mode=cursor&limit=999
```

**Expected:** `200 OK` — limit clamped to max 100

```
GET /api/v1/workflows?mode=cursor&limit=-5
```

**Expected:** `200 OK` — limit clamped to min 1

### Offset vs Cursor comparison

| | Offset (`page=N`) | Cursor (`cursor=xxx`) |
|---|---|---|
| Jump to page 5? | ✅ Yes | ❌ No — forward only |
| Total count? | ✅ `totalElements` | ❌ Not available |
| Consistent with inserts? | ❌ Rows shift | ✅ Stable — no skips/duplicates |
| Performance at page 10000? | ❌ Slow (`OFFSET 200000`) | ✅ Fast (indexed WHERE) |

---

## Step 9: Filtering + Sorting

Filtering uses JPA Specifications. Sorting uses Spring Data's `?sort=` parameter with a whitelist validator.

### Filtering

#### 29. Filter by status

```
GET /api/v1/workflows?status=ACTIVE
```

**Expected:** `200 OK` — only workflows with `status: ACTIVE`

Try each status value:

```
GET /api/v1/workflows?status=DRAFT
GET /api/v1/workflows?status=PAUSED
GET /api/v1/workflows?status=ARCHIVED
```

#### 30. Filter by minimum retries

```
GET /api/v1/workflows?minRetries=3
```

**Expected:** `200 OK` — only workflows with `maxRetries >= 3`

#### 31. Filter by maximum retries

```
GET /api/v1/workflows?maxRetries=5
```

**Expected:** `200 OK` — only workflows with `maxRetries <= 5`

#### 32. Filter by retry range

```
GET /api/v1/workflows?minRetries=2&maxRetries=5
```

**Expected:** `200 OK` — only workflows with `2 <= maxRetries <= 5`

#### 33. Filter by created after

```
GET /api/v1/workflows?createdAfter=2025-01-01T00:00:00Z
```

**Expected:** `200 OK` — only workflows created on or after Jan 1, 2025

#### 34. Filter by created before

```
GET /api/v1/workflows?createdBefore=2027-01-01T00:00:00Z
```

**Expected:** `200 OK` — only workflows created on or before Jan 1, 2027

#### 35. Filter by date range

```
GET /api/v1/workflows?createdAfter=2025-01-01T00:00:00Z&createdBefore=2027-01-01T00:00:00Z
```

**Expected:** `200 OK` — only workflows created within the date range

#### 36. Combine multiple filters

```
GET /api/v1/workflows?status=ACTIVE&minRetries=3
```

**Expected:** `200 OK` — only ACTIVE workflows with 3+ retries

```
GET /api/v1/workflows?status=DRAFT&maxRetries=2&createdAfter=2025-01-01T00:00:00Z
```

**Expected:** `200 OK` — DRAFT workflows with at most 2 retries, created after Jan 1 2025

#### 37. No filters (returns all)

```
GET /api/v1/workflows
```

**Expected:** `200 OK` — all workflows (same as before, backward compatible)

#### 38. Filter with no matches

```
GET /api/v1/workflows?status=ARCHIVED
```

**Expected:** `200 OK` — empty content array, `totalElements: 0`
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

#### 39. Invalid status value

```
GET /api/v1/workflows?status=INVALID_STATUS
```

**Expected:** `400 Bad Request` — Spring cannot convert to `WorkflowStatus` enum

---

### Sorting

#### 40. Sort by name ascending

```
GET /api/v1/workflows?sort=name,asc
```

**Expected:** `200 OK` — workflows sorted alphabetically by name (A → Z)

#### 41. Sort by name descending

```
GET /api/v1/workflows?sort=name,desc
```

**Expected:** `200 OK` — workflows sorted reverse alphabetically (Z → A)

#### 42. Sort by createdAt descending (newest first)

```
GET /api/v1/workflows?sort=createdAt,desc
```

**Expected:** `200 OK` — newest workflows first

#### 43. Sort by default direction (ascending)

```
GET /api/v1/workflows?sort=name
```

**Expected:** `200 OK` — sorted by name ascending (asc is default when direction omitted)

#### 44. Multi-column sort

```
GET /api/v1/workflows?sort=status,asc&sort=createdAt,desc
```

**Expected:** `200 OK` — sorted by status alphabetically, then by newest first within same status

```
GET /api/v1/workflows?sort=maxRetries,desc&sort=name,asc
```

**Expected:** `200 OK` — highest retries first, then alphabetically within same retry count

#### 45. Invalid sort field (whitelist rejection)

```
GET /api/v1/workflows?sort=password,asc
```

**Expected:** `400 Bad Request`
```json
{
  "status": 400,
  "error": "Bad Request",
  "errorCode": "INVALID_SORT_FIELD",
  "message": "Invalid sort field: 'password'. Allowed fields: [name, status, maxRetries, timeoutSeconds, createdAt, updatedAt]"
}
```

#### 46. Another invalid sort field

```
GET /api/v1/workflows?sort=id,asc
```

**Expected:** `400 Bad Request` — `id` is not in the whitelist

#### 47. Invalid field in multi-column sort

```
GET /api/v1/workflows?sort=name,asc&sort=secret,desc
```

**Expected:** `400 Bad Request` — `secret` is not in the whitelist (entire request rejected)

---

### Combined: Filtering + Sorting + Pagination

#### 48. Filter + sort

```
GET /api/v1/workflows?status=ACTIVE&sort=name,asc
```

**Expected:** `200 OK` — only ACTIVE workflows, sorted by name

#### 49. Filter + sort + pagination

```
GET /api/v1/workflows?status=DRAFT&minRetries=2&sort=createdAt,desc&page=0&size=5
```

**Expected:** `200 OK` — DRAFT workflows with 2+ retries, newest first, 5 per page

#### 50. All parameters combined

```
GET /api/v1/workflows?status=ACTIVE&minRetries=1&maxRetries=10&createdAfter=2025-01-01T00:00:00Z&createdBefore=2027-12-31T23:59:59Z&sort=maxRetries,desc&sort=name,asc&page=0&size=10
```

**Expected:** `200 OK` — ACTIVE workflows with 1-10 retries, created in 2025-2027, sorted by retries desc then name asc, page 0 with 10 items

### Allowed sort fields

| Field | Example |
|---|---|
| `name` | `?sort=name,asc` |
| `status` | `?sort=status,desc` |
| `maxRetries` | `?sort=maxRetries,asc` |
| `timeoutSeconds` | `?sort=timeoutSeconds,desc` |
| `createdAt` | `?sort=createdAt,desc` |
| `updatedAt` | `?sort=updatedAt,asc` |

---

## Step 10: Searching

A single `?search=` parameter performs case-insensitive LIKE search across `name` AND `description` fields. Combines with all existing filters, sorting, and pagination.

### Basic Search

#### 51. Search by name

```
GET /api/v1/workflows?search=order
```

**Expected:** `200 OK` — workflows where name OR description contains "order" (case-insensitive)

#### 52. Search by description keyword

```
GET /api/v1/workflows?search=payment
```

**Expected:** `200 OK` — matches workflows with "payment" in name or description

#### 53. Case-insensitive search

```
GET /api/v1/workflows?search=ORDER
GET /api/v1/workflows?search=Order
GET /api/v1/workflows?search=order
```

**Expected:** All three return the same results

#### 54. Partial match

```
GET /api/v1/workflows?search=pay
```

**Expected:** `200 OK` — matches "Payment Pipeline", "Refund Payment", etc. (substring match)

#### 55. No results

```
GET /api/v1/workflows?search=xyznonexistent
```

**Expected:** `200 OK` — empty content, `totalElements: 0`

#### 56. Empty / blank search (ignored)

```
GET /api/v1/workflows?search=
GET /api/v1/workflows?search=   
```

**Expected:** `200 OK` — returns all workflows (blank search is treated as no filter)

---

### Search + Filters + Sort + Pagination

#### 57. Search + status filter

```
GET /api/v1/workflows?search=order&status=ACTIVE
```

**Expected:** `200 OK` — ACTIVE workflows matching "order" in name or description

#### 58. Search + sort

```
GET /api/v1/workflows?search=process&sort=createdAt,desc
```

**Expected:** `200 OK` — workflows matching "process", newest first

#### 59. Search + filter + sort + pagination

```
GET /api/v1/workflows?search=pipe&status=DRAFT&sort=name,asc&page=0&size=5
```

**Expected:** `200 OK` — DRAFT workflows matching "pipe", sorted by name, 5 per page

#### 60. Everything combined

```
GET /api/v1/workflows?search=work&status=ACTIVE&minRetries=2&maxRetries=8&createdAfter=2025-01-01T00:00:00Z&sort=createdAt,desc&sort=name,asc&page=0&size=10
```

**Expected:** `200 OK` — ACTIVE workflows matching "work" with 2-8 retries, created after 2025, sorted by date then name, 10 per page

---

## Step 11: Filter + Interceptor

### X-Request-Id Header (RequestIdFilter)

#### 61. Auto-generated request ID

```
GET /api/v1/workflows
```

**Expected:** `200 OK`
- Response header `X-Request-Id` is present with a UUID value
- Example: `X-Request-Id: 3f2504e0-4f89-11d3-9a0c-0305e82c3301`

#### 62. Client-provided request ID (passthrough)

```
GET /api/v1/workflows
X-Request-Id: my-custom-id-123
```

**Expected:** `200 OK`
- Response header `X-Request-Id: my-custom-id-123` (same value echoed back)

---

### Request Timing (RequestTimingFilter)

#### 63. Check server logs for timing

Make any request:

```
GET /api/v1/workflows
```

**Expected:** Server console logs a line like:
```
INFO  ... RequestTimingFilter : GET /api/v1/workflows 200 — 45 ms
```

Verify it shows: method, URI, status code, and duration in milliseconds.

---

### Rate Limiting (RateLimitInterceptor)

#### 64. Rate limit headers present

```
GET /api/v1/workflows
```

**Expected:** `200 OK`
- `X-RateLimit-Limit: 50`
- `X-RateLimit-Remaining: 49` (or less)

#### 65. Rate limit exceeded

Send more than 50 requests within 1 minute to any `/api/**` endpoint.

**Expected:** `429 Too Many Requests`
- `Retry-After: 45` (seconds remaining in current window)

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "You have exceeded the rate limit. Try again in 45 seconds."
}
```

#### 66. Rate limit resets after window

Wait 1 minute after hitting the limit, then:

```
GET /api/v1/workflows
```

**Expected:** `200 OK` — rate limit window has reset, `X-RateLimit-Remaining` back to `49`

#### 67. Rate limit only applies to /api/** paths

```
GET /h2-console
```

**Expected:** Not rate-limited (interceptor only applies to `/api/**`)

---

## Step 12: Custom AOP Aspect

### AOP Proxy Verification

#### 68. Check AOP proxy info

```
GET /api/v1/debug/aop
```

**Expected:** `200 OK`
```json
{
  "isAopProxy": true,
  "isCglibProxy": true,
  "targetClass": "WorkflowService",
  "actualClass": "WorkflowService$$SpringCGLIB$$0"
}
```

- `isAopProxy: true` confirms Spring wrapped the service in a proxy
- `actualClass` contains `$$SpringCGLIB$$` proving CGLIB subclass proxying

---

### @LogExecution Aspect Logging

#### 69. Verify aspect logs on create

```
POST /api/v1/workflows
Content-Type: application/json

{
  "name": "AOP Test Workflow",
  "description": "Testing aspect logging",
  "maxRetries": 3,
  "timeoutSeconds": 30
}
```

**Expected:** `201 Created` + server console shows:
```
INFO  LogExecutionAspect : → WorkflowService.create() called with 1 arg(s): [WorkflowCreateRequest]
INFO  LogExecutionAspect : ← WorkflowService.create() returned in 45 ms
```

#### 70. Verify aspect logs on findAll

```
GET /api/v1/workflows
```

**Expected:** `200 OK` + server console shows:
```
INFO  LogExecutionAspect : → WorkflowService.findAll() called with 2 arg(s): [WorkflowFilterRequest, PageRequest]
INFO  LogExecutionAspect : ← WorkflowService.findAll() returned in 12 ms
```

#### 71. Verify aspect logs exception

```
GET /api/v1/workflows/00000000-0000-0000-0000-000000000000
```

**Expected:** `404 Not Found` + server console shows:
```
INFO  LogExecutionAspect : → WorkflowService.findByIdWithSteps() called with 1 arg(s): [UUID]
ERROR LogExecutionAspect : ✖ WorkflowService.findByIdWithSteps() threw ResourceNotFoundException after 5 ms: Workflow not found with id: 00000000-...
```

---

## Step 12b: PostgreSQL Full-Text Search

> **Prerequisite:** Run with `postgres` profile (default).
> Create the database first: `CREATE DATABASE flowforge;`
> The app uses `ddl-auto: create-drop` and runs `schema-postgres.sql` to create the GIN index.

### Setup: Seed test data

Create several workflows with varied names and descriptions:

```
POST /api/v1/workflows
{ "name": "Order Processing Pipeline", "description": "Handles payment processing and order fulfillment", "maxRetries": 3, "timeoutSeconds": 60 }

POST /api/v1/workflows
{ "name": "User Notification Service", "description": "Sends email and SMS notifications to users", "maxRetries": 5, "timeoutSeconds": 30 }

POST /api/v1/workflows
{ "name": "Payment Gateway Integration", "description": "Processes credit card payments via Stripe", "maxRetries": 2, "timeoutSeconds": 45 }

POST /api/v1/workflows
{ "name": "Data Export Pipeline", "description": "Exports analytics data to CSV and sends reports", "maxRetries": 1, "timeoutSeconds": 120 }

POST /api/v1/workflows
{ "name": "Inventory Sync", "description": "Synchronizes inventory levels across warehouses", "maxRetries": 4, "timeoutSeconds": 90 }
```

---

### Full-Text Search Tests

#### 72. Basic full-text search — single word

```
GET /api/v1/workflows?search=payment
```

**Expected:** `200 OK` — returns workflows containing "payment" in name OR description
- "Order Processing Pipeline" (description has "payment processing")
- "Payment Gateway Integration" (name has "Payment")
- Results ordered by **relevance rank** (not alphabetically)

#### 73. Stemming — search for different word forms

```
GET /api/v1/workflows?search=process
```

**Expected:** `200 OK` — PostgreSQL stemming matches:
- "Order Processing Pipeline" (matches "processing" → stem "process")
- "Payment Gateway Integration" (matches "Processes" → stem "process")

```text
This is the magic of full-text search:
  "process" matches "processing", "processes", "processed"
  LIKE search would NOT match "processing" when searching "process"
```

#### 74. Multi-word search — implicit AND

```
GET /api/v1/workflows?search=payment order
```

**Expected:** `200 OK` — returns workflows matching BOTH "payment" AND "order"
- "Order Processing Pipeline" (has both in name+description)
- `plainto_tsquery` treats multiple words as AND by default

#### 75. No results — unmatched term

```
GET /api/v1/workflows?search=kubernetes
```

**Expected:** `200 OK` with empty content:
```json
{
  "content": [],
  "totalElements": 0
}
```

#### 76. Full-text search combined with filters

```
GET /api/v1/workflows?search=payment&minRetries=3
```

**Expected:** `200 OK` — only workflows matching "payment" AND maxRetries >= 3
- "Order Processing Pipeline" (has "payment" + maxRetries=3)
- "Payment Gateway Integration" excluded (maxRetries=2 < 3)

#### 77. Full-text search combined with status filter

```
PATCH /api/v1/workflows/{payment-gateway-id}
{ "status": "ACTIVE" }

GET /api/v1/workflows?search=payment&status=ACTIVE
```

**Expected:** `200 OK` — only ACTIVE workflows matching "payment"
- "Payment Gateway Integration" (ACTIVE + matches "payment")
- "Order Processing Pipeline" excluded (still DRAFT)

#### 78. Relevance ranking — most relevant first

```
GET /api/v1/workflows?search=notification
```

**Expected:** `200 OK`
- "User Notification Service" should rank **first** (word in name = higher relevance)
- Any workflows with "notification" only in description rank lower

#### 79. Stop words are ignored

```
GET /api/v1/workflows?search=the and or
```

**Expected:** `200 OK` — empty or all results
```text
PostgreSQL ignores stop words: "the", "and", "or", "is", "a", etc.
These are too common to be meaningful for search.
```

#### 80. Compare LIKE vs Full-Text — switch to H2 profile

```
# Start with: -Dspring.profiles.active=h2
GET /api/v1/workflows?search=process
```

**Expected:** `200 OK` — LIKE search does NOT match "processing"
- LIKE `%process%` matches "processing" (substring match)
- But LIKE `%process%` won't match stemmed forms like "processed"
- Full-text search handles ALL word forms via stemming

---

<!-- New test sections will be added below as we build more features -->
