# 3. Inheritance in Java — Deep Dive

Inheritance is where OOP starts becoming significantly more interesting because it introduces **relationships between classes** and leads directly into **polymorphism**.

The core idea is simple:

> **Inheritance allows one class to acquire and extend the state and behavior of another class.**

But mastering inheritance means understanding **what Java actually inherits, what it doesn't, constructor chaining, overriding, `super`, `protected`, upcasting/downcasting, and when inheritance is actually a bad design choice.**

---

# 1. The Basic Idea

Suppose we have:

```java
class Vehicle {

    String brand;

    void start() {
        System.out.println("Vehicle started");
    }
}
```

Now:

```java
class ElectricVehicle extends Vehicle {

    double batteryCapacity;
}
```

We have:

```text
Vehicle
   ↑
   │ extends
   │
ElectricVehicle
```

`ElectricVehicle` is a specialized form of `Vehicle`.

This represents:

> **IS-A relationship**

An electric vehicle **is a** vehicle.

---

# 2. Why Does Inheritance Exist?

Suppose we didn't use inheritance.

We might write:

```java
class ElectricVehicle {

    String brand;

    void start() {
        System.out.println("Vehicle started");
    }

    double batteryCapacity;
}
```

and:

```java
class PetrolVehicle {

    String brand;

    void start() {
        System.out.println("Vehicle started");
    }

    double fuelCapacity;
}
```

We've duplicated:

```text
brand
start()
```

Inheritance lets us put common behavior in one place:

```text
             Vehicle
          /           \
         /             \
ElectricVehicle    PetrolVehicle
```

Shared behavior:

```text
Vehicle
 ├── brand
 └── start()
```

Specialized behavior:

```text
ElectricVehicle
 └── batteryCapacity

PetrolVehicle
 └── fuelCapacity
```

---

# 3. Basic Syntax

```java
class Vehicle {

    String brand;

    void start() {
        System.out.println("Vehicle started");
    }
}
```

Child:

```java
class ElectricVehicle extends Vehicle {

    double batteryCapacity;
}
```

Now:

```java
ElectricVehicle ev = new ElectricVehicle();

ev.brand = "Mercedes";
ev.batteryCapacity = 90;

ev.start();
```

Even though `brand` and `start()` are declared in `Vehicle`, the `ElectricVehicle` object can use them.

---

# 4. Think of Inheritance as Extension

The child doesn't replace the parent.

It **extends** it.

Conceptually:

```text
ElectricVehicle
│
├── inherited from Vehicle
│   ├── brand
│   └── start()
│
└── its own
    └── batteryCapacity
```

So:

```java
class ElectricVehicle extends Vehicle
```

means:

> ElectricVehicle is a Vehicle with additional characteristics/behavior.

---

# 5. IS-A Relationship

This is one of the most important rules.

Ask:

> "Can I truthfully say X is a Y?"

If yes, inheritance may make sense.

```text
ElectricVehicle IS-A Vehicle
Car            IS-A Vehicle
Dog            IS-A Animal
Manager        IS-A Employee
```

But:

```text
Car IS-A Engine ❌
Car HAS-A Engine ✅
```

So:

```java
class Car extends Engine
```

is conceptually wrong.

Instead:

```java
class Car {

    private Engine engine;
}
```

This is **composition**, which we'll study after polymorphism.

---

# 6. What Does `extends` Mean?

When you write:

```java
class ElectricVehicle extends Vehicle
```

Java establishes an inheritance relationship.

The child gets access to accessible members of the parent.

For example:

```java
class Vehicle {

    protected String brand;

    public void start() {
        System.out.println("Starting");
    }
}
```

Then:

```java
class ElectricVehicle extends Vehicle {

    void printBrand() {
        System.out.println(brand);
    }
}
```

works.

---

# 7. `private` Members and Inheritance

This is a subtle but important point.

Suppose:

```java
class Vehicle {

    private String brand;
}
```

and:

```java
class ElectricVehicle extends Vehicle {

    void printBrand() {
        System.out.println(brand); // ❌
    }
}
```

The child cannot directly access the parent's private field.

Why?

Because `private` means:

> Accessible only inside the class where it is declared.

So:

```text
Vehicle
 └── private brand
       ↑
       │
       └── accessible directly only inside Vehicle
```

This is one reason encapsulation and inheritance need to be understood together.

---

# 8. Use Methods Instead

Instead of exposing the field:

```java
class Vehicle {

    private String brand;

    public String getBrand() {
        return brand;
    }
}
```

The child can do:

```java
class ElectricVehicle extends Vehicle {

    void printBrand() {
        System.out.println(getBrand());
    }
}
```

This preserves encapsulation.

---

# 9. `protected`

Java provides `protected` specifically for cases where subclasses need access.

```java
class Vehicle {

    protected String brand;
}
```

Then:

```java
class ElectricVehicle extends Vehicle {

    void printBrand() {
        System.out.println(brand);
    }
}
```

works.

But don't automatically make everything `protected`.

Often:

```java
private
```

with:

```java
protected/public method
```

is a better design because the parent retains control over its state.

---

# 10. Constructors Are NOT Inherited

This is a very common interview question.

Suppose:

```java
class Vehicle {

    Vehicle(String brand) {
        this.brand = brand;
    }
}
```

Then:

```java
class ElectricVehicle extends Vehicle {
}
```

You cannot do:

```java
new ElectricVehicle("Mercedes");
```

just because the parent has that constructor.

Constructors are **not inherited**.

The child must define its own constructor.

---

# 11. Constructor Chaining

Here's where it gets interesting.

```java
class Vehicle {

    String brand;

    Vehicle(String brand) {
        this.brand = brand;
    }
}
```

Child:

```java
class ElectricVehicle extends Vehicle {

    double batteryCapacity;

    ElectricVehicle(
            String brand,
            double batteryCapacity) {

        super(brand);
        this.batteryCapacity = batteryCapacity;
    }
}
```

Notice:

```java
super(brand);
```

This calls the parent constructor.

---

# 12. What Does `super()` Mean?

`super` refers to the parent portion of the current object.

There are two major uses:

### Calling parent constructor

```java
super(...);
```

### Accessing parent implementation

```java
super.start();
```

We'll see both.

---

# 13. Constructor Execution Order

This is very important.

Consider:

```java
class Vehicle {

    Vehicle() {
        System.out.println("Vehicle constructor");
    }
}
```

```java
class ElectricVehicle extends Vehicle {

    ElectricVehicle() {
        System.out.println("EV constructor");
    }
}
```

Now:

```java
new ElectricVehicle();
```

Output:

```text
Vehicle constructor
EV constructor
```

Why?

Because the parent part must be initialized before the child part.

Conceptually:

```text
new ElectricVehicle()
        ↓
Vehicle constructor
        ↓
ElectricVehicle constructor
```

---

# 14. `super()` Is Implicit

If you don't explicitly write:

```java
super();
```

Java tries to insert it as the first statement of the child constructor.

Example:

```java
class ElectricVehicle extends Vehicle {

    ElectricVehicle() {
        System.out.println("EV");
    }
}
```

is conceptually:

```java
class ElectricVehicle extends Vehicle {

    ElectricVehicle() {
        super();
        System.out.println("EV");
    }
}
```

But there's a catch.

If the parent doesn't have a no-argument constructor:

```java
class Vehicle {

    Vehicle(String brand) {
    }
}
```

then this:

```java
class ElectricVehicle extends Vehicle {

    ElectricVehicle() {
    }
}
```

will fail.

You need:

```java
ElectricVehicle() {
    super("Mercedes");
}
```

---

# 15. Constructor Chain Visualization

Consider:

```java
class Vehicle {

    Vehicle(String brand) {
        System.out.println("Vehicle");
    }
}
```

```java
class ElectricVehicle extends Vehicle {

    ElectricVehicle(String brand) {
        super(brand);
        System.out.println("EV");
    }
}
```

Creating:

```java
new ElectricVehicle("Mercedes");
```

results in:

```text
             new ElectricVehicle()
                      │
                      ↓
              ElectricVehicle()
                      │
                      │ super()
                      ↓
                 Vehicle()
                      │
                      ↓
              Parent initialized
                      │
                      ↓
              Child initialized
```

---

# 16. Method Overriding

Inheritance becomes much more powerful when the child changes inherited behavior.

Parent:

```java
class Vehicle {

    void start() {
        System.out.println("Vehicle starting");
    }
}
```

Child:

```java
class ElectricVehicle extends Vehicle {

    @Override
    void start() {
        System.out.println("Battery system starting");
    }
}
```

Now:

```java
ElectricVehicle ev = new ElectricVehicle();

ev.start();
```

prints:

```text
Battery system starting
```

The child has **overridden** the parent's method.

---

# 17. Overloading vs Overriding

This distinction is extremely important.

### Overloading

Same class/inheritance context, same method name, different parameter list.

```java
add(int, int)
add(double, double)
```

Resolved at compile time.

### Overriding

Child provides a new implementation of a parent method.

```java
Vehicle.start()
ElectricVehicle.start()
```

Resolved dynamically at runtime.

We'll dive into the runtime mechanism in the next topic: **Polymorphism**.

---

# 18. `@Override`

Always prefer:

```java
@Override
void start() {
}
```

instead of simply:

```java
void start() {
}
```

Why?

The annotation tells the compiler:

> "I intend to override a parent method."

If you accidentally make a mistake:

```java
@Override
void strt() {
}
```

the compiler catches it.

Without `@Override`, you might accidentally create a completely new method.

---

# 19. Calling the Parent Implementation

Sometimes you override a method but still want the parent behavior.

```java
class Vehicle {

    void start() {
        System.out.println("Vehicle systems starting");
    }
}
```

```java
class ElectricVehicle extends Vehicle {

    @Override
    void start() {

        super.start();

        System.out.println(
                "Battery management system starting"
        );
    }
}
```

Output:

```text
Vehicle systems starting
Battery management system starting
```

So:

```java
super.start();
```

means:

> Execute the parent's implementation of `start()`.

---

# 20. Can You Override a `private` Method?

No.

Example:

```java
class Parent {

    private void test() {
    }
}
```

```java
class Child extends Parent {

    void test() {
    }
}
```

This is **not overriding**.

Why?

Because the child cannot see the parent's private method.

It is simply a new method in the child.

---

# 21. Can You Override a `final` Method?

No.

```java
class Vehicle {

    final void start() {
    }
}
```

Then:

```java
class ElectricVehicle extends Vehicle {

    @Override
    void start() {  // ❌
    }
}
```

The compiler rejects it.

`final` means:

> This implementation cannot be overridden.

---

# 22. Can You Override a Static Method?

This is a common interview trap.

Static methods are **hidden**, not overridden.

## Why You Can't Override Static Methods

Static methods cannot be overridden because they belong to the class itself, not to instances of the class. When you declare a method as `static`, it's bound to the class at compile-time, not to objects at runtime.

**Key reasons:**

- **Static methods are class-level**: They're associated with the class, not with objects. There's no dynamic dispatch for static methods.
- **Compile-time binding**: The JVM determines which static method to call at compile-time based on the reference type, not the actual object type.
- **No polymorphism**: Static methods don't participate in polymorphism, which is the core mechanism behind method overriding.

## What Happens When You Try to "Override" a Static Method

You don't actually override it—you **hide** it. This is called **method hiding**.

Parent:

```java
class Parent {

    static void hello() {
        System.out.println("Parent");
    }
}
```

Child:

```java
class Child extends Parent {

    static void hello() {
        System.out.println("Child");
    }
}
```

Usage:

```java
Parent p = new Child();
p.hello();  // Output: "Parent" (based on reference type)

Child c = new Child();
c.hello();  // Output: "Child"
```

## Key Differences: Overriding vs Hiding

**Overriding** (instance methods): Runtime polymorphism - the actual object's method is called

**Hiding** (static methods): Compile-time binding - the reference type's method is called

## Best Practice

If you need polymorphic behavior, use instance methods. If you truly need class-level behavior, use static methods but don't try to override them in subclasses.

---

# 23. Upcasting

Suppose:

```java
class Vehicle {
}

class ElectricVehicle extends Vehicle {
}
```

You can do:

```java
Vehicle vehicle = new ElectricVehicle();
```

This is **upcasting**.

The object is still:

```text
ElectricVehicle
```

but the reference type is:

```text
Vehicle
```

Conceptually:

```text
Vehicle reference
       │
       ↓
ElectricVehicle object
```

This is safe because:

> Every ElectricVehicle is a Vehicle.

---

# 24. Why Upcasting Is Important

Upcasting is one of the foundations of polymorphism.

Suppose:

```java
class Vehicle {

    void start() {
        System.out.println("Vehicle");
    }
}
```

```java
class ElectricVehicle extends Vehicle {

    @Override
    void start() {
        System.out.println("Electric");
    }
}
```

Then:

```java
Vehicle vehicle =
        new ElectricVehicle();

vehicle.start();
```

prints:

```text
Electric
```

Even though the reference type is `Vehicle`.

**Why?**

Because the actual object is `ElectricVehicle`.

This is runtime polymorphism.

We'll dissect exactly why in the next topic.

---

# 25. What Can You Access After Upcasting?

Suppose:

```java
class Vehicle {

    void start() {
    }
}
```

```java
class ElectricVehicle extends Vehicle {

    void charge() {
    }
}
```

Then:

```java
Vehicle vehicle =
        new ElectricVehicle();
```

You can:

```java
vehicle.start(); // ✅
```

But:

```java
vehicle.charge(); // ❌
```

Why?

Because the **reference type** determines what members are visible to the compiler.

```text
Reference type:
Vehicle

Visible:
Vehicle members
```

But when an overridden instance method is called, runtime dispatch determines which implementation executes.

This distinction is fundamental:

```text
Compile time
    ↓
Reference type

Runtime
    ↓
Actual object type
```

---

# 26. Downcasting

You can explicitly cast back:

```java
Vehicle vehicle =
        new ElectricVehicle();

ElectricVehicle ev =
        (ElectricVehicle) vehicle;
```

Now:

```java
ev.charge();
```

works.

This is downcasting.

But it can be dangerous.

---

# 27. ClassCastException

Consider:

```java
class Vehicle {
}

class ElectricVehicle extends Vehicle {
}

class PetrolVehicle extends Vehicle {
}
```

Then:

```java
Vehicle vehicle =
        new PetrolVehicle();
```

This is invalid:

```java
ElectricVehicle ev =
        (ElectricVehicle) vehicle;
```

because the actual object is a `PetrolVehicle`.

At runtime:

```text
ClassCastException
```

---

# 28. `instanceof`

You can check:

```java
if (vehicle instanceof ElectricVehicle) {

    ElectricVehicle ev =
            (ElectricVehicle) vehicle;

    ev.charge();
}
```

Modern Java also supports pattern matching:

```java
if (vehicle instanceof ElectricVehicle ev) {
    ev.charge();
}
```

Much cleaner.

But don't use downcasting everywhere.

Frequent downcasting is often a sign that your design could be improved using polymorphism.

---

# 29. Inheritance Tree

We can build:

```text
                 Vehicle
                /       \
               /         \
      ElectricVehicle   PetrolVehicle
             |
             |
        LuxuryEV
```

Technically Java supports:

```text
single class inheritance
```

A class can extend only one class.

This is valid:

```java
class ElectricVehicle extends Vehicle
```

But this isn't:

```java
class ElectricVehicle
        extends Vehicle, Machine { // ❌
}
```

Java does not support multiple inheritance of classes.

---

# 30. Why Doesn't Java Support Multiple Class Inheritance?

One major problem is ambiguity.

Imagine:

```text
        A
       / \
      B   C
       \ /
        D
```

Suppose both `B` and `C` define:

```java
void execute()
```

What should `D.execute()` do?

```text
B.execute() ?
C.execute() ?
```

This is the classic **diamond problem**.

Java avoids this complexity for class inheritance.

But Java allows multiple **interfaces**.

We'll study exactly how that works when we reach interfaces.

---

# 31. `final` Class

You can prevent inheritance entirely:

```java
final class Vehicle {
}
```

Then:

```java
class ElectricVehicle extends Vehicle {
}
```

is illegal.

Examples from Java include classes designed to be non-inheritable.

This can be useful when you want to guarantee that behavior cannot be customized through inheritance.

---

# 32. Inheritance and Encapsulation

Notice the tension:

```text
Inheritance
    ↓
Reuse/extension

Encapsulation
    ↓
Hide implementation
```

If you expose too much internal state for subclasses:

```java
protected double balance;
```

you weaken encapsulation.

If you make everything private:

```java
private double balance;
```

subclasses can't directly manipulate it.

Often the best approach is:

```java
private state
    +
protected/public behavior
```

rather than:

```java
protected state
```

---

# 33. The Big Design Question

Just because you **can** use inheritance doesn't mean you **should**.

Suppose:

```text
Vehicle
├── ElectricVehicle
├── PetrolVehicle
├── HybridVehicle
├── HydrogenVehicle
├── AutonomousElectricVehicle
├── AutonomousPetrolVehicle
└── ...
```

As requirements grow, the hierarchy can become complicated.

Imagine adding:

```text
Luxury
Commercial
Autonomous
Connected
FourWheel
TwoWheel
```

You can quickly end up with a giant hierarchy.

That's one reason we eventually prefer:

> **Composition over inheritance**

But before we get there, we need to understand polymorphism.

---

# 34. The Fragile Base Class Problem

Another important inheritance problem.

Suppose:

```java
class Parent {

    void process() {
        ...
    }
}
```

Many subclasses depend on its behavior.

Later you change:

```java
process()
```

in the parent.

Suddenly multiple subclasses can behave differently or break.

The child is tightly coupled to the parent's implementation.

This is called the **fragile base class problem**.

It's one reason inheritance should be used deliberately.

---

# 35. Real Backend Example

Imagine:

```java
abstract class Notification {
    protected String recipient;

    public Notification(String recipient) {
        this.recipient = recipient;
    }

    public void validate() {
        ...
    }

    abstract void send();
}
```

Then:

```java
class EmailNotification extends Notification {

    @Override
    void send() {
        ...
    }
}
```

and:

```java
class SmsNotification extends Notification {

    @Override
    void send() {
        ...
    }
}
```

Here inheritance can make sense because:

```text
EmailNotification IS-A Notification
SmsNotification   IS-A Notification
```

And it sets us up perfectly for:

> **Abstract Classes**

which is one of the concepts in your original list.

---

# 36. Inheritance vs Composition Preview

Compare:

### Inheritance

```java
class ElectricVehicle extends Vehicle {
}
```

means:

```text
ElectricVehicle IS-A Vehicle
```

### Composition

```java
class ElectricVehicle {

    private Battery battery;
}
```

means:

```text
ElectricVehicle HAS-A Battery
```

Visualize:

```text
Inheritance:

ElectricVehicle
      │
      IS-A
      ↓
   Vehicle


Composition:

ElectricVehicle
      │
     HAS-A
      ↓
   Battery
```

This distinction will become extremely important when we study **Composition**.

---

# 37. Coding Exercise

Let's build on our previous EV domain.

Create:

```java
class Vehicle
```

with:

```text
registrationNumber
brand
model
```

and:

```java
void start()
void stop()
```

Then create:

```java
class ElectricVehicle extends Vehicle
```

with:

```text
batteryCapacity
currentBattery
```

and:

```java
void charge(double amount)
void drive(double kilometers)
```

Then create:

```java
class PetrolVehicle extends Vehicle
```

with:

```text
fuelCapacity
currentFuel
```

and:

```java
void refuel(double amount)
void drive(double kilometers)
```

Now you'll have:

```text
                    Vehicle
                   /       \
                  /         \
                 ↓           ↓
       ElectricVehicle   PetrolVehicle
```

---

# 38. Add Constructor Chaining

`Vehicle`:

```java
Vehicle(
    String registrationNumber,
    String brand,
    String model
)
```

`ElectricVehicle`:

```java
ElectricVehicle(
    String registrationNumber,
    String brand,
    String model,
    double batteryCapacity
)
```

Use:

```java
super(
    registrationNumber,
    brand,
    model
);
```

The child should not duplicate initialization of parent fields.

---

# 39. Override `start()`

Parent:

```java
void start() {
    System.out.println("Vehicle starting");
}
```

EV:

```java
@Override
void start() {
    System.out.println(
        "Electric motor starting silently"
    );
}
```

Petrol:

```java
@Override
void start() {
    System.out.println(
        "Petrol engine starting"
    );
}
```

Then test:

```java
Vehicle v1 =
        new ElectricVehicle(...);

Vehicle v2 =
        new PetrolVehicle(...);

v1.start();
v2.start();
```

Don't worry if the behavior here feels mysterious.

**That is exactly what we're going to unpack next.**

---

# 40. Your Key Questions to Answer

Before moving to polymorphism, make sure you can explain these:

### 1.

Why does this work?

```java
Vehicle v = new ElectricVehicle();
```

### 2.

Why doesn't this work?

```java
Vehicle v = new Vehicle();
v.charge();
```

### 3.

Why does this:

```java
Vehicle v = new ElectricVehicle();
v.start();
```

execute `ElectricVehicle.start()`?

### 4.

Why are constructors not inherited?

### 5.

What's the difference between:

```java
super()
```

and:

```java
this()
```

### 6.

What's the difference between:

```java
super.start();
```

and:

```java
this.start();
```

### 7.

Why is this dangerous?

```java
ElectricVehicle ev =
    (ElectricVehicle) vehicle;
```

### 8.

Why does Java allow multiple interfaces but only one parent class?

---

# 41. The Mental Model

Keep this picture:

```text
                         Vehicle
                            │
                 ┌──────────┴──────────┐
                 ↓                     ↓
         ElectricVehicle        PetrolVehicle
                 │                     │
          batteryCapacity        fuelCapacity
          currentBattery         currentFuel
                 │                     │
              charge()              refuel()
```

Inheritance gives you:

```text
                 IS-A
                  │
                  ↓
        shared abstraction
                  │
                  ↓
          specialized behavior
```

But inheritance alone isn't the ultimate goal.

The real payoff comes next:

```text
Inheritance
     ↓
Method overriding
     ↓
Upcasting
     ↓
Polymorphism
     ↓
Runtime method dispatch
```

---

# Next: Polymorphism

This is probably the **most important OOP concept for Java/Spring interviews**.

We'll go deep into:

```text
Compile-time polymorphism
Runtime polymorphism
Method overloading
Method overriding
Upcasting
Dynamic method dispatch
Reference type vs object type
Virtual method invocation
Casting
instanceof
Interface polymorphism
Polymorphism + dependency injection
```

And we'll answer the classic interview question:

> **"When I write `Vehicle v = new ElectricVehicle(); v.start();`, how does Java know at runtime to execute `ElectricVehicle.start()`?"**

That's where the JVM-level mental model becomes important.
