# Adapter Design Pattern

## ✅ What is the Adapter Pattern?

The **Adapter Pattern** is a structural design pattern that allows objects with incompatible interfaces to work together. It acts as a bridge between two incompatible interfaces.

> **Intent:** Convert the interface of a class into another interface clients expect.

## 🧠 Why Use It?

* You have an existing class, but its interface doesn't match what your code expects.
* You want to reuse existing classes without modifying them.
* You want to integrate third-party or legacy code into a unified system.

## 🔌 Real-World Analogy

A **power adapter** converts a plug from one type to another so that it fits a different type of socket. Similarly, in programming, an adapter converts one interface into another.

---

## 📦 Generic Java Example

### 🔧 Existing Class (Incompatible Interface)

```java
class OldCharger {
    public void chargeWithTwoPin() {
        System.out.println("Charging with two-pin charger");
    }
}
```

### 🎯 Target Interface

```java
interface ThreePinCharger {
    void charge();
}
```

### 🔁 Adapter

```java
class ChargerAdapter implements ThreePinCharger {
    private OldCharger oldCharger;

    public ChargerAdapter(OldCharger oldCharger) {
        this.oldCharger = oldCharger;
    }

    public void charge() {
        oldCharger.chargeWithTwoPin();
    }
}
```

### 👨‍💻 Client

```java
public class Main {
    public static void main(String[] args) {
        OldCharger old = new OldCharger();
        ThreePinCharger adapter = new ChargerAdapter(old);
        adapter.charge(); // Output: Charging with two-pin charger
    }
}
```

---

## 📱 Real-World Example: Notification Service

### 🎯 Target Interface

```java
public interface Notifier {
    void send(String message);
}
```

### ✅ Email Notifier (Compatible)

```java
public class EmailNotifier implements Notifier {
    public void send(String message) {
        System.out.println("Sending Email: " + message);
    }
}
```

### ❌ Third-party SMS Service (Incompatible Interface)

```java
public class ThirdPartySMS {
    public void sendTextMessage(String mobileNumber, String msg) {
        System.out.println("Sending SMS to " + mobileNumber + ": " + msg);
    }
}
```

### 🔁 Adapter for Third-party SMS

```java
public class SMSAdapter implements Notifier {
    private ThirdPartySMS smsService;
    private String mobileNumber;

    public SMSAdapter(ThirdPartySMS smsService, String mobileNumber) {
        this.smsService = smsService;
        this.mobileNumber = mobileNumber;
    }

    @Override
    public void send(String message) {
        smsService.sendTextMessage(mobileNumber, message);
    }
}
```

### 👨‍💻 Client

```java
public class NotificationClient {
    public static void main(String[] args) {
        Notifier emailNotifier = new EmailNotifier();
        emailNotifier.send("Welcome via Email!");

        ThirdPartySMS smsLib = new ThirdPartySMS();
        Notifier smsNotifier = new SMSAdapter(smsLib, "9876543210");
        smsNotifier.send("Welcome via SMS!");
    }
}
```

---

## ✅ Benefits of Adapter Pattern

* Promotes **code reusability**.
* Encourages **separation of concerns**.
* Helps **integrate third-party or legacy systems** without changes.
* Makes the code more **flexible** and **extensible**.

---

## 📌 Summary

| Feature         | Adapter Pattern                         |
| --------------- | --------------------------------------- |
| Purpose         | Convert one interface into another      |
| Common Use Case | Legacy or third-party integration       |
| Pattern Type    | Structural                              |
| Flexibility     | High – works with many types of clients |


## ✅ Key Properties

* **Open/Closed Principle**: You can add new adapters without modifying the base component.
* **Dynamic Composition**: You can compose adapters at runtime.
* **Reuseability**: You can reuse existing adapters.
* **Flexibility**: You can add new adapters without modifying the base component.
* **Extensibility**: You can add new adapters without modifying the base component.
* **Decoupling**: You can add new adapters without modifying the base component.
* **SRP**: Each adapter is responsible for a single responsibility.
