# ConcurrentSkipList Collections — Deep Dive

`ConcurrentSkipListMap` and `ConcurrentSkipListSet` are concurrent, sorted collections based on the **Skip List** data structure. They are the thread-safe alternatives to `TreeMap` and `TreeSet`.

> **ConcurrentSkipListMap gives you a sorted, thread-safe map with O(log n) operations — all without locking, using CAS. It's the go-to when you need concurrent access to data that must remain sorted.**

---

# 1. What is a Skip List?

A Skip List is a probabilistic data structure that maintains sorted order while providing O(log n) average time for search, insert, and delete — without the complexity of balancing (like AVL or Red-Black trees).

## Visualization

```text
Normal sorted linked list:
Level 0 (all nodes):
  1 → 3 → 7 → 12 → 19 → 26 → 37 → null
  
  Search for 19: must traverse 1 → 3 → 7 → 12 → 19 = 5 hops (O(n))

Skip List (multiple levels = "express lanes"):
Level 3:  1 ────────────────────────→ 37 → null
Level 2:  1 ──────────→ 12 ─────────→ 37 → null
Level 1:  1 ─────→ 7 → 12 ────→ 26 → 37 → null
Level 0:  1 → 3 → 7 → 12 → 19 → 26 → 37 → null (base level, all nodes)

Search for 19:
  Level 3: 1, jump to 37 — too far, drop down
  Level 2: 1, jump to 12 — less than 19, advance; next is 37 — too far, drop down
  Level 1: at 12, advance to 26 — too far, drop down
  Level 0: at 12, step to 19 — FOUND!
  Total: ~4-5 hops instead of 6 = O(log n)
```

Each node is randomly assigned to multiple levels during insertion (probabilistic balancing).

## Why Skip Lists for Concurrent Environments?

```text
Red-Black Tree (TreeMap):
  Rebalancing requires complex tree rotations
  Rotations affect multiple nodes
  Hard to make lock-free — rebalancing under concurrency is complex

Skip List:
  Insert/delete only affects local nodes (no global rebalancing)
  Each node is modified independently
  CAS can be applied locally → lock-free concurrent access is natural
  Much simpler to implement correctly in concurrent setting
```

---

# 2. ConcurrentSkipListMap

## What it is

A concurrent, sorted `NavigableMap` backed by a skip list. All keys are maintained in natural (or custom `Comparator`) sorted order.

```text
ConcurrentSkipListMap<String, Integer>:

  Sorted by key:
  "apple" → 1
  "banana" → 2
  "cherry" → 3
  "date" → 4

  Keys always in sorted order (ascending by default)
```

## Key Characteristics

```text
Thread-safe:   YES (lock-free CAS operations)
Sorted:        YES — always maintains key order
Ordering:      Natural (Comparable) or custom Comparator
Null keys:     NO
Null values:   NO
Operations:    O(log n) expected for get, put, remove
Size:          O(1) — maintains count
Memory:        Higher than HashMap (skip list nodes have level pointers)
```

## Navigable Operations — The Power Feature

`ConcurrentSkipListMap` extends `NavigableMap` — it supports **range queries** that `ConcurrentHashMap` cannot:

```java
ConcurrentSkipListMap<String, Order> orderMap = new ConcurrentSkipListMap<>();

// Range queries (only possible because keys are sorted!):
orderMap.subMap("order-100", "order-200");      // all orders in range [100, 200)
orderMap.headMap("order-150");                   // all orders BEFORE order-150
orderMap.tailMap("order-150");                   // all orders FROM order-150 onward

// Floor / ceiling (nearest neighbors):
orderMap.floorKey("order-145");                  // highest key ≤ order-145
orderMap.ceilingKey("order-145");                // lowest key ≥ order-145
orderMap.higherKey("order-145");                 // lowest key > order-145

// First / last
orderMap.firstKey();                             // smallest key
orderMap.lastKey();                              // largest key

// Reverse view
orderMap.descendingMap();                        // map in reverse order
```

---

## ConcurrentSkipListMap vs ConcurrentHashMap vs TreeMap

| | HashMap | TreeMap | ConcurrentHashMap | ConcurrentSkipListMap |
|---|---|---|---|---|
| Thread-safe | No | No | Yes | Yes |
| Ordered | No | Yes (sorted) | No | Yes (sorted) |
| get/put | O(1) avg | O(log n) | O(1) avg | O(log n) |
| Range queries | No | Yes | No | Yes |
| Lock-free | N/A | N/A | Partly | Fully |
| Null keys | Yes | Yes (if comparator allows) | No | No |
| Best for | Fast unordered map | Single-threaded sorted | High-throughput unordered | Concurrent sorted |

---

## Real-Life Use Cases

### Use Case 1: Leaderboard / Score Ranking

```java
// username → score, sorted by username
// For score-based sorting, use a custom Comparator or score as key
ConcurrentSkipListMap<Integer, String> leaderboard =
    new ConcurrentSkipListMap<>(Comparator.reverseOrder());  // highest first

leaderboard.put(9500, "Alice");
leaderboard.put(8200, "Bob");
leaderboard.put(9800, "Charlie");

// Top 3 players (naturally — map is sorted!)
leaderboard.entrySet().stream().limit(3).forEach(System.out::println);
// Charlie: 9800, Alice: 9500, Bob: 8200

// Players above 9000 score
leaderboard.headMap(9000);  // Charlie and Alice
```

### Use Case 2: Time-Series Data (Event Log by Timestamp)

```java
ConcurrentSkipListMap<Long, List<Event>> eventLog = new ConcurrentSkipListMap<>();

// Events arrive concurrently from multiple threads
void recordEvent(Event event) {
    long timestamp = event.getTimestamp();
    eventLog.computeIfAbsent(timestamp, k -> new CopyOnWriteArrayList<>())
            .add(event);
}

// Query: events in the last 5 minutes (sorted by time)
long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
Map<Long, List<Event>> recentEvents = eventLog.tailMap(fiveMinutesAgo);
```

### Use Case 3: Price-Level Order Book (Financial Trading)

```java
// Buy orders: sorted by price (descending — highest bids first)
ConcurrentSkipListMap<BigDecimal, List<Order>> buyOrders =
    new ConcurrentSkipListMap<>(Comparator.reverseOrder());

// Sell orders: sorted by price (ascending — lowest asks first)
ConcurrentSkipListMap<BigDecimal, List<Order>> sellOrders = new ConcurrentSkipListMap<>();

// Best bid (highest buy price)
BigDecimal bestBid = buyOrders.firstKey();

// Best ask (lowest sell price)
BigDecimal bestAsk = sellOrders.firstKey();

// Spread
BigDecimal spread = bestAsk.subtract(bestBid);

// Orders at or above a price level
Map<BigDecimal, List<Order>> expensiveBuys = buyOrders.headMap(targetPrice);
```

### Use Case 4: Rate Limiting with Time Windows

```java
// Track request timestamps per user
ConcurrentSkipListMap<Long, String> requestTimestamps = new ConcurrentSkipListMap<>();

void recordRequest(String userId) {
    requestTimestamps.put(System.currentTimeMillis(), userId);
}

// Count requests in last 60 seconds
int countRequestsLastMinute(String userId) {
    long oneMinuteAgo = System.currentTimeMillis() - 60_000;
    // tailMap gives all timestamps >= oneMinuteAgo
    return (int) requestTimestamps.tailMap(oneMinuteAgo)
        .values().stream()
        .filter(id -> id.equals(userId))
        .count();
}
```

### Use Case 5: Distributed System — Consistent Hashing Ring

```java
// Virtual nodes on the consistent hashing ring, sorted by hash
ConcurrentSkipListMap<Integer, String> ring = new ConcurrentSkipListMap<>();

// Add server (with virtual nodes)
void addServer(String serverName) {
    for (int i = 0; i < 100; i++) {
        int hash = hash(serverName + "-" + i);
        ring.put(hash, serverName);  // concurrent-safe addition
    }
}

// Route request to correct server
String routeRequest(String requestKey) {
    int hash = hash(requestKey);
    // Find the first server clockwise from the hash
    Map.Entry<Integer, String> entry = ring.ceilingEntry(hash);
    if (entry == null) entry = ring.firstEntry();  // wrap around
    return entry.getValue();
}
```

---

# 3. ConcurrentSkipListSet

## What it is

A thread-safe, sorted `NavigableSet` backed by `ConcurrentSkipListMap` (internally stores entries as keys with a dummy value).

```java
ConcurrentSkipListSet<String> set = new ConcurrentSkipListSet<>();
set.add("cherry");
set.add("apple");
set.add("banana");

// Always sorted: apple, banana, cherry
System.out.println(set);  // [apple, banana, cherry]

// Range operations
set.headSet("cherry");   // [apple, banana] — elements before cherry
set.tailSet("banana");   // [banana, cherry] — elements from banana onward
set.subSet("apple", "cherry");  // [apple, banana]

// Navigable operations
set.floor("blueberry");   // "banana" — greatest element ≤ blueberry
set.ceiling("blueberry"); // "cherry" — smallest element ≥ blueberry
set.first();  // "apple"
set.last();   // "cherry"
```

## Key Characteristics

```text
Thread-safe:  YES (lock-free)
Sorted:       YES — natural ordering or Comparator
Unique:       YES (Set semantics — no duplicates)
Null:         NO
Operations:   O(log n)
```

## ConcurrentSkipListSet vs ConcurrentHashSet (via ConcurrentHashMap)

```java
// ConcurrentHashSet approach (ConcurrentHashMap.newKeySet())
Set<String> concurrentHashSet = ConcurrentHashMap.newKeySet();

// ConcurrentSkipListSet
Set<String> sortedConcurrentSet = new ConcurrentSkipListSet<>();
```

| | ConcurrentHashMap.newKeySet() | ConcurrentSkipListSet |
|---|---|---|
| Thread-safe | Yes | Yes |
| Sorted | No | Yes |
| contains | O(1) avg | O(log n) |
| add/remove | O(1) avg | O(log n) |
| Range queries | No | Yes |
| Best for | Fast unordered set | Sorted, range-query set |

---

## Real-life Use Cases for ConcurrentSkipListSet

### Use Case 1: Active User Sessions (Sorted by Login Time)

```java
ConcurrentSkipListSet<UserSession> activeSessions =
    new ConcurrentSkipListSet<>(Comparator.comparing(UserSession::getLoginTime));

// Sessions always sorted by login time
// Easy to find: first logged-in user, oldest sessions, etc.
void login(UserSession session) { activeSessions.add(session); }
void logout(UserSession session) { activeSessions.remove(session); }

// Find sessions older than 30 minutes (for timeout)
UserSession cutoff = new UserSession(null, thirtyMinutesAgo);
SortedSet<UserSession> expiredSessions = activeSessions.headSet(cutoff);
```

### Use Case 2: Event IDs for Deduplication (Ordered Window)

```java
// Track processed event IDs in a sorted window
ConcurrentSkipListSet<Long> processedIds = new ConcurrentSkipListSet<>();

boolean process(long eventId) {
    if (processedIds.add(eventId)) {
        // Not a duplicate — process it
        doProcess(eventId);

        // Clean up old IDs (keep only last 10000)
        if (processedIds.size() > 10000) {
            processedIds.pollFirst();  // remove lowest (oldest) ID
        }
        return true;
    }
    return false;  // duplicate — skip
}
```

---

# 4. Summary — ConcurrentSkipList Collections

```text
Choose ConcurrentSkipListMap when:
  ✓ Need sorted map with concurrent access
  ✓ Need range queries (subMap, headMap, tailMap)
  ✓ Need floor/ceiling/first/last key operations
  ✓ Time-series data, leaderboards, order books, consistent hashing rings
  ✓ TreeMap use-case but multithreaded

Choose ConcurrentSkipListSet when:
  ✓ Need sorted set with concurrent access
  ✓ Need range operations on a set
  ✓ Sorted, unique elements across threads
  ✓ Windowed deduplication with sorted ordering

Avoid when:
  ✗ You don't need sorting → ConcurrentHashMap is O(1) vs O(log n)
  ✗ Very memory-constrained → Skip list has higher memory overhead than hash table
  ✗ Very high write throughput on the same key → blocking skip list has less
     per-bucket parallelism than CHM's bucket-level CAS
```

---

# Interview Preparation — ConcurrentSkipList

---

## Q1: What is a Skip List and why is it used for concurrent sorted collections?

**Answer:**

A Skip List is a probabilistic, sorted linked-list data structure with multiple "express lane" levels. Each level skips over more nodes. This achieves O(log n) average-case search/insert/delete without the complex rebalancing of AVL or Red-Black trees.

Why used for concurrent collections:
- **Local modifications**: inserting or deleting a node only requires changing a few local pointers, not a global tree rotation
- **CAS-friendly**: individual node updates can be made atomically with CAS, enabling fully lock-free operations
- Red-Black trees require rebalancing that can affect many nodes simultaneously → very hard to make lock-free

---

## Q2: What operations does ConcurrentSkipListMap support that ConcurrentHashMap cannot?

**Answer:**

`ConcurrentSkipListMap` supports all `NavigableMap` operations that depend on sorted order:

```java
// Range queries — not possible with ConcurrentHashMap
map.subMap("apple", "mango");   // entries between two keys
map.headMap("lemon");            // all entries before lemon
map.tailMap("lemon");            // all entries from lemon onward

// Neighbor queries
map.floorKey("key");    // largest key ≤ given key
map.ceilingKey("key");  // smallest key ≥ given key
map.lowerKey("key");    // largest key strictly < given key
map.higherKey("key");   // smallest key strictly > given key

// Extremes
map.firstKey();  // minimum key
map.lastKey();   // maximum key
```

None of these work in `ConcurrentHashMap` because it stores data in a hash table — no ordering information.

---

## Q3: Give a scenario where ConcurrentSkipListMap is clearly the right choice.

**Answer:**

**Real-time leaderboard for an online game:**

```text
Requirements:
  - Thousands of players updating scores simultaneously
  - Frequent reads: top 10 players, rank of a specific player
  - Range queries: players with scores between 5000-8000 (for matchmaking)
  - Scores must be retrieved in sorted order
```

```java
ConcurrentSkipListMap<Integer, String> leaderboard =
    new ConcurrentSkipListMap<>(Comparator.reverseOrder());

// Concurrent score updates from game servers
void updateScore(int score, String playerId) {
    leaderboard.put(score, playerId);
}

// Top 10 — naturally ordered, no sorting needed
leaderboard.entrySet().stream().limit(10).collect(toList());

// Matchmaking: find players with similar scores
int playerScore = getScore(playerId);
leaderboard.subMap(playerScore + 200, playerScore - 200);
```

`ConcurrentHashMap` would require sorting on every read — expensive. `TreeMap` is not thread-safe. `ConcurrentSkipListMap` is the perfect fit.

---

## Q4: How does ConcurrentSkipListMap compare to ConcurrentHashMap in performance?

**Answer:**

| Operation | ConcurrentHashMap | ConcurrentSkipListMap |
|---|---|---|
| get/put/remove | O(1) average | O(log n) |
| Range query | Not supported | O(log n + results) |
| Sorted iteration | Not supported | O(n) |
| Memory | Less | More (level pointers) |

For operations that don't need ordering, `ConcurrentHashMap` is faster. The O(1) vs O(log n) difference becomes significant at very high throughput (millions of ops/sec).

Use `ConcurrentSkipListMap` only when sorted ordering or range queries are genuinely needed. Don't use it as a drop-in replacement for `ConcurrentHashMap` without reason.

---

## Q5: How would you implement a concurrent time-series window using ConcurrentSkipListMap?

**Answer:**

```java
ConcurrentSkipListMap<Long, Metric> metrics = new ConcurrentSkipListMap<>();

// Add metric (concurrent from many threads)
void record(Metric m) {
    metrics.put(m.getTimestamp(), m);
}

// Query last 5 minutes (concurrent with writes — safe)
List<Metric> lastFiveMinutes() {
    long fiveMinAgo = System.currentTimeMillis() - 300_000;
    return new ArrayList<>(metrics.tailMap(fiveMinAgo).values());
}

// Evict old data (periodic cleanup)
void evictOld(long cutoffMs) {
    Long oldestKey;
    while ((oldestKey = metrics.firstKey()) != null && oldestKey < cutoffMs) {
        metrics.remove(oldestKey);  // concurrent-safe removal
    }
}
```

The skip list's sorted order makes range queries (tailMap) O(log n), and eviction of old entries (pollFirst/remove from front) is O(log n). This would require O(n) scanning with `ConcurrentHashMap`.
