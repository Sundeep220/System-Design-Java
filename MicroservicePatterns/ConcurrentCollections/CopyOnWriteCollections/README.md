# CopyOnWrite Collections — Deep Dive

Copy-On-Write (COW) collections provide thread-safety through a radical strategy: **every write operation creates a brand-new copy of the underlying data structure**. Reads always access the latest snapshot — no locks needed.

> **CopyOnWrite collections are optimized for the case where reads vastly outnumber writes. Reads are completely lock-free and non-blocking. Writes are expensive but infrequent.**

---

# 1. The Core Idea

```text
Regular ArrayList (not thread-safe):
  Internal: int[] array
  Multiple threads → race condition on reads and writes

CopyOnWriteArrayList:
  Internal: volatile Object[] array   ← a reference to the current array

  READ:
    Read the volatile reference → access array → done
    NO LOCK. Ever.

  WRITE:
    Acquire lock (only one writer at a time)
    Copy the entire current array
    Make modification on the COPY
    Atomically update the volatile reference to point to new copy
    Release lock
```

Visualization:

```text
BEFORE write:
  Reference → [A][B][C][D]

Write: add("E")
  1. Copy:  new array [A][B][C][D][E]
  2. Atomically swap reference:
  Reference → [A][B][C][D][E]
  Old array [A][B][C][D] → garbage collected

DURING write:
  Readers: still reading [A][B][C][D] (snapshot before write)
  Writer: working on [A][B][C][D][E] (new copy)
  No conflict!
```

---

# 2. CopyOnWriteArrayList

## What it is

A thread-safe `List` implementation using the copy-on-write strategy.

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("service-A");
list.add("service-B");
list.add("service-C");
```

## Key Characteristics

```text
Read operations (get, size, contains, iterator):
  Lock-free
  Wait-free (reads complete in bounded time regardless of other threads)
  Always see a consistent snapshot

Write operations (add, remove, set, clear):
  Single global lock (ReentrantLock)
  Copies entire array → O(n) time and memory
  Only ONE writer at a time

Iterator behavior:
  Iterator reflects the state of the array AT THE TIME the iterator was created
  Even if other threads modify the list, the iterator sees the original snapshot
  NO ConcurrentModificationException — ever
```

## The Iterator Snapshot Guarantee

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));

// Thread 1 — starts iterating
Iterator<String> iter = list.iterator();  // snapshot taken: [A, B, C]

// Thread 2 — modifies list
list.add("D");  // creates new array: [A, B, C, D]

// Thread 1 — continues iterating
while (iter.hasNext()) {
    System.out.println(iter.next());  // prints A, B, C — does NOT see D
}
// No ConcurrentModificationException!
```

This is essential for safe iteration while the list may be modified by other threads.

## Read Operations Performance

```text
get(i):       O(1) — direct array access, no lock
size():       O(1) — volatile field read, no lock
contains(o):  O(n) — scans array, no lock
iterator():   O(1) — returns iterator over current snapshot
```

## Write Operations Performance

```text
add(e):       O(n) — copies entire array + add element
remove(o):    O(n) — scans + copies entire array
set(i, e):    O(n) — copies entire array + set element
addAll(c):    O(n + m) — copies entire array + add all elements
```

The O(n) cost of writes is the trade-off for lock-free reads.

---

## CopyOnWriteArrayList vs ArrayList vs Collections.synchronizedList

| | ArrayList | synchronizedList | CopyOnWriteArrayList |
|---|---|---|---|
| Thread-safe | No | Yes | Yes |
| Read lock | None (unsafe) | Yes (blocks others) | None (safe!) |
| Write lock | None (unsafe) | Yes | Yes (single writer) |
| Iterator safety | Fail-fast (CME) | Manual lock needed | Snapshot (no CME) |
| Read performance | Fast | Blocked if writer | Always fast |
| Write performance | Fast | Fast | Slow (O(n) copy) |
| Memory | Low | Low | High (extra copy per write) |
| Best for | Single-threaded | Equal read/write | Read-heavy |

---

## Real-life Use Cases

### Use Case 1: Service Registry / Discovery

```java
@Service
public class ServiceRegistry {

    // Services are registered at startup and rarely change
    // But health-check threads READ this list constantly (every second)
    private final CopyOnWriteArrayList<ServiceInstance> instances = new CopyOnWriteArrayList<>();

    public void register(ServiceInstance instance) {
        instances.add(instance);  // rare write — new service comes online
    }

    public void deregister(ServiceInstance instance) {
        instances.remove(instance);  // rare write — service goes offline
    }

    public List<ServiceInstance> getInstances() {
        return Collections.unmodifiableList(instances);  // frequent reads — lock-free
    }
}
```

```text
Health checks: 50 threads reading instance list every second = 50 reads/sec
Service registration: once per deployment = 1 write/day

CopyOnWriteArrayList is perfect:
  - 50 concurrent reads = no locks, no contention
  - 1 write per day = copy overhead is irrelevant
```

### Use Case 2: Event Listener Management

```java
@Component
public class EventPublisher {

    private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(EventListener listener) {
        listeners.add(listener);  // rare
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);  // rare
    }

    public void publishEvent(Event event) {
        // Safe iteration — listeners may be added/removed concurrently
        // This iterator uses the snapshot — no CME even if listener removed during iteration
        for (EventListener listener : listeners) {
            listener.onEvent(event);  // frequent, concurrent
        }
    }
}
```

This pattern is extremely common in Spring's application events, Swing event listeners, and many frameworks.

### Use Case 3: Hot Configuration Reloading

```java
// Config rules are read millions of times per day
// But only updated when operators change config (rare)
private final CopyOnWriteArrayList<FirewallRule> rules = new CopyOnWriteArrayList<>();

// Config reload (rare — once per config change)
public void reloadRules(List<FirewallRule> newRules) {
    rules.clear();
    rules.addAll(newRules);  // two writes — expensive but rare
}

// Per-request check (very frequent)
public boolean isAllowed(Request request) {
    for (FirewallRule rule : rules) {  // lock-free read
        if (rule.matches(request)) return false;
    }
    return true;
}
```

### Use Case 4: Read-Heavy Feature Flag List

```java
private final CopyOnWriteArrayList<String> enabledFeatures = new CopyOnWriteArrayList<>();

// Flag management (rare — operator changes)
public void enableFeature(String feature) { enabledFeatures.add(feature); }
public void disableFeature(String feature) { enabledFeatures.remove(feature); }

// Feature check per request (very frequent)
public boolean isEnabled(String feature) {
    return enabledFeatures.contains(feature);  // lock-free, safe
}
```

---

## When NOT to Use CopyOnWriteArrayList

```text
1. Write-heavy workloads
   → Every write copies the entire array → O(n) time + O(n) memory
   → With frequent writes: huge GC pressure, poor performance
   → Use ConcurrentLinkedQueue or synchronizedList instead

2. Very large lists
   → Copying a 1M-element array on every write → 1M array created every write
   → Memory pressure, GC pauses

3. When iterator needs to see real-time updates
   → COW iterator sees a SNAPSHOT — it will NOT see concurrent modifications
   → If freshness is required, not suitable
```

Rule of thumb:
```text
writes < 1% of operations → CopyOnWriteArrayList is excellent
writes > 10% of operations → DO NOT use CopyOnWriteArrayList
```

---

# 3. CopyOnWriteArraySet

## What it is

A thread-safe `Set` implementation backed by `CopyOnWriteArrayList`.

```java
CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();
set.add("service-A");
set.add("service-B");
set.add("service-A");  // duplicate — ignored (it's a Set)
// set contains: [service-A, service-B]
```

## Characteristics

```text
Backed by: CopyOnWriteArrayList (internally)
Uniqueness: guaranteed (no duplicates)
Ordering: insertion order
Contains check: O(n) — linear scan (NOT O(1) like HashSet!)
Write: O(n) — copies entire list, checks for duplicates before adding
```

## CopyOnWriteArraySet vs HashSet vs ConcurrentSkipListSet

| | HashSet | CopyOnWriteArraySet | ConcurrentSkipListSet |
|---|---|---|---|
| Thread-safe | No | Yes | Yes |
| Read lock | None (unsafe) | None (safe) | None (safe) |
| contains() | O(1) | O(n) | O(log n) |
| add/remove | O(1) amortized | O(n) | O(log n) |
| Ordered | No | Insertion order | Sorted |
| Best for | Single-threaded | Small read-heavy sets | Sorted concurrent sets |

## Use Cases

```text
Small sets (< 100 elements) with rare writes:
  - Active feature flags set
  - Connected admin users set
  - Enabled plugin names
  - Server instances in a cluster (when cluster is small)
```

For larger sets with more writes, prefer `ConcurrentSkipListSet` or `Collections.newSetFromMap(new ConcurrentHashMap<>())`.

---

# 4. Summary — When to Choose CopyOnWrite Collections

```text
Choose CopyOnWriteArrayList when:
  ✓ Reads >> Writes (reads 99%+ of operations)
  ✓ Need safe iteration without external synchronization
  ✓ Collection is relatively small (< thousands of elements)
  ✓ Write latency is acceptable (not real-time write-critical)
  ✓ Iterator snapshot semantics are acceptable

Avoid CopyOnWriteArrayList when:
  ✗ Frequent writes (> 10% of operations)
  ✗ Very large collections (copying is expensive)
  ✗ Iterator must see real-time changes
  ✗ Memory is constrained
```

---

# Interview Preparation — CopyOnWrite Collections

---

## Q1: How does CopyOnWriteArrayList achieve thread-safety?

**Answer:**

Every write operation (add, remove, set) acquires a lock, creates a new copy of the underlying array with the modification applied, then atomically replaces the volatile reference to point to the new array.

Reads access the volatile reference directly — no lock ever. They always see a consistent, complete snapshot.

Key insight: reads and writes never operate on the same array simultaneously. Readers use the old array while the writer works on a new copy.

---

## Q2: What happens when you iterate over a CopyOnWriteArrayList while another thread modifies it?

**Answer:**

The iterator works on a **snapshot** of the array taken at the time the iterator was created. Even if another thread adds or removes elements, the iterator continues iterating over the original snapshot.

Result:
- No `ConcurrentModificationException` (ever)
- The iterator may NOT see modifications made after it was created
- This is intentional — it's called "snapshot semantics" or "weakly consistent iteration"

This makes CopyOnWriteArrayList excellent for event listener patterns where you iterate over listeners while new listeners might be added.

---

## Q3: When would you use CopyOnWriteArrayList over Collections.synchronizedList?

**Answer:**

Use `CopyOnWriteArrayList` when **reads heavily dominate writes** AND you need safe iteration without external locking.

`synchronizedList` requires you to manually synchronize on the list during iteration:
```java
synchronized (syncList) {
    for (String item : syncList) { ... }  // manual lock needed
}
```

If you forget, you get `ConcurrentModificationException`. And it blocks all other threads during the iteration.

`CopyOnWriteArrayList` makes iteration automatically safe and non-blocking. No external synchronization needed for reads or iteration.

---

## Q4: What is the major trade-off of CopyOnWriteArrayList?

**Answer:**

Every write is O(n) — it copies the entire array. With frequent writes:
- High memory allocation rate → GC pressure
- Each write creates a temporary copy → memory overhead
- CPU cycles spent copying data

Real impact: if you have a list of 100,000 elements and 1,000 writes/second, you're allocating 100,000 × 1,000 = 100M elements/second worth of array copies → massive GC pressure.

Rule: only use CopyOnWriteArrayList when writes are genuinely rare (< 1% of operations).

---

## Q5: How is CopyOnWriteArraySet different from HashSet in terms of performance?

**Answer:**

`CopyOnWriteArraySet` is backed by a list, not a hash table:
- `contains()`: O(n) linear scan (NOT O(1) like HashSet)
- `add()`: checks all existing elements for duplicates before adding → O(n)

This is fine for small sets (< 100 elements) but terrible for large sets.

`HashSet` is O(1) for contains/add but is not thread-safe. For thread-safe O(1) operations:
```java
Set<String> concurrentSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
```
This gives you a thread-safe set with O(1) contains and add, backed by ConcurrentHashMap.

---

## Q6: Give a real-world scenario where CopyOnWriteArrayList is the right choice.

**Answer:**

**Spring Security's filter chain**. The list of security filters is configured once at application startup and never changes during normal operation. Every HTTP request iterates through all filters to apply security rules.

```text
Writes: once at startup (application initialization)
Reads: every HTTP request (thousands per second)
Read:Write ratio: millions to one

CopyOnWriteArrayList is perfect:
- Zero lock overhead on every request
- No ConcurrentModificationException risk
- Filters list is small (< 50 elements) → copy cost at startup is negligible
```

Similarly: event publisher listener lists, service discovery client registries, feature flag lists in request handlers.
