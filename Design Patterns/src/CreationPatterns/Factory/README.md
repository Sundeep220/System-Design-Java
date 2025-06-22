# 🏭 Factory Design Pattern in Java

## 🎯 Intent

> Define an interface for creating an object, but let subclasses decide which class to instantiate.

---

## 🧠 Real-World Analogy

Think of a **Pizza Store**:

* You order "Veg Pizza" or "Cheese Pizza" — but you don’t care how it’s made.
* The **factory (kitchen)** handles the creation logic based on your request.

---

## 📦 Structure

```java
interface Product {
    void use();
}

class ConcreteProductA implements Product {
    public void use() {
        System.out.println("Using Product A");
    }
}

class ConcreteProductB implements Product {
    public void use() {
        System.out.println("Using Product B");
    }
}

class ProductFactory {
    public static Product createProduct(String type) {
        if (type.equalsIgnoreCase("A")) return new ConcreteProductA();
        else if (type.equalsIgnoreCase("B")) return new ConcreteProductB();
        else throw new IllegalArgumentException("Unknown product type");
    }
}

// Usage:
Product p = ProductFactory.createProduct("A");
p.use();
```

---

## ✅ Advantages

* Decouples object creation logic from usage
* Easier to add new types
* Cleaner code with **Open/Closed Principle**

---

## 🔧 When to Use

* When you need to **create objects without exposing the creation logic**
* When the **class being instantiated can vary at runtime**

---

## 🧪 Example: Notification Factory

```java
interface Notification {
    void notifyUser();
}

class SMSNotification implements Notification {
    public void notifyUser() {
        System.out.println("Sending SMS Notification");
    }
}

class EmailNotification implements Notification {
    public void notifyUser() {
        System.out.println("Sending Email Notification");
    }
}

class NotificationFactory {
    public static Notification createNotification(String channel) {
        if (channel.equalsIgnoreCase("SMS")) return new SMSNotification();
        else if (channel.equalsIgnoreCase("Email")) return new EmailNotification();
        else throw new IllegalArgumentException("Unknown channel");
    }
}

// Usage:
Notification n = NotificationFactory.createNotification("Email");
n.notifyUser();
```

---

## 📌 Summary

| Feature  | Description                          |
| -------- | ------------------------------------ |
| Type     | Creational Pattern                   |
| Key Idea | Encapsulate object creation logic    |
| Benefits | Flexibility, Clean Code, Scalability |
| When     | Type of object determined at runtime |

---
