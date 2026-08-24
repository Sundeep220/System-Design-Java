# Multithreading — Navigation Guide

This folder covers Java multithreading concepts in depth — thread mechanics, synchronization primitives, and concurrency utilities.

> **Prerequisite:** Before reading this folder, make sure you have read `JavaMemoryModel/README.md` inside `JVM Basics/`. JMM is the foundation — volatile, synchronized, and happens-before are all assumed knowledge here.

---

## Current Topics

| # | Folder | What You Learn |
|---|---|---|
| 1 | `ThreadLocal/README.md` | Per-thread data storage, `ThreadLocal` internals (ThreadLocalMap), memory leak risk (must call `remove()`), `InheritableThreadLocal`, use cases in request context, MDC logging, Spring `RequestContextHolder` |

---

## Recommended Reading Context

`ThreadLocal` sits at the intersection of threading and memory — read it after:

```
JVM Basics/JVMStack/README.md         → understand thread stacks
JVM Basics/JavaMemoryModel/README.md  → understand thread visibility
                  ↓
Multithreading/ThreadLocal/README.md  → per-thread isolated storage
                  ↓
ConcurrentCollections/ (entire folder) → thread-safe shared data structures
```

---

## Topics Planned / Coming Soon

As more threading topics are added, they will appear here in this order:

```
1. ThreadLocal/          ← current
2. ThreadLifecycle/      → Thread states, daemon threads, interruption protocol
3. SynchronizedBlocks/   → intrinsic locks, monitor, reentrant locking
4. ReentrantLock/        → explicit locking, tryLock, fairness, Condition
5. ReadWriteLock/        → concurrent reads, exclusive writes
6. CountDownLatch/       → one-time gate for thread coordination
7. CyclicBarrier/        → reusable meeting point for threads
8. Semaphore/            → controlling concurrent access to a resource
9. CompletableFuture/    → async programming, chaining, exception handling
10. VirtualThreads/      → Java 21 lightweight threads, structured concurrency
```
