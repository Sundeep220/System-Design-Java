# 1. Fixed Window Counter

## Core idea

Divide time into fixed intervals.

Example:

```text
Limit = 5 requests
Window = 1 minute
```

Time is divided into:

```text
00:00 ───────── 01:00
01:00 ───────── 02:00
02:00 ───────── 03:00
```

For each user:

```text
user1 → {
    count = 3,
    windowStart = 00:00
}
```

Every request increments the counter.

```text
if count < limit:
    count++
    ALLOW
else:
    REJECT
```

When the window expires:

```text
count = 0
```

---

## Example

Limit:

```text
5 requests / minute
```

Requests:

```text
00:59 → 5 requests → ALLOW
01:00 → 5 requests → ALLOW
```

The system allowed:

```text
10 requests in roughly 1 second
```

This is called the **boundary problem**.

---

## Advantages

### Very simple

Only maintain:

```text
count
windowStartTime
```

### Very memory efficient

Per user:

```text
O(1)
```

### Fast

Usually:

```text
O(1)
```

### Easy to distribute

For distributed systems, a centralized counter can be stored in:

```text
Redis
```

with atomic increment and expiration.

---

## Disadvantages

The boundary problem can allow bursts.

```text
Window 1:
[█████] 5 requests

Window 2:
[█████] 5 requests
```

---

## Best production use cases

Use Fixed Window when:

### 1. Simplicity matters

Example:

```text
Maximum 1000 API calls per day
```

You do not need extremely precise rate limiting.

### 2. Daily/monthly quotas

Examples:

```text
Free user → 1000 API calls per day
Premium user → 100,000 API calls per month
```

### 3. Simple authentication limits

Example:

```text
Maximum 5 password attempts per 15 minutes
```

Although for security-sensitive cases, you should carefully consider the boundary behavior.

### 4. Large-scale distributed counters

Fixed window works well with atomic counters.

Conceptually:

```text
INCR user:123:window
EXPIRE user:123:window 60
```

---

# 2. Sliding Window Log

## Core idea

Store the timestamp of every request.

For each user:

```text
user1 → [1000, 1005, 1010, 1015]
```

Suppose:

```text
Current time = 1020
Window = 10 seconds
```

The active window is:

```text
[1010, 1020]
```

Remove timestamps before `1010`.

Then count the remaining requests.

---

## Example

Limit:

```text
3 requests / 10 seconds
```

Requests:

```text
Time 1 → ALLOW
Time 3 → ALLOW
Time 5 → ALLOW
Time 7 → REJECT
```

At time `12`:

```text
Window = [2, 12]
```

Timestamp `1` expires.

Now:

```text
[3, 5]
```

Only 2 requests remain, so another request can be allowed.

---

## Advantages

### Very accurate

It actually counts requests in the exact rolling time interval.

### No fixed-window boundary problem

Instead of:

```text
[00 - 10]
[10 - 20]
```

we always use:

```text
[currentTime - 10, currentTime]
```

---

## Disadvantages

### Memory usage

If a user makes many requests, we store many timestamps.

```text
O(number of requests in window)
```

### More expensive

Compared to a simple counter.

### Distributed implementation is harder

You need to manage:

```text
sorted timestamps
expiration
concurrent updates
```

---

## Best production use cases

Use Sliding Window Log when:

### 1. Accuracy is very important

Example:

```text
Maximum 100 requests in the last 60 seconds
```

where you need an exact interpretation.

### 2. Security-sensitive operations

Examples:

```text
Password reset requests
OTP generation
Login attempts
```

### 3. Fair API usage enforcement

Example:

```text
No user should make more than 100 requests in ANY rolling 60-second period.
```

The word **any** is important.

---

# 3. Sliding Window Counter

This is an important variation.

Instead of storing every timestamp, we combine:

```text
Current fixed window
+
Previous fixed window
```

Suppose:

```text
Limit = 100 requests / minute
```

Current window:

```text
[12:01 - 12:02]
```

Previous window:

```text
[12:00 - 12:01]
```

Suppose:

```text
Previous count = 80
Current count = 20
```

At 12:01:30, only half of the previous window is relevant.

Approximate count:

```text
80 × 50% + 20
= 60
```

This gives a good approximation of a sliding window.

---

## Why it is useful

It gives a balance:

```text
Fixed Window
      │
      │ simple but boundary issue
      │
Sliding Window Counter
      │
      │ approximate but efficient
      │
Sliding Window Log
```

---

## Best production use cases

This is excellent for:

```text
High-scale API gateways
Distributed rate limiters
Cloud infrastructure
```

because it provides:

```text
low memory usage
good accuracy
high scalability
```

---

# 4. Token Bucket

## Core idea

A bucket contains tokens.

Example:

```text
Capacity = 5 tokens
Refill rate = 1 token / second
```

Initially:

```text
[● ● ● ● ●]
```

Each request consumes one token.

```text
Request → remove one token
```

If:

```text
tokens > 0
```

then:

```text
ALLOW
```

Otherwise:

```text
REJECT
```

---

## Example

Capacity:

```text
5
```

Refill:

```text
1 token per second
```

Initially:

```text
5 tokens
```

Requests:

```text
Request 1 → 4 tokens → ALLOW
Request 2 → 3 tokens → ALLOW
Request 3 → 2 tokens → ALLOW
Request 4 → 1 token  → ALLOW
Request 5 → 0 tokens → ALLOW
Request 6 → REJECT
```

After 2 seconds:

```text
2 tokens refill
```

So:

```text
2 more requests → ALLOW
```

---

## Why Token Bucket is powerful

It separates two concepts:

### Burst capacity

```text
Bucket capacity
```

How many requests can happen immediately?

### Long-term rate

```text
Refill rate
```

How quickly can the user continue making requests?

For example:

```text
Capacity = 100
Refill = 10 tokens/sec
```

means:

```text
Burst:
100 requests immediately

Long-term:
10 requests/sec
```

This is extremely useful.

---

## Best production use cases

Token Bucket is often the best general-purpose API rate limiter.

### API gateways

Examples:

```text
100 requests/sec
Burst capacity = 200
```

A client can briefly send 200 requests, but over time the average rate is limited to 100/sec.

### Cloud services

Many cloud APIs use concepts similar to:

```text
request rate
burst capacity
```

### Network traffic

It allows short bursts while preventing sustained overload.

### User-facing APIs

Suppose a user suddenly opens an application.

The client might send:

```text
10 requests immediately
```

A strict fixed rate might reject them unnecessarily.

Token Bucket can allow the burst if capacity exists.

---

# 5. Leaky Bucket

## Core idea

Requests enter a queue.

```text
Incoming requests
        ↓
┌────────────────┐
│ R1 R2 R3 R4 R5 │
└───────┬────────┘
        │
        ▼
 Fixed processing rate
```

The bucket leaks at a constant rate.

Example:

```text
1 request / second
```

Then:

```text
Time 1 → R1
Time 2 → R2
Time 3 → R3
Time 4 → R4
```

The output is smooth.

---

## Important distinction from Token Bucket

Suppose 100 requests arrive at once.

### Token Bucket

If there are enough tokens:

```text
100 requests may be accepted immediately
```

### Leaky Bucket

They are queued and processed:

```text
R1 → wait → R2 → wait → R3
```

at a fixed rate.

---

## Best production use cases

### 1. Traffic shaping

When downstream systems need a predictable rate.

Example:

```text
Database can handle only 100 writes/sec
```

You can use a leaky-bucket-like queue to smooth incoming traffic.

### 2. Network packet shaping

Prevent bursts from overwhelming a network.

### 3. Message processing

Example:

```text
Kafka consumer
External API
Database writer
```

You may want:

```text
Exactly 100 requests per second
```

rather than allowing bursts.

### 4. Backpressure systems

If the downstream service is slower than the incoming traffic, queue requests and process them at a controlled rate.

---

# Important: Rate Limiting vs Traffic Shaping

This is a useful distinction.

### Rate Limiting

Question:

> Should I allow this request?

Output:

```text
ALLOW / REJECT
```

Common algorithms:

```text
Fixed Window
Sliding Window
Token Bucket
```

### Traffic Shaping

Question:

> When should this request be processed?

Output:

```text
Process now
or
Queue for later
```

Leaky Bucket is particularly useful for this.

---

# Complete Comparison

| Algorithm              | Stores                  |             Burst Support |           Accuracy |      Memory | Output Rate | Best Use             |
| ---------------------- | ----------------------- | ------------------------: | -----------------: | ----------: | ----------: | -------------------- |
| Fixed Window           | Counter                 |        High at boundaries |                Low |        O(1) |    Variable | Simple quotas        |
| Sliding Window Log     | Timestamps              |                       Low |          Very High |        O(N) |    Variable | Precise limits       |
| Sliding Window Counter | Counters                |                    Medium | Good approximation |        O(1) |    Variable | Large-scale APIs     |
| Token Bucket           | Tokens                  |           Yes, controlled |               Good |        O(1) |    Variable | General API limiting |
| Leaky Bucket           | Queued requests / level | No immediate output burst |               Good | O(capacity) |    Constant | Traffic shaping      |

---

# Visual Comparison

## Fixed Window

```text
Window 1        Window 2

█████            █████
 5 requests       5 requests

Boundary can create burst
```

---

## Sliding Window Log

```text
      ┌──────────────┐
      │ Last 60 sec  │
      └──────────────┘

Every request timestamp is tracked.
```

Most precise.

---

## Sliding Window Counter

```text
Previous Window  +  Current Window

      60%              40%
      │                 │
      ▼                 ▼

    count × 60%       count
```

Approximate but efficient.

---

## Token Bucket

```text
Tokens refill
      ↓
[● ● ● ● ●]
      ↓
Requests consume tokens

Allows controlled bursts
```

---

## Leaky Bucket

```text
R1 R2 R3 R4 R5
      ↓
   Queue
      ↓
R1 → R2 → R3 → R4

Smooth constant output
```

---

# Which One Should You Choose?

## Use Fixed Window when:

```text
Simplicity > precision
```

Example:

```text
1000 API calls per day
```

---

## Use Sliding Window Log when:

```text
Exact rolling-window enforcement is required
```

Example:

```text
Maximum 5 OTP requests in any 10 minutes
```

---

## Use Sliding Window Counter when:

```text
You need scalability + better accuracy than Fixed Window
```

Example:

```text
Large distributed API gateway
```

---

## Use Token Bucket when:

```text
You want to allow legitimate bursts
while controlling the long-term rate.
```

Example:

```text
100 requests/sec
Burst capacity = 200
```

This is usually the best general-purpose choice for APIs.

---

## Use Leaky Bucket when:

```text
You need a smooth, predictable output rate.
```

Example:

```text
Database supports 100 writes/sec
```

Incoming traffic can be queued and released at:

```text
100 writes/sec
```

---

# Interview Summary

A good answer in an interview would be:

> I would use the Strategy Pattern to support different rate-limiting algorithms. Fixed Window is simple and memory efficient but suffers from boundary bursts. Sliding Window Log provides precise rolling-window enforcement but uses more memory. Sliding Window Counter provides a scalable approximation using fewer counters. Token Bucket is useful when controlled bursts are acceptable because tokens accumulate and refill at a fixed rate. Leaky Bucket is better when we need to smooth traffic and process requests at a predictable constant rate. For a general API gateway, I would typically choose Token Bucket, while for strict rolling-window enforcement I would choose Sliding Window or Sliding Window Counter depending on scale.
