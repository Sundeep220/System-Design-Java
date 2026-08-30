# LinkedList - Deep Dive

## What is LinkedList?

`LinkedList` is a **doubly-linked list** implementation of the `List` and `Deque` interfaces in Java. It is part of `java.util` package.

```java
public class LinkedList<E> extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```

### Key Characteristics
- **Ordered**: Maintains insertion order
- **Allows duplicates**: Can store duplicate elements
- **Allows null**: Can store null values
- **Not synchronized**: Not thread-safe by default
- **No random access**: Does NOT implement `RandomAccess` — accessing by index is O(n)
- **Implements Deque**: Can be used as a Stack, Queue, or Double-ended Queue

---

## How LinkedList Works Internally

### 1. Node Structure

Each element in a LinkedList is wrapped inside a **Node** object:

```java
private static class Node<E> {
    E item;           // The actual element
    Node<E> next;     // Reference to the next node
    Node<E> prev;     // Reference to the previous node

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

### 2. LinkedList Fields

```java
transient int size = 0;       // Number of elements
transient Node<E> first;      // Pointer to the first (head) node
transient Node<E> last;       // Pointer to the last (tail) node
```

### 3. Visual Representation

```
     first                                          last
       │                                              │
       ▼                                              ▼
   ┌────────┐     ┌────────┐     ┌────────┐     ┌────────┐
   │ prev:  │◄────│ prev   │◄────│ prev   │◄────│ prev   │
   │  null  │     │        │     │        │     │        │
   │ item:A │     │ item:B │     │ item:C │     │ item:D │
   │ next   │────►│ next   │────►│ next   │────►│ next:  │
   │        │     │        │     │        │     │  null  │
   └────────┘     └────────┘     └────────┘     └────────┘
      Node 0        Node 1        Node 2        Node 3
```

- `first.prev == null` — no node before the head
- `last.next == null` — no node after the tail
- For a single element: `first == last` (same node)
- For empty list: `first == null && last == null`

---

## How Operations Work Internally

### 1. addLast(e) / add(e) — Adding at the End

```java
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;   // List was empty
    else
        l.next = newNode;  // Link old last to new node
    size++;
    modCount++;
}
```

```
Before: A ↔ B ↔ C       (last = C)

addLast("D"):
  1. Create new Node(prev=C, item=D, next=null)
  2. last = new Node
  3. C.next = new Node

After:  A ↔ B ↔ C ↔ D   (last = D)
```

**Time Complexity: O(1)** — direct pointer manipulation.

### 2. addFirst(e) — Adding at the Beginning

```java
void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null)
        last = newNode;    // List was empty
    else
        f.prev = newNode;  // Link old first back to new node
    size++;
    modCount++;
}
```

**Time Complexity: O(1)**

### 3. add(index, element) — Adding at a Specific Index

```java
public void add(int index, E element) {
    checkPositionIndex(index);
    if (index == size)
        linkLast(element);
    else
        linkBefore(element, node(index));  // node(index) is O(n)!
}
```

**The `node(index)` method** is the key to understanding LinkedList performance:

```java
Node<E> node(int index) {
    if (index < (size >> 1)) {
        // Search from the beginning
        Node<E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        // Search from the end
        Node<E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```

**Optimization**: It checks if the index is in the first half or second half and traverses from the closer end. Still **O(n/2) = O(n)**.

### 4. remove(index) — Removing by Index

```java
public E remove(int index) {
    checkElementIndex(index);
    return unlink(node(index));  // node(index) is O(n)
}
```

```java
E unlink(Node<E> x) {
    final E element = x.item;
    final Node<E> next = x.next;
    final Node<E> prev = x.prev;

    if (prev == null) {
        first = next;           // Was the first node
    } else {
        prev.next = next;       // Bypass x
        x.prev = null;          // Help GC
    }

    if (next == null) {
        last = prev;            // Was the last node
    } else {
        next.prev = prev;       // Bypass x
        x.next = null;          // Help GC
    }

    x.item = null;              // Help GC
    size--;
    modCount++;
    return element;
}
```

```
Before: A ↔ B ↔ C ↔ D

remove(1)  → remove "B"

  1. Find node at index 1 → Node(B)     [O(n) traversal]
  2. A.next = C                          [O(1) unlinking]
  3. C.prev = A
  4. Null out B's pointers (GC help)

After:  A ↔ C ↔ D
```

---

## Time Complexity Analysis

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| `addFirst(e)` | **O(1)** | Direct pointer update |
| `addLast(e)` / `add(e)` | **O(1)** | Direct pointer update |
| `add(index, e)` | **O(n)** | O(n) to find node, O(1) to link |
| `getFirst()` / `getLast()` | **O(1)** | Direct pointer access |
| `get(index)` | **O(n)** | Must traverse from head/tail |
| `set(index, e)` | **O(n)** | Must traverse to find node |
| `removeFirst()` | **O(1)** | Direct pointer update |
| `removeLast()` | **O(1)** | Direct pointer update |
| `remove(index)` | **O(n)** | O(n) to find, O(1) to unlink |
| `remove(Object)` | **O(n)** | Linear search |
| `contains(e)` | **O(n)** | Linear search |
| `size()` | **O(1)** | Maintained as field |
| `iterator.remove()` | **O(1)** | Already at the node |

---

## LinkedList as a Deque (Double-Ended Queue)

Since `LinkedList` implements `Deque`, it supports operations at both ends:

```java
LinkedList<String> deque = new LinkedList<>();

// Queue operations (FIFO)
deque.offer("A");       // Add at tail
deque.poll();           // Remove from head

// Stack operations (LIFO)
deque.push("A");        // Add at head
deque.pop();            // Remove from head

// Deque operations
deque.offerFirst("A");  // Add at head
deque.offerLast("B");   // Add at tail
deque.pollFirst();      // Remove from head
deque.pollLast();       // Remove from tail

// Peek without removing
deque.peekFirst();      // Look at head
deque.peekLast();       // Look at tail
```

### Deque Method Pairs

| Operation | Throws Exception | Returns Special Value |
|-----------|-----------------|----------------------|
| Insert at head | `addFirst(e)` | `offerFirst(e)` |
| Insert at tail | `addLast(e)` | `offerLast(e)` |
| Remove from head | `removeFirst()` | `pollFirst()` |
| Remove from tail | `removeLast()` | `pollLast()` |
| Examine head | `getFirst()` | `peekFirst()` |
| Examine tail | `getLast()` | `peekLast()` |

---

## LinkedList vs ArrayList — The Real Comparison

### Memory Overhead Per Element

```
ArrayList:
  Each element = 1 object reference (4-8 bytes)

LinkedList:
  Each element = 1 Node object (header: 12-16 bytes)
               + 3 references (item + prev + next): 12-24 bytes
               + padding: ~8 bytes
  Total per element ≈ 40-48 bytes (vs 4-8 bytes for ArrayList)
```

**LinkedList uses ~5-10x more memory per element** than ArrayList.

### CPU Cache Performance

```
ArrayList (contiguous memory):
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ A │ B │ C │ D │ E │ F │ G │ H │  ← Single cache line can hold multiple elements
└───┴───┴───┴───┴───┴───┴───┴───┘
  → Sequential access = cache-friendly = FAST

LinkedList (scattered memory):
  Node@0x100 → Node@0x500 → Node@0x230 → Node@0x800
       ↓            ↓            ↓            ↓
     item A       item B       item C       item D
  → Random access pattern = cache-unfriendly = SLOW
```

### When to Use What

| Scenario | Winner |
|----------|--------|
| Random access by index | **ArrayList** (O(1) vs O(n)) |
| Iteration | **ArrayList** (cache locality) |
| Add/remove at end | **ArrayList** (amortized O(1), cache-friendly) |
| Add/remove at beginning | **LinkedList** (O(1) vs O(n)) |
| Add/remove in middle (with iterator) | **LinkedList** (O(1) unlink vs O(n) shift) |
| Memory efficiency | **ArrayList** (5-10x less per element) |
| Use as Queue/Deque | **ArrayDeque** (better than both) |

> **Rule of thumb**: Use `ArrayList` unless you have a specific, measured need for `LinkedList`. In benchmarks, ArrayList wins in almost every scenario due to hardware-level cache optimizations.

---

## Fail-Fast Iterator

Same as ArrayList, LinkedList's iterator is fail-fast:

```java
LinkedList<String> list = new LinkedList<>(Arrays.asList("A", "B", "C"));

// WRONG — ConcurrentModificationException
for (String s : list) {
    list.remove(s);
}

// CORRECT — Use iterator's remove
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (s.equals("B")) {
        it.remove();  // O(1) — already at the node!
    }
}
```

### Descending Iterator

```java
Iterator<String> descIt = list.descendingIterator();
while (descIt.hasNext()) {
    System.out.println(descIt.next());  // Iterates from tail to head
}
```

---

## ListIterator — Bidirectional Traversal

LinkedList supports `ListIterator` which allows traversal in both directions:

```java
ListIterator<String> lit = list.listIterator();

// Forward
while (lit.hasNext()) {
    System.out.println(lit.next());
}

// Backward
while (lit.hasPrevious()) {
    System.out.println(lit.previous());
}

// Modify during iteration
lit.add("X");      // Insert before the element that would be returned by next()
lit.set("Y");      // Replace the last element returned by next() or previous()
lit.remove();      // Remove the last element returned by next() or previous()
```

---

## Common Interview Questions

### Q1: Is LinkedList a singly or doubly linked list?
**Doubly linked list**. Each node has `prev` and `next` pointers. This allows O(1) removal from both ends and backward traversal.

### Q2: How does get(index) work? Is it efficient?
No. `get(index)` traverses from `first` or `last` (whichever is closer) — it's **O(n)**. This is why LinkedList does NOT implement `RandomAccess`.

### Q3: When is LinkedList actually better than ArrayList?
- When you frequently add/remove from the **beginning** of the list
- When you iterate using an **iterator** and remove elements during iteration (O(1) per removal vs O(n) shift in ArrayList)
- When using it as a **Queue or Deque** (though `ArrayDeque` is usually better)

### Q4: Can LinkedList be used as a Stack?
Yes. Use `push()` (addFirst) and `pop()` (removeFirst). But `ArrayDeque` is preferred:
```java
Deque<String> stack = new ArrayDeque<>();  // Preferred
stack.push("A");
stack.pop();
```

### Q5: Why does Java's LinkedList have both first and last pointers?
To enable O(1) operations at both ends, making it usable as a Deque. Also allows the `node(index)` optimization of traversing from the closer end.

### Q6: How does LinkedList handle memory?
Each element creates a new `Node` object on the heap. When a node is unlinked, its references are set to `null` to help GC. There's no bulk deallocation like ArrayList's array — each node is individually garbage collected.

---

## Memory Layout

```
LinkedList Object (Heap)
┌──────────────┐
│ first ───────┼──→ Node@0x100
│ last  ───────┼──→ Node@0x300
│ size = 3     │
│ modCount = 3 │
└──────────────┘

       Node@0x100              Node@0x200              Node@0x300
    ┌──────────────┐       ┌──────────────┐       ┌──────────────┐
    │ prev = null  │       │ prev ────────┼──→100 │ prev ────────┼──→200
    │ item = "A"   │       │ item = "B"   │       │ item = "C"   │
    │ next ────────┼──→200 │ next ────────┼──→300 │ next = null  │
    └──────────────┘       └──────────────┘       └──────────────┘

Note: Nodes are scattered across the heap — NOT contiguous in memory
```

---

## Best Practices

1. **Prefer ArrayList** for most use cases — it's faster due to cache locality
2. **Prefer ArrayDeque** over LinkedList for Queue/Stack — lower memory overhead
3. **Use iterator's remove()** when removing during iteration — it's O(1)
4. **Never use get(i) in a loop** on LinkedList — it's O(n²) total
5. **Use `descendingIterator()`** for reverse traversal instead of manual index-based access
6. **Program to the interface**: `List<String> list = new LinkedList<>()` or `Deque<String> deque = new LinkedList<>()`
