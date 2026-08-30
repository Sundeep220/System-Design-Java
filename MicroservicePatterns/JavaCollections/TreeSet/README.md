# TreeSet - Deep Dive

## What is TreeSet?

`TreeSet` is a **Red-Black tree** based implementation of the `NavigableSet` interface. It stores elements in **sorted order** and provides **O(log n)** performance for all basic operations.

```java
public class TreeSet<E> extends AbstractSet<E>
        implements NavigableSet<E>, Cloneable, java.io.Serializable
```

### Key Characteristics
- **Sorted**: Elements are always in natural order or Comparator-defined order
- **No duplicates**: Rejects duplicates (based on compareTo/compare, NOT equals)
- **No null**: Throws `NullPointerException` (with natural ordering)
- **O(log n)**: For add, remove, contains
- **Not synchronized**: Not thread-safe
- **Backed by TreeMap**: Internally uses a TreeMap

---

## How TreeSet Works Internally

### 1. TreeSet IS a TreeMap

Just like HashSet is backed by HashMap, **TreeSet is backed by TreeMap**:

```java
public class TreeSet<E> {
    private transient NavigableMap<E, Object> m;  // The backing TreeMap

    private static final Object PRESENT = new Object();  // Dummy value

    public TreeSet() {
        this(new TreeMap<>());
    }

    public boolean add(E e) {
        return m.put(e, PRESENT) == null;
    }

    public boolean contains(Object o) {
        return m.containsKey(o);
    }

    public boolean remove(Object o) {
        return m.remove(o) == PRESENT;
    }
}
```

### 2. Visual Representation

```
TreeSet: {5, 10, 15, 20, 25}

Internally → TreeMap (Red-Black Tree):

              15 (BLACK)
             /          \
        10 (RED)       20 (RED)
       /               /      \
    5(BLK)          (nil)    25(BLK)

In-order traversal: 5, 10, 15, 20, 25  ← Always sorted!

Each node stores: key=element, value=PRESENT (dummy)
```

---

## Ordering: Comparable vs Comparator

### Natural Ordering (Comparable)

```java
TreeSet<Integer> set = new TreeSet<>();
set.add(30);
set.add(10);
set.add(20);
System.out.println(set);  // [10, 20, 30]  (sorted by Integer.compareTo)

TreeSet<String> strSet = new TreeSet<>();
strSet.add("Banana");
strSet.add("Apple");
strSet.add("Cherry");
System.out.println(strSet);  // [Apple, Banana, Cherry]  (alphabetical)
```

### Custom Ordering (Comparator)

```java
// Reverse order
TreeSet<Integer> set = new TreeSet<>(Comparator.reverseOrder());
set.addAll(Arrays.asList(10, 20, 30));
System.out.println(set);  // [30, 20, 10]

// Case-insensitive string ordering
TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

// Custom objects
TreeSet<Employee> set = new TreeSet<>(Comparator.comparing(Employee::getName)
                                                .thenComparingInt(Employee::getAge));
```

### Critical: compareTo Determines Equality in TreeSet

```java
class Student implements Comparable<Student> {
    String name;
    int grade;

    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.grade, other.grade);
    }
}

TreeSet<Student> set = new TreeSet<>();
set.add(new Student("Alice", 90));
set.add(new Student("Bob", 90));   // NOT added! compareTo returns 0

System.out.println(set.size());  // 1 — Bob is considered "equal" to Alice
```

**TreeSet uses `compareTo()`/`compare()` for equality, NOT `equals()`**. If `compareTo` returns 0, TreeSet treats the elements as duplicates.

---

## NavigableSet Operations

TreeSet provides powerful navigation methods inherited from `NavigableSet`:

### Floor, Ceiling, Higher, Lower

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50));

set.floor(25);     // 20  (greatest element ≤ 25)
set.ceiling(25);   // 30  (smallest element ≥ 25)
set.lower(30);     // 20  (greatest element < 30)
set.higher(30);    // 40  (smallest element > 30)

set.first();       // 10  (smallest element)
set.last();        // 50  (largest element)

set.pollFirst();   // Removes and returns 10
set.pollLast();    // Removes and returns 50
```

### SubSet, HeadSet, TailSet (Range Views)

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50));

// subSet(fromInclusive, toExclusive)
set.subSet(20, 40);              // [20, 30]
set.subSet(20, true, 40, true);  // [20, 30, 40]

// headSet(toExclusive)
set.headSet(30);                 // [10, 20]
set.headSet(30, true);           // [10, 20, 30]

// tailSet(fromInclusive)
set.tailSet(30);                 // [30, 40, 50]
set.tailSet(30, false);          // [40, 50]

// descendingSet (reverse order view)
NavigableSet<Integer> desc = set.descendingSet();
System.out.println(desc);  // [50, 40, 30, 20, 10]

// descendingIterator
Iterator<Integer> descIt = set.descendingIterator();
```

**Important**: subSet, headSet, tailSet return **views** — modifications reflect bidirectionally. Adding an element outside the view's range throws `IllegalArgumentException`.

```java
SortedSet<Integer> sub = set.subSet(20, 40);
sub.add(25);  // OK — reflected in original set
sub.add(50);  // IllegalArgumentException — outside range [20, 40)
```

---

## Time Complexity

| Operation | Time | Notes |
|-----------|------|-------|
| `add(e)` | **O(log n)** | BST traversal + rebalancing |
| `remove(e)` | **O(log n)** | BST traversal + rebalancing |
| `contains(e)` | **O(log n)** | BST traversal |
| `first()` / `last()` | **O(log n)** | Walk to leftmost/rightmost |
| `floor()` / `ceiling()` | **O(log n)** | BST traversal |
| `lower()` / `higher()` | **O(log n)** | BST traversal |
| `subSet()` / `headSet()` / `tailSet()` | **O(log n)** | View creation |
| Iteration | **O(n)** | In-order traversal |
| `size()` | **O(1)** | Maintained as field |

---

## TreeSet vs HashSet vs LinkedHashSet

| Feature | HashSet | LinkedHashSet | TreeSet |
|---------|---------|---------------|---------|
| **Backed by** | HashMap | LinkedHashMap | TreeMap |
| **Order** | None | Insertion order | Sorted order |
| **Performance** | O(1) | O(1) | O(log n) |
| **Null** | 1 allowed | 1 allowed | Not allowed |
| **Equality** | equals() | equals() | compareTo() |
| **Navigation** | No | No | Yes (floor, ceiling, etc.) |
| **Range queries** | No | No | Yes (subSet, headSet, etc.) |

---

## Common Use Cases

### 1. Maintaining a Sorted Collection

```java
TreeSet<Integer> scores = new TreeSet<>();
scores.add(85);
scores.add(92);
scores.add(78);
scores.add(95);
scores.add(88);

System.out.println("Sorted scores: " + scores);  // [78, 85, 88, 92, 95]
System.out.println("Highest: " + scores.last());   // 95
System.out.println("Lowest: " + scores.first());   // 78
```

### 2. Finding Nearest Values

```java
TreeSet<Integer> pricePoints = new TreeSet<>(Arrays.asList(100, 250, 500, 750, 1000));

int budget = 600;
System.out.println("Best price ≤ budget: " + pricePoints.floor(budget));    // 500
System.out.println("Next price > budget: " + pricePoints.higher(budget));   // 750
```

### 3. Range Queries

```java
TreeSet<LocalDate> dates = new TreeSet<>();
// ... add dates

// Get all dates in January 2024
NavigableSet<LocalDate> january = dates.subSet(
    LocalDate.of(2024, 1, 1), true,
    LocalDate.of(2024, 1, 31), true
);
```

### 4. Removing Duplicates While Sorting

```java
List<Integer> list = Arrays.asList(5, 3, 8, 1, 3, 5, 7, 1);
TreeSet<Integer> sortedUnique = new TreeSet<>(list);
System.out.println(sortedUnique);  // [1, 3, 5, 7, 8]
```

---

## Common Interview Questions

### Q1: How does TreeSet internally work?
TreeSet is backed by a **TreeMap** (Red-Black tree). Elements are stored as keys in the TreeMap with a dummy constant value. All operations delegate to TreeMap's key operations.

### Q2: How does TreeSet determine equality?
Using **`compareTo()` (natural ordering) or `compare()` (Comparator)**, NOT `equals()`. If `compareTo` returns 0, TreeSet considers elements equal, even if `equals()` returns false. This is a common interview trap.

### Q3: Why doesn't TreeSet allow null?
Because `compareTo()` or `compare()` would throw `NullPointerException` when comparing with null. A custom Comparator that handles null can technically allow it.

### Q4: What happens if elements are not Comparable and no Comparator is given?
`ClassCastException` at runtime when `add()` is called, because TreeSet tries to cast the element to `Comparable`.

### Q5: What is the difference between floor() and lower()?
- `floor(e)`: Greatest element **≤ e** (inclusive)
- `lower(e)`: Greatest element **< e** (exclusive)
- Similarly: `ceiling(e)` ≥ e, `higher(e)` > e

### Q6: Can TreeSet store custom objects?
Yes, but they must either:
1. Implement `Comparable<T>` (natural ordering), or
2. A `Comparator<T>` must be provided to the TreeSet constructor

---

## Best Practices

1. **Use TreeSet when you need sorted, unique elements** — don't sort a HashSet manually
2. **Ensure compareTo is consistent with equals** to avoid subtle bugs
3. **Use Comparator over Comparable** for flexibility (can have multiple sort orders)
4. **Leverage NavigableSet methods** (floor, ceiling, subSet) — they're purpose-built and efficient
5. **Use HashSet for pure membership testing** — O(1) beats O(log n) if you don't need sorting
6. **Be careful with mutable elements** — if an element's compareTo result changes after insertion, the tree becomes inconsistent
7. **Use `descendingSet()`** for reverse iteration instead of creating a new set
