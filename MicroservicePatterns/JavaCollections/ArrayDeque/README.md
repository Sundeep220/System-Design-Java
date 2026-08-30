# ArrayDeque - Deep Dive

## What is ArrayDeque?

`ArrayDeque` is a **resizable-array** implementation of the `Deque` interface. It can be used as both a **Stack** and a **Queue**, and is generally faster than `Stack` (for LIFO) and `LinkedList` (for FIFO).

```java
public class ArrayDeque<E> extends AbstractCollection<E>
        implements Deque<E>, Cloneable, Serializable
```

### Key Characteristics
- **Double-ended**: Efficient add/remove at both head and tail
- **Resizable circular array**: Grows dynamically, wraps around
- **No null elements**: Throws `NullPointerException`
- **Not synchronized**: Not thread-safe
- **Faster than Stack and LinkedList**: Preferred for stack/queue use cases
- **O(1) amortized**: For add/remove at both ends
- **No capacity limit**: Unbounded (grows as needed)

---

## How ArrayDeque Works Internally

### 1. Circular Array (Ring Buffer)

ArrayDeque uses a **circular array** with two pointers:

```java
transient Object[] elements;  // The circular array
transient int head;            // Index of the head element
transient int tail;            // Index AFTER the last element
```

### 2. Visual Representation

```
Physical array:
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ D │ E │   │   │   │ A │ B │ C │
└───┴───┴───┴───┴───┴───┴───┴───┘
  0   1   2   3   4   5   6   7
              ↑               ↑
             tail            head

Logical order (deque): A → B → C → D → E
                       head              tail

The array "wraps around" — head can be AFTER tail in the physical array!
```

### 3. How Wrapping Works

```
Index calculation uses bitwise AND (faster than modulo):

nextIndex = (index + 1) & (elements.length - 1)
prevIndex = (index - 1) & (elements.length - 1)

Example with capacity 8 (length - 1 = 7 = 0b111):
  (7 + 1) & 7 = 8 & 7 = 0    → wraps from index 7 to index 0
  (0 - 1) & 7 = -1 & 7 = 7   → wraps from index 0 to index 7
```

**This is why capacity must be a power of 2** — for the bitmask trick to work.

---

## How addFirst() Works

```java
public void addFirst(E e) {
    if (e == null) throw new NullPointerException();
    final Object[] es = elements;
    es[head = (head - 1) & (es.length - 1)] = e;  // Decrement head (wrapping)
    if (head == tail)
        grow(1);  // Array is full → resize
}
```

```
Before: head=5, tail=2, capacity=8
[D, E, _, _, _, A, B, C]
 0  1  2  3  4  5  6  7
        ↑           ↑
       tail        head

addFirst("Z"):
  head = (5 - 1) & 7 = 4
  elements[4] = "Z"

After: head=4, tail=2
[D, E, _, _, Z, A, B, C]
 0  1  2  3  4  5  6  7
        ↑     ↑
       tail  head

Logical order: Z → A → B → C → D → E
```

---

## How addLast() Works

```java
public void addLast(E e) {
    if (e == null) throw new NullPointerException();
    final Object[] es = elements;
    es[tail] = e;
    if ((tail = (tail + 1) & (es.length - 1)) == head)
        grow(1);  // Array is full → resize
}
```

```
Before: head=5, tail=2, capacity=8
[D, E, _, _, _, A, B, C]

addLast("F"):
  elements[2] = "F"
  tail = (2 + 1) & 7 = 3

After: head=5, tail=3
[D, E, F, _, _, A, B, C]

Logical order: A → B → C → D → E → F
```

---

## How pollFirst() Works

```java
public E pollFirst() {
    final Object[] es = elements;
    final int h = head;
    E e = (E) es[h];
    if (e != null) {
        es[h] = null;  // Help GC
        head = (h + 1) & (es.length - 1);  // Advance head
    }
    return e;
}
```

```
Before: head=5, tail=2
[D, E, _, _, _, A, B, C]

pollFirst() → returns "A"
  elements[5] = null
  head = (5 + 1) & 7 = 6

After: head=6, tail=2
[D, E, _, _, _, _, B, C]

Logical order: B → C → D → E
```

---

## Growth Strategy

When `head == tail` (array is full), the array doubles in size:

```java
private void grow(int needed) {
    final int oldCapacity = elements.length;
    int newCapacity;
    int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
    // Small arrays: ~double. Large arrays: 50% growth.
    newCapacity = oldCapacity + jump;
    // Copy elements into new array, placing head at index 0
}
```

```
Before resize (capacity=8, all slots full):
head=5, tail=5
[D, E, F, G, H, A, B, C]
 0  1  2  3  4  5  6  7

After resize (capacity=16):
head=0, tail=8
[A, B, C, D, E, F, G, H, _, _, _, _, _, _, _, _]
 0  1  2  3  4  5  6  7  8  9  ...

Elements are "unwrapped" into contiguous order.
```

---

## ArrayDeque as Stack vs Queue

### As a Stack (LIFO)

```java
Deque<String> stack = new ArrayDeque<>();

stack.push("A");     // addFirst — O(1)
stack.push("B");     // addFirst — O(1)
stack.push("C");     // addFirst — O(1)

stack.peek();        // peekFirst → "C" (top of stack) — O(1)
stack.pop();         // removeFirst → "C" — O(1)
stack.pop();         // removeFirst → "B" — O(1)
```

```
push("A"):  [A]
push("B"):  [B, A]
push("C"):  [C, B, A]    ← head is at C
pop():      [B, A]       ← returns C
```

### As a Queue (FIFO)

```java
Deque<String> queue = new ArrayDeque<>();

queue.offer("A");    // addLast — O(1)
queue.offer("B");    // addLast — O(1)
queue.offer("C");    // addLast — O(1)

queue.peek();        // peekFirst → "A" (front of queue) — O(1)
queue.poll();        // removeFirst → "A" — O(1)
queue.poll();        // removeFirst → "B" — O(1)
```

```
offer("A"):  [A]
offer("B"):  [A, B]
offer("C"):  [A, B, C]   ← head at A, tail after C
poll():      [B, C]       ← returns A
```

---

## Time Complexity

| Operation | Time | Notes |
|-----------|------|-------|
| `addFirst(e)` / `push(e)` | **O(1) amortized** | O(n) when resizing |
| `addLast(e)` / `offer(e)` | **O(1) amortized** | O(n) when resizing |
| `removeFirst()` / `poll()` / `pop()` | **O(1)** | |
| `removeLast()` / `pollLast()` | **O(1)** | |
| `peekFirst()` / `peek()` | **O(1)** | |
| `peekLast()` | **O(1)** | |
| `remove(Object)` | **O(n)** | Linear search |
| `contains(Object)` | **O(n)** | Linear search |
| `size()` | **O(1)** | Computed from head and tail |
| Iteration | **O(n)** | |

---

## ArrayDeque vs LinkedList vs Stack

### Performance Comparison

| Feature | ArrayDeque | LinkedList | Stack |
|---------|-----------|------------|-------|
| Push/Pop (LIFO) | **O(1)** | O(1) | O(1) |
| Offer/Poll (FIFO) | **O(1)** | O(1) | N/A |
| Memory per element | **~4-8 bytes** | ~40-48 bytes | ~4-8 bytes |
| Cache performance | **Excellent** | Poor | Excellent |
| Null elements | No | Yes | Yes |
| Thread-safe | No | No | Yes (legacy sync) |
| Random access | No | No | Yes (but slow) |

### Why ArrayDeque is Preferred

1. **Memory**: No per-element Node object overhead (unlike LinkedList)
2. **Cache locality**: Contiguous memory → CPU cache-friendly (unlike LinkedList's scattered nodes)
3. **No synchronization overhead**: Unlike legacy `Stack` class
4. **No capacity limit**: Grows dynamically (unlike fixed-capacity ring buffers)

**Official Java recommendation**:
> "This class is likely to be faster than Stack when used as a stack, and faster than LinkedList when used as a queue." — Java Docs

---

## Important Nuances

### 1. No Null Elements

```java
ArrayDeque<String> deque = new ArrayDeque<>();
deque.add(null);  // NullPointerException!

// This is because null is used as a sentinel to detect empty slots
// poll() returns null for empty deque — can't distinguish from a stored null
```

### 2. Not Thread-Safe

```java
// Thread-safe alternatives:
Deque<String> syncDeque = Collections.synchronizedDeque(new ArrayDeque<>());  // Java 17+

// For blocking queue behavior:
BlockingDeque<String> blockingDeque = new LinkedBlockingDeque<>();

// For concurrent stack:
ConcurrentLinkedDeque<String> concDeque = new ConcurrentLinkedDeque<>();
```

### 3. Capacity is Always Power of 2

```java
ArrayDeque<String> deque = new ArrayDeque<>(10);
// Internal capacity = 16 (next power of 2)
// In newer Java versions, the minimum might differ slightly
```

### 4. Fail-Fast Iterator

```java
ArrayDeque<String> deque = new ArrayDeque<>(Arrays.asList("A", "B", "C"));

// WRONG
for (String s : deque) {
    deque.remove(s);  // ConcurrentModificationException
}

// CORRECT
Iterator<String> it = deque.iterator();
while (it.hasNext()) {
    it.next();
    it.remove();
}
```

---

## Deque Method Summary

| Operation | Head (First) | Tail (Last) |
|-----------|-------------|-------------|
| **Insert** | `addFirst(e)` / `offerFirst(e)` / `push(e)` | `addLast(e)` / `offerLast(e)` / `offer(e)` |
| **Remove** | `removeFirst()` / `pollFirst()` / `poll()` / `pop()` | `removeLast()` / `pollLast()` |
| **Examine** | `getFirst()` / `peekFirst()` / `peek()` | `getLast()` / `peekLast()` |

| Method | Throws Exception | Returns null/false |
|--------|-------------------|-------------------|
| Insert | `addFirst()`, `addLast()` | `offerFirst()`, `offerLast()` |
| Remove | `removeFirst()`, `removeLast()` | `pollFirst()`, `pollLast()` |
| Examine | `getFirst()`, `getLast()` | `peekFirst()`, `peekLast()` |

---

## Common Interview Questions

### Q1: Why is ArrayDeque preferred over Stack?
`Stack` is a legacy class that extends `Vector`, meaning every method is `synchronized` — unnecessary overhead for single-threaded use. `Stack` also allows random access via index, which breaks the stack abstraction. ArrayDeque has none of these issues.

### Q2: Why is ArrayDeque preferred over LinkedList for queues?
ArrayDeque uses a contiguous array (cache-friendly) with no per-element Node allocation overhead. LinkedList creates a new Node object for each element (~40 bytes overhead), scattered across the heap (cache-unfriendly).

### Q3: How does the circular array work?
Two pointers (`head` and `tail`) track the front and back. When they reach the end of the array, they "wrap around" using bitmask: `(index + 1) & (length - 1)`. This makes the array behave as a circle.

### Q4: Why doesn't ArrayDeque allow null?
Because `poll()` returns `null` to indicate an empty deque. If null elements were allowed, there would be no way to distinguish between "deque is empty" and "deque contains null at the head".

### Q5: What happens when ArrayDeque is full?
It resizes — creates a new array (roughly double the size), copies all elements in logical order (head to tail), and resets head to 0. This is an O(n) operation but happens rarely, so add is O(1) amortized.

### Q6: Can ArrayDeque be used as both a stack and queue simultaneously?
Yes, since it implements `Deque`, you can add/remove from both ends. For example, you can `addFirst()` (push) and `removeLast()` (like a double-ended processing pattern).

---

## Memory Layout

```
ArrayDeque Object
┌──────────────────┐
│ elements ────────┼──→ Object[] (circular array)
│ head = 5         │    ┌───┬───┬───┬───┬───┬───┬───┬───┐
│ tail = 2         │    │ D │ E │   │   │   │ A │ B │ C │
│ modCount = 5     │    └───┴───┴───┴───┴───┴───┴───┴───┘
└──────────────────┘      0   1   2   3   4   5   6   7
                                  ↑               ↑
                                 tail            head

Logical order: A(5) → B(6) → C(7) → D(0) → E(1)
               head ──────────────────────→ tail
```

---

## Best Practices

1. **Use ArrayDeque as the default Stack/Queue** — faster and more memory-efficient than alternatives
2. **Never use `java.util.Stack`** — it's a legacy class; use `Deque<E> stack = new ArrayDeque<>()`
3. **Prefer ArrayDeque over LinkedList for queues** — better cache performance
4. **Pre-size when possible**: `new ArrayDeque<>(expectedSize)` to avoid resizing
5. **Don't store null** — it will throw `NullPointerException`
6. **Use `Deque` interface**: `Deque<String> deque = new ArrayDeque<>()` for stack/queue operations
7. **For thread-safe deques**, use `ConcurrentLinkedDeque` or `LinkedBlockingDeque`
8. **Use for BFS** — ArrayDeque is the ideal queue for breadth-first search
