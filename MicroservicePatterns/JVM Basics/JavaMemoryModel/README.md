# Java Memory Model (JMM) — Deep Dive

The Java Memory Model is the specification that defines **how threads interact through memory** in Java. It answers the question: "If Thread A writes a value, when and under what conditions can Thread B see it?"

> **The JMM is the foundation of all Java concurrency. Without understanding it, you cannot reason correctly about `volatile`, `synchronized`, `Atomic` classes, locks, or any concurrent code. It is one of the most frequently asked senior Java interview topics.**

---

# 1. The Problem JMM Solves

On modern hardware, memory is NOT simply one flat store that all threads share:

```text
Reality of modern hardware:

CPU 1                    CPU 2
  |                        |
L1 Cache               L1 Cache
  |                        |
L2 Cache               L2 Cache
  |                        |
        L3 Cache (shared)
              |
           Main RAM
```

Each CPU has its own cache. When Thread A on CPU 1 writes to a variable:

```text
Thread A (CPU 1):
  x = 42

Stored in: CPU 1's L1 cache
Flushed to: L3 cache / RAM  → eventually (when? unknown!)

Thread B (CPU 2):
  reads x → gets value from CPU 2's L1 cache
  → might see x = 0 (old value!) because CPU 1's write is still in L1
```

Without rules, threads can see stale values indefinitely.

Additionally, compilers and CPUs **reorder instructions** for performance:

```java
// Written as:
x = 1;
y = 2;
flag = true;

// CPU or compiler may reorder to:
flag = true;  // moved ahead!
x = 1;
y = 2;
```

Another thread checking `flag` first might then read x=0 even though the code "set x before flag."

**JMM defines rules that guarantee visibility and ordering when you use synchronization correctly.**

---

# 2. The Core Concept: Happens-Before

The entire JMM is built on one fundamental concept:

> **If action A "happens-before" action B, then the effects of A (writes to variables) are guaranteed to be visible to B.**

Happens-before is not about time (A finishing before B starts). It's about **memory visibility guarantees**.

```text
Happens-Before (HB):

A HB B → A's writes are visible to B

A does NOT HB B → B may or may not see A's writes (data race!)
```

---

# 3. The Happens-Before Rules

The JMM defines specific actions that create happens-before relationships:

## Rule 1: Program Order Rule

Within a single thread, actions happen-before subsequent actions in program order:

```java
// In the SAME thread:
x = 10;          // A
int y = x + 1;   // B

// A HB B — guaranteed within the thread
// B will always see x = 10
```

This is obvious, but it's the foundation — within one thread, there's always ordering.

## Rule 2: Monitor Lock Rule (synchronized)

An unlock of a monitor happens-before every subsequent lock of the same monitor:

```java
synchronized (lock) {
    x = 42;           // write inside lock
}                      // ← unlock HB next lock

// ...

synchronized (lock) {
    int y = x;        // ← lock HB this read
    // Guaranteed: y = 42
}
```

```text
Thread A:  write x=42 → unlock
                          ↓  (HB relationship)
Thread B:              lock → read x  (sees 42)
```

## Rule 3: Volatile Variable Rule

A write to a volatile variable happens-before every subsequent read of that same variable:

```java
volatile boolean flag = false;
volatile int x = 0;

Thread A:
    x = 42;        // ordinary write
    flag = true;   // volatile write ← HB any subsequent volatile read of flag

Thread B:
    if (flag) {    // volatile read ← sees flag = true
        // Guaranteed: sees x = 42 too! (because A's entire memory state
        // up to the volatile write is visible to B after the volatile read)
    }
```

## Rule 4: Thread Start Rule

`thread.start()` happens-before any action in the started thread:

```java
x = 42;
thread.start();  // ← HB all actions in the new thread

// Inside new thread:
System.out.println(x);  // guaranteed to see x = 42
```

## Rule 5: Thread Join Rule

All actions in a thread happen-before `thread.join()` returns:

```java
thread.join();  // ← HB to here

// After join(), you see everything thread did:
System.out.println(x);  // sees thread's writes
```

## Rule 6: Transitivity

If A HB B and B HB C, then A HB C.

```text
A HB B HB C → A HB C

Thread A writes x, then unlocks L
Thread B acquires L, writes y, then unlocks M
Thread C acquires M, reads x and y

Thread C sees BOTH A's writes (via transitivity) and B's writes
```

---

# 4. What is a Data Race?

A **data race** occurs when:

```text
1. Two threads access the same variable
2. At least one access is a WRITE
3. There is NO happens-before relationship between them
```

Data races produce **undefined behavior** — the program may:
- See stale values
- See partially written values
- Behave differently on different hardware
- Behave differently with different JVM implementations
- Work fine in development, fail in production

Example of a data race:

```java
int x = 0;  // shared, not volatile, not synchronized

Thread A: x = 42;   // write — no synchronization
Thread B: int y = x; // read — no synchronization

// No HB relationship between A and B
// → DATA RACE → y might be 0 or 42 — undefined!
```

---

# 5. Visibility — The Core Problem

```java
class DataHolder {
    boolean ready = false;  // NOT volatile
    int value = 0;          // NOT volatile
}

DataHolder holder = new DataHolder();

Thread producer = new Thread(() -> {
    holder.value = 42;
    holder.ready = true;  // signals consumer
});

Thread consumer = new Thread(() -> {
    while (!holder.ready) { }  // spin until ready
    System.out.println(holder.value);  // might print 0!
});
```

Why might this print 0?

```text
1. producer.ready = true might be cached in CPU 1's cache
   → consumer on CPU 2 never sees it → infinite loop

2. OR: due to reordering:
   CPU might execute:
     holder.ready = true;   // set ready FIRST
     holder.value = 42;     // set value SECOND
   
   consumer sees ready=true, then reads value → sees 0!
```

Fix with volatile:

```java
volatile boolean ready = false;
volatile int value = 0;

// Write to volatile establishes HB with read of same volatile
// Volatile write flushes all preceding writes to main memory
// Volatile read reads from main memory (bypasses cache)
```

---

# 6. The volatile Keyword in Depth

`volatile` provides two guarantees:

### Guarantee 1: Visibility

```text
volatile write:
  → All previously written variables are FLUSHED to main memory
  → The volatile variable itself is written to main memory

volatile read:
  → The volatile variable is read from main memory (not cache)
  → All subsequent reads see the most recent writes
```

### Guarantee 2: Ordering (No Reordering)

```text
volatile prevents reordering around the volatile access:

  x = 1;        (ordinary write)
  flag = true;  (volatile write) ← nothing after this can move before this line

  if (flag) {   (volatile read) ← nothing before this can move after this line
      y = x;    (ordinary read)
  }
```

### What volatile does NOT provide: Atomicity

```java
volatile int counter = 0;

// Thread 1 and Thread 2 both do:
counter++;  // NOT ATOMIC even though volatile!

// counter++ is actually:
// 1. READ counter
// 2. ADD 1
// 3. WRITE counter
// → Three operations, not one → race condition possible
```

`volatile` is safe for:
```text
✓ Flag variables (write once, read many)
✓ Single writer, multiple readers
✓ Publishing reference to immutable object
```

`volatile` is NOT safe for:
```text
✗ Compound operations (read-modify-write: counter++, i += 5)
✗ When you need atomicity of a group of writes
→ Use AtomicInteger, synchronized, or Lock instead
```

---

# 7. synchronized in Depth

`synchronized` provides stronger guarantees than `volatile`:

```text
synchronized block ENTRY:
  → Acquires the monitor lock
  → Invalidates thread's local cache (must read from main memory)
  → All reads after entry see latest writes

synchronized block EXIT:
  → Flushes all writes to main memory
  → Releases the monitor lock
  → Creates HB with next thread's synchronized entry on same lock
```

Visualization:

```text
Thread A:                      Thread B:
synchronized(lock) {           synchronized(lock) {  ← blocked until A exits
    x = 42;                        y = x;            // sees x = 42
    y = 100;                       z = y;            // sees y = 100
}  ← flushes to memory,       }
   releases lock → HB
```

Mutual exclusion: only one thread inside `synchronized(lock)` at a time.
Memory visibility: writes by A before unlock are visible to B after lock.

---

# 8. Reordering — Why It Matters

The JVM (JIT compiler) and CPU are both allowed to reorder instructions **within a thread** as long as the single-thread semantics are preserved.

Within a single thread:

```java
int a = x;
int b = y;
int c = a + b;
// Can be reordered to:
int b = y;
int a = x;
int c = a + b;
// Same result for this thread
```

But across threads, reordering causes problems:

```java
// Thread A (initializing):
config.timeout = 30;      // step 1
config.host = "prod";     // step 2
initialized = true;       // step 3

// CPU might reorder to:
initialized = true;       // step 3 moved first!
config.timeout = 30;      // step 1
config.host = "prod";     // step 2

// Thread B:
if (initialized) {
    use(config.host);     // sees initialized=true but host might still be null!
}
```

Fix: make `initialized` volatile. The volatile write in Thread A creates a barrier that prevents preceding writes from being reordered after it, and Thread B's volatile read establishes visibility of everything written before the volatile write.

---

# 9. Memory Barriers

Memory barriers (also called memory fences) are CPU instructions that enforce ordering.

```text
StoreStore barrier: All stores before the barrier complete before stores after
StoreLoad barrier:  All stores before complete before loads after (most expensive)
LoadLoad barrier:   All loads before complete before loads after
LoadStore barrier:  All loads before complete before stores after
```

`volatile` writes insert a StoreLoad barrier (strongest).
`synchronized` uses full barriers on entry and exit.

You don't normally use barriers directly — `volatile` and `synchronized` abstract them. But understanding them explains why these constructs have the guarantees they do.

---

# 10. Final Fields and Safe Publication

The JMM has a special rule for `final` fields:

> **A write to a final field, completed in the constructor, happens-before any read of that final field via a properly published reference to the object.**

```java
class ImmutablePoint {
    final int x;
    final int y;

    ImmutablePoint(int x, int y) {
        this.x = x;  // writes to final fields
        this.y = y;
    }
}

// As long as the ImmutablePoint reference is safely published:
ImmutablePoint p = new ImmutablePoint(10, 20);
// Any thread that receives p will see x=10, y=20
// Even without synchronization!
```

This is why immutable objects (all fields final) are inherently thread-safe once safely published.

**Safe publication**: making a reference visible to another thread in a way that includes the HB relationship:
```java
// Safe publication methods:
volatile ImmutablePoint p;  // via volatile
static final ImmutablePoint p = new ...;  // via static initializer (class init HB)
synchronized publish(p) { ... }  // via synchronized
```

**Unsafe publication (data race):**
```java
ImmutablePoint p;  // no volatile, no sync
// Thread B may see a partially initialized object if P is published unsafely
```

---

# 11. Common JMM Pitfalls

## Pitfall 1: Double-Checked Locking (Classic Bug)

```java
// BROKEN before Java 5 / without volatile:
class Singleton {
    private static Singleton instance;  // NOT volatile — BUG!

    public static Singleton getInstance() {
        if (instance == null) {          // check 1
            synchronized (Singleton.class) {
                if (instance == null) {  // check 2
                    instance = new Singleton();  // ← can be seen partially initialized!
                }
            }
        }
        return instance;
    }
}
```

Problem: `instance = new Singleton()` compiles to roughly:

```text
1. Allocate memory
2. Write reference to instance  ← might be reordered BEFORE step 3!
3. Initialize object (run constructor)
```

Thread B might see `instance != null` but the object isn't fully initialized yet.

Fix: make `instance` volatile:

```java
private static volatile Singleton instance;  // volatile fixes the reordering
```

## Pitfall 2: Stale Loop

```java
boolean running = true;  // NOT volatile

Thread t = new Thread(() -> {
    while (running) { }  // might loop forever!
    // JIT might hoist `running` out of loop: "running is always true in this thread"
});

// Main thread:
running = false;  // might never be seen by thread t!
```

Fix: `volatile boolean running`.

## Pitfall 3: Assuming synchronized on Different Objects Provides Ordering

```java
// WRONG: these do NOT create HB between them!
synchronized (lockA) { x = 42; }  // Thread A
synchronized (lockB) { y = x; }   // Thread B (different lock!)
// No HB → Thread B may not see x=42
```

Happens-before through `synchronized` only works on the **same** monitor.

---

# 12. JMM and Java's Concurrency Utilities

All of Java's concurrency utilities are built on the JMM:

```text
ReentrantLock:     uses AbstractQueuedSynchronizer (volatile state field)
AtomicInteger:     volatile int + CAS operations
CountDownLatch:    volatile state via AQS
Semaphore:         volatile state via AQS
ConcurrentHashMap: volatile reads, CAS + synchronized writes
ThreadPoolExecutor:volatile state for lifecycle management
```

Understanding JMM lets you understand WHY these work correctly.

---

# 13. Real-life: Spring Boot Microservice

```java
@Service
public class FeatureFlagService {

    // WRONG: not volatile — may read stale value!
    private boolean darkModeEnabled = false;

    // CORRECT: volatile — changes immediately visible to all threads
    private volatile boolean darkModeEnabled = false;

    // Called by admin thread when config changes
    public void setDarkMode(boolean enabled) {
        darkModeEnabled = enabled;  // volatile write → HB
    }

    // Called by HTTP request threads (many, concurrent)
    public boolean isDarkModeEnabled() {
        return darkModeEnabled;  // volatile read → sees latest write
    }
}
```

Without `volatile`, HTTP request threads might see `darkModeEnabled = false` even hours after an admin enabled it — because the JVM cached the value in each thread's CPU cache.

---

# 14. Summary — The Rules to Remember

```text
HAPPENS-BEFORE creates:
  Program order (within thread)
  synchronized unlock → next lock (same monitor)
  volatile write → subsequent volatile read (same variable)
  thread.start() → first action of new thread
  last action of thread → thread.join() returning

volatile provides:
  ✓ Visibility (all threads see latest write)
  ✓ Ordering (no reordering across volatile access)
  ✗ Atomicity (counter++ is still a race!)

synchronized provides:
  ✓ Visibility (flush/invalidate caches on enter/exit)
  ✓ Ordering (no reordering into/out of synchronized block)
  ✓ Atomicity (mutual exclusion — one thread at a time)
  ✗ Does NOT help if threads use DIFFERENT locks

final fields provide:
  ✓ Safe read visibility for immutable objects (once properly published)
```

---

# Interview Preparation — Java Memory Model

---

## Q1: What is the Java Memory Model and why does it exist?

**Answer:**

The Java Memory Model (JMM) is a specification that defines how threads interact through memory — specifically, what values a thread is guaranteed to see when reading a variable written by another thread.

It exists because:
1. Modern CPUs have multi-level caches — writes aren't immediately visible to other CPUs
2. Compilers and CPUs reorder instructions for performance
3. Without rules, concurrent programs are unpredictable across platforms

The JMM defines the happens-before relationship: if action A happens-before action B, then B is guaranteed to see A's writes. The JMM specifies exactly what actions create happens-before relationships (synchronized, volatile, thread start/join, etc.).

---

## Q2: What is the happens-before relationship?

**Answer:**

Happens-before is the JMM's formal definition of memory visibility guarantees. If A happens-before B:
- All memory writes by A are visible to B
- All writes by actions that happen-before A are also visible to B (transitivity)

Key happens-before rules:
- **Program order**: within one thread, each statement HB the next
- **Monitor unlock HB lock**: synchronized unlock HB next lock on same monitor
- **Volatile write HB read**: volatile write HB any subsequent read of same variable
- **Thread start**: `start()` HB first action of started thread
- **Thread join**: last thread action HB `join()` returning

Without a HB relationship between two accesses to the same variable (where at least one is a write) → **data race** → undefined behavior.

---

## Q3: What does volatile guarantee? What doesn't it guarantee?

**Answer:**

`volatile` guarantees:
1. **Visibility**: writes to a volatile variable are immediately visible to all threads (flushed to main memory; subsequent reads bypass cache)
2. **Ordering**: no reordering of instructions across a volatile access — all writes before a volatile write are committed before it; all reads after a volatile read happen after it

`volatile` does NOT guarantee:
- **Atomicity**: `counter++` on a volatile int is still a race condition — it's three operations (read, increment, write), not one atomic operation

Safe uses: flag variables, single writer/multiple readers, publishing references to immutable objects.
Unsafe uses: `counter++`, `i += n`, any check-then-act.

---

## Q4: Explain the double-checked locking pattern and why volatile is required.

**Answer:**

Double-checked locking (DCL) is a pattern for lazy singleton initialization:

```java
class Singleton {
    private static volatile Singleton instance;  // volatile is REQUIRED

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

Without `volatile`, `instance = new Singleton()` can be seen as:
1. Allocate memory
2. Write reference to `instance` (non-null now)
3. Run constructor to initialize fields

A thread B might see `instance != null` after step 2 but before step 3 — and then use an uninitialized object. This is a real reordering that the JVM is allowed to do without `volatile`.

With `volatile`, the write to `instance` is a happens-before barrier — no writes can be reordered after it, and Thread B's volatile read sees a fully initialized object.

---

## Q5: What is a data race?

**Answer:**

A data race occurs when:
1. Two or more threads access the same variable concurrently
2. At least one access is a write
3. There is NO happens-before relationship between the accesses

Data races produce undefined behavior — results are unpredictable and platform-dependent. The JMM does not guarantee what value a thread will see in a data race.

Common examples:
```java
// Data race: two threads write/read x without synchronization
int x = 0;  // no volatile, no synchronized
Thread A: x = 42;
Thread B: int y = x;  // data race — y could be 0 or 42
```

Fix: use volatile, synchronized, or atomic classes to establish HB.

---

## Q6: How does synchronized provide memory visibility?

**Answer:**

`synchronized` provides both mutual exclusion AND memory visibility:

1. **On lock entry**: the thread invalidates its local cache — all subsequent reads go to main memory
2. **On lock exit**: the thread flushes all its writes to main memory, and releases the lock

The JMM rule: **unlock of a monitor HB every subsequent lock of the same monitor**.

This means: everything Thread A wrote while holding the lock is visible to Thread B after Thread B acquires the same lock.

Critical caveat: both threads must synchronize on the **same** object/lock. Synchronizing on different objects provides no happens-before relationship between them.

---

## Q7: Why is volatile not sufficient for counter++ ?

**Answer:**

`counter++` is actually three operations:
1. Read current value of `counter`
2. Add 1
3. Write new value back to `counter`

Even with `volatile`, two threads can interleave these steps:

```text
Thread A: reads counter = 0
Thread B: reads counter = 0 (also 0! A hasn't written yet)
Thread A: writes counter = 1
Thread B: writes counter = 1  (overwrites A's write — lost update!)
Result: counter = 1 instead of 2
```

`volatile` only ensures each individual read or write is atomic and visible — it does NOT make the compound operation (read-modify-write) atomic.

Fix: use `AtomicInteger.incrementAndGet()` (CAS-based atomic operation) or `synchronized` block.

---

## Q8: What is safe publication and why does it matter?

**Answer:**

Safe publication means making an object reference visible to other threads in a way that guarantees they see the object fully initialized.

Safe publication mechanisms (establish HB):
- Writing to a `volatile` field
- Writing to a `static final` field (class initialization HB)
- Writing inside a `synchronized` block before exiting
- Using concurrent collection (ConcurrentHashMap) to store the reference

Unsafe publication (data race on the reference itself):
```java
MyObject obj;  // no volatile, no sync
// Thread A initializes and publishes
obj = new MyObject(42);  // another thread might see obj != null but uninitialized!
```

The `final` field rule: writes to final fields in a constructor happen-before any read of those fields by any thread, as long as the reference is safely published. This is why immutable objects (all-final fields) are thread-safe once safely published.
