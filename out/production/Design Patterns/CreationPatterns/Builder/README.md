# 🧱 Builder Design Pattern in Java

## 🎯 Intent

> Separate the construction of a complex object from its representation so that the same construction process can create different representations.

---

## 🧠 Real-World Analogy

Think of ordering a **custom burger**:

* You choose bun, patty, cheese, toppings
* A **burger builder** constructs your burger step-by-step

Different customers can build different burgers using the **same steps** but with different choices.

---

## 🔧 Structure

```java
class Burger {
    private String bun;
    private String patty;
    private boolean cheese;
    private boolean lettuce;

    private Burger(BurgerBuilder builder) {
        this.bun = builder.bun;
        this.patty = builder.patty;
        this.cheese = builder.cheese;
        this.lettuce = builder.lettuce;
    }

    public static class BurgerBuilder {
        private String bun;
        private String patty;
        private boolean cheese;
        private boolean lettuce;

        public BurgerBuilder setBun(String bun) {
            this.bun = bun;
            return this;
        }

        public BurgerBuilder setPatty(String patty) {
            this.patty = patty;
            return this;
        }

        public BurgerBuilder setCheese(boolean cheese) {
            this.cheese = cheese;
            return this;
        }

        public BurgerBuilder setLettuce(boolean lettuce) {
            this.lettuce = lettuce;
            return this;
        }

        public Burger build() {
            return new Burger(this);
        }
    }

    public void showBurger() {
        System.out.println("Burger: " + bun + ", " + patty + ", Cheese: " + cheese + ", Lettuce: " + lettuce);
    }
}
```

---

## ✅ Usage

```java
Burger burger = new Burger.BurgerBuilder()
    .setBun("Sesame")
    .setPatty("Chicken")
    .setCheese(true)
    .setLettuce(true)
    .build();

burger.showBurger();
```

---

## ✅ Advantages

* Builds complex objects step-by-step
* More readable than telescoping constructors
* Supports immutability (if needed)
* Allows optional values without multiple constructors

---

## 🔄 Telescoping Constructor Problem

```java
public class Car {
    public Car(String engine) {} // ok
    public Car(String engine, String wheels) {} // getting verbose
    public Car(String engine, String wheels, String color, boolean ac, boolean sunroof) {}
}
```

Builder solves this with a **fluent, readable API**.

---

## 🧪 Real-World Use Cases

* DTO / model construction in layered architectures
* Setting up test data (builders in test cases)
* UI configuration (e.g., dialog builders)
* `StringBuilder`, `StringBuffer` in Java

---

## 📌 Summary

| Feature     | Description                                        |
| ----------- | -------------------------------------------------- |
| Type        | Creational Pattern                                 |
| Key Idea    | Build complex objects in steps                     |
| Strength    | Readability, flexibility, immutability support     |
| When to Use | Object has lots of optional fields or combinations |

---
