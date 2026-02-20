# 🧾 Design a Vending Machine (Low-Level Design)

## 📌 Problem Statement

Design a vending machine system that allows users to select multiple products, make online payments, and receive the selected products upon successful payment.

The system should handle concurrent transactions safely and provide administrative controls for managing inventory.

---

## ✅ Functional Requirements

### 🛒 Product & Cart Management

* The vending machine should support **multiple products**.
* Each product should have:

    * Unique ID
    * Name
    * Price
* The machine should maintain **product quantities (stock)**.
* Users should be able to:

    * Select multiple products
    * Add products to a cart
    * Specify quantity per product
* Total amount should be calculated at checkout.

---

### 💳 Payment

* The vending machine should support **online payments only**.
* Payment should follow the **Strategy Pattern**, allowing different payment methods such as:

    * UPI
    * Card
* Payment processing should:

    * Return success or failure
    * Provide a payment identifier
* Products should be dispensed **only after successful payment**.

---

### 📦 Inventory Management

* The machine should:

    * Track available product quantities
    * Prevent negative stock
    * Ensure atomic deduction for multi-product carts
* The system must avoid:

    * Partial stock deduction
    * Double-selling under concurrent transactions

---

### 🔄 Transaction Management

* Each checkout attempt should create a **Transaction**.
* A transaction should maintain:

    * Transaction ID
    * List of purchased items
    * Total amount
    * Status
    * Timestamp
* Transaction statuses should include:

    * INITIATED
    * PAYMENT_SUCCESS
    * PAYMENT_FAILED
    * FAILED
    * COMPLETED

---

### ⚙️ Concurrency

* The system should handle **multiple transactions concurrently**.
* It must:

    * Ensure data consistency
    * Avoid race conditions
    * Prevent deadlocks
    * Guarantee atomic stock deduction for multi-product checkout

---

### 👨‍💼 Admin Capabilities

The system should allow administrators to:

* Add new products
* Restock existing products
* Remove products from inventory

---

### 🏗 Design Constraints

* The vending machine should be implemented as a **Singleton**.
* The design should follow:

    * Single Responsibility Principle (SRP)
    * Open/Closed Principle (OCP)
    * Strategy Pattern for payments
* Inventory deduction for multiple products should be **atomic and deadlock-safe**.

---
