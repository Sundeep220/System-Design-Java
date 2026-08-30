# TreeMap - Deep Dive

## What is TreeMap?

`TreeMap` is a **Red-Black tree** based implementation of the `NavigableMap` interface. It stores key-value pairs in **sorted order** of keys.

```java
public class TreeMap<K,V> extends AbstractMap<K,V>
        implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

### Key Characteristics
- **Sorted**: Keys are always in sorted order (natural ordering or custom Comparator)
- **O(log n)**: All basic operations — get, put, remove
- **No null keys**: Throws `NullPointerException` (with natural ordering)
- **Allows null values**: Multiple null values allowed
- **Not synchronized**: Not thread-safe
- **NavigableMap**: Supports range queries, floor, ceiling, higher, lower

---

## How TreeMap Works Internally

### 1. Red-Black Tree Basics

A Red-Black tree is a **self-balancing Binary Search Tree (BST)** with the following properties:

1. **Every node is either RED or BLACK**
2. **Root is always BLACK**
3. **Every leaf (null/NIL) is BLACK**
4. **If a node is RED, both its children must be BLACK** (no two consecutive red nodes)
5. **Every path from root to a leaf has the same number of BLACK nodes** (black-height)

These properties guarantee that the tree height is at most **2 * log₂(n+1)**, ensuring O(log n) operations.

### 2. Entry (Node) Structure

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;      // Left child
    Entry<K,V> right;     // Right child
    Entry<K,V> parent;    // Parent node
    boolean color = BLACK; // Node color (RED or BLACK)
}
```

### 3. Visual Representation

```
TreeMap with keys: 10, 5, 15, 3, 7, 12, 20

              10 (BLACK)
             /          \
        5 (RED)        15 (RED)
       /     \        /      \
    3(BLK)  7(BLK)  12(BLK)  20(BLK)

In-order traversal: 3, 5, 7, 10, 12, 15, 20  ← Always sorted!
```

### 4. Self-Balancing Operations

When inserting or deleting, the tree may violate Red-Black properties. It fixes itself using:

#### Rotations

```
LEFT ROTATION (around node X):         RIGHT ROTATION (around node Y):

      X                Y                     Y                X
     / \              / \                   / \              / \
    a   Y    →→→     X   c                X   c    →→→     a   Y
       / \          / \                   / \                  / \
      b   c        a   b                a   b                b   c
```

#### Recoloring

Simply flipping the color of nodes from RED to BLACK or vice versa.

#### Insertion Fix-Up Cases

After inserting a RED node, there are 3 main cases to fix:

```
Case 1: Uncle is RED → Recolor parent, uncle, grandparent
Case 2: Uncle is BLACK, node is inner child → Rotate to make Case 3
Case 3: Uncle is BLACK, node is outer child → Rotate + Recolor
```

---

## How put() Works

```
put(key, value):
    │
    ▼
Step 1: If tree is empty
    → Create root node (BLACK)
    │
    ▼
Step 2: BST insertion (find correct position)
    → Compare key with current node using compareTo() or Comparator
    → Go left if key < current, right if key > current
    → If key equals current → replace value, return old value
    → Insert new node as RED leaf
    │
    ▼
Step 3: Fix Red-Black violations (fixAfterInsertion)
    → Perform rotations and recoloring as needed
```

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {
        compare(key, key); // Type check (even null check)
        root = new Entry<>(key, value, null);
        size = 1;
        modCount++;
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        // Use provided Comparator
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)       t = t.left;
            else if (cmp > 0)  t = t.right;
            else               return t.setValue(value);  // Key exists, update
        } while (t != null);
    } else {
        // Use natural ordering (Comparable)
        // Similar loop using ((Comparable<? super K>)key).compareTo(t.key)
    }
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        parent.left = e;
    else
        parent.right = e;
    fixAfterInsertion(e);  // Balance the tree
    size++;
    modCount++;
    return null;
}
```

---

## NavigableMap Operations

TreeMap implements `NavigableMap`, providing powerful navigation methods:

### Floor, Ceiling, Higher, Lower

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "A");
map.put(20, "B");
map.put(30, "C");
map.put(40, "D");

map.floorKey(25);     // 20  (greatest key ≤ 25)
map.ceilingKey(25);   // 30  (smallest key ≥ 25)
map.lowerKey(30);     // 20  (greatest key < 30)
map.higherKey(30);    // 40  (smallest key > 30)

map.floorEntry(25);   // 20=B
map.ceilingEntry(25); // 30=C

map.firstKey();       // 10  (smallest key)
map.lastKey();        // 40  (largest key)
map.firstEntry();     // 10=A
map.lastEntry();      // 40=D

map.pollFirstEntry(); // Removes and returns 10=A
map.pollLastEntry();  // Removes and returns 40=D
```

### SubMap, HeadMap, TailMap (Range Views)

```java
TreeMap<Integer, String> map = new TreeMap<>();
// map: {10=A, 20=B, 30=C, 40=D, 50=E}

// subMap(fromKey inclusive, toKey exclusive)
map.subMap(20, 40);              // {20=B, 30=C}
map.subMap(20, true, 40, true);  // {20=B, 30=C, 40=D}  (inclusive bounds)

// headMap(toKey exclusive)
map.headMap(30);                 // {10=A, 20=B}
map.headMap(30, true);           // {10=A, 20=B, 30=C}

// tailMap(fromKey inclusive)
map.tailMap(30);                 // {30=C, 40=D, 50=E}
map.tailMap(30, false);          // {40=D, 50=E}

// descendingMap (reverse order view)
NavigableMap<Integer, String> desc = map.descendingMap();
// {50=E, 40=D, 30=C, 20=B, 10=A}
```

**Important**: These return **views**, not copies. Changes in the view reflect in the original map and vice versa.

---

## Comparator vs Comparable

### Natural Ordering (Comparable)

```java
TreeMap<String, Integer> map = new TreeMap<>();  // Uses String's natural ordering
map.put("Banana", 2);
map.put("Apple", 1);
map.put("Cherry", 3);
// Iteration: Apple=1, Banana=2, Cherry=3  (alphabetical)
```

### Custom Ordering (Comparator)

```java
// Reverse order
TreeMap<String, Integer> map = new TreeMap<>(Comparator.reverseOrder());
map.put("Banana", 2);
map.put("Apple", 1);
map.put("Cherry", 3);
// Iteration: Cherry=3, Banana=2, Apple=1

// Case-insensitive ordering
TreeMap<String, Integer> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

// Custom object ordering
TreeMap<Employee, String> map = new TreeMap<>(Comparator.comparing(Employee::getSalary));
```

### Comparator Consistency with equals()

If `comparator.compare(a, b) == 0` but `!a.equals(b)`, the TreeMap considers them the same key. This can cause unexpected behavior:

```java
TreeMap<String, Integer> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
map.put("Hello", 1);
map.put("HELLO", 2);  // Replaces "Hello"! Because comparator treats them as equal
System.out.println(map.size()); // 1
```

---

## Time Complexity

| Operation | Time | Notes |
|-----------|------|-------|
| `put(key, value)` | **O(log n)** | BST traversal + rebalancing |
| `get(key)` | **O(log n)** | BST traversal |
| `remove(key)` | **O(log n)** | BST traversal + rebalancing |
| `containsKey(key)` | **O(log n)** | BST traversal |
| `containsValue(value)` | **O(n)** | Must scan all nodes |
| `firstKey()` / `lastKey()` | **O(log n)** | Walk left/right to leaf |
| `floorKey()` / `ceilingKey()` | **O(log n)** | BST traversal |
| `subMap()` / `headMap()` / `tailMap()` | **O(log n)** | View creation is O(log n), iteration is O(k) |
| Iteration | **O(n)** | In-order traversal |

---

## TreeMap vs HashMap vs LinkedHashMap

| Feature | HashMap | LinkedHashMap | TreeMap |
|---------|---------|---------------|---------|
| **Data Structure** | Hash table | Hash table + linked list | Red-Black tree |
| **Order** | None | Insertion/Access order | Sorted order |
| **Get/Put** | O(1) | O(1) | O(log n) |
| **Null keys** | 1 allowed | 1 allowed | Not allowed* |
| **Null values** | Yes | Yes | Yes |
| **Memory** | Low | Medium | High |
| **Range queries** | No | No | Yes |
| **Use case** | General | LRU cache | Sorted/range |

*TreeMap allows null keys only if Comparator explicitly handles null.

---

## Common Interview Questions

### Q1: What is the underlying data structure of TreeMap?
**Red-Black tree** — a self-balancing BST that guarantees O(log n) height by enforcing color properties and using rotations/recoloring to maintain balance.

### Q2: Why can't TreeMap have null keys (by default)?
Because it calls `compareTo()` or `compare()` on keys during insertion, which throws `NullPointerException` for null. A custom Comparator that handles null can bypass this.

### Q3: What happens if key objects are not Comparable and no Comparator is provided?
`ClassCastException` is thrown at runtime when `put()` is called, because the TreeMap tries to cast the key to `Comparable`.

### Q4: How does TreeMap guarantee O(log n)?
The Red-Black tree properties ensure the tree height never exceeds 2 * log₂(n+1). After each insert/delete, the tree rebalances using at most O(log n) rotations and recoloring.

### Q5: What's the difference between subMap(), headMap(), and tailMap()?
- `subMap(from, to)`: Keys in range [from, to)
- `headMap(to)`: Keys less than `to`
- `tailMap(from)`: Keys greater than or equal to `from`
All return **live views** — modifications are bidirectional.

### Q6: When should you use TreeMap over HashMap?
- When you need **sorted iteration**
- When you need **range queries** (subMap, headMap, tailMap)
- When you need **floor/ceiling/higher/lower** operations
- When the slight overhead of O(log n) vs O(1) is acceptable

---

## Best Practices

1. **Use TreeMap when you need sorted keys** — don't sort a HashMap's keys manually
2. **Provide a Comparator** for custom objects instead of implementing Comparable (more flexible)
3. **Ensure Comparator consistency with equals()** to avoid unexpected behavior
4. **Use NavigableMap methods** (floor, ceiling, etc.) — they're O(log n) and purpose-built
5. **Use subMap/headMap/tailMap** for range operations instead of manual filtering
6. **Don't use TreeMap for O(1) lookups** — use HashMap instead
7. **Use `descendingMap()`** for reverse iteration instead of sorting manually
