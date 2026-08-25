# MicroservicePatterns — Master Index

This folder is a comprehensive knowledge base for Java, JVM internals, microservices, Spring Boot, and system design. Each subfolder has its own `README.md` that shows the correct reading order within that topic.

---

## Subfolders and Their Purpose

| Folder | What It Covers | Start Here |
|---|---|---|
| `JavaCore/` | OOP fundamentals: classes, encapsulation, inheritance, polymorphism, overloading/overriding, annotations, reflection | `JavaCore/README.md` |
| `JVM Basics/` | JVM internals: architecture, class loading, memory areas, GC, JIT, diagnostics (profiling, thread/heap dumps) | `JVM Basics/README.md` |
| `Multithreading/` | Java threading: ThreadLocal, locks, synchronization, concurrent utilities | `Multithreading/README.md` |
| `ConcurrentCollections/` | Thread-safe collections: ConcurrentHashMap, blocking queues, CopyOnWrite, skip lists | `ConcurrentCollections/README.md` |
| `HTTP/` | HTTP/1.1 vs HTTP/2 vs HTTP/3, request/response anatomy, headers, cookies, status codes, caching, ETags, keep-alive | `HTTP/README.md` |
| `Security/` | TLS vs SSL, TLS handshake, X.509 certificates, mTLS (microservice auth, Istio), SSH (key auth, tunneling, CI/CD), TLS vs SSH comparison | `Security/README.md` |
| `ConsistentHashing/` | The hash ring algorithm behind Kafka, Cassandra, DynamoDB | `ConsistentHashing/README.md` |
| `DistributedSystems/` | Core distributed systems building blocks: Bloom Filters, SSTables (LSM Tree), Gossip Protocol, Anti-Entropy, Vector Clocks, MVCC, CRDTs | `DistributedSystems/README.md` |
| `SpringBootRoadmap/` | Spring Boot concepts, project structure, and practice project guide | `SpringBootRoadmap/README.md` |
| `MicroserviceRoadmap/` | Microservice patterns, design principles, and architecture guide | `MicroserviceRoadmap/README.md` |

---

## Recommended Full Learning Path

If you are building knowledge from scratch, follow this order across all folders:

### Phase 1 — Java Core (Weeks 1-2)
```
JavaCore/README.md   (read the navigation guide first)
  1. JavaCore/Classes/
  2. JavaCore/Encapsulation/
  3. JavaCore/Inheritance/
  4. JavaCore/Polymorphism/
  5. JavaCore/OverloadingVsOverriding/
  6. JavaCore/Annotations/
  7. JavaCore/Reflection/
```

### Phase 2 — JVM Internals (Weeks 3-5)
```
JVM Basics/README.md   (read the navigation guide first)
  Layer 1: Architecture (2 docs)
  Layer 2: ClassLoading + Metaspace
  Layer 3: ObjectLayout + StringPool + Heaps + JVMStack
  Layer 4: JavaMemoryModel + ReferenceTypes
  Layer 5: GarbageCollection + YoungGeneration + OldGeneration + GCPauses + Safepoints
  Layer 6: JIT
  Layer 7: MemoryLeaks + OutOfMemoryError + CPUProfiling + ThreadDumps + HeapDumps
```

### Phase 3 — Threading & Concurrent Collections (Weeks 6-7)
```
Multithreading/README.md
  1. Multithreading/ThreadLocal/

ConcurrentCollections/README.md
  2. ConcurrentCollections/ConcurrentHashMap/
  3. ConcurrentCollections/CopyOnWriteCollections/
  4. ConcurrentCollections/ConcurrentLinkedCollections/
  5. ConcurrentCollections/BlockingQueues/
  6. ConcurrentCollections/ConcurrentSkipList/
```

### Phase 4 — System Design (Weeks 8-9)
```
ConsistentHashing/README.md
  1. ConsistentHashing/consistent_hashing_detailed_notes.md

DistributedSystems/README.md
  2. DistributedSystems/BloomFilter/
  3. DistributedSystems/SSTable/
  4. DistributedSystems/GossipProtocol/
  5. DistributedSystems/VectorClocks/
```

### Phase 5 — Spring Boot & Microservices (Weeks 9-10)
```
SpringBootRoadmap/README.md
MicroserviceRoadmap/README.md
SpringBootRoadmap/Practise_project.md
```

---

## Quick Reference — What to Read for an Interview

### Java Basics interview
```
JavaCore/ (entire folder, in order)
```

### JVM / Memory interview
```
JVM Basics/Architecture → Heaps → GarbageCollection → JavaMemoryModel → JIT → OutOfMemoryError
```

### Concurrency interview
```
JVM Basics/JavaMemoryModel → Multithreading/ThreadLocal → ConcurrentCollections/ (all)
```

### System Design interview
```
ConsistentHashing → DistributedSystems/BloomFilter → DistributedSystems/SSTable
→ DistributedSystems/GossipProtocol → DistributedSystems/VectorClocks → MicroserviceRoadmap
```

### Spring Boot interview
```
JavaCore/Annotations → JavaCore/Reflection → SpringBootRoadmap
```

---

## Topic Dependency Map

```
JavaCore/Classes
    ↓
JavaCore/Encapsulation → Inheritance → Polymorphism → Overloading/Overriding
    ↓
JavaCore/Annotations → Reflection
    ↓
JVM Basics/Architecture
    ↓
JVM Basics/ClassLoading → Metaspace
    ↓
JVM Basics/ObjectLayout → StringPool → Heaps → JVMStack
    ↓
JVM Basics/JavaMemoryModel → ReferenceTypes
    ↓
JVM Basics/GarbageCollection → YoungGen → OldGen → GCPauses → Safepoints
    ↓
JVM Basics/JIT
    ↓
JVM Basics/MemoryLeaks → OOM → CPUProfiling → ThreadDumps → HeapDumps
    ↓
Multithreading/ThreadLocal
    ↓
ConcurrentCollections/ (all topics)
    ↓
ConsistentHashing/
    ↓
DistributedSystems/BloomFilter
    ↓
DistributedSystems/SSTable
    ↓
DistributedSystems/GossipProtocol
    ↓
DistributedSystems/VectorClocks
    ↓
SpringBootRoadmap/ + MicroserviceRoadmap/
```
