# 2. Heap — Deep Dive

Now let's go deep into the **JVM Heap**, because this is the foundation for understanding:

* Young Generation
* Old Generation
* Garbage Collection
* GC pauses
* Memory leaks
* `OutOfMemoryError`
* Heap dumps
* JVM memory tuning

The most important mental model is:

> **The heap is the JVM-managed memory area where objects are generally allocated and where the garbage collector manages object lifetime.**

---

# 1. What exactly is the Heap?

Consider:

```java
public class Main {

    public static void main(String[] args) {

        User user = new User("Sundeep");

        System.out.println(user.getName());
    }
}
```

When:

```java
new User("Sundeep")
```

executes, an object needs memory.

Conceptually:

```text
                    JVM
                     |
                  Heap
                     |
              +-------------+
              | User object |
              |-------------|
              | name        |
              | "Sundeep"   |
              +-------------+
```

And the local variable:

```java
user
```

is a reference to that object.

Conceptually:

```text
Thread Stack                    Heap

+---------------+              +-------------+
| user ---------|------------->| User object |
+---------------+              +-------------+
```

This distinction is fundamental:

```text
user
```

is a **reference**.

```text
new User(...)
```

is the **object**.

---

# 2. Why do we need a Heap?

Imagine there was no heap.

Suppose:

```java
User createUser() {
    User user = new User();
    return user;
}
```

The object needs to survive beyond the method invocation.

The stack frame for `createUser()` disappears when the method returns.

Therefore, objects that need a lifetime independent of a particular method invocation need a different memory area.

That's where heap allocation comes in.

Conceptually:

```text
createUser()
     |
     v
Stack frame
     |
     | creates
     v
Heap object
     |
     | method returns
     v
Stack frame disappears

Heap object can still exist
```

For example:

```java
User user = createUser();
```

The returned reference still points to the object.

---

# 3. Is everything in the Heap?

No.

This is an important interview trap.

Consider:

```java
int x = 10;
```

`x` is a local primitive variable.

Its execution state is associated with the current thread's stack frame.

But:

```java
User user = new User();
```

creates an object that is generally allocated on the heap.

Also consider:

```java
String name = "Sundeep";
```

The `String` object has heap-related behavior, while the local variable `name` is a reference.

So don't memorize:

> "Variables are on stack, objects are on heap."

Instead:

> **Each thread has stack frames containing local execution state and references, while objects are generally allocated in the heap. JVM optimizations can change the physical allocation behavior.**

---

# 4. Heap is shared between threads

Suppose you have:

```java
Thread T1
Thread T2
Thread T3
```

Each thread has its own stack:

```text
JVM
│
├── Heap
│    │
│    ├── Object A
│    ├── Object B
│    └── Object C
│
├── Thread 1
│    └── Stack
│
├── Thread 2
│    └── Stack
│
└── Thread 3
     └── Stack
```

All threads can potentially access objects in the heap.

For example:

```java
class Counter {
    int count;
}
```

If:

```java
Counter counter = new Counter();
```

is shared between threads:

```text
Thread 1 ───────┐
                │
                v
             Counter
                ^
                │
Thread 2 ───────┘
```

This is why heap objects are heavily involved in concurrency problems.

For example:

```java
counter.count++;
```

is not automatically thread-safe.

Heap + threads + synchronization will become important later.

---

# 5. Heap structure

Now we reach the important part.

Historically, generational JVMs commonly conceptualized the heap as:

```text
Heap
│
├── Young Generation
│   │
│   ├── Eden
│   ├── Survivor 0
│   └── Survivor 1
│
└── Old Generation
```

This is based on the **generational hypothesis**.

The basic observation is:

> **Most objects die young.**

For example:

```java
for (int i = 0; i < 1_000_000; i++) {

    User user = new User();

    process(user);
}
```

We're creating a huge number of objects.

Many may become unreachable very quickly.

Conceptually:

```text
new object
    ↓
used briefly
    ↓
becomes unreachable
    ↓
garbage
```

It would be wasteful to treat all objects identically.

So JVM garbage collectors exploit object age.

---

# 6. Young Generation

New objects are generally allocated in the **young generation** for collectors that use generational heap organization.

Conceptually:

```text
Young Generation
│
├── Eden
├── Survivor 0
└── Survivor 1
```

Usually the lifecycle looks approximately like:

```text
new object
    |
    v
  Eden
    |
    | survives GC
    v
Survivor
    |
    | survives multiple GCs
    v
Old Generation
```

We'll dedicate the next topic specifically to this.

For now, remember:

> **Young generation is where newly allocated objects generally begin their lifecycle.**

---

# 7. Eden

Most new allocations go into **Eden**.

For example:

```java
Order order = new Order();
```

Conceptually:

```text
Young Generation

+--------------------------------+
|             Eden               |
|                                |
| Order                          |
| User                           |
| Payment                        |
| DTO                            |
+--------------------------------+
| Survivor 0 | Survivor 1        |
+--------------------------------+
```

As your application allocates objects:

```text
Eden
████████████████████████████
```

eventually it becomes sufficiently full that the garbage collector needs to do collection work.

That's where a **young/minor collection** happens.

---

# 8. What happens during a young GC?

Imagine:

```text
Eden:

Object A → unreachable
Object B → reachable
Object C → unreachable
Object D → reachable
Object E → unreachable
```

The GC can conceptually identify:

```text
Dead:
A
C
E

Alive:
B
D
```

Instead of keeping all this fragmentation around, the collector can copy/evacuate surviving objects into survivor regions.

Conceptually:

```text
Before

Eden
+---+---+---+---+---+
| A | B | C | D | E |
+---+---+---+---+---+

A,C,E = dead
B,D   = alive


After

Survivor
+---+---+
| B | D |
+---+---+

Eden can be reused
```

The exact mechanics depend heavily on the collector.

This distinction matters because modern JVMs do not all implement generational GC in exactly the same way.

---

# 9. Survivor spaces

There are traditionally two survivor regions:

```text
S0
S1
```

Why two?

Because the JVM needs somewhere to move surviving objects while reclaiming the previous region.

Conceptually:

```text
Eden
  |
  | GC
  v
S0
```

Next collection:

```text
S0
  |
  | GC
  v
S1
```

Then:

```text
S1
  |
  | GC
  v
S0
```

So objects can bounce between survivor regions as they survive collections.

---

# 10. Object aging

The JVM tracks how long objects survive.

Imagine:

```text
Object A
```

is created.

After the first young GC:

```text
age = 1
```

After another:

```text
age = 2
```

Then:

```text
age = 3
```

Eventually the JVM may decide that the object is long-lived enough to promote it to the old generation.

Conceptually:

```text
Eden
  |
  v
Survivor
  |
  | age increases
  v
Old Generation
```

The exact promotion behavior is collector-dependent.

Don't memorize:

> "After exactly N GCs every object moves to old generation."

That's not universally correct.

---

# 11. Old Generation

The old generation contains longer-lived objects in generational collectors.

For example:

```java
static final Map<String, User> CACHE = new HashMap<>();
```

Suppose you continually add objects:

```java
CACHE.put(id, user);
```

Those objects may remain reachable for a long time.

They are therefore likely to become long-lived.

Conceptually:

```text
Young
  |
  | survives
  v
Old

Old:
+----------------------------+
| User                       |
| Order                      |
| Cache entries              |
| Long-lived application obj |
+----------------------------+
```

The old generation is particularly important for:

* memory pressure
* long GC cycles
* memory leaks
* heap dumps
* application latency

---

# 12. The most important concept: Reachability

Garbage collection isn't fundamentally:

> "Find objects that haven't been used recently."

Instead, the JVM determines whether objects are **reachable** from GC roots.

Suppose:

```java
User user = new User();
```

Conceptually:

```text
GC Root
   |
   v
user reference
   |
   v
User object
```

The object is reachable.

Therefore:

```text
NOT garbage
```

Now:

```java
user = null;
```

If no other reference exists:

```text
GC Root

(no reference)

      X

User object
```

The object becomes unreachable.

It can eventually be reclaimed.

---

# 13. What are GC Roots?

This is a very important concept for interviews and heap dumps.

GC roots can include things such as:

* active thread stacks
* static references
* JNI references
* certain JVM internal references

For example:

```java
public class Cache {

    static User user = new User();
}
```

The static field can keep the object reachable.

Conceptually:

```text
GC Root
   |
   v
static Cache.user
   |
   v
User object
```

Even if you no longer logically need that object, the GC cannot simply delete it because it is still reachable.

This is one of the foundations of a **memory leak in Java**.

---

# 14. Java can have memory leaks

This surprises many people.

People often think:

> "Java has garbage collection, so Java cannot have memory leaks."

False.

A Java memory leak can happen when:

> **Objects are no longer logically needed but remain strongly reachable.**

For example:

```java
class Cache {

    private static final List<User> USERS = new ArrayList<>();

    public static void add(User user) {
        USERS.add(user);
    }
}
```

Imagine:

```java
for (...) {
    Cache.add(new User());
}
```

The list keeps references:

```text
GC Root
   |
   v
static USERS
   |
   +----> User
   +----> User
   +----> User
   +----> User
   +----> User
```

The GC sees:

```text
GC Root
  ↓
USERS
  ↓
User
```

Therefore these objects are reachable.

GC **cannot reclaim them**.

Eventually:

```text
Heap usage
██████████████████████████████
```

and you may get:

```text
java.lang.OutOfMemoryError: Java heap space
```

This is why we'll later study memory leaks separately.

---

# 15. Heap size

The JVM doesn't necessarily use all physical RAM.

You configure heap limits.

Two important JVM options are:

```bash
-Xms
-Xmx
```

### `-Xms`

Initial heap size.

Example:

```bash
-Xms512m
```

means approximately:

```text
Initial heap = 512 MB
```

### `-Xmx`

Maximum heap size.

Example:

```bash
-Xmx2g
```

means approximately:

```text
Maximum heap = 2 GB
```

So:

```bash
java -Xms512m -Xmx2g -jar app.jar
```

means:

```text
Initial heap ≈ 512 MB
Maximum heap ≈ 2 GB
```

Important:

> `-Xmx2g` does **not** mean the JVM immediately consumes 2 GB of physical memory for Java objects.

It specifies a maximum heap limit; actual committed/resident memory behavior is more nuanced.

---

# 16. Heap is not the same as total JVM memory

This is extremely important in production.

Suppose Kubernetes gives your pod:

```text
Container memory limit = 2 GB
```

You might configure:

```text
-Xmx2g
```

and think:

> "Perfect, the JVM has 2 GB."

Not necessarily.

The process uses memory beyond the Java heap.

For example:

```text
Container Memory
│
├── Java Heap
│
├── Metaspace
├── Thread stacks
├── Code cache
├── Direct buffers
├── Native libraries
├── GC structures
├── JVM internal/native memory
└── Other native allocations
```

Therefore:

```text
Container memory ≠ Heap
```

This is **extremely important for Docker/Kubernetes/Spring Boot applications**.

For example:

```text
Pod memory limit = 2 GB
-Xmx = 2 GB
```

can be dangerous because other JVM/native memory also needs space.

Later, when we study JVM memory tuning, we'll discuss this in detail.

---

# 17. Why doesn't JVM just use all available RAM?

Because the JVM needs controlled memory boundaries.

Imagine:

```text
Machine RAM = 16 GB
```

You run:

```text
Service A
Service B
Service C
```

If Service A could freely consume all available memory:

```text
Service A → 14 GB
Service B → crashes
Service C → crashes
```

Instead, you can configure boundaries.

In Kubernetes, there are also container memory limits.

This is one reason heap sizing matters.

---

# 18. Heap allocation

Suppose you execute:

```java
Order order = new Order();
```

Conceptually:

```text
new Order()
    |
    v
JVM allocator
    |
    v
Young generation / allocation region
    |
    v
Memory reserved
    |
    v
Order object created
```

Modern JVMs optimize allocation heavily.

One important concept is:

### TLAB

**Thread Local Allocation Buffer**

Instead of every thread fighting over a single global allocation pointer, the JVM can give each thread a small allocation buffer.

Conceptually:

```text
Heap
+------------------------------------+
|                                    |
| TLAB Thread 1 | TLAB Thread 2     |
|                                    |
+------------------------------------+
```

Then:

```text
Thread 1 → allocate inside its TLAB
Thread 2 → allocate inside its TLAB
```

This reduces contention during allocation.

This becomes interesting when analyzing high-allocation-rate applications.

---

# 19. Example: Spring Boot API

Imagine:

```java
@PostMapping("/orders")
public OrderResponse createOrder(@RequestBody OrderRequest request) {

    Order order = new Order();

    Payment payment = new Payment();

    AuditEvent event = new AuditEvent();

    return mapper.toResponse(order);
}
```

Every request may create:

```text
OrderRequest
Order
Payment
AuditEvent
OrderResponse
String objects
Collections
JSON parser objects
Hibernate objects
Spring framework objects
```

Suppose:

```text
1000 requests/sec
```

and each request creates:

```text
50 KB
```

of temporary objects.

That's:

```text
1000 × 50 KB
= 50 MB/sec
```

of allocation pressure.

In one minute:

```text
≈ 3 GB
```

of allocations.

This **doesn't mean your heap needs 3 GB per minute**.

Why?

Because many objects die quickly and GC reclaims their memory.

This is the whole reason generational GC is useful.

---

# 20. Allocation rate vs heap size

This distinction is extremely important.

Suppose:

```text
Allocation rate = 500 MB/sec
Heap = 4 GB
```

That does **not** automatically mean:

> "We'll run out of memory in 8 seconds."

Because:

```text
Objects
   |
   +--> die quickly
   |
   +--> reclaimed by young GC
```

You could potentially have:

```text
500 MB/sec allocation
+
very efficient young GC
+
low live-set size
```

and run indefinitely.

Conversely:

```text
50 MB/sec allocation
+
huge number of long-lived objects
```

could eventually cause memory pressure.

Therefore:

> **Allocation rate and live-set size are different concepts.**

This distinction becomes extremely important in GC tuning.

---

# 21. Live set

Suppose your application allocates:

```text
1 GB/sec
```

but only:

```text
200 MB
```

of objects remain reachable.

Then:

```text
Allocation = 1 GB/sec
Live data = 200 MB
```

The rest can eventually be reclaimed.

Conceptually:

```text
Allocated objects
██████████████████████████████

Live objects
██████

Garbage
██████████████████████
```

GC is primarily concerned with finding/reclaiming the garbage while preserving reachable objects.

---

# 22. Heap fragmentation

Imagine memory:

```text
+----+----+----+----+----+----+
| A  |    | B  |    | C  |    |
+----+----+----+----+----+----+
```

There is free space, but it's fragmented.

Some collectors can compact or evacuate objects:

```text
Before:

A | free | B | free | C | free


After:

A | B | C | free | free | free
```

But modern collectors use sophisticated region/evacuation strategies.

We'll study this when we get to GC algorithms.

---

# 23. Heap vs Stack — interview comparison

| Heap                                            | Stack                                 |
| ----------------------------------------------- | ------------------------------------- |
| Generally shared                                | Per-thread                            |
| Objects generally allocated here                | Method frames                         |
| Managed by GC                                   | Frames removed as methods return      |
| Larger                                          | Usually much smaller                  |
| Can produce `OutOfMemoryError: Java heap space` | Can produce `StackOverflowError`      |
| Shared objects can cause concurrency issues     | Local execution state is thread-local |

Example:

```java
public void process() {

    User user = new User();

    calculate();
}
```

Conceptually:

```text
Thread Stack

+----------------------+
| process()            |
| user reference ----- |------+
+----------------------+      |
                              |
                              v
                           Heap
                       +----------+
                       | User     |
                       +----------+
```

---

# 24. Heap vs Metaspace

Another common interview question.

### Heap

Contains application object data.

```text
User
Order
Payment
List
Map
DTO
...
```

### Metaspace

Contains class metadata associated with loaded classes.

```text
User.class
Order.class
Payment.class
...
```

Conceptually:

```text
Heap                         Metaspace

User object                  User class metadata
Order object                 Order class metadata
Payment object               Payment class metadata
```

Don't confuse:

```text
User object
```

with:

```text
User.class metadata
```

They belong to different conceptual areas.

---

# 25. Heap exhaustion

Suppose:

```java
List<byte[]> list = new ArrayList<>();

while (true) {
    list.add(new byte[1024 * 1024]);
}
```

We're continuously creating 1 MB arrays and retaining them.

Conceptually:

```text
Heap

Object 1
Object 2
Object 3
Object 4
...
Object N
```

Because:

```text
list → object
```

the objects remain reachable.

Eventually:

```text
Heap
██████████████████████████████
             |
             v
        allocation fails
             |
             v
OutOfMemoryError
```

Typically:

```text
java.lang.OutOfMemoryError: Java heap space
```

The important lesson:

> GC cannot solve a problem where the application is intentionally retaining all the objects.

GC can reclaim **unreachable** objects, not objects that are still strongly reachable.

---

# 26. Heap dump

When you're investigating a memory problem, one powerful tool is a **heap dump**.

It gives you a snapshot of heap objects and their relationships.

Conceptually:

```text
Heap Dump
│
├── Object types
├── Object counts
├── Object sizes
├── References
├── Dominators
└── GC roots
```

For example, you might discover:

```text
HashMap
  |
  +-- 4,000,000 entries
  |
  +-- User objects
  |
  +-- huge retained size
```

Then you can trace:

```text
GC Root
   ↓
static cache
   ↓
HashMap
   ↓
User objects
```

and identify the leak.

We'll later do a dedicated deep dive into heap dumps.

---

# 27. A very important distinction: shallow size vs retained size

When analyzing a heap dump, you might see:

```text
Object A
```

with:

```text
Shallow size = 100 bytes
```

But it might reference:

```text
Object B
Object C
Object D
...
```

which together occupy:

```text
500 MB
```

So you need to understand:

### Shallow size

Memory directly occupied by the object itself.

### Retained size

Memory that would become collectible if that object/reference became unreachable, subject to the heap graph and dominator relationships.

Example:

```text
Cache
 |
 +----> Map
          |
          +----> User
          +----> User
          +----> User
          ...
```

The `Cache` object itself might be tiny.

But its **retained size** could be enormous.

This is why memory leak investigation isn't simply:

> "Find the biggest object."

---

# 28. Modern JVMs and the generational model

One important caveat before we move on:

You will often see diagrams like:

```text
Heap
├── Eden
├── S0
├── S1
└── Old
```

This is useful for learning.

But **the exact physical layout depends on the garbage collector**.

Modern JVMs include collectors such as:

* G1
* ZGC
* Shenandoah
* Parallel GC
* Serial GC

For example, G1 uses a region-based heap rather than one giant contiguous Eden/Old layout.

So don't assume:

> "Every JVM literally has one Eden area, two Survivor spaces, and one Old Generation."

The better mental model is:

> **Generational concepts describe object age/lifetime, while the physical heap organization depends on the selected garbage collector.**

This distinction is important for modern Java interviews.

---

# 29. The complete object lifecycle

For a traditional generational mental model:

```text
                 new Object()
                      |
                      v
                    Eden
                      |
                Young GC
                  /     \
                 /       \
            dead         alive
             |             |
             v             v
          reclaimed     Survivor
                           |
                     more GC cycles
                           |
                           v
                    Old Generation
                           |
                        GC cycle
                       /        \
                    dead        alive
                     |            |
                     v            v
                 reclaimed      remains
```

That lifecycle is the bridge between:

```text
Heap
  ↓
Young Generation
  ↓
Old Generation
  ↓
Garbage Collection
  ↓
GC pauses
```

---

# 30. The three numbers you should always think about

When analyzing a Java application's heap, don't just ask:

> "How much heap is being used?"

Think about three things:

### 1. Allocation rate

How quickly are we creating objects?

```text
MB/sec
```

### 2. Live set

How much data remains reachable?

```text
MB / GB
```

### 3. Heap capacity

How much heap do we have available?

```text
-Xmx
```

For example:

```text
Allocation rate = 500 MB/s
Live set        = 600 MB
Max heap        = 4 GB
```

could be perfectly healthy.

But:

```text
Allocation rate = 50 MB/s
Live set        = 3.8 GB
Max heap        = 4 GB
```

is potentially dangerous.

---

# 31. What happens when heap gets full?

Very simplified:

```text
Application
    |
    v
Allocate objects
    |
    v
Heap fills
    |
    v
GC
    |
    +---- garbage found
    |       |
    |       v
    |    reclaim
    |
    +---- insufficient space
            |
            v
       more GC / promotion /
       allocation failure
            |
            v
    OutOfMemoryError
```

The actual behavior depends on:

* GC algorithm
* allocation rate
* live set
* heap size
* object sizes
* promotion behavior
* fragmentation
* GC configuration

---

# 32. The key mental model

If you remember only one diagram from today's topic, remember this:

```text
                    JVM HEAP
                       |
          +------------+------------+
          |                         |
          v                         v
     Young Generation          Old Generation
          |                         |
     +----+----+                    |
     |    |    |                    |
     v    v    v                    |
   Eden  S0    S1                    |
     |    |    |                    |
     +----+----+                    |
          |                         |
          | objects survive         |
          +------------------------>|
                                    |
                              long-lived objects
                                    |
                                    v
                                   GC
                                    |
                          +---------+---------+
                          |                   |
                       reachable          unreachable
                          |                   |
                          v                   v
                       remains            reclaimed
```

And underneath everything:

```text
              Heap
                |
       +--------+--------+
       |                 |
 Allocation           Reachability
       |                 |
       v                 v
  new objects         GC Roots
       |                 |
       v                 v
Young generation     Live objects
                         |
                         v
                    GC preserves
```

---

# 33. What comes next

Now that we understand the heap, the **next topic should be Young Generation**, where we'll go much deeper into:

```text
Object allocation
      ↓
Eden
      ↓
Young GC
      ↓
Survivor spaces
      ↓
Object aging
      ↓
Promotion
      ↓
Old Generation
```

We'll also answer some very common interview questions:

* Why do most objects die young?
* What exactly happens during a Minor GC?
* What happens to surviving objects?
* Why are there two Survivor spaces?
* What is object age?
* What is promotion?
* What causes premature promotion?
* What are allocation failures?
* How does G1 change the traditional Eden/Survivor picture?
* How do you observe this in a real Spring Boot application?


Absolutely. Since you already understand the **heap**, the next important concept is:

> **A Java memory leak does NOT mean memory becomes unreachable. It means memory is still reachable according to the JVM, but your application no longer needs it.**

That's why GC cannot save you.

---

# 1. First understand the core idea

Suppose:

```java
public void process() {
    User user = new User();
}
```

After `process()` returns:

```text
Stack
  |
  X user

Heap
  User object
```

The `user` reference disappears.

So:

```text
GC Roots
   |
   X
   |
User object
```

The object becomes **unreachable** → GC can collect it.

---

## Memory leak

Now imagine:

```java
static List<User> users = new ArrayList<>();

public void process() {
    User user = new User();
    users.add(user);
}
```

Every call adds another `User`.

```text
GC Root
   |
static users
   |
   +---- User
   +---- User
   +---- User
   +---- User
   +---- User
   ...
```

Even if your application doesn't need those users anymore:

```text
GC Root
   ↓
static List
   ↓
User objects
```

They are still **reachable**.

Therefore:

```text
GC says:
"These objects are reachable.
I cannot delete them."
```

Eventually:

```text
Heap usage
   ↑
   ↑
   ↑
   ↑
   ↑
   💥 OutOfMemoryError
```

This is the fundamental pattern behind Java memory leaks.

---

# 2. The most important interview definition

Think of memory leaks as:

> **Unintended object retention.**

The object is:

* no longer logically needed
* but still strongly reachable from a GC Root

Therefore GC doesn't reclaim it.

---

# 3. The "sure-shot" ways you can create memory leaks

There are several patterns you should immediately recognize when reviewing Java code.

---

# 4. Static collections — probably the #1 thing to look for

This is the classic example.

```java
public class UserCache {

    private static final List<User> users = new ArrayList<>();

    public static void add(User user) {
        users.add(user);
    }
}
```

Every time:

```java
UserCache.add(user);
```

happens:

```text
static users
     |
     +--> User
     +--> User
     +--> User
     +--> User
     +--> User
```

If nothing removes them:

```java
users.size()
```

keeps growing forever.

### What to search for in code

Look for:

```java
static List
static Map
static Set
static Collection
static ConcurrentHashMap
static ConcurrentHashMap
```

Especially:

```java
static final List<...>
static final Map<...>
```

Static itself isn't a leak.

The problem is:

> **long-lived reference + unbounded growth**

---

# 5. Unbounded caches

This is extremely common in real applications.

Someone writes:

```java
private final Map<String, User> cache = new HashMap<>();

public User getUser(String id) {

    if (!cache.containsKey(id)) {
        cache.put(id, database.find(id));
    }

    return cache.get(id);
}
```

Looks innocent.

But imagine:

```text
Request 1 → user1
Request 2 → user2
Request 3 → user3
...
Request 1,000,000 → user1,000,000
```

The cache becomes:

```text
1 million objects
```

And nothing is ever removed.

### This is an extremely important distinction

A cache is not automatically safe.

A cache should generally have some eviction policy:

```text
Maximum size
TTL
LRU
Expiration
Weak references
Explicit invalidation
```

For example:

```text
Cache
 ├── max size = 10,000
 ├── TTL = 10 minutes
 └── evict old entries
```

---

# 6. ThreadLocal leaks

This one is **very important for backend/Spring Boot interviews**.

Consider:

```java
private static final ThreadLocal<UserContext> context =
        new ThreadLocal<>();
```

Then:

```java
public void process(UserContext userContext) {

    context.set(userContext);

    // processing
}
```

Someone forgets:

```java
context.remove();
```

Now consider a thread pool.

```text
ThreadPool

Thread-1
Thread-2
Thread-3
Thread-4
...
```

Threads are long-lived.

The ThreadLocal value can remain associated with the worker thread.

```text
Thread
  |
ThreadLocalMap
  |
UserContext
  |
Large Object Graph
```

So:

```text
Thread Pool
   ↓
long-lived threads
   ↓
ThreadLocal
   ↓
objects
```

And those objects stay alive.

### Correct pattern

Use:

```java
try {
    context.set(userContext);

    // work

} finally {
    context.remove();
}
```

This is especially important in:

* Spring applications
* servlet containers
* async executors
* scheduled executors
* application servers

because threads are reused.

---

# 7. List/Map that keeps accumulating request data

This is another extremely realistic backend leak.

Imagine:

```java
@Service
public class AuditService {

    private final List<String> logs = new ArrayList<>();

    public void audit(String message) {
        logs.add(message);
    }
}
```

Spring creates the service as a singleton.

So effectively:

```text
Application lifetime
       |
AuditService
       |
      logs
       |
       + message
       + message
       + message
       + message
       ...
```

Every request adds data.

If the application receives:

```text
10,000 requests
100,000 requests
1,000,000 requests
10,000,000 requests
```

the list grows continuously.

### When reviewing Spring Boot code, pay special attention to:

```java
@Service
@Component
@Repository
@Controller
```

because these are commonly long-lived beans.

If you see:

```java
@Service
public class Something {

    private List<Something> data = new ArrayList<>();
}
```

ask:

> **Who removes these objects?**

If the answer is "nobody", investigate.

---

# 8. Listeners / observers that aren't removed

Consider:

```java
eventBus.register(myListener);
```

If the event bus stores listeners:

```text
EventBus
   |
   +--> Listener A
   +--> Listener B
   +--> Listener C
```

Suppose an object is supposed to live for only 5 minutes.

But it registers itself:

```java
eventBus.register(this);
```

and never unregisters:

```java
eventBus.unregister(this);
```

Then:

```text
EventBus
   ↓
Listener
   ↓
Service
   ↓
large object graph
```

The listener keeps the entire object graph alive.

This is called a **listener/subscriber leak**.

Look for:

```java
addListener()
register()
subscribe()
addObserver()
addCallback()
```

and ask:

> Where is the corresponding `remove/unregister/unsubscribe`?

---

# 9. Event listeners are especially dangerous

For example:

```java
class MyComponent {

    void start() {
        eventSystem.subscribe(event -> {
            process(event);
        });
    }
}
```

If `start()` gets called repeatedly:

```text
subscribe
subscribe
subscribe
subscribe
subscribe
```

you may end up with:

```text
EventSystem
 ├── listener
 ├── listener
 ├── listener
 ├── listener
 └── listener
```

And each listener may capture references.

---

# 10. Anonymous classes / lambdas capturing objects

This is a slightly more subtle one.

Consider:

```java
class OrderProcessor {

    private HugeObject hugeObject;

    void register() {

        eventBus.register(() -> {
            hugeObject.process();
        });
    }
}
```

The lambda captures `this`.

So the relationship can effectively become:

```text
EventBus
   ↓
Lambda
   ↓
OrderProcessor
   ↓
HugeObject
```

Even if you think:

```java
OrderProcessor
```

is finished, the event bus still has a reference through the lambda.

Therefore the entire graph stays alive.

---

# 11. Executor / task queue leaks

Very important in concurrent Java applications.

Consider:

```java
ExecutorService executor =
        Executors.newSingleThreadExecutor();
```

Then:

```java
executor.submit(() -> process(hugeObject));
```

If tasks accumulate faster than they execute:

```text
Executor
   |
BlockingQueue
   |
   +--> Task
   +--> Task
   +--> Task
   +--> Task
   +--> Task
   ...
```

And every task contains:

```text
Task
 ↓
HugeObject
```

Then:

```text
Queue
 ↓
Task
 ↓
HugeObject
```

Memory grows.

This is especially dangerous when:

```text
Producer speed > Consumer speed
```

Example:

```text
Incoming requests: 10,000/sec
Processing capacity: 1,000/sec
```

The queue keeps growing.

Eventually:

```text
Heap 💥
```

### Code-review warning signs

Look for:

```java
ExecutorService
ThreadPoolExecutor
BlockingQueue
LinkedBlockingQueue
ArrayBlockingQueue
CompletableFuture
```

and especially **unbounded queues**.

---

# 12. CompletableFuture chains

You can also accidentally retain large objects through asynchronous chains.

For example:

```java
CompletableFuture
    .supplyAsync(() -> loadHugeObject())
    .thenApply(obj -> process(obj))
    .thenApply(obj -> ...)
```

If futures are retained in some global collection:

```java
List<CompletableFuture<?>> futures;
```

then the entire chain can remain reachable.

Again, the pattern is:

```text
Long-lived reference
        ↓
Future
        ↓
Lambda
        ↓
Large object
```

---

# 13. Maps with keys that should have disappeared

Consider:

```java
Map<User, UserSession> sessions = new HashMap<>();
```

Suppose users log out.

But you never do:

```java
sessions.remove(user);
```

Then:

```text
Map
 |
 +--> User1
 +--> User2
 +--> User3
 ...
```

Users remain strongly referenced.

A particularly dangerous variant is:

```java
Map<String, Object> map = new HashMap<>();
```

where keys are generated dynamically:

```java
map.put(UUID.randomUUID().toString(), hugeObject);
```

If there is no cleanup:

```text
unbounded key space
       +
no eviction
       =
memory leak
```

---

# 14. WeakHashMap can solve specific cases

Java provides:

```java
WeakHashMap
```

For example:

```java
Map<Key, Value> cache = new WeakHashMap<>();
```

The important idea is that keys can be garbage collected when there are no strong references elsewhere.

But don't think:

> "Use WeakHashMap and all memory leaks disappear."

It doesn't.

It is useful for particular cache/metadata scenarios, not a universal cache implementation.

---

# 15. JDBC / database resources

This is slightly different.

Traditionally:

```java
Connection connection = dataSource.getConnection();
Statement statement = connection.createStatement();
ResultSet resultSet = statement.executeQuery(...);
```

If resources aren't closed:

```text
Connection
Statement
ResultSet
```

can accumulate.

Modern Java should use:

```java
try (
    Connection connection = dataSource.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet resultSet = statement.executeQuery()
) {
    // use resources
}
```

This isn't always a **heap memory leak** in the strict sense, but it can absolutely cause resource exhaustion and eventually:

```text
OutOfMemoryError
Too many connections
Pool exhaustion
Native memory exhaustion
```

So when debugging a production memory problem, don't only think about heap.

---

# 16. File/InputStream/OutputStream leaks

Same idea:

```java
InputStream input = new FileInputStream(file);
```

without closing it.

Use:

```java
try (InputStream input = new FileInputStream(file)) {
    ...
}
```

Again, this is primarily a resource leak, but it can contribute to application failure.

---

# 17. Caches are probably the biggest real-world source

In backend systems you'll frequently encounter:

```text
Redis
Caffeine
Guava Cache
ConcurrentHashMap
HashMap
static Map
```

The dangerous question isn't:

> "Is this a cache?"

Ask:

> **"What controls its maximum lifetime and maximum size?"**

For example:

```java
Map<String, User> cache = new ConcurrentHashMap<>();
```

is suspicious if there is:

```text
NO:
  TTL
  max size
  eviction
  invalidation
```

---

# 18. A very important concept: GC Roots

To really understand leaks, memorize the concept of **GC Roots**.

Objects are collected based on reachability.

Simplified:

```text
GC Root
   ↓
Object A
   ↓
Object B
   ↓
Object C
```

All are reachable.

Therefore:

```text
GC cannot collect them.
```

Typical GC roots include things such as:

```text
Local variables of active threads
Static fields
Active threads
JNI/native references
Class-related references
```

Therefore, when you investigate a leak, you're basically asking:

> **"Why is this object still reachable from a GC Root?"**

That question is gold.

---

# 19. The most useful mental model

Don't think:

```text
Object exists → memory leak
```

Instead:

```text
Object exists
     ↓
Is it reachable?
     ↓
YES
     ↓
Is it still logically needed?
     ↓
NO
     ↓
MEMORY LEAK
```

For example:

```text
                 GC Root
                    ↓
              static cache
                    ↓
                 User
                    ↓
              UserProfile
                    ↓
              HugeMetadata
```

The entire graph stays alive.

---

# 20. What should YOU look for during code review?

When you're reviewing a Java/Spring Boot application, I would have this mental checklist.

### 🔴 1. Static mutable collections

Search:

```java
static List
static Map
static Set
static Cache
```

Ask:

> Who clears it?

---

### 🔴 2. Unbounded caches

Look for:

```java
HashMap
ConcurrentHashMap
List
Cache
```

Ask:

> What is the maximum size?

and:

> What is the eviction policy?

---

### 🔴 3. Singleton Spring beans holding request data

Look at:

```java
@Service
@Component
@Repository
```

If they have fields like:

```java
private List<...>
private Map<...>
private Set<...>
```

ask:

> Is this state supposed to live for the entire application lifetime?

---

### 🔴 4. ThreadLocal

Search:

```java
ThreadLocal
```

Ask:

> Is `remove()` called?

Especially with thread pools.

---

### 🔴 5. Listeners

Search:

```java
register
subscribe
addListener
addObserver
addCallback
```

Then find:

```java
unregister
unsubscribe
removeListener
removeObserver
removeCallback
```

---

### 🔴 6. Queues

Search:

```java
BlockingQueue
ExecutorService
ThreadPoolExecutor
```

Ask:

> Can producers add work faster than consumers process it?

---

### 🔴 7. Futures

Search:

```java
CompletableFuture
Future
```

Ask:

> Are completed futures being retained somewhere?

---

### 🔴 8. Request/session data

Look for:

```java
static
singleton
cache
session
ThreadLocal
```

holding:

```text
Request
User
HttpServletRequest
SecurityContext
large DTOs
large JSON
files
byte[]
```

---

### 🔴 9. Large byte arrays

Search:

```java
byte[]
ByteBuffer
InputStream
OutputStream
```

Especially:

```java
Map<String, byte[]>
List<byte[]>
```

because one forgotten reference can retain hundreds of MB.

---

# 21. One particularly dangerous example

Imagine:

```java
@Service
public class ImageService {

    private final Map<String, byte[]> images =
            new HashMap<>();

    public void upload(String id, byte[] image) {
        images.put(id, image);
    }
}
```

Each image:

```text
5 MB
```

Suppose:

```text
100,000 images
```

Then:

```text
5 MB × 100,000
= ~500 GB
```

Obviously the JVM would crash much earlier.

But the key point is:

```text
GC isn't broken.
```

GC sees:

```text
Spring Singleton
       ↓
ImageService
       ↓
HashMap
       ↓
byte[]
```

Everything is reachable.

Therefore:

> **GC correctly refuses to collect it.**

---

# 22. How a memory leak usually progresses in production

You might see:

```text
Application starts

Heap:
████░░░░░░ 40%

Requests arrive

Heap:
█████░░░░░ 50%

GC

Heap:
████░░░░░░ 42%

Requests

Heap:
██████░░░░ 60%

GC

Heap:
█████░░░░░ 52%

Requests

Heap:
████████░░ 80%

GC

Heap:
███████░░░ 72%

...

Heap:
██████████ 95%

GC

Heap:
█████████░ 91%

...

java.lang.OutOfMemoryError: Java heap space
```

The important clue is:

### **Post-GC heap usage keeps increasing.**

That is one of the strongest signals of a memory leak.

---

# 23. Leak vs normal memory growth

This distinction is extremely important.

Suppose:

```text
Before GC: 80%
After GC: 20%
```

That's probably fine.

But:

```text
After GC #1 → 20%
After GC #2 → 35%
After GC #3 → 50%
After GC #4 → 65%
After GC #5 → 80%
```

That's suspicious.

Because objects are surviving GC.

This gives you the famous **sawtooth pattern**:

```text
Heap
100% |                         /\
 80% |                    /\  /  \
 60% |               /\  /  \/    \
 40% |          /\  /  \/          \
 20% |____/\___/  \/                \
     +--------------------------------
             time →
```

Normal applications also have sawtooth patterns, so the key is whether the **post-GC baseline keeps rising**.

---

# 24. How do you actually prove there's a memory leak?

This is where heap dumps become extremely useful.

You take:

```text
Heap Dump #1
```

Then after the application has processed more traffic:

```text
Heap Dump #2
```

Then compare them.

Suppose:

```text
Heap Dump #1

User              10 MB
HashMap            5 MB
byte[]            20 MB
```

Later:

```text
Heap Dump #2

User             800 MB
HashMap           750 MB
byte[]            700 MB
```

Then investigate:

```text
Who owns the HashMap?
```

You might discover:

```text
GC Root
   ↓
Spring Singleton
   ↓
ImageService
   ↓
images
   ↓
HashMap
   ↓
byte[]
```

Boom.

You found the retention path.

---

# 25. The most important heap-dump concepts

When you eventually learn heap dumps, pay attention to:

### Shallow size

Memory consumed by the object itself.

### Retained size

Memory that would become collectible if this object became unreachable.

For leaks, **retained size** is often much more interesting.

For example:

```text
ImageService
  shallow size:     100 bytes

  retained size:    2 GB
```

The service itself is tiny.

But it retains:

```text
HashMap
 ↓
1 million entries
 ↓
byte[]
 ↓
huge object graph
```

So the service is the **retainer**.

---

# 26. A fantastic interview question

**Q: Can an object that is no longer used by the application still be garbage collected?**

Yes, if it is unreachable.

For example:

```java
User user = new User();
user = null;
```

If there are no other references:

```text
GC Root
   ↓
nothing

User
   X
```

GC can collect it.

But:

```java
static List<User> users = new ArrayList<>();

users.add(user);
```

Even if your business logic no longer needs that user:

```text
GC Root
 ↓
static users
 ↓
User
```

GC cannot know your business intent.

That's the key insight:

> **GC understands reachability, not business usefulness.**

---

# 27. The rule I want you to remember

Whenever you see a long-lived object holding another object, ask:

```text
Who owns this reference?

How long should it live?

Who removes it?

What happens if 1 million requests happen?

Is there a maximum size?

Is there TTL/expiration?

Can this reference survive forever?
```

If you train yourself to ask those questions, you'll start spotting leaks during code review.

---

# 28. Your Java/Spring memory-leak danger map

For the backend applications you're working with, I'd rank the areas like this:

```text
                    MEMORY LEAK RISK

                         🔥🔥🔥🔥🔥
                    Unbounded caches
                         │
                    Static collections
                         │
                    ThreadLocal
                         │
                    Spring singleton state
                         │
                    Event listeners
                         │
                    Executor queues
                         │
                    CompletableFuture retention
                         │
                    Session/request objects
                         │
                    Large byte[] / buffers
                         │
                    DB/file resources
                         │
                    Normal local variables
                         🔥
```

The last one is usually **not** your problem because local references disappear when the method/thread completes.

---

## The big picture

You can now connect **heap → GC → memory leak → OOM**:

```text
                 HEAP
                   │
                   ↓
            Objects allocated
                   │
                   ↓
            Objects become unused
                   │
            ┌──────┴──────┐
            │             │
      Unreachable      Reachable
            │             │
            ↓             ↓
        GC collects   Still retained
                          │
                          ↓
                    More allocations
                          │
                          ↓
                  Old objects survive GC
                          │
                          ↓
                Post-GC heap grows
                          │
                          ↓
                  Heap approaches max
                          │
                          ↓
              OutOfMemoryError
```

And the **root cause** is usually somewhere along:

```text
GC Root
   ↓
long-lived reference
   ↓
container/cache/listener/thread/etc.
   ↓
object that should have died
```

That's what you should hunt for.

Next, the natural step in your JVM checklist is **how to actually diagnose one in production using GC logs + heap dumps + Eclipse MAT/JProfiler/VisualVM**, including how to find the exact object → reference → GC Root that is causing the leak.
