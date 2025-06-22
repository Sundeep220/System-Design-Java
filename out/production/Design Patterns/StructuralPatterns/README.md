# Structural Design Patterns

Structural Design Patterns are concerned with how classes and objects are composed to form larger structures while keeping the system flexible and efficient. These patterns focus on simplifying relationships between entities and making them easier to understand and maintain.

---

## ✅ Why Use Structural Patterns?

* To **reduce complexity** by structuring classes and objects.
* To promote **code reusability** and **flexibility**.
* To make systems **easier to maintain and scale**.
* To improve **object composition** rather than relying on inheritance.

---

## 🧩 Types of Structural Design Patterns

### 1. **Adapter Pattern**

Converts one interface into another expected by the client. It allows incompatible interfaces to work together.

### 2. **Decorator Pattern**

Adds new behavior or responsibilities to objects dynamically without altering their structure.

### 3. **Proxy Pattern**

Provides a surrogate or placeholder for another object to control access or add additional behavior.

### 4. **Composite Pattern**

Composes objects into tree structures to represent part-whole hierarchies. Clients can treat individual objects and composites uniformly.

### 5. **Bridge Pattern**

Decouples an abstraction from its implementation so that the two can vary independently.

### 6. **Facade Pattern**

Provides a simplified interface to a complex subsystem.

### 7. **Flyweight Pattern**

Reduces memory usage by sharing common parts of state between multiple objects instead of storing them separately.

---

Each of these patterns plays a vital role in organizing your code architecture effectively. In the upcoming files, we'll explore each of them in detail with real-world analogies and Java examples.

## 🧩 Structural Design Patterns vs SOLID Principles
| Structural Pattern | Dominant SOLID Principle Applied          | 🔍 How it Applies                                                                                             |
| ------------------ | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| **Adapter**        | **Open/Closed Principle (OCP)**           | Enables adapting new classes without modifying existing code (open for extension, closed for modification).   |
| **Decorator**      | **Single Responsibility Principle (SRP)** | Allows behavior to be added to objects dynamically while keeping each class focused.                          |
| **Proxy**          | **Open/Closed Principle (OCP)**           | Add access control, lazy loading, or logging without changing the actual subject.                             |
| **Composite**      | **Liskov Substitution Principle (LSP)**   | Treat individual and composite objects uniformly through shared interfaces.                                   |
| **Bridge**         | **Dependency Inversion Principle (DIP)**  | Abstraction and implementation are decoupled, depending on interfaces rather than concrete classes.           |
| **Facade**         | **Single Responsibility Principle (SRP)** | Provides a unified interface, isolating clients from complex subsystem logic.                                 |
| **Flyweight**      | **Single Responsibility Principle (SRP)** | Separates shared (intrinsic) and unique (extrinsic) responsibilities to improve performance and memory usage. |
