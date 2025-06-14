# Creational Design Patterns - Assignment 1

## 🧠 Pattern: Singleton

### 📌 Problem Statement

Design a `Logger` class using the **Singleton pattern**. This logger should ensure **only one instance** is used across the application.

### 🧪 Requirements

- Implement the Singleton pattern in a thread-safe manner.
- Include the following methods:
    - `log(String message)` – Logs a message with a timestamp.
    - `getLogs()` – Returns all logs as a list or string.
- Demonstrate usage:
    - Create multiple threads that access the logger.
    - Ensure only one instance is used (prove using hashcode or object reference).

### 🧩 Example Output

```
    Logger instance created
    2023-09-12T10:15:30: Log from Thread-1
    2023-09-12T10:15:30: Log from Thread-2
    2023-09-12T10:15:30: Log from Thread-3
```



### 🚀 Objective

- Practice designing a Singleton in Java.
- Understand thread safety in Singleton.
- Real-world relevance: Logger, DB connection, ConfigurationManager.

---

✅ **Once you're done**, paste your code for review and I'll provide feedback.  
