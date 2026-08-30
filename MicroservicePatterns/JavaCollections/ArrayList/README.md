# ArrayList - Deep Dive

## What is ArrayList?

`ArrayList` is a **resizable-array** implementation of the `List` interface in Java. It is part of `java.util` package and implements `List`, `RandomAccess`, `Cloneable`, and `Serializable` interfaces.

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

### Key Characteristics
- **Ordered**: Maintains insertion order
- **Indexed**: Supports random access via index (O(1))
- **Allows duplicates**: Can store duplicate elements
- **Allows null**: Can store null values
- **Not synchronized**: Not thread-safe by default
- **Dynamically resizable**: Grows automatically when capacity is exceeded

---

## How ArrayList Works Internally

### 1. Underlying Data Structure

ArrayList internally uses a **plain Java array** (`Object[]`) called `elementData` to store elements.

```java
// Inside ArrayList source code
transient Object[] elementData;  // The actual array that stores elements
private int size;                // Number of elements currently stored
```

### 2. Default Capacity and Initialization

```java
private static final int DEFAULT_CAPACITY = 10;
private static final Object[] EMPTY_ELEMENTDATA = {};
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
```

When you create an ArrayList:

```java
// Creates an empty list with initial capacity 10 (lazy initialization)
ArrayList<String> list = new ArrayList<>();

// Creates an empty list with specified initial capacity
ArrayList<String> list = new ArrayList<>(20);

// Creates a list containing elements of the specified collection
ArrayList<String> list = new ArrayList<>(existingCollection);
```

**Important**: Since Java 8, `new ArrayList<>()` creates an empty array `{}`. The array of size 10 is only allocated when the **first element is added** (lazy initialization).

### 3. How add() Works — The Resizing Mechanism

```
add("A") called
    │
    ▼
ensureCapacityInternal(size + 1)
    │
    ▼
Is current array == DEFAULTCAPACITY_EMPTY_ELEMENTDATA?
    │── Yes → minCapacity = max(DEFAULT_CAPACITY, size+1) = 10
    │── No  → minCapacity = size + 1
    │
    ▼
ensureExplicitCapacity(minCapacity)
    │
    ▼
Is minCapacity > elementData.length?
    │── No  → Do nothing, array has room
    │── Yes → grow(minCapacity)
                │
                ▼
            newCapacity = oldCapacity + (oldCapacity >> 1)   // 1.5x growth
                │
                ▼
            elementData = Arrays.copyOf(elementData, newCapacity)
                │
                ▼
            elementData[size++] = element   ← Element is placed
```

### 4. Growth Formula

```java
int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5x growth
```

| Operation | Old Capacity | New Capacity |
|-----------|-------------|-------------|
| 1st add   | 0 → 10      | 10 (default)|
| 11th add  | 10           | 15          |
| 16th add  | 15           | 22          |
| 23rd add  | 22           | 33          |
| 34th add  | 33           | 49          |

The growth factor is **1.5x** (50% increase), not 2x. This is a balance between:
- Too small growth → frequent resizing (expensive)
- Too large growth → wasted memory

### 5. Arrays.copyOf — The Expensive Operation

When resizing occurs, `Arrays.copyOf()` is called which internally uses `System.arraycopy()` — a **native method** that copies the entire old array into a new, larger array.

```java
// Pseudocode of what happens
Object[] newArray = new Object[newCapacity];
System.arraycopy(oldArray, 0, newArray, 0, size);
elementData = newArray;  // Old array becomes eligible for GC
```

This is an **O(n)** operation, but since it happens infrequently, the **amortized cost** of `add()` is **O(1)**.

---

## Time Complexity Analysis

| Operation | Average Case | Worst Case | Notes |
|-----------|-------------|------------|-------|
| `get(index)` | O(1) | O(1) | Direct array access |
| `set(index, e)` | O(1) | O(1) | Direct array access |
| `add(e)` (end) | **O(1) amortized** | O(n) | O(n) when resizing |
| `add(index, e)` | O(n) | O(n) | Shifts elements right |
| `remove(index)` | O(n) | O(n) | Shifts elements left |
| `remove(Object)` | O(n) | O(n) | Linear search + shift |
| `contains(e)` | O(n) | O(n) | Linear search |
| `indexOf(e)` | O(n) | O(n) | Linear search |
| `size()` | O(1) | O(1) | Returns `size` field |
| `isEmpty()` | O(1) | O(1) | Checks `size == 0` |
| `clear()` | O(n) | O(n) | Nullifies all elements |

---

## How add(index, element) Works

When you insert at a specific index, elements must be **shifted right**:

```
Before: [A, B, C, D, _, _, _, _, _, _]   size=4
                                          
add(1, "X")

Step 1: Shift elements from index 1 onwards, one position to the right
        System.arraycopy(elementData, 1, elementData, 2, size - 1)
        [A, B, B, C, D, _, _, _, _, _]

Step 2: Place element at index 1
        elementData[1] = "X"
        [A, X, B, C, D, _, _, _, _, _]   size=5
```

This is why **inserting at the beginning is O(n)** — every element must shift.

---

## How remove(index) Works

```
Before: [A, X, B, C, D, _, _, _, _, _]   size=5

remove(1)  → removes "X"

Step 1: Calculate number of elements to move
        numMoved = size - index - 1 = 5 - 1 - 1 = 3

Step 2: Shift elements left
        System.arraycopy(elementData, 2, elementData, 1, 3)
        [A, B, C, D, D, _, _, _, _, _]

Step 3: Null out the last element (for GC)
        elementData[--size] = null
        [A, B, C, D, null, _, _, _, _, _]   size=4
```

---

## Important Concepts

### 1. RandomAccess Interface

`ArrayList` implements `RandomAccess` — a **marker interface** (no methods) that indicates this list supports fast (O(1)) random access.

```java
if (list instanceof RandomAccess) {
    // Use index-based loop (faster for ArrayList)
    for (int i = 0; i < list.size(); i++) {
        list.get(i);
    }
} else {
    // Use iterator (faster for LinkedList)
    for (Object o : list) { }
}
```

### 2. Fail-Fast Iterator

ArrayList's iterator is **fail-fast**. It uses a `modCount` (modification count) field to detect structural modifications during iteration.

```java
// This throws ConcurrentModificationException
ArrayList<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
for (String s : list) {
    if (s.equals("B")) {
        list.remove(s);  // Structural modification during iteration!
    }
}
```

**How it works internally:**
```java
// Inside ArrayList.Itr (iterator)
int expectedModCount = modCount;  // Snapshot at iterator creation

public E next() {
    checkForComodification();  // Checks if modCount == expectedModCount
    // ...
}

final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

**Safe removal during iteration:**
```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("B")) {
        it.remove();  // Safe — uses iterator's own remove
    }
}

// Or use removeIf (Java 8+)
list.removeIf(s -> s.equals("B"));
```

### 3. trimToSize()

After many removals, the internal array may be much larger than needed. Use `trimToSize()` to reclaim memory:

```java
list.trimToSize();  // Shrinks elementData to match size
```

### 4. ensureCapacity()

If you know you'll add many elements, pre-allocate to avoid repeated resizing:

```java
ArrayList<Integer> list = new ArrayList<>();
list.ensureCapacity(10000);  // Pre-allocate for 10000 elements

// Or better, use the constructor
ArrayList<Integer> list = new ArrayList<>(10000);
```

### 5. SubList — A View, Not a Copy

```java
List<String> sub = list.subList(1, 4);
// sub is a VIEW into the original list
// Modifications to sub reflect in the original list and vice versa

sub.set(0, "Z");  // Also changes list.get(1)
list.add("New");   // Invalidates sub → ConcurrentModificationException on sub access
```

### 6. Serialization

`elementData` is marked `transient` — it is NOT serialized directly. ArrayList has custom `writeObject`/`readObject` methods that serialize only the actual elements (not empty slots), saving space.

```java
// Only 'size' elements are serialized, not the full array capacity
private void writeObject(ObjectOutputStream s) {
    s.defaultWriteObject();
    s.writeInt(size);
    for (int i = 0; i < size; i++) {
        s.writeObject(elementData[i]);
    }
}
```

---

## ArrayList vs Array

| Feature | Array | ArrayList |
|---------|-------|-----------|
| Size | Fixed | Dynamic |
| Type | Primitives + Objects | Objects only (autoboxing for primitives) |
| Performance | Slightly faster (no overhead) | Slight overhead (resizing, boxing) |
| Type Safety | Can be non-generic | Always generic |
| Dimension | Multi-dimensional | Single-dimensional |
| Length | `.length` | `.size()` |
| Memory | More efficient | Extra space for empty slots |

---

## Thread Safety

ArrayList is **not thread-safe**. For concurrent access:

```java
// Option 1: Collections.synchronizedList (wraps every method with synchronized)
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// Option 2: CopyOnWriteArrayList (better for read-heavy scenarios)
List<String> cowList = new CopyOnWriteArrayList<>();

// Option 3: Manual synchronization
synchronized(list) {
    list.add("element");
}
```

---

## Common Interview Questions

### Q1: What happens when ArrayList is full?
When size reaches capacity, a new array of **1.5x** the old capacity is created, all elements are copied to the new array using `Arrays.copyOf()`, and the old array is garbage collected.

### Q2: Why is the default capacity 10?
It's a practical trade-off. Too small (like 1) would cause frequent resizing. Too large would waste memory. 10 is a reasonable default for typical use cases.

### Q3: Can ArrayList hold primitives?
No, it stores `Object` references. Primitives are **autoboxed** (`int` → `Integer`). For performance-critical code with primitives, use specialized libraries (e.g., Eclipse Collections IntArrayList).

### Q4: What is the difference between size() and capacity?
- `size()` = number of elements actually stored
- Capacity = length of the internal array (not directly accessible)

### Q5: When to use ArrayList vs LinkedList?
- **ArrayList**: Random access, iteration, add at end → O(1)
- **LinkedList**: Frequent insertions/deletions at beginning/middle → O(1) (if you have the node reference)
- In practice, **ArrayList almost always wins** due to CPU cache locality.

### Q6: How does ArrayList.clone() work?
It performs a **shallow copy** — the array is cloned but the elements themselves are not. Both lists point to the same objects.

```java
ArrayList<StringBuilder> original = new ArrayList<>();
original.add(new StringBuilder("Hello"));

ArrayList<StringBuilder> cloned = (ArrayList<StringBuilder>) original.clone();
cloned.get(0).append(" World");

System.out.println(original.get(0)); // "Hello World" — same object!
```

---

## Memory Layout

```
ArrayList Object (on Heap)
┌─────────────────────────┐
│ elementData (reference)──┼──→ Object[] array on Heap
│ size = 4                 │    ┌───┬───┬───┬───┬──────┬──────┐
│ modCount = 4             │    │ A │ B │ C │ D │ null │ null │
└─────────────────────────┘    └───┴───┴───┴───┴──────┴──────┘
                                 0   1   2   3    4      5
                                ◄── used (size) ──►◄─ unused ─►
                                ◄──── capacity (6) ────────────►
```

---

## Best Practices

1. **Specify initial capacity** when you know the approximate size
2. **Use `trimToSize()`** after bulk removals to free memory
3. **Prefer `isEmpty()`** over `size() == 0` for readability
4. **Use `removeIf()`** instead of manual iteration + removal
5. **Avoid frequent insertions at index 0** — use `ArrayDeque` or `LinkedList` instead
6. **Use `List.of()`** (Java 9+) for immutable lists instead of `Arrays.asList()` wrapping an ArrayList
