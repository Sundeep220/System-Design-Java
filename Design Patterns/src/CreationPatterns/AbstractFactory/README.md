# 🏭🏭 Abstract Factory Design Pattern in Java

## 🎯 Intent

> Provide an interface for creating **families of related or dependent objects** without specifying their concrete classes.

---

## 🧠 Real-World Analogy

Imagine a **Furniture Factory**:

* IKEA makes **Modern Chair**, **Modern Table**
* Victorian Furniture makes **Victorian Chair**, **Victorian Table**

Depending on the factory selected, a **consistent theme** of related products is created.

---

## 🔧 Structure

```java
// Abstract Product A
interface Chair {
    void sit();
}

// Abstract Product B
interface Table {
    void use();
}

// Concrete Product A1
class ModernChair implements Chair {
    public void sit() {
        System.out.println("Sitting on modern chair");
    }
}

// Concrete Product B1
class ModernTable implements Table {
    public void use() {
        System.out.println("Using modern table");
    }
}

// Concrete Product A2
class VictorianChair implements Chair {
    public void sit() {
        System.out.println("Sitting on victorian chair");
    }
}

// Concrete Product B2
class VictorianTable implements Table {
    public void use() {
        System.out.println("Using victorian table");
    }
}

// Abstract Factory
interface FurnitureFactory {
    Chair createChair();
    Table createTable();
}

// Concrete Factory 1
class ModernFurnitureFactory implements FurnitureFactory {
    public Chair createChair() { return new ModernChair(); }
    public Table createTable() { return new ModernTable(); }
}

// Concrete Factory 2
class VictorianFurnitureFactory implements FurnitureFactory {
    public Chair createChair() { return new VictorianChair(); }
    public Table createTable() { return new VictorianTable(); }
}

// Client Code
class Application {
    private Chair chair;
    private Table table;

    public Application(FurnitureFactory factory) {
        chair = factory.createChair();
        table = factory.createTable();
    }

    public void use() {
        chair.sit();
        table.use();
    }
}

// Usage
FurnitureFactory factory = new ModernFurnitureFactory();
Application app = new Application(factory);
app.use();
```

---

## ✅ Benefits

* Ensures products from the **same family** are used together
* Adds new product families **without modifying existing code**
* Promotes **consistency** across related products

---

## 🔄 Factory vs Abstract Factory

| Aspect     | Factory                  | Abstract Factory                          |
| ---------- | ------------------------ | ----------------------------------------- |
| Purpose    | Create one product       | Create related products (families)        |
| Complexity | Simple                   | Complex                                   |
| Example    | EmailNotificationFactory | UI Widget Kit (Button + Dropdown + Input) |

---

## 🧪 Real-World Use Case

* UI Toolkits: WindowsFactory, MacFactory, LinuxFactory
* Vehicle factories: LuxuryCarFactory, SportsCarFactory
* Theme-based widgets: LightThemeFactory, DarkThemeFactory

---

## 📌 Summary

| Feature  | Description                                              |
| -------- | -------------------------------------------------------- |
| Type     | Creational Pattern                                       |
| Key Idea | Group related factories under one interface              |
| Benefit  | Product family consistency, decoupled client code        |
| Use When | Your code needs to work with families of related objects |

---
