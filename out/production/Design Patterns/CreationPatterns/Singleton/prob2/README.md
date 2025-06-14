# 🧠 Singleton Pattern Assignment – ApplicationConfig Manager

## 🎯 Problem Statement

Design a class called `ApplicationConfig` using the **Singleton Pattern** in Java. This class will act as a **global configuration manager**, storing and retrieving key-value pairs used throughout the application.

---

## 📦 Class: `ApplicationConfig`

### Responsibilities:

- Ensure that only **one instance** of `ApplicationConfig` is created.
- Store configuration settings in a `HashMap<String, String>`.
- Provide methods to:
    - `set(String key, String value)` – to set a configuration value.
    - `get(String key)` – to retrieve a configuration value.
    - `getInstance()` – to retrieve the Singleton instance.

---

## 🧱 Requirements:

1. **Private constructor** – prevent external instantiation.
2. **Static instance variable** to hold the Singleton instance.
3. **Public static `getInstance()`** method to return the only instance.
4. Demonstrate that all parts of your code share the same instance.
5. Use a `main()` class to:
    - Set and get a few properties.
    - Print properties from different variables.
    - Check object identity using `==`.

---

## 🔍 Sample Main Class Usage:

```java
public class Main {
    public static void main(String[] args) {
        ApplicationConfig config1 = ApplicationConfig.getInstance();
        config1.set("db_url", "jdbc:mysql://localhost");
        config1.set("timeout", "30s");

        ApplicationConfig config2 = ApplicationConfig.getInstance();
        System.out.println("db_url from config1: " + config1.get("db_url"));
        System.out.println("timeout from config2: " + config2.get("timeout"));

        System.out.println("config1 == config2: " + (config1 == config2)); // true
    }
}
```

## Expected Output:

```
db_url from config1: jdbc:mysql://localhost
timeout from config2: 30s
config1 == config2: true
```