# HTTP Caching, ETags & Keep-Alive — Deep Dive

HTTP caching is one of the most impactful performance levers in web development. Done correctly, it eliminates redundant network requests entirely. Done incorrectly, users see stale data or caches never get used.

> **HTTP caching works by letting the client (or an intermediate proxy/CDN) store responses and reuse them without hitting your server. Understanding Cache-Control, ETags, and conditional requests is essential for building fast APIs.**

---

# 1. Why Caching Matters

```text
Without caching:
  Client requests /api/products (1MB response)
  Server: queries database, serializes JSON, sends 1MB
  Client: downloads 1MB, parses JSON
  → 300ms response time, 1MB bandwidth every time

With caching (cache hit):
  Client requests /api/products
  Browser: "I have this response cached and it's still valid"
  → 0ms network time, 0 bytes transferred, instant response

With caching (revalidation):
  Client requests /api/products
  Browser: "I have this but let me check if it's still fresh"
  Server: "It hasn't changed" → 304 Not Modified (no body, ~200 bytes)
  → 50ms instead of 300ms, 0.02% of the bandwidth
```

---

# 2. Where Caching Happens

```text
Client Browser
      ↓
Browser Cache (private — only for this user)
      ↓
Forward Proxy (e.g., corporate proxy)
      ↓
CDN Edge Node (shared — all users on this edge)
      ↓
Origin Server
```

Each layer can cache based on HTTP cache headers. The same response can be cached at every layer simultaneously.

```text
Private cache:    Browser cache — only for the specific user
                  (personalized data: user profile, cart contents)

Shared/Public cache: CDN, proxy — cached for ALL users
                  (public data: product catalog, news articles, API docs)
```

---

# 3. Cache-Control Header

`Cache-Control` is the primary mechanism for controlling caching behavior. It can appear in both requests and responses.

## Response Cache-Control Directives

### `max-age=N`

```text
Cache-Control: max-age=3600

The response is "fresh" for 3600 seconds from when it was received.
Client can serve this from cache without any network request for 3600s.
After 3600s, the response is "stale" — must revalidate or re-fetch.

Examples:
  max-age=0        → immediately stale (must revalidate every time)
  max-age=60       → fresh for 1 minute
  max-age=3600     → fresh for 1 hour
  max-age=86400    → fresh for 1 day
  max-age=31536000 → fresh for 1 year (use for immutable assets with hashed filenames)
```

### `s-maxage=N`

```text
Cache-Control: max-age=3600, s-maxage=86400

s-maxage overrides max-age specifically for SHARED caches (CDNs, proxies).
max-age applies to the browser cache.

Use case: 
  Product data should be fresh in browser for 1 hour,
  but CDN can cache it for 24 hours (CDN has its own invalidation mechanism)
```

### `public` vs `private`

```text
Cache-Control: public, max-age=3600
  → response CAN be cached by shared caches (CDNs, proxies)
  → use for: product catalog, blog posts, public API responses

Cache-Control: private, max-age=3600
  → response should ONLY be cached by the browser (not CDN/proxy)
  → use for: user profile, dashboard data, anything user-specific
  → ALWAYS use private for authenticated responses with user data

Cache-Control: no-store
  → DO NOT cache this at all — not in browser, not in CDN
  → use for: sensitive data (banking, medical), one-time tokens
  → response is always re-fetched from server

Cache-Control: no-cache
  → CONFUSING NAME! Does NOT mean "don't cache"
  → means: cache the response, but ALWAYS revalidate before using it
  → cache stores the response but must check with server before serving
  → typically paired with ETag or Last-Modified
  → use for: frequently changing data where you still want bandwidth savings
```

### `must-revalidate`

```text
Cache-Control: max-age=3600, must-revalidate

When the cached response becomes stale:
  → MUST contact server to revalidate before serving
  → If server is unreachable → return 504 Gateway Timeout (not stale response)
  
Without must-revalidate:
  → Cache MAY serve stale response if server is unavailable (depends on implementation)
```

### `stale-while-revalidate=N`

```text
Cache-Control: max-age=3600, stale-while-revalidate=86400

If max-age has expired (response is stale):
  → Serve the stale response immediately to user (no wait!)
  → SIMULTANEOUSLY send a background request to refresh the cache
  → New response will be used for subsequent requests

Benefits: user always gets instant response (even if stale), cache stays fresh in background
Used by: Vercel (Next.js), Cloudflare Workers, SWR (Stale While Revalidate) pattern
```

### `immutable`

```text
Cache-Control: max-age=31536000, immutable

Tells browser: this resource will NEVER change at this URL.
Browser will not even revalidate it during the max-age window.
(Normally browsers revalidate on page reload even if max-age hasn't expired)

Use with: fingerprinted/hashed static assets
  /static/app.a3f9d2.js → immutable, max-age=1 year
  When content changes, filename changes → new URL → no cache confusion
```

## Request Cache-Control Directives

```text
Cache-Control: no-cache   → force revalidation (browser hard refresh Ctrl+F5)
Cache-Control: no-store   → don't cache this request's response
Cache-Control: max-age=0  → treat cached response as stale immediately
Cache-Control: only-if-cached → only return cached response, never hit network
```

---

# 4. ETags — Entity Tags

An ETag is an **opaque identifier** (a "version fingerprint") for a specific version of a resource. The server generates it — clients just store and return it.

```text
Server response:
  HTTP/1.1 200 OK
  Content-Type: application/json
  ETag: "a3f9d2e1b8c7"
  Cache-Control: no-cache

  {"users": [...]}
```

Client stores: the response body + the ETag value `"a3f9d2e1b8c7"`.

## Conditional Request with ETag

Next time the client wants the same resource:

```text
Client request:
  GET /api/users HTTP/1.1
  If-None-Match: "a3f9d2e1b8c7"   ← "give me users, but only if ETag has changed"

Server: computes current ETag for /api/users
  Case 1: ETag unchanged (resource hasn't changed):
    HTTP/1.1 304 Not Modified
    ETag: "a3f9d2e1b8c7"
    [NO BODY]              ← saves bandwidth — client uses cached body

  Case 2: ETag changed (resource has changed):
    HTTP/1.1 200 OK
    ETag: "b9e1f4c2a7d8"   ← new ETag
    Content-Type: application/json

    {"users": [...]}      ← new body sent
```

The key benefits:
- **Bandwidth savings**: if resource unchanged → 304 with no body (vs full response)
- **Correctness**: client always gets fresh data when it changes
- **Even with `no-cache`**: the browser must revalidate, but if ETag matches → 304 (fast!)

## Strong vs Weak ETags

```text
Strong ETag:  ETag: "a3f9d2e1b8c7"
  → byte-for-byte identical response
  → strict equality: even one byte difference = different ETag

Weak ETag:    ETag: W/"a3f9d2e1b8c7"
  → semantically equivalent (same content, possibly different formatting)
  → used when gzip/uncompressed versions should match, or minor formatting differs
  → can be used with If-None-Match but not for Range requests
```

## How to Generate ETags

```text
Option 1: MD5/SHA hash of response body
  ETag: MD5(responseBody)
  → Correct: same content = same ETag
  → Expensive: must compute entire response to know ETag

Option 2: Database version/timestamp
  ETag: last_modified_timestamp + row_version
  → Cheap: database tracks version
  → Common: ETag: "2024-01-15T10:30:00Z-v42"

Option 3: Resource version number
  ETag: "version-42"
  → Simple: increment version on every update
```

In Spring Boot:
```java
@GetMapping("/products/{id}")
public ResponseEntity<Product> getProduct(@PathVariable Long id,
                                          @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
    Product product = productService.findById(id);
    String etag = "\"" + product.getVersion() + "\"";  // e.g., "42"

    if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(304).eTag(etag).build();  // 304 Not Modified
    }

    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(CacheControl.noCache())
        .body(product);
}
```

## ETags for Optimistic Locking (If-Match)

ETags are also used to prevent lost updates ("optimistic locking" over HTTP):

```text
Step 1: Client GETs the resource
  Server response includes: ETag: "v42"

Step 2: Client modifies and PUTs back
  PUT /api/products/42 HTTP/1.1
  If-Match: "v42"     ← "only update if version is still v42"
  Content-Type: application/json

  {"name": "Updated Product", ...}

Step 3: Server checks
  Current version is still "v42" → proceed with update → 200 OK with ETag: "v43"
  Current version is "v43" → someone else updated → 412 Precondition Failed
```

This prevents the "lost update problem":
```text
Alice: GET product → gets version 42
Bob:  GET product → gets version 42
Alice: PUT product (version 42) → succeeds → version becomes 43
Bob:  PUT product (version 42) → REJECTED (412) — Bob's version is stale!
Bob must re-fetch, merge, and try again.
```

---

# 5. Last-Modified / If-Modified-Since

The older alternative to ETags — uses timestamp instead of version token.

```text
Response:
  Last-Modified: Mon, 15 Jan 2024 10:30:00 GMT

Subsequent request:
  If-Modified-Since: Mon, 15 Jan 2024 10:30:00 GMT

Server response if unchanged: 304 Not Modified
Server response if changed:   200 OK + new body + new Last-Modified
```

**ETag vs Last-Modified:**

| | ETag | Last-Modified |
|---|---|---|
| Precision | Exact version token | Second-level precision |
| Computation | Requires hashing or version tracking | Timestamp from filesystem/DB |
| Sub-second changes | Detected (exact hash) | Not detected (same second) |
| Dynamic content | Works (compute hash) | Doesn't always work well |
| Recommendation | **Prefer ETag** | Fallback when ETag impractical |

---

# 6. Practical Caching Strategies

## Strategy 1: Static Assets with Fingerprinting

```text
Files like: /static/app.a3f9d2.js (hash in filename)

Cache-Control: max-age=31536000, immutable, public

→ Cache for 1 year without revalidation
→ Content change = filename change = new URL = no stale content
→ Old hash filenames just stop being requested (no invalidation needed)
```

## Strategy 2: API Responses (Dynamic Data)

```text
User-specific data (profile, cart):
  Cache-Control: private, no-cache
  ETag: "user-42-v100"
  → Cached in browser only, always revalidated
  → If unchanged → 304 (fast and bandwidth-efficient)
  → If changed → 200 with new data

Public, infrequently changing data (product catalog):
  Cache-Control: public, max-age=300, stale-while-revalidate=3600
  ETag: "catalog-v500"
  → CDN caches for 5 minutes (fresh period)
  → After 5 minutes: serve stale immediately, refresh in background
  → Up to 1 hour of stale-while-revalidate
  → Great for high-traffic public APIs

Real-time data (live scores, stock prices):
  Cache-Control: no-store
  → No caching at all — always hit the server
```

## Strategy 3: CDN Invalidation

When data changes (e.g., product updated), you need to tell the CDN to forget its cache:

```text
AWS CloudFront invalidation:
  POST /2020-11-01/distributions/{id}/invalidations
  Path: /api/products/P-42

Cloudflare Cache Purge:
  POST /client/v4/zones/{zone}/purge_cache
  {"files": ["https://api.example.com/api/products/P-42"]}
```

Or use `s-maxage` with short TTL and accept brief staleness.

---

# 7. Connection: Keep-Alive

## The Problem Keep-Alive Solves

Without keep-alive, every HTTP request requires:

```text
1. TCP 3-way handshake (SYN → SYN-ACK → ACK)  = 1 RTT
2. TLS handshake (if HTTPS)                    = 1-2 RTTs
3. HTTP request + response                     = 1 RTT
4. TCP FIN (connection teardown)

Total: 3-5 RTTs per request!

At 100ms RTT: 300-500ms overhead just for connection setup
```

## Keep-Alive: Reuse the Connection

```text
HTTP/1.1 default behavior (persistent connections):
  Connection 1:
    GET /api/users      → response
    GET /api/orders     → response (reuses connection!)
    GET /api/products   → response (reuses connection!)
  Connection closed after timeout or explicit close

No repeated handshakes for subsequent requests on same connection.
```

HTTP/1.1 headers:

```text
Request:
  Connection: keep-alive          (or omit — it's default in HTTP/1.1)

Response:
  Connection: keep-alive
  Keep-Alive: timeout=60, max=100  → keep alive for 60s or max 100 requests
```

To explicitly close:
```text
Connection: close   → close after this response
```

## TCP Slow Start and Why Keep-Alive Matters

TCP congestion control starts every new connection conservatively (slow start) — it increases bandwidth gradually:

```text
New TCP connection bandwidth ramp-up:
  RTT 1: send 10 packets
  RTT 2: send 20 packets (doubled)
  RTT 3: send 40 packets
  RTT 4: send 80 packets (now near full bandwidth)
  ...

If you open a new connection for every request:
  → Every request starts at minimum bandwidth
  → Large responses are SLOWER than they should be

With keep-alive:
  → Connection already at full bandwidth
  → Requests use full bandwidth immediately
```

## HTTP/2 and Keep-Alive

HTTP/2 makes keep-alive even more important:

```text
HTTP/1.1:
  Keep-alive reuses connection for sequential requests
  Still one request at a time (HoL blocking)

HTTP/2:
  ONE connection handles ALL concurrent requests (multiplexing)
  Keep-alive is even MORE critical — never close this connection
  Losing the connection means re-handshaking AND losing stream state

HTTP/2 uses PING frames to keep idle connections alive:
  Client: PING frame (every N seconds)
  Server: PONG frame
  → Both sides know the connection is still alive
```

## Connection Pooling (Server-Side)

For microservices making HTTP calls to each other, maintaining connection pools is critical:

```text
Service A makes 1000 requests/second to Service B

Without connection pool:
  Each request: TCP handshake + TLS + request = ~200ms overhead
  1000 requests × 200ms overhead = 200 seconds of overhead/second
  → Impossible

With connection pool (10 connections kept alive):
  10 persistent connections, each handling ~100 requests/second
  Requests reuse existing connections immediately
  → Overhead: 0ms per request (after initial pool establishment)
```

In Spring Boot (RestTemplate / WebClient):

```java
// HTTP connection pool configuration (Apache HttpClient 5)
PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
manager.setMaxTotal(200);          // total pool size
manager.setDefaultMaxPerRoute(20); // max connections per host

CloseableHttpClient httpClient = HttpClients.custom()
    .setConnectionManager(manager)
    .evictExpiredConnections()
    .evictIdleConnections(Duration.ofSeconds(30))
    .build();

// WebClient with connection pool
HttpClient httpClient = HttpClient.create()
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
    .responseTimeout(Duration.ofSeconds(10))
    .connectionProvider(ConnectionProvider.builder("custom")
        .maxConnections(200)
        .pendingAcquireTimeout(Duration.ofSeconds(5))
        .maxIdleTime(Duration.ofSeconds(30))
        .build());
```

## Keep-Alive Timeout Tuning

```text
Server keep-alive timeout too SHORT:
  → Connections closed before reuse → defeats the purpose

Server keep-alive timeout too LONG:
  → Too many idle connections held open → memory waste on server

Load Balancer keep-alive:
  LB timeout MUST be longer than server timeout
  If LB closes first → requests sent to dying connection → 502 errors

Typical values:
  Nginx worker_connections: 65535
  Nginx keepalive_timeout: 65s
  Spring Boot: server.connection-timeout=20s
  AWS ALB: idle timeout 60s (default)

Rule: LB idle timeout > Server keepalive timeout
```

---

# 8. Putting It All Together — Caching Flow Diagram

```text
Client makes request: GET /api/products

Step 1: Check browser cache
  Is there a cached response?
  → No: proceed to network
  → Yes: Is it still fresh (max-age not expired)?
      → Yes: return cached response immediately ✓
      → No: proceed to conditional request

Step 2: Conditional request (revalidation)
  Send: If-None-Match: "etag-value"
        If-Modified-Since: date

Step 3: Server evaluates
  → Resource unchanged: 304 Not Modified (no body)
      Client: update cache freshness, return cached body ✓
  → Resource changed: 200 OK with new body + new ETag
      Client: update cache with new response ✓
```

---

# Interview Preparation — Caching

## Q1: What is the difference between Cache-Control: no-cache and no-store?

**Answer:**

Despite the confusing name, they are very different:

- **`no-store`**: do not cache at all. The response is never stored locally. Every request goes to the server and the response is discarded after use. Use for sensitive data (banking, medical).

- **`no-cache`**: cache the response, but **always validate with the server before using it**. The client stores the response and its ETag. On the next request, it sends `If-None-Match` to the server. If unchanged → 304 (fast, no bandwidth). If changed → 200 with new data. So `no-cache` still saves bandwidth but ensures freshness.

**Summary**: `no-cache` = "cache but validate". `no-store` = "never cache".

## Q2: How does an ETag work and what problem does it solve?

**Answer:**

An ETag is a version identifier for a resource, generated by the server. The server sends it in responses:
```
ETag: "a3f9d2e1"
```

On subsequent requests, the client sends it back as a conditional header:
```
If-None-Match: "a3f9d2e1"
```

The server compares the current ETag with the received one:
- Same → 304 Not Modified (no body) — saves bandwidth
- Different → 200 OK with new body and new ETag

ETags solve two problems:
1. **Bandwidth efficiency**: unchanged resources return 304 (no body), saving large response payloads
2. **Optimistic concurrency**: with `If-Match`, ETags prevent lost updates — server rejects stale writes (412 Precondition Failed)

## Q3: What is the Cache-Control: public vs private difference?

**Answer:**

- **`public`**: the response can be cached by any cache — browser, CDN, proxy servers. Use for content that's the same for all users (product catalog, blog posts, static pages).

- **`private`**: the response may only be cached by the user's browser (private cache), not shared caches like CDNs. Use for personalized data (user profile, shopping cart) where serving one user's cached data to another would be a data leak.

Important: even authenticated responses are safe with `private` because the CDN won't cache them — only the browser will, and only for that specific user's session.

## Q4: What is the stale-while-revalidate directive and when is it useful?

**Answer:**

`Cache-Control: max-age=60, stale-while-revalidate=3600`

When the response is stale (max-age expired):
- Serve the **stale response immediately** (no latency for the user)
- **In the background**, send a request to refresh the cache
- Subsequent requests use the refreshed response

This is the "SWR" pattern — users never wait for revalidation, they always get a fast (possibly slightly stale) response. The cache refreshes asynchronously.

Use case: product catalog, news articles, public dashboards where 60 seconds of staleness is acceptable but you want zero latency.

## Q5: Why does Connection: keep-alive matter for microservices performance?

**Answer:**

Each new TCP connection to an HTTP server requires:
- TCP 3-way handshake (1 RTT)
- TLS handshake (1-2 RTTs)
- TCP slow start (bandwidth ramps up from minimum)

For microservices making hundreds/thousands of HTTP calls per second, creating new connections for each request is catastrophically slow. Connection pools with keep-alive maintain persistent connections that are reused across requests — eliminating the handshake overhead completely.

HTTP/2 makes this even more critical — all requests from one service share ONE TCP connection (multiplexing). That connection MUST stay alive. Configure `maxIdleTime` in connection pool settings, and ensure load balancer idle timeouts exceed server keep-alive timeouts (to avoid 502 errors from connections closed mid-flight).

## Q6: What is optimistic locking via HTTP ETags?

**Answer:**

When a client reads a resource, the server includes its ETag (version). When the client wants to update it, it sends `If-Match: "etag"`. The server only applies the update if the current ETag matches.

If another client updated the resource in between → ETag changed → server returns `412 Precondition Failed` → the first client must re-read, merge their changes, and try again.

This prevents the "lost update" race condition without pessimistic locking (no database row locks held during the network round trip). It's a standard pattern for REST APIs that need concurrent write safety.
