# 🧠 Behavioral Design Patterns – Introduction

**Behavioral patterns** are concerned with:

* **Object interaction and responsibility delegation**
* Making sure **objects cooperate efficiently**
* Assigning responsibility in a **loosely coupled** way

These patterns make it easier to understand how different parts of your application **communicate** and help make the system **more maintainable**, **scalable**, and **extensible**.

---

## ✅ Benefits of Behavioral Patterns

| Benefit                 | Explanation                                                                                   |
| ----------------------- | --------------------------------------------------------------------------------------------- |
| 🔄 Improved Flexibility | Objects are loosely coupled—behavior can change dynamically.                                  |
| 🧩 Reusability          | Encapsulate behavior so it can be reused across objects.                                      |
| 🔄 Dynamic Behavior     | Behaviors can be changed or extended at runtime.                                              |
| 📦 Better Encapsulation | Logic is moved to separate classes, following SRP.                                            |
| 📈 Scalable Design      | Makes it easier to add new behaviors without modifying existing code (Open/Closed Principle). |

---

## 📚 One-Line Summary of Each Behavioral Design Pattern

| Pattern                     | Description                                                                                                    |
| --------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **Chain of Responsibility** | Passes request along a chain of handlers until one handles it.                                                 |
| **Command**                 | Encapsulates a request as an object, allowing for parameterization and queuing.                                |
| **Interpreter**             | Implements a grammar and interpreter for a language.                                                           |
| **Iterator**                | Provides a way to access elements of a collection sequentially without exposing the underlying representation. |
| **Mediator**                | Centralizes complex communications between objects in a single mediator object.                                |
| **Memento**                 | Captures and restores an object’s internal state without violating encapsulation.                              |
| **Observer**                | Defines a one-to-many dependency so when one object changes state, all its dependents are notified.            |
| **State**                   | Allows an object to change its behavior when its internal state changes.                                       |
| **Strategy**                | Defines a family of algorithms, encapsulates each one, and makes them interchangeable.                         |
| **Template Method**         | Defines the skeleton of an algorithm in the superclass, letting subclasses override specific steps.            |
| **Visitor**                 | Allows adding new operations to existing object structures without modifying them.                             |

---