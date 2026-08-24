# GC Pauses — Deep Dive

GC pauses are moments when the JVM stops all application threads to perform garbage collection. They are directly visible to users as latency spikes and timeouts.

> **Every GC pause is time when your application is not serving requests.**

---

# 1. What is a GC Pause?

```text
Normal execution:
App: [request][request][request][request]

With GC pause:
App: [request][request][PAUSE 200ms][request]
                         ↑ all threads stopped, no requests served
```

From user perspective: a 50ms endpoint suddenly takes 250ms.

---

# 2. Why Stop-The-World?

GC needs a consistent snapshot of the object graph. If the app runs while GC marks:

```text
GC: marks A → references B
App: A.ref = null, C.ref = B  (reference moved!)
GC: never traces B via C → incorrectly collects B!
```

STW prevents this. Modern GCs (ZGC, Shenandoah) use barriers to reduce STW to milliseconds.

---

# 3. Types of GC Pauses

| Type | Trigger | Duration | Frequency | Impact |
|---|---|---|---|---|
| Minor GC | Eden full | 5-50ms | Frequent (seconds) | Barely noticeable |
| Major GC | Old Gen filling | 50ms-500ms | Infrequent | Visible latency spike |
| Full GC | Promotion failure, OOM | Seconds-minutes | Should be rare | App appears frozen |

---

# 4. G1GC Pause Phases

```text
G1 Young GC STW breakdown:
  Root Processing       (scan GC roots)
  Object Copy           (evacuate live objects — parallel, longest)
  Reference Processing  (soft/weak/phantom refs)
  Clear Card Table      (update remembered sets)
```

The dominant cost is **Object Copy** — copying live objects to new regions.

---

# 5. GC Logging

```bash
# Java 9+
-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=20m
```

Sample output:

```text
GC(42) Pause Young (Normal) 512M->256M(2048M) 45ms
GC(43) Pause Young (Normal) 512M->260M(2048M) 43ms
GC(44) Pause Young (Mixed)  512M->200M(2048M) 180ms   <- Mixed GC (Old included)
GC(45) Pause Full (Ergonomics) 1800M->600M(2048M) 3200ms  <- PROBLEM: Full GC
```

---

# 6. What Causes Long GC Pauses?

### 1. Large Live Set in Young Gen

```text
Too many live objects to copy → copy takes long
Fix: increase Survivor ratio, reduce object lifetime
```

### 2. Old Gen Too Full

```text
G1 Mixed GC must collect many Old regions
Fix: tune InitiatingHeapOccupancyPercent, increase heap
```

### 3. Full GC (Worst Case)

```text
Promotion failure or OOM → entire heap STW compaction
Fix: fix memory leak, size heap correctly
```

### 4. Reference Processing

```text
Many SoftReferences, WeakReferences, Finalizers
→ Reference queue processing during pause
Fix: minimize use of finalizers, use Cleaner API
```

### 5. Humongous Object Allocation

```text
Large objects allocated frequently → trigger G1 marking
Fix: reuse buffers, avoid large allocations in hot paths
```

---

# 7. Real-life Visualization: E-commerce App

```text
Timeline of a production Spring Boot app:

10:00  Minor GC  45ms  → normal
10:02  Minor GC  48ms  → normal
10:05  Minor GC  50ms  → normal
10:10  Mixed GC  160ms → G1 collecting some Old Gen
10:15  Minor GC  52ms  → normal
10:30  Full GC   3200ms → ALERT! All requests queued/timeout
       ↑ memory leak: cache growing without bound
```

Users see: random 3-second freezes every 15-30 minutes.

---

# 8. ZGC vs G1 Pause Comparison

```text
Parallel GC: [==================Full GC 5sec==================]

G1GC:        [50ms][concurrent work][50ms][concurrent work][200ms Mixed GC]

ZGC:         [1ms][concurrent work][1ms][concurrent work][1ms]
             ↑ sub-millisecond pauses even on 100GB heaps
```

---

# 9. Reducing GC Pauses

```text
Strategy 1: Switch to low-latency GC
  -XX:+UseZGC           # < 1ms pauses
  -XX:+UseShenandoahGC  # < 10ms pauses

Strategy 2: Right-size the heap
  -Xmx appropriately   # not too small (frequent GC) or too large (long GC)

Strategy 3: Reduce allocation rate
  Object pooling
  Reuse buffers
  Reduce intermediate objects

Strategy 4: Fix memory leaks
  Heap dump analysis → remove unbounded caches

Strategy 5: Tune G1 pause target
  -XX:MaxGCPauseMillis=200   # G1 will try to respect this

Strategy 6: Tune IHOP
  -XX:InitiatingHeapOccupancyPercent=35  # start concurrent marking earlier
```

---

# 10. GC Pause Metrics to Monitor

```text
p50 GC pause:  median pause — should be 10-50ms
p99 GC pause:  99th percentile — watch for spikes
p999 GC pause: 99.9th percentile — worst case tail latency
Max GC pause:  absolute worst — Full GC will show here

GC throughput: % of time app is running (target > 95%)
Full GC count: should be 0 in a healthy app
```

Tools: Prometheus + JVM micrometer metrics, GC Easy (gceasy.io), JDK Mission Control.

---

# Interview Preparation — GC Pauses

---

## Q1: What is a Stop-The-World pause and why does it happen?

**Answer:**

Stop-The-World (STW) is a pause where all application threads are suspended while the GC performs work that requires a consistent view of the heap.

Why needed: GC must trace the object graph to find live objects. If application threads run concurrently, they can change references (add/remove) while GC is tracing — leading to incorrect results (collecting live objects or missing garbage).

Modern GCs minimize STW by doing most work concurrently using write/load barriers to track reference changes. ZGC achieves < 1ms STW by doing almost all work concurrently.

---

## Q2: How do you diagnose GC pause problems in production?

**Answer:**

1. **Enable GC logging**: `-Xlog:gc*:file=gc.log:time,uptime`
2. **Analyze with tools**: GC Easy (gceasy.io), JDK Mission Control
3. **Look for patterns**:
   - Frequent Full GC → memory leak or heap too small
   - Long Mixed GC → Old Gen too full, tune IHOP
   - Long Minor GC → Survivor too small, many live objects in Young Gen
4. **Monitor metrics**: GC pause p99, GC throughput (%), Full GC count
5. **Heap dump**: if memory leak suspected — take heap dump, analyze with Eclipse MAT

---

## Q3: What is the difference between concurrent GC work and STW pauses?

**Answer:**

Concurrent work runs while the application is also running — no pause, but uses CPU:

```text
App: [running][running][running][running]
GC:       [concurrent mark =======]  <- GC works, app also works
```

STW pauses stop the application completely:

```text
App: [running][STOPPED][running]
GC:           [STW work]
```

G1GC: concurrent marking + STW evacuation pauses
ZGC: concurrent marking + concurrent relocation, only tiny STW for root scanning

---

## Q4: What GC flags would you use for a latency-sensitive microservice?

**Answer:**

```bash
# Use ZGC for sub-millisecond pauses
-XX:+UseZGC

# Or G1GC with aggressive pause target
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100   # target 100ms max pause

# Start concurrent marking earlier (prevent promotion failure)
-XX:InitiatingHeapOccupancyPercent=35

# Size heap with headroom
-Xms2g -Xmx4g   # not too tight

# GC logging (always in production)
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=20m
```

---

## Q5: What is "GC overhead limit exceeded" and how do you fix it?

**Answer:**

JVM throws `OutOfMemoryError: GC overhead limit exceeded` when:
- GC is spending > 98% of execution time
- Recovering less than 2% of heap per cycle
- This continues for 5 consecutive GC cycles

The app is essentially stuck in an infinite GC loop, making no progress.

Root causes: memory leak (heap full of reachable-but-unneeded objects), heap too small.

Fix:
1. Take a heap dump and find what's consuming memory
2. Fix the memory leak
3. Increase `-Xmx` as a temporary measure
4. Disable the check (not recommended): `-XX:-UseGCOverheadLimit`

---

## Q6: How does GC pause duration correlate with heap size?

**Answer:**

For mark-sweep-compact collectors (like Parallel GC Full GC): pause time scales with live set size — more live objects = more to mark and move.

For G1GC: pause time is controlled by `-XX:MaxGCPauseMillis`. G1 adjusts how many regions to collect per pause to meet the target. Larger heaps mean more regions, but G1 is incremental.

For ZGC: pause time is nearly constant regardless of heap size (< 1ms even on terabyte heaps) because most work is concurrent.

This is why ZGC is preferred for very large heaps — traditional GC pause times scale poorly with heap size.
