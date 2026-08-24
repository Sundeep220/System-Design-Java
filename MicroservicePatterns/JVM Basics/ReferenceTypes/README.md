# Reference Types — Deep Dive

Java has four types of references that control how the GC treats objects. Understanding them is essential for building memory-efficient caches, avoiding memory leaks, and implementing correct resource cleanup.

> **The four reference types are Java's mechanism for communicating with the GC: "this object is critical, never collect it" (Strong) vs "collect it if you need to" (Soft) vs "collect it at next GC" (Weak) vs "tell me after you've collected it" (Phantom).**

---

# 1. The Problem

Normally in Java, any object you hold a reference to cannot be collected:

```java
User user = new User();
// user is strongly referenced — GC CANNOT collect it
```

But sometimes you want the GC to be able to collect an object if memory is needed, while you still have a way to reference it. Regular (strong) references don't allow this.

The four reference types solve this:

```text
Strong reference:   GC NEVER collects (while reference exists)
Soft reference:     GC collects ONLY when heap is low on memory
Weak reference:     GC collects at NEXT GC cycle
Phantom reference:  Object already dead — used for post-mortem cleanup
```

---

# 2. Strong References (Default)

Every regular Java reference is a strong reference:

```java
Object obj = new Object();  // strong reference
List<String> list = new ArrayList<>();  // strong reference
```

As long as a strong reference exists anywhere (reachable from GC roots), the object **cannot** be garbage collected.

Setting to null breaks the strong reference:

```java
Object obj = new Object();  // strong reference
obj = null;  // reference gone → object eligible for GC (if no other refs)
```

Strong references are the reason Java memory leaks happen — objects you no longer need but still strongly reference cannot be collected.

---

# 3. Soft References (java.lang.ref.SoftReference)

A soft reference is collected **only when the JVM is running low on memory** — just before it would throw `OutOfMemoryError`.

```java
SoftReference<byte[]> softRef = new SoftReference<>(new byte[1024 * 1024]);  // 1MB

// Access the referent:
byte[] data = softRef.get();
if (data == null) {
    // GC collected it — recreate
    data = new byte[1024 * 1024];
    softRef = new SoftReference<>(data);
}
```

## Memory Lifecycle of Soft References

```text
Heap memory:      plenty        →    tight        →    very low
Soft reference:   kept alive   →    kept alive   →    COLLECTED

GC runs:
  Normal GC → soft refs NOT collected
  GC under memory pressure → soft refs COLLECTED (in order: oldest first)
  Just before OOM → all soft refs collected
```

The JVM's policy: soft references are collected if the object hasn't been accessed recently and heap space is needed. The `-XX:SoftRefLRUPolicyMSPerMB` flag controls how aggressively they are collected.

## Use Case: Memory-Sensitive Cache

```java
public class ImageCache {

    private final Map<String, SoftReference<BufferedImage>> cache = new HashMap<>();

    public BufferedImage get(String path) {
        SoftReference<BufferedImage> ref = cache.get(path);
        if (ref != null) {
            BufferedImage image = ref.get();
            if (image != null) return image;  // cache hit
        }
        // Cache miss (or GC collected it) — reload
        BufferedImage image = loadFromDisk(path);
        cache.put(path, new SoftReference<>(image));
        return image;
    }
}
```

The cache:
- Keeps images in memory when memory is plentiful
- Automatically releases them when memory is tight
- Never causes OOM (unlike a HashMap that holds strong references)

## When to Use Soft References

```text
✓ Caches where items can be recreated from another source
✓ Image caches, parsed object caches, expensive computation results
✓ When you want "keep this as long as there's memory, otherwise discard"
✗ Not for caches where items MUST be accessible (use strong refs then)
✗ Not a substitute for proper cache eviction (use Caffeine/Guava for production)
```

---

# 4. Weak References (java.lang.ref.WeakReference)

A weak reference is collected at the **next GC cycle** as soon as there are no other strong or soft references to the object.

```java
WeakReference<User> weakRef = new WeakReference<>(new User("Alice"));

// Force GC (for demonstration only)
System.gc();

User user = weakRef.get();
System.out.println(user);  // null — GC collected it!
```

## Weak vs Soft

```text
Soft: collected only when memory is LOW
Weak: collected at the VERY NEXT GC cycle (regardless of memory)

Weak references are much more aggressively collected than soft references.
```

## Use Case 1: WeakHashMap

`WeakHashMap` uses weak references for its keys. When a key has no other strong references, the entry is automatically removed by GC:

```java
WeakHashMap<ClassLoader, ClassMetrics> stats = new WeakHashMap<>();

stats.put(classLoader, new ClassMetrics());
// When classLoader is no longer referenced elsewhere → entry auto-removed from map
// Memory freed without manual cleanup!
```

Real-world use: associating metadata with objects without preventing their GC. Used in framework internals, ClassLoader tracking, and caching where keys are the "owner" objects.

## Use Case 2: Preventing Memory Leaks in Listener Pattern

```java
// PROBLEM: listener prevents orderService from being GC'd
class EventBus {
    List<EventListener> listeners = new ArrayList<>();
    void subscribe(EventListener l) { listeners.add(l); }  // strong ref!
}

// SOLUTION: WeakReference so listener can be GC'd when no other refs exist
class EventBus {
    List<WeakReference<EventListener>> listeners = new ArrayList<>();

    void subscribe(EventListener l) {
        listeners.add(new WeakReference<>(l));
    }

    void publish(Event event) {
        Iterator<WeakReference<EventListener>> it = listeners.iterator();
        while (it.hasNext()) {
            EventListener listener = it.next().get();
            if (listener == null) {
                it.remove();  // clean up dead references
            } else {
                listener.onEvent(event);
            }
        }
    }
}
```

## Use Case 3: Canonicalization Cache

```java
// Ensure only one instance per unique value (like String.intern())
Map<String, WeakReference<MyObject>> cache = new WeakHashMap<>();

MyObject canonical(String key) {
    WeakReference<MyObject> ref = cache.get(key);
    MyObject obj = (ref != null) ? ref.get() : null;
    if (obj == null) {
        obj = new MyObject(key);
        cache.put(key, new WeakReference<>(obj));
    }
    return obj;
}
// When no one holds the MyObject strongly → it's auto-removed from cache
```

---

# 5. Phantom References (java.lang.ref.PhantomReference)

Phantom references are the most unusual. `get()` always returns `null` — you can never access the object through a phantom reference. They are used **exclusively** for post-mortem cleanup notification.

```java
PhantomReference<Resource> phantomRef = new PhantomReference<>(resource, referenceQueue);

// phantomRef.get() ALWAYS returns null
// Unlike Soft/Weak, you can never access the object
```

## How Phantom References Work

1. Object becomes unreachable (no strong, soft, or weak refs)
2. Object is finalized (if it has a `finalize()` method)
3. JVM enqueues the PhantomReference into its ReferenceQueue
4. Your code polls the queue — knows the object has been collected
5. You do cleanup (close native resources, etc.)

```text
Object lifecycle with PhantomReference:

Object created
    ↓
Object becomes unreachable
    ↓
Object finalized (if applicable)
    ↓
PhantomReference enqueued in ReferenceQueue  ← YOUR CODE DETECTS THIS
    ↓
Object memory reclaimed
    ↓
Your cleanup runs
```

## Example: Native Resource Cleanup

```java
ReferenceQueue<DatabaseConnection> queue = new ReferenceQueue<>();

PhantomReference<DatabaseConnection> ref = 
    new PhantomReference<>(connection, queue);

// Background cleaner thread
Thread cleaner = new Thread(() -> {
    while (true) {
        Reference<?> dead = queue.remove();  // blocks until something is collected
        // connection has been GC'd — clean up native resources
        closeNativeHandle(connectionHandle);
    }
});
```

## Modern Alternative: java.lang.ref.Cleaner (Java 9+)

Java 9 introduced `Cleaner` which provides the same capability more safely:

```java
Cleaner cleaner = Cleaner.create();

class Resource implements AutoCloseable {
    private final Cleaner.Cleanable cleanable;
    private final int nativeHandle;

    Resource(int handle) {
        this.nativeHandle = handle;
        // Register cleanup action — runs when Resource becomes unreachable
        this.cleanable = cleaner.register(this, () -> closeHandle(handle));
    }

    @Override
    public void close() {
        cleanable.clean();  // explicit close also runs cleanup
    }
}
```

## Why Not Just Use finalize()?

`finalize()` is deprecated (Java 9+) and removed (Java 18+) because:
- Unpredictable timing — finalize might run long after object becomes unreachable
- Performance impact — finalizable objects require extra GC processing
- Security risks — object can be resurrected in finalize()
- No ordering guarantees

PhantomReference + ReferenceQueue (or `Cleaner`) is the correct replacement.

---

# 6. Reference Queue (java.lang.ref.ReferenceQueue)

A `ReferenceQueue` is how you get notified when a reference's referent has been collected:

```java
ReferenceQueue<User> queue = new ReferenceQueue<>();

WeakReference<User> ref1 = new WeakReference<>(new User("Alice"), queue);
WeakReference<User> ref2 = new WeakReference<>(new User("Bob"), queue);

// When User objects are GC'd, their WeakReferences appear in the queue:
Reference<?> dead;
while ((dead = queue.poll()) != null) {
    // a User was GC'd — do cleanup
    System.out.println("A user was collected: " + dead);
}
```

Works with Soft, Weak, and Phantom references (not Strong — no need).

Used by:
- `WeakHashMap` internally (to clean up dead entries)
- `Cleaner` (Java 9+)
- Framework resource management

---

# 7. Complete Reference Type Comparison

| | Strong | Soft | Weak | Phantom |
|---|---|---|---|---|
| Collection time | Never (while ref exists) | Only when memory is low | Next GC cycle | After finalization |
| `get()` returns | Object | Object or null | Object or null | Always null |
| Use case | Everything (default) | Memory-sensitive caches | Associating metadata, preventing leaks | Post-mortem cleanup |
| GC notification | No | No | ReferenceQueue (optional) | ReferenceQueue (required) |
| Object accessible | Yes | Until GC | Until GC | NEVER via phantom |

---

# 8. Reference Strength Ordering

```text
Strong > Soft > Weak > Phantom

An object is:
  Strongly reachable  → not collected
  Softly reachable    → collected only when memory low
  Weakly reachable    → collected at next GC
  Phantom reachable   → already finalized, about to be reclaimed
  Unreachable         → not reachable at all (or only via phantom)
```

An object is:
- **Strongly reachable**: reachable via at least one chain of strong references
- **Softly reachable**: not strongly reachable, but reachable via at least one soft ref
- **Weakly reachable**: not strongly or softly reachable, but reachable via weak ref
- **Phantom reachable**: not strongly, softly, or weakly reachable, but has an associated phantom ref

---

# 9. Real-life Scenarios

## Scenario 1: In-memory Image Cache (Soft Reference)

```text
Problem: Cache resized images in memory (expensive to recompute)
         Don't want cache to cause OOM

Solution: SoftReference values in the cache
  → Images stay cached as long as heap has room
  → Automatically evicted when memory is tight
  → Recreated on cache miss
```

## Scenario 2: Plugin Framework (Weak Reference)

```text
Problem: Framework holds callbacks from plugins
         When plugin is unloaded (ClassLoader becomes unreachable),
         callbacks should be auto-removed from framework's list

Solution: WeakReference to each callback
  → When plugin's ClassLoader is GC'd → WeakRef.get() returns null
  → Framework cleans up the dead entries
  → No explicit "unregister" call needed from plugins
```

## Scenario 3: Off-heap Resource Cleanup (Phantom Reference)

```text
Problem: Java object holds a native resource (file handle, GPU buffer)
         If object is GC'd without close() being called, resource leaks

Solution: PhantomReference + background cleaner thread
  → When Java object is GC'd → PhantomRef enqueued
  → Cleaner thread detects → closes native resource
  → No resource leak even if close() was forgotten
```

---

# Interview Preparation — Reference Types

---

## Q1: What are the four types of Java references and when is each collected?

**Answer:**

| Type | When collected | Use case |
|---|---|---|
| Strong | Never (while reference exists) | All normal references |
| Soft | Only when JVM is low on memory (before OOM) | Memory-sensitive caches |
| Weak | At the next GC cycle (when no strong/soft refs exist) | Metadata association, listener management |
| Phantom | After finalization (get() always returns null) | Post-mortem cleanup of native resources |

```java
SoftReference<T> soft = new SoftReference<>(obj);  // cache
WeakReference<T> weak = new WeakReference<>(obj);  // metadata/listeners
PhantomReference<T> phantom = new PhantomReference<>(obj, queue);  // cleanup
```

---

## Q2: What is WeakHashMap and when would you use it?

**Answer:**

`WeakHashMap` uses weak references for its keys. When a key has no other strong references outside the map → the GC collects it → the map entry is automatically removed.

Use when: you want to associate data with an object without preventing that object from being GC'd.

```java
// Associate ClassLoader statistics without keeping ClassLoaders alive
WeakHashMap<ClassLoader, Stats> stats = new WeakHashMap<>();

stats.put(pluginLoader, new Stats());
// When pluginLoader is no longer referenced elsewhere:
// → pluginLoader is GC'd
// → stats entry auto-removed
// → Stats object becomes unreachable → also GC'd
```

Do NOT use WeakHashMap when: the keys are literals or enums (always strongly reachable → entries never removed). Also be aware it's not thread-safe — use `Collections.synchronizedMap(new WeakHashMap<>())` if needed.

---

## Q3: What is the difference between SoftReference and WeakReference?

**Answer:**

Both allow the GC to collect the referent. The difference is WHEN:

- **SoftReference**: collected only when the JVM is running low on memory. Acts as a cache — the JVM keeps soft-referenced objects alive as long as there's heap space.
- **WeakReference**: collected at the **very next GC cycle** when no strong or soft references exist. Much more aggressively collected.

Rule of thumb:
- `SoftReference` for caches (want data to persist as long as memory allows)
- `WeakReference` for metadata/listeners (want data to disappear as soon as the associated object is no longer needed)

---

## Q4: Why are PhantomReferences used instead of finalize()?

**Answer:**

`finalize()` was Java's original cleanup mechanism but has fatal flaws:
1. **Unpredictable timing**: finalize() runs whenever the GC decides, possibly never
2. **Performance cost**: finalizable objects require extra GC processing
3. **Object resurrection**: finalize() can create a new strong reference to the object, preventing collection
4. **No ordering**: finalize() order is undefined between objects

PhantomReference + ReferenceQueue:
1. **Deterministic timing**: reference enqueued immediately after finalization
2. **No resurrection possible**: PhantomReference.get() always returns null
3. **Cleaner code**: background thread handles cleanup
4. **No special GC overhead**

Java 9+ `Cleaner` is the preferred modern API, wrapping PhantomReference + ReferenceQueue for you.

---

## Q5: How does WeakHashMap help prevent memory leaks?

**Answer:**

In normal listener/callback patterns:

```java
bus.subscribe(listener);  // bus holds strong reference to listener
// Even if listener is no longer needed, bus keeps it alive → memory leak
```

With WeakHashMap or WeakReference listeners:

```java
Map<Listener, Metadata> listeners = new WeakHashMap<>();
// When listener has no other strong references → auto-removed
// No explicit unsubscribe needed
```

This is particularly useful in plugin/module systems where components are loaded and unloaded dynamically. When a plugin's objects are no longer strongly reachable, all their listener registrations automatically become unreachable too.

---

## Q6: What is a ReferenceQueue and how does it work?

**Answer:**

A `ReferenceQueue` is a queue that the JVM automatically enqueues `Reference` objects into after their referent has been collected.

```java
ReferenceQueue<T> queue = new ReferenceQueue<>();
WeakReference<T> ref = new WeakReference<>(obj, queue);

// When obj is GC'd:
// → JVM automatically enqueues `ref` into `queue`

// Your code polls:
Reference<? extends T> dead = queue.poll();  // non-blocking
// OR:
Reference<? extends T> dead = queue.remove(); // blocking — waits until something is enqueued
```

Use cases:
- Detecting when cached objects were collected (to remove stale entries from a lookup table)
- Triggering cleanup of associated resources (files, native handles)
- Monitoring GC activity (which objects were collected)

`WeakHashMap` uses a ReferenceQueue internally to efficiently clean up dead entries on every map operation.
