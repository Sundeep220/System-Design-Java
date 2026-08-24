# Blocking Queues — Deep Dive

Blocking queues are thread-safe queues that **block the calling thread** when the queue is full (producer side) or empty (consumer side). They are the backbone of producer-consumer patterns in Java.

> **Blocking queues solve the fundamental coordination problem: producers don't need to know about consumers, and consumers don't need to know about producers. The queue provides the handoff — safely and efficiently.**

---

# 1. The Core Contract

Every `BlockingQueue` supports these key operations:

```text
Operation      | Returns   | Behavior when full/empty
---------------+-----------+------------------------------------------
put(e)         | void      | BLOCKS until space available
offer(e)       | boolean   | Returns false immediately if full
offer(e,t,u)   | boolean   | Waits up to timeout, then returns false
add(e)         | boolean   | Throws IllegalStateException if full
take()         | element   | BLOCKS until element available
poll()         | element   | Returns null immediately if empty
poll(t,u)      | element   | Waits up to timeout, then returns null
peek()         | element   | Returns head without removing (no block)
```

The two most important: **`put()` blocks when full** and **`take()` blocks when empty**.

---

# 2. The Producer-Consumer Visualization

```text
Producer Threads          BlockingQueue          Consumer Threads

[Producer 1] → put() ─┐
[Producer 2] → put() ─┤    [  Item  ]  ├─→ take() → [Consumer 1]
[Producer 3] → put() ─┘    [  Item  ]  ├─→ take() → [Consumer 2]
                            [  Item  ]  │
                            [  ....  ]  │

When queue is FULL:
  Producer calls put() → BLOCKED → waits until consumer takes

When queue is EMPTY:
  Consumer calls take() → BLOCKED → waits until producer puts
```

This is **backpressure** — the queue naturally throttles producers when consumers are slow.

---

# 3. ArrayBlockingQueue

## What it is

A bounded, FIFO blocking queue backed by a **fixed-size array**.

```text
ArrayBlockingQueue (capacity=4):

Front → [Item1][Item2][Item3][Item4] ← Back

put() adds to back, take() removes from front
When [Item1][Item2][Item3][Item4] → FULL → put() BLOCKS
When [] → EMPTY → take() BLOCKS
```

## Internal Structure

```text
Data structure: circular array (ring buffer)
                [_][_][_][_][_][_][_][_]
                 ↑                 ↑
              takeIndex         putIndex

Locking: ONE ReentrantLock shared by producers AND consumers
         + Two conditions: notFull, notEmpty

put():  lock → check full → insert → signal notEmpty → unlock
take(): lock → check empty → remove → signal notFull → unlock
```

One lock means producers and consumers cannot run truly simultaneously.

## Key Characteristics

```text
Bounded:         YES — capacity fixed at construction time
Ordering:        FIFO
Memory:          Predictable — fixed array, no GC churn from node creation
Fair mode:       Optional — new ArrayBlockingQueue(10, true) → FIFO thread ordering (slower)
Null allowed:    NO
```

## When to Use ArrayBlockingQueue

```text
✓ You need a HARD BOUND on queue size (backpressure is required)
✓ Memory predictability is important (no unbounded growth)
✓ Throughput is moderate (single lock is sufficient)
✓ Real-time systems where you must not let queue grow past limit
✓ Embedded in custom thread pools
```

## Real-life Example: Rate-Limited Task Processor

```java
// Max 100 tasks in flight at any time — backpressure on producers
BlockingQueue<Task> taskQueue = new ArrayBlockingQueue<>(100);

// Producer (HTTP request handler)
public void submitTask(Task task) throws InterruptedException {
    taskQueue.put(task);  // blocks if queue is full → natural rate limiting
}

// Consumer (worker thread)
Runnable worker = () -> {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            Task task = taskQueue.take();  // blocks if empty
            process(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
};
```

---

# 4. LinkedBlockingQueue

## What it is

A FIFO blocking queue backed by **linked nodes**. Optionally bounded (default: `Integer.MAX_VALUE` = effectively unbounded).

```text
LinkedBlockingQueue:

head → [Node1] → [Node2] → [Node3] → ... → tail

put() adds to tail, take() removes from head
```

## Internal Structure — The Key Difference

```text
Two separate locks (unlike ArrayBlockingQueue's one lock):
  takeLock  → held by consumers during take()
  putLock   → held by producers during put()

This means:
  Producer doing put() does NOT block consumers doing take()!
  Both can run simultaneously (on different ends of the queue)
  → MUCH higher throughput than ArrayBlockingQueue
```

Visualization:

```text
ArrayBlockingQueue:        LinkedBlockingQueue:
[put and take share        [put uses putLock]
 one lock → serial]        [take uses takeLock]
                           [Can run SIMULTANEOUSLY → higher throughput]
```

## Key Characteristics

```text
Bounded:         Optional — LinkedBlockingQueue(100) or new LinkedBlockingQueue() (unbounded)
Ordering:        FIFO
Memory:          Each node is a new object → GC pressure under high throughput
Fair mode:       NO fair mode option
Throughput:      Higher than ArrayBlockingQueue (two locks)
Null allowed:    NO
```

## ArrayBlockingQueue vs LinkedBlockingQueue

| | ArrayBlockingQueue | LinkedBlockingQueue |
|---|---|---|
| Backing structure | Array (ring buffer) | Linked nodes |
| Locking | 1 lock (producers + consumers contend) | 2 locks (put/take independent) |
| Throughput | Lower | Higher |
| Memory | Fixed, predictable | Variable, GC pressure |
| Bounded | Always | Optional |
| Fair mode | Yes | No |
| Best for | Bounded, moderate load | High throughput, bounded or unbounded |

## Real-life Example: Thread Pool Work Queue

```java
// Executors.newFixedThreadPool uses LinkedBlockingQueue internally
ExecutorService pool = Executors.newFixedThreadPool(10);

// Equivalent manual setup:
BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);  // bounded
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 10, 0, TimeUnit.SECONDS, workQueue
);
```

```text
Without bound: new LinkedBlockingQueue() (Integer.MAX_VALUE)
Danger: unbounded → can consume all memory if producers outpace consumers!
Always prefer bounded: new LinkedBlockingQueue(maxCapacity)
```

## Real-life Example: Kafka-like Message Buffer

```java
// Internal buffer before sending to Kafka
BlockingQueue<KafkaMessage> buffer = new LinkedBlockingQueue<>(10_000);

// High-throughput producers (many request threads)
public void enqueue(KafkaMessage msg) throws InterruptedException {
    buffer.put(msg);  // blocks if buffer full → backpressure
}

// Single Kafka sender thread (batches messages)
Runnable sender = () -> {
    List<KafkaMessage> batch = new ArrayList<>();
    while (true) {
        KafkaMessage msg = buffer.take();  // wait for first message
        batch.add(msg);
        buffer.drainTo(batch, 999);  // grab up to 999 more (non-blocking)
        kafkaProducer.send(batch);
        batch.clear();
    }
};
```

---

# 5. PriorityBlockingQueue

## What it is

An **unbounded** blocking queue that orders elements by **priority** (natural ordering or `Comparator`). The head is always the element with the **lowest** priority value (min-heap).

```text
PriorityBlockingQueue:

Internal structure: binary min-heap

         1 (highest priority)
        / \
       3   5
      / \ / \
     7  4 6  8

take() always returns the root (minimum = highest priority)
```

## Key Characteristics

```text
Bounded:         NO — unbounded (producer never blocks on put!)
Ordering:        Priority (not FIFO)
Elements:        Must implement Comparable OR provide Comparator
Memory:          Grows dynamically
Null allowed:    NO
```

**Important**: `put()` never blocks because it's unbounded. Only `take()` can block (when empty).

## Real-life Example: Task Scheduling by Priority

```java
public class PrioritizedTask implements Comparable<PrioritizedTask> {

    private final int priority;  // lower = higher priority
    private final Runnable task;
    private final String name;

    @Override
    public int compareTo(PrioritizedTask other) {
        return Integer.compare(this.priority, other.priority);
    }
}

PriorityBlockingQueue<PrioritizedTask> taskQueue = new PriorityBlockingQueue<>();

// Submit tasks with different priorities
taskQueue.put(new PrioritizedTask(5, orderProcessing, "order-1"));
taskQueue.put(new PrioritizedTask(1, fraudAlert, "fraud-check"));    // highest priority
taskQueue.put(new PrioritizedTask(3, emailNotification, "email-1"));

// Worker always picks highest priority task
while (true) {
    PrioritizedTask task = taskQueue.take();  // gets priority=1 first
    task.run();
}
```

## Real-life Use Cases

```text
1. Incident management: P0 > P1 > P2 tickets processed first
2. Order processing: VIP orders before standard orders
3. Resource scheduling: high-priority jobs get CPU/DB access first
4. Retry queue: retries with lower priority than fresh requests
5. Medical systems: critical patients before routine checkups
```

## PriorityBlockingQueue vs ArrayBlockingQueue

```text
ArrayBlockingQueue: FIFO, bounded
PriorityBlockingQueue: Priority-ordered, unbounded

If you need priority + bounded:
  PriorityBlockingQueue does NOT support bounding natively
  Workaround: combine PriorityBlockingQueue with a Semaphore to limit concurrent tasks
```

---

# 6. DelayQueue

## What it is

An **unbounded** blocking queue where elements can only be taken **after their delay has expired**. Elements implement `Delayed` interface specifying when they become available.

```text
DelayQueue:

Elements ordered by delay expiry time:
  [expires at T+1s][expires at T+3s][expires at T+10s]
                ↑
            take() blocks here until T+1s
            At T+1s: returns first element
            Until then: blocked
```

## Key Characteristics

```text
Bounded:         NO
Ordering:        By delay expiry time (earliest-expiring first)
take():          BLOCKS until the head element's delay has expired
Elements:        Must implement Delayed interface
```

## Real-life Use Cases

```text
1. Session expiry: sessions expire after N minutes of inactivity
2. Cache TTL eviction: background thread evicts expired cache entries
3. Retry with backoff: failed task retried after delay (exponential backoff)
4. Scheduled cleanup: cleanup tasks run after a grace period
5. Rate limiting: token refill after fixed intervals
6. Distributed system: lease renewal, heartbeat timeouts
```

## Real-life Example: Retry with Exponential Backoff

```java
public class RetryTask implements Delayed {

    private final String taskId;
    private final int retryCount;
    private final long readyTime;  // epoch millis when ready

    public RetryTask(String taskId, int retryCount) {
        this.taskId = taskId;
        this.retryCount = retryCount;
        // Exponential backoff: 1s, 2s, 4s, 8s...
        long delayMs = (long) Math.pow(2, retryCount) * 1000;
        this.readyTime = System.currentTimeMillis() + delayMs;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(readyTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.readyTime, ((RetryTask) other).readyTime);
    }
}

DelayQueue<RetryTask> retryQueue = new DelayQueue<>();

// On failure: schedule retry
retryQueue.put(new RetryTask("order-42", 1));  // retry after 2 seconds

// Retry worker
while (true) {
    RetryTask task = retryQueue.take();  // blocks until delay expires
    retryOrder(task.getTaskId());
}
```

---

# 7. SynchronousQueue

## What it is

A queue with **zero capacity**. Every `put()` must wait for a corresponding `take()`, and vice versa. It's a direct thread-to-thread handoff channel.

```text
SynchronousQueue:

NO INTERNAL STORAGE — elements are not stored, only handed off

Producer:  put("task") → BLOCKS until a consumer calls take()
Consumer:  take()      → BLOCKS until a producer calls put()

When both are ready: handoff happens instantly
```

## Key Characteristics

```text
Capacity:  ZERO
Blocking:  Always (both put and take always block)
Ordering:  FIFO (fair mode) or LIFO (unfair mode)
Use case:  Direct thread-to-thread handoff — no buffering
```

## Where Is It Used?

`Executors.newCachedThreadPool()` uses `SynchronousQueue` internally:

```text
CachedThreadPool + SynchronousQueue:
  New task arrives:
    Try to handoff to an idle thread via SynchronousQueue
    If no idle thread → create new thread
    If too many threads idle → threads time out and die
```

This creates a self-adjusting thread pool that:
- Grows to handle burst traffic
- Shrinks when traffic dies down

## Real-life Example: Pipeline Stage Handoff

```java
SynchronousQueue<DataPacket> handoff = new SynchronousQueue<>();

// Stage 1: produces data
Thread producer = new Thread(() -> {
    while (true) {
        DataPacket packet = fetchData();
        handoff.put(packet);  // blocks until stage 2 is ready
    }
});

// Stage 2: processes data (MUST be ready before stage 1 can proceed)
Thread consumer = new Thread(() -> {
    while (true) {
        DataPacket packet = handoff.take();  // blocks until stage 1 produces
        transform(packet);
    }
});
```

Useful when:

```text
Producer should not get ahead of consumer at all
Direct one-to-one coupling is desired
No buffering — backpressure is immediate
```

## SynchronousQueue vs ArrayBlockingQueue(1)

```text
SynchronousQueue:
  Zero capacity → handoff only
  put() blocks until take() is called
  
ArrayBlockingQueue(1):
  Capacity = 1 → stores one element
  put() blocks only after the one slot is filled
  Consumer can be slightly behind producer

SynchronousQueue is stricter — tighter coupling between producer and consumer.
```

---

# 8. LinkedTransferQueue

## What it is

A combination of `LinkedBlockingQueue` and `SynchronousQueue`. It supports both buffered and synchronous transfer modes.

```java
queue.transfer(element);   // blocks until consumer takes it (like SynchronousQueue)
queue.put(element);        // puts in queue, doesn't wait for consumer
queue.tryTransfer(element); // transfers only if consumer immediately ready, else returns false
```

## When to Use

```text
When you need BOTH:
  Fast async puts (producer doesn't wait) for most cases
  AND
  Occasional synchronous transfer with confirmation

Example: Reactive message processing where producers usually buffer
         but sometimes need acknowledgment
```

---

# 9. Blocking Queues in Thread Pools

`ThreadPoolExecutor` takes a `BlockingQueue<Runnable>` as its work queue. The choice dramatically affects behavior:

```text
Executor type              | Queue used              | Behavior
---------------------------+-------------------------+----------------------------------
newFixedThreadPool(n)      | LinkedBlockingQueue()   | Unbounded queue — tasks wait if all threads busy
newCachedThreadPool()      | SynchronousQueue()      | No queue — spawns new thread or rejects
newSingleThreadExecutor()  | LinkedBlockingQueue()   | Single thread, unlimited queue
Custom bounded executor    | ArrayBlockingQueue(100) | Bounded — rejects/blocks when full
```

Custom bounded pool with rejection handling:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                            // corePoolSize
    20,                            // maxPoolSize
    60, TimeUnit.SECONDS,          // idle thread timeout
    new ArrayBlockingQueue<>(200), // bounded queue
    new ThreadPoolExecutor.CallerRunsPolicy()  // caller runs if pool full
);
```

`CallerRunsPolicy` = if pool full + queue full, the submitting thread runs the task itself → automatic backpressure.

---

# 10. Comparison Summary

| | ArrayBlockingQueue | LinkedBlockingQueue | PriorityBlockingQueue | DelayQueue | SynchronousQueue |
|---|---|---|---|---|---|
| Bounded | Yes (always) | Optional | No | No | N/A (0 capacity) |
| Ordering | FIFO | FIFO | Priority | Delay-expiry | N/A |
| Memory | Fixed | Dynamic | Dynamic | Dynamic | None |
| Locking | 1 lock | 2 locks | 1 lock | 1 lock | CAS |
| put blocks | When full | When full | Never | Never | Always |
| take blocks | When empty | When empty | When empty | Delay pending | Always |
| Best for | Bounded work queue | High-throughput queue | Priority processing | Scheduling | Direct handoff |

---

# Interview Preparation — Blocking Queues

---

## Q1: What is the difference between put() and offer() in BlockingQueue?

**Answer:**

- `put(e)`: blocks indefinitely until space is available (or thread interrupted)
- `offer(e)`: returns `false` immediately if queue is full (no blocking)
- `offer(e, timeout, unit)`: waits up to the timeout, then returns `false`

Use `put()` in producer-consumer patterns where producers must wait. Use `offer()` when you want to detect backpressure without blocking (e.g., drop tasks or log a warning when queue is full).

---

## Q2: What is the key difference between ArrayBlockingQueue and LinkedBlockingQueue?

**Answer:**

| | ArrayBlockingQueue | LinkedBlockingQueue |
|---|---|---|
| Backing | Fixed array | Dynamic linked nodes |
| Locking | One shared lock | Two locks (put/take separate) |
| Throughput | Lower (put and take contend) | Higher (put and take independent) |
| Bounded | Always bounded | Optional |
| Memory | Predictable | Variable (GC pressure) |

LinkedBlockingQueue's two-lock design allows producers and consumers to operate truly in parallel — producer adds to tail while consumer removes from head simultaneously.

---

## Q3: When would you use PriorityBlockingQueue over LinkedBlockingQueue?

**Answer:**

Use `PriorityBlockingQueue` when tasks have different priorities and high-priority tasks must be processed first.

```text
LinkedBlockingQueue: FIFO — first submitted, first processed
PriorityBlockingQueue: highest priority first, regardless of submission order
```

Real example: a payment processing system where fraud-detection tasks are priority=1 and reporting tasks are priority=5. PriorityBlockingQueue ensures fraud checks are always processed before reports.

Caveat: `PriorityBlockingQueue` is unbounded — no backpressure on producers. Combine with a Semaphore if you need both priority + backpressure.

---

## Q4: How does SynchronousQueue work and where is it used in the JDK?

**Answer:**

`SynchronousQueue` has zero capacity — it never stores elements. A `put()` blocks until another thread calls `take()`, and vice versa. It's a direct thread-to-thread handoff.

Used internally in `Executors.newCachedThreadPool()` — when a task is submitted, the pool tries to hand it off directly to a waiting thread. If no waiting thread → create a new thread. If threads are idle and no tasks come → threads time out and die. This creates a pool that adapts its size to load.

---

## Q5: What is the DelayQueue used for?

**Answer:**

`DelayQueue` holds elements that become available only after their delay expires. `take()` blocks until the head element's delay has expired.

Use cases:
- **Retry with backoff**: failed tasks re-queued with exponential delay
- **Cache TTL**: background thread evicts expired entries
- **Session timeout**: sessions expire after inactivity period
- **Scheduled tasks**: lightweight alternative to ScheduledExecutorService
- **Rate limiting**: tokens refilled at fixed intervals

Elements must implement `Delayed` which provides `getDelay()` and `compareTo()` for ordering.

---

## Q6: How do blocking queues help implement backpressure in microservices?

**Answer:**

Backpressure is the mechanism where slow consumers signal producers to slow down.

With `ArrayBlockingQueue`:
```text
HTTP thread → put(task) → queue
                  ↑ BLOCKS if queue full

Consumer thread: take() → process

If processing is slow:
  → queue fills up
  → producers block in put()
  → HTTP threads back up
  → connection pool exhausts
  → upstream gets TCP timeouts
  → naturally throttled
```

This prevents out-of-memory errors from unbounded work accumulation. The queue capacity = the amount of "in-flight" work you can tolerate.

In microservices, backpressure prevents cascading failures: when a downstream service is slow, the upstream service naturally throttles rather than accumulating tasks until it OOMs.
