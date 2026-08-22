Absolutely. This is one of the **most important Java OOP topics for interviews**, because method overloading and overriding are often mixed up.

The easiest way to master them is to understand **what the compiler decides** versus **what the JVM decides at runtime**.

---

# 1. The Big Picture

There are two different concepts:

|                | Method Overloading                 | Method Overriding          |
| -------------- | ---------------------------------- | -------------------------- |
| Happens when   | Same class / inheritance hierarchy | Parent-child relationship  |
| Method name    | Same                               | Same                       |
| Parameters     | **Must be different**              | **Must be same**           |
| Return type    | Can differ, but not by itself      | Same or covariant          |
| Binding        | **Compile time**                   | **Runtime**                |
| Polymorphism   | Compile-time polymorphism          | Runtime polymorphism       |
| Main mechanism | Compiler chooses method            | JVM chooses implementation |
| `static`       | Can be overloaded                  | Cannot truly be overridden |
| `private`      | Can be overloaded                  | Cannot be overridden       |
| `final`        | Can be overloaded                  | Cannot be overridden       |

The most important sentence:

> **Overloading asks: "Which method signature should I call?"**
> **Overriding asks: "Which implementation of that method should execute?"**

---

# 2. Method Overloading

Suppose we have:

```java
class Calculator {

    int add(int a, int b) {
        return a + b;
    }

    int add(int a, int b, int c) {
        return a + b + c;
    }

    double add(double a, double b) {
        return a + b;
    }

    String add(String a, String b) {
        return a + b;
    }
}
```

Here we have four methods named:

```text
add
```

But their parameter lists are different.

```text
add(int, int)
add(int, int, int)
add(double, double)
add(String, String)
```

This is **method overloading**.

---

# 3. How Does Java Know Which Overloaded Method to Call?

Consider:

```java
Calculator calculator = new Calculator();

System.out.println(calculator.add(10, 20));
System.out.println(calculator.add(10, 20, 30));
System.out.println(calculator.add(10.5, 20.5));
System.out.println(calculator.add("Hello ", "World"));
```

Output:

```text
30
60
31.0
Hello World
```

The compiler looks at the arguments.

For:

```java
calculator.add(10, 20);
```

`10` and `20` are `int`.

So compiler selects:

```java
add(int, int)
```

For:

```java
calculator.add(10, 20, 30);
```

compiler selects:

```java
add(int, int, int)
```

So:

```text
                 add(...)
                    |
        +-----------+-----------+
        |           |           |
    int,int    int,int,int   double,double
```

This decision happens at **compile time**.

That's why overloading is called:

> **Compile-time polymorphism**

---

# 4. What Counts as a Different Method?

This is important.

The **parameter list** must be different.

### Different number of parameters

```java
void print(int x) {}

void print(int x, int y) {}
```

Valid.

---

### Different parameter types

```java
void print(int x) {}

void print(String x) {}
```

Valid.

---

### Different order of parameter types

```java
void print(int x, String y) {}

void print(String x, int y) {}
```

Valid.

Because:

```text
(int, String)
```

and

```text
(String, int)
```

are different signatures.

---

# 5. Return Type Alone Cannot Overload a Method

This is a common interview question.

This is **NOT valid**:

```java
class Test {

    int getValue() {
        return 10;
    }

    double getValue() {
        return 10.5;
    }
}
```

Why?

Because the parameter list is identical:

```text
getValue()
```

Java cannot determine which one you mean:

```java
getValue();
```

The return type isn't enough.

Therefore:

> **Return type is not part of method overloading resolution.**

---

# 6. Overloading + Type Conversion

Now things become interesting.

```java
class Calculator {

    void calculate(int x) {
        System.out.println("int");
    }

    void calculate(long x) {
        System.out.println("long");
    }

    void calculate(double x) {
        System.out.println("double");
    }
}
```

Now:

```java
Calculator c = new Calculator();

c.calculate(10);
```

Output:

```text
int
```

Because `10` is an `int`.

But:

```java
c.calculate(10L);
```

Output:

```text
long
```

And:

```java
c.calculate(10.5);
```

Output:

```text
double
```

---

# 7. Widening During Overloading

What happens here?

```java
c.calculate((short) 10);
```

There is no:

```java
calculate(short)
```

But Java can widen:

```text
short → int → long → float → double
```

So it selects:

```java
calculate(int)
```

Output:

```text
int
```

Java generally prefers the **closest applicable conversion**.

---

# 8. Overloading with Objects

This is where your original example becomes interesting.

Consider:

```java
class Printer {

    void print(Object obj) {
        System.out.println("Object");
    }

    void print(String str) {
        System.out.println("String");
    }
}
```

Now:

```java
Printer printer = new Printer();

printer.print("Hello");
```

Output:

```text
String
```

Why?

Because `"Hello"` is a `String`.

And:

```text
String
   ↓
Object
```

So both methods are technically applicable:

```java
print(String)
print(Object)
```

But `print(String)` is more specific.

Therefore:

```text
String
```

wins.

---

# 9. Now We Enter Overriding

Overriding requires **inheritance**.

Example:

```java
class Animal {

    void sound() {
        System.out.println("Animal sound");
    }
}
```

Child:

```java
class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Dog barks");
    }
}
```

Now:

```java
Animal animal = new Dog();

animal.sound();
```

Output:

```text
Dog barks
```

This is overriding.

Why?

The parent declares:

```java
void sound()
```

The child provides another implementation of the **same method**:

```java
void sound()
```

So:

```text
Animal
   |
   | sound()
   |
   ↓
Dog
   |
   | overrides sound()
```

---

# 10. Why Does Dog Execute?

This is the key difference.

Look at:

```java
Animal animal = new Dog();
```

There are two types involved.

### Reference type

```java
Animal
```

### Actual object type

```java
Dog
```

At compile time, Java sees:

```text
Animal animal
```

At runtime, the object is:

```text
new Dog()
```

For overridden methods, Java uses the **actual object type** at runtime.

Therefore:

```java
animal.sound();
```

executes:

```java
Dog.sound()
```

This is called:

> **Dynamic method dispatch**

---

# 11. The Most Important Mental Model

Think about every method call in two stages.

```text
             method call
                  |
                  ↓
        ┌───────────────────┐
        │ 1. Compile time   │
        │ Find signature    │
        └─────────┬─────────┘
                  ↓
        ┌───────────────────┐
        │ 2. Runtime        │
        │ Find implementation│
        └───────────────────┘
```

But this second step only matters for methods that participate in runtime dispatch.

This gives us:

```text
OVERLOADING
    ↓
Which signature?
    ↓
Compile time


OVERRIDING
    ↓
Which implementation?
    ↓
Runtime
```

---

# 12. Now Your Trick Question

Let's revisit your example.

Parent:

```java
class Parent {

    void print(Object obj) {
        System.out.println("Parent Object");
    }
}
```

Child:

```java
class Child extends Parent {

    void print(String obj) {
        System.out.println("Child String");
    }
}
```

Now:

```java
Parent p = new Child();

p.print("hello");
```

Many people think:

```text
Child String
```

But that's wrong.

The answer is:

```text
Parent Object
```

Let's understand **exactly why**.

---

# 13. Step 1 — Look at the Reference Type

We have:

```java
Parent p = new Child();
```

The reference type is:

```text
Parent
```

Therefore, at compile time, the compiler looks at the methods available through `Parent`.

Parent has:

```java
print(Object)
```

It does NOT have:

```java
print(String)
```

So the compiler resolves:

```java
p.print("hello");
```

to:

```java
print(Object)
```

---

# 14. Step 2 — Runtime Dispatch

Now we have:

```java
print(Object)
```

The compiler has already selected the signature.

Now runtime asks:

> Does the actual object override `print(Object)`?

Actual object:

```java
new Child()
```

Does Child have:

```java
print(Object)
```

?

No.

It only has:

```java
print(String)
```

And:

```java
print(String)
```

is **not an override** of:

```java
print(Object)
```

They're different signatures.

Therefore Parent's implementation executes.

Output:

```text
Parent Object
```

---

# 15. Why Isn't `print(String)` an Override?

This is extremely important.

Parent:

```java
void print(Object obj)
```

Child:

```java
void print(String obj)
```

Compare:

```text
Parent:
print(Object)

Child:
print(String)
```

The parameter types differ.

Therefore:

```text
print(Object) ≠ print(String)
```

So this is **overloading**, not overriding.

You can make this obvious to yourself by adding:

```java
@Override
void print(String obj) {
    System.out.println("Child String");
}
```

The compiler will give an error because there is no matching parent method to override.

That's why:

> **Always use `@Override` when you intend to override.**

It lets the compiler catch mistakes like this.

---

# 16. Your Second Example

Now:

```java
class Parent {

    void process(Object obj) {
        System.out.println("Parent Object");
    }

    void process(String str) {
        System.out.println("Parent String");
    }
}
```

Child:

```java
class Child extends Parent {

    @Override
    void process(Object obj) {
        System.out.println("Child Object");
    }
}
```

Now:

```java
Parent p = new Child();

p.process("hello");
```

What happens?

This is where the **two-stage model** becomes extremely useful.

---

# 17. Stage 1 — Overload Resolution

The compiler sees the reference type:

```java
Parent
```

Parent has:

```java
process(Object)
process(String)
```

Argument:

```java
"hello"
```

Type:

```text
String
```

Both methods could theoretically accept it:

```text
process(String)   ← exact match
process(Object)   ← String can be treated as Object
```

Compiler chooses the more specific:

```java
process(String)
```

So compile-time resolution produces:

```text
process(String)
```

---

# 18. Stage 2 — Runtime Dispatch

Now JVM asks:

> Does Child override `process(String)`?

Child has:

```java
process(Object)
```

but NOT:

```java
process(String)
```

Therefore:

```java
Child.process(String)
```

doesn't exist.

So the inherited:

```java
Parent.process(String)
```

executes.

Output:

```text
Parent String
```

This is the result:

```text
Parent String
```

---

# 19. The Complete Flow

For:

```java
Parent p = new Child();

p.process("hello");
```

think:

```text
                   p.process("hello")
                           |
                           ↓
                 Reference type = Parent
                           |
                           ↓
              ┌─────────────────────────┐
              │ Compile-time overload   │
              │ resolution              │
              └────────────┬────────────┘
                           ↓
                    process(String)
                           |
                           ↓
              ┌─────────────────────────┐
              │ Runtime dispatch        │
              │                         │
              │ Does Child override     │
              │ process(String)?        │
              └────────────┬────────────┘
                           ↓
                          NO
                           |
                           ↓
                Parent.process(String)
                           |
                           ↓
                   "Parent String"
```

---

# 20. An Even Better Example

Let's make the child override both methods.

```java
class Parent {

    void process(Object obj) {
        System.out.println("Parent Object");
    }

    void process(String str) {
        System.out.println("Parent String");
    }
}
```

```java
class Child extends Parent {

    @Override
    void process(Object obj) {
        System.out.println("Child Object");
    }

    @Override
    void process(String str) {
        System.out.println("Child String");
    }
}
```

Now:

```java
Parent p = new Child();

p.process("hello");
```

### Compile time

Compiler chooses:

```java
process(String)
```

### Runtime

Child overrides:

```java
process(String)
```

Therefore:

```text
Child String
```

---

# 21. What If the Argument Is Object?

Consider:

```java
Object obj = "hello";

p.process(obj);
```

Notice something subtle.

The actual object is a String:

```text
new String(...)
```

But the **compile-time type of the variable** is:

```text
Object
```

Therefore overload resolution sees:

```java
process(Object)
process(String)
```

but the argument's compile-time type is:

```text
Object
```

So it selects:

```java
process(Object)
```

Then runtime dispatch occurs.

Child overrides `process(Object)`.

Therefore:

```text
Child Object
```

This is a **very important interview example**.

---

# 22. Compare These Two

### Case 1

```java
String obj = "hello";

p.process(obj);
```

Compiler sees:

```text
String
```

Therefore:

```text
process(String)
```

Runtime:

```text
Child.process(String)
```

Output:

```text
Child String
```

---

### Case 2

```java
Object obj = "hello";

p.process(obj);
```

Compiler sees:

```text
Object
```

Therefore:

```text
process(Object)
```

Runtime:

```text
Child.process(Object)
```

Output:

```text
Child Object
```

Even though the actual object in both cases is a String!

That's because:

> **Overloading depends on compile-time types.**

---

# 23. This Is the Core Difference

Remember this table:

```text
                 OVERLOADING             OVERRIDING

Method name      Same                    Same

Parameters       Different              Same

Relationship     Same class or           Parent-child
                 inheritance

Decision         Compile time            Runtime

Based on         Reference/argument      Actual object
                 compile-time types      type

Purpose          Different ways          Different
                 to call a method        implementation
```

---

# 24. Classic Interview Trap

Consider:

```java
class Animal {

    void eat(Object obj) {
        System.out.println("Animal Object");
    }
}
```

```java
class Dog extends Animal {

    void eat(String str) {
        System.out.println("Dog String");
    }
}
```

Now:

```java
Animal animal = new Dog();

animal.eat("hello");
```

Answer:

```text
Animal Object
```

Not:

```text
Dog String
```

Why?

Because:

```java
eat(String)
```

is not an override of:

```java
eat(Object)
```

It is a new overloaded method in `Dog`.

---

# 25. Another Trap — Both Parent and Child Have Overloads

```java
class Parent {

    void test(Object obj) {
        System.out.println("Parent Object");
    }

    void test(String str) {
        System.out.println("Parent String");
    }
}
```

```java
class Child extends Parent {

    @Override
    void test(Object obj) {
        System.out.println("Child Object");
    }

    void test(Integer value) {
        System.out.println("Child Integer");
    }
}
```

Now:

```java
Parent p = new Child();

p.test(10);
```

What happens?

The compiler only considers methods visible through:

```text
Parent
```

So it sees:

```text
test(Object)
test(String)
```

It does **not** consider:

```text
Child.test(Integer)
```

because reference type is:

```java
Parent
```

`10` can be boxed to Integer and then widened to Object, so:

```java
test(Object)
```

is selected.

Runtime dispatch:

```text
Child overrides test(Object)
```

Therefore:

```text
Child Object
```

---

# 26. What If Reference Type Is Child?

Now:

```java
Child c = new Child();

c.test(10);
```

Now compiler sees:

```text
Parent.test(Object)
Parent.test(String)
Child.test(Integer)
```

The most specific applicable method is:

```java
test(Integer)
```

Therefore:

```text
Child Integer
```

This is another demonstration that **overload resolution happens using the reference's compile-time type**.

---

# 27. Overriding Rules You Should Know

For overriding:

### Same method name

```java
void run()
```

must correspond to:

```java
void run()
```

---

### Same parameter list

Parent:

```java
void run(int x)
```

Child:

```java
void run(int x)
```

Override.

But:

```java
void run(long x)
```

is not an override.

That's an overload.

---

# 28. Return Type During Overriding

Return types have a special rule.

Parent:

```java
class Animal {

    Animal getAnimal() {
        return new Animal();
    }
}
```

Child:

```java
class Dog extends Animal {

    @Override
    Dog getAnimal() {
        return new Dog();
    }
}
```

This is valid.

Why?

Because:

```text
Dog IS-A Animal
```

This is called a **covariant return type**.

But this isn't valid:

```java
class Dog extends Animal {

    @Override
    String getAnimal() {
        return "Dog";
    }
}
```

because `String` isn't a subtype of `Animal`.

---

# 29. Access Modifiers During Overriding

A child cannot reduce visibility.

Parent:

```java
class Parent {

    protected void test() {
    }
}
```

Child:

```java
class Child extends Parent {

    @Override
    public void test() {
    }
}
```

Valid.

You can go:

```text
protected → public
```

But not:

```text
protected → private
```

because that would make the method less accessible.

---

# 30. `final` Methods Cannot Be Overridden

```java
class Parent {

    final void test() {
        System.out.println("Parent");
    }
}
```

This is illegal:

```java
class Child extends Parent {

    @Override
    void test() {
        System.out.println("Child");
    }
}
```

Because `final` means:

> This implementation cannot be replaced by subclasses.

---

# 31. `private` Methods Are Not Overridden

Consider:

```java
class Parent {

    private void test() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    void test() {
        System.out.println("Child");
    }
}
```

This is **not overriding**.

Why?

Because `private` methods aren't visible to subclasses.

So Child's:

```java
test()
```

is simply a completely separate method.

---

# 32. Static Methods Are Not Overridden

This is another major interview question.

```java
class Parent {

    static void test() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    static void test() {
        System.out.println("Child");
    }
}
```

Now:

```java
Parent p = new Child();

p.test();
```

Output:

```text
Parent
```

Why?

Because static methods are associated with the **class**, not the runtime object.

This is called:

> **Method hiding**, not overriding.

The compiler resolves it based on the reference/class type.

---

# 33. Instance Method vs Static Method

Compare:

### Instance method

```java
Parent p = new Child();

p.test();
```

If `test()` is overridden:

```text
Child
```

because runtime dispatch happens.

### Static method

```java
Parent p = new Child();

p.test();
```

If `test()` is static:

```text
Parent
```

because static methods aren't dynamically dispatched.

---

# 34. Constructors Are Not Overridden

Constructors can be overloaded:

```java
class User {

    User() {
    }

    User(String name) {
    }

    User(String name, int age) {
    }
}
```

This is constructor overloading.

But constructors cannot be overridden because constructors aren't inherited.

---

# 35. One More Very Important Example

Consider:

```java
class Parent {

    void print(Object obj) {
        System.out.println("Parent Object");
    }

    void print(String str) {
        System.out.println("Parent String");
    }
}
```

```java
class Child extends Parent {

    @Override
    void print(Object obj) {
        System.out.println("Child Object");
    }
}
```

Now:

```java
Child c = new Child();

c.print("hello");
```

Compiler sees:

```text
print(Object)
print(String)
```

because Child inherits Parent's `print(String)`.

It chooses:

```text
print(String)
```

Child doesn't override it.

Therefore:

```text
Parent String
```

Notice something important:

**Inherited methods participate in overload resolution too.**

---

# 36. The Ultimate Mental Model

Whenever you see:

```java
reference.method(argument);
```

ask these questions in order.

### Question 1

What is the **compile-time type of the reference**?

Example:

```java
Parent p = new Child();
```

Answer:

```text
Parent
```

---

### Question 2

What methods are available from that compile-time type?

For example:

```text
Parent:
    process(Object)
    process(String)
```

---

### Question 3

Which overloaded signature best matches the argument?

If:

```java
p.process("hello");
```

then:

```text
String
```

matches:

```text
process(String)
```

So compiler selects:

```text
process(String)
```

---

### Question 4

Is that method overridden in the actual object?

Actual object:

```java
new Child()
```

If Child has:

```java
@Override
void process(String str)
```

then Child implementation executes.

If not, Parent implementation executes.

---

# 37. The Golden Formula

You can memorize this:

```text
             METHOD CALL
                  |
                  ↓
       ┌─────────────────────┐
       │ Compile Time        │
       │                     │
       │ Find overload       │
       │ using reference     │
       │ type + argument     │
       └──────────┬──────────┘
                  ↓
             Method Signature
                  |
                  ↓
       ┌─────────────────────┐
       │ Runtime             │
       │                     │
       │ Find overridden     │
       │ implementation      │
       │ using object type   │
       └──────────┬──────────┘
                  ↓
              EXECUTION
```

Or, even shorter:

> **Overloading → Which signature? → Compile time**
> **Overriding → Which implementation? → Runtime**

---

# 38. The Trick You Should Remember for Interviews

If you see:

```java
Parent p = new Child();

p.method(...);
```

**Don't immediately look at `Child`.**

First look at `Parent`.

Ask:

```text
1. What methods does Parent expose?
2. Which overload matches the arguments?
3. Now, is that exact method overridden by Child?
4. If yes → Child implementation.
5. If no → Parent implementation.
```

The phrase **"exact method"** is critical.

If Parent has:

```java
method(Object)
```

and Child has:

```java
method(String)
```

then Child did **not** override Parent's method.

They are two different methods.

---

# 39. Final Comparison with Your Two Examples

### Example A

```java
Parent p = new Child();

p.print("hello");
```

Methods:

```text
Parent:
    print(Object)

Child:
    print(String)
```

Compile time:

```text
print(String) is NOT visible through Parent
```

Therefore:

```text
print(Object)
```

Runtime:

```text
Child does not override print(Object)
```

Result:

```text
Parent Object
```

---

### Example B

```java
Parent p = new Child();

p.process("hello");
```

Methods:

```text
Parent:
    process(Object)
    process(String)

Child:
    process(Object)
```

Compile time:

```text
process(String)
```

Runtime:

```text
Child does NOT override process(String)
```

Result:

```text
Parent String
```

---

### Example C

If Child has:

```java
@Override
void process(String str) {
    System.out.println("Child String");
}
```

Then:

```java
Parent p = new Child();

p.process("hello");
```

Compile time:

```text
process(String)
```

Runtime:

```text
Child.process(String)
```

Result:

```text
Child String
```

That **compile-time overload selection → runtime override selection** sequence is one of the most important Java concepts to internalize before moving deeper into polymorphism.
