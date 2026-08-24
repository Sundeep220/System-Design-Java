# ConcurrentHashMap — Deep Dive

`ConcurrentHashMap` is the most important concurrent collection in Java. It is the thread-safe, high-performance alternative to `HashMap` and is used everywhere — from application caches to Spring internals to Kafka clients.

> **ConcurrentHashMap allows multiple threads to read and write simultaneously without corrupting data, while delivering near-HashMap performance under concurrency.**

---

# 1. Why ConcurrentHashMap Exists

The problem with alternatives:

```text
HashMap:
  No thread-safety at all
  Concurrent modification → data corruption, infinite loops

Hashtable:
  synchronized on EVERY method
  Only one thread at a time — entire table locked
  Thread 1: get("order-1") ← holds lock
  Thread 2: get("order-2") ← WAITING (different key, same lock!)
  → Terrible throughput

Collections.synchronizedMap(HashMap):
  Same as Hashtable — wraps every method in synchronized(this)
  One global lock → serial access

ConcurrentHashMap:
  Thread 1: get("order-1") ← no lock needed (reads are lock-free)
  Thread 2: get("order-2") ← also runs freely
  Thread 3: put("order-3") ← locks only bucket 3
  Thread 4: put("order-7") ← locks only bucket 7 (different bucket!)
  → Multiple threads make progress simultaneously
```

---

# 2. Internal Structure — How It Actually Works

## Java 7: Segment-based Locking

```text
ConcurrentHashMap (Java 7):

[Segment 0][Segment 1][Segment 2][Segment 3]... [Segment 15]
     |           |           |           |
  ReentrantLock  ReentrantLock  ...

Each Segment is essentially a mini-HashMap with its own lock.
Default: 16 segments → 16 threads can write simultaneously.
```

Key detail: each segment had its own lock — threads accessing different segments never blocked each other.

## Java 8: Bucket-level CAS + synchronized

Java 8 completely redesigned this — far more efficient:

```text
ConcurrentHashMap (Java 8):

[Bucket 0][Bucket 1][Bucket 2]...[Bucket n]

Each bucket is ONE of:
  - Empty slot (null)
  - Single node (one entry)
  - Linked list (multiple entries, same hash bucket)
  - Red-Black Tree (when bucket has > 8 entries → treeified)
```

Locking strategy (Java 8):

```text
READ (get):
  No lock at all — uses volatile reads + CAS
  Fully concurrent reads

WRITE (put) on EMPTY bucket:
  CAS (Compare-And-Swap) to set the first node
  No lock needed!

WRITE (put) on NON-EMPTY bucket:
  synchronized(firstNodeOfBucket)
  Only locks that specific bucket's head node
  Threads on other buckets are unaffected

RESIZE:
  Incremental, cooperative — multiple threads help resize simultaneously
```

Visualization:

```text
ConcurrentHashMap internal array (capacity 16):

Index:  [0] [1] [2] [3] [4] [5] [6] [7] ...

Bucket 3: → [Entry("order-1", v1)] → [Entry("order-9", v2)] (linked list)
Bucket 7: → TreeNode (red-black tree, > 8 entries)
Bucket 1: null (empty)

Thread A: put("order-15") → hashes to bucket 5 → CAS or sync on bucket 5
Thread B: get("order-1")  → reads bucket 3 → NO LOCK (volatile)
Thread C: put("order-7")  → hashes to bucket 3 → syncs on bucket 3's head
                             (Thread A and C run in parallel — different buckets)
```

---

# 3. Key Behaviors

## Reads Are Always Non-Blocking

```text
get() never acquires a lock
Uses volatile guarantees to see latest writes
Multiple threads can read simultaneously with zero contention
```

## Writes Use Fine-grained Locking

```text
put() on bucket X only locks bucket X
put() on bucket Y only locks bucket Y
Both can run simultaneously → high throughput
```

## Null Keys and Values Are NOT Allowed

```java
map.put(null, "value");  // NullPointerException!
map.put("key", null);    // NullPointerException!
```

Why? Because `null` is used internally as a sentinel value for "absent." Allowing null would make it impossible to distinguish "key absent" from "key maps to null."

## Size is Approximate

```java
map.size();  // Returns approximate size — not guaranteed exact under concurrency
```

Under high concurrency, `size()` may not reflect the most recent state. For accurate counts, use `mappingCount()` (returns long) or atomic counters.

---

# 4. Atomic Compound Operations

This is where `ConcurrentHashMap` becomes very powerful.

Regular `HashMap` (unsafe under concurrency):

```java
// Race condition: two threads can both read null, both insert!
if (!map.containsKey("key")) {
    map.put("key", computeValue());  // NOT ATOMIC
}
```

`ConcurrentHashMap` atomic operations:

```java
// ATOMIC: only one thread can insert if absent
map.putIfAbsent("key", value);

// ATOMIC: compute and insert only if key is absent
map.computeIfAbsent("key", k -> expensiveCompute(k));

// ATOMIC: update existing value
map.computeIfPresent("key", (k, oldValue) -> oldValue + 1);

// ATOMIC: compute regardless of presence
map.compute("key", (k, oldValue) -> {
    if (oldValue == null) return 1;
    return oldValue + 1;
});

// ATOMIC: merge old and new value
map.merge("key", 1, Integer::sum);
```

These compound operations are internally synchronized at the bucket level — they are atomically safe.

---

# 5. ConcurrentHashMap vs HashMap vs Hashtable vs synchronizedMap

```text
Feature                  | HashMap    | Hashtable  | synchronizedMap | ConcurrentHashMap
-------------------------+------------+------------+-----------------+------------------
Thread-safe              | NO         | YES        | YES             | YES
Null keys allowed        | YES        | NO         | YES             | NO
Null values allowed      | YES        | NO         | YES             | NO
Locking                  | None       | Full table | Full table      | Bucket-level
Concurrent reads         | Unsafe     | Blocked    | Blocked         | Non-blocking
Concurrent writes        | Unsafe     | Serial     | Serial          | Parallel (diff buckets)
Performance (concurrent) | N/A        | Poor       | Poor            | Excellent
CAS atomic ops           | NO         | NO         | NO              | YES
Iterator fail-safe       | NO (fail-fast)| YES     | YES             | YES (weakly consistent)
```

---

# 6. Iterator Behavior — Weakly Consistent

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);

for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry);
    map.put("c", 3);  // concurrent modification WHILE iterating
    // No ConcurrentModificationException!
    // But "c" may or may not appear in this iteration
}
```

`ConcurrentHashMap` iterators are **weakly consistent** — they do not throw `ConcurrentModificationException`, but they reflect the state of the map at or since the iterator was created. Entries added after the iterator started may or may not be visible.

Compare with `HashMap` iterator which is **fail-fast** — throws `ConcurrentModificationException` on concurrent modification.

---

# 7. ConcurrentHashMap Internals — Load Factor and Resize

```text
Default initial capacity: 16 buckets
Default load factor: 0.75

When entries > capacity × loadFactor → resize (double capacity)

Java 8 resize is CONCURRENT and INCREMENTAL:
  Multiple threads help transfer buckets to new array
  ForwardingNode placed in old bucket to redirect reads
  New writes go to new array
  Old array slowly migrated
```

This means resize does not cause a long pause — it's spread across multiple threads cooperatively.

---

# 8. Real-Life Use Cases

## Use Case 1: In-Memory Cache (Microservice)

```java
@Service
public class UserCacheService {

    private final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();

    public User getUser(String userId) {
        // computeIfAbsent is atomic — only one thread loads even if multiple threads miss
        return cache.computeIfAbsent(userId, id -> userRepository.findById(id));
    }

    public void invalidate(String userId) {
        cache.remove(userId);
    }
}
```

```text
Thread 1: getUser("user-42") → cache miss → computeIfAbsent → loads from DB → caches
Thread 2: getUser("user-42") → SAME TIME → computeIfAbsent → waits for Thread 1 → gets cached
Thread 3: getUser("user-99") → completely independent → runs in parallel → no blocking
```

## Use Case 2: Rate Limiting (Per-Endpoint Counter)

```java
@Component
public class RateLimiter {

    // endpoint → request count
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public boolean isAllowed(String endpoint) {
        AtomicLong counter = counters.computeIfAbsent(endpoint, k -> new AtomicLong(0));
        long count = counter.incrementAndGet();
        return count <= 100;  // max 100 per minute
    }
}
```

## Use Case 3: Request Deduplication

```java
// Prevent duplicate processing of the same event
private final ConcurrentHashMap<String, Boolean> processedEvents = new ConcurrentHashMap<>();

public void processEvent(Event event) {
    // putIfAbsent is atomic — only one thread processes each eventId
    Boolean existing = processedEvents.putIfAbsent(event.getId(), Boolean.TRUE);
    if (existing != null) {
        return;  // already processed
    }
    doProcess(event);
}
```

## Use Case 4: Metrics Aggregation

```java
// Aggregate request counts per service
private final ConcurrentHashMap<String, LongAdder> requestCounts = new ConcurrentHashMap<>();

public void recordRequest(String service) {
    requestCounts.computeIfAbsent(service, k -> new LongAdder()).increment();
}

public long getCount(String service) {
    LongAdder adder = requestCounts.get(service);
    return adder == null ? 0 : adder.sum();
}
```

Note: `LongAdder` is preferred over `AtomicLong` for very high-frequency increments — it reduces contention by maintaining multiple counters internally.

## Use Case 5: Distributed System Local Cache (per-node)

```text
Distributed system:
  Redis: shared cache for all nodes
  ConcurrentHashMap: per-node L1 cache (ultra-fast, no network)

Strategy:
  1. Check local ConcurrentHashMap
  2. If miss → check Redis
  3. If miss → query DB → store in Redis → store in local CHM

Thread 1 on node A: fast local CHM read
Thread 2 on node A: fast local CHM read
(no Redis round-trip for hot data)
```

---

# 9. When NOT to Use ConcurrentHashMap

```text
1. You need null keys or values
   → Use HashMap with external synchronization

2. You need strongly consistent size()
   → External atomic counter or use mappingCount() carefully

3. You need sorted key iteration
   → ConcurrentSkipListMap

4. You need compound operations on multiple keys atomically
   → ConcurrentHashMap only atomizes per-key operations
   → For multi-key atomicity, use external locks

5. You need to prevent all reads during a write
   → ReadWriteLock with HashMap
```

---

# 10. Performance Characteristics

```text
Concurrent reads:   O(1) — completely non-blocking
put (empty bucket): O(1) — CAS, no lock
put (collision):    O(1) amortized, O(k) where k = bucket size
get:                O(1) average, O(log n) for treeified bucket

Under 16+ threads writing different keys:
  ConcurrentHashMap throughput → near-linear scaling
  Hashtable / synchronizedMap → linear degradation (more threads = worse)
```

Visual comparison under 32 concurrent writer threads:

```text
Throughput (ops/sec):

ConcurrentHashMap: ████████████████████████████████████ 100%
synchronizedMap:   ██████                                 18%
Hashtable:         ██████                                 17%
HashMap (unsafe):  ████████████████████████████████████  (but data corrupted!)
```

---

# Interview Preparation — ConcurrentHashMap

---

## Q1: How does ConcurrentHashMap achieve thread-safety?

**Answer:**

Java 8 ConcurrentHashMap uses two mechanisms:

1. **CAS (Compare-And-Swap)**: for operations on empty buckets — no lock needed, atomic at hardware level
2. **Synchronized on bucket head**: for operations on non-empty buckets — only locks that specific bucket, not the whole map

Reads (`get`) use volatile reads — completely non-blocking. Multiple reads run simultaneously.

Writes to different buckets run simultaneously (different locks). Only writes to the same bucket contend.

This gives much higher throughput than `Hashtable` or `synchronizedMap` which use a single global lock.

---

## Q2: What changed in ConcurrentHashMap between Java 7 and Java 8?

**Answer:**

**Java 7**: Segment-based locking. The map was divided into 16 Segments (by default), each a mini-HashMap with its own ReentrantLock. Max concurrency = number of segments.

**Java 8**: Bucket-level CAS + synchronized. No more Segments. Each array bucket is individually lockable. Max concurrency = number of buckets (much higher). Also introduced treeification (buckets > 8 entries become Red-Black Trees → O(log n) instead of O(n) for hash collisions).

Java 8 is significantly faster and more memory-efficient.

---

## Q3: Why does ConcurrentHashMap not allow null keys or values?

**Answer:**

`ConcurrentHashMap` uses `null` internally as a sentinel value to indicate "absent" in its lock-free read path. If null were a valid value, the code couldn't distinguish between "key doesn't exist" and "key maps to null" without additional locking — which would defeat the non-blocking read design.

`HashMap` allows null because it doesn't need this distinction in a thread-safe way.

---

## Q4: What is the difference between putIfAbsent and computeIfAbsent?

**Answer:**

```java
// putIfAbsent: value is computed BEFORE the call
// Even if key exists, the value argument is already created
map.putIfAbsent("key", new ExpensiveObject());  // creates ExpensiveObject even if key exists!

// computeIfAbsent: function called ONLY if key is absent
// Lazy — function only runs if needed, and only ONCE (even if multiple threads race)
map.computeIfAbsent("key", k -> new ExpensiveObject());  // creates ONLY if absent
```

`computeIfAbsent` is preferred for caching patterns — it's lazy, atomic, and avoids wasteful object creation.

---

## Q5: Is ConcurrentHashMap iterator thread-safe?

**Answer:**

Yes, but **weakly consistent** — not strongly consistent.

- Does NOT throw `ConcurrentModificationException` (unlike `HashMap`'s fail-fast iterator)
- Reflects the state of the map at the time the iterator was created
- May or may not show elements added after the iterator started
- May or may not show elements removed after the iterator started

This is acceptable for most use cases (monitoring, metrics, cache iteration). If you need a consistent snapshot, take a `new HashMap<>(concurrentHashMap)` — but this is expensive.

---

## Q6: When would you use ConcurrentSkipListMap instead of ConcurrentHashMap?

**Answer:**

Use `ConcurrentSkipListMap` when you need **sorted key iteration** — i.e., you want to iterate keys in natural or custom order.

```java
ConcurrentSkipListMap<String, Integer> sorted = new ConcurrentSkipListMap<>();
sorted.put("banana", 2);
sorted.put("apple", 1);
sorted.put("cherry", 3);
// Iteration order: apple, banana, cherry (sorted)

// Also supports range operations:
sorted.subMap("apple", "cherry");  // keys between apple and cherry
sorted.firstKey();                  // lowest key
sorted.tailMap("banana");          // keys >= banana
```

`ConcurrentHashMap` does not maintain order — use it when order doesn't matter (usually). `ConcurrentSkipListMap` is slower (O(log n) vs O(1) average) but provides sorted ordering.

---

## Q7: How would you implement an LRU cache using ConcurrentHashMap?

**Answer:**

Pure `ConcurrentHashMap` doesn't support LRU eviction. Combine it with `LinkedHashMap` — but this doesn't scale concurrently.

Better approach: use **Caffeine** (which uses ConcurrentHashMap internally + W-TinyLFU eviction):

```java
Cache<String, User> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterAccess(1, TimeUnit.HOURS)
    .build();

cache.get("user-1", k -> userRepository.findById(k));
```

Or for a simple interview answer: `LinkedHashMap` wrapped in `synchronizedMap` with `accessOrder=true` and `removeEldestEntry()` override — but this is single-threaded only.

---

## Q8: How does ConcurrentHashMap handle high-concurrency write scenarios?

**Answer:**

At very high write concurrency to the same key:

1. Multiple threads try to put to the same bucket
2. One thread acquires the bucket's `synchronized` lock
3. Others wait (briefly — just for that bucket)
4. After the lock is released, next thread proceeds

For even higher throughput, use:
- `LongAdder` instead of `AtomicLong` for counters (striped counter, less CAS contention)
- Distribute load across multiple keys where possible
- `compute()` / `merge()` instead of get-then-put patterns

The map itself is not the bottleneck when keys are diverse — only when many threads contend on the exact same key.
