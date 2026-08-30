# LinkedHashMap - Deep Dive

## What is LinkedHashMap?

`LinkedHashMap` is a **hash table + doubly-linked list** implementation of the `Map` interface. It maintains a **predictable iteration order** — either **insertion order** (default) or **access order**.

```java
public class LinkedHashMap<K,V> extends HashMap<K,V>
        implements Map<K,V>
```

### Key Characteristics
- **Extends HashMap**: Inherits all HashMap functionality (hashing, buckets, treeification)
- **Maintains order**: Iteration order is predictable (insertion or access order)
- **Allows one null key**: And multiple null values
- **Not synchronized**: Not thread-safe
- **O(1) get/put**: Same as HashMap
- **Ideal for LRU Cache**: Built-in support via `removeEldestEntry()`

---

## How LinkedHashMap Works Internally

### 1. The Key Difference from HashMap

LinkedHashMap extends `HashMap.Node` to add `before` and `after` pointers, creating a **doubly-linked list** that threads through ALL entries:

```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before;   // Previous entry in linked list (insertion/access order)
    Entry<K,V> after;    // Next entry in linked list (insertion/access order)
}
```

```java
// LinkedHashMap fields
transient LinkedHashMap.Entry<K,V> head;  // Eldest entry (first inserted/least recently accessed)
transient LinkedHashMap.Entry<K,V> tail;  // Newest entry (last inserted/most recently accessed)
final boolean accessOrder;                 // false = insertion order, true = access order
```

### 2. Visual Representation

```
HashMap bucket array (for O(1) lookup):
┌───────────────────────────────────────────┐
│ [0] null                                  │
│ [1] Entry("B", 2)                         │
│ [2] null                                  │
│ [3] Entry("A", 1)                         │
│ [4] Entry("C", 3)                         │
│ ...                                       │
└───────────────────────────────────────────┘

Doubly-linked list (for ordered iteration):
head                                              tail
  │                                                │
  ▼                                                ▼
Entry("A",1) ──after──→ Entry("B",2) ──after──→ Entry("C",3)
             ◄─before──              ◄─before──
              
Insertion order: A → B → C
Iteration follows the linked list, NOT the bucket array.
```

**Key insight**: Each entry exists in TWO data structures simultaneously:
1. The HashMap's bucket array (for fast lookup)
2. The doubly-linked list (for ordered iteration)

### 3. How Insertion Maintains Order

When a new entry is added, it's appended to the **tail** of the linked list:

```java
// Simplified — called after inserting into the hash table
void afterNodeInsertion(boolean evict) {
    LinkedHashMap.Entry<K,V> first;
    if (evict && (first = head) != null && removeEldestEntry(first)) {
        K key = first.key;
        removeNode(hash(key), key, null, false, true);  // Remove eldest
    }
}

// New node is linked at the tail
private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
    LinkedHashMap.Entry<K,V> last = tail;
    tail = p;
    if (last == null)
        head = p;
    else {
        p.before = last;
        last.after = p;
    }
}
```

### 4. Access Order Mode

When `accessOrder = true`, any `get()` or `put()` (existing key) moves the entry to the **tail**:

```java
// Called after accessing an existing entry
void afterNodeAccess(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        // Unlink 'e' from its current position
        // Link 'e' at the tail
        // head = least recently accessed
        // tail = most recently accessed
    }
}
```

```
Initial (insertion order): A → B → C → D

get("B"):  A → C → D → B    (B moved to tail)
get("A"):  C → D → B → A    (A moved to tail)

head = C (least recently accessed)
tail = A (most recently accessed)
```

---

## Insertion Order vs Access Order

### Insertion Order (Default)

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.put("C", 3);
map.put("A", 1);
map.put("B", 2);

map.forEach((k, v) -> System.out.print(k + " "));
// Output: C A B  (insertion order preserved)

map.get("C");  // Does NOT change order
map.forEach((k, v) -> System.out.print(k + " "));
// Output: C A B  (still insertion order)
```

### Access Order

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);
//                                                                   ↑ accessOrder = true
map.put("C", 3);
map.put("A", 1);
map.put("B", 2);

map.forEach((k, v) -> System.out.print(k + " "));
// Output: C A B

map.get("C");  // Moves C to the end
map.forEach((k, v) -> System.out.print(k + " "));
// Output: A B C  (C is now most recently accessed)
```

---

## Building an LRU Cache with LinkedHashMap

### The removeEldestEntry() Hook

LinkedHashMap provides a method that is called after every `put()`:

```java
protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return false;  // Default: never remove
}
```

Override it to implement automatic eviction:

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LRUCache(int maxSize) {
        super(maxSize, 0.75f, true);  // accessOrder = true
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;  // Remove eldest when exceeding max size
    }
}
```

```java
LRUCache<String, Integer> cache = new LRUCache<>(3);
cache.put("A", 1);   // {A=1}
cache.put("B", 2);   // {A=1, B=2}
cache.put("C", 3);   // {A=1, B=2, C=3}
cache.get("A");       // Moves A to end: {B=2, C=3, A=1}
cache.put("D", 4);   // Evicts B (eldest): {C=3, A=1, D=4}

System.out.println(cache);  // {C=3, A=1, D=4}
```

### How LRU Eviction Works Internally

```
1. accessOrder = true
2. Every get()/put() moves accessed entry to TAIL
3. HEAD is always the LEAST recently accessed (eldest)
4. When size > maxSize, removeEldestEntry() returns true
5. The HEAD entry is removed

Timeline:
put(A) → [A]                    head=A, tail=A
put(B) → [A, B]                 head=A, tail=B
put(C) → [A, B, C]              head=A, tail=C
get(A) → [B, C, A]              head=B, tail=A  (A moved to tail)
put(D) → [B, C, A, D]           size > 3!
       → remove head (B)
       → [C, A, D]              head=C, tail=D
```

---

## Time Complexity

| Operation | LinkedHashMap | HashMap |
|-----------|--------------|---------|
| `put(key, value)` | **O(1)** | O(1) |
| `get(key)` | **O(1)** | O(1) |
| `remove(key)` | **O(1)** | O(1) |
| `containsKey(key)` | **O(1)** | O(1) |
| **Iteration** | **O(size)** | **O(capacity + size)** |

**Key advantage**: Iteration is O(size), not O(capacity + size) like HashMap, because it follows the linked list (skipping empty buckets).

---

## LinkedHashMap vs HashMap vs TreeMap

| Feature | HashMap | LinkedHashMap | TreeMap |
|---------|---------|---------------|---------|
| Order | None | Insertion / Access | Sorted (Comparator) |
| Get/Put | O(1) | O(1) | O(log n) |
| Iteration | O(capacity + size) | O(size) | O(size) |
| Memory | Lowest | Medium (extra pointers) | Highest (tree nodes) |
| Null keys | Yes | Yes | No* |
| Use case | General | Ordered/LRU cache | Sorted/range queries |

*TreeMap allows null keys only if a Comparator that handles nulls is provided.

---

## Common Interview Questions

### Q1: How does LinkedHashMap maintain insertion order?
By extending HashMap's Node with `before` and `after` pointers, forming a doubly-linked list through all entries. New entries are appended to the tail of this list.

### Q2: How to implement an LRU Cache using LinkedHashMap?
Create with `accessOrder = true` and override `removeEldestEntry()` to return `true` when `size() > maxSize`. Every access moves the entry to the tail; the head (eldest) is evicted when capacity is exceeded.

### Q3: Does put() on an existing key change insertion order?
**No** in insertion-order mode. Updating an existing key does NOT change its position in the linked list.
**Yes** in access-order mode. It's treated as an access and moves to the tail.

### Q4: Why is LinkedHashMap iteration faster than HashMap?
HashMap iterates over the entire bucket array (including empty slots): O(capacity + size). LinkedHashMap follows its doubly-linked list: O(size), skipping empty buckets entirely.

### Q5: What is the memory overhead of LinkedHashMap vs HashMap?
Each entry has **two extra pointers** (`before` and `after`) — about 8-16 extra bytes per entry. The linked list also requires maintaining `head` and `tail` references.

### Q6: Is LinkedHashMap thread-safe?
No. For thread-safe ordered maps, wrap with `Collections.synchronizedMap()` or use `ConcurrentHashMap` (which doesn't maintain order).

---

## Best Practices

1. **Use for LRU caches** — built-in support via `removeEldestEntry()`
2. **Use insertion-order mode** when you need predictable iteration matching insertion sequence
3. **Prefer LinkedHashMap over HashMap** when iteration order matters and you still need O(1) lookups
4. **Don't use for sorted data** — use `TreeMap` instead
5. **Specify initial capacity** to avoid unnecessary resizing
6. **Be careful with access-order mode + iteration**: Calling `get()` during iteration in access-order mode causes `ConcurrentModificationException` because it structurally modifies the linked list
