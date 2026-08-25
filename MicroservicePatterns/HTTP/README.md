# HTTP — Navigation Guide

HTTP (HyperText Transfer Protocol) is the foundation of all web communication. Every microservice, REST API, and browser interaction uses it. Understanding HTTP deeply — not just "GET returns data" — is essential for backend engineers.

---

## Reading Order

| # | Folder | What You Learn |
|---|---|---|
| 1 | `README.md` ← **you are here** | Overview, why HTTP matters, the big picture |
| 2 | `Versions/` | HTTP/1.1 vs HTTP/2 vs HTTP/3 — internal mechanics, multiplexing, QUIC, performance differences |
| 3 | `RequestResponse/` | Full anatomy: methods, headers, cookies, body, query/path params, content types, status codes |
| 4 | `Caching/` | Cache-Control, ETags, conditional requests, 304 Not Modified, keep-alive, connection pooling |

---

## Why This Order

```
Versions/ first: understand the transport-level differences between HTTP versions
                 so that later discussions of headers/caching make sense in context

RequestResponse/ second: the full message format — every topic references request/response

Caching/ last: caching is built on top of request/response (specific headers and ETags)
               and behaves differently across HTTP versions
```

---

## The Big Picture

```text
Client (Browser / Mobile App / Service A)
         |
         | HTTP Request
         ↓
    [TCP Connection]  (HTTP/1.1, HTTP/2)
    [QUIC/UDP]        (HTTP/3)
         |
         ↓
Server (Web Server / API Gateway / Service B)
         |
         | HTTP Response
         ↓
Client receives response
```

Every HTTP interaction is a **request-response** cycle over a connection:
- `HTTP/1.1` — text protocol, one request at a time per connection (pipelining mostly broken)
- `HTTP/2` — binary protocol, multiple requests simultaneously (multiplexing) over one TCP connection
- `HTTP/3` — binary protocol, multiplexing over QUIC (UDP), eliminates TCP head-of-line blocking

---

## Connection to Other Topics

```
HTTP/ → SpringBootRoadmap/  → REST APIs, @RestController, @RequestMapping use HTTP verbs,
                               status codes, content types you'll learn here

HTTP/ → MicroserviceRoadmap/ → service-to-service communication, API gateway,
                                load balancers all use HTTP headers and versions

HTTP/Caching/ → CDN caching, browser caching, proxy caching — all use same headers

HTTP/Versions/ → gRPC (uses HTTP/2 framing internally)
```

---

## Quick Reference

```
"How does HTTP/2 differ from HTTP/1.1?"  → Versions/
"What's inside an HTTP request?"          → RequestResponse/
"How does browser caching work?"          → Caching/
"What is an ETag?"                        → Caching/
"What does status 304 mean?"             → Caching/
"Difference between query and path param?"→ RequestResponse/
"What is a cookie vs a header?"          → RequestResponse/
```
