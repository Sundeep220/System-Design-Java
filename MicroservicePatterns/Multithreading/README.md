# Multithreading — Navigation Guide

This folder covers Java multithreading concepts in depth — thread mechanics, synchronization primitives, and concurrency utilities.

> **Prerequisite:** Before reading this folder, make sure you have read `JavaMemoryModel/README.md` inside `JVM Basics/`. JMM is the foundation — volatile, synchronized, and happens-before are all assumed knowledge here.

---

## Current Topics

| # | Folder | What You Learn |
|---|---|---|
| 1 | `ThreadBasics/README.md` | Thread creation (Thread, Runnable), `start()` vs `run()`, thread lifecycle (6 states), `sleep()`, `join()`, daemon threads, `ExecutorService` basics |
| 2 | `Callable-Future/README.md` | `Callable<V>` vs `Runnable`, `Future<V>` API, `get()` blocking, `invokeAll`/`invokeAny`, timeout, cancellation, **limitations of Future** |
| 3 | `CompletableFuture/README.md` | `supplyAsync`, chaining (`thenApply`/`thenCompose`), combining (`thenCombine`/`allOf`/`anyOf`), error handling (`exceptionally`/`handle`), timeouts, real-world patterns |
| 4 | `ForkJoinPool/README.md` | Fork/Join framework, `RecursiveTask`/`RecursiveAction`, `fork()`/`join()`, work stealing, `commonPool()`, threshold tuning, parallel streams internals |
| 5 | `ThreadLocal/README.md` | Per-thread data storage, `ThreadLocal` internals (ThreadLocalMap), memory leak risk (must call `remove()`), `InheritableThreadLocal`, use cases in request context, MDC logging, Spring `RequestContextHolder` |

---

## Recommended Reading Order

```
JVM Basics/JVMStack/README.md         → understand thread stacks
JVM Basics/JavaMemoryModel/README.md  → understand thread visibility
                  ↓
ThreadBasics/README.md                → threads, Runnable, ExecutorService
                  ↓
Callable-Future/README.md            → returning results from threads
                  ↓
CompletableFuture/README.md          → async chaining, composition
                  ↓
ForkJoinPool/README.md               → divide-and-conquer, work stealing
                  ↓
ThreadLocal/README.md                → per-thread isolated storage
                  ↓
ConcurrentCollections/ (entire folder) → thread-safe shared data structures
```

---

## Topics Planned / Coming Soon

As more threading topics are added, they will appear here in this order:

```
1. ThreadBasics/          ← done
2. Callable-Future/       ← done
3. CompletableFuture/     ← done
4. ForkJoinPool/          ← done
5. ThreadLocal/           ← done
6. SynchronizedBlocks/    → intrinsic locks, monitor, reentrant locking
7. ReentrantLock/         → explicit locking, tryLock, fairness, Condition
8. ReadWriteLock/         → concurrent reads, exclusive writes
9. CountDownLatch/        → one-time gate for thread coordination
10. CyclicBarrier/        → reusable meeting point for threads
11. Semaphore/            → controlling concurrent access to a resource
12. VirtualThreads/       → Java 21 lightweight threads, structured concurrency
```
