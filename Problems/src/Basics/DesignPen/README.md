# 🚀 Let’s Start With Very Basic Problems

We’ll go incrementally.

## 🟢 Level 1 — Very Basic Object Modeling

### Problem 1: Design a Pen

**Requirements:**

* A Pen has:

    * brand
    * color
    * type (gel, ball, fountain)
* A pen can:

    * write()
    * refill()

---

### 🎯 Why This Is Important

You may think this is trivial.

But this teaches:

* How to model simple entities
* How to think about responsibility
* When to use enum
* Encapsulation basics


Good. That’s actually perfect. 👌
This is where real LLD learning starts.

You’re not supposed to “know” — you’re supposed to **extract structure from vague text**.

Let’s train that skill.

---

# 🟢 Problem: Design a Pen

Requirement:

* A Pen has:

    * brand
    * color
    * type (gel, ball, fountain)
* A pen can:

    * write()
    * refill()

---

# 🧠 Step 1 — Extract Nouns (Expert Trick)

Whenever you're stuck:

👉 Underline nouns.

From the problem:

* Pen
* brand
* color
* type
* gel
* ball
* fountain

Now ask:

Which of these are real-world objects?

Only one object actually exists independently.

👉 **Pen**

Everything else is just data about a pen.

---

# ✅ So What Class Do We Need?

We need:

```
Pen
```

That’s it.

This is a **Level 1 modeling problem**.

No overengineering.

---

# 🧠 Step 2 — Identify Attributes (State)

From requirement:

A Pen has:

* brand
* color
* type

So inside class Pen:

```
private String brand;
private String color;
private PenType type;
```

Now notice something important:

Type has limited values:

* gel
* ball
* fountain

This is NOT a String.

This is an **enum**.

---

# ✅ So We Also Need:

```
enum PenType {
    GEL,
    BALL,
    FOUNTAIN
}
```

Now we have:

* 1 Class
* 1 Enum

Simple. Clean. Correct.

---

# 🧠 Step 3 — Identify Behaviors

Pen can:

* write()
* refill()

So methods:

```
public void write()
public void refill()
```

---

# 📘 Final Design (Complete Code)

```java
// Enum for type
public enum PenType {
    GEL,
    BALL,
    FOUNTAIN
}

// Pen class
public class Pen {

    private String brand;
    private String color;
    private PenType type;

    public Pen(String brand, String color, PenType type) {
        this.brand = brand;
        this.color = color;
        this.type = type;
    }

    public void write() {
        System.out.println(type + " pen is writing in " + color + " color.");
    }

    public void refill() {
        System.out.println("Refilling the pen...");
    }

    // Getters
    public String getBrand() { return brand; }
    public String getColor() { return color; }
    public PenType getType() { return type; }
}
```

---

# 🎯 What You Just Learned

Even in this tiny problem:

✔ Extract nouns
✔ Identify entity
✔ Decide enum vs String
✔ Assign responsibility
✔ Encapsulation

This is how LLD thinking starts.

---

# 🟢 Now Let’s Level It Slightly Up

New requirement added:

> Some pens are refillable, some are not.

Now think carefully.

❓ Should refill() exist in all pens?

If not, how would you redesign?

Try thinking. Don’t worry about being wrong.


Perfect. This is exactly how you build LLD thinking. 💪
You don’t “know” — you **reason**.

Let’s think like an expert.

---

# 🟢 New Requirement

> Some pens are refillable, some are not.

Current design:

```java
public void refill()
```

exists in `Pen`.

---

# 🧠 Step 1 — Ask the SRP Question

Does **every Pen** refill?

No.

So if we keep:

```java
public void refill()
```

inside `Pen`, then:

* Non-refillable pens will also have `refill()`
* That violates proper modeling

This is where beginners make mistakes.

---

# 🧠 Step 2 — Identify Variation

Whenever you hear:

> “Some objects behave differently”

Your brain should scream:

🚨 **INHERITANCE OR INTERFACE**

---

# 🟢 Expert Approach

We separate common behavior and special behavior.

### What is common?

* brand
* color
* type
* write()

### What is different?

* refill()

---

# ✅ Solution Approach 1 (Clean OOP Design)

Make `Pen` abstract.

Create two subclasses:

* `RefillablePen`
* `NonRefillablePen`

---

# 📘 Updated Design

```java
public enum PenType {
    GEL,
    BALL,
    FOUNTAIN
}

// Abstract base class
public abstract class Pen {

    protected String brand;
    protected String color;
    protected PenType type;

    public Pen(String brand, String color, PenType type) {
        this.brand = brand;
        this.color = color;
        this.type = type;
    }

    public void write() {
        System.out.println(type + " pen writing in " + color);
    }
}
```

---

## Refillable Pen

```java
public class RefillablePen extends Pen {

    public RefillablePen(String brand, String color, PenType type) {
        super(brand, color, type);
    }

    public void refill() {
        System.out.println("Refilling the pen...");
    }
}
```

---

## Non-Refillable Pen

```java
public class NonRefillablePen extends Pen {

    public NonRefillablePen(String brand, String color, PenType type) {
        super(brand, color, type);
    }
}
```

Notice:

NonRefillablePen does NOT have refill().

Correct modeling. Clean design.

---

# 🎯 What Just Happened?

You just learned:

* How to handle optional behavior
* When to use inheritance
* How SRP affects design
* Avoiding “fat base class”

This is real LLD thinking.

---

# 🟢 But Wait — Expert Level Thought

An even better design (more flexible):

Instead of inheritance, use **composition**.

Because refill is a behavior capability, not a type.

But that’s next level — we’ll reach that gradually.

---

Excellent. 🔥
Now you're thinking like someone who wants to become strong in LLD.

Let’s go to the **next level of thought** we hinted at:

> Instead of inheritance, use **composition** because refill is a capability, not a type.

This is where beginners become intermediate.

---

# 🧠 Why Inheritance May Not Be Ideal Here

We created:

* RefillablePen
* NonRefillablePen

But think carefully…

Is “refillable” really a *type* of pen?

Or is it a **behavior/capability**?

If tomorrow requirement changes:

> Some refillable pens have different refill mechanisms (ink cartridge, ink tank, syringe refill)

Now what?

You’ll create:

* CartridgeRefillablePen
* TankRefillablePen
* SyringeRefillablePen

This explodes quickly.

🚨 That’s inheritance misuse.

---

# 🧠 Expert Thought: Model Capabilities Separately

Refill is not a type.
It is a **behavior that can vary**.

So we extract it.

---

# 🟢 Step 1 — Create a RefillStrategy Interface

```java
public interface RefillStrategy {
    void refill();
}
```

Now we create concrete strategies.

---

## Cartridge Refill

```java
public class CartridgeRefill implements RefillStrategy {

    @Override
    public void refill() {
        System.out.println("Replacing ink cartridge...");
    }
}
```

---

## Tank Refill

```java
public class TankRefill implements RefillStrategy {

    @Override
    public void refill() {
        System.out.println("Refilling ink tank...");
    }
}
```

---

# 🟢 Step 2 — Modify Pen

Now Pen **has a** refill behavior.

Not *is a* refillable pen.

```java
public class Pen {

    private String brand;
    private String color;
    private PenType type;
    private RefillStrategy refillStrategy; // composition

    public Pen(String brand, String color, PenType type, RefillStrategy refillStrategy) {
        this.brand = brand;
        this.color = color;
        this.type = type;
        this.refillStrategy = refillStrategy;
    }

    public void write() {
        System.out.println(type + " pen writing in " + color);
    }

    public void refill() {
        if (refillStrategy == null) {
            System.out.println("This pen is not refillable.");
        } else {
            refillStrategy.refill();
        }
    }
}
```

---

# 🟢 Usage Example

```java
public class Main {
    public static void main(String[] args) {

        Pen refillablePen = new Pen(
                "Parker",
                "Blue",
                PenType.GEL,
                new CartridgeRefill()
        );

        Pen disposablePen = new Pen(
                "Reynolds",
                "Black",
                PenType.BALL,
                null
        );

        refillablePen.refill();  // Replacing ink cartridge...
        disposablePen.refill();  // This pen is not refillable.
    }
}
```

---

# 🎯 What Pattern Did You Just Learn?

This is **Strategy Pattern**.

We extracted variable behavior.

---

# 🧠 Why This Is Better (LLD Expert Thinking)

✔ No subclass explosion
✔ Open for extension
✔ Closed for modification
✔ Behavior can change at runtime
✔ Cleaner modeling

This is real OCP.

---

# 🔥 Important LLD Lesson

Whenever you see:

> “Some objects behave differently”

Ask yourself:

Is this a TYPE difference?
Or a BEHAVIOR difference?

If behavior → Prefer Composition.

If true type difference → Inheritance.

---

# 🟢 Now Let Me Ask You Something Important

Which relationship is this?

```
Pen --> RefillStrategy
```

Is it:

* Association?
* Aggregation?
* Composition?

Think carefully before answering.


