# 💼 Problem: **Pluggable Notification System with Decorators**

### 🧩 Problem Statement:

You are building a **Notification System** for a large enterprise SaaS product. The core system should be able to send **basic messages** (e.g., system alerts, user welcome messages). However, each client may want to **extend** notifications with features such as:

* ✅ Logging
* ✅ Encryption
* ✅ Retry logic
* ✅ Priority Tagging (e.g., "\[HIGH PRIORITY]")
* ✅ Delay before sending (e.g., scheduled/queued)

These behaviors should be **pluggable dynamically**, without modifying the base system or creating subclass explosions.

---

## 🎯 Objective

Use the **Decorator Pattern** to:

1. Define a `Notifier` interface for sending messages.
2. Implement a `BasicNotifier`.
3. Create decorators for:

    * Logging
    * Retry
    * Encryption
    * PriorityTag
    * DelaySending
4. Compose a dynamic notification chain like:

```
Priority + Logging + Encryption + Retry + BasicNotifier
```

---

## 🔧 Requirements

* Design each decorator to wrap around any other `Notifier`.
* Output should show the **order of processing** clearly.
* Simulate a few client configurations:

    * Basic logging notifier
    * Retry + encrypted + logging notifier
    * High priority + delayed + encrypted + retry + notifier

---

## 🚧 Bonus Constraint (Optional for Extra Challenge):

* Add a `NotifierFactory` that builds and composes decorators based on config.

---

Would you like to:

1. Try implementing it yourself and I’ll review it,
2. Or go step-by-step together starting with the interface and basic notifier?
