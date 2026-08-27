# REST API Design -- Production Patterns with Spring Boot

REST is not just slapping HTTP verbs on endpoints. This doc covers what you need to design APIs that are consistent, evolvable, and production-grade -- the kind that survives 3+ years of features without breaking clients.

---

# Part 1 -- Foundations (Brief)

## 1.1 Resource Modeling

**Core rule: model nouns, not verbs. Resources are things, actions are HTTP methods.**

```text
BAD  (RPC-style):                   GOOD (Resource-style):
POST /getUser                       GET  /users/{id}
POST /createOrder                   POST /orders
POST /cancelOrder                   POST /orders/{id}/cancellation  <- sub-resource for action
POST /searchProducts                GET  /products?q=shoes          <- query param for search
```

### Sub-resources = relationships

```text
GET  /users/{userId}/orders              <- orders belonging to a user
GET  /orders/{orderId}/items             <- items within an order
POST /workflows/{workflowId}/executions  <- start execution under a workflow
```

### When verbs are acceptable

Some actions don't map cleanly to CRUD. Use a "command sub-resource":

```text
POST /orders/{id}/cancellation    <- cancel order  (creates a Cancellation resource)
POST /payments/{id}/refund        <- refund payment (creates a Refund resource)
POST /users/{id}/password-reset   <- trigger reset  (creates a PasswordReset resource)
```

---

## 1.2 URI Design

### Rules

| Rule                         | Example                              |
|------------------------------|--------------------------------------|
| Plural nouns                 | `/users`, `/orders`                  |
| Lowercase, hyphens           | `/order-items` not `/orderItems`     |
| No trailing slash            | `/users` not `/users/`               |
| No file extensions           | `/users/1` not `/users/1.json`       |
| No verbs in path             | `GET /users` not `GET /getUsers`     |
| Hierarchy = path segments    | `/users/{id}/orders`                 |
| Filtering = query params     | `/products?category=electronics`     |
| API prefix                   | `/api/v1/users`                      |

### Anti-patterns

```text
/api/v1/getUserById?id=5        <- verb in path + query for identity
/api/v1/user/list               <- "list" is a verb, use GET /users
/api/v1/deleteOrder/5           <- verb + id in path
```

---

## 1.3 HTTP Methods

| Method  | Meaning           | Idempotent? | Safe? | Request Body | Typical Status        |
|---------|-------------------|-------------|-------|--------------|-----------------------|
| GET     | Read resource     | Yes         | Yes   | No           | 200 OK                |
| POST    | Create resource   | No          | No    | Yes          | 201 Created           |
| PUT     | Full replace      | Yes         | No    | Yes          | 200 OK                |
| PATCH   | Partial update    | Depends     | No    | Yes          | 200 OK                |
| DELETE  | Remove resource   | Yes         | No    | Optional     | 204 No Content        |

### Idempotent vs Safe

```text
Safe:       Does not change server state (GET, HEAD, OPTIONS)
Idempotent: Multiple identical requests = same result as one request (GET, PUT, DELETE)
POST:       Neither safe nor idempotent -- each call can create a new resource
```

---

## 1.4 Status Codes -- Quick Reference

### Success (2xx)

| Code | When                                             |
|------|--------------------------------------------------|
| 200  | Request succeeded, body contains response         |
| 201  | Resource created (POST), Location header set      |
| 202  | Accepted for async processing (not done yet)      |
| 204  | Success but no body (DELETE, PUT with no return)   |

### Client Errors (4xx)

| Code | When                                             |
|------|--------------------------------------------------|
| 400  | Bad request (validation failed, malformed JSON)   |
| 401  | Not authenticated (no token / expired token)      |
| 403  | Authenticated but not authorized                  |
| 404  | Resource not found                                |
| 405  | HTTP method not allowed on this endpoint          |
| 409  | Conflict (duplicate, version conflict)            |
| 422  | Unprocessable entity (valid JSON, invalid data)   |
| 429  | Rate limit exceeded                               |

### Server Errors (5xx)

| Code | When                                             |
|------|--------------------------------------------------|
| 500  | Internal server error (unhandled exception)       |
| 502  | Bad gateway (upstream service returned garbage)   |
| 503  | Service unavailable (overloaded, maintenance)     |
| 504  | Gateway timeout (upstream didn't respond in time) |

### Common Mistakes

```text
Problem:                                    Fix:
Return 200 for errors                       Use proper 4xx/5xx codes
Return 500 for validation errors            Use 400 or 422
Return 404 when list is empty               Return 200 with empty array []
Return 200 with { "success": false }        Use proper status codes!
```

---

# Part 2 -- Error Handling (Medium Detail)

## 2.1 Consistent Error Response Shape

Every error from your API should have the same structure. Clients should never have to guess.

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Invalid request",
  "path": "/api/v1/workflows",
  "traceId": "abc-123-def",
  "details": [
    {
      "field": "name",
      "rejected": "",
      "message": "must not be blank"
    },
    {
      "field": "maxRetries",
      "rejected": -1,
      "message": "must be >= 0"
    }
  ]
}
```

### Why this matters

```text
Without standard shape:
  Client code:
    if (response.error) ...        <- sometimes
    if (response.message) ...      <- sometimes
    if (response.errors) ...       <- sometimes
    if (response.detail) ...       <- sometimes

With standard shape:
  Client code:
    apiError = response.body       <- always same structure
    show(apiError.message)
    show(apiError.details)
```

## 2.2 Spring Boot Global Exception Handler

```java
// ErrorCode.java
public enum ErrorCode {
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    UNAUTHORIZED,
    FORBIDDEN,
    INTERNAL_ERROR,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE
}

// ApiError.java
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String traceId,
    List<FieldError> details
) {
    public record FieldError(String field, Object rejected, String message) {}
}

// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors (Bean Validation @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ApiError.FieldError> details = ex.getBindingResult()
            .getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(
                fe.getField(),
                fe.getRejectedValue(),
                fe.getDefaultMessage()))
            .toList();

        ApiError error = new ApiError(
            Instant.now(),
            400,
            ErrorCode.VALIDATION_ERROR.name(),
            "Validation failed",
            request.getRequestURI(),
            MDC.get("traceId"),
            details
        );
        return ResponseEntity.badRequest().body(error);
    }

    // Custom business exception: resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ApiError error = new ApiError(
            Instant.now(),
            404,
            ErrorCode.RESOURCE_NOT_FOUND.name(),
            ex.getMessage(),
            request.getRequestURI(),
            MDC.get("traceId"),
            null
        );
        return ResponseEntity.status(404).body(error);
    }

    // Conflict (e.g., duplicate name, version conflict)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        ApiError error = new ApiError(
            Instant.now(),
            409,
            ErrorCode.CONFLICT.name(),
            ex.getMessage(),
            request.getRequestURI(),
            MDC.get("traceId"),
            null
        );
        return ResponseEntity.status(409).body(error);
    }

    // Catch-all for unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception", ex);

        ApiError error = new ApiError(
            Instant.now(),
            500,
            ErrorCode.INTERNAL_ERROR.name(),
            "An unexpected error occurred",  // never expose internal details
            request.getRequestURI(),
            MDC.get("traceId"),
            null
        );
        return ResponseEntity.status(500).body(error);
    }
}
```

### Key principle: never leak internals in 500 errors

```text
BAD:   "message": "NullPointerException at UserService.java:42"
GOOD:  "message": "An unexpected error occurred"    + log the real error server-side
```

---

# Part 3 -- Partial Updates with PATCH (Medium Detail)

## 3.1 PUT vs PATCH

```text
PUT /workflows/1
{
  "name": "My Workflow",          <- must send ALL fields
  "description": "Updated",
  "maxRetries": 3,
  "timeout": 30,
  "active": true
}

PATCH /workflows/1
{
  "description": "Updated"        <- send ONLY what changed
}
```

### When to use which

```text
PUT:   Client always knows the full state. Simpler. Idempotent.
       Problem: over-the-wire waste, risk of accidentally nulling fields.

PATCH: Client sends only changes. More efficient.
       Problem: how do you distinguish "field not sent" from "set to null"?
```

## 3.2 JSON Merge Patch (RFC 7386)

The simplest PATCH approach. Rules:

```text
Field present with value    -> update to that value
Field present with null     -> set to null (or remove)
Field absent                -> leave unchanged
```

```json
// Current state
{ "name": "Flow A", "description": "Old desc", "timeout": 30 }

// PATCH body
{ "description": "New desc" }

// Result
{ "name": "Flow A", "description": "New desc", "timeout": 30 }
```

## 3.3 Type-Safe PATCH in Spring Boot

The problem: how do you know if a field was sent as `null` vs not sent at all?

```java
// Use Optional to distinguish "not sent" from "sent as null"
public record WorkflowPatchRequest(
    Optional<String> name,          // absent = don't touch
    Optional<String> description,   // Optional.empty() = set null
    Optional<Integer> maxRetries
) {}

// Service
public Workflow patchWorkflow(Long id, WorkflowPatchRequest patch) {
    Workflow workflow = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Workflow", id));

    patch.name().ifPresent(workflow::setName);
    patch.description().ifPresent(workflow::setDescription);
    patch.maxRetries().ifPresent(workflow::setMaxRetries);

    return repository.save(workflow);
}
```

### Alternative: use a Map

```java
@PatchMapping("/workflows/{id}")
public ResponseEntity<WorkflowResponse> patch(
        @PathVariable Long id,
        @RequestBody Map<String, Object> fields) {
    // Only fields present in the map get updated
    Workflow updated = workflowService.patch(id, fields);
    return ResponseEntity.ok(mapper.toResponse(updated));
}
```

---

# Part 4 -- API Versioning (Medium Detail)

## 4.1 Why Version?

```text
V1 clients expect:   { "name": "John Doe" }
V2 splits name:      { "firstName": "John", "lastName": "Doe" }

Without versioning:  V1 clients BREAK
With versioning:     V1 and V2 coexist
```

## 4.2 Strategies

### Strategy 1: URI Path Versioning (Most Common)

```text
GET /api/v1/users
GET /api/v2/users
```

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    @GetMapping("/{id}")
    public UserV1Response getUser(@PathVariable Long id) { ... }
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    @GetMapping("/{id}")
    public UserV2Response getUser(@PathVariable Long id) { ... }
}
```

**Pros:** Simple, visible, easy to route.
**Cons:** URL changes, cache-friendly but duplicates endpoints.
**Used by:** Twitter, Facebook, Google, Stripe.

### Strategy 2: Custom Header Versioning

```text
GET /api/users
X-API-Version: 2
```

```java
@GetMapping(value = "/{id}", headers = "X-API-Version=1")
public UserV1Response getUserV1(@PathVariable Long id) { ... }

@GetMapping(value = "/{id}", headers = "X-API-Version=2")
public UserV2Response getUserV2(@PathVariable Long id) { ... }
```

**Pros:** Clean URLs.
**Cons:** Hidden, harder to test in browser.
**Used by:** Microsoft Azure APIs.

### Strategy 3: Accept Header (Content Negotiation)

```text
GET /api/users
Accept: application/vnd.myapp.v2+json
```

```java
@GetMapping(value = "/{id}", produces = "application/vnd.myapp.v1+json")
public UserV1Response getUserV1(@PathVariable Long id) { ... }

@GetMapping(value = "/{id}", produces = "application/vnd.myapp.v2+json")
public UserV2Response getUserV2(@PathVariable Long id) { ... }
```

**Pros:** RESTful purist approach.
**Cons:** Complex, hard to test.
**Used by:** GitHub API.

### Strategy 4: Query Parameter

```text
GET /api/users?version=2
```

**Pros:** Easy to switch.
**Cons:** Easy to forget, not in path.

## 4.3 Deprecation Strategy

```text
Phase 1:  V2 released, V1 still works
Phase 2:  V1 returns Deprecation header:  Deprecation: true
                                          Sunset: 2025-06-01
Phase 3:  V1 logs warnings server-side
Phase 4:  V1 returns 410 Gone
```

### Recommendation for FlowForge

**Use URI versioning** (`/api/v1/...`). It's the most practical for a learning project:
- Easy to test with any HTTP client
- Clear routing
- Most common in production

---

# Part 5 -- Idempotency (Detailed)

## 5.1 The Problem

```text
Client sends:       POST /orders    { item: "laptop", qty: 1 }
Network timeout:    Client doesn't know if server received it
Client retries:     POST /orders    { item: "laptop", qty: 1 }

Without idempotency:  TWO orders created
With idempotency:     ONE order created, second request returns same result
```

## 5.2 How It Works

```text
1. Client generates a unique key:  Idempotency-Key: abc-123-def
2. Client sends request with key
3. Server checks: have I seen this key before?
   - NO  -> process request, store result with key
   - YES -> return stored result without re-processing
```

```text
Request 1:
  POST /orders
  Idempotency-Key: abc-123
  Body: { item: "laptop" }
                                    Server:
                                    key "abc-123" not found
                                    -> process order
                                    -> store: abc-123 = { orderId: 42, status: 201 }
                                    -> return 201 { orderId: 42 }

Request 2 (retry):
  POST /orders
  Idempotency-Key: abc-123
  Body: { item: "laptop" }
                                    Server:
                                    key "abc-123" FOUND
                                    -> return stored: 201 { orderId: 42 }
                                    -> NO duplicate order
```

## 5.3 Spring Boot Implementation

### Database-backed approach

```sql
CREATE TABLE idempotency_keys (
    idempotency_key  VARCHAR(64)  PRIMARY KEY,
    http_status      INT          NOT NULL,
    response_body    TEXT,
    created_at       TIMESTAMP    DEFAULT NOW(),
    expires_at       TIMESTAMP    NOT NULL
);
```

```java
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {
    @Id
    private String idempotencyKey;
    private int httpStatus;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private Instant createdAt;
    private Instant expiresAt;
}

// IdempotencyService.java
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public <T> ResponseEntity<T> executeIdempotent(
            String key,
            Class<T> responseType,
            Supplier<ResponseEntity<T>> action) {

        // 1. Check if key already processed
        Optional<IdempotencyRecord> existing = repository.findById(key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            T body = objectMapper.readValue(record.getResponseBody(), responseType);
            return ResponseEntity.status(record.getHttpStatus()).body(body);
        }

        // 2. Execute the action
        ResponseEntity<T> result = action.get();

        // 3. Store the result
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setHttpStatus(result.getStatusCode().value());
        record.setResponseBody(objectMapper.writeValueAsString(result.getBody()));
        record.setCreatedAt(Instant.now());
        record.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        repository.save(record);

        return result;
    }
}

// Usage in Controller
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody OrderCreateRequest request) {

    return idempotencyService.executeIdempotent(
        idempotencyKey,
        OrderResponse.class,
        () -> {
            Order order = orderService.create(request);
            return ResponseEntity.status(201).body(mapper.toResponse(order));
        }
    );
}
```

### Redis-backed approach (for high throughput)

```java
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);

    public <T> ResponseEntity<T> executeIdempotent(
            String key,
            Class<T> responseType,
            Supplier<ResponseEntity<T>> action) {

        String redisKey = "idempotency:" + key;

        // Try to get existing result
        String cached = redis.opsForValue().get(redisKey);
        if (cached != null) {
            CachedResponse cr = objectMapper.readValue(cached, CachedResponse.class);
            T body = objectMapper.readValue(cr.body(), responseType);
            return ResponseEntity.status(cr.status()).body(body);
        }

        // Race condition guard: SET NX (set if not exists)
        Boolean acquired = redis.opsForValue()
            .setIfAbsent(redisKey + ":lock", "processing", Duration.ofSeconds(30));

        if (Boolean.FALSE.equals(acquired)) {
            // Another request is processing this key right now
            return ResponseEntity.status(409).build(); // or retry
        }

        try {
            ResponseEntity<T> result = action.get();

            CachedResponse cr = new CachedResponse(
                result.getStatusCode().value(),
                objectMapper.writeValueAsString(result.getBody())
            );
            redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(cr), TTL);
            return result;
        } finally {
            redis.delete(redisKey + ":lock");
        }
    }

    record CachedResponse(int status, String body) {}
}
```

## 5.4 Which Methods Need Idempotency?

```text
GET:     Already idempotent (safe + idempotent)       -> no key needed
PUT:     Already idempotent (full replace)             -> no key needed
DELETE:  Already idempotent (delete same thing twice)  -> no key needed
PATCH:   Depends (increment operations are NOT)        -> sometimes needed
POST:    NOT idempotent (creates new resource)         -> ALWAYS needs key
```

## 5.5 Real-World Usage

```text
Stripe:    Idempotency-Key header for all POST requests (charges, refunds)
PayPal:    PayPal-Request-Id header
AWS:       ClientToken for CreateStack, RunInstances
Razorpay:  X-Idempotency-Key header
```

---

# Part 6 -- Bulk APIs (Detailed)

## 6.1 Why Bulk APIs?

```text
Without Bulk:
  POST /users  { "name": "Alice" }      -> 1 HTTP round trip
  POST /users  { "name": "Bob" }        -> 1 HTTP round trip
  POST /users  { "name": "Charlie" }    -> 1 HTTP round trip
  ...repeat 1000 times                  -> 1000 round trips, 1000 TCP connections

With Bulk:
  POST /users/bulk
  [
    { "name": "Alice" },
    { "name": "Bob" },
    { "name": "Charlie" },
    ...998 more
  ]                                     -> 1 HTTP round trip
```

### When to use Bulk APIs

```text
- Importing data (CSV upload, migration)
- Batch status updates (mark 100 notifications as read)
- Multi-resource creation (add items to cart)
- Admin operations (disable 50 accounts)
- IoT / telemetry (device sends 1000 readings at once)
```

## 6.2 Design Patterns

### Pattern 1: All-or-Nothing (Transactional)

```text
POST /users/bulk
Request:  [A, B, C]
B fails validation
Response: 400 -- NONE created (A and C rolled back)
```

```java
@PostMapping("/bulk")
@Transactional
public ResponseEntity<List<UserResponse>> bulkCreate(
        @Valid @RequestBody List<UserCreateRequest> requests) {

    if (requests.size() > 100) {
        throw new BadRequestException("Max 100 items per bulk request");
    }

    List<User> users = requests.stream()
        .map(userService::create)
        .toList();

    return ResponseEntity.status(201)
        .body(users.stream().map(mapper::toResponse).toList());
}
```

**Use when:** Data integrity matters (financial transactions, order creation).

### Pattern 2: Partial Success (Per-Item Status)

```text
POST /users/bulk
Request:  [A, B, C]
B fails validation
Response: 207 Multi-Status
  A -> 201 Created
  B -> 400 Validation Error
  C -> 201 Created
```

```java
// BulkResponse.java
public record BulkResponse<T>(
    int totalRequested,
    int succeeded,
    int failed,
    List<BulkItemResult<T>> results
) {}

public record BulkItemResult<T>(
    int index,           // position in original request
    int status,          // HTTP status for this item
    T data,              // response body (if success)
    ApiError error       // error details (if failed)
) {}

// Controller
@PostMapping("/bulk")
public ResponseEntity<BulkResponse<UserResponse>> bulkCreate(
        @RequestBody List<UserCreateRequest> requests) {

    if (requests.size() > 100) {
        throw new BadRequestException("Max 100 items per bulk request");
    }

    List<BulkItemResult<UserResponse>> results = new ArrayList<>();
    int succeeded = 0, failed = 0;

    for (int i = 0; i < requests.size(); i++) {
        try {
            User user = userService.create(requests.get(i));
            results.add(new BulkItemResult<>(i, 201, mapper.toResponse(user), null));
            succeeded++;
        } catch (ValidationException e) {
            results.add(new BulkItemResult<>(i, 400, null, toApiError(e)));
            failed++;
        } catch (ConflictException e) {
            results.add(new BulkItemResult<>(i, 409, null, toApiError(e)));
            failed++;
        }
    }

    BulkResponse<UserResponse> response = new BulkResponse<>(
        requests.size(), succeeded, failed, results
    );

    // 207 if mixed, 201 if all succeeded, 400 if all failed
    int httpStatus = failed == 0 ? 201 : succeeded == 0 ? 400 : 207;
    return ResponseEntity.status(httpStatus).body(response);
}
```

**Use when:** Independence between items (user imports, notifications).

### Pattern 3: Async Bulk (for large datasets)

```text
POST /users/bulk-import
Request:  [10,000 users]
Response: 202 Accepted
          { "jobId": "job-abc-123", "statusUrl": "/jobs/job-abc-123" }

Client polls:
  GET /jobs/job-abc-123
  { "status": "PROCESSING", "progress": 65, "total": 10000, "succeeded": 6500 }

Later:
  GET /jobs/job-abc-123
  { "status": "COMPLETED", "total": 10000, "succeeded": 9995, "failed": 5 }
```

```java
@PostMapping("/bulk-import")
public ResponseEntity<JobResponse> bulkImport(
        @RequestBody List<UserCreateRequest> requests) {

    String jobId = UUID.randomUUID().toString();
    // Queue the job for async processing
    bulkJobService.submitJob(jobId, requests);

    JobResponse response = new JobResponse(
        jobId,
        "QUEUED",
        requests.size(),
        "/api/v1/jobs/" + jobId
    );
    return ResponseEntity.accepted().body(response);
}

@GetMapping("/jobs/{jobId}")
public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    return ResponseEntity.ok(bulkJobService.getStatus(jobId));
}
```

**Use when:** Large datasets, long processing time, file uploads.

## 6.3 Bulk Delete and Bulk Update

```java
// Bulk Delete
@DeleteMapping("/bulk")
public ResponseEntity<BulkResponse<Void>> bulkDelete(
        @RequestBody List<Long> ids) {

    if (ids.size() > 100) {
        throw new BadRequestException("Max 100 items per bulk request");
    }
    // Process deletions...
    return ResponseEntity.ok(bulkDeleteService.delete(ids));
}

// Bulk Update (e.g., mark notifications as read)
@PatchMapping("/bulk")
public ResponseEntity<BulkResponse<NotificationResponse>> bulkUpdate(
        @RequestBody BulkUpdateRequest request) {
    // request.ids = [1, 2, 3]
    // request.fields = { "read": true }
    return ResponseEntity.ok(notificationService.bulkPatch(request));
}
```

## 6.4 Production Considerations

```text
1. SIZE LIMITS:      Always cap bulk size (50-1000 items max)
2. TIMEOUTS:         Large bulks -> use async pattern
3. VALIDATION:       Validate all items BEFORE processing any (for all-or-nothing)
4. RATE LIMITING:    Count bulk as N requests for rate limit purposes
5. IDEMPOTENCY:      Bulk + idempotency = each item needs its own idempotency key
6. LOGGING:          Log batch ID + per-item results for debugging
7. PROGRESS:         For async, provide progress endpoint
8. PARTIAL RETRY:    Return failed item indexes so client can retry only those
```

---

# Part 7 -- Pagination (Detailed)

## 7.1 Why Pagination?

```text
GET /executions    <- 10 million rows
Response: 2 GB JSON, server OOM, client crashes

GET /executions?page=0&size=20    <- 20 rows
Response: 4 KB JSON, fast, predictable
```

**Rule: NEVER return unbounded lists from an API.**

## 7.2 Offset/Page-Number Pagination

The simplest approach. Used by most APIs.

```text
GET /executions?page=0&size=20     <- first 20 rows
GET /executions?page=1&size=20     <- rows 21-40
GET /executions?page=2&size=20     <- rows 41-60
```

### SQL under the hood

```sql
SELECT * FROM executions
ORDER BY created_at DESC
LIMIT 20 OFFSET 40;         -- page=2, size=20
```

### Spring Boot Implementation

```java
@GetMapping("/executions")
public ResponseEntity<PageResponse<ExecutionResponse>> listExecutions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    // Cap page size to prevent abuse
    size = Math.min(size, 100);

    Page<Execution> result = executionRepository.findAll(
        PageRequest.of(page, size, Sort.by("createdAt").descending())
    );

    return ResponseEntity.ok(toPageResponse(result));
}

// Standard page response wrapper
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
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
            page.isFirst(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
```

### Response

```json
{
  "content": [ ... 20 items ... ],
  "page": 2,
  "size": 20,
  "totalElements": 15847,
  "totalPages": 793,
  "first": false,
  "last": false,
  "hasNext": true,
  "hasPrevious": true
}
```

### The Problem with Offset Pagination

```text
Page 1: OFFSET 0   -> DB scans 0 rows, returns 20         FAST
Page 10: OFFSET 180 -> DB scans 180 rows, returns 20       OK
Page 500: OFFSET 9980 -> DB scans 9980 rows, returns 20    SLOW
Page 50000: OFFSET 999980 -> DB scans 999980 rows          VERY SLOW

The deeper you paginate, the slower it gets.
DB must scan and discard all rows before the offset.
```

```text
Also: data shifting problem
  Page 1: [A, B, C, D, E]    <- user reads page 1
  New item X inserted at top
  Page 2: [E, F, G, H, I]    <- E appears AGAIN (shifted from page 1)
  Item E is seen twice!
```

**Use offset pagination when:**
- Total count is needed (admin dashboards, tables with page numbers)
- Dataset is small-to-medium (< 100K rows)
- Random page access is needed ("jump to page 50")

---

## 7.3 Cursor/Keyset Pagination

Solves the offset problem. Used by Twitter, Facebook, Slack for feeds and timelines.

```text
Instead of: "give me page 5"
You say:    "give me 20 items after item X"
```

### How it works

```text
Request 1:
  GET /executions?size=20
  Response: [...20 items...], cursor = "eyJpZCI6MTAwfQ=="  (base64 of last item's sort key)

Request 2:
  GET /executions?size=20&cursor=eyJpZCI6MTAwfQ==
  Response: [...next 20 items...], cursor = "eyJpZCI6MTIwfQ=="

Request 3:
  GET /executions?size=20&cursor=eyJpZCI6MTIwfQ==
  Response: [...next 20 items...], cursor = null  (no more data)
```

### SQL under the hood

```sql
-- Offset:  scans ALL rows before offset
SELECT * FROM executions ORDER BY created_at DESC LIMIT 20 OFFSET 10000;

-- Keyset:  uses index, starts exactly where needed
SELECT * FROM executions
WHERE created_at < '2025-01-15T10:30:00Z'  -- cursor value
ORDER BY created_at DESC
LIMIT 20;
```

### Spring Boot Implementation

```java
// Cursor is a Base64-encoded JSON of the last item's sort values
public record CursorPageResponse<T>(
    List<T> content,
    int size,
    String nextCursor,    // null if no more data
    boolean hasNext
) {}

@GetMapping("/executions")
public ResponseEntity<CursorPageResponse<ExecutionResponse>> listExecutions(
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String cursor) {

    size = Math.min(size, 100);

    // Decode cursor
    Instant cursorTime = null;
    Long cursorId = null;
    if (cursor != null) {
        CursorData decoded = decodeCursor(cursor);
        cursorTime = decoded.createdAt();
        cursorId = decoded.id();
    }

    // Fetch size + 1 to know if there's a next page
    List<Execution> items;
    if (cursorTime != null) {
        items = executionRepository
            .findByCreatedAtBeforeOrderByCreatedAtDescIdDesc(
                cursorTime, cursorId, PageRequest.of(0, size + 1));
    } else {
        items = executionRepository
            .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, size + 1));
    }

    boolean hasNext = items.size() > size;
    if (hasNext) {
        items = items.subList(0, size);  // remove the extra item
    }

    String nextCursor = hasNext
        ? encodeCursor(items.get(items.size() - 1))
        : null;

    return ResponseEntity.ok(new CursorPageResponse<>(
        items.stream().map(mapper::toResponse).toList(),
        size,
        nextCursor,
        hasNext
    ));
}

// Cursor encoding/decoding
private String encodeCursor(Execution last) {
    CursorData data = new CursorData(last.getCreatedAt(), last.getId());
    return Base64.getEncoder().encodeToString(
        objectMapper.writeValueAsBytes(data));
}

private CursorData decodeCursor(String cursor) {
    byte[] decoded = Base64.getDecoder().decode(cursor);
    return objectMapper.readValue(decoded, CursorData.class);
}

record CursorData(Instant createdAt, Long id) {}
```

### Repository with Keyset Query

```java
@Query("""
    SELECT e FROM Execution e
    WHERE (e.createdAt < :cursorTime)
       OR (e.createdAt = :cursorTime AND e.id < :cursorId)
    ORDER BY e.createdAt DESC, e.id DESC
    """)
List<Execution> findByCreatedAtBeforeOrderByCreatedAtDescIdDesc(
    @Param("cursorTime") Instant cursorTime,
    @Param("cursorId") Long cursorId,
    Pageable pageable);
```

### Comparison

```text
Feature             Offset              Cursor/Keyset
--------------------------------------------------------------
Random page access  YES (page=50)       NO (forward/back only)
Performance         Degrades at depth   Constant O(log n)
Data consistency    Items can shift     No duplicates/missed
Total count         YES (COUNT(*))      NO (expensive)
Implementation      Simple              More complex
Best for            Admin tables        Feeds, timelines, logs
Index required      Nice to have        MUST have
```

### When to use Cursor Pagination

```text
- Social media feeds (Twitter, Instagram)
- Chat message history (Slack)
- Log/audit trails
- Real-time event streams
- Any dataset > 100K rows where users scroll forward
- Mobile apps with infinite scroll
```

---

## 7.4 Slice vs Page in Spring Data

```text
Page<T>:
  - Runs data query + COUNT(*) query
  - Returns totalElements, totalPages
  - 2 SQL queries per request

Slice<T>:
  - Runs data query only (fetches size+1)
  - Returns hasNext (true/false)
  - 1 SQL query per request
  - Better performance for large tables
```

```java
// Using Slice (no count query)
Slice<Execution> findByStatus(ExecutionStatus status, Pageable pageable);

// Using Page (with count query)
Page<Execution> findByWorkflowId(Long workflowId, Pageable pageable);
```

**Rule of thumb:**
- Need "Page 3 of 50"? Use `Page`
- Need "Load more" / infinite scroll? Use `Slice` or Cursor

---

# Part 8 -- Filtering (Detailed)

## 8.1 Static Filtering

Hardcoded query parameters mapped to specific fields.

```text
GET /executions?status=FAILED&workflowId=42
```

```java
// Repository method -- simple and readable
List<Execution> findByStatusAndWorkflowId(
    ExecutionStatus status, Long workflowId, Pageable pageable);

// Controller
@GetMapping("/executions")
public ResponseEntity<PageResponse<ExecutionResponse>> list(
        @RequestParam(required = false) ExecutionStatus status,
        @RequestParam(required = false) Long workflowId,
        Pageable pageable) {

    Page<Execution> page;
    if (status != null && workflowId != null) {
        page = repo.findByStatusAndWorkflowId(status, workflowId, pageable);
    } else if (status != null) {
        page = repo.findByStatus(status, pageable);
    } else if (workflowId != null) {
        page = repo.findByWorkflowId(workflowId, pageable);
    } else {
        page = repo.findAll(pageable);
    }
    return ResponseEntity.ok(PageResponse.from(page, mapper::toResponse));
}
```

### Problem: Combinatorial Explosion

```text
3 filters = 8 combinations (2^3)
5 filters = 32 combinations
7 filters = 128 combinations

You can't write 128 repository methods!
```

## 8.2 Dynamic Filtering with JPA Specifications

Spring Data JPA Specification = reusable, composable query predicates.

```java
// ExecutionSpecifications.java -- each filter is a separate Specification
public class ExecutionSpecifications {

    public static Specification<Execution> hasStatus(ExecutionStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Execution> hasWorkflowId(Long workflowId) {
        return (root, query, cb) ->
            workflowId == null ? null : cb.equal(root.get("workflowId"), workflowId);
    }

    public static Specification<Execution> startedAfter(Instant after) {
        return (root, query, cb) ->
            after == null ? null : cb.greaterThanOrEqualTo(root.get("startedAt"), after);
    }

    public static Specification<Execution> startedBefore(Instant before) {
        return (root, query, cb) ->
            before == null ? null : cb.lessThanOrEqualTo(root.get("startedAt"), before);
    }

    public static Specification<Execution> createdByUser(String userId) {
        return (root, query, cb) ->
            userId == null ? null : cb.equal(root.get("createdBy"), userId);
    }
}

// Repository -- just extend JpaSpecificationExecutor
public interface ExecutionRepository
        extends JpaRepository<Execution, Long>,
                JpaSpecificationExecutor<Execution> {
}

// Service -- compose specifications dynamically
public Page<Execution> findFiltered(ExecutionFilterRequest filter, Pageable pageable) {
    Specification<Execution> spec = Specification
        .where(ExecutionSpecifications.hasStatus(filter.status()))
        .and(ExecutionSpecifications.hasWorkflowId(filter.workflowId()))
        .and(ExecutionSpecifications.startedAfter(filter.startedAfter()))
        .and(ExecutionSpecifications.startedBefore(filter.startedBefore()))
        .and(ExecutionSpecifications.createdByUser(filter.createdBy()));

    return executionRepository.findAll(spec, pageable);
}

// Controller -- clean, no combinatorial explosion
@GetMapping("/executions")
public ResponseEntity<PageResponse<ExecutionResponse>> list(
        @ModelAttribute ExecutionFilterRequest filter,
        Pageable pageable) {

    Page<Execution> page = executionService.findFiltered(filter, pageable);
    return ResponseEntity.ok(PageResponse.from(page, mapper::toResponse));
}

// Filter DTO
public record ExecutionFilterRequest(
    ExecutionStatus status,
    Long workflowId,
    Instant startedAfter,
    Instant startedBefore,
    String createdBy
) {}
```

### How Specifications compose to SQL

```text
GET /executions?status=FAILED&workflowId=42&startedAfter=2025-01-01T00:00:00Z

Generated SQL:
SELECT * FROM executions
WHERE status = 'FAILED'
  AND workflow_id = 42
  AND started_at >= '2025-01-01T00:00:00Z'
ORDER BY created_at DESC
LIMIT 20;
```

### Alternative: Criteria API (lower-level, same concept)

```java
public List<Execution> findFiltered(ExecutionFilterRequest filter) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Execution> cq = cb.createQuery(Execution.class);
    Root<Execution> root = cq.from(Execution.class);

    List<Predicate> predicates = new ArrayList<>();

    if (filter.status() != null) {
        predicates.add(cb.equal(root.get("status"), filter.status()));
    }
    if (filter.workflowId() != null) {
        predicates.add(cb.equal(root.get("workflowId"), filter.workflowId()));
    }
    if (filter.startedAfter() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), filter.startedAfter()));
    }

    cq.where(predicates.toArray(new Predicate[0]));
    cq.orderBy(cb.desc(root.get("createdAt")));

    return entityManager.createQuery(cq)
        .setMaxResults(filter.size())
        .setFirstResult(filter.page() * filter.size())
        .getResultList();
}
```

### Alternative: QueryDSL (type-safe, most elegant)

```java
// QueryDSL generates Q-classes from entities at compile time
QExecution execution = QExecution.execution;

BooleanBuilder where = new BooleanBuilder();

if (filter.status() != null) {
    where.and(execution.status.eq(filter.status()));
}
if (filter.workflowId() != null) {
    where.and(execution.workflowId.eq(filter.workflowId()));
}
if (filter.startedAfter() != null) {
    where.and(execution.startedAt.goe(filter.startedAfter()));
}

Page<Execution> result = executionRepository.findAll(where, pageable);
```

### Comparison

```text
Approach          Type-Safe?  Complexity  Best For
--------------------------------------------------------------
Static queries    Yes         Low         Few filters, simple APIs
Specifications    Yes         Medium      Most Spring Boot projects
Criteria API      Yes         High        Full JPA control needed
QueryDSL          Yes         Medium      Complex queries, large codebases
```

---

# Part 9 -- Sorting (Detailed)

## 9.1 Single-Column Sort

```text
GET /executions?sort=createdAt,desc
GET /executions?sort=status,asc
```

### Spring Boot -- works out of the box with Pageable

```java
// Spring auto-binds ?sort=field,direction to Pageable
@GetMapping("/executions")
public ResponseEntity<PageResponse<ExecutionResponse>> list(Pageable pageable) {
    // pageable already contains Sort from query params
    Page<Execution> page = executionRepository.findAll(pageable);
    return ResponseEntity.ok(PageResponse.from(page, mapper::toResponse));
}
```

```text
GET /executions?page=0&size=20&sort=createdAt,desc

Spring resolves Pageable:
  page = 0
  size = 20
  sort = createdAt DESC
```

## 9.2 Multi-Column Sort

```text
GET /executions?sort=status,asc&sort=createdAt,desc
```

This means: sort by status ascending FIRST, then by createdAt descending within each status group.

```sql
-- Generated SQL:
SELECT * FROM executions
ORDER BY status ASC, created_at DESC
LIMIT 20;
```

## 9.3 Whitelist Allowed Sort Fields

**Critical security concern:** never allow arbitrary sort fields.

```text
BAD:   GET /executions?sort=password,asc          <- sorts by password column!
BAD:   GET /executions?sort=internalScore,desc     <- leaks internal data
```

```java
@Component
public class SortValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "createdAt", "updatedAt", "status", "workflowId", "startedAt", "completedAt"
    );

    public Sort validateAndBuild(Sort requestedSort) {
        List<Sort.Order> validOrders = requestedSort.stream()
            .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
            .toList();

        if (validOrders.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt"); // default sort
        }
        return Sort.by(validOrders);
    }
}

// Controller
@GetMapping("/executions")
public ResponseEntity<PageResponse<ExecutionResponse>> list(
        @PageableDefault(size = 20, sort = "createdAt",
                         direction = Sort.Direction.DESC) Pageable pageable) {

    Sort validatedSort = sortValidator.validateAndBuild(pageable.getSort());
    Pageable validatedPageable = PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize(), validatedSort);

    Page<Execution> page = executionRepository.findAll(validatedPageable);
    return ResponseEntity.ok(PageResponse.from(page, mapper::toResponse));
}
```

## 9.4 Sorting + Indexes (Performance)

```text
Sort without index:
  DB does "filesort" -- loads ALL matching rows into memory, sorts, returns top 20
  Slow for large datasets

Sort with index:
  DB walks the index in order -- returns first 20 directly
  Fast, O(log n)
```

```sql
-- If you frequently sort/filter by these:
CREATE INDEX idx_executions_status_created
ON executions (status, created_at DESC);

-- This index serves:
-- WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 20
-- Very fast: index scan, no filesort
```

### Verify with EXPLAIN

```sql
EXPLAIN ANALYZE
SELECT * FROM executions
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 20;

-- WITHOUT index: Seq Scan -> Sort -> Limit   (slow)
-- WITH index:    Index Scan                   (fast)
```

---

# Part 10 -- Searching (Detailed)

## 10.1 Simple Search with LIKE/ILIKE

```text
GET /workflows?q=payment
```

```java
// Repository
@Query("""
    SELECT w FROM Workflow w
    WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(w.description) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
Page<Workflow> search(@Param("query") String query, Pageable pageable);

// Controller
@GetMapping("/workflows")
public ResponseEntity<PageResponse<WorkflowResponse>> search(
        @RequestParam(required = false) String q,
        Pageable pageable) {

    Page<Workflow> page;
    if (q != null && !q.isBlank()) {
        page = workflowRepository.search(q.trim(), pageable);
    } else {
        page = workflowRepository.findAll(pageable);
    }
    return ResponseEntity.ok(PageResponse.from(page, mapper::toResponse));
}
```

### Problem with LIKE '%term%'

```text
LIKE '%payment%'
  -> CANNOT use a B-Tree index (leading wildcard)
  -> Full table scan on every search
  -> 10 million rows = very slow

LIKE 'payment%'
  -> CAN use a B-Tree index (prefix match)
  -> Fast, but only matches start of string
```

## 10.2 PostgreSQL Full-Text Search

Better than LIKE for text search. Uses inverted indexes.

```sql
-- Add a tsvector column
ALTER TABLE workflows ADD COLUMN search_vector tsvector;

-- Populate it
UPDATE workflows SET search_vector =
    to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''));

-- Create GIN index
CREATE INDEX idx_workflows_search ON workflows USING GIN(search_vector);

-- Search with ranking
SELECT *, ts_rank(search_vector, plainto_tsquery('english', 'payment processing')) AS rank
FROM workflows
WHERE search_vector @@ plainto_tsquery('english', 'payment processing')
ORDER BY rank DESC
LIMIT 20;
```

### Spring Boot with PostgreSQL Full-Text Search

```java
@Query(value = """
    SELECT w.*, ts_rank(w.search_vector,
        plainto_tsquery('english', :query)) AS rank
    FROM workflows w
    WHERE w.search_vector @@ plainto_tsquery('english', :query)
    ORDER BY rank DESC
    """, nativeQuery = true)
List<Workflow> fullTextSearch(@Param("query") String query, Pageable pageable);
```

### Capabilities

```text
Feature             LIKE          PostgreSQL FTS     Elasticsearch
----------------------------------------------------------------------
Prefix match        Yes           Yes                Yes
Substring match     Yes (slow)    No                 Yes
Stemming            No            Yes (run/running)  Yes
Ranking/relevance   No            Yes                Yes (advanced)
Fuzzy matching      No            No                 Yes (typo tolerance)
Faceted search      No            No                 Yes
Auto-complete       No            Partial            Yes
Performance (10M)   Very slow     Good               Excellent
Setup complexity    None          Low                High
```

## 10.3 When to use Elasticsearch

```text
Use PostgreSQL FTS when:
  - Simple keyword search
  - < 10 million rows
  - No need for facets/aggregations
  - Don't want another infrastructure component

Use Elasticsearch when:
  - Complex search (fuzzy, synonyms, auto-complete, facets)
  - > 10 million rows
  - Search is a core feature (e-commerce, document search)
  - Need real-time analytics/aggregations
  - Multi-language search
```

### Architecture with Elasticsearch

```text
Write path:
  App -> PostgreSQL (source of truth)
      -> Kafka/CDC -> Elasticsearch (search index)

Read path:
  Search query -> Elasticsearch (fast, ranked results)
  Detail query -> PostgreSQL (full data)
```

---

# Part 11 -- Combining Everything (Dynamic Querying)

## 11.1 The Real-World API

In production, a single endpoint combines ALL of the above:

```text
GET /executions?
    workflowId=42                          <- filter
    &status=FAILED                         <- filter
    &startedAfter=2025-01-01               <- filter (range)
    &q=payment                             <- search
    &sort=startedAt,desc                   <- sort
    &sort=status,asc                       <- multi-sort
    &page=0                                <- pagination
    &size=20                               <- pagination
```

## 11.2 Spring Boot Implementation

```java
// Filter + Search DTO
public record ExecutionSearchRequest(
    ExecutionStatus status,
    Long workflowId,
    Instant startedAfter,
    Instant startedBefore,
    String createdBy,
    String q                  // search query
) {}

// Service -- combines Specifications + search + pagination + sorting
@Service
@RequiredArgsConstructor
public class ExecutionQueryService {

    private final ExecutionRepository repository;
    private final SortValidator sortValidator;

    public Page<Execution> query(ExecutionSearchRequest filter, Pageable pageable) {
        // Validate sort fields
        Sort validatedSort = sortValidator.validateAndBuild(pageable.getSort());
        Pageable validatedPageable = PageRequest.of(
            pageable.getPageNumber(),
            Math.min(pageable.getPageSize(), 100),
            validatedSort);

        // Build specification
        Specification<Execution> spec = Specification
            .where(hasStatus(filter.status()))
            .and(hasWorkflowId(filter.workflowId()))
            .and(startedAfter(filter.startedAfter()))
            .and(startedBefore(filter.startedBefore()))
            .and(createdByUser(filter.createdBy()))
            .and(searchText(filter.q()));

        return repository.findAll(spec, validatedPageable);
    }

    // Search as a Specification
    private Specification<Execution> searchText(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) return null;
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}

// Controller -- clean and simple
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionQueryService queryService;
    private final ExecutionMapper mapper;

    @GetMapping
    public ResponseEntity<PageResponse<ExecutionResponse>> list(
            @ModelAttribute ExecutionSearchRequest filter,
            @PageableDefault(size = 20, sort = "createdAt",
                direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Execution> page = queryService.query(filter, pageable);
        return ResponseEntity.ok(PageResponse.from(page, mapper::toResponse));
    }
}
```

## 11.3 Database Index Strategy

```text
For the query:
  WHERE status = ? AND workflow_id = ? AND started_at >= ?
  ORDER BY started_at DESC

Optimal composite index:
  CREATE INDEX idx_exec_filter ON executions (workflow_id, status, started_at DESC);

For search + filter:
  WHERE status = ? AND (name ILIKE '%term%' OR description ILIKE '%term%')

  B-Tree index won't help with ILIKE '%term%'
  Use PostgreSQL GIN trigram index:
  CREATE EXTENSION pg_trgm;
  CREATE INDEX idx_exec_name_trgm ON executions USING GIN (name gin_trgm_ops);
```

---

# Summary -- Decision Matrix

| Topic         | Simple Project          | Production System           |
|---------------|-------------------------|-----------------------------|
| Pagination    | Offset (Page/Slice)     | Cursor for feeds + Offset for admin |
| Filtering     | Static queries          | JPA Specifications or QueryDSL |
| Sorting       | Single column           | Multi-column + whitelist + index |
| Searching     | LIKE                    | PostgreSQL FTS or Elasticsearch |
| Error format  | Ad-hoc                  | Standard ApiError shape      |
| Versioning    | URI path (`/v1/`)       | URI path + Deprecation headers |
| Idempotency   | Not needed              | DB/Redis for all POST endpoints |
| Bulk APIs     | All-or-nothing          | Partial success + async for large |
| PATCH         | Map-based               | Optional fields or JSON Merge Patch |

---

# FlowForge Application

In our FlowForge practice project, we will implement ALL of these:

```text
Workflow CRUD          -> Resource modeling, HTTP methods, status codes
Error responses        -> Global exception handler, standard error shape
Workflow update        -> PUT (full) + PATCH (partial)
API evolution          -> /api/v1/ prefix, versioning strategy
Execution creation     -> POST with Idempotency-Key header
Bulk step creation     -> POST /workflows/{id}/steps/bulk (partial success)
Execution history      -> Cursor pagination (large dataset)
Workflow listing       -> Offset pagination (admin dashboard)
Execution filtering    -> JPA Specifications (status, workflowId, date range)
Execution sorting      -> Multi-column sort with whitelist
Workflow search        -> PostgreSQL FTS (name + description)
Combined query         -> Filter + Search + Sort + Paginate in one endpoint
```

This gives us a real reason to implement each pattern rather than just reading about it.
