# 🛫 Problem: **Air Traffic Control System (ATC)**

### 🧩 Scenario:

You're building a simulation for an **Air Traffic Control Tower (ATC)**.
Multiple **airplanes** want to take off or land, but **only one plane can use the runway at a time**.

You need to build a **mediator-based system** where:

### ✈️ Planes:

* Don't talk to each other directly.
* Instead, they **ask the ATC** for permission to **land or take off**.
* They wait if the runway is **occupied**.
* When one plane finishes landing or taking off, **ATC notifies** others.

---

## ✅ Functional Requirements:

1. Create a `Mediator` interface, e.g., `AirTrafficControlTower`, with methods like:

    * `requestToLand(Airplane)`
    * `requestToTakeOff(Airplane)`
    * `notifyRunwayClear()`

2. Create concrete `AirTrafficControl` that:

    * Tracks the **runway status** (free/busy).
    * Maintains a **queue of waiting planes**.
    * Notifies planes when they are cleared.

3. Planes (`Airplane` class) should:

    * Request to land or take off via the ATC.
    * Receive messages from ATC (e.g., “You are cleared to land”).

---

## 🎯 Output Example:

```
Plane A requests to land.
ATC: Runway is free. Plane A is cleared to land.

Plane B requests to land.
ATC: Runway is busy. Plane B, please wait.

Plane A has landed.
ATC: Runway now clear. Plane B is cleared to land.
```

---
