# 1️⃣ The Two Most Important Relationships in OOP

In object-oriented design, relationships between classes are mainly:

1. **IS-A** → Inheritance
2. **HAS-A** → Composition / Aggregation

These define how your system is structured.

---

# 🟢 2️⃣ IS-A Relationship (Inheritance)

## 🔎 Meaning

> One class is a type of another class.

Example:

```
Dog IS-A Animal
Car IS-A Vehicle
SavingsAccount IS-A Account
```

In Java:

```java
class Animal {
    void eat() {}
}

class Dog extends Animal {
    void bark() {}
}
```

Here:

* Dog inherits Animal.
* Dog automatically gets eat().

---

## 🔥 What IS-A Focuses On

* Generalization / Specialization
* Reusability through inheritance
* Polymorphism
* Substitutability

---

## 🔹 Liskov Substitution Principle (VERY IMPORTANT)

If A IS-A B, then:

> A should be usable wherever B is expected.

Example:

```java
void feed(Animal animal) {
    animal.eat();
}
```

This should work for:

* Dog
* Cat
* Lion

If your subclass breaks this → design is wrong.

---

## ❌ Classic Wrong Example

```
Bird
  ├── Sparrow
  └── Penguin
```

If Bird has:

```java
void fly();
```

Penguin cannot fly.

So:

```
Penguin IS-A Bird
But Penguin cannot behave like Bird
```

This violates LSP.

⚠️ Inheritance was used incorrectly.

---

## 🔥 When to Use IS-A

Use inheritance only when:

1. There is a true "type of" relationship.
2. Subclass fully satisfies parent behavior.
3. You need runtime polymorphism.
4. Behavior is stable and unlikely to change.

---

# 🟡 3️⃣ HAS-A Relationship (Composition)

## 🔎 Meaning

> One class contains another class as a member.

Example:

```
Car HAS-A Engine
House HAS-A Room
Order HAS-A Payment
```

In Java:

```java
class Engine {
    void start() {}
}

class Car {
    private Engine engine = new Engine();
}
```

Car uses Engine.

---

## 🔥 What HAS-A Focuses On

* Composition
* Flexibility
* Loose coupling
* Replaceability

---

# 🟠 4️⃣ Composition vs Aggregation

Both are HAS-A.

But slightly different.

---

## 🔸 Composition (Strong Ownership)

If parent dies → child dies.

Example:

```
House HAS-A Room
```

If house is destroyed, rooms don’t exist independently.

```java
class House {
    private Room room = new Room();
}
```

---

## 🔸 Aggregation (Weak Ownership)

Child can exist independently.

Example:

```
Team HAS-A Player
```

Player exists even if team is deleted.

```java
class Team {
    private List<Player> players;
}
```

Players exist separately.

---

# 🧠 5️⃣ Why HAS-A Is Often Better Than IS-A

Very important interview principle:

> Prefer Composition over Inheritance.

Why?

Inheritance:

* Tight coupling
* Rigid hierarchy
* Hard to modify
* Can break LSP

Composition:

* Flexible
* Replaceable behavior
* Easier to extend
* Follows SOLID

---

# 🔥 Example: Payment System

❌ Bad (Inheritance abuse):

```
Payment
   ├── CreditCardPayment
   ├── UpiPayment
   └── PaypalPayment
```

If tomorrow:

* Add fraud detection
* Add logging
* Add retry logic

You must modify all subclasses.

---

✅ Better (Composition + Strategy)

```
Payment
    HAS-A PaymentStrategy
```

```java
interface PaymentStrategy {
    void pay();
}

class CreditCardPayment implements PaymentStrategy {}
class UpiPayment implements PaymentStrategy {}

class Payment {
    private PaymentStrategy strategy;

    void process() {
        strategy.pay();
    }
}
```

Now you can switch behavior easily.

This is clean LLD.

---

# 🧩 6️⃣ Comparing IS-A vs HAS-A

| Feature                 | IS-A          | HAS-A             |
| ----------------------- | ------------- | ----------------- |
| Type                    | Inheritance   | Composition       |
| Coupling                | Tight         | Loose             |
| Flexibility             | Low           | High              |
| Runtime behavior change | Hard          | Easy              |
| LSP risk                | High          | Low               |
| Interview safe?         | Use carefully | Usually preferred |

---

# 🧱 7️⃣ Real LLD Example (Uber)

Let’s model:

### Wrong:

```
CarDriver extends Driver
BikeDriver extends Driver
AutoDriver extends Driver
```

This might work.

But if pricing differs?

Better:

```
Driver HAS-A Vehicle
Vehicle HAS-A PricingStrategy
```

Now:

* Change vehicle
* Change pricing
* Change driver behavior

without breaking hierarchy.

---

# 🎯 8️⃣ Golden Rules for Interviews

### Rule 1:

If sentence makes sense as:

> X is a type of Y

Then consider IS-A.

---

### Rule 2:

If sentence makes sense as:

> X has a Y

Then use HAS-A.

---

### Rule 3:

If behavior may change dynamically → use HAS-A.

---

### Rule 4:

If subclass cannot fully behave like parent → DO NOT use inheritance.

---

# 🏗 9️⃣ UML Perspective

In UML:

IS-A → Solid line with hollow triangle arrow
HAS-A → Line with diamond

You’ll draw these in LLD interviews.

---

# 🔥 1️⃣0️⃣ How Interviewers Trick You

They give:

> Design a Bird system

Most candidates write:

```
Bird
  ├── FlyingBird
  ├── NonFlyingBird
```

Better:

```
Bird HAS-A FlyingBehavior
```

Strategy Pattern.

Clean design.

---

# 🚀 1️⃣1️⃣ How This Connects to Design Patterns

| Pattern         | Uses IS-A | Uses HAS-A |
| --------------- | --------- | ---------- |
| Strategy        | ❌         | ✅          |
| Decorator       | ❌         | ✅          |
| Observer        | ❌         | ✅          |
| Template Method | ✅         | ❌          |
| Factory         | ❌         | ✅          |

Modern clean architecture favors HAS-A patterns.

---

# 🧠 1️⃣2️⃣ Advanced Insight (SDE-2 Level)

Inheritance is about **identity**
Composition is about **capability**

Example:

Dog IS-A Animal → Identity
Car HAS-A Engine → Capability

Capabilities change.
Identity rarely changes.

That’s why composition is more flexible.

---

# 💎 Final Mental Model

If you’re unsure, ask yourself:

> “Am I modeling a type relationship or a responsibility relationship?”

Type → IS-A
Responsibility / behavior → HAS-A

