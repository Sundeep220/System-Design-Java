# HashSet - Deep Dive

## What is HashSet?

`HashSet` is a **hash table** based implementation of the `Set` interface. It stores **unique elements** with no guaranteed order and provides **O(1) average-case** performance for add, remove, and contains.

```java
public class HashSet<E> extends AbstractSet<E>
        implements Set<E>, Cloneable, java.io.Serializable
```

### Key Characteristics
- **No duplicates**: Automatically rejects duplicate elements
- **Unordered**: Does NOT guarantee any iteration order
- **Allows one null**: Can store a single null element
- **O(1) average**: For add, remove, contains
- **Not synchronized**: Not thread-safe
- **Backed by HashMap**: Internally uses a HashMap

---

## How HashSet Works Internally

### 1. The Secret — HashSet IS a HashMap

This is the **most important interview fact** about HashSet. Internally, HashSet is just a wrapper around a `HashMap`:

```java
public class HashSet<E> {
    private transient HashMap<E, Object> map;

    // Dummy value used as the value for all entries in the backing HashMap
    private static final Object PRESENT = new Object();
}
```

Every element in the HashSet is stored as a **key** in the backing HashMap, with a **dummy constant value** `PRESENT`:

```java
// When you do:
set.add("Hello");

// Internally it does:
map.put("Hello", PRESENT);  // PRESENT is the same Object for ALL entries
```

### 2. Visual Representation

```
HashSet: {"Apple", "Banana", "Cherry"}

Internally → HashMap:
┌─────────────────────────────────────────┐
│ Key        │ Value                       │
├─────────────────────────────────────────┤
│ "Apple"    │ PRESENT (dummy Object)      │
│ "Banana"   │ PRESENT (same Object)       │
│ "Cherry"   │ PRESENT (same Object)       │
└─────────────────────────────────────────┘

All values point to the SAME Object instance (PRESENT).
```

### 3. How Each Operation Maps to HashMap

```java
// HashSet.add(e)
public boolean add(E e) {
    return map.put(e, PRESENT) == null;
    // Returns true if key was NEW (put returned null)
    // Returns false if key EXISTED (put returned old PRESENT)
}

// HashSet.remove(o)
public boolean remove(Object o) {
    return map.remove(o) == PRESENT;
}

// HashSet.contains(o)
public boolean contains(Object o) {
    return map.containsKey(o);
}

// HashSet.size()
public int size() {
    return map.size();
}

// HashSet.isEmpty()
public boolean isEmpty() {
    return map.isEmpty();
}

// HashSet.clear()
public void clear() {
    map.clear();
}

// HashSet.iterator()
public Iterator<E> iterator() {
    return map.keySet().iterator();
}
```

### 4. How Uniqueness is Enforced

When you call `add(element)`:

```
add("Apple")
    │
    ▼
Compute: hash = hash("Apple")
Compute: bucketIndex = hash & (capacity - 1)
    │
    ▼
Check bucket at table[bucketIndex]:
    │
    ├── Empty? → Insert Node("Apple", PRESENT) → return true
    │
    ├── Has entries? → Walk chain
    │       For each node:
    │           if (node.hash == hash && node.key.equals("Apple"))
    │               → Key exists! Replace value (still PRESENT)
    │               → return false (element was already present)
    │           else
    │               → Continue to next node
    │       → End of chain reached → Append new Node → return true
```

**Uniqueness depends entirely on `hashCode()` and `equals()`** — same as HashMap keys.

---

## Constructors

```java
// Default: capacity=16, loadFactor=0.75
HashSet<String> set = new HashSet<>();

// With initial capacity
HashSet<String> set = new HashSet<>(100);

// With initial capacity and load factor
HashSet<String> set = new HashSet<>(100, 0.5f);

// From existing collection
HashSet<String> set = new HashSet<>(existingList);  // Removes duplicates!
```

---

## Time Complexity

| Operation | Average | Worst Case | Notes |
|-----------|---------|------------|-------|
| `add(e)` | **O(1)** | O(n) / O(log n)* | Same as HashMap.put() |
| `remove(e)` | **O(1)** | O(n) / O(log n)* | Same as HashMap.remove() |
| `contains(e)` | **O(1)** | O(n) / O(log n)* | Same as HashMap.containsKey() |
| `size()` | **O(1)** | O(1) | |
| `isEmpty()` | **O(1)** | O(1) | |
| Iteration | **O(capacity + size)** | O(capacity + size) | Scans entire backing HashMap |

*O(log n) worst case in Java 8+ due to treeification (when many elements hash to same bucket).

---

## The hashCode() and equals() Contract for Sets

### Critical Rule

For HashSet to work correctly, your objects MUST properly implement **both** `hashCode()` and `equals()`:

```java
class Student {
    String name;
    int id;

    // If two Students are "equal", they MUST have the same hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id == student.id && Objects.equals(name, student.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
```

### What Happens Without Proper hashCode/equals

```java
class BadStudent {
    String name;
    // No hashCode() or equals() override — uses Object defaults
}

HashSet<BadStudent> set = new HashSet<>();
set.add(new BadStudent("John"));
set.add(new BadStudent("John"));  // Added as separate element!

System.out.println(set.size());  // 2! (should be 1)
// Each 'new' creates a different object with different identity hashCode
```

---

## HashSet vs LinkedHashSet vs TreeSet

| Feature | HashSet | LinkedHashSet | TreeSet |
|---------|---------|---------------|---------|
| **Backed by** | HashMap | LinkedHashMap | TreeMap |
| **Order** | None | Insertion order | Sorted order |
| **Add/Remove/Contains** | O(1) | O(1) | O(log n) |
| **Null elements** | 1 allowed | 1 allowed | Not allowed* |
| **Iteration** | O(cap + size) | O(size) | O(size) |
| **Memory** | Low | Medium | High |

*TreeSet allows null only with a Comparator that handles null.

---

## Important Operations and Patterns

### Set Operations (Union, Intersection, Difference)

```java
Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
Set<Integer> setB = new HashSet<>(Arrays.asList(3, 4, 5, 6, 7));

// Union (A ∪ B)
Set<Integer> union = new HashSet<>(setA);
union.addAll(setB);
// {1, 2, 3, 4, 5, 6, 7}

// Intersection (A ∩ B)
Set<Integer> intersection = new HashSet<>(setA);
intersection.retainAll(setB);
// {3, 4, 5}

// Difference (A - B)
Set<Integer> difference = new HashSet<>(setA);
difference.removeAll(setB);
// {1, 2}

// Symmetric Difference (A △ B) — elements in either but not both
Set<Integer> symDiff = new HashSet<>(setA);
symDiff.addAll(setB);
Set<Integer> temp = new HashSet<>(setA);
temp.retainAll(setB);
symDiff.removeAll(temp);
// {1, 2, 6, 7}
```

### Removing Duplicates from a List

```java
List<String> listWithDups = Arrays.asList("A", "B", "A", "C", "B", "D");
List<String> unique = new ArrayList<>(new HashSet<>(listWithDups));
// [A, B, C, D] (order not guaranteed)

// Preserve order:
List<String> uniqueOrdered = new ArrayList<>(new LinkedHashSet<>(listWithDups));
// [A, B, C, D] (insertion order preserved)
```

### Checking Subset/Superset

```java
Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
Set<Integer> setB = new HashSet<>(Arrays.asList(2, 3));

setA.containsAll(setB);  // true — B is a subset of A
```

---

## Fail-Fast Iterator

```java
Set<String> set = new HashSet<>(Arrays.asList("A", "B", "C"));

// WRONG — ConcurrentModificationException
for (String s : set) {
    set.remove(s);
}

// CORRECT
Iterator<String> it = set.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (s.equals("B")) {
        it.remove();  // Safe
    }
}

// CORRECT (Java 8+)
set.removeIf(s -> s.equals("B"));
```

---

## Common Interview Questions

### Q1: How does HashSet ensure uniqueness?
It uses the backing HashMap. Elements are stored as HashMap keys. When `add()` is called, `HashMap.put(element, PRESENT)` is invoked. If the key already exists (determined by `hashCode()` + `equals()`), the existing entry is returned and `add()` returns `false`.

### Q2: What is the internal implementation of HashSet?
HashSet is backed by a `HashMap<E, Object>`. Every element is a key in the HashMap. The value is always the same dummy `Object` called `PRESENT`.

### Q3: Can HashSet contain null?
Yes, **one null**. It's stored at bucket 0 in the backing HashMap (since `hash(null) = 0`).

### Q4: How does HashSet handle collisions?
Same as HashMap — separate chaining with linked lists, converting to Red-Black trees when chain length ≥ 8 (Java 8+).

### Q5: What is the difference between HashSet.add() returning true vs false?
- `true`: Element was NOT already present — it was added
- `false`: Element was already present — the set is unchanged

### Q6: Why is the value in HashSet's backing HashMap a constant Object?
To save memory. Instead of storing different value objects for each entry, all entries share the same `PRESENT` object. Only the keys (set elements) matter.

### Q7: Which is faster to check membership — HashSet or ArrayList?
**HashSet** — `contains()` is O(1) vs ArrayList's O(n). For lookups, always prefer HashSet.

---

## Memory Overhead

```
Each element in HashSet requires:
  1 HashMap.Node object:
    - int hash:        4 bytes
    - Object key:      4-8 bytes (reference)
    - Object value:    4-8 bytes (reference to PRESENT — shared)
    - Node next:       4-8 bytes (reference)
    - Object header:   12-16 bytes
    Total per entry:   ~32-48 bytes

vs. ArrayList: ~4-8 bytes per element (just the reference)

HashSet uses ~5-10x more memory than ArrayList per element.
```

---

## Thread Safety

```java
// Option 1: Synchronized wrapper
Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());

// Option 2: CopyOnWriteArraySet (for small, read-heavy sets)
Set<String> cowSet = new CopyOnWriteArraySet<>();

// Option 3: ConcurrentHashMap.newKeySet() (for large concurrent sets)
Set<String> concSet = ConcurrentHashMap.newKeySet();
```

---

## Best Practices

1. **Always override hashCode() and equals()** for custom objects stored in HashSet
2. **Use HashSet for fast lookups** — O(1) contains vs O(n) for List
3. **Specify initial capacity** when size is known: `new HashSet<>(expectedSize / 0.75 + 1)`
4. **Use Set.of()** (Java 9+) for small immutable sets
5. **Use LinkedHashSet** when you need predictable iteration order
6. **Use TreeSet** when you need sorted elements
7. **Don't use mutable objects** as set elements — if hashCode changes after insertion, the element becomes "lost"
8. **Use `removeIf()`** instead of manual iterator removal (Java 8+)
