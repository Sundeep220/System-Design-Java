# Heap Dumps — Deep Dive

A heap dump is a snapshot of all objects in the JVM heap at a specific point in time. It records every live object, its type, size, field values, and the references between objects.

The core mental model is:

> **A heap dump is an X-ray of your application's memory. It shows exactly what objects exist, how much memory they consume, who is keeping them alive, and — critically — why they cannot be garbage collected.**

---

# 1. What is a Heap Dump?

```text
JVM Heap (running state):
  Order[1]: id=1, userId=42, status=PENDING, items=[...]
  Order[2]: id=2, userId=17, status=COMPLETE, items=[...]
  User[1]:  id=42, name="Alice"
  HashMap[1]: 2.4M entries
  byte[][1]: 100MB buffer
  ...

Heap dump = all of the above captured to a file (HPROF format)
```

The heap dump file can be:

```text
Small app: 50-200MB
Typical microservice: 1-4GB
Large service: 10-50GB+
```

---

# 2. When Do You Need a Heap Dump?

```text
1. OutOfMemoryError: Java heap space
   → What is consuming all memory?

2. Steadily growing memory usage
   → Is there a memory leak?

3. Too many Full GCs
   → What is filling Old Generation?

4. Production alert: Heap > 85%
   → Investigate before OOM hits

5. Unexpected memory increase after deployment
   → What did the new code add to memory?
```

---

# 3. How to Take a Heap Dump

### Method 1: Automatic on OOM (Best Practice)

Always configure this in production:

```bash
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/java/heapdump.hprof \
     -jar app.jar
```

The JVM automatically creates the dump just before dying.

### Method 2: jmap (Manual)

```bash
# Dump only live objects (recommended — smaller file, no dead objects)
jmap -dump:live,format=b,file=heap.hprof <pid>

# Dump all objects including unreachable
jmap -dump:format=b,file=heap_all.hprof <pid>
```

Note: `jmap` causes a Stop-The-World pause while generating the dump. On large heaps, this can be seconds. Use on production only if absolutely necessary.

### Method 3: jcmd

```bash
jcmd <pid> GC.heap_dump /var/log/heap.hprof
```

### Method 4: Java Flight Recorder

```bash
# During JFR recording, heap dump can be triggered via JMC
# Or programmatically via JFR events
```

### Method 5: From Code (for testing)

```java
com.sun.management.HotSpotDiagnosticMXBean mxBean =
    ManagementFactory.newPlatformMXBeanProxy(
        ManagementFactory.getPlatformMBeanServer(),
        "com.sun.management:type=HotSpotDiagnostic",
        com.sun.management.HotSpotDiagnosticMXBean.class
    );
mxBean.dumpHeap("/tmp/heap.hprof", true);
```

---

# 4. Tools to Analyze Heap Dumps

### Eclipse MAT (Memory Analyzer Tool) — Best

```text
Free, open-source
Purpose-built for heap dump analysis
Features:
  - Leak Suspects report
  - Dominator tree
  - Retained heap
  - GC root path
  - Object query language (OQL)
  - Compare two heap dumps

Download: https://eclipse.dev/mat/
```

### JDK Mission Control (JMC)

```text
Built into JDK
Heap dump analysis + JFR recording analysis
Good all-in-one tool
```

### VisualVM

```text
Built into JDK (or downloadable)
Simple GUI for heap dump analysis
Good for basic investigation
```

### JProfiler / YourKit (Commercial)

```text
Most powerful features
Remote heap dump analysis
Object tracking over time
```

---

# 5. Key Concepts in Heap Analysis

## 5.1 Shallow Size vs Retained Size

This is the most important distinction in heap analysis.

```text
Shallow Size:
  Memory directly occupied by the object itself
  Only the object's own fields
  NOT including referenced objects

Retained Size:
  Total memory that would be freed if this object were GC'd
  = object itself + all objects reachable ONLY through this object
```

Example:

```text
HashMap object:
  Shallow size: 48 bytes (HashMap fields: table, size, loadFactor)
  Retained size: 1.2 GB (HashMap + all Entry objects + all keys + all values)
```

In MAT:

```text
When investigating memory:
  Sort by RETAINED size (not shallow size)
  The object with largest retained size is your suspect
```

Visualization:

```text
Object graph:
  Config (16 bytes) → HashMap → 4M entries → User objects → ...

Config shallow:  16 bytes
Config retained: 1.5 GB  ← fix this
```

---

## 5.2 Dominator Tree

The dominator tree shows which objects are responsible for keeping other objects alive.

```text
Object A dominates Object B
  means: every path from GC root to B goes through A
  means: if A were removed, B would be garbage

Dominator Tree:
  GC Root
    └── OrderService (retains 800MB)
          └── orderCache: HashMap (retains 799MB)
                └── Map.Entry[1] (retains 200bytes)
                └── Map.Entry[2] (retains 200bytes)
                ...
                └── Map.Entry[4M] (retains 200bytes)
```

Finding the dominator of a large memory block = finding the root cause.

---

## 5.3 GC Root Path

Every live object has a path from a GC root. Finding this path answers:

> "Why can't GC collect this object?"

In MAT: right-click object → "Path To GC Roots"

```text
GC Root: static field ProductService.productCache
  → HashMap
  → HashMap$Entry
  → Product object  ← why this isn't collected
```

This is the chain you need to break to fix the memory leak.

---

# 6. Eclipse MAT Workflow

### Step 1: Open heap dump

```text
File → Open Heap Dump → select .hprof file
MAT parses the file (may take minutes for large dumps)
```

### Step 2: Run Leak Suspects Report

```text
Overview → Leak Suspects
MAT automatically identifies likely memory leaks
Shows: problem objects, their retained size, class
```

Example output:

```text
Problem 1: One instance of HashMap occupying 1.2 GB
  com.example.ProductService.productCache (static field)
  4,213,456 entries

Recommendation: Check if this cache has size limits
```

### Step 3: Examine Dominator Tree

```text
Window → Heap Dump Details → Dominator Tree
Sort by Retained Heap
```

### Step 4: Find GC Root Path

```text
Select the large object
Right-click → Path To GC Roots → exclude weak/soft references
```

### Step 5: Examine Object Details

```text
See actual field values
Count instances: how many User objects exist?
What HashMap has 4M entries?
```

---

# 7. OQL — Object Query Language

MAT supports SQL-like queries over the heap:

```sql
-- Find all String objects larger than 1MB
SELECT * FROM java.lang.String s WHERE s.value.@length > 1048576

-- Count instances of a class
SELECT COUNT(*) FROM com.example.Order

-- Find all instances with a specific field value
SELECT * FROM com.example.Order o WHERE o.status = "PENDING"

-- Find objects with large arrays
SELECT * FROM int[] a WHERE a.@length > 10000
```

Extremely powerful for targeted investigation.

---

# 8. Comparing Two Heap Dumps

Useful for finding memory leaks over time:

```bash
# Take dump 1 (baseline)
jmap -dump:live,format=b,file=heap_before.hprof <pid>

# Wait 1 hour

# Take dump 2
jmap -dump:live,format=b,file=heap_after.hprof <pid>

# In MAT: compare the two
# Window → Compare Baskets → add both dumps
# Shows: which classes GREW between the two snapshots
```

Example comparison output:

```text
Class                     | Count before | Count after | Delta | Retained delta
com.example.Order         |     50,000   |    450,000  | +400K | +2.4 GB  ← LEAK!
com.example.ProductCache  |      5,000   |      5,001  |    +1 | normal
java.lang.String          |    200,000   |    210,000  | +10K  | normal
```

The `Order` class grew by 400K instances = memory leak in Order caching.

---

# 9. Real-life: Memory Leak Investigation

```text
Alert: Heap usage crossed 80% and growing
       Minor GC every 5 seconds
       Full GC every 20 minutes
       Old Gen usage after each GC: growing
```

```bash
Step 1: Take heap dump
  jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>

Step 2: Copy to analyst machine (not on prod server!)
  scp prod-server:/tmp/heap.hprof ./

Step 3: Open in Eclipse MAT
  Leak Suspects report runs automatically
```

Report shows:

```text
Problem 1:
  1 instance of java.util.HashMap@0x12345
  Retained Heap: 2.8 GB
  
  This instance is referenced by:
  com.example.ReportService.reportCache (static field)
```

Dominator tree:

```text
ReportService.reportCache (static) → HashMap (2.8 GB)
  → Map$Entry (18,000,000 entries!)
    → ReportData objects
      → byte[] (raw report data, 150KB each)
```

Root cause:

```java
@Service
public class ReportService {
    // LEAK: no eviction, grows forever
    private static final Map<String, ReportData> reportCache = new HashMap<>();

    public ReportData generateReport(String reportId) {
        return reportCache.computeIfAbsent(reportId, this::buildReport);
    }
}
```

Every unique report ID is cached forever. With 18M unique report IDs → 2.8GB.

Fix:

```java
private final Cache<String, ReportData> reportCache = Caffeine.newBuilder()
    .maximumSize(10_000)          // max 10K reports
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build();
```

---

# 10. Heap Dump in Kubernetes

In Kubernetes, taking a heap dump from a crashed pod is tricky:

```bash
# Before crash: enable automatic heap dump
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/dumps/  # must be a mounted volume!

# Mount a persistent volume or emptyDir:
volumes:
  - name: heap-dumps
    emptyDir: {}
volumeMounts:
  - name: heap-dumps
    mountPath: /dumps

# After OOM: copy dump before pod restarts
kubectl cp <pod>:/dumps/heapdump.hprof ./heapdump.hprof
```

Or use a sidecar to automatically upload heap dumps to S3/blob storage:

```text
Init container: set up dump directory
Main container: JVM with heap dump path
Sidecar: watch for new .hprof files → upload to S3
```

---

# 11. Heap Dump vs Thread Dump — When to Use Which

| Tool | Use For | Shows |
|---|---|---|
| Thread Dump | Hangs, deadlocks, high CPU threads, thread starvation | Thread state, stack traces, lock info |
| Heap Dump | Memory leaks, OOM, growing memory, object counts | Object instances, sizes, references, GC roots |

Both together:

```text
High CPU + slow responses → Thread dump (what are threads doing?)
Growing memory + OOM → Heap dump (what is consuming memory?)
Unknown issue → both (comprehensive picture)
```

---

# Interview Preparation — Heap Dumps

---

## Q1: What is a heap dump and what information does it contain?

**Answer:**

A heap dump is a binary snapshot of the JVM heap at a specific point in time. It contains:

- Every object instance in the heap (class type, size, field values)
- Reference relationships between objects (who references what)
- GC root information (what keeps objects alive)
- Class metadata (class names, field names)

Format: HPROF binary format (`.hprof` files).

Used to diagnose: memory leaks, OutOfMemoryError, unexpected memory growth, what's occupying memory.

---

## Q2: What is the difference between shallow size and retained size?

**Answer:**

**Shallow size**: memory directly occupied by the object itself — just its own fields, not the objects it references.

**Retained size**: total memory that would be freed if this object became unreachable — the object itself plus all objects reachable ONLY through it (i.e., removing this object would allow them to be GC'd).

Example:
```text
HashMap object: shallow = 48 bytes, retained = 1.5 GB
                         ↑ just HashMap fields   ↑ HashMap + all entries + all keys + values
```

For memory leak investigation, always focus on **retained size** — that's what you'll free when you fix the problem.

---

## Q3: How do you take a heap dump in production safely?

**Answer:**

Best practice — configure before OOM happens:
```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/dumps/heap.hprof
```

Manual dump (causes STW pause!):
```bash
jmap -dump:live,format=b,file=heap.hprof <pid>
# or
jcmd <pid> GC.heap_dump /tmp/heap.hprof
```

Safety considerations:
- Use `live` option to exclude dead objects (smaller file)
- Takes a STW pause proportional to heap size — be careful on production
- Copy the `.hprof` file off the production server before analyzing
- For very large heaps (> 16GB), use JFR instead to avoid long pauses

---

## Q4: How do you find a memory leak using Eclipse MAT?

**Answer:**

1. Open `.hprof` file in Eclipse MAT
2. Run "Leak Suspects" report (automatic analysis)
3. Check Dominator Tree (Window → Heap Dump Details → Dominator Tree) — sort by Retained Heap
4. Find the large object — identify its class and size
5. Right-click → "Path To GC Roots" → exclude weak/soft references
6. This shows the reference chain: GC root → ... → large objects
7. Identify the root (usually a static field or singleton bean)
8. Fix: add eviction, fix lifecycle, fix the design

---

## Q5: What is the dominator tree?

**Answer:**

The dominator tree is a hierarchical view where each node dominates all objects below it — meaning every GC root path to those lower objects must go through the dominator.

If object A dominates a set of objects, then removing A from the heap would allow all those objects to be garbage collected.

```text
ServiceCache (dominates 1.2GB)
  └── HashMap (dominates 1.2GB)
        └── Entry × 4M (dominates 300bytes each)
              └── Product × 4M (dominates 300bytes each)
```

This tells you: fix the leak in `ServiceCache` → free 1.2GB.

---

## Q6: How do you use OQL to query a heap dump?

**Answer:**

OQL (Object Query Language) in Eclipse MAT is SQL-like:

```sql
-- Count all Order instances
SELECT COUNT(*) FROM com.example.Order

-- Find orders in PENDING state
SELECT * FROM com.example.Order o WHERE o.status = "PENDING"

-- Find strings larger than 100KB
SELECT * FROM java.lang.String s WHERE s.value.@length > 102400

-- Find the largest arrays
SELECT * FROM int[] a ORDER BY a.@length DESC LIMIT 10
```

Useful for targeted investigation: "How many Order objects exist? What state are they in? Which are huge?"

---

## Q7: How do you handle heap dumps in Kubernetes environments?

**Answer:**

Challenge: pods restart after OOM — dump might be lost.

Solution:
1. Mount a persistent volume (or emptyDir with large enough allocation) at `/dumps/`
2. Configure JVM: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/`
3. Either:
   - Copy dump before restart: `kubectl cp <pod>:/dumps/heap.hprof ./heap.hprof`
   - Add a sidecar container that watches `/dumps/` and uploads to S3/GCS/Azure Blob
   - Use `lifecycle.preStop` hook to upload the file before pod terminates

Modern approach: use `-XX:+ExitOnOutOfMemoryError` to ensure the pod exits cleanly after dump, then Kubernetes restarts it (new pod) while the old dump persists in the volume.
