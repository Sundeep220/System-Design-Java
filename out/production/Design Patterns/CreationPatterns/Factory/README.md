# 🏭 Factory Method Pattern Assignment

## 🎯 Problem: Notification Service Factory

You are designing a **Notification System** that sends notifications through different channels: Email, SMS, and Push Notification.

The notification types may vary and the client code should not be responsible for instantiating the concrete notification classes directly.

---

## 📦 Requirements

### 1. Define a `Notification` interface (or abstract class)
- Method: `void send(String message)`

### 2. Implement concrete classes:
- `EmailNotification`
- `SMSNotification`
- `PushNotification`

Each class should implement the `send()` method to simulate sending a notification by printing a message like:

```java
Sending Email with message: <message>
Sending SMS with message: <message>
Sending Push Notification with message: <message>
```

---

### 3. Create a `NotificationFactory` class

- Method:
```java
public static Notification createNotification(String type)
```

- Based on the `type` ("EMAIL", "SMS", "PUSH"), return the corresponding `Notification` instance.
- If the type is unknown, return `null` or throw an `IllegalArgumentException`.

---

## 🧪 Testing

In a `Main` class:

1. Get a notification instance using the factory:
   ```java
   Notification notification = NotificationFactory.createNotification("EMAIL");
   ```

2. Call the `send()` method with any sample message.

3. Repeat the same for all types: "EMAIL", "SMS", "PUSH".

4. Try an invalid input like "WHATSAPP" and handle it gracefully.

---

## 🧠 Bonus (Optional)

- Instead of using strings directly, use an `enum NotificationType { EMAIL, SMS, PUSH }` for better type safety.
- Add a configuration (Map) that decides which type to use dynamically.

---

## ✅ Expected Output (Sample)

```
Sending Email with message: Hello from Email!
Sending SMS with message: Hello from SMS!
Sending Push Notification with message: Hello from Push!
Invalid notification type: WHATSAPP
```

---

### 🔍 Design Intent

- You should NOT instantiate `EmailNotification`, `SMSNotification`, etc., directly in the `Main` class.
- Let the factory abstract away the creation logic.
