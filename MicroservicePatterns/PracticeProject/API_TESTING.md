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

<!-- New test sections will be added below as we build more features -->
