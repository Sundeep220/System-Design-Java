# Java Keywords — Interview Deep Dive

A comprehensive guide to the most frequently asked Java keywords in interviews, covering **what they do**, **how they work internally**, and **tricky interview questions**.

---

## Table of Contents

1. [final](#1-final)
2. [static](#2-static)
3. [transient](#3-transient)
4. [volatile](#4-volatile)
5. [synchronized](#5-synchronized)
6. [native](#6-native)
7. [strictfp](#7-strictfp)
8. [abstract](#8-abstract)
9. [default](#9-default)
10. [sealed / permits (Java 17)](#10-sealed--permits-java-17)
11. [instanceof (Pattern Matching)](#11-instanceof--pattern-matching)
12. [this & super](#12-this--super)
13. [throw vs throws](#13-throw-vs-throws)
14. [try-with-resources & AutoCloseable](#14-try-with-resources--autocloseable)
15. [assert](#15-assert)
16. [enum](#16-enum)
17. [record (Java 16)](#17-record-java-16)
18. [var (Java 10)](#18-var-java-10)
19. [yield (Java 14)](#19-yield-java-14)

---

## 1. final

The `final` keyword can be applied to **variables**, **methods**, and **classes**. It means "cannot be changed/overridden/extended".

### final Variable

```java
final int x = 10;
x = 20;  // Compilation error — cannot reassign

final List<String> list = new ArrayList<>();
list.add("Hello");      // OK — modifying the object, not the reference
list = new ArrayList<>();  // Compilation error — cannot reassign reference
```

**Key insight**: `final` makes the **reference** constant, NOT the **object**. The object's internal state can still change.

### final with Primitives vs References

```
final int x = 10;
  x ──→ [10]           ← Value is frozen. Cannot change.

final List<String> list = new ArrayList<>();
  list ──→ [ArrayList@0x100]  ← Reference is frozen. Cannot point elsewhere.
              │
              └── ["Hello", "World"]  ← Object contents CAN change.
```

### final Method

```java
class Parent {
    public final void show() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
    @Override
    public void show() {  // Compilation error — cannot override final method
        System.out.println("Child");
    }
}
```

**Why use it?**
- Prevents subclasses from altering critical behavior
- Enables **inlining** by the JIT compiler (performance optimization)
- The JVM knows the method cannot be overridden, so it can inline the method body directly at the call site

### final Class

```java
public final class String { ... }    // Cannot be extended
public final class Integer { ... }   // Cannot be extended
public final class Math { ... }      // Cannot be extended

class MyString extends String { }    // Compilation error
```

**Why make a class final?**
- **Immutability guarantee**: Prevents subclasses from breaking immutability (e.g., String)
- **Security**: Prevents malicious subclassing
- **Design intent**: The class is complete and should not be extended

### final Parameters

```java
public void process(final String name) {
    name = "other";  // Compilation error
    // Useful in lambdas — lambdas can only capture effectively final variables
}
```

### Effectively Final (Java 8+)

A variable that is never reassigned after initialization is **effectively final**, even without the `final` keyword:

```java
String name = "John";  // Effectively final — never reassigned
Runnable r = () -> System.out.println(name);  // OK in lambda

String name2 = "John";
name2 = "Jane";  // Not effectively final — reassigned
Runnable r2 = () -> System.out.println(name2);  // Compilation error
```

### Blank Final Variable

```java
class Config {
    final int timeout;  // Blank final — not initialized at declaration

    Config(int timeout) {
        this.timeout = timeout;  // MUST be initialized in constructor (exactly once)
    }
}
```

### Interview Questions

**Q: Can a final variable be initialized in a constructor?**
Yes. A "blank final" variable declared without a value MUST be assigned exactly once in every constructor path.

**Q: Can a final method be overloaded?**
Yes. `final` prevents overriding (in subclass), not overloading (in same class).

**Q: Does final improve performance?**
Yes, slightly. The JIT compiler can inline final methods and the JVM can make optimizations knowing the value/method/class won't change. For fields, `final` also provides memory visibility guarantees (similar to volatile for construction).

**Q: What is the difference between final, finally, and finalize?**
| Keyword | Purpose |
|---------|---------|
| `final` | Makes variable/method/class immutable/non-overridable/non-extendable |
| `finally` | Block that always executes after try/catch (cleanup) |
| `finalize()` | Deprecated method called by GC before object destruction |

---

## 2. static

The `static` keyword means "belongs to the class, not to any instance".

### static Variable (Class Variable)

```java
class Counter {
    static int count = 0;  // Shared across ALL instances
    int id;

    Counter() {
        id = ++count;
    }
}

Counter c1 = new Counter();  // count=1, c1.id=1
Counter c2 = new Counter();  // count=2, c2.id=2
Counter c3 = new Counter();  // count=3, c3.id=3

// All share the SAME count variable
System.out.println(Counter.count);  // 3
```

**Memory**: Static variables are stored in the **Metaspace** (Java 8+), not on the heap with instances.

### static Method

```java
class MathUtils {
    public static int add(int a, int b) {
        return a + b;
    }
}

MathUtils.add(2, 3);  // Called on the CLASS, not an instance
```

**Restrictions of static methods:**
- Cannot access instance variables or instance methods directly
- Cannot use `this` or `super`
- Cannot be overridden (but CAN be hidden in subclass)

```java
class Parent {
    static void greet() { System.out.println("Parent"); }
}

class Child extends Parent {
    static void greet() { System.out.println("Child"); }  // Method HIDING, not overriding
}

Parent p = new Child();
p.greet();  // "Parent" — static methods resolve at compile time (not polymorphic)
```

### static Block (Static Initializer)

```java
class DatabaseConfig {
    static final Map<String, String> CONFIG;

    static {
        // Runs ONCE when the class is first loaded
        CONFIG = new HashMap<>();
        CONFIG.put("url", "jdbc:mysql://localhost:3306/db");
        CONFIG.put("driver", "com.mysql.cj.jdbc.Driver");
        System.out.println("Static block executed");
    }
}
```

**Execution order**: Static blocks run in the order they appear, ONCE, when the class is loaded by the ClassLoader.

### static Inner Class

```java
class Outer {
    private int x = 10;
    private static int y = 20;

    static class Inner {
        void show() {
            // System.out.println(x);  // Error — cannot access instance variable
            System.out.println(y);     // OK — can access static variable
        }
    }
}

Outer.Inner inner = new Outer.Inner();  // No need for Outer instance
```

### static Import

```java
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

double area = PI * r * r;       // Instead of Math.PI
double root = sqrt(16);          // Instead of Math.sqrt(16)
```

### Interview Questions

**Q: Can a static method be overridden?**
No. Static methods are resolved at **compile time** (early binding). They can be **hidden** in a subclass, but that's not polymorphism.

**Q: Can we access a static variable using an instance reference?**
Yes, but it's bad practice. `obj.staticVar` compiles to `ClassName.staticVar`. The compiler issues a warning.

**Q: What is the order of initialization?**
1. Static variables and static blocks (in source order) — once per class
2. Instance variables and instance blocks (in source order) — once per object
3. Constructor

**Q: Can a constructor be static?**
No. Constructors initialize instances — `static` contradicts that purpose.

---

## 3. transient

The `transient` keyword **excludes a field from serialization**. When an object is serialized, transient fields are skipped and get their default values upon deserialization.

### How It Works

```java
class User implements Serializable {
    private static final long serialVersionUID = 1L;

    String username;            // Serialized
    transient String password;  // NOT serialized — security sensitive
    transient int loginCount;   // NOT serialized — derived/temporary data
}

// Serialize
User user = new User();
user.username = "john";
user.password = "secret123";
user.loginCount = 42;

ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("user.ser"));
oos.writeObject(user);

// Deserialize
ObjectInputStream ois = new ObjectInputStream(new FileInputStream("user.ser"));
User restored = (User) ois.readObject();

System.out.println(restored.username);    // "john"
System.out.println(restored.password);    // null (default for String)
System.out.println(restored.loginCount);  // 0 (default for int)
```

### When to Use transient

| Use Case | Example |
|----------|---------|
| **Security** | Passwords, tokens, secrets |
| **Derived data** | Cached computations, calculated fields |
| **Non-serializable fields** | Logger, Thread, Socket, DB connections |
| **Large temporary data** | Buffers, caches that can be rebuilt |

### transient vs static

```java
class Example implements Serializable {
    static int staticVar = 100;       // NOT serialized (belongs to class, not instance)
    transient int transientVar = 200; // NOT serialized (explicitly excluded)
}
```

Both are excluded from serialization, but for different reasons:
- `static`: Not part of the object's state (class-level)
- `transient`: Part of the object's state but explicitly excluded

### Custom Serialization with transient

You can still save transient data using custom serialization:

```java
class SecureUser implements Serializable {
    String username;
    transient String password;

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(encrypt(password));  // Save encrypted version
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        password = decrypt((String) ois.readObject());  // Restore decrypted
    }
}
```

### ArrayList Uses transient

```java
// Inside ArrayList source code
transient Object[] elementData;  // Array is transient!
```

Why? Because the array may have empty slots (capacity > size). ArrayList uses custom `writeObject`/`readObject` to serialize only the actual elements, not empty slots — saving space.

### Interview Questions

**Q: What is the default value of a transient field after deserialization?**
The default value for its type: `null` for objects, `0` for numbers, `false` for boolean.

**Q: Can a final field be transient?**
Yes. But since final fields must be initialized, they'll be set by the constructor/initializer during deserialization (via `readObject` mechanism).

**Q: Can a static field be transient?**
Yes, but it's meaningless. Static fields are never serialized anyway. The compiler won't complain but it has no effect.

---

## 4. volatile

The `volatile` keyword ensures **visibility** and **ordering** of a variable across threads. It tells the JVM: "Don't cache this variable — always read from and write to main memory."

### The Problem volatile Solves

```
Without volatile:

Thread 1 (CPU Core 1)          Thread 2 (CPU Core 2)
┌──────────────┐               ┌──────────────┐
│ CPU Cache     │               │ CPU Cache     │
│ flag = true   │               │ flag = false  │  ← Stale!
└──────┬───────┘               └──────┬───────┘
       │                              │
       ▼                              ▼
┌─────────────────────────────────────────────┐
│              Main Memory                     │
│              flag = true                     │
└─────────────────────────────────────────────┘

Thread 2 may NEVER see flag = true because it reads from its CPU cache.
```

### How volatile Fixes It

```java
volatile boolean flag = false;
```

```
With volatile:

Thread 1 writes flag = true
  → Flushes to MAIN MEMORY immediately
  → Invalidates all other CPU caches

Thread 2 reads flag
  → Always reads from MAIN MEMORY
  → Sees true
```

### Visibility Guarantee

```java
class SharedResource {
    volatile boolean running = true;

    // Thread 1
    public void stop() {
        running = false;  // Write goes to main memory
    }

    // Thread 2
    public void run() {
        while (running) {  // Always reads from main memory
            // do work
        }
        System.out.println("Stopped!");  // Guaranteed to see running = false
    }
}
```

### Happens-Before Guarantee

A write to a volatile variable **happens-before** every subsequent read of that variable. This also applies to ALL variables written before the volatile write:

```java
class Example {
    int a = 0;
    int b = 0;
    volatile boolean flag = false;

    // Thread 1
    void writer() {
        a = 1;           // Regular write
        b = 2;           // Regular write
        flag = true;     // Volatile write — creates a "fence"
        // All writes before the volatile write are guaranteed
        // to be visible to any thread that reads flag == true
    }

    // Thread 2
    void reader() {
        if (flag) {      // Volatile read
            // a is guaranteed to be 1
            // b is guaranteed to be 2
        }
    }
}
```

### What volatile Does NOT Do — Atomicity

```java
volatile int count = 0;

// Thread 1 & Thread 2 both do:
count++;  // NOT atomic! This is: read count → increment → write count

// Three separate operations:
// 1. Read count from main memory (say 5)
// 2. Increment in register (6)
// 3. Write back to main memory (6)
// Another thread can read between steps 1 and 3!
```

**volatile guarantees visibility, NOT atomicity**. For atomic operations, use `AtomicInteger`, `synchronized`, or `Lock`.

### volatile vs synchronized

| Feature | volatile | synchronized |
|---------|----------|-------------|
| **Visibility** | Yes | Yes |
| **Atomicity** | No | Yes |
| **Mutual exclusion** | No | Yes |
| **Blocking** | No (lock-free) | Yes (blocks other threads) |
| **Scope** | Single variable | Block of code |
| **Performance** | Faster | Slower |
| **Use case** | Flags, single reads/writes | Compound operations |

### Double-Checked Locking (Classic volatile Use Case)

```java
class Singleton {
    private static volatile Singleton instance;  // MUST be volatile

    public static Singleton getInstance() {
        if (instance == null) {                 // 1st check (no lock)
            synchronized (Singleton.class) {
                if (instance == null) {          // 2nd check (with lock)
                    instance = new Singleton();  // Without volatile, this can be reordered!
                }
            }
        }
        return instance;
    }
}
```

**Why volatile is critical here:**

`instance = new Singleton()` is NOT atomic. It involves:
1. Allocate memory
2. Initialize the object (constructor)
3. Assign reference to `instance`

Without volatile, the JVM may **reorder** steps 2 and 3. Another thread could see a non-null `instance` that hasn't been fully constructed yet (partially constructed object).

### Interview Questions

**Q: Does volatile make operations atomic?**
No. It only guarantees visibility. `count++` on a volatile int is still not thread-safe. Use `AtomicInteger` for atomic compound operations.

**Q: Can volatile be used with local variables?**
No. Local variables are thread-confined (on the stack) — they're never shared between threads. `volatile` only makes sense for instance/static fields.

**Q: When to use volatile vs synchronized?**
- **volatile**: One thread writes, others read. Simple flags, status variables.
- **synchronized**: Multiple threads read AND write. Compound operations (check-then-act).

---

## 5. synchronized

The `synchronized` keyword provides **mutual exclusion** (only one thread at a time) and **memory visibility** (changes are visible to other threads after releasing the lock).

### How synchronized Works Internally — Monitor Lock

Every Java object has an associated **monitor** (intrinsic lock). When a thread enters a `synchronized` block, it **acquires** the monitor. Other threads trying to enter must **wait**.

```
Object lock
┌─────────────────┐
│ Object Header    │
│ ┌───────────────┐│
│ │ Mark Word      ││ ← Contains lock state
│ │ (lock info)    ││
│ └───────────────┘│
│ ┌───────────────┐│
│ │ Klass Pointer  ││
│ └───────────────┘│
└─────────────────┘

Mark Word states:
  Unlocked:    [hash | age | 0 | 01]
  Biased:      [thread_id | epoch | age | 1 | 01]
  Lightweight: [lock_record_ptr | 00]
  Heavyweight: [monitor_ptr | 10]
  GC marked:   [forwarding_ptr | 11]
```

### Lock Escalation (Biased → Lightweight → Heavyweight)

```
1. BIASED LOCK (no contention)
   → Lock is "biased" toward the first thread that acquires it
   → No atomic operations needed for subsequent acquisitions by same thread
   → Fastest — almost zero overhead

2. LIGHTWEIGHT LOCK (low contention)
   → When a second thread tries to acquire the biased lock
   → Uses CAS (Compare-And-Swap) spin on the Mark Word
   → No OS thread blocking — spins in user space

3. HEAVYWEIGHT LOCK (high contention)
   → When spinning fails (too much contention)
   → Escalates to OS-level mutex
   → Blocking — thread goes to sleep, context switch
   → Slowest
```

### Synchronized Method

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // Only one thread at a time
    }
    // Lock object: 'this' (the Counter instance)

    public static synchronized void staticMethod() {
        // Lock object: Counter.class (the Class object)
    }
}
```

### Synchronized Block

```java
class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized (lock) {  // Fine-grained — lock specific object
            count++;
        }
    }
}
```

### Reentrant Lock Behavior

`synchronized` is **reentrant** — the same thread can acquire the same lock multiple times:

```java
synchronized void methodA() {
    methodB();  // OK — same thread already holds 'this' lock
}

synchronized void methodB() {
    // Same lock, same thread — allowed (reentrant)
}
```

### wait(), notify(), notifyAll()

These methods MUST be called inside a `synchronized` block on the same object:

```java
class ProducerConsumer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 10;

    public synchronized void produce(int item) throws InterruptedException {
        while (queue.size() == CAPACITY) {
            wait();  // Release lock, go to sleep
        }
        queue.add(item);
        notifyAll();  // Wake up waiting consumers
    }

    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();  // Release lock, go to sleep
        }
        int item = queue.poll();
        notifyAll();  // Wake up waiting producers
        return item;
    }
}
```

### Interview Questions

**Q: What is the difference between synchronized method and synchronized block?**
- **Method**: Locks `this` (or `Class` for static). Entire method is synchronized.
- **Block**: Locks any specified object. Only critical section is synchronized (more granular).

**Q: Can two threads execute two different synchronized methods of the same object simultaneously?**
No. Both methods lock on `this`. Only one thread can hold the object's monitor at a time.

**Q: What happens if a synchronized method throws an exception?**
The lock is automatically released. No deadlock risk from exceptions.

**Q: Synchronized vs ReentrantLock?**
| Feature | synchronized | ReentrantLock |
|---------|-------------|---------------|
| Syntax | Keyword | API (explicit lock/unlock) |
| Try-lock | No | Yes (`tryLock()`) |
| Timed wait | No | Yes (`tryLock(timeout)`) |
| Interruptible | No | Yes (`lockInterruptibly()`) |
| Fairness | No | Optional (`new ReentrantLock(true)`) |
| Condition variables | 1 (`wait/notify`) | Multiple (`newCondition()`) |
| Automatic release | Yes (on block exit) | No (must call `unlock()` in finally) |

---

## 6. native

The `native` keyword indicates that a method is implemented in **platform-specific native code** (C/C++) via **JNI (Java Native Interface)**.

```java
public class System {
    public static native long currentTimeMillis();  // Implemented in C
    public static native void arraycopy(Object src, int srcPos, 
                                         Object dest, int destPos, int length);
}

public class Object {
    public native int hashCode();        // Default identity hash — native
    protected native Object clone();     // Native memory copy
    public final native void wait();     // OS-level thread parking
    public final native void notify();   // OS-level thread waking
}

public class Thread {
    private native void start0();        // OS thread creation
    public static native void sleep(long millis);
}
```

### When Native Methods Are Used
- **OS interaction**: File I/O, networking, thread management
- **Performance-critical code**: Math operations, array copy
- **Hardware access**: GPU, sensors, low-level I/O
- **Legacy library integration**: Calling existing C/C++ libraries

### JNI Flow

```
Java Code → JNI Bridge → Native Code (C/C++) → OS/Hardware
                ↕
        JVM manages the transition
        (argument marshalling, memory safety)
```

### Interview Questions

**Q: Can a native method have a body?**
No. It's declared with just a semicolon (like an abstract method), but it's not abstract — its body is in native code.

**Q: Can a native method be abstract?**
No. `abstract` means "no implementation, subclass must provide it." `native` means "implementation exists in native code." They're contradictory.

---

## 7. strictfp

The `strictfp` keyword ensures **consistent floating-point calculations** across all platforms. Without it, the JVM may use platform-specific extended precision (80-bit registers on x86), giving different results on different hardware.

```java
strictfp class Calculator {
    // All methods in this class use IEEE 754 standard precision
    public double compute(double a, double b) {
        return a * b + a / b;  // Same result on ALL platforms
    }
}

strictfp interface MathOperations {
    double calculate(double x);  // All implementing classes use strict FP
}
```

### Since Java 17

`strictfp` is **effectively obsolete** since Java 17. All floating-point operations now follow IEEE 754 by default. The keyword is still accepted but has no effect.

### Interview Questions

**Q: What does strictfp do?**
Ensures IEEE 754 compliance for floating-point arithmetic, making results identical across platforms. Obsolete since Java 17.

---

## 8. abstract

The `abstract` keyword applies to **classes** and **methods**. An abstract class cannot be instantiated. An abstract method has no body — subclasses must implement it.

### Abstract Class

```java
abstract class Shape {
    String color;

    // Concrete method — has implementation
    public String getColor() {
        return color;
    }

    // Abstract method — no body, must be overridden
    public abstract double area();

    // Abstract class CAN have constructors (called via super)
    Shape(String color) {
        this.color = color;
    }
}

class Circle extends Shape {
    double radius;

    Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;  // Must implement
    }
}
```

### Abstract vs Interface (Java 8+)

| Feature | Abstract Class | Interface |
|---------|---------------|-----------|
| Constructors | Yes | No |
| Instance variables | Yes | No (only `public static final`) |
| Multiple inheritance | No (single extends) | Yes (multiple implements) |
| Access modifiers | Any | `public` (methods default) |
| Default methods | N/A | Yes (Java 8+) |
| Static methods | Yes | Yes (Java 8+) |
| Private methods | Yes | Yes (Java 9+) |

### Interview Questions

**Q: Can an abstract class have a constructor?**
Yes. It cannot be instantiated directly, but subclass constructors call it via `super()`.

**Q: Can an abstract class have no abstract methods?**
Yes. It just can't be instantiated. Useful for preventing direct instantiation while providing shared code.

**Q: Can an abstract method be static?**
No. Static methods belong to the class and can't be overridden — contradicts the purpose of abstract (requiring subclass implementation).

---

## 9. default

### default in Interfaces (Java 8+)

Allows adding method implementations to interfaces **without breaking existing implementations**:

```java
interface Collection<E> {
    // Abstract method (existing)
    boolean add(E e);

    // Default method (Java 8+ — added without breaking implementations)
    default void forEach(Consumer<? super E> action) {
        for (E e : this) {
            action.accept(e);
        }
    }

    // Static method in interface (Java 8+)
    static <T> Collection<T> empty() {
        return Collections.emptyList();
    }
}
```

### Diamond Problem Resolution

```java
interface A {
    default void hello() { System.out.println("A"); }
}

interface B {
    default void hello() { System.out.println("B"); }
}

class C implements A, B {
    // MUST override — ambiguous
    @Override
    public void hello() {
        A.super.hello();  // Explicitly choose A's implementation
    }
}
```

### default in Switch

```java
switch (day) {
    case MONDAY:  System.out.println("Start"); break;
    case FRIDAY:  System.out.println("End"); break;
    default:      System.out.println("Midweek"); break;
}
```

---

## 10. sealed / permits (Java 17)

`sealed` restricts which classes can extend/implement a class/interface:

```java
public sealed class Shape permits Circle, Rectangle, Triangle {
    // Only Circle, Rectangle, Triangle can extend Shape
}

public final class Circle extends Shape { }        // final — no further extension
public sealed class Rectangle extends Shape        // sealed — further restricted
        permits Square { }
public non-sealed class Triangle extends Shape { } // non-sealed — open for extension

public final class Square extends Rectangle { }
```

### Hierarchy Rules

Subclasses of a sealed class MUST be one of:
- `final` — no further extension
- `sealed` — further restricted
- `non-sealed` — opens up for unrestricted extension

### sealed + switch (Pattern Matching)

```java
double area(Shape shape) {
    return switch (shape) {
        case Circle c    -> Math.PI * c.radius * c.radius;
        case Rectangle r -> r.width * r.height;
        case Triangle t  -> 0.5 * t.base * t.height;
        // No default needed — compiler knows all subtypes!
    };
}
```

### Interview Questions

**Q: Why use sealed classes?**
- **Exhaustive pattern matching**: Compiler knows all subtypes → no default needed in switch
- **Controlled extension**: Library authors control the type hierarchy
- **Better modeling**: Algebraic data types (sum types)

---

## 11. instanceof + Pattern Matching

### Traditional instanceof

```java
if (obj instanceof String) {
    String s = (String) obj;  // Explicit cast needed
    System.out.println(s.length());
}
```

### Pattern Matching instanceof (Java 16+)

```java
if (obj instanceof String s) {   // Cast + bind in one step
    System.out.println(s.length());  // 's' is already a String
}

// Works with negation too
if (!(obj instanceof String s)) {
    return;
}
// 's' is in scope here

// In complex conditions
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s);
}
```

### Guarded Patterns (Java 21+)

```java
switch (obj) {
    case Integer i when i > 0  -> System.out.println("Positive: " + i);
    case Integer i             -> System.out.println("Non-positive: " + i);
    case String s when s.isEmpty() -> System.out.println("Empty string");
    case String s              -> System.out.println("String: " + s);
    case null                  -> System.out.println("Null!");
    default                    -> System.out.println("Other");
}
```

---

## 12. this & super

### this

```java
class Employee {
    String name;

    // 1. Distinguish instance variable from parameter
    Employee(String name) {
        this.name = name;  // this.name = instance variable, name = parameter
    }

    // 2. Constructor chaining
    Employee() {
        this("Unknown");  // Calls Employee(String) — must be first statement
    }

    // 3. Pass current object
    void register() {
        EventBus.register(this);  // Passes current instance
    }

    // 4. Return current object (fluent API / builder pattern)
    Employee setName(String name) {
        this.name = name;
        return this;
    }
}
```

### super

```java
class Animal {
    String name;
    Animal(String name) { this.name = name; }
    void speak() { System.out.println("..."); }
}

class Dog extends Animal {
    Dog(String name) {
        super(name);  // 1. Call parent constructor — must be first statement
    }

    @Override
    void speak() {
        super.speak();  // 2. Call parent's method
        System.out.println("Woof!");
    }
}
```

### Interview Questions

**Q: Can this() and super() both appear in the same constructor?**
No. Both must be the first statement — only one can be first. Use this() for constructor chaining within the same class, super() for parent constructor.

**Q: Can this be used in a static context?**
No. `this` refers to the current instance. Static methods have no instance.

---

## 13. throw vs throws

```java
// throws — declares that a method MAY throw exceptions (checked)
public void readFile(String path) throws IOException, FileNotFoundException {
    // ...
    if (!file.exists()) {
        throw new FileNotFoundException("File not found: " + path);  // throw — actually throws
    }
}
```

| Feature | throw | throws |
|---------|-------|--------|
| **Purpose** | Actually throws an exception | Declares possible exceptions |
| **Location** | Inside method body | Method signature |
| **Count** | One exception at a time | Multiple exceptions (comma-separated) |
| **Followed by** | Exception object | Exception class names |

### Checked vs Unchecked

```java
// Checked — MUST be declared in throws or caught
throw new IOException("...");        // Must handle
throw new SQLException("...");       // Must handle

// Unchecked (RuntimeException) — no throws required
throw new NullPointerException();     // Optional to declare
throw new IllegalArgumentException(); // Optional to declare
```

---

## 14. try-with-resources & AutoCloseable

Automatically closes resources when the try block exits:

```java
// Before Java 7 — manual close
BufferedReader br = null;
try {
    br = new BufferedReader(new FileReader("file.txt"));
    String line = br.readLine();
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (br != null) {
        try { br.close(); } catch (IOException e) { /* swallowed */ }
    }
}

// Java 7+ — try-with-resources
try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
    String line = br.readLine();
} catch (IOException e) {
    e.printStackTrace();
}
// br.close() is called automatically, even if an exception occurs
```

### Custom AutoCloseable

```java
class DatabaseConnection implements AutoCloseable {
    public DatabaseConnection() {
        System.out.println("Connection opened");
    }

    @Override
    public void close() {
        System.out.println("Connection closed");  // Called automatically
    }
}

try (DatabaseConnection conn = new DatabaseConnection()) {
    // use connection
}  // close() called here — whether or not an exception occurred
```

### Suppressed Exceptions

If both the try block AND the `close()` method throw exceptions:

```java
try (MyResource r = new MyResource()) {
    throw new Exception("try exception");
}
// close() also throws an exception → it becomes a SUPPRESSED exception

// Access suppressed exceptions:
catch (Exception e) {
    Throwable[] suppressed = e.getSuppressed();
}
```

---

## 15. assert

Used for **debugging** — validates assumptions during development:

```java
assert x > 0;                           // Throws AssertionError if x ≤ 0
assert x > 0 : "x must be positive";    // With error message

// Assertions are DISABLED by default at runtime
// Enable with: java -ea MyApp (or -enableassertions)
```

**Never use for production validation** — use exceptions instead. Assertions can be disabled.

---

## 16. enum

Enums are **full classes** in Java, not just integer constants.

### Enum Internals

```java
public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;
}

// Compiler generates (roughly):
public final class Season extends Enum<Season> {
    public static final Season SPRING = new Season("SPRING", 0);
    public static final Season SUMMER = new Season("SUMMER", 1);
    public static final Season AUTUMN = new Season("AUTUMN", 2);
    public static final Season WINTER = new Season("WINTER", 3);

    private static final Season[] VALUES = {SPRING, SUMMER, AUTUMN, WINTER};

    public static Season[] values() { return VALUES.clone(); }
    public static Season valueOf(String name) { ... }
}
```

### Enum with Fields and Methods

```java
public enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    VENUS(4.869e+24, 6.0518e6),
    EARTH(5.976e+24, 6.37814e6);

    private final double mass;
    private final double radius;

    Planet(double mass, double radius) {  // Constructor is implicitly private
        this.mass = mass;
        this.radius = radius;
    }

    double surfaceGravity() {
        return 6.67300E-11 * mass / (radius * radius);
    }
}
```

### Enum Singleton (Thread-Safe)

```java
public enum Singleton {
    INSTANCE;

    public void doSomething() { ... }
}

// Usage: Singleton.INSTANCE.doSomething();
// JVM guarantees: single instance, thread-safe, serialization-safe
```

### Interview Questions

**Q: Can enum extend another class?**
No. All enums implicitly extend `java.lang.Enum`. Java doesn't support multiple inheritance.

**Q: Can enum implement interfaces?**
Yes. Enums can implement any number of interfaces.

**Q: Why is enum considered the best singleton implementation?**
Thread-safe (JVM guarantees), serialization-safe (no duplicate instances), reflection-safe (can't create instances via reflection), and concise.

---

## 17. record (Java 16)

Records are **immutable data carriers** — automatically generate constructor, getters, `equals()`, `hashCode()`, and `toString()`.

```java
public record Point(int x, int y) { }

// Compiler generates:
// - Constructor: Point(int x, int y)
// - Accessors: x(), y() (NOT getX/getY)
// - equals(), hashCode(), toString()
// - All fields are private final

Point p = new Point(3, 4);
p.x();          // 3
p.y();          // 4
p.toString();   // "Point[x=3, y=4]"
```

### Customization

```java
public record Person(String name, int age) {
    // Compact constructor — validation
    public Person {
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
        name = name.trim();  // Can modify before assignment
    }

    // Custom methods
    public String greeting() {
        return "Hello, " + name;
    }

    // Static methods and fields
    public static Person unknown() {
        return new Person("Unknown", 0);
    }
}
```

### Restrictions

- Cannot extend another class (implicitly extends `Record`)
- All fields are `final` — immutable
- Cannot declare instance fields outside the header
- Can implement interfaces

---

## 18. var (Java 10)

Local variable type inference — the compiler infers the type:

```java
var list = new ArrayList<String>();   // Inferred as ArrayList<String>
var map = Map.of("key", "value");     // Inferred as Map<String, String>
var stream = list.stream();           // Inferred as Stream<String>

// Useful for complex generic types
var entries = map.entrySet().iterator();
// Instead of: Iterator<Map.Entry<String, String>> entries = ...
```

### Restrictions

```java
var x;              // Error — no initializer
var x = null;       // Error — can't infer type from null
var x = {1, 2, 3};  // Error — can't infer array type
var x = () -> 42;   // Error — can't infer lambda type

// Cannot use for:
// - Method parameters
// - Return types
// - Fields (instance/static)
```

### Interview Questions

**Q: Is `var` a keyword?**
No, it's a **reserved type name**. You can still use `var` as a variable name (backward compatibility), but not as a class or interface name.

**Q: Does var make Java dynamically typed?**
No. The type is still inferred at **compile time**. It's just syntactic sugar — the bytecode is identical.

---

## 19. yield (Java 14)

Used in **switch expressions** to return a value from a block:

```java
// Arrow syntax — implicit yield
int numDays = switch (month) {
    case JANUARY, MARCH -> 31;
    case FEBRUARY -> 28;
    case APRIL, JUNE -> 30;
    default -> throw new IllegalArgumentException();
};

// Block syntax — explicit yield
int numDays = switch (month) {
    case FEBRUARY: {
        if (isLeapYear) {
            yield 29;  // Returns value from the switch expression
        } else {
            yield 28;
        }
    }
    default:
        yield 30;
};
```

**yield vs return**: `return` exits the method. `yield` exits only the switch expression.

---

## Master Comparison Table

| Keyword | Applied To | Purpose |
|---------|-----------|---------|
| `final` | Variable, Method, Class | Immutable reference / no override / no extend |
| `static` | Variable, Method, Block, Class | Class-level (no instance needed) |
| `transient` | Field | Excluded from serialization |
| `volatile` | Field | Thread visibility, no CPU caching |
| `synchronized` | Method, Block | Mutual exclusion + visibility |
| `native` | Method | Implemented in C/C++ via JNI |
| `strictfp` | Class, Interface, Method | IEEE 754 floating-point (obsolete Java 17+) |
| `abstract` | Class, Method | Incomplete — must be subclassed/implemented |
| `default` | Interface method | Provides default implementation |
| `sealed` | Class, Interface | Restricts permitted subclasses |
| `record` | Class | Immutable data carrier with auto-generated methods |
| `var` | Local variable | Type inference |
| `yield` | Switch expression | Returns value from switch block |
| `assert` | Statement | Debug-time invariant check |
| `enum` | Type | Type-safe constant set with class features |
