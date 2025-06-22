## 🏗️ Creational Design Patterns Recap

Creational patterns deal with **object creation mechanisms**, aiming to create objects in a manner suitable to the situation. They help make a system independent of how its objects are created, composed, and represented.

---

### ✅ 1. Singleton Pattern

* **Intent:** Ensure a class has only one instance and provide a global access point.
* **Real-World Analogy:** Government (only one prime minister at a time)
* **Implementation Types:** Eager, Lazy, Thread-safe (double-checked locking)
* **Use Case:** ConfigurationManager, Logger
* **Key Concept:** Control instantiation.

---

### ✅ 2. Factory Method Pattern

* **Intent:** Define an interface for creating an object but let subclasses decide which class to instantiate.
* **Real-World Analogy:** Hiring through a recruitment agency
* **Use Case:** NotificationFactory to create Email/SMS/Push notifications
* **Key Concept:** Delegates object creation to subclasses.

---

### ✅ 3. Abstract Factory Pattern

* **Intent:** Provide an interface for creating families of related or dependent objects without specifying their concrete classes.
* **Real-World Analogy:** Car manufacturing plant producing engine + wheels + body for electric or petrol cars
* **Use Case:** UIFactory creating Buttons and Checkboxes for Windows vs Mac
* **Key Concept:** Multiple related factories under one abstraction.

---

### ✅ 4. Builder Pattern

* **Intent:** Separate the construction of a complex object from its representation.
* **Real-World Analogy:** MealBuilder customizing a burger (bun + patty + sauce + extras)
* **Use Case:** Building `User` object with optional fields (name, age, email)
* **Key Concept:** Step-by-step construction of immutable objects.

---

### ✅ 5. Prototype Pattern

* **Intent:** Create new objects by copying an existing object (prototype).
* **Real-World Analogy:** Document templates or object cloning
* **Use Case:** Deep/cloneable Document system with a Registry
* **Key Concept:** Object cloning (shallow vs deep)

---

## 🧪 Mini Project: Vehicle Factory System (Combined Creational Patterns)

* ✅ Used **Singleton** for Logging System
* ✅ **Factory + Abstract Factory** for building Cars, Bikes, Scooters
* ✅ **Builder** for configuring vehicles with different specs
* ✅ **Prototype** for vehicle cloning and versioning
* ✅ Real-world features: versioning, caching, pricing logic, and logging

---

## ✅ Summary Table

| Pattern          | Key Concept                  | Example                          |
| ---------------- | ---------------------------- | -------------------------------- |
| Singleton        | One instance only            | Logger, Configuration            |
| Factory Method   | Subclass decides product     | Notification creation            |
| Abstract Factory | Grouped factories            | Cross-platform UI widget factory |
| Builder          | Step-by-step object creation | UserBuilder, PizzaBuilder        |
| Prototype        | Clone from prototype         | Document templates, Car registry |

