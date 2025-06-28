# 🚦 Problem: **Travel Route Planner with Transport Strategies**

### 📘 Scenario:

You’re building a **TravelRoutePlanner** app that calculates how long it takes to travel between two cities using different **transport strategies**:

* 🚗 Car
* 🚆 Train
* ✈️ Flight

Each mode has a different **speed** and potentially unique **route calculation logic**.

---

### 🧱 Requirements

* Define a `TravelStrategy` interface with a method: `calculateTime(distanceInKm)`
* Implement concrete strategies for:

    * `CarTravelStrategy` (average speed: 60 km/h)
    * `TrainTravelStrategy` (average speed: 120 km/h)
    * `FlightTravelStrategy` (average speed: 800 km/h)
* Create a `TravelPlanner` class that takes a `TravelStrategy` and delegates the time calculation.
* Allow the strategy to be swapped dynamically at runtime.

---

## 🧑‍💻 Expected Demo Usage

```java
TravelPlanner planner = new TravelPlanner();

planner.setStrategy(new CarTravelStrategy());
planner.planTrip(300); // Output: Estimated travel time by Car: 5.0 hrs

planner.setStrategy(new TrainTravelStrategy());
planner.planTrip(300); // Output: Estimated travel time by Train: 2.5 hrs

planner.setStrategy(new FlightTravelStrategy());
planner.planTrip(300); // Output: Estimated travel time by Flight: 0.375 hrs
```

---