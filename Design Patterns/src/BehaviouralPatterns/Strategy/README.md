# 🧠 Strategy Design Pattern – Full Explanation

### 🔍 **Intent**:

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it."*

---

## 🏠 Real-World Analogy

### 🔧 Payment Gateway Example:

Imagine an **e-commerce checkout system**.
You want to support multiple payment options:

* Credit Card
* PayPal
* UPI

Instead of hardcoding `if-else` everywhere, you define a **strategy for each payment type** and just **plug it in at runtime**.

> The **strategy pattern decouples the payment method logic from the checkout system.**

---

## 🧱 Problem Before Strategy

* When multiple behaviors (like sorting, compression, payment, etc.) are needed, developers often write long `if-else` or `switch-case` logic.
* Adding a new behavior means modifying existing code → violates the **Open/Closed Principle**.
* Clients are tightly coupled to specific algorithm implementations.

---

## ✅ Strategy Pattern to the Rescue

* Define an **interface** for a family of behaviors.
* Implement multiple **concrete strategies**.
* In the **context** class, use the strategy polymorphically (can be changed at runtime).

---

## 🧱 Participants

| Role                 | Description                                       |
| -------------------- | ------------------------------------------------- |
| **Strategy**         | Common interface for all supported algorithms.    |
| **ConcreteStrategy** | Each class implements a different algorithm.      |
| **Context**          | Uses a `Strategy` to delegate the algorithm call. |

---

## 🧑‍💻 Java Example – Dynamic Payment Strategy

---

### ✅ 1. Strategy Interface

```java
interface PaymentStrategy {
    void pay(double amount);
}
```

---

### ✅ 2. Concrete Strategies

```java
class CreditCardPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid ₹" + amount + " using Credit Card.");
    }
}

class PayPalPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid ₹" + amount + " using PayPal.");
    }
}

class UpiPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid ₹" + amount + " using UPI.");
    }
}
```

---

### ✅ 3. Context Class (Checkout)

```java
class ShoppingCart {
    private PaymentStrategy paymentStrategy;

    // Inject strategy at runtime
    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.paymentStrategy = strategy;
    }

    public void checkout(double amount) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("No payment strategy selected.");
        }
        paymentStrategy.pay(amount);
    }
}
```

---

### ✅ 4. Demo

```java
public class StrategyPatternDemo {
    public static void main(String[] args) {
        ShoppingCart cart = new ShoppingCart();

        cart.setPaymentStrategy(new CreditCardPayment());
        cart.checkout(1500.0);

        cart.setPaymentStrategy(new UpiPayment());
        cart.checkout(450.0);

        cart.setPaymentStrategy(new PayPalPayment());
        cart.checkout(999.0);
    }
}
```

---

### ✅ Output:

```
Paid ₹1500.0 using Credit Card.
Paid ₹450.0 using UPI.
Paid ₹999.0 using PayPal.
```

---

## ✅ When to Use Strategy Pattern

* When you have many related classes that only differ in **behavior or algorithm**.
* You want to **switch behavior at runtime**.
* You want to eliminate large `if-else` or `switch` blocks.
* You want to **decouple the algorithm** from the class that uses it.

---

## 🚫 Avoid When

* You only have one algorithm (no variation).
* Runtime switching is unnecessary and adds complexity.

---