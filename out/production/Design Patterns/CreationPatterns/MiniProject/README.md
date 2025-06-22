# 🚗 Mini Project: Vehicle Factory System

## 📅 Date: June 22, 2025

### 🎯 Objective
Design a **Vehicle Factory System** using **all Creational Design Patterns** in Java:

- 🔁 Singleton
- 🏭 Factory
- 🧬 Abstract Factory
- 🧱 Builder
- 🧪 Prototype

---

## 🧩 Description

You are building a vehicle manufacturing system that allows flexible creation, configuration, and cloning of different types of vehicles like **Car**, **Bike**, and **Truck**. The system must be designed using appropriate Creational Design Patterns.

---

## 🔧 Pattern Usage

| Pattern            | Role in the Project                                               |
|--------------------|--------------------------------------------------------------------|
| **Singleton**      | Used in `VehicleRegistry` to provide a global access point         |
| **Factory**        | Used in `VehicleFactory` to create basic vehicle instances         |
| **Abstract Factory** | Used to create families of vehicles like Electric, Petrol, Diesel |
| **Builder**        | Used to construct complex vehicle objects step by step             |
| **Prototype**      | Used to clone pre-configured vehicle templates                     |

---

## 🏗️ Components

### Interface: `Vehicle`
```java
interface Vehicle extends Cloneable {
    void drive();
    Vehicle clone();
}
```

### Classes:
- `Car`, `Bike`, `Truck` – implements `Vehicle`, supports deep cloning
- `VehicleBuilder` – builds customized vehicles with color, engine, seats, etc.
- `VehicleFactory` – factory to return basic type instances
- `VehicleFamilyFactory` – abstract factory to create vehicle families
- `VehicleRegistry` – singleton + prototype-based registry

---

## 🧪 Sample Usage

```java
// Using Factory
Vehicle bike = VehicleFactory.createVehicle("bike");
bike.drive();

// Using Builder
Vehicle car = new VehicleBuilder("Car")
    .setColor("Red")
    .setSeats(5)
    .setEngine("V8")
    .build();
car.drive();

// Using Abstract Factory
VehicleFamilyFactory carFactory = new CarFactory();
Vehicle electricCar = carFactory.createElectric();
electricCar.drive();

// Using Prototype
Vehicle sedanBase = new Car("Sedan", "Black", "V6", 4);
VehicleRegistry.getInstance().register("sedan", sedanBase);
Vehicle clonedSedan = VehicleRegistry.getInstance().getClone("sedan");
clonedSedan.drive();
```

---

## ✅ Goal

This project should help you solidify your understanding of all **Creational Design Patterns** by applying them to a meaningful real-world system.

---
