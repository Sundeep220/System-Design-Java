# PriorityQueue - Deep Dive

## What is PriorityQueue?

`PriorityQueue` is an **unbounded priority heap** implementation of the `Queue` interface. It orders elements based on their **natural ordering** or a **Comparator**, ensuring the element with the highest priority (smallest value by default) is always at the head.

```java
public class PriorityQueue<E> extends AbstractQueue<E>
        implements java.io.Serializable
```

### Key Characteristics
- **Min-Heap by default**: Smallest element is always at the head
- **Not sorted**: Only guarantees the head is the min (or max with custom Comparator)
- **No null elements**: Throws `NullPointerException`
- **O(log n) insert and remove**: Heap operations
- **O(1) peek**: Access the head element
- **Unbounded**: Grows dynamically (like ArrayList)
- **Not synchronized**: Not thread-safe
- **No random access**: Cannot efficiently access elements by index

---

## How PriorityQueue Works Internally

### 1. Underlying Data Structure — Binary Heap as Array

PriorityQueue uses a **binary min-heap** stored in a **plain array**:

```java
transient Object[] queue;  // The heap array
int size;                   // Number of elements
```

### 2. Heap Property (Min-Heap)

For every node at index `i`:
- **Parent index**: `(i - 1) / 2` or `(i - 1) >>> 1`
- **Left child index**: `2 * i + 1`
- **Right child index**: `2 * i + 2`
- **Heap property**: `queue[parent] ≤ queue[child]`

```
Array representation: [1, 3, 2, 7, 5, 4, 6]

Tree visualization:
              1           (index 0)
           /     \
         3         2      (index 1, 2)
        / \       / \
       7   5     4   6    (index 3, 4, 5, 6)

Parent of index 4 (value 5): (4-1)/2 = index 1 (value 3) ✓  3 ≤ 5
Left child of index 1 (value 3): 2*1+1 = index 3 (value 7) ✓  3 ≤ 7
Right child of index 1 (value 3): 2*1+2 = index 4 (value 5) ✓  3 ≤ 5
```

### 3. Why Array Instead of Tree with Pointers?

- **Cache-friendly**: Contiguous memory, great CPU cache performance
- **No pointer overhead**: No left/right/parent references per node
- **Index arithmetic**: Parent/child access via simple math (O(1))
- **Memory efficient**: Just the array + size field

---

## How offer() / add() Works — Sift Up

```
offer(element):
    │
    ▼
Step 1: Place element at the END of the array (index = size)
Step 2: "Sift Up" — Compare with parent, swap if smaller
Step 3: Repeat until heap property is restored or root is reached
```

```java
private void siftUp(int k, E x) {
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = queue[parent];
        if (comparator.compare(x, (E) e) >= 0)
            break;                      // Heap property satisfied
        queue[k] = e;                   // Move parent down
        k = parent;                     // Move up
    }
    queue[k] = x;
}
```

### Example: Adding 0 to [1, 3, 2, 7, 5, 4, 6]

```
Step 1: Place 0 at index 7
[1, 3, 2, 7, 5, 4, 6, 0]
              1
           /     \
         3         2
        / \       / \
       7   5     4   6
      /
     0  ← New element

Step 2: Sift Up
  Compare 0 with parent (index 3, value 7): 0 < 7 → Swap
  [1, 3, 2, 0, 5, 4, 6, 7]
              1
           /     \
         3         2
        / \       / \
       0   5     4   6
      /
     7

  Compare 0 with parent (index 1, value 3): 0 < 3 → Swap
  [1, 0, 2, 3, 5, 4, 6, 7]
              1
           /     \
         0         2
        / \       / \
       3   5     4   6
      /
     7

  Compare 0 with parent (index 0, value 1): 0 < 1 → Swap
  [0, 1, 2, 3, 5, 4, 6, 7]
              0           ← 0 is now the root (minimum)
           /     \
         1         2
        / \       / \
       3   5     4   6
      /
     7

Time: O(log n) — at most height of the tree
```

---

## How poll() / remove() Works — Sift Down

```
poll():
    │
    ▼
Step 1: Save the root element (this is the minimum)
Step 2: Move the LAST element to the root position
Step 3: "Sift Down" — Compare with children, swap with the SMALLER child
Step 4: Repeat until heap property is restored or leaf is reached
Step 5: Return the saved root element
```

```java
private void siftDown(int k, E x) {
    int half = size >>> 1;          // Loop while a non-leaf
    while (k < half) {
        int child = (k << 1) + 1;  // Left child
        Object c = queue[child];
        int right = child + 1;
        if (right < size && comparator.compare((E) c, (E) queue[right]) > 0)
            c = queue[child = right]; // Choose smaller child
        if (comparator.compare(x, (E) c) <= 0)
            break;                    // Heap property satisfied
        queue[k] = c;                 // Move child up
        k = child;                    // Move down
    }
    queue[k] = x;
}
```

### Example: Polling from [0, 1, 2, 3, 5, 4, 6, 7]

```
Step 1: Save root (0)
Step 2: Move last element (7) to root
[7, 1, 2, 3, 5, 4, 6]
              7           ← Last element at root
           /     \
         1         2
        / \       / \
       3   5     4   6

Step 3: Sift Down
  Children of 7: left=1, right=2. Smaller child = 1. 7 > 1 → Swap
  [1, 7, 2, 3, 5, 4, 6]
              1
           /     \
         7         2
        / \       / \
       3   5     4   6

  Children of 7: left=3, right=5. Smaller child = 3. 7 > 3 → Swap
  [1, 3, 2, 7, 5, 4, 6]
              1
           /     \
         3         2
        / \       / \
       7   5     4   6

  7 is now a leaf. Stop.

Return: 0
```

---

## Growth Strategy

```java
private static final int DEFAULT_INITIAL_CAPACITY = 11;

private void grow(int minCapacity) {
    int oldCapacity = queue.length;
    // Double for small sizes, grow by 50% for larger
    int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                     (oldCapacity + 2) :
                                     (oldCapacity >> 1));
    queue = Arrays.copyOf(queue, newCapacity);
}
```

| Old Capacity | Growth | New Capacity |
|-------------|--------|-------------|
| 11 | +13 (double+2) | 24 |
| 24 | +26 | 50 |
| 50 | +52 | 102 |
| 102 | +51 (50%) | 153 |
| 153 | +76 | 229 |

---

## Time Complexity

| Operation | Time | Notes |
|-----------|------|-------|
| `offer(e)` / `add(e)` | **O(log n)** | Sift up |
| `poll()` / `remove()` | **O(log n)** | Sift down |
| `peek()` | **O(1)** | Direct array access (index 0) |
| `remove(Object)` | **O(n)** | Linear search + sift |
| `contains(Object)` | **O(n)** | Linear search |
| `size()` | **O(1)** | |
| `toArray()` | **O(n)** | |
| Iteration | **O(n)** | But NOT in sorted order! |

**Critical**: Iterating over a PriorityQueue does NOT give elements in sorted order. The iterator traverses the backing array, which is a heap — only partially ordered.

```java
PriorityQueue<Integer> pq = new PriorityQueue<>(Arrays.asList(5, 1, 3, 2, 4));

// WRONG — not guaranteed to be sorted
for (int x : pq) {
    System.out.print(x + " ");  // Might print: 1 2 3 5 4 (heap order, not sorted)
}

// CORRECT — poll gives sorted order
while (!pq.isEmpty()) {
    System.out.print(pq.poll() + " ");  // Always: 1 2 3 4 5
}
```

---

## Min-Heap vs Max-Heap

### Default (Min-Heap)

```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.addAll(Arrays.asList(5, 1, 3, 2, 4));
minHeap.poll();  // Returns 1 (smallest)
```

### Max-Heap

```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
maxHeap.addAll(Arrays.asList(5, 1, 3, 2, 4));
maxHeap.poll();  // Returns 5 (largest)

// Or equivalently:
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
```

### Custom Priority

```java
// Priority by string length (shorter = higher priority)
PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(String::length));
pq.addAll(Arrays.asList("Banana", "Hi", "Apple", "A"));
pq.poll();  // "A" (length 1)
pq.poll();  // "Hi" (length 2)

// Task scheduler — higher priority number = dequeue first
PriorityQueue<Task> taskQueue = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority).reversed()
);
```

---

## Common Use Cases

### 1. Top-K Elements

```java
// Find the K largest elements (use min-heap of size K)
public List<Integer> topK(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll();  // Remove smallest — only K largest remain
        }
    }
    return new ArrayList<>(minHeap);
}
```

### 2. Merge K Sorted Lists

```java
public List<Integer> mergeKSorted(List<List<Integer>> lists) {
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    // a[0] = value, a[1] = list index, a[2] = element index

    for (int i = 0; i < lists.size(); i++) {
        if (!lists.get(i).isEmpty()) {
            pq.offer(new int[]{lists.get(i).get(0), i, 0});
        }
    }

    List<Integer> result = new ArrayList<>();
    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        result.add(curr[0]);
        int nextIdx = curr[2] + 1;
        if (nextIdx < lists.get(curr[1]).size()) {
            pq.offer(new int[]{lists.get(curr[1]).get(nextIdx), curr[1], nextIdx});
        }
    }
    return result;
}
```

### 3. Dijkstra's Shortest Path

```java
PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
pq.offer(new int[]{source, 0});  // [node, distance]

while (!pq.isEmpty()) {
    int[] curr = pq.poll();
    int node = curr[0], dist = curr[1];
    // Process neighbors...
    pq.offer(new int[]{neighbor, dist + weight});
}
```

### 4. Running Median

```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder()); // Lower half
PriorityQueue<Integer> minHeap = new PriorityQueue<>();  // Upper half

// Balance: maxHeap.size() == minHeap.size() or maxHeap.size() == minHeap.size() + 1
// Median is always maxHeap.peek() (or average of both peeks)
```

---

## PriorityQueue vs TreeSet

| Feature | PriorityQueue | TreeSet |
|---------|---------------|---------|
| Duplicates | Allowed | Not allowed |
| Access | Only head (min/max) | Any element |
| Remove arbitrary | O(n) | O(log n) |
| Navigation | No | Yes (floor, ceiling) |
| Iteration order | NOT sorted | Sorted |
| Use case | Process by priority | Sorted unique collection |

---

## Common Interview Questions

### Q1: What is the internal data structure?
**Binary min-heap** stored as an **array**. Parent at index `i`, left child at `2i+1`, right child at `2i+2`.

### Q2: Does iterating over PriorityQueue give sorted output?
**No!** The iterator traverses the array in heap order, which is only partially ordered. Use repeated `poll()` for sorted output.

### Q3: Can PriorityQueue contain null?
**No.** `NullPointerException` is thrown because null cannot be compared.

### Q4: Can PriorityQueue contain duplicates?
**Yes.** Unlike TreeSet, PriorityQueue allows duplicate elements.

### Q5: How to create a max-heap?
Use `new PriorityQueue<>(Comparator.reverseOrder())` or `new PriorityQueue<>(Collections.reverseOrder())`.

### Q6: What is the time complexity of remove(Object)?
**O(n)** — it must perform a linear search to find the element, then O(log n) to sift. Total: O(n).

### Q7: What is the difference between offer() and add()?
Functionally identical for PriorityQueue (since it's unbounded). `add()` throws exception on failure (for bounded queues), `offer()` returns false. For PriorityQueue, both always succeed.

### Q8: How is PriorityQueue different from sorting?
- Sorting: O(n log n) upfront, then O(1) access to any rank
- PriorityQueue: O(log n) per insert/remove, O(1) for min/max only
- Use PriorityQueue when you only need the min/max element repeatedly, not full sorted access

---

## Thread-Safe Alternative

```java
// PriorityBlockingQueue — thread-safe, unbounded
PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
taskQueue.put(task);   // Never blocks (unbounded)
taskQueue.take();      // Blocks if empty
```

---

## Best Practices

1. **Always specify a Comparator** for custom objects — don't rely on Comparable alone
2. **Use poll() for sorted output**, never iterate directly
3. **Pre-size the queue** if you know the approximate size: `new PriorityQueue<>(expectedSize)`
4. **Use for "streaming top-K"** problems — maintain a heap of size K
5. **Prefer PriorityQueue over TreeSet** when duplicates are needed or you only access min/max
6. **Don't use for random access** — contains() and remove(Object) are O(n)
7. **Use PriorityBlockingQueue** for concurrent producer-consumer patterns
