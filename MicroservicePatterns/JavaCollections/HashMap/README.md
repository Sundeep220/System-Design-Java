# HashMap - Deep Dive

## What is HashMap?

`HashMap` is a **hash table** based implementation of the `Map` interface. It stores key-value pairs and provides **O(1) average-case** time complexity for `get()` and `put()` operations.

```java
public class HashMap<K,V> extends AbstractMap<K,V>
        implements Map<K,V>, Cloneable, Serializable
```

### Key Characteristics
- **Unordered**: Does NOT guarantee any order of entries
- **Allows one null key**: And multiple null values
- **Allows duplicate values**: But keys must be unique
- **Not synchronized**: Not thread-safe
- **O(1) average**: For get/put operations
- **Based on hashing**: Uses hashCode() and equals() of keys

---

## How HashMap Works Internally

### 1. Core Data Structure

HashMap internally uses an **array of Node objects** called the **table** (also referred to as **buckets**).

```java
transient Node<K,V>[] table;     // The bucket array
transient int size;               // Number of key-value mappings
int threshold;                    // size at which to resize (capacity * loadFactor)
final float loadFactor;           // Default 0.75
transient int modCount;           // Structural modification count
```

### 2. Node Structure

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;       // Cached hash of the key
    final K key;          // The key
    V value;              // The value
    Node<K,V> next;       // Next node in the bucket (for chaining)
}
```

### 3. Visual Representation

```
table[] (bucket array)
Index  
  0   → null
  1   → [hash|"key1"|"val1"|next] → [hash|"key5"|"val5"|null]   ← Collision chain
  2   → null
  3   → [hash|"key2"|"val2"|null]
  4   → null
  5   → [hash|"key3"|"val3"|next] → [hash|"key6"|"val6"|next] → ... → TreeNode (if chain ≥ 8)
  6   → null
  7   → [hash|"key4"|"val4"|null]
  ...
  15  → null

  ◄── capacity = 16 (default) ──►
```

---

## The Hashing Process — How put() Works

### Step-by-Step Flow

```
put(key, value)
    │
    ▼
Step 1: Compute hash
    hash = hash(key)
    hash = key.hashCode() ^ (key.hashCode() >>> 16)   // "spread" function
    │
    ▼
Step 2: Find bucket index
    index = hash & (capacity - 1)     // Equivalent to hash % capacity (when capacity is power of 2)
    │
    ▼
Step 3: Check bucket
    │
    ├── Bucket is empty?
    │       → Create new Node, place at table[index]
    │
    ├── Bucket has nodes (collision)?
    │       → Walk the linked list / tree
    │       → For each node, check:
    │            if (node.hash == hash && (node.key == key || key.equals(node.key)))
    │                → Key exists! Replace value, return old value
    │            else
    │                → Move to next node
    │       → If end of chain reached → append new Node
    │
    ▼
Step 4: Check if treeification needed
    If chain length ≥ TREEIFY_THRESHOLD (8) AND table.length ≥ 64
        → Convert linked list to Red-Black Tree
    │
    ▼
Step 5: Check if resize needed
    if (++size > threshold)    // threshold = capacity * loadFactor
        → resize()            // Double the capacity
```

### The hash() Function — Why XOR with Right Shift?

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**Why not just use `key.hashCode()` directly?**

The bucket index is computed as `hash & (capacity - 1)`. If capacity is 16, then only the **last 4 bits** of the hash are used. The XOR with the upper 16 bits "spreads" the influence of higher-order bits into lower-order bits, reducing collisions.

```
hashCode():    1010 1100 0011 0111 0001 1010 0110 1100
                                          ↓
h >>> 16:      0000 0000 0000 0000 1010 1100 0011 0111
                                          ↓
XOR result:    1010 1100 0011 0111 1011 0110 0101 1011
                                    ↑
                        Higher bits now influence lower bits
```

### Bucket Index Calculation

```java
index = hash & (n - 1);   // where n = capacity (always power of 2)
```

Why `hash & (n-1)` instead of `hash % n`?
- `&` is a **bitwise AND** — much faster than modulo
- Works correctly only when `n` is a **power of 2** (e.g., 16, 32, 64...)
- `16 - 1 = 15 = 0000 1111` → masks out all bits except the last 4

```
hash:        1011 0110 0101 1011
n-1 (15):    0000 0000 0000 1111
             ─────────────────── AND
result:      0000 0000 0000 1011 = 11  → bucket index 11
```

---

## Collision Handling

### What is a Collision?

When two different keys produce the same bucket index:
```java
hash("key1") & 15 = 5
hash("key9") & 15 = 5   // Same bucket!
```

### Before Java 8: Separate Chaining (Linked List Only)

```
Bucket 5: Node("key1") → Node("key9") → Node("key17") → null
```

**Problem**: In worst case (all keys in same bucket), `get()` becomes **O(n)**.

### Java 8+: Treeification (Linked List → Red-Black Tree)

When a single bucket's chain length reaches **8** (TREEIFY_THRESHOLD) AND the table size is at least **64** (MIN_TREEIFY_CAPACITY), the chain is converted to a **Red-Black Tree**.

```
Bucket 5 (chain ≥ 8):

Before treeification:
Node → Node → Node → Node → Node → Node → Node → Node → Node
                    O(n) lookup

After treeification:
              TreeNode("key9")
              /              \
    TreeNode("key1")    TreeNode("key17")
    /        \            /         \
  ...       ...        ...         ...
                    O(log n) lookup
```

### TreeNode Structure

```java
static final class TreeNode<K,V> extends LinkedHashMap.LinkedHashMapEntry<K,V> {
    TreeNode<K,V> parent;
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // Needed to unlink on deletion
    boolean red;           // Red-Black tree color
}
```

### Treeification Constants

```java
static final int TREEIFY_THRESHOLD = 8;      // Chain length to convert to tree
static final int UNTREEIFY_THRESHOLD = 6;     // Chain length to convert back to list
static final int MIN_TREEIFY_CAPACITY = 64;   // Min table size for treeification
```

**Why 8?** Based on Poisson distribution — with a good hash function and load factor 0.75, the probability of 8 elements in one bucket is approximately **0.00000006** (1 in 10 million). So treeification is a safety net for pathological cases.

---

## Resizing (Rehashing)

### When Does Resizing Happen?

```java
if (++size > threshold)   // threshold = capacity * loadFactor
    resize();
```

Default: capacity = 16, loadFactor = 0.75 → threshold = 12.
So when the 13th element is added, resize triggers.

### How resize() Works

```
Step 1: Create new table with DOUBLE the capacity
        oldCap = 16 → newCap = 32

Step 2: Rehash ALL entries into new table
        For each entry:
            newIndex = hash & (newCap - 1)

Step 3: Replace old table with new table
```

### The Clever Rehashing in Java 8+

In Java 8, entries either stay at the **same index** or move to **index + oldCapacity**:

```
Old capacity = 16 (mask = 0000 1111)
New capacity = 32 (mask = 0001 1111)

The only difference is one extra bit!

hash:        xxxx xxxx xxxx Y xxxx    (Y is the extra bit)

if Y == 0: newIndex = oldIndex           (stays in place)
if Y == 1: newIndex = oldIndex + 16      (moves to oldIndex + oldCap)
```

This avoids recomputing the hash — just check one bit!

### Resizing Progression

| # Elements | Capacity | Threshold (0.75) |
|-----------|----------|-------------------|
| 0-12      | 16       | 12                |
| 13-24     | 32       | 24                |
| 25-48     | 64       | 48                |
| 49-96     | 128      | 96                |
| 97-192    | 256      | 192               |

---

## Time Complexity

| Operation | Average | Worst Case | Notes |
|-----------|---------|------------|-------|
| `put(key, value)` | **O(1)** | O(n) / O(log n)* | O(log n) with treeification |
| `get(key)` | **O(1)** | O(n) / O(log n)* | |
| `remove(key)` | **O(1)** | O(n) / O(log n)* | |
| `containsKey(key)` | **O(1)** | O(n) / O(log n)* | |
| `containsValue(value)` | **O(n)** | O(n) | Must scan all entries |
| `keySet()` / `values()` / `entrySet()` | **O(1)** | O(1) | Returns view, not copy |
| Iteration | **O(capacity + size)** | O(capacity + size) | Scans entire table |

*Worst case is O(log n) in Java 8+ due to treeification, O(n) in Java 7.

---

## The hashCode() and equals() Contract

### The Contract

1. **If `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` MUST be true**
2. If `a.hashCode() == b.hashCode()`, `a.equals(b)` may or may not be true (collision)
3. `hashCode()` must be consistent — return the same value across invocations (if object hasn't changed)

### What Happens When the Contract is Violated?

```java
class BadKey {
    String name;

    @Override
    public boolean equals(Object o) {
        return name.equals(((BadKey) o).name);
    }

    // Missing hashCode() override!
    // Uses Object.hashCode() → returns different hash for equal objects
}

BadKey k1 = new BadKey("test");
BadKey k2 = new BadKey("test");

map.put(k1, "value1");
map.get(k2);  // Returns null! k1.equals(k2) is true but hashCode differs
              // k2 goes to a different bucket than k1
```

### Good hashCode() Implementation

```java
@Override
public int hashCode() {
    int result = 17;
    result = 31 * result + (name == null ? 0 : name.hashCode());
    result = 31 * result + age;
    return result;
}

// Or use Objects.hash() (Java 7+)
@Override
public int hashCode() {
    return Objects.hash(name, age);
}
```

**Why 31?** It's an odd prime. `31 * i` can be optimized by the JVM to `(i << 5) - i`, which is faster than multiplication.

---

## Null Key Handling

HashMap allows **exactly one null key**, stored at **bucket 0**:

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    //     ↑ null key always hashes to 0
}
```

```java
map.put(null, "value1");  // Stored at table[0]
map.get(null);            // Looks at table[0]
map.put(null, "value2");  // Replaces value at table[0]
```

---

## Load Factor

### What is Load Factor?

```
Load Factor = Number of entries / Number of buckets
```

- **Default: 0.75** — a good balance between time and space
- **Lower (e.g., 0.5)**: Fewer collisions, more memory, faster lookups
- **Higher (e.g., 0.9)**: More collisions, less memory, slower lookups

```java
// Custom load factor
HashMap<String, String> map = new HashMap<>(16, 0.5f);  // Resize at 50% full
```

### Why 0.75?

At load factor 0.75, the average chain length (with a good hash function) is about **0.5**, meaning most buckets have 0 or 1 entries. This keeps average lookup at O(1) while using only 75% of allocated space.

---

## Important Concepts

### 1. Capacity Must Be Power of 2

HashMap enforces that capacity is always a power of 2:

```java
// If you request capacity 13, HashMap rounds up to 16
HashMap<String, String> map = new HashMap<>(13);  // Internal capacity = 16
```

```java
static final int tableSizeFor(int cap) {
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
// tableSizeFor(13) = 16
// tableSizeFor(17) = 32
```

**Why?** Because `hash & (capacity - 1)` only works correctly when capacity is a power of 2.

### 2. Fail-Fast Iterators

```java
Map<String, String> map = new HashMap<>();
map.put("A", "1");
map.put("B", "2");

// Throws ConcurrentModificationException
for (Map.Entry<String, String> entry : map.entrySet()) {
    map.remove(entry.getKey());
}

// Safe way
Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
while (it.hasNext()) {
    Map.Entry<String, String> entry = it.next();
    if (entry.getKey().equals("A")) {
        it.remove();  // Safe
    }
}

// Or use removeIf
map.entrySet().removeIf(entry -> entry.getKey().equals("A"));
```

### 3. Iteration Order is Unpredictable

```java
map.put("C", "3");
map.put("A", "1");
map.put("B", "2");

for (String key : map.keySet()) {
    System.out.println(key);  // Could print in ANY order
}
// For ordered iteration, use LinkedHashMap or TreeMap
```

### 4. HashMap is NOT Thread-Safe

Concurrent modification can cause:
- **Lost updates**: Two threads overwriting each other
- **Infinite loops** (Java 7): Due to linked list cycle during resize
- **Data corruption**: Partially visible state

```java
// Thread-safe alternatives
Map<K,V> syncMap = Collections.synchronizedMap(new HashMap<>());
ConcurrentHashMap<K,V> concMap = new ConcurrentHashMap<>();  // Preferred
```

---

## Common Interview Questions

### Q1: How does HashMap handle collisions?
**Separate chaining**. In Java 7: linked list only. In Java 8+: linked list that converts to Red-Black tree when chain length ≥ 8 and table size ≥ 64, providing O(log n) worst-case instead of O(n).

### Q2: What happens when two keys have the same hashCode?
They end up in the **same bucket**. HashMap then uses `equals()` to distinguish between them. This is why both `hashCode()` and `equals()` must be correctly overridden.

### Q3: Why is the default capacity 16 and load factor 0.75?
- **16**: A power of 2 that's large enough to avoid immediate resizing for small maps
- **0.75**: Empirically determined sweet spot between collision frequency and memory usage

### Q4: Can we use a mutable object as a HashMap key?
Technically yes, but it's **extremely dangerous**. If the object's `hashCode()` changes after insertion, the entry becomes unreachable (stuck in the wrong bucket). Always use **immutable objects** (String, Integer, etc.) as keys.

### Q5: What is the maximum capacity of HashMap?
`1 << 30` = **1,073,741,824** (about 1 billion buckets). The capacity is an `int`, but it's capped at 2^30, not 2^31, because it must be a positive power of 2.

### Q6: How does HashMap differ from Hashtable?
| Feature | HashMap | Hashtable |
|---------|---------|-----------|
| Thread-safe | No | Yes (synchronized) |
| Null keys | 1 allowed | Not allowed |
| Null values | Allowed | Not allowed |
| Performance | Faster | Slower (synchronization overhead) |
| Iterator | Fail-fast | Fail-fast (Enumerator is not) |
| Superclass | AbstractMap | Dictionary (legacy) |
| Since | Java 1.2 | Java 1.0 |

### Q7: What is the time complexity of HashMap iteration?
**O(capacity + size)**, not O(size). HashMap must scan the entire bucket array, including empty buckets. This is why a HashMap with capacity 10000 but only 5 entries is slower to iterate than one with capacity 16 and 5 entries.

### Q8: Explain the difference between HashMap, LinkedHashMap, and TreeMap
| Feature | HashMap | LinkedHashMap | TreeMap |
|---------|---------|---------------|---------|
| Order | No order | Insertion/Access order | Sorted (natural/comparator) |
| Implementation | Hash table | Hash table + Linked list | Red-Black tree |
| Get/Put | O(1) | O(1) | O(log n) |
| Null keys | Yes | Yes | No (with natural ordering) |
| Use case | General purpose | LRU cache, ordered iteration | Sorted/range queries |

---

## Memory Layout

```
HashMap Object
┌───────────────────┐
│ table ────────────┼──→ Node<K,V>[] (bucket array)
│ size = 3          │    ┌──────────────────────────────────────────┐
│ threshold = 12    │    │ [0] null                                 │
│ loadFactor = 0.75 │    │ [1] null                                 │
│ modCount = 3      │    │ [2] Node(hash, "name", "John", null)     │
└───────────────────┘    │ [3] null                                 │
                         │ [4] null                                 │
                         │ [5] Node(hash, "age", 25, next)──→Node   │
                         │ [6] null                                 │
                         │ ...                                      │
                         │ [11] Node(hash, "city", "NYC", null)     │
                         │ ...                                      │
                         │ [15] null                                │
                         └──────────────────────────────────────────┘
                         ◄────────── capacity = 16 ──────────────────►
```

---

## Best Practices

1. **Specify initial capacity** if you know the expected number of entries: `new HashMap<>(expectedSize / 0.75 + 1)`
2. **Use immutable objects as keys** (String, Integer, enums)
3. **Always override both hashCode() and equals()** when using custom objects as keys
4. **Use ConcurrentHashMap** instead of `Collections.synchronizedMap()` for concurrent access
5. **Use `computeIfAbsent()`** instead of check-then-put patterns
6. **Use `getOrDefault()`** instead of null checks after `get()`
7. **Use `Map.of()`** (Java 9+) for small immutable maps
8. **Prefer `entrySet()` iteration** over `keySet()` when you need both key and value

```java
// Anti-pattern
for (String key : map.keySet()) {
    String value = map.get(key);  // Extra lookup!
}

// Better
for (Map.Entry<String, String> entry : map.entrySet()) {
    String key = entry.getKey();
    String value = entry.getValue();
}
```
