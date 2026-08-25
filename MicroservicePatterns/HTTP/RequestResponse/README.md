# HTTP Request & Response — Deep Dive

Every backend developer works with HTTP requests and responses daily, but surprisingly few understand every component in depth. This doc covers the full anatomy — what every part is, how it works internally, and when each matters in production.

---

# 1. The Full HTTP Request Anatomy

```text
┌─────────────────────────────────────────────────────────────┐
│  Request Line                                               │
│  POST /api/v1/orders?source=mobile HTTP/1.1                 │
│  ──── ──────────────────────────── ────────                 │
│  Method   Path + Query String       Version                 │
├─────────────────────────────────────────────────────────────┤
│  Headers                                                    │
│  Host: api.example.com                                      │
│  Content-Type: application/json                             │
│  Content-Length: 87                                         │
│  Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...             │
│  Accept: application/json                                   │
│  Cookie: sessionId=abc123; preferences=dark-mode           │
│  X-Request-ID: f4a3d2e1-...                                 │
├─────────────────────────────────────────────────────────────┤
│  (Blank line — mandatory separator)                         │
├─────────────────────────────────────────────────────────────┤
│  Body                                                       │
│  {"productId": "P-42", "quantity": 3, "address": {...}}     │
└─────────────────────────────────────────────────────────────┘
```

---

# 2. HTTP Methods

HTTP methods define the **semantics** of the operation — what the client intends to do.

## The Core Methods

| Method | Idempotent | Safe | Has Body | Use |
|---|---|---|---|---|
| GET | Yes | Yes | No (technically allowed, ignored) | Retrieve resource |
| POST | No | No | Yes | Create resource / non-idempotent action |
| PUT | Yes | No | Yes | Replace entire resource |
| PATCH | No | No | Yes | Partially update resource |
| DELETE | Yes | No | No (usually) | Remove resource |
| HEAD | Yes | Yes | No | Same as GET but response has no body — check existence, headers |
| OPTIONS | Yes | Yes | No | Get allowed methods, CORS preflight |

**Idempotent**: making the same request N times = same result as making it once
**Safe**: does not change server state

```text
GET /users/42          → "give me user 42" — safe, idempotent
POST /users            → "create a user" — NOT idempotent (creates a new user each time)
PUT /users/42          → "replace user 42 with this body" — idempotent (same result each time)
PATCH /users/42        → "update name field of user 42" — typically NOT idempotent
DELETE /users/42       → "delete user 42" — idempotent (first call deletes, subsequent calls = 404 or no-op)
```

## GET vs POST — Common Confusion

```text
GET:
  Data in URL (query string)
  Logged in access logs, cached by browsers, bookmarkable
  Max URL length ~2048 chars (practical browser limit)
  No body (by convention — some servers ignore GET body)
  NEVER use for sensitive data (passwords, tokens)

POST:
  Data in request body
  Not logged (body usually omitted from access logs)
  Not cached by default
  No length limit
  Used for: creating resources, form submission, sensitive data
```

## PUT vs PATCH — Important Distinction

```text
User resource:
  { "id": 42, "name": "Alice", "email": "alice@example.com", "role": "admin" }

PUT /users/42 with body: { "name": "Alice Smith" }
  → REPLACES entire resource: { "id": 42, "name": "Alice Smith" }
  → email and role are GONE (null/missing)

PATCH /users/42 with body: { "name": "Alice Smith" }
  → MERGES: { "id": 42, "name": "Alice Smith", "email": "alice@example.com", "role": "admin" }
  → Only name is changed, everything else preserved
```

---

# 3. URL Structure — Path and Query Parameters

```text
https://api.example.com/api/v1/orders/ORDER-42/items?status=pending&page=2&limit=20
│       │               │           │         │     └──────────────────────────────┘
│       │               │           │         │              Query String
│       │               │           │         │
│       │               │           │    Path Parameter (ORDER-42 is the order ID)
│       │               │     Path Parameter (/orders/{orderId}/items)
│       │          Base Path (/api/v1)
│       Host
Scheme (HTTPS)
```

## Path Parameters

Path parameters are **part of the resource identifier**. They identify WHICH resource.

```text
/api/users/{userId}             → identifies a specific user
/api/orders/{orderId}/items     → identifies items of a specific order
/api/products/{productId}       → identifies a specific product

Examples:
GET /api/users/42               → user with ID 42
GET /api/orders/ORDER-42/items  → items of order ORDER-42
DELETE /api/products/P-100      → delete product P-100
```

In Spring Boot:
```java
@GetMapping("/users/{userId}")
public User getUser(@PathVariable Long userId) { ... }

@GetMapping("/orders/{orderId}/items")
public List<Item> getItems(@PathVariable String orderId) { ... }
```

**When to use path params:** when the parameter IS the identity of the resource. The URL represents the "address" of a specific thing.

## Query Parameters

Query parameters come after `?` and are `key=value` pairs separated by `&`. They are used for **filtering, sorting, pagination, and optional modifiers** — not for identifying the resource.

```text
/api/orders?status=pending          → filter orders by status
/api/products?category=electronics&minPrice=100&maxPrice=500  → filter + range
/api/users?page=2&limit=20&sort=createdAt&order=desc          → pagination + sort
/api/search?q=java+concurrency&highlight=true                  → search query
```

In Spring Boot:
```java
@GetMapping("/orders")
public Page<Order> getOrders(
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int limit
) { ... }
```

**Path param vs Query param decision rule:**

```text
"Is this parameter REQUIRED to identify the resource?" → Path parameter
"Is this parameter optional, or used to filter/sort/paginate?" → Query parameter

Good:    GET /users/42?fields=name,email    (42 = identity, fields = optional modifier)
Bad:     GET /users?id=42                  (id should be path param for a specific user)
Bad:     GET /users/42/status/pending      (status is a filter, not a path segment)
Good:    GET /users/42/orders?status=pending (42 = user identity, status = filter)
```

---

# 4. Headers — The Metadata Layer

Headers are **key-value metadata** sent with every request and response. They control caching, authentication, content negotiation, connection behavior, and more.

## Common Request Headers

### Host (required in HTTP/1.1)
```text
Host: api.example.com

Why: one server can host multiple domains (virtual hosting)
     The Host header tells the server WHICH site the request is for
```

### Content-Type
```text
Content-Type: application/json          → body is JSON
Content-Type: application/xml           → body is XML
Content-Type: multipart/form-data       → file upload
Content-Type: application/x-www-form-urlencoded → HTML form submission
Content-Type: text/plain                → plain text
Content-Type: application/octet-stream  → binary data
Content-Type: application/json; charset=UTF-8  → with charset

Rule: ALWAYS set Content-Type when sending a body (POST, PUT, PATCH)
```

### Accept
```text
Accept: application/json                → client wants JSON response
Accept: application/json, text/xml      → JSON preferred, XML acceptable
Accept: */*                             → any format accepted
Accept: application/json;q=0.9, text/html;q=0.8  → quality values (q= = preference)
```

Content negotiation: server reads `Accept`, returns response in best matching format. If no match → `406 Not Acceptable`.

### Authorization
```text
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...  → JWT Bearer token (most common in APIs)
Authorization: Basic dXNlcjpwYXNz            → username:password Base64-encoded (legacy)
Authorization: ApiKey abc123def456           → API key (non-standard, varies by API)
```

### User-Agent
```text
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0

Used by servers to:
  - Analytics and logging (which browsers/clients are using the API)
  - Content adaptation (mobile vs desktop)
  - Bot detection
  - A/B testing by client type
```

### Accept-Encoding
```text
Accept-Encoding: gzip, deflate, br

Client signals it can accept compressed responses
Server may compress response body with gzip/Brotli → smaller payload
Decompression is transparent to the client
```

### Other Common Request Headers
```text
X-Request-ID: uuid          → correlation ID for distributed tracing
X-Forwarded-For: client-ip  → original client IP (from proxy/load balancer)
Referer: https://...        → page that linked to this request
Origin: https://app.example → CORS — where the request originates from
If-None-Match: "etag-value" → conditional request (only send if changed)
If-Modified-Since: date     → conditional request (only send if modified)
```

## Common Response Headers

```text
Content-Type: application/json; charset=UTF-8  → format of response body
Content-Length: 1234                            → exact body size in bytes
Content-Encoding: gzip                          → body is compressed with gzip
Location: /api/users/42                         → new resource URL (used with 201/3xx)
Cache-Control: max-age=60, public               → caching instructions
ETag: "abc123"                                  → resource version identifier
Last-Modified: Mon, 01 Jan 2024 00:00:00 GMT    → when resource last changed
Set-Cookie: sessionId=abc; HttpOnly; Secure     → sets a cookie on the client
Access-Control-Allow-Origin: https://app.com    → CORS permission
Retry-After: 60                                 → rate limit: retry after N seconds
X-RateLimit-Remaining: 99                       → remaining API calls
```

### Custom Headers

Headers starting with `X-` were traditionally custom headers (not standardized). Modern practice:

```text
X-Request-ID:    correlation ID (distributed tracing)
X-User-ID:       propagated user context (internal services)
X-API-Version:   client API version
X-B3-TraceId:    Zipkin distributed trace ID
X-RateLimit-*:   rate limiting metadata (non-standard, varies by API)
```

---

# 5. Cookies

Cookies are small pieces of data the server sends to the browser, stored on the client, and **automatically sent back on every subsequent request** to the same domain.

## How Cookies Work

```text
Request 1 (first visit):
Client: GET /homepage HTTP/1.1

Response 1 (server sets cookies):
Server: HTTP/1.1 200 OK
        Set-Cookie: sessionId=abc123; HttpOnly; Secure; Path=/; Max-Age=3600
        Set-Cookie: preferences=dark-mode; Path=/; Max-Age=86400

Request 2 (same domain, later):
Client: GET /dashboard HTTP/1.1
        Cookie: sessionId=abc123; preferences=dark-mode
        (browser automatically includes all matching cookies!)
```

## Cookie Attributes

```text
Set-Cookie: name=value; attribute1; attribute2=value
```

| Attribute | Meaning |
|---|---|
| `HttpOnly` | **NOT accessible via JavaScript** (document.cookie). Prevents XSS attacks stealing cookies. Always set for session cookies. |
| `Secure` | Cookie only sent over **HTTPS** connections. Never sent over HTTP. Always set for sensitive cookies. |
| `SameSite=Strict` | Cookie NOT sent on cross-site requests (e.g., link from another site). Maximum CSRF protection. |
| `SameSite=Lax` | Cookie sent on top-level navigation (clicking links) but NOT on sub-resource cross-site requests. Good default. |
| `SameSite=None` | Cookie sent on ALL cross-site requests. Requires `Secure`. Used for embedded widgets, OAuth flows. |
| `Max-Age=3600` | Cookie expires in 3600 seconds. Stored on disk (survives browser close). |
| `Expires=date` | Cookie expires at specific date. Older alternative to Max-Age. |
| `Path=/api` | Cookie only sent for requests to `/api` and sub-paths. |
| `Domain=.example.com` | Cookie sent to `example.com` and all subdomains. |

## Session Cookie vs Persistent Cookie

```text
Session cookie (no Max-Age, no Expires):
  Deleted when browser is closed
  Lives only in memory
  Used for: login sessions (user should be logged out when browser closes)

Persistent cookie (has Max-Age or Expires):
  Stored on disk
  Survives browser close
  Used for: "remember me" feature, preferences, analytics
```

## Cookies vs Headers for Authentication

```text
Cookie (sessionId or JWT in cookie):
  ✓ Automatically sent by browser — no JavaScript needed
  ✓ HttpOnly flag prevents XSS stealing
  ✓ Works for traditional web apps (server-side sessions)
  ✗ Vulnerable to CSRF (use SameSite + CSRF token to mitigate)
  ✗ Doesn't work well for mobile apps or cross-domain APIs

Authorization header (Bearer JWT):
  ✓ Not subject to CSRF (not auto-sent)
  ✓ Works for mobile apps, SPAs, cross-domain APIs
  ✗ Must be stored in JavaScript (localStorage/sessionStorage) — XSS risk
  ✗ Manually attached to every request
```

---

# 6. Request Body

The body carries the **payload** of the request. Only relevant for POST, PUT, PATCH (and occasionally DELETE).

## JSON Body

```text
POST /api/orders HTTP/1.1
Content-Type: application/json

{
    "productId": "P-42",
    "quantity": 3,
    "shippingAddress": {
        "street": "123 Main St",
        "city": "Mumbai"
    }
}
```

Most common for REST APIs. JSON is parsed into objects server-side.

## Form Data (URL-encoded)

```text
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=alice&password=secret&remember=true
```

Used by HTML forms. Data is encoded like query strings. Suitable for simple key-value data.

## Multipart Form Data (File Upload)

```text
POST /api/profile/photo HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="userId"

42
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="photo"; filename="avatar.png"
Content-Type: image/png

[binary image data]
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

Used when uploading files alongside other fields. Each "part" can have different content types.

## Binary Body

```text
POST /api/data HTTP/1.1
Content-Type: application/octet-stream
Content-Length: 1024

[raw binary bytes]
```

Used for: protobuf payloads (gRPC), raw file upload, compiled artifacts.

---

# 7. Status Codes — Complete Reference

Status codes are 3-digit numbers that tell the client exactly what happened. The first digit indicates the category. **Choosing the right status code is part of good API design** — it allows clients to handle errors programmatically without parsing error messages.

```text
1xx — Informational   (request received, process continuing)
2xx — Success         (request received, understood, and accepted)
3xx — Redirection     (further action needed to complete the request)
4xx — Client Error    (request contains bad syntax or cannot be fulfilled — client's fault)
5xx — Server Error    (server failed to fulfil an apparently valid request — server's fault)
```

---

## 1xx — Informational

These are interim responses. Rare in everyday REST APIs but important to know.

---

### 100 Continue
```text
Meaning: Server received request headers. Client should proceed to send the body.

When used:
  Client sends large request body (e.g., 100MB file upload)
  Client first sends headers with "Expect: 100-continue"
  Server validates headers (auth, content-type, size limits)
  If OK → 100 Continue → client sends body
  If not OK → 413 or 401 directly → client saves bandwidth (never sends body)

Without this: client blindly sends 100MB, server rejects at the headers → wasted upload
```

---

### 101 Switching Protocols
```text
Meaning: Server is upgrading the connection to a different protocol.

Most common use: WebSocket upgrade
  Client sends:
    GET /chat HTTP/1.1
    Upgrade: websocket
    Connection: Upgrade
    Sec-WebSocket-Key: abc123

  Server responds:
    HTTP/1.1 101 Switching Protocols
    Upgrade: websocket
    Connection: Upgrade
    Sec-WebSocket-Accept: xyz789
  
  After this: connection is no longer HTTP — it's a full-duplex WebSocket.
  Also used for: HTTP/2 upgrade (h2c — cleartext HTTP/2 upgrade from HTTP/1.1)
```

---

### 103 Early Hints
```text
Meaning: Server sends preliminary response headers before the final response.
         Client can start prefetching resources immediately.

Example:
  Client: GET /index.html
  Server sends 103 BEFORE it finishes processing:
    103 Early Hints
    Link: </style.css>; rel=preload; as=style
    Link: </app.js>; rel=preload; as=script
  
  Client: starts downloading style.css and app.js immediately
  Server: finishes processing, sends:
    200 OK
    [full HTML body]
  
  Performance win: CSS/JS downloaded in parallel with server processing time.
  Supported by: Cloudflare, nginx (modern versions), Chrome 103+
```

---

## 2xx — Success

---

### 200 OK
```text
Meaning: Request succeeded. Response body contains the requested data.

The default success code. Use for:
  GET   → body contains the resource
  PUT   → body contains the updated resource
  PATCH → body contains the updated resource
  POST  → when resource isn't "created" per se (e.g., search, login, action)

Common mistake: returning 200 for every response including errors.
  BAD:  200 OK  { "status": "error", "message": "Not found" }
  GOOD: 404 Not Found  { "error": "User not found" }
  (status codes are the protocol-level error communication — use them!)
```

---

### 201 Created
```text
Meaning: Resource was successfully created.

Always pair with: Location header pointing to the new resource.

  POST /api/users
  → 201 Created
     Location: /api/users/42
     Content-Type: application/json
     { "id": 42, "name": "Alice", ... }

Use for: POST creating a resource, PUT creating a resource at a specific URL
NOT for: POST actions that don't create a persistent resource (use 200 instead)
```

---

### 202 Accepted
```text
Meaning: Request accepted and queued for processing, but NOT yet completed.
         Response body usually contains a job ID or polling URL.

Use for: asynchronous / long-running operations

Example:
  POST /api/reports/generate
  → 202 Accepted
     Location: /api/jobs/job-abc123
     { "jobId": "job-abc123", "status": "queued", "estimatedTimeSeconds": 30 }
  
  Client polls: GET /api/jobs/job-abc123
  → 200 OK  { "status": "running", "progress": 60 }
  → 200 OK  { "status": "complete", "reportUrl": "/api/reports/R-42" }

Common for: report generation, bulk data imports, video encoding, email campaigns
```

---

### 204 No Content
```text
Meaning: Request succeeded, but there is NO response body.

Use for:
  DELETE  → resource deleted successfully (nothing to return)
  PUT/PATCH → update succeeded, body not needed
  OPTIONS → CORS preflight response (no body needed)
  Webhooks → receiver acknowledging receipt

  DELETE /api/users/42
  → 204 No Content  (no body at all — not even {})

Important: 204 response MUST NOT include a body.
           Do NOT return { "success": true } — that's 200 OK.
```

---

### 206 Partial Content
```text
Meaning: Server returning only part of the resource (range request).

Triggered by: Range request header from client
  GET /api/files/video.mp4
  Range: bytes=0-1048575   ← first 1MB

  → 206 Partial Content
     Content-Range: bytes 0-1048575/52428800  ← first 1MB of 50MB total
     Content-Length: 1048576
     [1MB of video data]

Use for: video/audio streaming, large file download resume, paginated binary data
         Every video player (YouTube, Netflix) uses 206 to stream video in chunks.

Without 206: video player would have to download entire file before playing.
```

---

## 3xx — Redirection

These tell the client to look for the resource elsewhere.

---

### 301 Moved Permanently
```text
Meaning: Resource has permanently moved to a new URL.
         Browsers, crawlers, and caches update their bookmarks/index to the new URL.
         Google transfers SEO "link juice" to the new URL.

  Response:
    301 Moved Permanently
    Location: https://new.example.com/page

IMPORTANT: browsers may change POST to GET when following 301 redirect.
           If method must be preserved → use 308 instead.

Use for: domain migrations (http → https), URL restructuring, permanent renames
```

---

### 302 Found
```text
Meaning: Resource is temporarily at a different URL.
         Client should continue using the original URL for future requests.

  Location: /login    (temporary, use /dashboard next time)

IMPORTANT: browsers may change POST to GET when following 302.
           If method must be preserved → use 307 instead.

Use for: post-login redirect, maintenance pages, A/B test variants
```

---

### 303 See Other
```text
Meaning: Response to the request is at another URL, and client MUST use GET to retrieve it.
         Always causes method change to GET (intentional).

Classic use: POST-Redirect-GET pattern (prevents form resubmission):
  POST /orders (submit order)
  → 303 See Other
     Location: /orders/ORD-42/confirmation
  
  Browser: GET /orders/ORD-42/confirmation  (changed to GET automatically)
  User: sees confirmation page
  User refreshes: GET /orders/ORD-42/confirmation  (safe — no duplicate order!)

Without 303: browser refresh on POST page → "Resend form data?" dialog → duplicate order!
```

---

### 304 Not Modified
```text
Meaning: Resource hasn't changed since the version the client already has cached.
         Client should use its cached copy. Response body is EMPTY.

How it works (conditional request):
  Client: GET /api/products
          If-None-Match: "etag-v42"   ← has this cached
  
  Server: checks if current ETag is still "etag-v42"
  → Unchanged: 304 Not Modified  (no body — huge bandwidth saving!)
  → Changed:   200 OK + new body + new ETag

Practical impact: 304 response is ~200 bytes vs 200 response of 50KB
                  Your CDN and browser cache rely on this heavily.
```

---

### 307 Temporary Redirect
```text
Meaning: Temporarily redirect to another URL. HTTP method MUST NOT change.

  POST /api/orders → 307 → POST /api/v2/orders  (POST preserved!)

vs 302: 302 allows browsers to silently change POST to GET.
        307 strictly requires the same method.

Use for: API versioning redirects, load balancer routing when method matters
```

---

### 308 Permanent Redirect
```text
Meaning: Permanently redirect to another URL. HTTP method MUST NOT change.

  POST /api/v1/orders → 308 → POST /api/v2/orders  (POST preserved!)

vs 301: 301 allows browsers to change POST to GET.
        308 strictly requires the same method.
        308 also transfers SEO value like 301.

Use for: permanent API endpoint moves where POST/PUT/PATCH must be preserved
```

---

## 4xx — Client Errors

The client sent something wrong. **It is the client's responsibility to fix it.**

---

### 400 Bad Request
```text
Meaning: Request is malformed and the server cannot process it.
         The error is in the request syntax/structure itself.

Causes:
  • Malformed JSON body  (missing comma, unclosed bracket)
  • Missing required field
  • Invalid data type (string where int expected)
  • URL encoding issues
  • Invalid query parameter format

  POST /api/users
  Body: { "name": "Alice", "age": "not-a-number" }
  → 400 Bad Request
     { "error": "INVALID_TYPE", "field": "age", "message": "age must be an integer" }

Difference from 422:
  400 = malformed (can't even parse it)
  422 = parseable but business rules violated (see 422 below)

Response body MUST include: specific error message, which field failed, why
```

---

### 401 Unauthorized ⚠️ Misleadingly named — really means "Unauthenticated"
```text
Meaning: Client has not authenticated, or authentication credentials are invalid/expired.
         Server doesn't know WHO the client is.

Causes:
  • No Authorization header at all
  • JWT token expired
  • JWT signature invalid
  • API key incorrect
  • Basic auth credentials wrong

  GET /api/orders
  Authorization: Bearer expired.jwt.token
  → 401 Unauthorized
     WWW-Authenticate: Bearer realm="api", error="token_expired"
     { "error": "TOKEN_EXPIRED", "message": "Token expired, please re-authenticate" }

Required: WWW-Authenticate header (tells client how to authenticate)
Client action: re-authenticate (redirect to login, refresh the token)

vs 403: 401 = "I don't know who you are" | 403 = "I know who you are but NO"
```

---

### 402 Payment Required
```text
Meaning: Payment is required to access this resource.
         Originally reserved for digital payment systems — rarely used as intended.

Modern real-world use:
  SaaS APIs that enforce subscription tiers:
    GET /api/analytics/advanced
    → 402 Payment Required
       { "error": "SUBSCRIPTION_REQUIRED", 
         "message": "Advanced analytics requires a Pro plan",
         "upgradeUrl": "https://app.example.com/billing" }

Used by: Stripe, GitHub Copilot, various SaaS APIs for premium feature gating
```

---

### 403 Forbidden
```text
Meaning: Client is authenticated (server knows who they are) but NOT authorized
         to access this resource or perform this action.

Causes:
  • User trying to access another user's private data
  • Non-admin trying to call admin endpoint
  • CORS: origin not in allowed list
  • IP allowlist: client IP not permitted
  • Resource-level permissions: user has "read" but not "write"
  • Account suspended or deactivated

  GET /api/users/99/profile   (logged in as user 42)
  → 403 Forbidden
     { "error": "INSUFFICIENT_PERMISSIONS",
       "message": "You can only access your own profile" }

Security pattern — return 404 instead of 403:
  403 /api/admin/secret-config → reveals the endpoint EXISTS (security info leak)
  404 /api/admin/secret-config → hides existence entirely (security through obscurity)
  
  Use 404 when even revealing that a resource exists is a security concern.
```

---

### 404 Not Found
```text
Meaning: Requested resource does not exist on this server.

Causes:
  • Genuinely doesn't exist (wrong ID, deleted resource)
  • Deliberately returned instead of 403 (hide existence)
  • Misconfigured routing
  • Typo in URL

  GET /api/users/999999
  → 404 Not Found
     { "error": "USER_NOT_FOUND", "message": "User 999999 does not exist" }

Best practice: include details about WHAT was not found — "User not found" not just "Not found"
Do NOT: return 404 for empty list results
  GET /api/users?role=admin → 0 users → 200 OK  []  (not 404!)
  404 means the ENDPOINT doesn't exist, not that the collection is empty
```

---

### 405 Method Not Allowed
```text
Meaning: HTTP method used is not supported for this endpoint.

  DELETE /api/users   (deleting all users — endpoint only allows GET/POST)
  → 405 Method Not Allowed
     Allow: GET, POST  ← required header listing allowed methods

Common causes:
  • Calling DELETE on a read-only resource
  • Calling POST on a resource that only accepts GET
  • Misconfigured router

Required: Always include Allow header listing the supported methods.
```

---

### 406 Not Acceptable
```text
Meaning: Server cannot produce content in any format the client's Accept header accepts.

  GET /api/products
  Accept: application/xml   ← client wants XML
  
  Server: "I only serve JSON"
  → 406 Not Acceptable

Use when: implementing content negotiation and no format matches.
Practical: most modern REST APIs just ignore Accept and return JSON regardless.
           406 is only returned when content negotiation is strictly enforced.
```

---

### 408 Request Timeout
```text
Meaning: Server timed out waiting for the client to complete the request.
         Server is closing the connection.

Cause: Client started sending a request but was too slow to finish
       (slow upload, network interruption mid-request)

  Client: starts uploading a 1GB file...
  Client: connection pauses for 120 seconds (network issue)
  Server: "I'm not waiting anymore"
  → 408 Request Timeout
     Connection: close

Client action: retry the request (ideally with exponential backoff)
```

---

### 409 Conflict
```text
Meaning: Request conflicts with the current state of the server.
         Cannot be processed in the current state.

Causes:
  • Duplicate unique value (email already registered)
  • Optimistic locking conflict (version mismatch)
  • Business rule conflict (can't cancel a shipped order)
  • Concurrent modification (two users editing same resource)

Examples:
  POST /api/users  { "email": "alice@example.com" }
  → 409 Conflict
     { "error": "EMAIL_ALREADY_EXISTS", "field": "email" }

  PUT /api/orders/O-42 with If-Match: "v3"  (current version is v4)
  → 409 Conflict
     { "error": "VERSION_CONFLICT", "currentVersion": "v4" }

  POST /api/orders/O-42/cancel  (order already shipped)
  → 409 Conflict
     { "error": "INVALID_STATE_TRANSITION",
       "currentStatus": "SHIPPED", "message": "Cannot cancel a shipped order" }
```

---

### 410 Gone
```text
Meaning: Resource PERMANENTLY no longer available. It's been deleted and won't come back.
         Different from 404 which could mean "never existed" or "temporarily unavailable".

  GET /api/users/42  (user was deleted, data purged per GDPR)
  → 410 Gone
     { "error": "RESOURCE_PERMANENTLY_DELETED" }

Why use 410 over 404:
  404 = "doesn't exist" (reason unknown — could be a typo)
  410 = "DID exist, is GONE, stop trying" → crawlers de-index it, clients stop retrying

Use for: GDPR deletion, deprecated API versions, retired features
```

---

### 411 Length Required
```text
Meaning: Server requires Content-Length header but it wasn't provided.

  POST /api/upload  (no Content-Length header)
  → 411 Length Required

Rare in practice — most HTTP clients set Content-Length automatically.
Relevant for: chunked transfer encoding scenarios, streaming uploads.
```

---

### 412 Precondition Failed
```text
Meaning: A conditional header's precondition evaluated to false — request not executed.

Used with If-Match (optimistic locking):
  PUT /api/products/42
  If-Match: "v5"   ← "only update if still version 5"
  
  Server: current version is "v6" (someone else updated!)
  → 412 Precondition Failed
     { "error": "PRECONDITION_FAILED",
       "currentETag": "v6",
       "message": "Resource was modified since you last read it. Re-fetch and retry." }

Also returned for: If-Unmodified-Since, If-None-Match (when match found on safe method)

Important in: optimistic concurrency control, preventing lost updates in REST APIs
```

---

### 413 Content Too Large (previously "Payload Too Large")
```text
Meaning: Request body exceeds the server's configured size limit.

  POST /api/upload  (sending 500MB file, server limit is 100MB)
  → 413 Content Too Large
     { "error": "PAYLOAD_TOO_LARGE",
       "maxSizeBytes": 104857600,
       "message": "File size exceeds the 100MB limit" }

Common server limits:
  nginx:     client_max_body_size 10m;  (default 1MB)
  Spring Boot: spring.servlet.multipart.max-file-size=10MB
  AWS ALB:   max 1MB by default for non-multipart

Clients should: check file size before uploading, inform user before sending
```

---

### 414 URI Too Long
```text
Meaning: Request URI (URL) is longer than the server can process.

  GET /api/search?ids=1,2,3,...(thousands of IDs)...
  → 414 URI Too Long

Practical limits:
  Most browsers: ~2048 characters max URL length
  nginx: client_max_uri_size  (default 8KB)

Fix: move large parameters to POST request body instead of query string
     Use POST /api/search with body: { "ids": [1, 2, 3, ...] }
```

---

### 415 Unsupported Media Type
```text
Meaning: Server cannot process the Content-Type of the request body.

  POST /api/users
  Content-Type: application/xml   ← server only accepts JSON
  [XML body]
  → 415 Unsupported Media Type
     { "error": "UNSUPPORTED_MEDIA_TYPE",
       "supported": ["application/json"],
       "received": "application/xml" }

Common causes:
  • Sending XML to a JSON-only API
  • Forgetting to set Content-Type header (server defaults may reject it)
  • Sending multipart when JSON expected

Fix: always explicitly set Content-Type: application/json for JSON bodies
```

---

### 416 Range Not Satisfiable
```text
Meaning: Server cannot serve the requested byte range (Range header invalid).

  GET /api/files/video.mp4
  Range: bytes=999999999-1000000000   ← beyond file size
  → 416 Range Not Satisfiable
     Content-Range: bytes */52428800   ← actual file size

Causes:
  • Requested range starts beyond end of file
  • Invalid range format

Relevant for: video streaming, large file downloads, download resume
```

---

### 422 Unprocessable Entity (Content)
```text
Meaning: Request is syntactically valid (parseable JSON), but fails semantic/business validation.
         Server understood the request but cannot process it due to validation errors.

  POST /api/users
  Content-Type: application/json
  {
    "email": "not-a-valid-email",
    "age": -5,
    "username": ""
  }
  → 422 Unprocessable Entity
     {
       "error": "VALIDATION_ERROR",
       "violations": [
         { "field": "email",    "message": "Invalid email format" },
         { "field": "age",      "message": "Age must be a positive number" },
         { "field": "username", "message": "Username cannot be empty" }
       ]
     }

vs 400:
  400 = structurally broken (malformed JSON, wrong Content-Type, can't parse)
  422 = valid structure, but values fail validation rules

Spring Boot: @Valid + MethodArgumentNotValidException → typically returns 422 or 400
Most common status code from form validation in REST APIs.
```

---

### 423 Locked
```text
Meaning: The resource is currently locked and cannot be accessed.
         Originally a WebDAV status code, but used in distributed systems too.

Use cases:
  • Resource is being processed/modified by another operation
  • Distributed lock held by another instance
  • File system lock (WebDAV)

  PATCH /api/orders/O-42
  → 423 Locked
     Retry-After: 5
     { "error": "RESOURCE_LOCKED",
       "message": "Order is being processed. Retry in 5 seconds.",
       "lockOwner": "worker-instance-3" }
```

---

### 425 Too Early
```text
Meaning: Server refuses to process the request because it might be replayed.

Used with: HTTP/3's 0-RTT (Early Data)
  In 0-RTT, requests sent before the TLS handshake completes could be replayed by attackers.
  Servers can reject non-idempotent requests (POST, PUT, DELETE) received via 0-RTT.

  POST /api/payments  (sent via 0-RTT early data)
  → 425 Too Early
  Client: retry after full TLS handshake completes
```

---

### 426 Upgrade Required
```text
Meaning: Client must switch to a different protocol before making this request.

  GET /api/secure-resource HTTP/1.1
  → 426 Upgrade Required
     Upgrade: TLS/1.3, HTTP/2

Use for: endpoints that REQUIRE HTTP/2 or TLS 1.3 minimum
Rare in REST APIs but relevant for high-security or performance-critical services.
```

---

### 428 Precondition Required
```text
Meaning: Server requires the request to be conditional (missing If-Match or similar header).
         Used to prevent "lost update" problems at the API design level.

  PUT /api/products/42  (no If-Match header)
  → 428 Precondition Required
     { "error": "PRECONDITION_REQUIRED",
       "message": "Include If-Match header with the resource ETag to prevent concurrent update conflicts" }

Design pattern:
  API enforces that all PUT/PATCH requests must include If-Match
  Prevents clients from accidentally overwriting concurrent changes
  Teaches clients to implement optimistic locking
```

---

### 429 Too Many Requests
```text
Meaning: Client has exceeded the rate limit. Slow down.

  GET /api/users
  → 429 Too Many Requests
     Retry-After: 60            ← wait 60 seconds before retrying
     X-RateLimit-Limit: 1000    ← requests allowed per window
     X-RateLimit-Remaining: 0   ← none left
     X-RateLimit-Reset: 1706745600  ← Unix timestamp when limit resets
     {
       "error": "RATE_LIMIT_EXCEEDED",
       "message": "1000 requests per hour limit exceeded. Retry after 60 seconds."
     }

Rate limiting strategies:
  Fixed window:   1000 req/hour (resets on the hour)
  Sliding window: 1000 req/last 60 minutes (rolling)
  Token bucket:   allows bursts up to bucket size
  Leaky bucket:   constant output rate regardless of burst

Clients MUST: implement exponential backoff on 429
              Parse Retry-After and respect it
```

---

### 431 Request Header Fields Too Large
```text
Meaning: Request headers (individually or in total) exceed the server's size limit.

Cause: Extremely common in production!
  • Too many cookies accumulated over time (bloated cookie jar)
  • Very large JWT token (too many claims, too many roles)
  • Large Authorization header (long API keys)
  • Many custom headers

nginx default limits:
  client_header_buffer_size: 1k (each header)
  large_client_header_buffers: 4 8k (total)

Real-world scenario:
  User accumulates cookies across many pages
  Total cookie size exceeds 8KB
  → 431 on every request → user appears logged out → confused users

Fix: clear old cookies, reduce JWT payload, increase server header limits
```

---

### 451 Unavailable For Legal Reasons
```text
Meaning: Resource unavailable for legal reasons (government order, DMCA, court injunction).
         Named after Fahrenheit 451 (Ray Bradbury's book about censorship).

  GET /api/content/blocked-video
  → 451 Unavailable For Legal Reasons
     Link: <https://legal.example.com/removal-notice>; rel="blocked-by"
     {
       "error": "LEGAL_RESTRICTION",
       "message": "This content is unavailable in your region due to a legal order.",
       "legalNotice": "https://legal.example.com/details"
     }

vs 403: 451 specifically means LEGAL reasons, not permission/auth reasons
Use for: GDPR right-to-be-forgotten (specific regions), copyright takedowns,
         government-ordered content restrictions, compliance requirements
```

---

## 5xx — Server Errors

The server received a valid request but failed to fulfil it. **The client is not at fault.**

---

### 500 Internal Server Error
```text
Meaning: Generic catch-all for unexpected server errors.
         Something went wrong on the server that wasn't anticipated.

Causes:
  • Unhandled exception (NullPointerException, ArrayIndexOutOfBounds)
  • Database connection failure
  • Bug in business logic
  • Out of memory
  • External service returned unexpected data

  GET /api/orders/O-42
  → 500 Internal Server Error
     { "error": "INTERNAL_ERROR",
       "requestId": "req-abc123",   ← include trace ID for debugging!
       "message": "An unexpected error occurred. Contact support with request ID." }

Best practices:
  • Never expose stack traces, DB errors, or internal paths to clients
  • Log the full exception server-side with requestId
  • Return the requestId/correlationId so users can report it
  • Alert/PagerDuty on 5xx spike (monitor 5xx rate, not just up/down)
```

---

### 501 Not Implemented
```text
Meaning: Server does not support the HTTP method or feature used in the request.
         Distinct from 405 (endpoint exists but doesn't support the method).

  TRACE /api/users
  → 501 Not Implemented
  (TRACE method not supported at all by this server)

Also used for: API endpoints planned but not yet built
  GET /api/analytics/ml-predictions
  → 501 Not Implemented  { "message": "ML predictions coming in Q2 2025" }
```

---

### 502 Bad Gateway
```text
Meaning: Server acting as a gateway/proxy received an invalid response from upstream.
         The upstream server is not reachable or returned something unexpected.

Your microservice chain:
  Client → Nginx → Spring Boot Service → Database

  Spring Boot crashes / returns garbage:
  Nginx → 502 Bad Gateway to client

Common causes:
  • Upstream service crashed
  • Upstream returned non-HTTP response (binary garbage, wrong protocol)
  • Upstream application error causing malformed HTTP response
  • nginx proxy_pass to a port with nothing listening

In Kubernetes:
  Pod not ready → 502 from ingress controller
  Pod crashloop → 502 intermittently

Monitoring: 502 spike = upstream service is broken, not the gateway itself
```

---

### 503 Service Unavailable
```text
Meaning: Server is temporarily unable to handle requests.
         Expected to be temporary — client should retry later.

Causes:
  • Server is overloaded (too many concurrent requests)
  • Server is in maintenance mode
  • Graceful shutdown in progress (new deployment rolling out)
  • Circuit breaker is open (downstream dependency failing)
  • Health check failing → load balancer removed server from pool

  GET /api/users
  → 503 Service Unavailable
     Retry-After: 30
     { "error": "SERVICE_UNAVAILABLE",
       "message": "System under maintenance. Back in 30 seconds." }

Always include: Retry-After header so clients know when to retry

In Kubernetes:
  Pod starting up (readiness probe not yet passing) → 503 from load balancer
  Rolling deployment → brief 503 if not handled properly

vs 502: 503 = THIS server is refusing | 502 = THIS server got bad response from UPSTREAM
```

---

### 504 Gateway Timeout
```text
Meaning: Server acting as gateway/proxy timed out waiting for an upstream server's response.

Your microservice chain:
  Client → API Gateway → Service A → Service B (slow DB query)

  Service B takes 35 seconds, API Gateway timeout is 30 seconds:
  → 504 Gateway Timeout to client

Common causes:
  • Slow database query (missing index, N+1 query)
  • External API call taking too long
  • Downstream service overloaded
  • Deadlock in database
  • Large data processing in downstream service

Debugging: check downstream service logs for the slow operation
           Trace with correlation/request ID across services

Timeouts to configure:
  nginx: proxy_read_timeout 60s
  Spring Boot: RestTemplate timeout, WebClient responseTimeout
  AWS ALB: idle timeout (default 60s)
  Feign: feign.client.config.default.readTimeout
```

---

### 505 HTTP Version Not Supported
```text
Meaning: Server does not support the HTTP protocol version used in the request.

  GET /api/data HTTP/3.0  (server only supports HTTP/1.1 and HTTP/2)
  → 505 HTTP Version Not Supported

Rare in practice — HTTP clients negotiate version automatically.
```

---

### 507 Insufficient Storage
```text
Meaning: Server cannot complete the request because storage is full.

  POST /api/files/upload  (disk full on server)
  → 507 Insufficient Storage
     { "error": "STORAGE_FULL",
       "message": "Upload failed: storage quota exceeded. Current usage: 50GB/50GB" }

Use for: file upload services, user storage quotas, disk-full scenarios
```

---

### 511 Network Authentication Required
```text
Meaning: Client must authenticate with the network (not the server) to proceed.
         Typically used by captive portals (hotel WiFi, airport networks).

  GET /api/data
  → 511 Network Authentication Required
     Location: http://wifi-login.hotel.com/

Not used by APIs directly — returned by network infrastructure (WiFi hotspots).
Relevant for: building apps that detect captive portals (mobile network detection).
```

---

## Key Comparisons — The Confusing Ones

### 400 vs 422 — Request Errors

```text
400 Bad Request:
  The request STRUCTURE is broken — can't even parse it.
  "I can't understand what you sent me."
  
  Examples:
    Malformed JSON: { "name": "Alice" (no closing brace)
    Wrong Content-Type: sending XML with Content-Type: application/json
    Missing Host header

422 Unprocessable Entity:
  The structure is fine, values fail validation — business rules not met.
  "I understood your request, but the data is wrong."
  
  Examples:
    Email address format invalid
    Age is negative
    Username already taken (debatable — some prefer 409 for this)
    Order total doesn't match sum of items
```

### 401 vs 403 — Auth Errors

```text
401 Unauthorized (really: Unauthenticated):
  "Who are you? Prove your identity first."
  → No token, expired token, invalid token
  → Client should re-authenticate (redirect to login)

403 Forbidden:
  "I know who you are. You're not allowed."
  → Valid token, but wrong role/permissions
  → Client should show "access denied" — NOT redirect to login
```

### 404 vs 410 — Missing Resources

```text
404 Not Found:
  Resource doesn't exist (or server won't confirm it exists)
  Could be: wrong ID, never created, temporarily unavailable, typo in URL
  Search engines: will retry crawling this URL later

410 Gone:
  Resource DID exist and has been PERMANENTLY deleted/removed
  Will not return. Ever.
  Search engines: immediately de-index this URL (better for SEO cleanup)
  Use for: deleted accounts (GDPR), deprecated API endpoints
```

### 301 vs 302 vs 307 vs 308 — Redirects

```text
                    Permanent?   Method preserved?
301 Moved Perm:       YES            NO (browser may change POST to GET)
302 Found:            NO             NO (browser may change POST to GET)  
307 Temp Redirect:    NO             YES (method always preserved)
308 Perm Redirect:    YES            YES (method always preserved)

Rule of thumb:
  Moving a GET endpoint permanently?     → 301
  Moving a POST endpoint permanently?    → 308
  Temporarily redirecting POST?          → 307
  Post-Redirect-Get pattern?             → 303
```

### 502 vs 503 vs 504 — Server/Gateway Errors

```text
In a microservice chain: Client → Gateway → Service A → Service B

502 Bad Gateway:
  Gateway received a BAD (invalid/malformed) response from Service A.
  "Upstream sent me garbage."
  Service A is up but broken.

503 Service Unavailable:
  Gateway itself (or Service A) is too busy/down to accept the connection.
  "Cannot accept connection right now."
  Service A is down or rejecting connections.

504 Gateway Timeout:
  Gateway reached Service A, Service A reached Service B,
  but Service B took TOO LONG — timeout hit before response arrived.
  "Upstream timed out."
  Service B is alive but slow.
```

---

## Correct Code for Common Scenarios

| Scenario | Correct Code | Wrong Code (common mistake) |
|---|---|---|
| Resource created successfully | 201 Created | 200 OK |
| Delete successful (nothing to return) | 204 No Content | 200 OK `{}` |
| Async operation queued | 202 Accepted | 200 OK |
| JWT token expired | 401 Unauthorized | 403 Forbidden |
| User accessing another user's data | 403 Forbidden | 401 Unauthorized |
| Email already registered | 409 Conflict | 400 Bad Request |
| Form validation failed (invalid email) | 422 Unprocessable | 400 Bad Request |
| Resource permanently deleted (GDPR) | 410 Gone | 404 Not Found |
| Rate limit hit | 429 Too Many Requests | 403 Forbidden |
| POST creating resource that already exists | 409 Conflict | 500 Internal Server Error |
| Header too large (big cookie/JWT) | 431 Header Too Large | 400 Bad Request |
| Unhandled NullPointerException | 500 Internal Server Error | 200 OK `{"error": ...}` |
| Upstream service crashed | 502 Bad Gateway | 500 Internal Server Error |
| Service restarting / overloaded | 503 Service Unavailable | 500 Internal Server Error |
| Downstream DB query timeout | 504 Gateway Timeout | 500 Internal Server Error |
| File upload too large | 413 Content Too Large | 400 Bad Request |
| Method not supported for endpoint | 405 Method Not Allowed | 404 Not Found |

---

## Spring Boot — Returning Correct Status Codes

```java
// 200 OK (default for @GetMapping)
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { return userService.find(id); }

// 201 Created
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody @Valid CreateUserRequest req) {
    User user = userService.create(req);
    URI location = URI.create("/api/users/" + user.getId());
    return ResponseEntity.created(location).body(user);  // 201 + Location header
}

// 204 No Content
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();  // 204
}

// 202 Accepted (async)
@PostMapping("/reports")
public ResponseEntity<JobDto> generateReport(@RequestBody ReportRequest req) {
    String jobId = reportService.queue(req);
    return ResponseEntity.accepted()
        .header("Location", "/api/jobs/" + jobId)
        .body(new JobDto(jobId, "QUEUED"));  // 202
}

// 409 Conflict
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody CreateUserRequest req) {
    if (userService.emailExists(req.getEmail())) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "EMAIL_ALREADY_EXISTS", "field", "email"));
    }
    ...
}

// 422 Validation Error (with @Valid)
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ValidationErrorResponse> handleValidationError(
        MethodArgumentNotValidException ex) {
    List<FieldError> errors = ex.getBindingResult().getFieldErrors()
        .stream()
        .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
        .toList();
    return ResponseEntity.unprocessableEntity()
        .body(new ValidationErrorResponse("VALIDATION_ERROR", errors));  // 422
}

// 429 Rate Limit
@ExceptionHandler(RateLimitExceededException.class)
public ResponseEntity<?> handleRateLimit(RateLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .header("X-RateLimit-Remaining", "0")
        .body(Map.of("error", "RATE_LIMIT_EXCEEDED",
                     "retryAfter", ex.getRetryAfterSeconds()));
}
```

---

# 8. Content Types — Reference

```text
application/json                    → JSON data (APIs)
application/xml                     → XML data
application/x-www-form-urlencoded   → HTML form data
multipart/form-data                 → file uploads + form fields
application/octet-stream            → raw binary (generic)
application/pdf                     → PDF file
application/zip                     → ZIP archive
application/grpc+proto              → gRPC protobuf
text/html                           → HTML page
text/plain                          → plain text
text/csv                            → CSV data
text/event-stream                   → Server-Sent Events (SSE)
image/jpeg, image/png, image/webp   → images
audio/mpeg, video/mp4               → media
application/jwt                     → JWT token in body (non-standard but used)
```

In Spring Boot:
```java
@GetMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
```

---

# Interview Preparation — Request & Response

## Q1: What is the difference between path parameters and query parameters?

**Answer:**

**Path parameters** identify the resource — they are part of the URL path that defines which specific resource is being accessed:
```
GET /users/42          → user with ID 42 (42 is the identity)
GET /orders/O-100      → order O-100
```

**Query parameters** filter, sort, or configure how the resource is returned — they come after `?` and are optional:
```
GET /users?role=admin&status=active    → filter users
GET /orders?page=2&limit=20&sort=date  → paginate and sort
```

Rule: if the parameter is required to identify the resource → path param. If it's optional or a modifier → query param.

## Q2: What is the difference between 401 and 403?

**Answer:**

- **401 Unauthorized**: "I don't know who you are." The request lacks valid authentication credentials. Send credentials and try again. (Despite the name "Unauthorized," it's really about authentication.)

- **403 Forbidden**: "I know who you are, but you can't do this." The client is authenticated but not authorized for the requested resource/action.

In practice: 401 triggers a login prompt, 403 shows an "access denied" page.

## Q3: What is the difference between PUT and PATCH?

**Answer:**

- **PUT**: replaces the **entire** resource with the request body. Fields not included in the body are set to null/absent.
- **PATCH**: applies a **partial update** — only the fields in the request body are changed; other fields are left as-is.

PUT is idempotent (sending the same PUT twice gives the same result). PATCH is typically NOT idempotent (some implementations use JSON Patch operations that could apply multiple times differently).

## Q4: What does the HttpOnly cookie attribute do and why is it important?

**Answer:**

`HttpOnly` prevents JavaScript from accessing the cookie via `document.cookie`. The browser still sends it with HTTP requests, but it's invisible to JavaScript running on the page.

This prevents XSS (Cross-Site Scripting) attacks: even if an attacker injects malicious JavaScript that runs on the page, it cannot steal `HttpOnly` cookies (like session tokens).

Always set `HttpOnly` on session cookies and authentication tokens stored as cookies.

## Q5: When would you use a 202 Accepted instead of 201 Created?

**Answer:**

- **201 Created**: use when the resource is immediately created and available. Response body or `Location` header points to the new resource.
- **202 Accepted**: use when the request was accepted but processing happens **asynchronously** — the resource isn't created yet.

Example: `POST /api/reports` that triggers a background report generation (takes 30 seconds). Return 202 with a job ID. Client polls `GET /api/jobs/{jobId}` to check status. When done, 200 with the report or 303 See Other to the report URL.

## Q6: What is content negotiation?

**Answer:**

Content negotiation is the mechanism by which client and server agree on the format of the response.

Client sends `Accept: application/json, application/xml;q=0.9, */*;q=0.5`.

Server reads this header and returns the response in the best matching format it supports. If no format matches → `406 Not Acceptable`.

`q=` values are quality factors (preference scores, 0-1, default 1.0). Higher q = more preferred.

Common example: a client that supports both JSON and XML prefers JSON (`q` not specified = 1.0), accepts XML with lower priority (`q=0.9`). Server returns JSON.
