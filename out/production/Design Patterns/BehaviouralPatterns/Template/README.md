# 🧠 Template Method Pattern – Full Explanation

### 🔍 **Intent**:

> *"Define the skeleton of an algorithm in a method, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps of an algorithm without changing its structure."*

---

## 🏠 Real-World Analogy

### ☕ Coffee/Tea Preparation Template

When making tea or coffee:

* **Boil water**
* **Brew** (different for tea vs. coffee)
* **Pour in cup**
* **Add condiments** (milk/sugar/lemon)

You can **template** this process, and let subclasses like `Tea` and `Coffee` customize just the `brew()` and `addCondiments()` steps.

---

## ✅ Problem Before Template Pattern

* Common steps shared across algorithms are **duplicated** in multiple classes.
* Hard to ensure consistent structure.
* If shared steps change, **every class needs updates** → violates **DRY principle**.

---

## ✅ Template Method Pattern to the Rescue

* The **abstract base class** defines the **template method** — the fixed algorithm.
* Certain steps in the algorithm are abstract or "hooks" — to be **implemented or overridden** by subclasses.

---

## 🧱 Participants

| Role               | Description                                                |
| ------------------ | ---------------------------------------------------------- |
| **Abstract Class** | Defines the **template method** and abstract/hook methods. |
| **Concrete Class** | Implements the variable parts of the algorithm.            |

---

## 🧑‍💻 Java Example – Beverage Preparation

---

### ✅ 1. Abstract Class

```java
abstract class Beverage {
    // Template method
    public final void prepareRecipe() {
        boilWater();
        brew();
        pourInCup();
        addCondiments();
    }

    private void boilWater() {
        System.out.println("Boiling water");
    }

    private void pourInCup() {
        System.out.println("Pouring into cup");
    }

    // Steps to be implemented by subclasses
    protected abstract void brew();
    protected abstract void addCondiments();
}
```

---

### ✅ 2. Concrete Classes

```java
class Tea extends Beverage {
    protected void brew() {
        System.out.println("Steeping the tea");
    }

    protected void addCondiments() {
        System.out.println("Adding lemon");
    }
}

class Coffee extends Beverage {
    protected void brew() {
        System.out.println("Dripping coffee through filter");
    }

    protected void addCondiments() {
        System.out.println("Adding sugar and milk");
    }
}
```

---

### ✅ 3. Demo

```java
public class TemplatePatternDemo {
    public static void main(String[] args) {
        Beverage tea = new Tea();
        System.out.println("Making tea...");
        tea.prepareRecipe();

        System.out.println("\nMaking coffee...");
        Beverage coffee = new Coffee();
        coffee.prepareRecipe();
    }
}
```

---

### ✅ Output:

```
Making tea...
Boiling water
Steeping the tea
Pouring into cup
Adding lemon

Making coffee...
Boiling water
Dripping coffee through filter
Pouring into cup
Adding sugar and milk
```

---

## ✅ When to Use Template Pattern

* When multiple classes share a common **algorithm structure**, but differ in **some steps**.
* When you want to enforce **consistent flow** but allow custom behavior in parts.
* When following **Hollywood Principle**: "Don't call us, we’ll call you" (base class controls flow, subclasses fill in details).

---

## 🔧 Hook Methods (Optional Step)

You can define **optional steps** as "hooks":

```java
protected boolean customerWantsCondiments() {
    return true;
}
```

And then:

```java
if (customerWantsCondiments()) {
    addCondiments();
}
```

Subclasses can override the hook to skip that step.
