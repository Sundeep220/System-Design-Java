# Java Collections Framework — Interview Guide

A comprehensive, interview-focused guide covering the most important Java collections with **internal workings**, **time complexities**, and **common interview questions**.

---

## Collections at a Glance

```
                        Iterable
                           │
                      Collection
                     /     |      \
                  List    Set     Queue
                 / |       |  \      |   \
          ArrayList |   HashSet  \  PriorityQueue  Deque
         LinkedList |  LinkedHashSet  \             |
                    |     TreeSet   SortedSet   ArrayDeque
                    |                           LinkedList
                    │
                  Map (separate hierarchy)
                 / |  \
           HashMap  |  TreeMap
       LinkedHashMap|  SortedMap
          Hashtable |  NavigableMap
     ConcurrentHashMap
```

---

## Quick Comparison Table

### List Implementations

| Feature | ArrayList | LinkedList |
|---------|-----------|------------|
| **Data Structure** | Dynamic array | Doubly-linked list |
| **Random Access** | O(1) ✅ | O(n) ❌ |
| **Add at end** | O(1) amortized | O(1) |
| **Add at beginning** | O(n) | O(1) ✅ |
| **Remove by index** | O(n) | O(n) (O(1) with iterator) |
| **Memory per element** | ~4-8 bytes | ~40-48 bytes |
| **Cache Performance** | Excellent | Poor |
| **Use as Queue/Stack** | No | Yes (but prefer ArrayDeque) |

### Set Implementations

| Feature | HashSet | LinkedHashSet | TreeSet |
|---------|---------|---------------|---------|
| **Data Structure** | HashMap | LinkedHashMap | TreeMap (Red-Black tree) |
| **Order** | None | Insertion order | Sorted |
| **Add/Remove/Contains** | O(1) | O(1) | O(log n) |
| **Null elements** | 1 | 1 | No |
| **Navigation methods** | No | No | Yes |

### Map Implementations

| Feature | HashMap | LinkedHashMap | TreeMap |
|---------|---------|---------------|---------|
| **Data Structure** | Hash table | Hash table + linked list | Red-Black tree |
| **Order** | None | Insertion/Access order | Sorted by key |
| **Get/Put** | O(1) | O(1) | O(log n) |
| **Null keys** | 1 | 1 | No |
| **Range queries** | No | No | Yes |
| **Best for** | General use | LRU cache | Sorted/range operations |

### Queue/Deque Implementations

| Feature | PriorityQueue | ArrayDeque | LinkedList |
|---------|---------------|------------|------------|
| **Data Structure** | Binary heap (array) | Circular array | Doubly-linked list |
| **Order** | Priority (min/max) | FIFO/LIFO | FIFO/LIFO |
| **Add** | O(log n) | O(1) amortized | O(1) |
| **Remove head** | O(log n) | O(1) | O(1) |
| **Peek** | O(1) | O(1) | O(1) |
| **Null** | No | No | Yes |
| **Best for** | Priority processing | Stack/Queue | Deque (but ArrayDeque is better) |

---

## Detailed Guides

Each collection has a dedicated deep-dive README:

| Collection | Guide | Key Interview Topics |
|------------|-------|---------------------|
| **ArrayList** | [ArrayList](./ArrayList/README.md) | Dynamic array resizing (1.5x), amortized O(1), fail-fast iterator, `modCount` |
| **LinkedList** | [LinkedList](./LinkedList/README.md) | Doubly-linked nodes, O(1) head/tail ops, cache locality disadvantage |
| **HashMap** | [HashMap](./HashMap/README.md) | Hashing, bucket collision, treeification (Java 8), `hashCode()`/`equals()` contract, load factor, resize |
| **LinkedHashMap** | [LinkedHashMap](./LinkedHashMap/README.md) | Insertion vs access order, LRU cache implementation, `removeEldestEntry()` |
| **TreeMap** | [TreeMap](./TreeMap/README.md) | Red-Black tree, NavigableMap, floor/ceiling, range views, Comparator vs Comparable |
| **HashSet** | [HashSet](./HashSet/README.md) | Backed by HashMap, PRESENT dummy value, set operations (union, intersection) |
| **TreeSet** | [TreeSet](./TreeSet/README.md) | Backed by TreeMap, `compareTo` determines equality (not `equals`), NavigableSet |
| **PriorityQueue** | [PriorityQueue](./PriorityQueue/README.md) | Binary heap, sift up/down, min-heap vs max-heap, Top-K pattern |
| **ArrayDeque** | [ArrayDeque](./ArrayDeque/README.md) | Circular array, preferred over Stack/LinkedList, no null allowed |

---

## Decision Flowchart: Which Collection to Use?

```
Need key-value pairs?
├── Yes → Need sorted keys?
│         ├── Yes → TreeMap
│         └── No → Need insertion order?
│                  ├── Yes → LinkedHashMap
│                  └── No → Need thread-safety?
│                           ├── Yes → ConcurrentHashMap
│                           └── No → HashMap
│
└── No → Need unique elements?
         ├── Yes → Need sorted?
         │         ├── Yes → TreeSet
         │         └── No → Need insertion order?
         │                  ├── Yes → LinkedHashSet
         │                  └── No → HashSet
         │
         └── No → Need priority ordering?
                  ├── Yes → PriorityQueue
                  └── No → Need Stack (LIFO)?
                           ├── Yes → ArrayDeque
                           └── No → Need Queue (FIFO)?
                                    ├── Yes → ArrayDeque
                                    └── No → Need random access?
                                             ├── Yes → ArrayList
                                             └── No → Need frequent head inserts?
                                                      ├── Yes → LinkedList
                                                      └── No → ArrayList
```

---

## Top Interview Concepts Across Collections

### 1. hashCode() and equals() Contract
Required for: **HashMap, HashSet, LinkedHashMap, LinkedHashSet**
- If `a.equals(b)` → `a.hashCode() == b.hashCode()` (MUST)
- If `hashCode` equal → `equals` may or may not be true

### 2. Fail-Fast vs Fail-Safe Iterators
- **Fail-fast**: ArrayList, LinkedList, HashMap, HashSet, TreeMap, TreeSet — throw `ConcurrentModificationException`
- **Fail-safe**: ConcurrentHashMap, CopyOnWriteArrayList — work on a copy/snapshot

### 3. Comparable vs Comparator
Required for: **TreeMap, TreeSet, PriorityQueue**
- `Comparable`: Natural ordering, implemented by the class itself (`compareTo`)
- `Comparator`: External ordering, more flexible (`compare`)

### 4. Thread Safety
None of these collections are thread-safe by default. Options:
- `Collections.synchronizedXxx()` wrappers
- `ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedDeque`
- `PriorityBlockingQueue`, `LinkedBlockingDeque`

### 5. Null Handling
| Allows null | Does NOT allow null |
|------------|---------------------|
| ArrayList, LinkedList | PriorityQueue |
| HashMap (1 key), HashSet (1 element) | ArrayDeque |
| LinkedHashMap, LinkedHashSet | TreeMap*, TreeSet* |
| | ConcurrentHashMap |

*Unless Comparator handles null explicitly.
