# OutOfMemoryError — Deep Dive

`OutOfMemoryError` (OOM) is a `java.lang.Error` thrown by the JVM when it cannot allocate memory for an object or resource. Unlike checked exceptions, OOM indicates a fundamental resource exhaustion — the JVM cannot continue normally.

The core mental model is:

> **OOM is not one single error. It has many distinct variants, each pointing to a different memory area and a different root cause. Diagnosing OOM correctly requires understanding which variant was thrown.**

---

# 1. OOM is an Error, not an Exception

```java
// Error hierarchy:
java.lang.Object
  └── java.lang.Throwable
        ├── java.lang.Exception  (recoverable)
        └── java.lang.Error      (usually unrecoverable)
              └── java.lang.VirtualMachineError
                    └── java.lang.OutOfMemoryError
```

```text
Should you catch OOM? Generally NO.
The JVM is in an unknown state.
Partial recovery is dangerous.
Only catch OOM to log and shutdown gracefully.
```

---

# 2. The Seven Types of OutOfMemoryError

---

## 2.1 Java Heap Space

```text
java.lang.OutOfMemoryError: Java heap space
```

The most common OOM.

**Cause**: JVM cannot allocate an object — heap is full and GC cannot reclaim enough space.

**Root causes**:
- Memory leak (objects reachable but not needed)
- Live set larger than max heap
- Sudden traffic spike allocating many objects

**Visualization**:

```text
Heap:
[========================  97% full  ========================]
                                                               ↑ new object requested here
                                                               ← OOM thrown
```

**How to trigger** (for learning):

```java
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]);  // add 1MB chunks forever
}
// → OutOfMemoryError: Java heap space
```

**Fix**:
- Find and fix memory leak (heap dump)
- Increase `-Xmx`
- Reduce live set size

---

## 2.2 GC Overhead Limit Exceeded

```text
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Cause**: GC is spending > 98% of time for 5+ consecutive GC cycles, recovering < 2% of heap.

The app is stuck in a GC loop — running GC constantly but making no progress.

**Visualization**:

```text
Timeline:
GC [====98%====][app 2%][GC====98%====][app 2%][GC====98%====]
→ OOM thrown after 5 cycles
```

**Fix**: same as heap space OOM — fix the memory leak or increase heap.
Disable (not recommended): `-XX:-UseGCOverheadLimit`

---

## 2.3 Metaspace

```text
java.lang.OutOfMemoryError: Metaspace
```

**Cause**: Metaspace (native memory for class metadata) is full.

**Root causes**:
- Too many classes loaded (frameworks generating dynamic proxies)
- ClassLoader leak — ClassLoaders keep loading classes without being GC'd
- Metaspace limit set too low (`-XX:MaxMetaspaceSize`)

**How to trigger** (for learning):

```java
// Continuously generate new classes at runtime (using ASM, CGLib, Javassist)
while (true) {
    generateNewClass();  // each iteration creates a new Class in Metaspace
}
// → OutOfMemoryError: Metaspace
```

**Visualization**:

```text
Metaspace (native memory):
[==class metadata==[==class metadata==[==class metadata==] FULL
                                                            ↑ OOM
```

**Fix**:
- Increase `-XX:MaxMetaspaceSize=512m`
- Fix ClassLoader leak
- Reduce dynamic class generation

---

## 2.4 Direct Buffer Memory

```text
java.lang.OutOfMemoryError: Direct buffer memory
```

**Cause**: Java NIO direct byte buffers (off-heap) are exhausted.

```java
ByteBuffer buf = ByteBuffer.allocateDirect(100 * 1024 * 1024);  // 100MB direct
// If too much direct memory allocated: OOM
```

**Where it's used**:
- Netty (high-performance networking framework)
- Kafka producers/consumers
- NIO file channels
- SSL/TLS handshakes

**Limit**: controlled by `-XX:MaxDirectMemorySize`.

**Fix**:
- Release direct buffers when done (`((DirectBuffer) buf).cleaner().clean()`)
- Increase `-XX:MaxDirectMemorySize=512m`
- Use pooled direct buffers (Netty's PooledByteBufAllocator)

---

## 2.5 Unable to Create New Native Thread

```text
java.lang.OutOfMemoryError: unable to create new native thread
```

**Cause**: OS cannot create a new thread. This can happen because:
- OS thread limit per process reached (`ulimit -u`)
- System-wide thread limit reached
- Each thread requires native stack memory — not enough native memory

**How to trigger** (for learning):

```java
while (true) {
    new Thread(() -> {
        try { Thread.sleep(Long.MAX_VALUE); } catch (Exception e) {}
    }).start();
    // Eventually: OutOfMemoryError: unable to create new native thread
}
```

**Visualization**:

```text
JVM process:
Thread 1 (stack ~1MB)
Thread 2 (stack ~1MB)
...
Thread 1000 (stack ~1MB)  → OS limit reached
Thread 1001 → OOM!
```

**Fix**:
- Reduce thread count (use async/reactive instead of thread-per-request)
- Use thread pools appropriately
- Increase OS thread limit (`ulimit -u 4096`)
- Reduce thread stack size (`-Xss256k`) to allow more threads
- Switch to virtual threads (Java 21+) — millions of virtual threads, few OS threads

---

## 2.6 Requested Array Size Exceeds VM Limit

```text
java.lang.OutOfMemoryError: Requested array size exceeds VM limit
```

**Cause**: Attempting to create an array larger than the JVM allows.

```java
int[] arr = new int[Integer.MAX_VALUE];  // 2^31 - 1 ints = 8GB!
// → OutOfMemoryError: Requested array size exceeds VM limit
```

The JVM limits arrays to approximately `Integer.MAX_VALUE - 5` elements.

**Fix**: algorithm error — review why such a large array is needed.

---

## 2.7 Kill Process or Subprocess

```text
# Not a JVM error message — comes from OS
java.lang.OutOfMemoryError: Kill process or subprocess
```

**Cause**: Linux OOM Killer killed the JVM process because the OS ran out of physical memory + swap.

The Linux kernel OOM killer chooses processes to kill to free memory.

```text
OS memory: 8GB RAM + 4GB swap
All processes: consuming 12GB+
Linux OOM Killer: finds JVM process → kills it
JVM: sudden death
```

**Fix**:
- Properly size JVM heap relative to OS memory
- Set container memory limits (in Kubernetes)
- Reduce memory usage of the process

---

# 3. OOM vs StackOverflowError

These are often confused.

```text
StackOverflowError:
  → Thread's stack is full
  → Too many recursive method calls
  → NOT a heap problem

OutOfMemoryError: Java heap space:
  → Heap is full
  → Too many live objects
  → NOT a stack problem
```

```java
// StackOverflowError
void recurse() { recurse(); }

// OutOfMemoryError: Java heap space
List<byte[]> list = new ArrayList<>();
while (true) { list.add(new byte[1024 * 1024]); }
```

---

# 4. Automatic Heap Dump on OOM

Always configure this in production:

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/java/heapdump.hprof
```

When OOM occurs, the JVM automatically writes a heap dump before dying.

Analyze with: Eclipse MAT, JDK Mission Control, or YourKit.

---

# 5. OOM Diagnostic Workflow

```text
Step 1: Identify the OOM variant
  Read the error message carefully!
  "Java heap space" → heap problem
  "Metaspace" → class loading problem
  "unable to create new native thread" → thread leak

Step 2: Check recent changes
  New feature deployed? New dependency added?
  Traffic spike?

Step 3: Check GC logs
  Was GC struggling before OOM? (frequent Full GC)
  Heap usage growing over time?

Step 4: Analyze heap dump (if "Java heap space")
  jmap / Eclipse MAT
  What's taking up space?
  Which GC root keeps objects alive?

Step 5: Fix root cause
  Not just "increase -Xmx" — that's a band-aid
  Fix the leak, fix the bug, fix the design
```

---

# 6. Kubernetes OOM Kill vs Java OOM

In Kubernetes containers, there are TWO types of OOM to be aware of:

### Java OOM (inside JVM)

```text
JVM detects heap full
→ throws java.lang.OutOfMemoryError
→ JVM may continue running (OOM caught somewhere)
→ Or JVM exits with OOM error
```

### Container OOM Kill (Linux kernel)

```text
Container memory limit: 2GB
JVM uses: 2.1GB (heap + metaspace + threads + direct buffers)
Linux kernel: kills JVM process (SIGKILL)
Kubernetes: pod goes into OOMKilled state
```

Container OOM Kill is harder to diagnose — no Java stack trace.

Fix: set `-Xmx` lower than container limit:

```bash
Container memory limit: 2GB
-Xmx: 1.5GB   # leaves 500MB for Metaspace, threads, JVM overhead
```

Or use JVM container awareness (Java 10+):

```bash
-XX:+UseContainerSupport           # auto-detect container limits
-XX:MaxRAMPercentage=75.0          # use 75% of container RAM for heap
```

---

# 7. Tuning to Prevent OOM

```bash
# Heap
-Xms1g -Xmx4g

# Container-aware heap
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0

# Metaspace
-XX:MaxMetaspaceSize=256m

# Direct memory
-XX:MaxDirectMemorySize=512m

# Thread stack (smaller = more threads)
-Xss256k

# Dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof

# GC logging
-Xlog:gc*:file=gc.log:time,uptime
```

---

# Interview Preparation — OutOfMemoryError

---

## Q1: What are the different types of OutOfMemoryError in Java?

**Answer:**

1. **Java heap space**: heap is full, GC cannot reclaim enough — most common
2. **GC overhead limit exceeded**: GC spends > 98% time for 5 cycles recovering < 2% — app in GC loop
3. **Metaspace**: native memory for class metadata is full — too many classes or ClassLoader leak
4. **Direct buffer memory**: NIO direct byte buffers exhausted — common with Netty/Kafka
5. **Unable to create new native thread**: OS cannot create more threads — thread leak or OS limit
6. **Requested array size exceeds VM limit**: array too large — algorithm bug
7. **Kill process** (Linux OOM killer): OS killed the JVM — container/system memory exhausted

---

## Q2: How do you diagnose OutOfMemoryError: Java heap space?

**Answer:**

1. Check GC logs: was heap usage growing? Were Full GCs increasing in frequency?
2. Take a heap dump (configure in advance: `-XX:+HeapDumpOnOutOfMemoryError`)
3. Open with Eclipse MAT → run "Leak Suspects" report
4. Identify the largest retained heap object
5. Trace GC root path: GC root → ? → large objects
6. Root cause is usually: unbounded cache, static collection, listener not removed, ThreadLocal not cleaned
7. Fix the root cause (add eviction, fix lifecycle)

---

## Q3: What is the difference between OutOfMemoryError and StackOverflowError?

**Answer:**

| | OutOfMemoryError | StackOverflowError |
|---|---|---|
| Memory area | Heap (or Metaspace, native) | Thread Stack |
| Cause | Too many live objects, allocation failure | Too many nested method calls (deep recursion) |
| Common trigger | Memory leak, heap too small | Infinite recursion, very deep call chain |
| Fix | Fix leak, increase heap | Fix recursion, increase `-Xss` |

They are completely different problems in completely different memory areas.

---

## Q4: How do you configure JVM memory correctly in a Kubernetes container?

**Answer:**

```bash
# WRONG: -Xmx equals container limit → JVM OOM + system headroom missing
Container: 2GB, -Xmx2g  # dangerous!

# CORRECT: leave room for non-heap memory
Container: 2GB, -Xmx1.5g   # 500MB for Metaspace, threads, direct buffers

# BEST: use container-aware settings
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0    # JVM auto-sizes heap to 75% of container limit
```

Also configure Metaspace and enable heap dump on OOM:
```bash
-XX:MaxMetaspaceSize=256m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/
```

---

## Q5: What causes OutOfMemoryError: unable to create new native thread?

**Answer:**

This OOM occurs when the OS cannot create a new thread for the JVM.

Causes:
- OS per-process thread limit reached (`ulimit -u`)
- System-wide thread limit reached (`/proc/sys/kernel/threads-max`)
- Not enough native memory for thread stacks (each thread needs `-Xss` stack space)

Common scenario: thread pool misconfiguration, or creating a new thread per request without bounds.

Fixes:
- Use thread pools (not unbounded `new Thread()`)
- Use async/reactive (WebFlux, virtual threads) to reduce OS thread count
- Increase OS limits: `ulimit -u 4096`
- Reduce thread stack size: `-Xss256k` (allows more threads)
- Java 21: use virtual threads (`Thread.ofVirtual()`) — millions of virtual threads on few OS threads

---

## Q6: What is a Kubernetes OOM Kill vs a Java OOM?

**Answer:**

**Java OOM** (`OutOfMemoryError`): thrown inside the JVM when a specific memory area is exhausted. The JVM may or may not survive (depends on whether it's caught and handled).

**Kubernetes OOM Kill**: the Linux kernel kills the JVM process (SIGKILL) because the container exceeded its memory limit. No Java exception is thrown. The pod enters `OOMKilled` state.

```bash
# Check if a pod was OOMKilled:
kubectl describe pod <pod-name>
# Look for: Exit Code: 137, Reason: OOMKilled
```

Prevention: set `-Xmx` well below container limit, accounting for Metaspace, thread stacks, and direct buffers. Use `-XX:MaxRAMPercentage=75` for automatic sizing.
