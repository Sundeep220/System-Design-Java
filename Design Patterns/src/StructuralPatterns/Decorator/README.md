Awesome! Let’s now dive into the **Decorator Pattern** — one of the most powerful and flexible structural design patterns.

---

## 🎨 Decorator Pattern

### 📖 Intent:

**Add responsibilities to objects dynamically** at run-time without changing their code.

---

## 🧠 Why Use It?

* To **add behavior** without using inheritance.
* To **layer features dynamically** (e.g., adding logging, caching, compression, encryption).
* To **avoid class explosion** caused by subclassing every combination of features.

---

## 💡 Real-World Analogy

Imagine you're ordering a **base coffee** at a coffee shop, and you want to add:

* Milk
* Sugar
* Caramel
* Whipped cream

Rather than creating a class for every combo (e.g., `CoffeeWithMilkAndSugarAndCream`), each **add-on is a decorator** that wraps the base coffee and adds cost and description.

---

## 🔧 Java Implementation — Base Structure

### ✅ Step 1: Component Interface

```java
public interface Notifier {
    void send(String message);
}
```

---

### ✅ Step 2: Concrete Component

```java
public class BasicNotifier implements Notifier {
    @Override
    public void send(String message) {
        System.out.println("Sending notification: " + message);
    }
}
```

---

## 🔄 Now Let’s Add Decorators

### ✅ Step 3: Abstract Decorator

```java
public abstract class NotifierDecorator implements Notifier {
    protected Notifier notifier;

    public NotifierDecorator(Notifier notifier) {
        this.notifier = notifier;
    }

    public void send(String message) {
        notifier.send(message);
    }
}
```

---

### ✅ Step 4: Concrete Decorators

#### 🔐 Logging Decorator

```java
public class LoggingNotifier extends NotifierDecorator {
    public LoggingNotifier(Notifier notifier) {
        super(notifier);
    }

    @Override
    public void send(String message) {
        System.out.println("Log: Sending notification -> " + message);
        super.send(message);
    }
}
```

#### 🔁 Retry Decorator

```java
public class RetryNotifier extends NotifierDecorator {
    public RetryNotifier(Notifier notifier) {
        super(notifier);
    }

    @Override
    public void send(String message) {
        System.out.println("Attempt 1 to send message...");
        try {
            super.send(message);
        } catch (Exception e) {
            System.out.println("Retrying...");
            super.send(message);
        }
    }
}
```

#### 🔒 Encryption Decorator

```java
public class EncryptedNotifier extends NotifierDecorator {
    public EncryptedNotifier(Notifier notifier) {
        super(notifier);
    }

    @Override
    public void send(String message) {
        String encrypted = "ENCRYPTED(" + message + ")";
        super.send(encrypted);
    }
}
```

---

### ✅ Step 5: Client Code

```java
public class Main {
    public static void main(String[] args) {
        Notifier baseNotifier = new BasicNotifier();

        // Decorate it with Logging + Encryption + Retry
        Notifier secureNotifier = new RetryNotifier(
                                    new EncryptedNotifier(
                                        new LoggingNotifier(baseNotifier)));

        secureNotifier.send("User registration successful.");
    }
}
```

---

### 🧾 Output:

```
Log: Sending notification -> User registration successful.
Attempt 1 to send message...
Sending notification: ENCRYPTED(User registration successful.)
```

---

## 📊 Summary Table

| Concept           | Role                        |
|------------------|-----------------------------|
| Notifier         | Component Interface         |
| BasicNotifier    | Concrete Component          |
| NotifierDecorator| Abstract Decorator          |
| LoggingNotifier  | Concrete Decorator          |
| RetryNotifier    | Concrete Decorator          |
| EncryptedNotifier| Concrete Decorator          |
| Main             | Client                      |

---


## ✅ Key Benefits

* You can **compose features at runtime**.
* All decorators follow the same interface.
* Avoids subclassing explosion.
* Decorators can be composed at runtime.
* Follows Open/Closed Principle.
* Avoids subclassing for every feature combination.
* Each responsibility is isolated in a small, reusable class.

## ✅ Key Properties

* **Open/Closed Principle**: You can add new decorators without modifying the base component.
* **Dynamic Composition**: You can compose decorators at runtime.
* **Reuseability**: You can reuse existing decorators.
* **Flexibility**: You can add new decorators without modifying the base component.
* **Extensibility**: You can add new decorators without modifying the base component.
* **Decoupling**: You can add new decorators without modifying the base component.
* **SRP**: Each decorator is responsible for a single responsibility.
---
