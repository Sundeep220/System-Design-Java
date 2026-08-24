# String Pool — Deep Dive

The String Pool (also called String Intern Pool or String Literal Pool) is a special memory area in the JVM heap where **String literals are stored and reused**. Understanding it is critical for both performance and correctness in Java.

> **Java Strings are immutable. The JVM exploits this immutability to reuse String literals — multiple variables pointing to the same `"hello"` string in memory rather than creating duplicate copies. This is the String Pool.**

---

# 1. What is the String Pool?

```java
String a = "hello";
String b = "hello";
```

Are `a` and `b` the same object in memory?

```text
Without pool:
  "hello" [object 1]   ← a
  "hello" [object 2]   ← b
  Two separate objects, duplicate memory

With pool (actual Java behavior):
  String Pool:
    "hello" [ONE object]
             ↑       ↑
             a       b

  a and b point to the SAME object
```

This is the String Pool — a cache of string literals backed by a hash map inside the JVM.

---

# 2. String Literals vs new String()

This is the most fundamental distinction:

```java
String a = "hello";              // goes to pool
String b = "hello";              // same pool entry as a
String c = new String("hello");  // new object on heap, NOT in pool
String d = new String("hello");  // another new object on heap

System.out.println(a == b);      // true  — same pool object
System.out.println(a == c);      // false — c is a different heap object
System.out.println(c == d);      // false — c and d are different heap objects
System.out.println(a.equals(c)); // true  — same content (equals() compares value)
```

Visualization:

```text
String Pool (in Heap):
  +----------+
  | "hello"  | ← a, b both point here
  +----------+

Heap (outside pool):
  +----------+  +----------+
  | "hello"  |  | "hello"  |  ← c and d (separate objects)
  +----------+  +----------+
```

**Key rule:**
- `==` compares references (memory addresses)
- `.equals()` compares content (character values)
- For strings, ALWAYS use `.equals()` for content comparison

---

# 3. Where is the String Pool located?

This has changed across Java versions:

```text
Java 6 and earlier:
  String Pool → PermGen (permanent generation, fixed size)
  Problem: too many interned strings → OutOfMemoryError: PermGen space

Java 7+:
  String Pool → Java Heap
  Benefit: GC can collect unreachable interned strings
           Heap is larger, dynamically sized

Java 8+:
  String Pool → Java Heap (same as Java 7)
  PermGen replaced by Metaspace (but String Pool is still in Heap, NOT Metaspace)
```

**Common interview trap:** "Is the String Pool in Metaspace?"

**Answer:** NO. String Pool is in the **Java Heap**. Metaspace stores class metadata, not String objects.

---

# 4. How Strings Enter the Pool

### Path 1: String Literals (automatic)

```java
String s = "hello";  // automatically added to pool by compiler/JVM
```

The compiler replaces string literals with constants. At class load time, these constants reference the pool.

### Path 2: String.intern() (manual)

```java
String s = new String("hello");  // on heap, NOT in pool
String pooled = s.intern();      // manually add to pool (or get existing)

// Now:
System.out.println(s == pooled);              // false (s is outside pool)
System.out.println("hello" == pooled);         // true (same pool object!)
System.out.println(s.equals(pooled));          // true (same content)
```

`intern()` returns the pool's canonical representation:
- If pool already has `"hello"` → returns the existing pool reference
- If pool doesn't have it → adds this string to the pool, returns it

---

# 5. Compile-Time String Constants

The Java compiler is smart about string literals:

```java
String a = "hello";
String b = "hel" + "lo";  // compile-time constant — compiler merges to "hello"
String c = "hel";
String d = c + "lo";      // runtime concatenation — not a constant!

System.out.println(a == b);  // true  — same pool entry (compiler optimized)
System.out.println(a == d);  // false — d is a new heap String from runtime concat
```

Why `a == b` is true: the compiler computes `"hel" + "lo"` at compile time and produces `"hello"` — exactly the same literal as `a`. They share the same pool entry.

Why `a == d` is false: `c + "lo"` is computed at runtime (c could theoretically change — even though it won't) → creates a new String object on the heap.

---

# 6. String Immutability and the Pool

Strings can be safely shared in a pool because they are **immutable** — once created, their content never changes.

```java
String s = "hello";
// Cannot do: s.setChar(0, 'H');  // NO SUCH METHOD
// s.toUpperCase() returns a NEW string, s is unchanged

String upper = s.toUpperCase();  // new "HELLO" object

// s is still "hello"
// upper is "HELLO"
// If "HELLO" was already in the pool, toUpperCase() returns that pool entry
```

If Strings were mutable, sharing them would be catastrophic:
```text
String a = "hello"  → pool entry
String b = "hello"  → same pool entry

// If mutable:
a.modify(0, 'H')
// Now b also shows "Hello"! — shared object corrupted
```

Immutability makes the pool safe.

---

# 7. String Interning Performance — When to Use intern()

### Use case: deduplication of many identical strings

```java
// Scenario: reading 10 million CSV records where "country" field repeats
// Without intern: 10M String objects in heap for country names
// With intern: ~200 unique country name objects in pool

String country = record.getCountry().intern();  // deduplicates
```

### When intern() is beneficial:

```text
✓ Reading large datasets with repeated string values
✓ String keys in large in-memory structures
✓ Reducing memory footprint when many identical strings exist
✓ Caching/lookups where == comparison (after interning) is faster than .equals()
```

### When intern() is NOT beneficial:

```text
✗ Strings that are already unique (no duplicates to save)
✗ High intern() call frequency — intern() has internal synchronization overhead
✗ Random UUIDs, timestamps — all unique, pool just grows
✗ Short-lived strings — pool keeps them alive (no GC even if you're done with them)
```

---

# 8. String Pool Memory Implications

### Strings in the pool are STRONGLY referenced

Once in the pool, a String stays until:
- The pool entry becomes unreachable (Java 7+, since pool is in heap, GC can collect)
- The JVM shuts down

In Java 6 (pool in PermGen), interned strings stayed FOREVER — a common source of PermGen OOM.

In Java 7+, the GC can collect pool entries when they become unreachable:

```java
String s = new String("temp").intern();  // added to pool
s = null;  // s reference dropped
// If no other reference points to "temp" in the pool → can be GC'd
```

### Controlling pool size

```bash
# Set the initial capacity of the String pool hash table (bucket count)
-XX:StringTableSize=65536  # default varies; prime number recommended

# In Java 8u40+, default was increased to 60013
# For large applications with many unique strings, increase this
```

---

# 9. String Pool Internals

The String Pool is implemented as a fixed-size hash table (like a HashMap) inside the JVM.

```text
String Pool (hash table):
  Bucket 0: → "apple" → null
  Bucket 1: → null
  Bucket 2: → "banana" → "basil" → null  (hash collision chain)
  Bucket 3: → "cherry" → null
  ...
  Bucket N: → null
```

Each bucket holds a chain of String entries with the same hash.

`String.intern()` operation:
1. Compute hash of the String's content
2. Find the bucket
3. Scan chain for a string with equal content
4. If found: return the existing pool reference
5. If not found: add to pool, return this reference

This is `O(k)` where k = chain length (usually O(1) with good sizing).

---

# 10. String Concatenation and the Pool

```java
// Case 1: all literals → compile-time constant → pool
String s = "hello" + " " + "world";  // → "hello world" in pool

// Case 2: runtime concatenation → heap
String name = "Java";
String s = "Hello " + name;  // → new String on heap (NOT in pool)

// Case 3: explicitly interned
String s = ("Hello " + name).intern();  // → into pool
```

String concatenation with `+` on non-constant strings uses `StringBuilder` internally:

```java
String s = "Hello " + name + "!";
// JVM compiles to approximately:
String s = new StringBuilder()
    .append("Hello ")
    .append(name)
    .append("!")
    .toString();
// → new String object on heap, not in pool
```

---

# 11. Real-life Impact

### In a microservice processing JSON:

```java
// HTTP request body parsed from JSON
String action = jsonNode.get("action").asText();

// action might be "CREATE", "UPDATE", "DELETE"
// These repeat across millions of requests
// Without intern: millions of "CREATE" String objects
// With intern (if appropriate):
action = action.intern();
// Now all "CREATE" strings share one pool reference
```

### In a Spring Boot application:

```text
Spring's @Value, @RequestParam, HTTP headers — many repeated strings
Spring internally uses intern() in some places for efficiency

Common repeated strings:
  HTTP methods: "GET", "POST", "PUT", "DELETE"
  Content-Type: "application/json"
  Header names: "Authorization", "Content-Type", "Accept"

These are often pooled automatically (they're literals in the source code)
```

---

# 12. Summary

```text
String Pool:
  Location:    Java Heap (Java 7+)
  Purpose:     Cache of string literals — reuse identical strings
  How to add:  Automatically for string literals
               Manually via String.intern()

"abc" == "abc"        → true (same pool entry)
new String("abc") == new String("abc") → false (different heap objects)
Always use .equals() for string comparison!

Compile-time constants are pooled:
  "a" + "b" == "ab"  → true (compiler merges)
  
Runtime concatenation is NOT pooled:
  var a = "a"; (a + "b") == "ab" → false
```

---

# Interview Preparation — String Pool

---

## Q1: What is the String Pool and where is it located?

**Answer:**

The String Pool (intern pool) is a special cache inside the JVM that stores unique String literals. When you write `"hello"` in code, the JVM ensures only one object representing `"hello"` exists in the pool — all variables assigned this literal share the same object reference.

Location:
- Java 6 and earlier: **PermGen** (fixed size, could cause OOM)
- Java 7+: **Java Heap** (GC can collect unreachable entries, dynamic sizing)
- Java 8+: Still in **Heap** (NOT Metaspace — common interview trap)

---

## Q2: What is the difference between String s = "hello" and String s = new String("hello")?

**Answer:**

```java
String a = "hello";              // goes to String Pool — one shared object
String b = "hello";              // same pool object as a

String c = new String("hello");  // always creates new heap object, bypasses pool
String d = new String("hello");  // another new heap object
```

- `a == b` → `true` (same pool object)
- `a == c` → `false` (c is a separate heap object)
- `a.equals(c)` → `true` (same content)

**Always use `.equals()` for string content comparison, never `==`.**

The `new String()` form is almost never needed and wastes memory. Prefer string literals.

---

## Q3: What does String.intern() do?

**Answer:**

`String.intern()` returns the pool's canonical copy of the string:
- If the pool contains a string equal to this string → returns the pool reference
- If not → adds this string to the pool and returns it

```java
String s = new String("hello");  // heap object, not in pool
String p = s.intern();           // returns pool entry for "hello"
System.out.println(p == "hello"); // true — same pool object
```

Use cases: deduplicating large numbers of identical strings in memory (e.g., parsing CSV/JSON with repeated field values). Avoid for unique strings or high-frequency calls (intern() has synchronization overhead).

---

## Q4: Why is String immutability important for the String Pool?

**Answer:**

The String Pool works only because Strings are immutable. If two variables point to the same "hello" pool object, neither can modify it — so the shared reference is safe.

If Strings were mutable:
```text
String a = "hello" → pool["hello"]
String b = "hello" → same pool["hello"]
a.modify(0, 'H')  → pool["Hello"]
// b now shows "Hello" too! — catastrophic
```

Immutability guarantees sharing is safe. It also enables:
- String hashCode caching (computed once, cached in object)
- Safe use as HashMap/ConcurrentHashMap keys
- Safe sharing across threads without synchronization

---

## Q5: Will "hello" + "world" be in the String Pool?

**Answer:**

```java
String s1 = "hello" + "world";       // YES — compile-time constant → "helloworld" in pool
String s2 = "helloworld";             // YES — same pool entry as s1
System.out.println(s1 == s2);        // true

String x = "hello";
String s3 = x + "world";             // NO — runtime concatenation → heap object
System.out.println(s1 == s3);        // false
System.out.println(s1.equals(s3));   // true (same content)
```

The compiler evaluates `"hello" + "world"` at compile time to the single constant `"helloworld"` and places it in the pool. Runtime concatenation (where at least one operand is a variable) produces a new heap `String` via `StringBuilder.toString()`.

---

## Q6: Can Strings in the pool be garbage collected?

**Answer:**

In Java 7+, YES — because the pool is in the Java Heap. If no code references a pooled String, the GC can collect it.

In Java 6 (PermGen), NO — pool entries lasted for the JVM lifetime, which caused PermGen OOM with excessive `intern()` usage.

Practical implication: in Java 7+, it's generally safe to call `intern()` on transient strings — they will be collected when no longer referenced. However, long-lived interned strings (kept alive by code that holds references) still accumulate.

---

## Q7: How does the String Pool size affect performance?

**Answer:**

The pool is implemented as a hash table. If the table is too small for the number of unique strings, many strings end up in the same bucket (hash collisions) → `intern()` becomes O(n) chain traversal instead of O(1).

Tune with:
```bash
-XX:StringTableSize=131072  # power of 2 or prime; increase for apps with many unique strings
```

Monitor with:
```bash
jcmd <pid> VM.stringtable   # shows pool statistics: size, count, buckets
```

For applications that intern many strings (parsing-heavy), increasing `StringTableSize` can reduce `intern()` contention and improve lookup speed.
