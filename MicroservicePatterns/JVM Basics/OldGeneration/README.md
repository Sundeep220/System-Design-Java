# Old Generation — Deep Dive

The Old Generation (also called Tenured Generation) is the part of the heap where **long-lived objects reside**. It is larger than the Young Generation and collected less frequently, but its GC cycles are much more expensive.

The core mental model is:

> **Objects that survive many Minor GCs are promoted to the Old Generation. When the Old Generation fills up, a Major or Full GC runs — which is far more expensive than a Minor GC.**

---

# 1. Old Generation in Context

```text
JVM Heap
├── Young Generation (new objects, short-lived)
│   ├── Eden
│   ├── Survivor 0
│   └── Survivor 1
│
└── Old Generation  ← long-lived objects live here
    (much larger than Young Gen)
    (collected by Major GC / Full GC)
```

Typical size ratio:

```bash
-XX:NewRatio=2    # Old:Young = 2:1
                  # With 6GB heap: 4GB Old, 2GB Young
```

---

# 2. What Lives in Old Generation?

### 1. Application Caches

```java
public class UserCache {
    private static final Map<Long, User> cache = new ConcurrentHashMap<>();
    public static void put(Long id, User user) {
        cache.put(id, user);
    }
}
```

### 2. Connection Pools

```java
DataSource dataSource = HikariPool.getDataSource();
// Connection objects kept alive in the pool
```

### 3. Spring Application Context

```java
@Service
public class OrderService { ... }
// Spring beans (singletons) live for app lifetime
```

### 4. Session Data

```java
HttpSession session = request.getSession();
session.setAttribute("cart", cart);
```

### 5. Large Byte Arrays / Buffers

```java
byte[] buffer = new byte[10 * 1024 * 1024];  // 10MB
// Large arrays allocated directly to Old Gen
```

---

# 3. How Objects Enter Old Generation

### Path 1: Tenuring Threshold

```text
Object born in Eden
→ Survives Minor GC: age = 1
→ Survives again: age = 2
...
→ Age = 15 → Promoted to Old Gen
```

### Path 2: Survivor Overflow (Premature Promotion)

```text
Survivor space too small
→ Excess objects promoted to Old Gen regardless of age
```

### Path 3: Large Object Allocation

```java
byte[] largeArray = new byte[5 * 1024 * 1024];  // 5MB
// Goes directly to Old Gen (or Humongous region in G1)
```

---

# 4. Major GC vs Full GC

### Major GC (Old Generation GC)

```text
Collects Old Generation
Duration: longer than Minor GC (10s ms to seconds)
Can be concurrent in G1/ZGC/Shenandoah
```

### Full GC

```text
Collects entire heap (Young + Old + Metaspace)
Completely Stop-The-World
Longest pause (seconds, even minutes on large heaps)
Triggered by:
  - Promotion failure
  - Explicit System.gc()
  - Metaspace full
  - Humongous object cannot fit
```

---

# 5. Old Generation GC — Mark-Sweep-Compact

Traditional collectors use mark-sweep-compact for Old Gen:

### Phase 1: Mark

```text
Old Generation:
+----+----+----+----+----+----+----+
| S  | D  | S  | D  | D  | S  | S  |
+----+----+----+----+----+----+----+
  yes  no   yes  no   no   yes  yes

S = Survived (reachable), D = Dead (unreachable)
```

### Phase 2: Sweep

Reclaim memory of dead objects, leaving fragmented free gaps.

### Phase 3: Compact

```text
Move all live objects to one end:
+----+----+----+----+    free space    +
| S  | S  | S  | S  |                 |
+----+----+----+----+                 |
```

All free space is now contiguous. Expensive but eliminates fragmentation.

---

# 6. G1GC and Old Generation

G1GC handles Old Generation as a set of regions, not one contiguous space:

```text
G1 Heap:
[E][E][E][S][S][O][O][O][H][E][O][E][O]

E = Eden, S = Survivor, O = Old, H = Humongous
```

G1 uses concurrent marking:

```text
Phase 1: Initial Mark (STW — brief, piggybacks on Minor GC)
Phase 2: Root Region Scan (concurrent)
Phase 3: Concurrent Mark (concurrent — runs while app runs)
Phase 4: Remark (STW — brief)
Phase 5: Cleanup (partially STW)
```

Then G1 runs Mixed GC — collects Young + selected Old regions with most garbage.

---

# 7. Old Generation Fragmentation

Without compaction (like in CMS — deprecated), Old Gen becomes fragmented:

```text
Old Gen after many collections:
+--+  +--+  +--+  +--+  +--+
|L |  |L |  |L |  |L |  |L |
+--+  +--+  +--+  +--+  +--+
   gap  gap   gap   gap

Total free: 200MB
Largest contiguous free block: 20MB
```

A 50MB object cannot be allocated even though 200MB is "free."
This triggers a Full GC.

---

# 8. Old Generation and Memory Leaks

Memory leaks almost always manifest in Old Generation:

```text
Memory leak = objects no longer needed but still referenced
           = long-lived, survive many GCs
           = promoted to Old Gen
           = never collected
```

Classic growing pattern:

```text
Time 0:  Old Gen: [====50%====]
Time 1h: Old Gen: [========70%========]
Time 2h: Old Gen: [===========85%===========]
Time 3h: Old Gen: [=============95%=============]
Time 4h: OOM
```

Diagnosis: heap used after each GC never decreases — only grows.

---

# 9. Real-life Scenarios

### Scenario 1: Healthy Application

```text
Old Gen usage:
  After startup: 200MB (Spring context, connection pools)
  Under load:    400MB (sessions, caches — bounded)
  After Full GC: 200MB (back to baseline)
```

### Scenario 2: Unbounded Cache

```java
@Service
public class ProductCache {
    private Map<String, Product> cache = new HashMap<>(); // NO eviction!
    
    public void cache(String id, Product p) {
        cache.put(id, p);  // grows forever
    }
}
```

Fix: use Caffeine with eviction:

```java
Cache<String, Product> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build();
```

### Scenario 3: Session Leak

```java
// session created but never invalidated
HttpSession session = request.getSession(true);
session.setAttribute("data", largeObject);
```

Each user gets a session. Sessions live in Old Gen. With 100k users and no timeout → OOM.

---

# 10. Old Generation Tuning

```bash
# Total heap
-Xmx8g

# Young:Old ratio
-XX:NewRatio=2   # Old gets 2/3 of heap

# G1GC pause target
-XX:MaxGCPauseMillis=200

# G1GC marking trigger
-XX:InitiatingHeapOccupancyPercent=45   # start concurrent marking at 45% Old Gen full

# G1GC Mixed GC aggressiveness
-XX:G1MixedGCCountTarget=8             # spread Old Gen collection over 8 Mixed GCs
```

---

# 11. Monitoring Old Generation

Key metrics:

```text
Old Gen occupancy after each GC
   Stable = healthy
   Monotonically growing = memory leak

Frequency of Major/Full GC
   Should be rare (hours between Full GCs)
   Frequent (minutes) = problem

Full GC pause duration
   With G1: target < 500ms
   With ZGC: target < 10ms

Promotion rate (Young → Old per second)
   High rate = premature promotion, small Survivor spaces
```

GC log indicators:

```text
Healthy:
  [GC pause (Young) 120ms]
  [GC pause (Mixed) 180ms]  <- G1 occasionally collects some Old

Unhealthy:
  [Full GC (Ergonomics) 3.2 sec]   <- Full GC happening
  [Full GC (Ergonomics) 4.1 sec]   <- repeatedly
  [Full GC (Ergonomics) 4.8 sec]   <- worsening = memory leak
```

---

# Interview Preparation — Old Generation

---

## Q1: What is the Old Generation and what kinds of objects live there?

**Answer:**

The Old Generation (Tenured Generation) holds long-lived objects that have survived many Minor GC cycles. Objects arrive here via:

1. **Tenuring**: survived `MaxTenuringThreshold` (default 15) Minor GCs
2. **Premature promotion**: Survivor space overflow
3. **Large object allocation**: objects too large for Eden/TLAB

Typical occupants: Spring singleton beans, database connection pools, application caches, user sessions, thread pools, large byte buffers.

---

## Q2: How is Old Generation collected differently from Young Generation?

**Answer:**

| | Young Generation | Old Generation |
|---|---|---|
| Algorithm | Copying collector | Mark-Sweep-Compact |
| STW | Yes (brief) | Longer (or concurrent phases in G1/ZGC) |
| Pause duration | Milliseconds | 10s ms to seconds |
| Frequency | Very frequent | Infrequent |
| Fragmentation | None (copying) | Possible without compaction |

Young Gen uses fast copying (most objects are dead, few copied). Old Gen requires marking and compacting a large space with many live objects.

---

## Q3: What is a Full GC and when is it triggered?

**Answer:**

Full GC collects the entire heap in a Stop-The-World pause.

Triggers:
- **Promotion failure**: Old Gen has no room for objects promoted from Young Gen
- **Explicit `System.gc()`**: called by application or frameworks
- **Metaspace full**: too many classes loaded
- **Concurrent mode failure**: in CMS/G1, concurrent GC couldn't finish before heap was full
- **Humongous allocation failure**: in G1, cannot find contiguous regions for large object

Full GC is the most disruptive event. Pauses can range from seconds to minutes. Goal should be to have Full GC rarely or never in a healthy production system.

---

## Q4: What is "promotion failure" in G1GC?

**Answer:**

Promotion failure happens when G1GC cannot find enough free regions to evacuate (copy) live objects during a Young or Mixed GC.

During evacuation, G1 copies live objects from collected regions to free regions. If no free regions are available:

```text
Evacuation fails
→ G1 falls back to Full GC (Stop-The-World)
→ Long pause
→ Objects left in place (not copied)
→ Very expensive recovery
```

Prevention:
- Keep enough headroom in Old Gen (don't let it fill to 100%)
- Tune `InitiatingHeapOccupancyPercent` to start marking earlier
- Increase heap size

---

## Q5: What is concurrent mode failure?

**Answer:**

Concurrent mode failure was a CMS (now deprecated) concept but applies broadly.

It occurs when the concurrent GC cycle (marking) cannot complete before the Old Generation becomes full. The GC was running concurrently with the application — but the application kept allocating and promoting objects faster than the concurrent GC could mark.

Result: forced Full GC to prevent OOM.

In G1GC, the equivalent is when concurrent marking is too slow relative to heap filling rate, causing evacuation failure.

Fix: increase heap, tune `InitiatingHeapOccupancyPercent` to start marking earlier (lower value).

---

## Q6: How does G1GC prevent long Full GC pauses in Old Generation?

**Answer:**

G1GC uses incremental Old Generation collection via Mixed GC:

1. **Concurrent Marking**: identify garbage in Old regions concurrently (no STW)
2. **Garbage First**: collect Old regions with most garbage first (highest ROI)
3. **Mixed GC**: collect Young + selected Old regions incrementally, each within pause target
4. **Incremental cleanup**: spread Old Gen collection over multiple GC cycles

Instead of one huge Full GC, G1 does many small Mixed GCs:

```text
Without G1: 
  [   Full GC 3 seconds   ]

With G1:
  [Mixed 150ms][Mixed 180ms][Mixed 160ms]
  (same work, spread over time, each < 200ms)
```

---

## Q7: What causes the Old Generation to grow over time (memory leak signs)?

**Answer:**

Old Gen grows over time due to:

1. **Unbounded caches**: `HashMap` or `List` that keeps adding entries without eviction
2. **Listener leaks**: event listeners registered but never removed
3. **Static collections**: `static List<T>` accumulating objects forever
4. **ThreadLocal not cleaned**: `ThreadLocal.remove()` never called in thread pools
5. **Session accumulation**: sessions created but never invalidated
6. **Connection/resource leaks**: unclosed resources keeping their objects alive

Diagnostic signal: plot "Old Gen used after Full GC" over time. If this value grows — it's a memory leak. GC can't help because the objects are still reachable.

Use a heap dump to identify which objects are growing.
