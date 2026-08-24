# Consistent Hashing — Navigation Guide

This folder covers consistent hashing — the key algorithm behind distributed data partitioning used in Kafka, Cassandra, DynamoDB, Redis Cluster, and load balancers.

---

## Reading Order

This folder has a single comprehensive document:

| # | File | What You Learn |
|---|---|---|
| 1 | `consistent_hashing_detailed_notes.md` | Why consistent hashing exists, the hash ring, virtual nodes, hotspot mitigation, replication with successor nodes, real-world use in Kafka/Cassandra/DynamoDB, comparison with modulo-based partitioning |

---

## Prerequisites

No strict prerequisites, but consistent hashing is easier to appreciate if you already understand:

```
Basic data structures: hash tables, linked lists
Distributed systems basics: partitioning, replication, node failures
```

---

## Where Consistent Hashing Is Used

```
Kafka          → partition assignment for consumer groups
Cassandra      → token ring data distribution
DynamoDB       → key-space partitioning
Redis Cluster  → slot assignment (16384 slots mapped via hash)
Load Balancers → sticky sessions / server selection
CDNs           → cache node routing
```

---

## Connection to Other Topics

```
ConsistentHashing/ → ConcurrentCollections/ConcurrentSkipList/
  ConcurrentSkipListMap is used to implement a consistent hashing ring in Java:
  ring.ceilingEntry(hash) finds the next virtual node clockwise
```
