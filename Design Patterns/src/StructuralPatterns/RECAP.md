## 📦 Structural Design Patterns Recap

Structural design patterns help compose objects into larger structures and provide new functionality without altering the internal logic. They are especially useful for adapting, simplifying, and optimizing class relationships.

---

### ✅ 1. Adapter Pattern

* **Intent:** Convert the interface of one class into another that clients expect.
* **Real-World Analogy:** Power plug adapter
* **Use Case:** Adapting different notification systems (Email, SMS) to a common `Notifier` interface.
* **Key Concept:** Compatibility without changing existing code.

---

### ✅ 2. Decorator Pattern

* **Intent:** Add behavior to individual objects dynamically without affecting others.
* **Real-World Analogy:** Adding toppings to a pizza or features to a notification (like email logging, SMS retrying)
* **Use Case:** Enhancing `Notification` service with logging and retry.
* **Key Concept:** Wrapping classes recursively to enhance behavior.

---

### ✅ 3. Proxy Pattern

* **Intent:** Provide a surrogate or placeholder to control access to another object.
* **Real-World Analogy:** Credit card as a proxy to your bank account.
* **Use Case:** `VideoDownloaderProxy` that caches downloaded videos.
* **Key Concept:** Control access (e.g., caching, logging, security).

---

### ✅ 4. Composite Pattern

* **Intent:** Compose objects into tree structures and treat individual objects and compositions uniformly.
* **Real-World Analogy:** File system with folders and files.
* **Use Case:** Notification groups sending messages to multiple channels.
* **Key Concept:** Recursive structure with base `Component` interface.

---

### ✅ 5. Bridge Pattern

* **Intent:** Decouple abstraction from implementation so they can vary independently.
* **Real-World Analogy:** Remote (abstraction) controlling devices (implementation)
* **Use Case:** Report generation system separating ReportType (Summary, Detailed) from OutputFormat (PDF, HTML).
* **Key Concept:** Composition instead of inheritance for cross-dimension variation.

---

### ✅ 6. Facade Pattern

* **Intent:** Provide a simplified interface to a complex subsystem.
* **Real-World Analogy:** Universal remote to control your home theater.
* **Use Case:** TravelBooking system using `FlightService`, `HotelService`, `CarRentalService`, `PaymentService`, and `ItineraryService`.
* **Key Concept:** Hides complexity, improves readability.

---

### ✅ 7. Flyweight Pattern

* **Intent:** Use sharing to support large numbers of similar objects efficiently.
* **Real-World Analogy:** Character formatting in MS Word shares style.
* **Use Case:** Map system that renders 3000+ markers but only 3 shared `MarkerStyle` objects.
* **Key Concept:** Split intrinsic (shared) and extrinsic (unique) state to optimize memory.

---

## ✅ Summary Table

| Pattern   | Key Concept                       | Example                         |
| --------- | --------------------------------- | ------------------------------- |
| Adapter   | Interface compatibility           | SMS/Email notifier              |
| Decorator | Add dynamic behavior              | Logging/retry for notifications |
| Proxy     | Controlled access (e.g., caching) | Cached video downloader         |
| Composite | Tree-like structure               | Nested notifications            |
| Bridge    | Decoupling via composition        | Report format vs report type    |
| Facade    | Simplified unified API            | Travel booking + payment        |
| Flyweight | Shared state optimization         | Map marker rendering            |

---
