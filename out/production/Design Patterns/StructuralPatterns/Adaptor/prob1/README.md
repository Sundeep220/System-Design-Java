# 💼 Problem: **MultiPayment Gateway Integration System**

### 🧩 Problem Statement:

You are developing a **Payment Gateway Integrator System** for an e-commerce platform.

Your system uses a unified interface `PaymentProcessor` to process payments.

Your application already supports:

* ✅ `CreditCardProcessor`

Now, your platform needs to integrate **two new third-party payment APIs** that use completely different interfaces and methods:

* ❌ `PayPalAPI` – requires `authenticate()` and then `sendPayment(double amount)`
* ❌ `StripeSDK` – processes payments using `makePayment(int cents)` method

You are **not allowed to modify** these third-party classes.

### 🎯 Objective:

* Design a system that lets you treat **all payment processors** uniformly via `PaymentProcessor` interface.
* Use the **Adapter Pattern** to integrate `PayPalAPI` and `StripeSDK`.

---

## ✅ Requirements

1. Create a unified `PaymentProcessor` interface with `void pay(double amount)`.
2. Create adapters:

    * `PayPalAdapter` for `PayPalAPI`
    * `StripeAdapter` for `StripeSDK`
3. Write a `PaymentService` class that takes any `PaymentProcessor` and triggers payment.
4. Simulate the client code where:

    * A credit card payment is made.
    * A PayPal payment is made.
    * A Stripe payment is made.

---

## 📦 Provided Third-Party Classes (You CANNOT change these)

```java
// Library: PayPal
public class PayPalAPI {
    public void authenticate() {
        System.out.println("Authenticated with PayPal");
    }

    public void sendPayment(double amount) {
        System.out.println("Paid $" + amount + " using PayPal");
    }
}

// Library: Stripe
public class StripeSDK {
    public void makePayment(int cents) {
        System.out.println("Paid $" + cents / 100.0 + " using Stripe");
    }
}
```

---

## 🧠 Your Task:

* Implement the complete solution using the **Adapter Pattern**.
* Use proper abstraction, OOP principles, and separation of concerns.

---

Would you like me to check your solution after you try it, or would you prefer to go step by step together?
