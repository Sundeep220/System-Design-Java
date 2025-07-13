## 🚗 Updated Problem Statement: Multi-Gate Smart Parking Lot System

### 📌 **Scenario**

Design a **smart parking lot system** for a single building floor with:

* 2️⃣ Entrances
* 2️⃣ Exits
* Multiple **spot types**: `Compact`, `Large`, `Bike`, `Handicapped`
* Multiple **payment methods**: `UPI`, `Card`, `Cash`
* A **monitoring system** to track lot usage
* Spots must be allocated based on **proximity to entrance**
* System should follow **Design Patterns** to ensure minimal changes when adding:

    * A new vehicle type
    * A new spot type
    * A new payment method

---

## ✅ Functional Requirements

1. **Entry Gates**:

    * Each entrance gate is identified by a location (e.g., Gate A, Gate B).
    * When a vehicle enters, assign it the **nearest available compatible spot** to that entrance.

2. **Exit Gates**:

    * Accept tickets for exiting vehicles.
    * Calculate time spent.
    * Display fees.
    * Accept payment via **UPI**, **Card**, or **Cash**.

3. **Vehicle and Spot Types**:

    * Vehicles: Car, Bike, Truck, EV, etc.
    * Spots: Compact, Large, Bike, Handicapped
    * Each vehicle type can only fit into certain types of spots.

4. **Payment**:

    * Supports multiple methods using **Strategy Pattern**.
    * Should be easily extendable to add new methods like Wallet or NetBanking.

5. **Monitoring System**:

    * Track:

        * Total vehicles parked
        * Available spots by type
        * Total revenue collected

6. **Scalability**:

    * Adding new vehicle/spot/payment types should require **zero changes to existing classes**.
    * Use OOP + SOLID + Design Patterns.

---

## ✅ Design Patterns to Use

| Pattern               | Purpose                                           |
| --------------------- | ------------------------------------------------- |
| **Strategy**          | Payment handling — add new payment methods easily |
| **Factory**           | Create Spot or VehicleType dynamically            |
| **Singleton**         | Central ParkingLot manager                        |
| **Observer**          | Monitoring system listens for parking events      |
| **Open-Closed (OCP)** | Extensible entities without modifying logic       |

---

## 🧱 Class Breakdown (High-Level Overview)

### 🔹 Core Entities

* `Vehicle` → contains `VehicleType`
* `VehicleType` (interface) → e.g., `Car`, `Bike`, `Truck`
* `Spot` → abstract base class with concrete types: `CompactSpot`, `LargeSpot`, etc.
* `EntranceGate`, `ExitGate` → handle entry/exit by location

### 🔹 Parking Logic

* `ParkingLot` (Singleton) → holds map of spots and their locations
* `SpotAllocator` (Strategy) → chooses nearest spot for vehicle
* `MonitoringSystem` (Observer) → listens for entry/exit events
* `Ticket` → entry time, vehicle, spot, gate

### 🔹 Payment

* `PaymentService` → interface

    * `UPIPayment`, `CardPayment`, `CashPayment` (strategy implementations)
* `PaymentProcessor` → uses `PaymentService` strategy

---

## 🧠 Next Step: Architecture First, Then Code

Would you like me to:

1. **Draw class diagram and explain interactions first?**
2. Or jump straight into **code scaffolding** — building each class file-by-file with explanations?

This is a multi-part problem. I recommend doing it step-by-step so you **fully understand each part** and can extend it yourself later. Let me know how you’d like to proceed.
