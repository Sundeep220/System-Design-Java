# JVM Basics — Navigation Guide

Follow this order. Each topic builds on the previous. Do not jump around.

---

## Layer 1 — Architecture (Start Here)

Get the complete bird's-eye view before diving into any specific area.

| # | File | What You Learn |
|---|---|---|
| 1 | `Architecture/README.md` | JVM components, memory areas, class loader, execution engine — the full map |
| 2 | `Architecture/NativeCodeExplanation.md` | JNI, native method stacks, how Java calls C/C++ |

---

## Layer 2 — Class Loading System

Before any object exists, a class must be loaded. Understand how `.class` files enter the JVM.

| # | File | What You Learn |
|---|---|---|
| 3 | `ClassLoading/README.md` | Loading → Linking → Initialization phases, ClassLoader hierarchy, parent delegation, ClassNotFoundException vs NoClassDefFoundError, class unloading |
| 4 | `Metaspace/README.md` | Where class metadata lives (native memory), PermGen vs Metaspace, ClassLoader leaks, OOM: Metaspace |

---

## Layer 3 — Memory Structure

How the JVM memory areas are structured — where objects live, how they are laid out.

| # | File | What You Learn |
|---|---|---|
| 5 | `ObjectLayout/README.md` | Object header (mark word + klass pointer), compressed OOPs, field alignment, memory cost per object, lock inflation in mark word |
| 6 | `StringPool/README.md` | String literals vs `new String()`, pool location (Heap, NOT Metaspace), `intern()`, compile-time constants |
| 7 | `Heaps/README.md` | The main heap, Young + Old generation overview, TLAB, GC roots, reachability, heap vs total JVM memory (Kubernetes!) |
| 8 | `JVMStack/README.md` | Per-thread stacks, stack frames, LIFO method calls, StackOverflowError, how references connect to heap objects |

---

## Layer 4 — Memory Semantics

The rules that govern how threads see each other's memory, and how references interact with GC.

| # | File | What You Learn |
|---|---|---|
| 9 | `JavaMemoryModel/README.md` | **Most important for concurrency interviews.** Happens-before, volatile (visibility + ordering, NOT atomicity), synchronized, data races, reordering, safe publication, double-checked locking |
| 10 | `ReferenceTypes/README.md` | Strong / Soft / Weak / Phantom references, when each is collected, ReferenceQueue, WeakHashMap, Cleaner API |

---

## Layer 5 — Garbage Collection

How the GC decides what to keep, what to collect, and how different collectors work.

| # | File | What You Learn |
|---|---|---|
| 11 | `GarbageCollection/README.md` | Reachability, Mark-Sweep-Compact, copying collectors, generational hypothesis, all GC algorithms (Serial/Parallel/G1/ZGC/Shenandoah), Minor/Major/Full GC |
| 12 | `YoungGeneration/README.md` | Eden + Survivor spaces, Minor GC walkthrough, TLAB allocation, object aging, card table, premature promotion |
| 13 | `OldGeneration/README.md` | What lives here, three promotion paths, Major/Full GC, G1 Mixed GC, fragmentation, how leaks show up here |
| 14 | `GCPauses/README.md` | Why Stop-The-World is needed, pause types and durations, GC logging, ZGC vs G1 comparison, reducing pauses |
| 15 | `Safepoints/README.md` | What safepoints are, Time to Safepoint (TTSP) as hidden pause component, long TTSP causes, safepoint bias in profiling |

---

## Layer 6 — Execution Engine

How the JVM executes code and makes it fast.

| # | File | What You Learn |
|---|---|---|
| 16 | `JIT/README.md` | Interpreter vs JIT, hotspot detection, C1 vs C2, tiered compilation, method inlining, escape analysis, warmup, code cache, deoptimization, GraalVM Native Image |

---

## Layer 7 — Diagnosis & Troubleshooting

The hands-on section. Use these when debugging real production problems.

| # | File | What You Learn |
|---|---|---|
| 17 | `MemoryLeaks/README.md` | Why Java CAN have leaks, 7 common patterns (static collections, unbounded caches, ThreadLocal, listeners, inner classes), detection via GC logs, investigation workflow |
| 18 | `OutOfMemoryError/README.md` | All 7 OOM variants, Java heap space, Metaspace, native thread, direct buffer, Kubernetes OOM Kill vs Java OOM |
| 19 | `CPUProfiling/README.md` | Sampling vs instrumentation, flame graphs, async-profiler, JFR, CPU vs wall-clock time, common Spring Boot hotspots |
| 20 | `ThreadDumps/README.md` | Thread states, jstack/jcmd, deadlock diagnosis, starvation, correlating top -H with thread dump |
| 21 | `HeapDumps/README.md` | Shallow vs retained size, Eclipse MAT workflow, Leak Suspects, Dominator Tree, Path to GC Roots, OQL, Kubernetes strategy |

---

## Quick Reference — Problem to Starting Topic

```
Too much memory usage    → MemoryLeaks → HeapDumps → OldGeneration
High CPU / slow          → CPUProfiling → JIT → Safepoints
GC pauses                → GCPauses → GarbageCollection → YoungGeneration
OutOfMemoryError         → OutOfMemoryError → MemoryLeaks → HeapDumps
App frozen / threads stuck → ThreadDumps → JavaMemoryModel
Interview revision       → Architecture → ClassLoading → Heaps → GarbageCollection → JavaMemoryModel → JIT
```
