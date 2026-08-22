# 2. Encapsulation in Java — Deep Dive

Encapsulation is one of the **four core OOP principles**, and it's much more than:

> "Make variables `private` and generate getters/setters."

That is the beginner definition.

For real mastery, think of encapsulation as:

> **An object controls access to its internal state and protects its invariants by exposing only the operations that make sense.**

This becomes extremely important in backend development because your domain objects should prevent invalid state.

---

# 1. The Problem Encapsulation Solves

Consider this:

```java
class BankAccount {
    public double balance;
}
```

Now anyone can do:

```java
BankAccount account = new BankAccount();

account.balance = 100000;
account.balance = -50000;
account.balance = -999999999;
```

The class has **no control** over its own state.

We can put it into an invalid state.

That's the fundamental problem encapsulation solves.

---

# 2. The Basic Idea

Instead of:

```java
class BankAccount {

    public double balance;
}
```

we hide the state:

```java
class BankAccount {

    private double balance;
}
```

Now external code cannot directly modify it:

```java
account.balance = -1000; // ❌
```

The class itself decides how its state can change.

```java
class BankAccount {

    private double balance;

    public void deposit(double amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        balance += amount;
    }
}
```

Now:

```java
account.deposit(1000);
```

is allowed.

But:

```java
account.deposit(-500);
```

is rejected.

This is encapsulation.

---

# 3. Encapsulation Has Two Important Parts

Think about it as:

```text
             ENCAPSULATION
                  │
        ┌─────────┴─────────┐
        ↓                   ↓
  Hide internal state    Control behavior
        │                   │
      private          methods validate
        │                   │
        └─────────┬─────────┘
                  ↓
          protect invariants
```

So:

### Part 1 — Information hiding

Don't expose implementation details unnecessarily.

### Part 2 — Controlled access

Expose meaningful operations instead of unrestricted mutation.

---

# 4. What Is an Invariant?

This is a very important concept for interviews and design.

An **invariant** is a condition that should always remain true for a valid object.

For example:

```text
BankAccount balance >= 0
```

or:

```text
ElectricVehicle currentBattery >= 0
currentBattery <= batteryCapacity
```

or:

```text
Order total >= 0
```

or:

```text
Employee salary > 0
```

A well-encapsulated object protects these invariants.

---

# 5. Bad Design

Suppose:

```java
class ElectricVehicle {

    public double batteryCapacity;
    public double currentBattery;
}
```

External code can do:

```java
vehicle.currentBattery = -500;
```

or:

```java
vehicle.currentBattery = 999999;
```

The object has no control.

---

# 6. Better Design

```java
class ElectricVehicle {

    private final double batteryCapacity;
    private double currentBattery;

    public ElectricVehicle(double batteryCapacity) {

        if (batteryCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Battery capacity must be positive"
            );
        }

        this.batteryCapacity = batteryCapacity;
        this.currentBattery = batteryCapacity;
    }

    public void charge(double amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Charge amount must be positive"
            );
        }

        currentBattery =
                Math.min(
                        currentBattery + amount,
                        batteryCapacity
                );
    }

    public void discharge(double amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Discharge amount must be positive"
            );
        }

        if (amount > currentBattery) {
            throw new IllegalStateException(
                    "Insufficient battery"
            );
        }

        currentBattery -= amount;
    }

    public double getCurrentBattery() {
        return currentBattery;
    }
}
```

Now:

```java
vehicle.discharge(20);
```

is meaningful.

And:

```java
vehicle.currentBattery = -500;
```

is impossible from outside the class.

---

# 7. Why Getters and Setters Are Not Automatically Encapsulation

This is an important interview distinction.

Many developers write:

```java
class Employee {

    private String name;
    private double salary;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }
}
```

They say:

> "This is encapsulation because the fields are private."

Technically, you've hidden the fields, but you've potentially **not protected the object's state**.

For example:

```java
employee.setSalary(-500000);
```

If the setter accepts anything, the object can still become invalid.

Better:

```java
public void setSalary(double salary) {

    if (salary <= 0) {
        throw new IllegalArgumentException(
                "Salary must be positive"
        );
    }

    this.salary = salary;
}
```

Even better, sometimes you shouldn't expose a setter at all.

---

# 8. Setter vs Behavior

Consider a bank account.

Bad:

```java
account.setBalance(account.getBalance() + 500);
```

This exposes too much of the internal model.

Better:

```java
account.deposit(500);
```

Why?

Because `deposit()` represents a **business operation**.

The class can internally enforce:

```text
amount > 0
account active
daily limit
fraud checks
transaction rules
etc.
```

The caller doesn't need to know how.

This is much stronger encapsulation.

---

# 9. Tell, Don't Ask

This leads to an important object-oriented design principle:

> **Tell an object what to do instead of asking for its internal data and manipulating it yourself.**

Bad:

```java
if (account.getBalance() >= amount) {
    account.setBalance(
        account.getBalance() - amount
    );
}
```

The caller is controlling the account's internal state.

Better:

```java
account.withdraw(amount);
```

The account itself decides whether the operation is valid.

---

# 10. Encapsulation Example: Order

Suppose:

```java
class Order {

    private double total;
    private String status;
}
```

Bad API:

```java
order.setTotal(1000);
order.setStatus("PAID");
```

Anyone can potentially do:

```java
order.setStatus("PAID");
```

without actually paying.

Instead:

```java
class Order {

    private double total;
    private OrderStatus status;

    public void pay() {

        if (total <= 0) {
            throw new IllegalStateException(
                    "Cannot pay an empty order"
            );
        }

        status = OrderStatus.PAID;
    }
}
```

Now the state transition is controlled.

This becomes extremely useful when we study **Enums and State Machines** later.

---

# 11. Encapsulation and Access Modifiers

Java gives us access modifiers:

| Modifier    | Accessible from           |
| ----------- | ------------------------- |
| `private`   | Same class                |
| default     | Same package              |
| `protected` | Same package + subclasses |
| `public`    | Everywhere                |

For encapsulation, the most important one is:

```java
private
```

because it allows the class to hide implementation details.

---

# 12. But `private` Isn't the Whole Story

Suppose:

```java
class Employee {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

The field is private.

But the class exposes unrestricted mutation.

So a better definition is:

> **Encapsulation is controlling how an object's internal state is accessed and modified, not merely making fields private.**

---

# 13. Read-Only Objects

Sometimes you want an object whose state can be read but not modified externally.

For example:

```java
class Employee {

    private final String employeeId;
    private String name;

    public String getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }
}
```

There is no setter for `employeeId`.

Therefore:

```java
employee.getEmployeeId();
```

works.

But:

```java
employee.setEmployeeId(...);
```

doesn't exist.

This is a useful technique for protecting state.

---

# 14. Encapsulation and `final`

Consider:

```java
class Employee {

    private final String employeeId;

    Employee(String employeeId) {
        this.employeeId = employeeId;
    }
}
```

Now the ID cannot be reassigned after construction.

This gives us:

```text
private
  +
final
  +
no setter
```

which provides strong protection for that field.

But remember:

**`final` doesn't mean immutable.**

We'll study that separately.

---

# 15. Encapsulation and Collections

Here's a very common trap.

Suppose:

```java
class Team {

    private List<String> members = new ArrayList<>();

    public List<String> getMembers() {
        return members;
    }
}
```

Looks encapsulated.

But:

```java
team.getMembers().clear();
```

works.

Why?

Because the getter exposes the actual internal list.

The caller now has a reference to the internal mutable object.

---

# 16. Defensive Copying

One solution:

```java
public List<String> getMembers() {
    return new ArrayList<>(members);
}
```

Now the caller receives a copy.

```text
Internal list
     │
     │ copy
     ↓
External list
```

Changes to the external list don't affect the internal list.

Another option:

```java
public List<String> getMembers() {
    return Collections.unmodifiableList(members);
}
```

Now callers can read the list but cannot modify it through that reference.

Modern Java also provides:

```java
return List.copyOf(members);
```

We'll revisit this when we study **immutability**.

---

# 17. Encapsulation and Composition

Suppose:

```java
class Car {

    private Engine engine;

}
```

The engine is internal to the car.

The caller shouldn't necessarily manipulate:

```java
car.getEngine().setFuelInjectionSomething(...);
```

Instead:

```java
car.start();
```

The car controls how the engine is used.

This is where:

> **Encapsulation + Composition**

become powerful together.

We'll study composition separately.

---

# 18. Encapsulation vs Abstraction

These two are frequently confused.

### Encapsulation

Focuses on:

> **How do I protect internal state and implementation?**

Example:

```java
private double balance;
```

### Abstraction

Focuses on:

> **What should the caller need to know?**

Example:

```java
interface PaymentProcessor {
    void pay(double amount);
}
```

The caller doesn't care how payment happens.

Think:

```text
Encapsulation
    ↓
Hide/protect internals

Abstraction
    ↓
Expose essential concept
```

They work together, but they're not the same thing.

---

# 19. Encapsulation vs Data Hiding

These are related but slightly different.

### Data hiding

Prevent direct access to internal data.

```java
private double balance;
```

### Encapsulation

Bundle state + behavior and control how they interact.

```java
class BankAccount {

    private double balance;

    public void deposit(double amount) {
        ...
    }

    public void withdraw(double amount) {
        ...
    }
}
```

So encapsulation is broader.

---

# 20. Real Spring Boot Example

Imagine:

```java
@Entity
public class Order {

    @Id
    private Long id;

    private BigDecimal total;

    private OrderStatus status;
}
```

A beginner might generate:

```java
getId()
setId()

getTotal()
setTotal()

getStatus()
setStatus()
```

for everything.

But think about business rules.

Should anyone be able to do:

```java
order.setStatus(PAID);
```

Probably not.

Instead:

```java
public void markAsPaid() {

    if (status != OrderStatus.PENDING) {
        throw new IllegalStateException(
            "Only pending orders can be paid"
        );
    }

    status = OrderStatus.PAID;
}
```

Now the domain object protects its lifecycle.

This becomes extremely useful when designing real applications.

---

# 21. Encapsulation in Your Future Project

We'll eventually have objects like:

```text
User
Vehicle
ChargingStation
Connector
ChargingSession
Payment
Invoice
Contract
Subscription
Notification
```

For example:

```java
class ChargingSession {

    private final String sessionId;
    private final String vehicleId;

    private SessionStatus status;
    private double energyConsumed;

    public void start() {
        ...
    }

    public void stop() {
        ...
    }

    public void recordEnergy(double energy) {
        ...
    }
}
```

Outside code should not arbitrarily manipulate:

```java
session.status = COMPLETED;
```

Instead:

```java
session.stop();
```

The object controls the state transition.

That is proper domain modeling.

---

# 22. A Very Important Rule

When designing a class, ask:

> **What state must always be valid?**

Then ask:

> **Who should be allowed to change that state?**

Then:

> **What operation should represent that change?**

For example:

```text
Invariant:
battery >= 0

Who changes it?
ElectricVehicle

Operation:
drive()

Not:
setBattery()
```

This is a much more mature way of thinking about encapsulation.

---

# 23. Bad vs Good Design

### Bad

```java
class BankAccount {

    public double balance;
}
```

Usage:

```java
account.balance -= 1000;
```

Problem:

```text
No validation
No control
No invariant protection
```

### Better

```java
class BankAccount {

    private double balance;

    public void withdraw(double amount) {

        if (amount > balance) {
            throw new IllegalArgumentException();
        }

        balance -= amount;
    }
}
```

Usage:

```java
account.withdraw(1000);
```

Now:

```text
Caller
   │
   │ withdraw()
   ↓
BankAccount
   │
   ├── validate
   ├── enforce rules
   └── change state
```

---

# 24. Common Interview Question

### "Is encapsulation achieved by making variables private?"

**Answer:**

Not necessarily.

`private` provides access restriction, but proper encapsulation means that the class controls access to its internal state and exposes a meaningful public API that preserves its invariants.

For example:

```java
private double balance;
```

with:

```java
deposit()
withdraw()
```

is generally stronger encapsulation than:

```java
private double balance;

getBalance()
setBalance()
```

because the latter may expose unrestricted state mutation.

---

# 25. Another Interview Question

### Why is this bad?

```java
account.setBalance(
    account.getBalance() - amount
);
```

Because it exposes internal state and moves the responsibility for maintaining the account's invariants outside the object.

Prefer:

```java
account.withdraw(amount);
```

The object owns the operation that changes its state.

---

# 26. Another Important Question

### Does encapsulation make a class immutable?

No.

These are different concepts.

An encapsulated object can still be mutable:

```java
class BankAccount {

    private double balance;

    public void deposit(double amount) {
        balance += amount;
    }
}
```

`balance` is protected from direct external access, but it can still change.

So:

```text
Encapsulation ≠ Immutability
```

We'll study immutability later.

---