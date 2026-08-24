# CPU Profiling — Deep Dive

CPU profiling is the process of measuring where your application spends its CPU time. It identifies **hot spots** — methods that consume the most CPU — so you can optimize them.

The core mental model is:

> **CPU profiling answers: "What is my CPU doing?" It helps you find bottlenecks, optimize hot code paths, and reduce CPU usage in production Java applications.**

---

# 1. Why CPU Profile?

Without profiling, optimization is guesswork:

```text
Developer: "I think the database query is slow."
Profiler:  "Actually, 70% of CPU time is in JSON serialization."
```

Or in production:

```text
Alert: CPU usage 95% on all pods
Guess: traffic spike? GC? database?
Profiler: method X in service Y is called 10M times/sec with O(n^2) complexity
```

CPU profiling gives data, not opinions.

---

# 2. Types of CPU Profiling

### Sampling Profiler

```text
Every N milliseconds: take a snapshot of all thread stacks
Count how many times each method appears in snapshots
Methods appearing most = most CPU time

Pros:
  Low overhead (< 1-5%)
  Safe for production use
  No code instrumentation needed

Cons:
  Not 100% accurate (statistical sampling)
  May miss short-lived methods
```

### Instrumentation Profiler

```text
Inject code at every method entry/exit
Measure exact time spent in each method

Pros:
  100% accurate timing
  Records every call

Cons:
  High overhead (10-100x)
  Changes timing behavior (observer effect)
  NOT safe for production
  Only for development/testing
```

For production: use **sampling profiler**.
For development: can use instrumentation profiler for accuracy.

---

# 3. What Does a CPU Profile Show?

A CPU profile shows:

```text
Method             | CPU Time  | % of Total | Call Count
----------------------------------------------------------
OrderService.process()    | 2500ms | 45%     | 150,000
JsonSerializer.write()    | 1800ms | 32%     | 450,000
ProductRepository.find()  | 800ms  | 14%     | 150,000
OrderValidator.validate() | 500ms  | 9%      | 150,000
```

Or as a flame graph (more useful):

```text
main (100%)
├── handleRequest (95%)
│   ├── processOrder (60%)
│   │   ├── validateOrder (20%)
│   │   ├── fetchProduct (25%)
│   │   │   └── SQL query (20%)
│   │   └── calculatePrice (15%)
│   └── serializeResponse (35%)
│       └── Jackson.writeValue (35%)
└── logging (5%)
```

The widest bars in the flame graph = most CPU time.

---

# 4. Flame Graphs

Flame graphs are the most effective CPU profiling visualization.

```text
How to read a flame graph:
- X-axis: CPU time (not time)
  Wider = more CPU consumed
- Y-axis: call stack (bottom = entry point, top = deepest call)
- Each bar = one method on the call stack

cpu-profile.svg:
                 [json serialize]
         [mapper.toResponse()]
    [OrderService.process()]
[HttpServlet.service()]
[main]
```

The goal: find wide bars near the top (deepest methods consuming most CPU).

---

# 5. Tools for CPU Profiling

### async-profiler (Best for Production)

```bash
# Attach to running JVM
./profiler.sh -d 30 -f profile.html <pid>

# Or start with agent
java -agentpath:/path/to/libasyncProfiler.so=start,event=cpu,file=profile.html -jar app.jar
```

- Sampling profiler using `AsyncGetCallTrace` JVM API
- Very low overhead (1-3%)
- Safe for production
- Produces flame graphs
- Avoids safepoint bias (explained below)

### JDK Mission Control + Java Flight Recorder

```bash
# Start with JFR
java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=recording.jfr -jar app.jar

# Or attach to running JVM
jcmd <pid> JFR.start duration=60s filename=recording.jfr
```

- Built into JDK (no extra download)
- Low overhead sampling
- Rich data: CPU, GC, threads, heap, IO all in one recording
- Analyze with JDK Mission Control GUI

### VisualVM

```bash
# Start VisualVM (included in JDK)
jvisualvm
```

- Visual GUI for profiling local/remote JVMs
- Sampling and instrumentation modes
- CPU + memory profiling
- Good for development environments

### JProfiler / YourKit

- Commercial tools
- Most feature-rich
- Best for complex profiling scenarios
- Instrumentation + sampling modes

---

# 6. Safepoint Bias — Why it Matters

A subtle problem with naive sampling profilers:

```text
JVM can only take thread stack snapshots at "safepoints"
Safepoints are fixed points in JVM bytecode execution

Problem: some code paths have few safepoints
→ That code appears less in samples than it actually runs
→ Profile is BIASED toward safepoint-heavy code

Example:
  Actually 40% CPU in tight numeric loop
  Profiler shows: 10% (loop has few safepoints)
```

`async-profiler` uses a different mechanism (`AsyncGetCallTrace`) to sample at ANY point, not just safepoints — giving accurate profiles.

Tools with safepoint bias: VisualVM, JDK sampling in some modes.
Tools without: `async-profiler`, YourKit (with specific settings).

---

# 7. Reading CPU Profile Output — Example

Suppose a Spring Boot REST service has high CPU:

```text
Top methods (sampling, 30 seconds):

 60% com.fasterxml.jackson.databind.ser.BeanSerializer.serialize()
 15% com.example.ProductService.buildProductTree()
 10% java.util.HashMap.getNode()
  8% org.hibernate.collection.internal.PersistentBag.iterator()
  7% other
```

Analysis:

```text
60% CPU in Jackson serialization → investigate why
  - Are we serializing huge objects?
  - Are we using slow reflection-based serialization?
  - Could we cache serialization results?
  - Could we use a faster serializer?

15% in buildProductTree() → O(n^2) algorithm?
  - Profile deeper: what's inside buildProductTree()?
  - Maybe building tree from flat list in O(n^2) instead of O(n)?

10% in HashMap.getNode() → many map lookups?
  - Which map? Is there a better data structure?
```

---

# 8. CPU vs Wall-Clock Profiling

Two different questions:

```text
CPU time:      How much CPU did this method use?
Wall-clock time: How long did this method take (including waits)?
```

Example:

```java
public Order getOrder(String id) {
    // This call waits 200ms for DB response
    return repository.findById(id);  // wall-clock: 200ms, CPU: ~1ms
}
```

CPU profiler shows 1ms for `findById` (it's waiting, not computing).
Wall-clock profiler shows 200ms (includes waiting time).

For I/O bottlenecks → use wall-clock profiling.
For CPU bottlenecks → use CPU profiling.

```bash
# async-profiler: CPU mode
./profiler.sh -e cpu -d 30 <pid>

# async-profiler: wall-clock mode (includes waiting time)
./profiler.sh -e wall -d 30 <pid>
```

---

# 9. Common CPU Hot Spots in Java Apps

### 1. JSON Serialization/Deserialization

```text
Jackson ObjectMapper is slow with complex graphs
Fix: use streaming API, pre-configure ObjectMapper as singleton,
     use faster serializer (jsonb, protobuf)
```

### 2. String Operations

```java
// Slow: string concatenation in loop
String result = "";
for (int i = 0; i < 10000; i++) {
    result += "item " + i;  // O(n^2)!
}

// Fast: StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sb.append("item ").append(i);
}
```

### 3. Reflection

```java
// Slow: reflection in hot path
Method method = clazz.getMethod("getName");
method.invoke(obj);  // much slower than direct call

// Fast: call directly, or cache Method, or use MethodHandle
```

### 4. Database Query in Loop (N+1 Problem)

```java
// SLOW: N+1 queries
for (Order order : orders) {
    User user = userService.findById(order.getUserId());  // N queries!
}

// FAST: batch fetch
List<User> users = userService.findByIds(
    orders.stream().map(Order::getUserId).collect(toList())
);
```

### 5. Inefficient Data Structures

```java
// Slow: LinkedList for random access
List<Item> list = new LinkedList<>();
item = list.get(5000);  // O(n)

// Fast: ArrayList
List<Item> list = new ArrayList<>();
item = list.get(5000);  // O(1)
```

### 6. GC Pressure (Allocation Rate)

High allocation rate → frequent Minor GC → CPU spent in GC.

```bash
# Check allocation rate with async-profiler
./profiler.sh -e alloc -d 30 -f alloc.html <pid>
```

---

# 10. Real-life: Profiling a Slow API

```text
Problem: /api/reports endpoint takes 3 seconds (SLA: 500ms)

Step 1: Take CPU profile
  ./profiler.sh -e cpu -d 30 <pid>

Step 2: Open flame graph
  Widest bar: 2.1 seconds in ReportService.generateReport()
  Inside: 1.8 seconds in ProductHierarchy.buildTree()

Step 3: Look at buildTree():
```

```java
// PROBLEM: O(n^2) algorithm
List<Product> buildTree(List<Product> products) {
    List<Product> roots = new ArrayList<>();
    for (Product p : products) {        // O(n)
        Product parent = null;
        for (Product candidate : products) {  // O(n) INSIDE O(n) = O(n^2)
            if (candidate.getId().equals(p.getParentId())) {
                parent = candidate;
                break;
            }
        }
        if (parent == null) roots.add(p);
    }
    return roots;
}
```

```text
With 1000 products: 1000 * 1000 = 1,000,000 comparisons

Step 4: Fix with O(n) algorithm using a Map:
```

```java
List<Product> buildTree(List<Product> products) {
    Map<String, Product> index = products.stream()
        .collect(toMap(Product::getId, identity()));  // O(n) to build

    return products.stream()
        .filter(p -> !index.containsKey(p.getParentId()))  // O(1) per lookup
        .collect(toList());
}
```

```text
With 1000 products: 1000 operations (O(n))
Result: 3 seconds → 50ms
```

---

# 11. Continuous Profiling in Production

Modern approach: always-on low-overhead profiling in production.

```text
Tools:
  async-profiler (< 1% overhead)
  Java Flight Recorder (continuous recording)
  Datadog Continuous Profiler
  Pyroscope (open source)
  Grafana Pyroscope
```

```text
Benefits:
  No need to reproduce issues in dev — data from actual production traffic
  Find hotspots under real load patterns
  Compare before/after deployments
  Alert on CPU hotspot changes
```

---

# Interview Preparation — CPU Profiling

---

## Q1: What is CPU profiling and when do you use it?

**Answer:**

CPU profiling measures where an application spends its CPU time, identifying the hottest (most time-consuming) code paths.

Use it when:
- API response time is slow and you don't know why
- CPU usage is unexpectedly high
- You want to optimize performance but don't know where to focus
- Before/after performance optimization to verify improvement

The profiler answers: "What is the CPU doing?" — replacing guesswork with data.

---

## Q2: What is the difference between sampling and instrumentation profiling?

**Answer:**

| | Sampling | Instrumentation |
|---|---|---|
| Mechanism | Periodic stack snapshots | Code injected at every method |
| Accuracy | Statistical (approximate) | Exact |
| Overhead | Low (1-5%) | High (10-100x) |
| Production safe | Yes | No |
| Use case | Production profiling | Development, precise benchmarking |

Sampling profilers periodically capture thread stacks and count appearances — methods that appear most frequently consume most CPU. Instrumentation profilers measure every method call's exact duration but change the program's behavior significantly.

---

## Q3: What is a flame graph and how do you read it?

**Answer:**

A flame graph visualizes profiling data as stacked bars representing call stacks.

```text
X-axis: CPU time (wider = more CPU)
Y-axis: call stack depth (bottom = entry, top = deepest call)
Each row: a stack frame (method)
```

How to read:
1. Find the WIDEST bars (most CPU time)
2. Look at the TOP of wide columns (the deepest method = the actual work)
3. The method at the top of a wide column = optimization target

Tools: `async-profiler -f profile.html` generates interactive SVG flame graphs.

---

## Q4: What is safepoint bias and why does it matter?

**Answer:**

Most JVM sampling profilers can only capture thread stacks at JVM "safepoints" — specific points in bytecode where all threads can safely be paused.

Problem: some code has few safepoints (tight loops, native code). This code is underrepresented in samples — you see 10% in the profile even though it's using 40% of CPU. This is safepoint bias.

`async-profiler` uses `AsyncGetCallTrace` to sample at ANY point, not just safepoints — giving unbiased, accurate profiles. This is why `async-profiler` is preferred over JVM's built-in sampling.

---

## Q5: How do you find and fix CPU hotspots in a Java microservice?

**Answer:**

1. **Profile with async-profiler**: `./profiler.sh -e cpu -d 30 -f profile.html <pid>`
2. **Open flame graph**: identify widest bars
3. **Find the root cause**:
   - Wide bar in serialization → too much data, wrong format, missing caching
   - Wide bar in algorithm → check complexity (O(n^2) vs O(n))
   - Wide bar in HashMap → many hash collisions? Wrong capacity?
   - Wide bar in GC → high allocation rate, reduce object creation
4. **Fix** the root cause (not the symptom)
5. **Verify**: profile again after fix — measure improvement

---

## Q6: What is the difference between CPU time and wall-clock time in profiling?

**Answer:**

**CPU time**: actual CPU cycles consumed by the method (excludes waiting).

**Wall-clock time**: total elapsed time for the method (includes waiting for I/O, locks, sleep).

Example:
```java
Order order = repository.findById(id);  // waits 200ms for DB
```
- CPU time: ~1ms (just JVM overhead)
- Wall-clock time: 201ms

When to use each:
- CPU profiling (`-e cpu`): find CPU-hungry code (algorithms, serialization)
- Wall-clock profiling (`-e wall`): find slow code including I/O waits, lock contention
- Use both together for complete picture

---

## Q7: What are common CPU hotspots in Spring Boot microservices?

**Answer:**

1. **Jackson serialization**: complex object graphs, reflection-heavy; fix by caching ObjectMapper (singleton), using Jackson Streaming API, or switching format
2. **N+1 query problem**: 1 query to get N orders + N queries to get their users; fix with JOIN fetch or batch loading
3. **String concatenation in loops**: `result += item` is O(n^2); fix with `StringBuilder`
4. **Reflection in hot paths**: Spring AOP proxies, bean property access; fix by caching `Method` objects or using `MethodHandle`
5. **Inefficient algorithms**: O(n^2) inside request handlers; fix with proper data structures
6. **High GC pressure**: excessive object creation causing frequent Minor GC; profile allocation with `async-profiler -e alloc`
