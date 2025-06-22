# 🔁 Singleton Design Pattern in Java

## 🎯 Intent

Ensure a class has **only one instance** and provide a **global point of access** to it.

---

## 🔍 Why Use Singleton?

* You need a **single shared resource** (e.g., DB connection, config manager, logger)
* You want **controlled access** to an instance
* You want to **avoid repeated creation** of heavy objects

---

## 🧠 Real-World Analogy

Think of a **President** of a country:

* There can only be **one official president** at a time
* Everyone accesses the same individual for authoritative decisions

---

## 🧱 Structure

```java
public class Singleton {
    private static Singleton instance;

    // Private constructor
    private Singleton() {
        // expensive initialization code
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```

---

## 🔐 Making it Thread-Safe (Lazy Initialization)

```java
public class Singleton {
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

---

## ✅ Best Practices

| Concern           | Best Practice                                            |
| ----------------- | -------------------------------------------------------- |
| Thread safety     | Use synchronized block or static holder                  |
| Serialization     | Override `readResolve()`                                 |
| Reflection attack | Throw exception from constructor if already instantiated |

---

## 📌 Variations

1. **Eager Initialization**

   ```java
   public class Singleton {
       private static final Singleton instance = new Singleton();
       private Singleton() {}
       public static Singleton getInstance() {
           return instance;
       }
   }
   ```

2. **Static Inner Class**

   ```java
   public class Singleton {
       private Singleton() {}
       private static class Holder {
           private static final Singleton INSTANCE = new Singleton();
       }
       public static Singleton getInstance() {
           return Holder.INSTANCE;
       }
   }
   ```

---

## 🧪 Example: Logger Singleton

```java
public class Logger {
    private static Logger instance;
    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
}

// Usage
Logger logger = Logger.getInstance();
logger.log("Singleton pattern in action!");
```

---

## 🚫 When **NOT** to Use Singleton

* When your class needs to support **multiple configurations or test instances**
* When you want to avoid **global state** (testability, side-effects)

---

## ✅ Summary

| Feature              | Value                                         |
| -------------------- | --------------------------------------------- |
| Ensures one instance | ✅                                             |
| Global access        | ✅                                             |
| Thread-safe options  | ✅ (use double-checked locking or inner class) |
| Common use cases     | Logger, DB connection, config manager         |

---

