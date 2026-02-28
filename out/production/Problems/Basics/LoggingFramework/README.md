# 🧱 Designing a Logging Framework

---

# 📌 Problem Statement

Design a logging framework that:

* Supports multiple log levels: `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`
* Logs message with:

    * Timestamp
    * Log level
    * Message content
* Supports multiple output destinations:

    * Console
    * File
    * Database
* Provides configuration mechanism
* Is thread-safe
* Is extensible for future enhancements

---

# 🏗 High-Level Design Approach

We divided the system into **clear responsibility layers**:

```
Application
   ↓
Logger (Orchestrator)
   ↓
LogRecord (Immutable Event)
   ↓
Appender (Destination Strategy)
   ↓
Formatter (Presentation Strategy)
   ↓
Output (Console/File/DB)
```

We applied:

* Strategy Pattern (Formatter, Appender)
* Builder Pattern (Configuration)
* Decorator Pattern (AsyncLogger)
* SRP & Open/Closed Principle

---

# 🧩 Core Components and Responsibilities

---

## 1️⃣ LogLevel (Enum)

### Purpose:

Represents logging severity levels.

### Features:

* Explicit priority values
* Comparison method

### Why:

* Supports hierarchical logging
* Avoids fragile ordinal-based comparison

Example:

```java
DEBUG(1), INFO(2), WARN(3), ERROR(4), FATAL(5)
```

---

## 2️⃣ LogRecord (Immutable Data Class)

### Purpose:

Represents one logging event.

### Fields:

* `String message`
* `LogLevel level`
* `Instant timestamp`
* `String threadName`

### Why Immutable?

* Thread-safe
* No accidental mutation
* Predictable behavior

This ensures every log event is a stable object.

---

## 3️⃣ Formatter (Strategy Pattern)

### Interface:

```java
String format(LogRecord record);
```

### Implementations:

* `PlainTextFormatter`
* (Can add `JsonFormatter` later)

### Purpose:

Converts LogRecord → formatted string.

Why Strategy?

* Different appenders may need different formats.
* Easily extensible.

---

## 4️⃣ Appender (Strategy Pattern)

### Interface:

```java
void append(LogRecord record);
```

### Implementations:

* `ConsoleAppender`
* `FileAppender`
* `DatabaseAppender`

### Responsibility:

* Owns a Formatter
* Converts record to string (if needed)
* Writes to destination

Why Strategy?

* Open for new destinations
* Logger doesn’t change when new output is added

---

## 5️⃣ Logger (Interface)

### Methods:

```java
debug(), info(), warn(), error(), fatal()
```

### Purpose:

Provides clean logging API.

---

## 6️⃣ DefaultLogger (Core Implementation)

### Fields:

* `LogLevel currentLevel`
* `List<Appender> appenders`

### Responsibilities:

1. Filter logs based on level
2. Create LogRecord
3. Dispatch to all appenders
4. Handle appender exceptions safely

This class is the orchestrator.

---

## 7️⃣ LoggerBuilder (Configuration Mechanism)

### Purpose:

Allows flexible configuration.

Example usage:

```java
Logger logger = LoggerBuilder.newBuilder()
    .setLogLevel(LogLevel.INFO)
    .addAppender(new ConsoleAppender(new PlainTextFormatter()))
    .addAppender(new FileAppender("app.log", new PlainTextFormatter()))
    .build();
```

### Why Builder?

* Multiple optional parameters
* Clean API
* Extensible
* Avoids telescoping constructors

---

## 8️⃣ AsyncLogger (Thread-Safe Wrapper)

### Purpose:

Ensures non-blocking, thread-safe logging.

### Internals:

* `BlockingQueue<LogRecord>`
* Worker thread
* Delegates to underlying logger

### Flow:

1. Threads enqueue log events
2. Worker consumes in FIFO order
3. Writes sequentially

Why this approach?

* Prevents thread contention
* Preserves order
* Scales well
* Industry-standard design

---

# 🔄 End-to-End Logging Flow

When user writes:

```java
logger.info("Payment successful");
```

### Step 1:

Logger checks if INFO ≥ configured level

### Step 2:

Creates immutable LogRecord:

* timestamp = now
* threadName = current thread

### Step 3:

If AsyncLogger:

* Record added to queue

### Step 4:

Worker thread consumes record

### Step 5:

For each Appender:

* Appender calls formatter.format(record)
* Writes to destination

---

# 🧠 Thread Safety Strategy

We achieved thread safety by:

* Immutable LogRecord
* Async queue-based processing
* Single writer thread
* No shared mutable state

This ensures:

* No interleaving writes
* No blocking main threads
* Ordering preserved

---

# 🔓 Extensibility

Our design supports:

### New Log Level:

Add enum constant → no other changes.

### New Appender:

Create class implementing Appender → no Logger change.

### New Formatter:

Implement Formatter → no Logger change.

### New Logger Type:

Implement Logger interface → no system break.

Open/Closed Principle fully respected.

---

# 📊 Requirement Coverage Matrix

| Requirement                  | Status |
| ---------------------------- | ------ |
| Multiple log levels          | ✅      |
| Timestamp + metadata         | ✅      |
| Multiple output destinations | ✅      |
| Configuration mechanism      | ✅      |
| Thread-safe                  | ✅      |
| Extensible                   | ✅      |

---

# 🎯 Design Patterns Used

| Pattern   | Where Used                           |
| --------- | ------------------------------------ |
| Strategy  | Formatter, Appender                  |
| Builder   | LoggerBuilder                        |
| Decorator | AsyncLogger                          |
| SRP       | Clean separation of responsibilities |
| OCP       | Extensible appenders and formatters  |

---

# 🏆 Final Architecture Strength

This logging framework:

* Is modular
* Is scalable
* Is production-ready
* Follows SOLID principles
* Separates concerns cleanly
* Can evolve without breaking existing code

---

# 💬 How You Explain This in Interview

You would say:

> “I separated log creation, formatting, and writing into independent components using Strategy pattern. Logger handles orchestration and filtering. Configuration is handled using Builder pattern. For thread safety, I wrapped the logger in an AsyncLogger that uses a BlockingQueue and worker thread. The design follows Open/Closed principle and is extensible for future enhancements like JSON logging or log rotation.”


