Absolutely. We’ve covered **JVM Architecture → Heap**, so now we move to **Stack**.

# 3. JVM Stack — Deep Dive

The JVM stack is one of the most important concepts for understanding:

* method calls
* local variables
* recursion
* thread execution
* `StackOverflowError`
* thread dumps
* concurrency
* debugging
* how references relate to heap objects

The core mental model is:

> **Every Java thread has its own JVM stack, and every method invocation creates a stack frame on that thread's stack.**

---

# 1. Where does Stack fit into JVM memory?

From our JVM architecture:

```text
                         JVM
                          |
             +------------+------------+
             |                         |
             v                         v
       Shared Memory              Thread Memory
             |                         |
       +-----+------+             +----+----+
       |            |             |         |
      Heap      Metaspace       Stack      PC
                                  |
                             Per Thread
```

The most important distinction:

### Heap

Shared by threads.

### Stack

**Each thread gets its own stack.**

For example:

```text
JVM
│
├── Heap
│
├── Metaspace
│
├── Thread 1
│   └── Stack 1
│
├── Thread 2
│   └── Stack 2
│
└── Thread 3
    └── Stack 3
```

This is fundamental to understanding Java concurrency.

---

# 2. Why does every thread need its own stack?

Imagine:

```java
void methodA() {
    int x = 10;
    methodB();
}

void methodB() {
    int y = 20;
}
```

Now imagine two threads:

```text
Thread 1 → methodA()
Thread 2 → methodA()
```

Each thread needs its own:

```text
x
y
method call state
return address
operand stack
```

Therefore:

```text
Thread 1
   |
   v
Stack 1
+----------------+
| methodB frame  |
+----------------+
| methodA frame  |
+----------------+


Thread 2
   |
   v
Stack 2
+----------------+
| methodB frame  |
+----------------+
| methodA frame  |
+----------------+
```

They execute independently.

---

# 3. Stack works like LIFO

Stack follows:

> **Last In, First Out**

Suppose:

```java
main()
  ↓
methodA()
  ↓
methodB()
  ↓
methodC()
```

The stack looks conceptually like:

```text
+-------------------+
| methodC()         | ← top
+-------------------+
| methodB()         |
+-------------------+
| methodA()         |
+-------------------+
| main()            |
+-------------------+
```

When `methodC()` finishes:

```text
+-------------------+
| methodB()         | ← top
+-------------------+
| methodA()         |
+-------------------+
| main()            |
+-------------------+
```

When `methodB()` finishes:

```text
+-------------------+
| methodA()         |
+-------------------+
| main()            |
+-------------------+
```

This naturally matches nested method execution.

---

# 4. What is a Stack Frame?

This is the most important concept in JVM Stack.

Every time a method is invoked, the JVM creates a:

> **Stack Frame**

For:

```java
public static void main(String[] args) {
    calculate();
}
```

the JVM creates a frame for:

```text
main()
```

Then when:

```java
calculate();
```

executes:

```text
main()
   |
   v
calculate()
```

another frame is created.

Conceptually:

```text
Stack

+-----------------------+
| calculate() frame     |
+-----------------------+
| main() frame          |
+-----------------------+
```

When `calculate()` returns, its frame is removed.

---

# 5. What is inside a Stack Frame?

A JVM stack frame conceptually contains:

```text
Stack Frame
│
├── Local Variable Array
│
├── Operand Stack
│
├── Reference to Runtime Constant Pool
│
└── Other implementation-specific information
```

These three are particularly important:

1. **Local variables**
2. **Operand stack**
3. **Runtime constant pool reference**

Let's understand them.

---

# 6. Local Variable Array

Consider:

```java
static void calculate() {

    int x = 10;
    int y = 20;

    int result = x + y;
}
```

The frame needs to maintain the method's local variables.

Conceptually:

```text
calculate() frame

Local Variables
+----------------+
| x = 10         |
| y = 20         |
| result = 30    |
+----------------+
```

The JVM bytecode uses local variable slots.

For example:

```text
slot 0
slot 1
slot 2
...
```

The exact slot usage depends on the method and types.

---

# 7. What about object references?

Consider:

```java
static void process() {

    User user = new User();

}
```

Conceptually:

```text
Stack

process() frame
+-----------------------+
| user reference -------|------+
+-----------------------+      |
                               |
                               v
                            Heap
                         +---------+
                         | User    |
                         +---------+
```

So the stack frame contains the local reference associated with `user`, while the actual object is generally in the heap.

This gives us the classic relationship:

```text
Stack                    Heap

user -----------------> User object
```

But remember our earlier caveat:

> This is the conceptual model. The JVM's JIT compiler can optimize allocations, and an object doesn't necessarily have to physically exist as a conventional heap object in every optimized execution.

---

# 8. Primitive vs reference

Consider:

```java
int age = 24;
User user = new User();
```

Conceptually:

```text
Stack Frame

+-----------------------+
| age = 24              |
|                       |
| user -----------+     |
+------------------|----+
                   |
                   |
                   v
                 Heap
              +---------+
              | User    |
              +---------+
```

The important distinction is:

```text
age
```

is a primitive value.

```text
user
```

is a reference.

And:

```text
new User()
```

is the object.

---

# 9. The Operand Stack

This is where JVM bytecode becomes particularly interesting.

The JVM is a **stack-based virtual machine**.

That means many bytecode operations use an operand stack.

Consider:

```java
int result = 10 + 20;
```

Conceptually, bytecode execution might look like:

```text
push 10
push 20
add
store result
```

The operand stack evolves approximately like:

```text
Initially:

+-------+
|       |
+-------+
```

Push 10:

```text
+-------+
|  10   |
+-------+
```

Push 20:

```text
+-------+
|  20   |
+-------+
|  10   |
+-------+
```

Execute add:

```text
20 + 10
```

result:

```text
+-------+
|  30   |
+-------+
```

Then store it into a local variable.

---

# 10. Why is Java bytecode stack-based?

Because the JVM instruction set is designed around an operand stack.

For example, conceptually:

```text
iload
iload
iadd
istore
```

instead of instructions explicitly specifying many CPU registers.

This makes the bytecode relatively platform-independent.

The JVM's execution engine eventually maps this work to the underlying machine.

This becomes especially interesting once the JIT compiler gets involved.

---

# 11. Method invocation

Consider:

```java
public static void main(String[] args) {

    int result = add(10, 20);

}

static int add(int a, int b) {

    return a + b;
}
```

Execution conceptually:

### Step 1

`main()` starts.

```text
Stack

+----------------+
| main()         |
+----------------+
```

### Step 2

`add(10, 20)` is invoked.

```text
Stack

+----------------+
| add()          |
+----------------+
| main()         |
+----------------+
```

### Step 3

`add()` executes.

Its frame contains:

```text
a = 10
b = 20
```

### Step 4

`return a + b`.

`add()` frame is removed.

```text
Stack

+----------------+
| main()         |
+----------------+
```

The result is returned to the caller.

---

# 12. Stack frames are method-specific

Suppose:

```java
void A() {
    int x = 10;
    B();
}

void B() {
    int y = 20;
    C();
}

void C() {
    int z = 30;
}
```

At the deepest point:

```text
Stack

+-------------------+
| C()               |
| z = 30            |
+-------------------+
| B()               |
| y = 20            |
+-------------------+
| A()               |
| x = 10            |
+-------------------+
| caller            |
+-------------------+
```

Each frame has its own local state.

Therefore:

```text
A.x
B.y
C.z
```

are independent.

---

# 13. What happens when a method returns?

This is very important.

Suppose:

```java
void calculate() {

    int x = 100;

}
```

When `calculate()` returns:

```text
calculate() frame
```

is removed from the stack.

Conceptually:

```text
Before return:

+----------------+
| calculate()    |
| x = 100        |
+----------------+
| main()         |
+----------------+


After return:

+----------------+
| main()         |
+----------------+
```

The local variable `x` is gone as part of that frame's execution state.

---

# 14. Does the Stack contain the object?

Consider:

```java
void process() {

    User user = new User();

}
```

A useful conceptual model is:

```text
Stack

process()
+------------------+
| user --------+   |
+--------------|---+
               |
               v
Heap
+------------------+
| User object      |
+------------------+
```

When `process()` returns:

```text
Stack frame removed
```

The reference `user` disappears.

Now, if there are no other references:

```text
User object
    ^
    |
    X

no references
```

the object becomes eligible for GC.

This is an important connection:

> **Stack frame lifetime can influence object reachability.**

---

# 15. Stack and Garbage Collection

Suppose:

```java
void process() {

    User user = new User();

    doSomething();
}
```

While `process()` is executing:

```text
GC Root
   |
   v
Thread Stack
   |
   v
user reference
   |
   v
User object
```

The object is reachable.

When `process()` returns:

```text
Thread Stack
   |
   X
```

The reference may disappear.

If there are no other references:

```text
User object
    |
    v
unreachable
```

Now GC can reclaim it.

This is why thread stacks are one source of **GC roots**.

---

# 16. Stack is per-thread

This is one of the biggest differences from heap.

Suppose:

```java
Thread T1
Thread T2
```

Each gets:

```text
T1 → Stack 1
T2 → Stack 2
```

For example:

```text
                 JVM Heap
              /           \
             /             \
        shared objects   shared objects


Thread 1                  Thread 2
   |                         |
   v                         v
Stack 1                   Stack 2
+---------+               +---------+
| methodA |               | methodB |
| x = 10  |               | x = 20   |
+---------+               +---------+
```

Their local variables don't automatically interfere with each other.

---

# 17. But stack doesn't mean thread-safe

This is a subtle but important point.

Suppose:

```java
void process() {

    int x = 10;

}
```

`x` is local to the current thread's stack frame.

So another thread cannot directly access that local variable.

That's naturally isolated.

But:

```java
void process() {

    sharedObject.count++;

}
```

is different.

`sharedObject` may point to a heap object shared between threads.

```text
Thread 1
  |
  v
Stack
  |
  +---- sharedObject -----+
                           |
Thread 2                   v
  |                    Heap object
  v
Stack
```

Therefore stack-local state is naturally thread-confined, while shared heap state requires synchronization/atomicity considerations.

---

# 18. Recursion and Stack

This is where Stack becomes particularly important for DSA.

Consider:

```java
void recurse() {

    recurse();

}
```

Execution:

```text
recurse()
   |
   v
recurse()
   |
   v
recurse()
   |
   v
recurse()
```

Every invocation requires another stack frame.

So:

```text
Stack

+----------------+
| recurse()      |
+----------------+
| recurse()      |
+----------------+
| recurse()      |
+----------------+
| recurse()      |
+----------------+
| ...            |
+----------------+
```

Eventually the thread runs out of stack space.

Then:

```text
java.lang.StackOverflowError
```

---

# 19. StackOverflowError

A common example:

```java
public class Demo {

    static void recurse() {
        recurse();
    }

    public static void main(String[] args) {
        recurse();
    }
}
```

Eventually:

```text
Exception in thread "main"
java.lang.StackOverflowError
```

Why?

Not because the heap is full.

Because:

```text
Thread Stack
████████████████████
         |
         v
no more stack space
```

This distinction is extremely important:

```text
StackOverflowError
        ≠
OutOfMemoryError: Java heap space
```

---

# 20. StackOverflow vs Heap OOM

### Stack overflow

Usually caused by excessive method-call depth.

Example:

```java
recursiveMethod();
```

Result:

```text
StackOverflowError
```

### Heap exhaustion

Usually caused by excessive live objects / allocation pressure / insufficient heap.

Example:

```java
List<byte[]> list = new ArrayList<>();

while (true) {
    list.add(new byte[1024 * 1024]);
}
```

Result may be:

```text
OutOfMemoryError: Java heap space
```

So:

```text
Stack
  ↓
StackOverflowError


Heap
  ↓
OutOfMemoryError: Java heap space
```

---

# 21. How large is the Java Stack?

You can configure thread stack size with:

```bash
-Xss
```

For example:

```bash
-Xss1m
```

means roughly:

```text
Thread stack size ≈ 1 MB
```

So:

```bash
java -Xss1m -jar app.jar
```

sets the thread stack size accordingly.

But don't interpret this as:

> "The entire application gets a 1 MB stack."

No.

It's generally **per thread**.

This is extremely important.

---

# 22. Why `-Xss` matters in microservices

Suppose your application has:

```text
1000 threads
```

and each thread has a stack reservation/configured stack size around:

```text
1 MB
```

Conceptually, that's potentially significant native memory pressure.

```text
1000 threads
×
1 MB
≈
1 GB
```

This is simplified because actual memory commitment/usage is more nuanced, but the point is:

> **Thread count has a memory cost beyond the Java heap.**

This is particularly important for:

* Spring Boot
* Tomcat
* WebFlux
* Kafka consumers
* scheduled executors
* async processing
* Kubernetes memory limits

---

# 23. Stack and thread dumps

This is where your future topic:

> **Thread Dumps**

connects directly to Stack.

A thread dump shows what threads are doing and, importantly, their current **stack traces**.

For example:

```text
"http-nio-8080-exec-10" #42
    java.lang.Thread.State: WAITING

    at java.util.concurrent.locks.LockSupport.park(...)
    at ...
    at com.example.OrderService.process(OrderService.java:42)
    at ...
```

This tells you the method call chain.

Conceptually:

```text
Thread
  |
  v
Current Stack
  |
  +--> process()
  |
  +--> service()
  |
  +--> repository()
  |
  +--> JDBC
```

This is why thread dumps are so useful when diagnosing:

* deadlocks
* blocked threads
* thread starvation
* slow requests
* stuck threads
* lock contention

We'll later do a complete thread-dump analysis.

---

# 24. Stack trace vs Stack

Don't confuse these.

### Stack

Actual runtime execution structure maintained for a thread.

### Stack trace

A representation of the method call path.

For example:

```text
main()
  ↓
service()
  ↓
repository()
  ↓
query()
```

A stack trace might show:

```text
at com.example.Repository.query()
at com.example.Service.service()
at com.example.Main.main()
```

So:

```text
Stack
   ↓
runtime structure

Stack trace
   ↓
representation/debugging output
```

---

# 25. Why stack traces are so useful

Suppose you get:

```text
NullPointerException
```

and Java prints:

```text
at com.example.OrderService.createOrder(OrderService.java:42)
at com.example.OrderController.create(OrderController.java:28)
at ...
```

You are effectively seeing part of the current call stack.

Conceptually:

```text
createOrder()
      ↑
      |
create()
      ↑
      |
main/request thread
```

The stack trace tells you:

> "How did execution get here?"

---

# 26. Stack and method parameters

Consider:

```java
static int add(int a, int b) {
    return a + b;
}
```

When called:

```java
add(10, 20);
```

the method frame needs to represent:

```text
a = 10
b = 20
```

Conceptually:

```text
add() Frame

Local Variables
+----------------+
| a = 10         |
| b = 20         |
+----------------+
```

Then the method performs the addition using the operand stack.

Conceptually:

```text
10
20
 ↓
iadd
 ↓
30
```

Then:

```text
return 30
```

and the frame disappears.

---

# 27. Stack is not literally an infinite Java data structure

Don't think of the JVM stack as:

```java
Stack<StackFrame>
```

That's just a conceptual model.

The JVM specification defines the behavior, but actual JVM implementations can implement stacks using native memory and optimized mechanisms.

So when discussing JVM internals:

> **The JVM specification defines runtime behavior, while the exact memory representation is implementation-dependent.**

This distinction becomes important when talking about:

* HotSpot
* OpenJ9
* native memory
* JIT optimizations

---

# 28. JVM specification vs implementation

This is a very useful interview distinction.

The JVM specification says conceptually:

```text
Each thread has a JVM stack.
```

But it doesn't require every JVM implementation to organize the underlying native memory in exactly the same way.

Similarly:

```text
"Objects are allocated in heap"
```

is the JVM-level conceptual model.

But a JIT compiler can optimize an allocation away.

For example:

```java
static int calculate() {

    Point p = new Point(10, 20);

    return p.x + p.y;
}
```

The JVM may realize:

> `p` never escapes this method.

Through **escape analysis**, the JIT may optimize away the actual object allocation.

We'll study this deeply when we reach JIT.

---

# 29. Escape Analysis

This is one of the more advanced stack/heap concepts.

Suppose:

```java
static int calculate() {

    Point p = new Point(10, 20);

    return p.x + p.y;
}
```

The object:

```text
p
```

doesn't escape the method.

The JIT can potentially transform the computation into something closer to:

```java
return 10 + 20;
```

rather than actually creating a full heap object.

This is called **scalar replacement** in relevant optimization scenarios.

So:

```text
Java source
    ↓
new Point()
    ↓
JIT analyzes escape
    ↓
object doesn't escape
    ↓
allocation may be eliminated
```

This is why simplistic statements like:

> "Every `new` always creates an object on the heap."

aren't universally accurate at the physical implementation level.

---

# 30. Stack and garbage collection

One subtle point:

The JVM does not generally "GC the stack" like it does the heap.

Stack frames naturally disappear as methods return.

For example:

```text
main
 ↓
A
 ↓
B
 ↓
C
```

After `C` returns:

```text
main
 ↓
A
 ↓
B
```

After `B` returns:

```text
main
 ↓
A
```

After `A` returns:

```text
main
```

So stack memory is largely managed through method invocation/return.

Heap memory requires GC because object lifetime isn't necessarily tied to method lifetime.

---

# 31. A powerful comparison

Think about these two objects:

### Local variable

```java
void process() {

    int x = 10;

}
```

Its lifetime is associated with:

```text
process() frame
```

### Heap object

```java
void process() {

    User user = new User();

    cache.put("user", user);

}
```

After `process()` returns:

```text
user reference in process frame → gone
```

But:

```text
cache → User
```

still exists.

Therefore the object remains reachable.

This illustrates:

> **Object lifetime can outlive the stack frame that originally created the reference.**

That's a crucial JVM concept.

---

# 32. Stack and closures/lambdas

Consider:

```java
int x = 10;

Runnable r = () -> {
    System.out.println(x);
};
```

You might wonder:

> "What happens to `x` when the method returns?"

Java's lambda/capture semantics require the captured local to be effectively final, and the JVM/compiler arranges the necessary state so that the lambda can retain what it needs.

This is another example of why:

```text
Java source variable
```

doesn't always map one-to-one to:

```text
physical stack slot
```

The compiler and JIT are allowed to transform the implementation while preserving Java semantics.

---

# 33. Stack and native methods

Remember from JVM architecture:

```text
Java Stack
```

is different conceptually from:

```text
Native Method Stack
```

Java code:

```java
someJavaMethod();
```

runs through JVM execution mechanisms.

Native code:

```text
JNI
C/C++
native library
```

may involve native stacks.

This becomes important when debugging JVM crashes.

For example:

```text
Java application
     ↓
JVM
     ↓
JNI/native library
     ↓
segmentation fault
     ↓
SIGSEGV
```

A Java stack alone isn't necessarily enough to understand such failures.

---

# 34. Stack memory vs native memory

Another advanced distinction:

```text
Java Heap
```

is not the same thing as all memory used by the JVM.

A process can consume memory through:

```text
Heap
Thread stacks
Metaspace
Code cache
Direct buffers
Native libraries
JVM internals
```

So imagine:

```text
Container memory = 2 GB

Heap              = 1.2 GB
Metaspace         = 200 MB
Thread stacks     = 300 MB
Direct memory     = 150 MB
Other native      = 100 MB

Total             ≈ 1.95 GB
```

The exact numbers are illustrative, but this is the mental model you need for Kubernetes troubleshooting.

---

# 35. Common Stack-related problems

There are several classes of problems.

### 1. Excessive recursion

```java
void recurse() {
    recurse();
}
```

Result:

```text
StackOverflowError
```

---

### 2. Huge thread count

```text
1000s of threads
```

Each thread needs stack/native memory.

Possible result:

```text
OutOfMemoryError
```

but not necessarily:

```text
Java heap space
```

There are different forms of JVM/native memory exhaustion.

---

### 3. Deep call chains

Framework-heavy applications can create large call stacks.

For example:

```text
Controller
 ↓
Service
 ↓
Proxy
 ↓
Interceptor
 ↓
Transaction
 ↓
Repository
 ↓
Hibernate
 ↓
JDBC
```

This isn't inherently bad, but deep nesting can increase stack usage.

---

### 4. Thread explosion

For example:

```java
for (...) {
    new Thread(...).start();
}
```

Creating huge numbers of threads can cause:

```text
memory pressure
CPU scheduling overhead
context switching
thread exhaustion
```

This is why production applications generally use controlled thread pools.

---

# 36. Stack and Spring Boot

A typical Spring Boot request may conceptually look like:

```text
HTTP Request
    |
    v
Tomcat thread
    |
    v
DispatcherServlet
    |
    v
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
Hibernate
    |
    v
JDBC
```

That execution occurs on a thread.

That thread has its own stack:

```text
HTTP Thread
    |
    v
Stack
+-----------------------+
| JDBC                  |
+-----------------------+
| Hibernate             |
+-----------------------+
| Repository            |
+-----------------------+
| Service               |
+-----------------------+
| Controller            |
+-----------------------+
```

If the request hangs, a thread dump can show you this stack.

For example:

```text
"http-nio-8080-exec-15"
WAITING

at java.util.concurrent.locks.LockSupport.park(...)
at ...
at com.example.OrderService.createOrder(...)
at com.example.OrderController.create(...)
```

Now you can reason about what the thread is doing.

---

# 37. Stack and concurrency

Suppose:

```java
void process() {

    int localValue = 10;

}
```

Every thread has its own:

```text
localValue
```

because each invocation has its own stack frame.

So:

```text
Thread 1                 Thread 2

localValue = 10          localValue = 10
```

No synchronization is required for the local primitive itself.

But:

```java
class Counter {
    int count;
}
```

with:

```java
counter.count++;
```

can be shared.

Then:

```text
Thread 1 ──┐
           |
Thread 2 ──┼──> Heap Counter
           |
Thread 3 ──┘
```

Now synchronization becomes relevant.

---

# 38. Important interview question: Is Stack thread-safe?

The answer needs nuance.

You shouldn't say:

> "Stack is thread-safe."

Instead:

> **Each Java thread has its own JVM stack, so stack-local variables are isolated between threads. However, references in those local variables can point to shared heap objects, which may require synchronization.**

Example:

```java
void method() {

    int x = 10;             // thread-local

    sharedCounter.increment(); // shared heap state
}
```

---

# 39. Stack vs Heap — final mental model

```text
                    JVM
                     |
        +------------+------------+
        |                         |
       HEAP                     THREAD
        |                         |
    Shared                      Stack
        |                    Per-thread
        |                         |
 +------+------+            +-----+-----+
 |      |      |            |           |
Object Object Object      Frame       Frame
                              |
                              |
                     +--------+--------+
                     |        |        |
                  Locals   Operand   Method
                            Stack     state
```

And:

```text
Stack
  |
  +---- reference ----------------+
                                  |
                                  v
                               Heap
                                  |
                               Object
```

---

# 40. The key concepts to memorize

If you're preparing for interviews, make sure these are crystal clear:

### 1.

**Every Java thread has its own stack.**

### 2.

**Every method invocation creates a stack frame.**

### 3.

**A frame contains local execution state, including local variables and an operand stack.**

### 4.

**The JVM is a stack-based virtual machine.**

### 5.

**Objects are generally allocated in the heap, while stack frames contain references/local execution state.**

### 6.

**Stack frames disappear as methods return.**

### 7.

**Deep/unbounded recursion can cause `StackOverflowError`.**

### 8.

**Stack memory is different from heap memory and contributes to total JVM/process memory usage.**

### 9.

**Each thread having its own stack doesn't make referenced heap objects thread-safe.**

### 10.

**JIT optimizations mean the conceptual stack/heap model isn't always the exact physical implementation.**

---

# 41. Connecting our checklist so far

We've now covered:

```text
1. JVM Architecture
       |
       +-------------------------+
       |                         |
       v                         v
    Heap                      Stack
       |                         |
       |                    Stack Frames
       |                         |
       |                    Method Calls
       |                         |
       |                    Local State
       |                         |
       |                    Recursion
       |
       v
   Object Lifetime
```

Our remaining roadmap is:

```text
✅ JVM architecture
✅ Heap
✅ Stack

⬜ Metaspace
⬜ Class loading
⬜ JIT
⬜ Garbage collection
⬜ Young generation
⬜ Old generation
⬜ GC pauses
⬜ Memory leaks
⬜ OutOfMemoryError
⬜ CPU profiling
⬜ Thread dumps
⬜ Heap dumps
```

## Next: Metaspace

The next topic is **Metaspace**, and this is where we'll connect JVM memory to **classes themselves**.

We'll cover:

```text
Class
  |
  v
Class metadata
  |
  v
Metaspace

Metaspace
  |
  +--> loaded classes
  +--> class metadata
  +--> method metadata
  +--> runtime structures
  +--> class unloading
  |
  v
OutOfMemoryError: Metaspace
```

And importantly, we'll distinguish **Heap vs Stack vs Metaspace** with concrete examples, then connect Metaspace to **Spring proxies, Hibernate, CGLIB/ByteBuddy, class loaders, dynamic class generation, and class-loader memory leaks**.
