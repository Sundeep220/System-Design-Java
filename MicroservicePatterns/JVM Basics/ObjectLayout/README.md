# Object Layout & Memory — Deep Dive

Every Java object has a hidden overhead beyond its fields. Understanding how objects are laid out in memory explains why Java uses more memory than C, how the GC tracks objects, and how locks work — all without any Java code changes.

> **Every Java object has a 12-16 byte header before your actual data. This overhead adds up significantly in large-scale applications. Knowing object layout is critical for memory optimization and senior-level interviews.**

---

# 1. What is Object Layout?

When you create a Java object:

```java
class Point {
    int x;   // 4 bytes
    int y;   // 4 bytes
}

Point p = new Point();
```

You might expect `p` to use 8 bytes (two ints). In reality:

```text
Memory layout of Point object:
+---------------------+
|  Object Header       |  12-16 bytes (hidden overhead!)
|---------------------|
|  int x              |  4 bytes
|  int y              |  4 bytes
|---------------------|
|  Padding            |  0-4 bytes (alignment)
+---------------------+
Total: 20-24 bytes for two ints!
```

The object header is always present, regardless of how few fields you have.

---

# 2. The Object Header

Every Java object has a header composed of:

```text
Object Header (12 bytes with compressed OOPs, 16 bytes without):

+------------------+
|  Mark Word       |  8 bytes — GC info, lock state, hash code
+------------------+
|  Klass Pointer   |  4 bytes (compressed) or 8 bytes — pointer to class metadata
+------------------+
```

### Mark Word (8 bytes)

The mark word is the most versatile field — its meaning changes based on the object's state:

```text
Object State          | Mark Word Content
---------------------+--------------------------------------------------
Normal (unlocked)    | identity hashCode (31 bits) + age (4 bits) + flags
Biased locking       | thread ID + epoch + age + biased lock flag
Lightweight locked   | pointer to lock record on thread stack
Heavyweight locked   | pointer to inflated monitor (OS mutex)
Marked for GC        | forwarding pointer (during GC copying)
```

This one 8-byte field serves MULTIPLE purposes:

```text
1. GC uses it:
   - Object age (how many Minor GCs survived, 4 bits → max age 15)
   - GC state (during collection: marks, forwarding pointers)

2. Synchronization uses it:
   - Lock state (unlocked / biased / lightweight / heavyweight)
   - Thread ID for biased locking
   - Pointer to monitor for synchronized(obj) {}

3. Identity hash code (System.identityHashCode()):
   - Computed on first call, stored in mark word
   - Stays the same even if GC moves the object
```

### Klass Pointer (4 or 8 bytes)

Points to the class's metadata in Metaspace:

```text
Object
  mark word ──→ (lock/GC state)
  klass ptr ──→ Metaspace: Class metadata for Point
                  ├── field descriptions
                  ├── method vtable
                  └── ...
```

With compressed OOPs (Compressed Ordinary Object Pointers): 4 bytes.
Without: 8 bytes.

---

# 3. Compressed OOPs

On a 64-bit JVM, pointers are naturally 8 bytes. But with compressed OOPs enabled (default for heaps up to ~32GB):

```text
JVM stores object pointers as 32-bit values instead of 64-bit
32-bit value × 8 (alignment multiplier) = 36-bit address space = ~32 GB addressable

Benefits:
  - Klass pointer: 8 bytes → 4 bytes (saves 4 bytes per object!)
  - All references: 8 bytes → 4 bytes
  - Massive memory savings on applications with millions of objects
```

Controlled by:
```bash
-XX:+UseCompressedOops      # default on for heaps < ~32GB
-XX:+UseCompressedClassPointers  # compress klass pointer specifically
```

When heap > 32GB:

```text
Compressed OOPs automatically disabled
  → All references become 8 bytes again
  → Memory usage increases by ~30-50% for reference-heavy apps
  → Often BETTER to use two 28GB JVMs than one 56GB JVM!
```

---

# 4. Memory Overhead Per Object

```text
Minimum object size: 16 bytes (12 byte header + at least 4 bytes for alignment)

Object class          | Actual size (64-bit, compressed OOPs)
---------------------+--------------------------------------
new Object()         | 16 bytes  (12 header + 4 padding)
new Integer(42)      | 16 bytes  (12 header + 4 int value)
new Long(42L)        | 24 bytes  (12 header + 8 long + 4 padding)
new String("")       | 24 bytes  (12 header + 4 ref to char[] + 4 hash + 4 padding)
new ArrayList()      | 40 bytes  (+ internal array object)
```

For arrays, there's an extra 4-byte length field:

```text
Array header: 16 bytes (12 byte object header + 4 byte length)

int[] arr = new int[10]:
  16 bytes header + 40 bytes (10 ints × 4) = 56 bytes
  
Object[] arr = new Object[10]:
  16 bytes header + 40 bytes (10 refs × 4, compressed OOPs) = 56 bytes
```

---

# 5. Field Alignment and Padding

The JVM aligns fields to their natural alignment for CPU efficiency:

```text
Alignment rules:
  byte, boolean:  1-byte alignment
  short, char:    2-byte alignment
  int, float:     4-byte alignment
  long, double:   8-byte alignment
  reference:      4-byte alignment (compressed) or 8-byte (uncompressed)

Objects must be 8-byte aligned (total size is multiple of 8)
```

Example of how field ordering affects memory:

```java
// BAD field ordering — wastes memory
class BadLayout {
    boolean flag;  // 1 byte + 3 bytes padding (for int alignment)
    int value;     // 4 bytes
    boolean flag2; // 1 byte + 7 bytes padding (for object 8-byte alignment)
    long bigValue; // 8 bytes
    // Total: 1+3+4+1+7+8 = 24 bytes (of fields) + 12 header = 36 → padded to 40 bytes
}

// BETTER field ordering (JVM usually reorders automatically)
class GoodLayout {
    long bigValue; // 8 bytes
    int value;     // 4 bytes
    boolean flag;  // 1 byte
    boolean flag2; // 1 byte + 2 bytes padding
    // Total: 8+4+1+1+2 = 16 bytes (of fields) + 12 header = 28 → padded to 32 bytes
}
```

The HotSpot JVM actually **reorders fields** for optimal packing — but not perfectly, and there are cases where padding is unavoidable. JVM Tools like JOL (Java Object Layout) can show you exact field layout.

---

# 6. Shallow vs Deep Object Size

```text
Shallow size:
  Memory directly occupied by the object
  Just the header + its own fields (not what it references)

Deep size (retained size):
  Shallow size + all objects reachable ONLY through this object

Example:
  ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));

  Shallow size of ArrayList:
    12 (header) + 4 (Object[] elementData ref) + 4 (int size) + ... ≈ 40 bytes

  Deep size of ArrayList:
    40 (ArrayList itself)
    + 64 (Object[] with 3 refs + 3 padding)
    + 24 × 3 (three String objects)
    = ~160 bytes
```

This distinction is why heap dump tools show both "shallow heap" and "retained heap."

---

# 7. Object Identity and Hash Code

Every object has an **identity** — even two `new Object()` instances are distinct objects.

```java
Object a = new Object();
Object b = new Object();

a == b  // false — different objects (different memory addresses)
System.identityHashCode(a) != System.identityHashCode(b)  // usually
```

Identity hash code is stored in the mark word on first call to `System.identityHashCode()` or `Object.hashCode()` (if not overridden):

```text
First call to hashCode():
  → Computed (based on memory address or random value)
  → Stored in mark word
  → Returned

Subsequent calls:
  → Read from mark word
  → Same value returned (even after GC moves the object!)
```

When GC moves an object:
```text
Before GC: Object at address 0x1000, hashCode stored as 0xABC
After GC:  Object moved to address 0x5000, but hashCode still 0xABC (stored in mark word, moved with object)
```

This is why object identity hash code is stable despite GC copying collectors.

---

# 8. Lock State in the Mark Word

`synchronized(obj)` uses the mark word to track lock state:

### Biased Locking (eliminated in Java 15+)

```text
If only one thread ever uses the lock:
  Mark word = thread ID of the biasing thread
  Lock acquisition: just check thread ID in mark word (very fast, no CAS)
  Cost: revocation is expensive if another thread tries to lock
```

### Lightweight Lock

```text
When a second thread contends:
  Mark word = pointer to "Lock Record" on the owning thread's stack
  Lock acquisition: CAS operation to swap mark word
  Cost: moderate (CAS is faster than OS mutex)
```

### Heavyweight Lock (Inflated Monitor)

```text
When lightweight lock contention is high:
  Mark word = pointer to an "ObjectMonitor" object
  ObjectMonitor contains: owner thread, entry queue, wait queue
  Lock acquisition: OS-level mutex (park/unpark)
  Cost: highest (OS context switch)
```

Lock inflation is one-way — once inflated to heavyweight, rarely deflated.

This is why:
```java
synchronized(obj) { }  // trivial operation with no contention: very fast
synchronized(obj) { }  // with high contention: expensive OS mutex
```

---

# 9. Object Size in Practice — Cost of Small Objects

Consider a `Map.Entry` in a HashMap:

```java
HashMap<Integer, String> map = new HashMap<>();
map.put(42, "hello");
```

Memory for this one entry:

```text
HashMap itself:          48 bytes
HashMap$Node (entry):   32 bytes (header + hash int + key ref + value ref + next ref)
Integer(42):            16 bytes
String("hello"):        24 bytes
char[] value ("hello"): 32 bytes (header + length + 5 chars × 2 bytes)
Total for one entry: ≈ 152 bytes for what conceptually is: 42 → "hello"
```

With 1 million entries: ~150 MB just for this map.

This is why memory-efficient data structures matter at scale:
- `int[]` vs `Integer[]`: 4 bytes vs 16+ bytes per element
- Primitive arrays vs boxed collections: 4x-8x memory difference
- Custom compact objects vs chained small objects: significant difference

---

# 10. Tools to Inspect Object Layout

### JOL (Java Object Layout) Library

```java
// Add to build.gradle: implementation 'org.openjdk.jol:jol-core:0.17'

System.out.println(ClassLayout.parseClass(Point.class).toPrintable());
```

Output:
```text
# Running 64-bit HotSpot VM.
# Using compressed oop with 3-bit shift.
# Point object internals:
 OFFSET  SIZE   TYPE DESCRIPTION
      0     4        (object header)
      4     4        (object header)
      8     4        (object header - compressed klass)
     12     4    int Point.x
     16     4    int Point.y
     20     4        (loss due to the next object alignment)
Instance size: 24 bytes
```

### jcmd

```bash
jcmd <pid> GC.class_histogram  # shows object counts and sizes per class
```

### Eclipse MAT

Shows shallow and retained sizes for every object in a heap dump.

---

# 11. Practical Impact on Production

### For microservices processing high request volumes:

```text
Each HTTP request may create:
  Request object:       200 bytes
  Response object:      300 bytes
  Service objects:      500 bytes
  Headers map:          800 bytes
  DTO objects:         1000 bytes
  Intermediate strings: 400 bytes
  ≈ 3KB per request

At 1000 req/sec: 3MB/sec allocation rate
At 10000 req/sec: 30MB/sec allocation rate
→ Young Gen needs to absorb this allocation rate
```

Understanding object size helps you:
- Estimate Young Gen size requirements
- Identify allocation hotspots (via `async-profiler -e alloc`)
- Decide when to pool/reuse objects

---

# Interview Preparation — Object Layout

---

## Q1: What is the object header in Java and what does it contain?

**Answer:**

Every Java object has a header (12-16 bytes) that precedes its fields:

1. **Mark Word (8 bytes)**: multipurpose field containing:
   - GC age (how many GC cycles the object survived — 4 bits, max 15)
   - Lock state (unlocked / biased / lightweight / heavyweight)
   - Identity hash code (31 bits, stored after first `hashCode()` call)
   - GC forwarding pointer (during copying GC)

2. **Klass Pointer (4 or 8 bytes)**: pointer to the class's metadata in Metaspace. With compressed class pointers (default), 4 bytes; otherwise 8 bytes.

This header means even the simplest objects have significant overhead — `new Object()` is 16 bytes, `new Integer(42)` is 16 bytes, even though an int is 4 bytes.

---

## Q2: What are Compressed OOPs and why do they matter?

**Answer:**

On 64-bit JVMs, object pointers are naturally 64 bits (8 bytes). Compressed OOPs (Ordinary Object Pointers) store references as 32-bit values by exploiting the fact that objects are 8-byte aligned:

```text
32-bit stored value × 8 (alignment) = can address 32 GB of heap
```

Benefits:
- References: 8 bytes → 4 bytes (saves memory proportional to number of references)
- Klass pointers: 8 bytes → 4 bytes
- Applications with many objects: 20-50% memory savings

Automatically enabled for heaps up to ~32GB. Disabled for larger heaps → all references become 8 bytes again.

Practical implication: a 32GB heap often performs better than a 34GB heap because compressed OOPs are disabled above ~32GB, causing memory usage to spike and potentially reducing overall efficiency.

---

## Q3: Why does object field ordering affect memory usage?

**Answer:**

Fields must be aligned to their natural size (ints to 4-byte, longs to 8-byte boundaries). Poorly ordered fields cause wasted padding bytes:

```java
// Wasteful:
class A {
    boolean b;   // 1 byte + 3 bytes padding (before int)
    int i;       // 4 bytes
    boolean b2;  // 1 byte + 7 bytes padding (before long, for 8-byte alignment)
    long l;      // 8 bytes
    // Total field space: 24 bytes
}

// Efficient (same fields):
class B {
    long l;    // 8 bytes
    int i;     // 4 bytes
    boolean b; // 1 byte
    boolean b2;// 1 byte + 2 bytes padding
    // Total field space: 16 bytes
}
```

HotSpot JVM actually reorders fields to minimize padding (longs/doubles first, then ints, then shorts, then bytes, then references). But knowing this matters when using JOL to analyze memory usage or designing data-intensive classes.

---

## Q4: How does the identity hash code relate to GC?

**Answer:**

`System.identityHashCode(obj)` returns the object's identity hash — unique to that object instance, not its content.

The hash code is stored in the **mark word** after its first computation. When GC moves an object (copying collector), the mark word moves with the object — preserving the hash code.

Important property: the identity hash code is stable across the object's lifetime, even if GC moves it many times. This is why `IdentityHashMap` and other identity-based structures work correctly despite GC.

Biased locking interaction: once an identity hash code is computed for an object, it occupies space in the mark word that biased locking also needs → the object cannot use biased locking. This is a subtle JVM detail.

---

## Q5: How does lock inflation work in Java's synchronized?

**Answer:**

The JVM uses three lock modes, escalating on contention:

1. **Biased locking** (removed in Java 15): mark word stores thread ID. Lock acquisition is just a memory read — no CAS. Excellent for single-threaded access patterns.

2. **Lightweight (thin) lock**: when a second thread contends, the lock inflates to use CAS. A "lock record" is pushed on the owning thread's stack; the mark word points to it. Contending threads spin briefly.

3. **Heavyweight lock**: under high contention, the JVM creates an `ObjectMonitor` object. The mark word points to it. Threads that fail to acquire park (OS-level blocking). Most expensive — OS context switch involved.

Lock inflation is progressive: once a lock inflates, it generally stays at that level. This is why contended `synchronized` blocks are much more expensive than uncontended ones.
