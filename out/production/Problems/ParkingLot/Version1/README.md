## 🚗 Low-Level Design Problem: Single-Floor Parking Lot System with One Entrance and Exit

### 📌 **Problem Statement**

Design and implement a **Parking Lot System** for a **single-floor** parking area that has exactly **one entrance** and **one exit**. The system must manage parking operations for **multiple vehicle types**, handle **spot allocation** at the entrance, issue **entry tickets**, and support **fee calculation** and **payment** at the exit based on the duration of parking.

---

### ✅ Functional Requirements

1. The parking lot supports multiple **vehicle types**, such as:

    * Car, Bike, Truck, Electric Vehicle (EV), etc.
2. The system must have exactly:

    * **One Entrance Gate**: manages vehicle entry, allocates spots, and issues tickets.
    * **One Exit Gate**: manages vehicle exit, calculates fee, and processes payment.
3. Each **parking spot** is assigned a **vehicle type** (e.g., Spot S1 → Car).
4. When a vehicle **enters**:

    * The system finds the **first available spot** matching its vehicle type.
    * Allocates the spot and marks it as occupied.
    * Issues a **ticket** containing:

        * Vehicle details
        * Spot ID
        * Entry time
        * Unique ticket ID
5. When a vehicle **exits**:

    * The system verifies the ticket.
    * Calculates the **parking fee** based on duration and vehicle type.
    * A **minimum of 1 hour** is charged.
    * The user can pay via **UPI** or **Card**.
    * The spot is marked as **available again**.

---

### ✅ Design Constraints

* The system should follow **Object-Oriented Design principles**.
* It must follow the **Open-Closed Principle (OCP)**: adding a new vehicle type must not require modifying existing logic.
* The parking lot should be implemented as a **Singleton** — only one instance should exist.
* Separate classes should be used for **EntranceGate** and **ExitGate** to ensure modularity and SRP.

---

### 🧱 Key Components

| Component        | Responsibility                                        |
| ---------------- | ----------------------------------------------------- |
| `ParkingLot`     | Manages spots, ticket registry, and lifecycle         |
| `Vehicle`        | Represents an incoming vehicle with a `VehicleType`   |
| `VehicleType`    | Interface used for dynamic pricing (OCP compliant)    |
| `Spot`           | Represents a parking spot for a specific vehicle type |
| `Ticket`         | Issued on entry, used on exit for fee calculation     |
| `EntranceGate`   | Handles vehicle entry and ticket generation           |
| `ExitGate`       | Handles fee calculation and payment                   |
| `PaymentService` | Simulates UPI or Card payment processing              |

---

### 💰 Sample Pricing Table

| Vehicle Type | Hourly Rate (₹) |
| ------------ | --------------- |
| Bike         | 10              |
| Car          | 20              |
| Truck        | 50              |
| EV Car       | 15              |

---

### 📤 Sample Workflow

1. A `Car` enters the parking lot:

    * Assigned to `Spot S1`
    * Ticket issued: `ID123`, Entry: 10:00 AM
2. A `Bike` enters:

    * Assigned to `Spot S2`, ticket issued
3. At 12:00 PM, Car exits:

    * Fee calculated: ₹40 (2 hours × ₹20/hour)
    * Payment made via Card
    * Spot S1 becomes available again

---

### 🔍 Sample Output

```
Ticket Issued: TCK123 for Spot: S1
Ticket Issued: TCK124 for Spot: S2
Total Fee for 2 hour(s): ₹40.0
Paid ₹40.0 via CARD
```

---

### 🌱 Extension Ideas (Future Enhancements)

* Multi-floor support with elevators
* Entry/Exit logs and analytics
* QR-based digital ticketing
* Reservation system
* Support for multiple concurrent entrance/exit gates

---

Let me know if you'd like a **PDF**, **UML Diagram**, or turn this into an **LLD practice template** for interviews!
