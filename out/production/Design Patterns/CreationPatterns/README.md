# 🔨 Creational Design Patterns in Java

## 🎯 What are Creational Design Patterns?

Creational design patterns are concerned with the **way objects are created**. These patterns abstract the instantiation process, making it more flexible and reusable.

They help:
- Decouple your code from the specific classes it uses
- Handle object creation in controlled, scalable, and optimized ways

---

## 📦 Types of Creational Patterns

| Pattern | Purpose |
|--------|---------|
| 🔁 Singleton | Ensure only **one instance** of a class exists and provide a global access point |
| 🏭 Factory Method | Define an interface for creating objects but allow subclasses to alter the type of objects that will be created |
| 🏭🏭 Abstract Factory | Create families of related objects without specifying their concrete classes |
| 🧱 Builder | Construct a complex object step-by-step and produce different representations of it |
| 🧬 Prototype | Clone existing objects to avoid costly creation and configuration |

---

## ✅ When to Use Creational Patterns

- When your code frequently creates **complex objects**
- When the instantiation logic **needs to be abstracted**
- When you want to **decouple object creation from business logic**
- When object creation is **expensive or error-prone**

---

## 🧠 Key Goals

- Improve **flexibility**
- Enable **reusability**
- Enhance **testability** by using interfaces over concrete types
- Support **immutability** where needed (e.g., Builder)

---

## 📌 Summary

| Pattern       | Object Creation Style        | Used When... |
|---------------|------------------------------|---------------|
| Singleton      | Only one global instance     | Single point of control (e.g., DB connection, logger) |
| Factory Method | Subclass decides the type    | You don’t want to bind your code to specific classes |
| Abstract Factory | Groups of related factories | You need to create related objects (like UI kits) |
| Builder        | Step-by-step, customizable   | Object has many optional/configurable parts |
| Prototype      | Clone existing object        | Object creation is costly and similar copies are needed |

---

## 🚀 Creational Patterns vs SOLID Principles:
| Design Pattern        | Dominant SOLID Principle(s)                       | Reason                                                                |
| --------------------- | ------------------------------------------------- | --------------------------------------------------------------------- |
| 🔁 Singleton          | **S** - Single Responsibility                     | Centralizes access to a single instance (e.g., Logger, ConfigManager) |
| 🏭 Factory Method     | **O** - Open/Closed                               | Easy to extend by adding new product types without modifying factory  |
| 🏭🏭 Abstract Factory | **O**, **D** - Open/Closed, Dependency Inversion  | Depends on abstractions to create families of related objects         |
| 🧱 Builder            | **S**, **O** - Single Responsibility, Open/Closed | Separates construction from representation, and is extensible         |
| 🧬 Prototype          | **O**, **L** - Open/Closed, Liskov                | New types can extend prototype and be cloned without altering clients |


