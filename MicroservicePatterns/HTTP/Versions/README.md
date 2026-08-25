# HTTP Versions — HTTP/1.1 vs HTTP/2 vs HTTP/3

Understanding the differences between HTTP versions is critical for backend engineers. Each version solves specific bottlenecks of the previous one. When your API is slow, knowing the protocol layer often reveals the root cause.

---

# 1. HTTP/1.1 — The Foundation (1997)

HTTP/1.1 is a **text-based, request-response protocol over TCP**. It is still the most widely deployed version, and most REST APIs run on it.

## The Request-Response Cycle

```text
Client                          Server
  |                               |
  |── TCP Handshake (SYN/SYN-ACK/ACK) ──→|   (3 packets, ~1 RTT)
  |── TLS Handshake (if HTTPS) ──────────→|   (2 RTTs for TLS 1.2, 1 RTT for TLS 1.3)
  |                               |
  |── GET /users HTTP/1.1 ────────→|
  |                               |
  |←─ HTTP/1.1 200 OK ────────────|
  |   [headers + body]            |
  |                               |
  |── GET /orders HTTP/1.1 ───────→|   (next request must wait!)
  |                               |
  |←─ HTTP/1.1 200 OK ────────────|
```

## Key Characteristics

```text
Protocol:       Text-based (human-readable headers and request lines)
Transport:      TCP (reliable, ordered, byte-stream)
Connections:    Persistent by default (Connection: keep-alive)
Requests:       Sequential — one at a time per connection
Pipelining:     Technically supported but widely broken (servers respond in order)
```

## The Problem: Head-of-Line (HoL) Blocking

```text
HTTP/1.1 with keep-alive (one connection):

Request 1: GET /users    ──────────→  [waits]
Request 2: GET /orders   ──────[blocked until response 1 arrives]──→
Request 3: GET /products ──────────────────[blocked until response 2 arrives]──→

Even if /orders response is ready, /users must be returned first → serial execution
```

### The Browser's Workaround: Multiple Connections

Browsers open **6 connections per origin** in HTTP/1.1 to parallelize requests:

```text
Connection 1: GET /style.css
Connection 2: GET /app.js
Connection 3: GET /logo.png
Connection 4: GET /data/users
Connection 5: GET /data/config
Connection 6: (free)
```

This works, but each connection has:
- TCP slow start (bandwidth ramps up gradually)
- Memory overhead on the server (6× state per client)
- TLS overhead repeated per connection

## HTTP/1.1 Key Headers

```text
Connection: keep-alive      → reuse TCP connection (default in HTTP/1.1)
Connection: close           → close after response
Keep-Alive: timeout=60      → keep connection alive for 60 seconds
Transfer-Encoding: chunked  → stream response in chunks (no Content-Length needed)
```

## HTTP/1.1 Wire Format (Text)

```text
GET /api/users?page=1 HTTP/1.1
Host: api.example.com
Accept: application/json
Authorization: Bearer eyJhbGc...
User-Agent: Mozilla/5.0

                          ← blank line separates headers from body
```

Response:
```text
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 234
Cache-Control: max-age=60

{"users": [...]}
```

Everything is ASCII text — readable but verbose (headers repeat the same strings on every request).

---

# 2. HTTP/2 — Multiplexing Revolution (2015)

HTTP/2 was designed to fix HTTP/1.1's sequential request problem without changing the semantics (same methods, headers, status codes — just different transport).

## The Core Innovation: Binary Framing + Multiplexing

```text
HTTP/1.1: One request at a time per connection (sequential)

Request 1 ────────────── Response 1
Request 2 ────────────── Response 2  (must wait for Response 1)
Request 3 ────────────── Response 3  (must wait for Response 2)


HTTP/2: Multiple requests simultaneously on ONE connection (multiplexed)

Stream 1 (GET /users)   ──→  ←── Stream 1 Response
Stream 3 (GET /orders)  ──→  ←── Stream 3 Response (may arrive first!)
Stream 5 (GET /products)──→  ←── Stream 5 Response
Stream 7 (GET /config)  ──→  ←── Stream 7 Response

All on ONE TCP connection, interleaved as binary frames
```

## Streams and Frames

```text
HTTP/2 connection:
  One TCP connection
    ├── Stream 1 (odd = client-initiated)
    │     ├── HEADERS frame (request headers)
    │     └── DATA frame (request body, if any)
    ├── Stream 3
    │     └── HEADERS frame
    ├── Stream 5
    │     └── HEADERS frame
    └── ...

Server responses (any order):
    Stream 5 HEADERS + DATA  ← /products ready first
    Stream 1 HEADERS + DATA  ← /users next
    Stream 3 HEADERS + DATA  ← /orders last
```

Streams can be interleaved at the **frame level** (each frame is 16KB max), so a large response doesn't block small responses.

## HPACK Header Compression

HTTP headers are often repetitive across requests:

```text
Request 1:  Host: api.example.com, Accept: application/json, Authorization: Bearer xxx
Request 2:  Host: api.example.com, Accept: application/json, Authorization: Bearer xxx
Request 3:  Host: api.example.com, Accept: application/json, Authorization: Bearer xxx

HTTP/1.1: sends all headers verbatim on EVERY request (often 500-800 bytes)

HTTP/2 HPACK:
  Request 1: sends full headers, builds compression table
  Request 2: sends only CHANGED headers (e.g., just the path: GET /orders)
  → 80-90% header size reduction on repeated requests
```

HPACK uses a static table (common headers) + dynamic table (per-connection learned headers) + Huffman encoding.

## Server Push

```text
Client: GET /index.html

Server: sends /index.html response
        + PUSH_PROMISE for /style.css   ← server knows client will need this!
        + PUSH_PROMISE for /app.js

Client: receives /index.html and already has /style.css and /app.js cached
        → no additional round trips needed
```

Server push is controversial — it can push resources the client already has in its cache. Most production deployments use it selectively or not at all. The feature was removed from Chrome's h2 push support in 2022, and HTTP/3 also deprecated it.

## Stream Prioritization

Clients can tell the server which streams are more important:

```text
Stream 1 (HTML): weight=256 (highest — render page first)
Stream 3 (CSS):  weight=128 (render needs CSS)
Stream 5 (JS):   weight=64  (JS is deferred)
Stream 7 (image):weight=1   (lowest — load last)
```

Server can allocate bandwidth proportionally.

## HTTP/2 Still Has a Problem: TCP HoL Blocking

Even though HTTP/2 eliminates application-level HoL blocking, **TCP still causes it at the transport level**:

```text
HTTP/2 multiplexed streams over TCP:

Packet 1 (Stream 1 frame) ──────────────────────→ received
Packet 2 (Stream 3 frame) ──────────────────────→ LOST!
Packet 3 (Stream 5 frame) ──────────────────────→ received (buffered)
Packet 4 (Stream 1 frame) ──────────────────────→ received (buffered)

TCP: detects packet 2 loss, retransmits
     BLOCKS all streams until packet 2 is received (reordering buffer)
     → Stream 5 data sits in buffer waiting for Stream 3's packet!

Loss of ONE packet blocks ALL streams
```

This is the problem HTTP/3 solves.

---

# 3. HTTP/3 — QUIC Revolution (2022)

HTTP/3 replaces TCP with **QUIC** — a new transport protocol built on top of UDP. This eliminates TCP-level head-of-line blocking.

## QUIC — The Transport Layer

```text
HTTP/1.1 & HTTP/2:
  Application: HTTP
  Security:    TLS
  Transport:   TCP
  Network:     IP

HTTP/3:
  Application: HTTP/3
  Transport:   QUIC  ← new! (includes TLS 1.3 built-in)
  Network:     IP/UDP
```

QUIC provides:
- **Reliable delivery** (like TCP, but at the stream level, not connection level)
- **Ordered delivery within each stream** (but streams don't block each other)
- **Built-in TLS 1.3** (no separate TLS handshake — saves round trips)

## Solving TCP HoL Blocking

```text
HTTP/2 over TCP:                    HTTP/3 over QUIC:

[Stream 1][Stream 3][Stream 5]      Each stream is independently reliable
         ↓                                    ↓
Single TCP byte stream              Separate QUIC streams per HTTP stream
One lost packet → ALL blocked       Lost packet for Stream 3:
                                    → Only Stream 3 waits for retransmit
                                    → Streams 1 and 5 continue unaffected!
```

## Faster Connection Setup

```text
HTTP/1.1 (HTTPS):
  TCP SYN → SYN-ACK → ACK          (1 RTT)
  TLS ClientHello → ServerHello → Finished → Finished  (2 RTTs for TLS 1.2)
  First request                     (1 RTT)
  Total: 3+ RTTs before first response

HTTP/2 (HTTPS):
  Same as HTTP/1.1 (TCP + TLS) but TLS 1.3 = 1 RTT
  Total: 2 RTTs before first response

HTTP/3 (QUIC):
  QUIC Initial (combines TCP+TLS handshake) (1 RTT)
  First request (0-RTT for resumed connections)
  Total: 1 RTT (or 0-RTT for known servers!)
```

0-RTT resumption: if client has connected to this server before, it can send data in the very first QUIC packet — no handshake round trip at all.

## Connection Migration

```text
HTTP/1.1 / HTTP/2:
  TCP connection is identified by: src-IP + src-port + dst-IP + dst-port
  Change network (WiFi → 4G)? → src-IP changes → TCP connection dies → reconnect

HTTP/3:
  QUIC connection is identified by: Connection ID (random token in packets)
  Change network? → same Connection ID used over new network path
  → Connection survives network switch seamlessly!
```

This is why HTTP/3 is excellent for mobile applications.

---

# 4. Version Comparison

| Feature | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---|---|---|---|
| Protocol format | Text | Binary | Binary |
| Transport | TCP | TCP | QUIC (UDP) |
| Multiplexing | No (1 req/connection) | Yes (streams) | Yes (independent streams) |
| Header compression | No | HPACK | QPACK |
| TLS | Separate (optional) | Separate (required in practice) | Built into QUIC |
| Connection setup | TCP + TLS (2+ RTTs) | TCP + TLS (2+ RTTs) | QUIC (1 RTT, 0-RTT resume) |
| TCP HoL blocking | Yes | Yes | No |
| App HoL blocking | Yes | No | No |
| Server Push | No | Yes (mostly deprecated) | No |
| Connection migration | No | No | Yes (via Connection ID) |
| Adoption (2024) | ~30% | ~35% | ~30% |

---

# 5. Which Version to Use?

```text
REST API between microservices (internal):
  → HTTP/2 via gRPC or H2C (HTTP/2 cleartext) is common
  → Lower latency, header compression, multiplexing
  → Spring Boot + Netty or Tomcat support HTTP/2

Browser-facing HTTPS API:
  → Enable HTTP/2 on your load balancer / API gateway
  → Most modern load balancers (nginx, AWS ALB, Cloudflare) support it
  → HTTP/3 if CDN/edge (Cloudflare, Fastly) supports it

High packet loss environments (mobile, global):
  → HTTP/3 dramatically improves performance
  → Cloudflare / CDNs handle HTTP/3 at the edge automatically

Legacy integration / simple APIs:
  → HTTP/1.1 is fine if requests are infrequent and latency isn't critical
```

## In Spring Boot

```yaml
# application.properties — enable HTTP/2
server.http2.enabled=true

# Requires HTTPS (TLS) for HTTP/2 with most clients
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password
```

For internal service-to-service HTTP/2 without TLS (H2C):
```java
// Spring WebClient with HTTP/2
WebClient client = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(
        HttpClient.create().protocol(HttpProtocol.H2C)
    ))
    .build();
```

---

# 6. gRPC and HTTP/2

gRPC (Google Remote Procedure Call) uses HTTP/2 as its transport:

```text
gRPC frame layout over HTTP/2:

:method: POST
:path: /packageName.ServiceName/MethodName
:scheme: https
content-type: application/grpc+proto
grpc-timeout: 5S

[Length-Prefixed Message]  ← protobuf binary payload
```

gRPC benefits from HTTP/2's:
- **Multiplexing**: many RPC calls on one connection (no connection pool thrashing)
- **Header compression**: gRPC metadata (auth tokens, trace IDs) compressed
- **Streaming**: client-side, server-side, and bidirectional streaming supported natively

---

# Interview Preparation — HTTP Versions

## Q1: What is the main problem HTTP/2 solves compared to HTTP/1.1?

**Answer:**

HTTP/1.1 suffers from **head-of-line (HoL) blocking** — only one request can be in-flight at a time per connection. Even though HTTP/1.1 supports persistent connections (keep-alive), requests are still processed sequentially.

HTTP/2 introduces **binary framing and multiplexing**: multiple requests are split into frames and interleaved on a single TCP connection. Each request is a "stream" (odd-numbered, client-initiated) and can be processed concurrently. Responses can arrive out of order.

Additionally, HTTP/2 adds **HPACK header compression** — headers repeated across requests are compressed using a shared compression table, reducing 500-byte headers to a few bytes on subsequent requests.

## Q2: If HTTP/2 has multiplexing, why does HTTP/3 still exist?

**Answer:**

HTTP/2's multiplexing operates at the **application layer** — but it runs over TCP, which provides a single ordered byte stream. If a TCP packet is lost, all streams are blocked until the retransmit arrives, even if the lost packet only belongs to one stream. This is **TCP-level head-of-line blocking**.

HTTP/3 replaces TCP with **QUIC** (built on UDP). QUIC implements reliable, ordered delivery independently per stream. A lost packet only blocks the specific stream it belongs to — other streams continue unaffected.

Additionally, QUIC has TLS 1.3 built in (saving handshake round trips), supports **connection migration** (network changes don't kill the connection), and achieves **0-RTT resumption** for known servers.

## Q3: What is QUIC and why is it built on UDP instead of TCP?

**Answer:**

QUIC (Quick UDP Internet Connections) is a transport protocol designed by Google, standardized by IETF for HTTP/3. It's built on UDP because:

1. **Flexibility**: UDP is minimal — no built-in ordering, reliability, or connection state. QUIC implements exactly the features it needs (reliability per stream) without TCP's limitations.
2. **Avoids TCP HoL blocking**: since each QUIC stream has independent reliability, a lost packet doesn't block other streams.
3. **Faster handshake**: QUIC integrates TLS 1.3, achieving 1 RTT (and 0-RTT for resumption) vs TCP+TLS's 2+ RTTs.
4. **Deployability**: TCP is deeply embedded in OS kernels, slow to change. UDP implementations can be updated in userspace (in the application layer).

## Q4: What changed in connection setup time between HTTP/1.1 and HTTP/3?

**Answer:**

```text
HTTP/1.1 (HTTPS, TLS 1.2): 3 RTTs before first data
  1 RTT TCP handshake + 2 RTT TLS 1.2

HTTP/2 (HTTPS, TLS 1.3): 2 RTTs
  1 RTT TCP handshake + 1 RTT TLS 1.3

HTTP/3 (QUIC): 1 RTT (0-RTT for resumption)
  1 RTT combined QUIC+TLS handshake
  0 RTT if client has previously connected and has a session ticket
```

For a client 100ms away from the server:
- HTTP/1.1: 300ms before first byte of data
- HTTP/2: 200ms
- HTTP/3: 100ms (or 0ms + processing time for 0-RTT)

This matters significantly for mobile apps with high latency and frequent new connections.
