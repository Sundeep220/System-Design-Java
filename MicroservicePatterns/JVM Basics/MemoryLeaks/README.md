# Memory Leaks in Java — Deep Dive

Java has garbage collection, so many developers assume Java cannot have memory leaks. This is **wrong**.

The core mental model is:

> **A Java memory leak occurs when objects are no longer needed by the application but are still strongly reachable — so the GC cannot collect them. Memory grows over time, eventually causing OutOfMemoryError.**

---

# 1. The Misconception

```text
Wrong mental model:
  GC = automatic memory management = no memory leaks

Correct mental model:
  GC = collects UNREACHABLE objects
  Memory leak = objects are REACHABLE but no longer NEEDED
  GC cannot distinguish "needed" from "unreachable"
```

The distinction:

```text
Not needed by app logic: GC DOES NOT KNOW THIS
Unreachable (no references): GC CAN collect this
```

---

# 2. Classic Memory Leak Pattern

```java
public class LeakyCache {

    private static final List<byte[]> cache = new ArrayList<>();

    public static void store(byte[] data) {
        cache.add(data);  // never removed!
    }
}
```

```text
GC root: static field `cache`
    ↓
    ArrayList
    ↓
    byte[] → byte[] → byte[] → byte[] → ... (growing forever)
```

All reachable → GC cannot collect → heap grows → OOM.

---

# 3. The Seven Most Common Memory Leak Patterns

---

## 3.1 Static Field Accumulation

```java
class RequestTracker {
    // LEAK: static list never cleared
    private static final List<RequestLog> allLogs = new ArrayList<>();

    public static void log(RequestLog log) {
        allLogs.add(log);  // grows with every request
    }
}
```

Fix: use bounded queue, or clear periodically:

```java
private static final Deque<RequestLog> recentLogs = new ArrayDeque<>();

public static synchronized void log(RequestLog log) {
    recentLogs.addLast(log);
    if (recentLogs.size() > 1000) {
        recentLogs.removeFirst();  // bounded
    }
}
```

---

## 3.2 Cache Without Eviction

```java
@Service
public class ProductService {
    // LEAK: cache grows indefinitely
    private final Map<String, Product> cache = new HashMap<>();

    public Product getProduct(String id) {
        return cache.computeIfAbsent(id, productRepository::findById);
    }
}
```

With 1 million unique product IDs → 1 million objects in memory forever.

Fix: use a cache with eviction:

```java
private final Cache<String, Product> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build();
```

---

## 3.3 Listener / Observer Not Removed

```java
class EventBus {
    private final List<EventListener> listeners = new ArrayList<>();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    // No unsubscribe method!
}

// Each component subscribes...
class OrderProcessor {
    public OrderProcessor(EventBus bus) {
        bus.subscribe(this::handleEvent);  // registered, never removed
    }
}
```

```text
EventBus (static/singleton)
    ↓
    listeners list
    ↓
    OrderProcessor instance (can't be GC'd even after OrderProcessor's work is done)
    ↓
    all objects OrderProcessor holds reference to
```

Fix: implement unsubscribe and call it when the component is disposed.

```java
void shutdown() {
    eventBus.unsubscribe(this::handleEvent);
}
```

---

## 3.4 ThreadLocal Not Cleaned

ThreadLocal is per-thread. In a thread pool, threads are reused:

```java
private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();

// In a request handler:
public void handleRequest(User user) {
    userContext.set(new UserContext(user));
    processRequest();
    // LEAK: forgot to call userContext.remove()!
}
```

```text
Thread pool thread (lives forever)
    ↓
    ThreadLocalMap (internal to Thread)
    ↓
    UserContext (stored per-thread, never cleared)
```

With 100 threads in pool and each UserContext holding 1MB:
100MB permanently held.

Fix: always use try-finally:

```java
public void handleRequest(User user) {
    try {
        userContext.set(new UserContext(user));
        processRequest();
    } finally {
        userContext.remove();  // always clean up
    }
}
```

---

## 3.5 Unclosed Resources Keeping Objects Alive

```java
public void processFile(String path) throws Exception {
    FileInputStream fis = new FileInputStream(path);
    // process file...
    // LEAK: fis not closed! Stream + its buffer objects stay alive
}
```

With try-with-resources:

```java
public void processFile(String path) throws Exception {
    try (FileInputStream fis = new FileInputStream(path)) {
        // process file...
    }  // automatically closed
}
```

---

## 3.6 Inner Class Holding Outer Class Reference

Non-static inner classes implicitly hold a reference to the outer class:

```java
public class OuterService {

    private final LargeData data = new LargeData();  // 100MB

    public Runnable createTask() {
        return new Runnable() {  // ANONYMOUS INNER CLASS
            @Override
            public void run() {
                // implicitly holds reference to OuterService.this
                // and therefore to LargeData!
            }
        };
    }
}

// Usage:
Runnable task = outerService.createTask();
executor.submit(task);  // task lives → OuterService lives → LargeData lives
```

Fix: use static inner class or lambda (which doesn't capture outer `this` unless needed):

```java
public Runnable createTask() {
    return () -> doSomeWork();  // doesn't implicitly capture OuterService.this
}
```

---

## 3.7 equals() / hashCode() Violations in Collections

```java
class BrokenKey {
    String id;

    // equals() and hashCode() NOT implemented
}

Map<BrokenKey, String> map = new HashMap<>();
BrokenKey key = new BrokenKey("order-1");
map.put(key, "value");

// Later:
BrokenKey sameKey = new BrokenKey("order-1");
map.get(sameKey);  // returns null! Because hashCode is different

map.put(sameKey, "value");  // inserts a SECOND entry!
```

Result: the map grows endlessly with "duplicate" keys that aren't equal due to missing `equals()`/`hashCode()`.

---

# 4. Detecting Memory Leaks

## Pattern 1: Monotonically Growing Old Generation

```text
Plot "Heap used after Full GC" over time:

Healthy:
  GC|    GC|    GC|
    200MB  202MB  201MB  (stable, ~200MB baseline)

Leak:
  GC|    GC|    GC|
    200MB  250MB  310MB  (growing! baseline rises)
```

This is the clearest signal of a memory leak.

## Pattern 2: Frequent Full GC

```text
Healthy: Full GC once per day
Leak: Full GC every 30 minutes, then every 10 minutes, then every minute
```

GC is trying to free space but can't — because objects are reachable.

## Pattern 3: OutOfMemoryError: Java heap space

After a memory leak runs long enough, the heap fills completely and OOM is thrown.

---

# 5. Memory Leak Investigation Workflow

```text
Step 1: Enable GC logging
  -Xlog:gc*:file=gc.log

Step 2: Monitor heap usage over time
  Plot "Old Gen used after GC"
  Growing baseline → memory leak

Step 3: Take a heap dump
  jmap -dump:live,format=b,file=heap.hprof <pid>
  Or: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/

Step 4: Analyze with Eclipse MAT
  Open heap.hprof
  Run "Leak Suspects" report
  Look for:
    - Largest retained objects
    - Which GC root keeps them alive
    - Object count growing over time

Step 5: Fix the root cause
  Remove unbounded cache
  Add eviction policy
  Fix listener registration
  Add ThreadLocal.remove()
  Fix resource lifecycle

Step 6: Verify fix
  Redeploy and monitor Old Gen growth
  Should be stable now
```

---

# 6. WeakReference and SoftReference — Leak-Safe Caching

Java provides reference types that allow GC to collect objects even if referenced:

### WeakReference

```java
Map<Key, WeakReference<Value>> cache = new WeakHashMap<>();
```

If the Key has no other strong references → WeakHashMap entry is GC'd.

```text
Strong reference:   GC will NEVER collect
Soft reference:     GC MAY collect when memory is low
Weak reference:     GC collects at NEXT GC cycle
Phantom reference:  object already finalized, used for cleanup hooks
```

### WeakHashMap Example

```java
// ClassLoader-to-metadata cache
Map<ClassLoader, ClassMetadata> cache = new WeakHashMap<>();
// When ClassLoader is unloaded, its entry is automatically removed
```

### SoftReference for Caching

```java
SoftReference<byte[]> buffer = new SoftReference<>(new byte[1024 * 1024]);

byte[] b = buffer.get();  // null if GC collected it
if (b == null) {
    b = new byte[1024 * 1024];  // recreate if needed
    buffer = new SoftReference<>(b);
}
```

SoftReferences are cleared before OOM, so they can safely cache data without causing OOM.

---

# 7. Real-life: Microservice Memory Leak

```text
Spring Boot microservice — Order Service
Initial heap: 512MB Young + 2GB Old
Normal operation: Old Gen stable at 400MB

After 2 hours of traffic:
  Old Gen: 800MB
After 4 hours:
  Old Gen: 1.4GB
After 6 hours:
  Old Gen: 1.9GB
After 7 hours:
  Full GC every 5 minutes (desperate)
After 8 hours:
  OutOfMemoryError: Java heap space
  Pod crashes in Kubernetes
```

Investigation:

```text
Heap dump analysis:
  Top object: HashMap$Entry → 1.2GB retained
  GC root: OrderService.orderCache (static field)
  orderCache: HashMap, 2.4 million entries
```

Root cause:

```java
@Service
public class OrderService {
    // LEAK: no size limit, no eviction!
    private static final Map<String, Order> orderCache = new HashMap<>();

    public Order getOrder(String id) {
        return orderCache.computeIfAbsent(id, repo::findById);
    }
}
```

Every unique order ID ever accessed stays in memory forever.
With 2.4M unique orders, each ~500 bytes → 1.2GB.

Fix:

```java
private final Cache<String, Order> orderCache = Caffeine.newBuilder()
    .maximumSize(50_000)     // max 50K entries
    .expireAfterAccess(30, TimeUnit.MINUTES)  // evict inactive
    .build();
```

---

# Interview Preparation — Memory Leaks

---

## Q1: Can Java have memory leaks? Explain.

**Answer:**

Yes. Java memory leaks occur when objects are no longer needed by the application but are still strongly reachable — so GC cannot collect them.

GC collects unreachable objects. But "no longer needed" and "unreachable" are different concepts. If a static List holds references to objects the app no longer uses, those objects are still reachable from the GC root (static field) → GC cannot collect them → memory grows.

Classic example: an unbounded cache, a list that grows forever, listeners never unregistered.

---

## Q2: What are the most common causes of Java memory leaks?

**Answer:**

1. **Static collections without eviction**: `static List<T>` or `static Map<K,V>` that keeps growing
2. **Caches without size limit**: caching every unique request without eviction
3. **Listeners not removed**: event listeners registered on long-lived objects, never unregistered
4. **ThreadLocal not cleaned**: in thread pools, ThreadLocal values persist across requests if not removed
5. **Inner class references**: non-static inner classes/anonymous classes hold implicit reference to outer class
6. **Unclosed resources**: streams, connections that aren't closed keep their internal buffers alive
7. **Wrong equals/hashCode**: causes duplicate Map entries that accumulate indefinitely

---

## Q3: How do you detect a memory leak in production?

**Answer:**

1. **Monitor Old Gen usage after each GC**: if "Old Gen used after GC" grows monotonically over time → memory leak
2. **Watch Full GC frequency**: increasing frequency (once/day → once/hour → once/minute) signals growing heap pressure
3. **Watch for OOM**: `OutOfMemoryError: Java heap space` is the end result
4. **Take a heap dump**: use `jmap -dump:live,format=b,file=heap.hprof <pid>` or `-XX:+HeapDumpOnOutOfMemoryError`
5. **Analyze with Eclipse MAT**: run Leak Suspects report, find largest retained heap, trace GC root path

---

## Q4: What is the difference between a strong, soft, weak, and phantom reference?

**Answer:**

| Reference | GC behavior | Use case |
|---|---|---|
| Strong | Never collected while reference exists | Normal object references |
| Soft | Collected when JVM needs memory (before OOM) | Memory-sensitive caches |
| Weak | Collected at next GC cycle | Caches where key has other owners (WeakHashMap) |
| Phantom | Object already finalized; used for cleanup notification | Resource cleanup hooks |

```java
SoftReference<byte[]> cache = new SoftReference<>(data);  // safer cache
WeakReference<Object> ref = new WeakReference<>(obj);      // weak cache entry
```

---

## Q5: How does ThreadLocal cause a memory leak?

**Answer:**

ThreadLocal stores values per-thread in a `ThreadLocalMap` inside each `Thread` object.

In a thread pool, threads are never destroyed — they're reused. If you set a ThreadLocal value but forget to call `ThreadLocal.remove()`, the value stays in that thread's map indefinitely:

```java
userContext.set(new UserContext(user));  // stored in thread's map
// ... forgot: userContext.remove()
// Thread returns to pool
// Next request gets a DIFFERENT thread
// But this thread still has the old UserContext
```

Fix: always use try-finally:
```java
try {
    userContext.set(new UserContext(user));
    processRequest();
} finally {
    userContext.remove();
}
```

---

## Q6: What is a WeakHashMap and when would you use it?

**Answer:**

`WeakHashMap` uses `WeakReference` for its keys. When a key has no other strong references, the entry is automatically removed by GC.

Use case: associating metadata with objects without preventing their GC:

```java
// Associate extra info with ClassLoader without keeping it alive
Map<ClassLoader, ClassStats> stats = new WeakHashMap<>();
stats.put(classLoader, new ClassStats());
// When classLoader is no longer referenced elsewhere → entry auto-removed
```

Another use: caching where the key is the "owner":

```java
// Cache expensive computation results tied to the source object
WeakHashMap<Request, ComputedResult> cache = new WeakHashMap<>();
```

When the `Request` object is no longer alive → cache entry auto-evicted. This prevents the cache from becoming a memory leak.

---

## Q7: How do you fix a memory leak in a Spring Boot microservice?

**Answer:**

Typical investigation:

1. **Observe**: heap used after GC growing monotonically in monitoring
2. **Confirm**: take heap dump with `-XX:+HeapDumpOnOutOfMemoryError`
3. **Analyze**: Eclipse MAT → Leak Suspects → find largest retained heap
4. **Identify**: trace GC root path — usually static field → collection → objects
5. **Fix**: add eviction to cache (Caffeine), add size limit, fix listener lifecycle

Concrete fixes:
```java
// Instead of:
private static Map<String, Object> cache = new HashMap<>();

// Use:
private Cache<String, Object> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build();
```

Prevention:
- Always use bounded caches (Caffeine, Guava)
- Always clean ThreadLocal in finally block
- Always unsubscribe listeners when component is destroyed
- Use try-with-resources for all I/O
