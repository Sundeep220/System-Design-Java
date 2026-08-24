# ConcurrentLinked Collections — Deep Dive

ConcurrentLinked collections are **non-blocking**, **lock-free** data structures that use **CAS (Compare-And-Swap)** operations instead of locks. They provide very high throughput in high-concurrency scenarios where blocking is unacceptable.

> **ConcurrentLinkedQueue and ConcurrentLinkedDeque never block. Operations return immediately — either successfully completing or indicating the queue is empty. No thread ever waits for another thread.**

---

# 1. Non-Blocking vs Blocking — The Key Distinction

```text
Blocking Queue (ArrayBlockingQueue):
  take() → queue empty → WAIT until something is added
  put()  → queue full  → WAIT until something is removed
  Thread is suspended → OS context switch → slow!

Non-Blocking Queue (ConcurrentLinkedQueue):
  poll() → queue empty → returns null immediately
  offer() → always succeeds (unbounded, no capacity limit)
  Thread NEVER waits → no OS context switch → fast!
```

Trade-off:

```text
Blocking:     Easy to use (wait naturally), built-in backpressure
Non-blocking: Harder to use (must handle null returns), caller decides what to do
              BUT: no blocking → no OS scheduling overhead → higher throughput
```

---

# 2. CAS — The Foundation of Lock-Free Collections

CAS (Compare-And-Swap) is a single atomic CPU instruction:

```text
CAS(address, expected, newValue):
  If memory[address] == expected:
    memory[address] = newValue
    return SUCCESS
  Else:
    return FAIL (someone else changed it)

This is atomic — no thread can interrupt it mid-operation.
```

How ConcurrentLinkedQueue uses CAS to add an element:

```text
Retry loop (optimistic concurrency):

1. Read current tail node
2. Try CAS: set tail.next = newNode (if tail.next is still null)
3. If CAS succeeds → done!
4. If CAS fails (another thread modified tail.next) → retry from step 1

No locks needed — just retries on contention.
Under low contention: CAS succeeds on first try
Under high contention: a few retries, but still faster than lock acquisition
```

---

# 3. ConcurrentLinkedQueue

## What it is

An unbounded, non-blocking, thread-safe FIFO queue based on the **Michael-Scott non-blocking queue algorithm**.

```text
ConcurrentLinkedQueue:

head → [Node1] → [Node2] → [Node3] → null ← tail

offer(): CAS on tail.next → atomically links new node
poll():  CAS on head → atomically advances head pointer
```

## Key Characteristics

```text
Unbounded:    YES — grows dynamically, offer() never blocks or returns false
Ordering:     FIFO (first in, first out)
Thread-safe:  YES (lock-free via CAS)
Null allowed: NO
Blocking:     NO — offer() always returns true, poll() returns null if empty
size():       O(n) — must traverse entire list (not O(1)!)
```

## Operations

```java
ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();

// Producer
queue.offer(task);   // always returns true (unbounded)
queue.add(task);     // same as offer for CLQ

// Consumer
Task task = queue.poll();   // returns null if empty (never blocks)
Task task = queue.peek();   // looks at head without removing

// Check
queue.isEmpty();     // check before poll in a spin-wait
queue.size();        // O(n) — avoid in hot paths!
```

## size() Warning

```java
// AVOID in hot paths:
if (queue.size() > 0) { ... }  // O(n) — traverses entire queue!

// PREFER:
if (!queue.isEmpty()) { ... }   // O(1) in most implementations
// or:
Task t = queue.poll();
if (t != null) { process(t); }
```

---

## ConcurrentLinkedQueue vs LinkedBlockingQueue

| | ConcurrentLinkedQueue | LinkedBlockingQueue |
|---|---|---|
| Blocking | No | Yes |
| Bounded | No | Optional |
| Backpressure | No | Yes (when bounded) |
| Null allowed | No | No |
| size() | O(n) | O(1) |
| Throughput | Higher | Lower (lock overhead) |
| Best for | High-throughput, non-blocking | Producer-consumer with backpressure |

---

## Real-life Use Cases

### Use Case 1: Work-Stealing Parallelism

```java
// Each worker has its own task queue
ConcurrentLinkedQueue<Task>[] workerQueues = new ConcurrentLinkedQueue[numWorkers];

// Workers steal tasks from each other when idle
Runnable worker = (int myId) -> {
    while (true) {
        Task task = workerQueues[myId].poll();  // check my queue
        if (task == null) {
            // Try to steal from other workers
            for (int i = 0; i < numWorkers; i++) {
                if (i != myId) {
                    task = workerQueues[i].poll();
                    if (task != null) break;
                }
            }
        }
        if (task != null) process(task);
        else Thread.yield();
    }
};
```

Non-blocking is essential here — a worker checking other queues cannot afford to block.

### Use Case 2: Event Queue in Reactive Systems

```java
// High-frequency events (sensor data, metrics) collected concurrently
ConcurrentLinkedQueue<SensorEvent> eventBuffer = new ConcurrentLinkedQueue<>();

// Multiple sensor threads producing events concurrently
sensorThread1: eventBuffer.offer(new SensorEvent("temperature", 25.5));
sensorThread2: eventBuffer.offer(new SensorEvent("humidity", 60.0));
sensorThread3: eventBuffer.offer(new SensorEvent("pressure", 1013.0));

// Single aggregation thread consuming events
Runnable aggregator = () -> {
    while (true) {
        SensorEvent event = eventBuffer.poll();
        if (event != null) {
            aggregate(event);
        } else {
            // No events right now — do other work or sleep briefly
            LockSupport.parkNanos(1_000_000);  // 1ms
        }
    }
};
```

### Use Case 3: Log Buffer (High-Throughput Logging)

```java
// Multiple request threads log concurrently
ConcurrentLinkedQueue<LogEntry> logBuffer = new ConcurrentLinkedQueue<>();

// Per-request logging (very frequent, non-blocking)
public void log(LogEntry entry) {
    logBuffer.offer(entry);  // instantly returns — request not delayed
}

// Background thread flushes logs to disk
Runnable logFlusher = () -> {
    while (true) {
        List<LogEntry> batch = new ArrayList<>();
        LogEntry entry;
        while ((entry = logBuffer.poll()) != null) {
            batch.add(entry);
            if (batch.size() >= 1000) break;  // batch max
        }
        if (!batch.isEmpty()) {
            writeToFile(batch);
        } else {
            Thread.sleep(100);  // no logs right now
        }
    }
};
```

### Use Case 4: Connection Pool Free List

```java
// Pool of reusable database connections
ConcurrentLinkedQueue<Connection> freeConnections = new ConcurrentLinkedQueue<>();

public Connection borrow() {
    Connection conn = freeConnections.poll();  // get a free connection (non-blocking)
    if (conn == null) {
        conn = createNewConnection();  // create if none available
    }
    return conn;
}

public void returnConnection(Connection conn) {
    freeConnections.offer(conn);  // return to pool
}
```

---

# 4. ConcurrentLinkedDeque

## What it is

A non-blocking, lock-free **double-ended queue** (deque). Supports adding and removing from **both ends**.

```text
ConcurrentLinkedDeque:

         addFirst / peekFirst / pollFirst
            ↓                     ↑
head → [Node1] ↔ [Node2] ↔ [Node3] → tail
                                  ↑   ↓
              addLast / peekLast / pollLast
```

## Key Operations

```java
ConcurrentLinkedDeque<Task> deque = new ConcurrentLinkedDeque<>();

// Add to both ends
deque.addFirst(task);   // add to front
deque.addLast(task);    // add to back
deque.offerFirst(task); // same as addFirst (returns boolean)
deque.offerLast(task);  // same as addLast

// Remove from both ends
deque.pollFirst();  // remove from front (null if empty)
deque.pollLast();   // remove from back (null if empty)
deque.peekFirst();  // look at front without removing
deque.peekLast();   // look at back without removing
```

## Use Cases

### Use Case 1: Work-Stealing Scheduler (Like ForkJoinPool)

```java
ConcurrentLinkedDeque<Task>[] workerDeques;

// Worker adds tasks to its OWN deque from the back (LIFO for locality)
void addTask(int workerId, Task task) {
    workerDeques[workerId].addLast(task);
}

// Worker takes from its OWN deque from the back (LIFO for cache locality)
Task takeOwnTask(int workerId) {
    return workerDeques[workerId].pollLast();  // own tasks from back
}

// Worker STEALS from others from the front (FIFO — oldest task)
Task stealTask(int victimId) {
    return workerDeques[victimId].pollFirst();  // steal from front
}
```

This is exactly how `ForkJoinPool` implements work-stealing:
- Owner takes from back (stack-like — LIFO for good cache locality)
- Stealers take from front (queue-like — FIFO to avoid conflicts with owner)

### Use Case 2: Browser History (Forward/Back Navigation)

```java
ConcurrentLinkedDeque<String> history = new ConcurrentLinkedDeque<>();

void navigate(String url) {
    history.addLast(url);  // add to end
}

String goBack() {
    history.pollLast();    // remove current
    return history.peekLast();  // see previous
}
```

### Use Case 3: Sliding Window Buffer

```java
ConcurrentLinkedDeque<Metric> window = new ConcurrentLinkedDeque<>();
int windowSize = 60;  // last 60 metrics

void addMetric(Metric m) {
    window.addLast(m);
    if (window.size() > windowSize) {
        window.pollFirst();  // remove oldest
    }
}
```

---

# 5. Spin-Wait Pattern — Common with Non-Blocking Collections

Since `poll()` returns null when empty (instead of blocking), you often see a spin-wait pattern:

```java
// BAD: 100% CPU spin — wastes CPU
while (true) {
    Task t = queue.poll();
    if (t != null) process(t);
    // else: immediately try again → burns CPU
}

// BETTER: yield or sleep briefly when empty
while (!Thread.currentThread().isInterrupted()) {
    Task t = queue.poll();
    if (t != null) {
        process(t);
    } else {
        Thread.yield();  // hint to scheduler: let others run
        // Or: LockSupport.parkNanos(1_000_000);  // 1ms sleep
    }
}

// BEST: use a blocking queue if you want to block on empty
// Or combine: try non-blocking first, then block if truly empty
Task t = queue.poll();  // try non-blocking
if (t == null) {
    t = blockingQueue.poll(100, TimeUnit.MILLISECONDS);  // then block briefly
}
```

---

# 6. When to Use Non-Blocking vs Blocking Collections

```text
Use NON-BLOCKING (ConcurrentLinkedQueue) when:
  ✓ Throughput is the primary concern
  ✓ Queue is expected to rarely be empty (producers fast)
  ✓ Caller handles empty queue gracefully (null check)
  ✓ You're building a reactive/event-driven system
  ✓ Backpressure not required
  ✓ Single or multiple consumers that check periodically

Use BLOCKING (ArrayBlockingQueue, LinkedBlockingQueue) when:
  ✓ Producers wait naturally for consumers (consumer-paced)
  ✓ Backpressure is required (bounded queue slows producers)
  ✓ Simpler code (no null checking / spin loops)
  ✓ Thread pool work queues (thread parks when no work)
  ✓ Consumer should sleep when no work (not waste CPU)
```

---

# 7. Performance Comparison

```text
Under high contention (many threads, many operations):

ConcurrentLinkedQueue:  ████████████████████████  Very high throughput
LinkedBlockingQueue:    ████████████████          High throughput (2 locks)
ArrayBlockingQueue:     ████████████              Moderate throughput (1 lock)
synchronizedQueue:      ████                      Low throughput (global lock)
```

Non-blocking wins because:
- No OS-level thread suspension/wakeup
- CAS is a single CPU instruction (much cheaper than mutex)
- No priority inversion
- No convoy effect (one slow thread blocking fast ones)

---

# Interview Preparation — ConcurrentLinked Collections

---

## Q1: How does ConcurrentLinkedQueue achieve thread-safety without locks?

**Answer:**

`ConcurrentLinkedQueue` uses the **Michael-Scott non-blocking queue algorithm** based on CAS (Compare-And-Swap) operations.

For `offer()` (add to tail):
1. Read current tail
2. CAS: try to set tail.next = newNode (only if tail.next is still null)
3. If CAS fails (another thread added a node first) → retry

For `poll()` (remove from head):
1. Read current head
2. CAS: try to advance head to head.next (only if head is still the head)
3. If CAS fails → retry

CAS is a single atomic CPU instruction that succeeds only if the memory hasn't changed. This eliminates the need for locks while ensuring consistency.

---

## Q2: When would you use ConcurrentLinkedQueue over LinkedBlockingQueue?

**Answer:**

Use `ConcurrentLinkedQueue` when:
- You don't need blocking semantics — callers handle empty queue by checking for null
- Throughput is the priority (no lock overhead)
- Queue can grow unboundedly without risk (producers don't need backpressure)
- Building reactive/event-driven systems where blocking is unacceptable

Use `LinkedBlockingQueue` when:
- Consumer threads should sleep when queue is empty (park instead of spin)
- You need bounded capacity for backpressure
- Thread pool work queues (worker threads block on empty queue)
- Simpler code: just call `take()` and let it block naturally

---

## Q3: What is work-stealing and how do deques enable it?

**Answer:**

Work-stealing is a load-balancing strategy for parallel execution: idle worker threads "steal" tasks from busy workers' queues.

`ForkJoinPool` (and `CompletableFuture`'s default pool) uses it:

- Each worker has its own `ConcurrentLinkedDeque`
- Worker pushes and pops its own tasks from the **back** (LIFO — better cache locality)
- Idle workers steal tasks from others' **front** (FIFO — oldest task, least likely to cause conflicts)

The deque enables this split: owner uses one end, stealers use the other end. This minimizes contention while maximizing throughput.

---

## Q4: Why is size() O(n) in ConcurrentLinkedQueue and why does it matter?

**Answer:**

`ConcurrentLinkedQueue` uses a linked node structure without a separate counter. To compute size, it must traverse all nodes — O(n).

Why no counter? Maintaining an atomic counter on every add/remove would add CAS overhead, reducing the lock-free performance advantage.

Why it matters:
- **Don't** use `size()` in tight loops or hot paths — it's O(n)
- **Don't** use `size() > 0` to check if non-empty — use `!isEmpty()` (faster) or just `poll()` (check for null)
- For queues where you frequently need size, use `LinkedBlockingQueue` which maintains an `AtomicInteger` count → O(1) size

---

## Q5: Explain a scenario in a microservice where ConcurrentLinkedQueue is the right choice.

**Answer:**

**High-frequency metrics collection in an observability service:**

```text
Scenario: 200 HTTP handler threads each emit metrics on every request
          (latency, status code, endpoint, etc.)
          → ~5,000 metrics/second

Single metrics aggregator thread collects and batches metrics every 100ms
→ uploads to Prometheus/Datadog
```

Why ConcurrentLinkedQueue:
- 200 threads writing concurrently → lock-free offer() = no contention
- Aggregator polls (non-blocking) and drains all available metrics into a batch
- Queue should NEVER block the HTTP threads (that would add latency to requests)
- Queue growing slightly is fine — aggregator catches up every 100ms
- No backpressure needed — if metrics accumulate, aggregator drains them

`LinkedBlockingQueue` would work but its lock overhead would slow down the 200 HTTP threads with high contention on the putLock.
