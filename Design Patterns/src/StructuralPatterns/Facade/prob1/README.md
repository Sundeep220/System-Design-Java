Perfect! Let's dive into an **advanced Facade Pattern assignment** to solidify your understanding through a practical, real-world scenario.

---

## 💼 Advanced Problem: **Travel Booking Facade System**

---

### 🧩 Problem Statement:

You’re building a **Travel Booking System**. Users want to book a **complete travel package** through a **single call** instead of manually interacting with:

* **FlightService**
* **HotelService**
* **CarRentalService**

Each of these subsystems has its own complex interface and rules.

Your job is to create a **TravelFacade** that simplifies this process for the client.

---

## ✅ Requirements

### Subsystems:

Each subsystem should have its own method for booking and cancellation:

* `FlightService.bookFlight(from, to)`
* `HotelService.bookHotel(location)`
* `CarRentalService.bookCar(location)`

---

### Facade Responsibilities:

* Method: `bookTrip(String from, String to)`

    * Books flight, hotel at destination, and car at destination.
* Method: `cancelTrip()`

    * Cancels car, hotel, and flight in reverse order.

---

## 🔧 Let's Implement It Step-by-Step

---

### ✅ 1. Subsystems

```java
public class FlightService {
    public void bookFlight(String from, String to) {
        System.out.println("Flight booked from " + from + " to " + to);
    }

    public void cancelFlight() {
        System.out.println("Flight booking cancelled");
    }
}
```

```java
public class HotelService {
    public void bookHotel(String location) {
        System.out.println("Hotel booked at " + location);
    }

    public void cancelHotel() {
        System.out.println("Hotel booking cancelled");
    }
}
```

```java
public class CarRentalService {
    public void bookCar(String location) {
        System.out.println("Car booked at " + location);
    }

    public void cancelCar() {
        System.out.println("Car booking cancelled");
    }
}
```

---

### ✅ 2. Travel Facade

```java
public class TravelFacade {
    private FlightService flightService;
    private HotelService hotelService;
    private CarRentalService carService;

    public TravelFacade() {
        this.flightService = new FlightService();
        this.hotelService = new HotelService();
        this.carService = new CarRentalService();
    }

    public void bookTrip(String from, String to) {
        System.out.println("\nBooking complete travel package...");
        flightService.bookFlight(from, to);
        hotelService.bookHotel(to);
        carService.bookCar(to);
        System.out.println("Trip booking successful!\n");
    }

    public void cancelTrip() {
        System.out.println("Cancelling entire trip...");
        carService.cancelCar();
        hotelService.cancelHotel();
        flightService.cancelFlight();
        System.out.println("Trip cancellation successful.\n");
    }
}
```

---

### ✅ 3. Client Code

```java
public class Main {
    public static void main(String[] args) {
        TravelFacade travelFacade = new TravelFacade();

        travelFacade.bookTrip("Delhi", "Goa");
        travelFacade.cancelTrip();
    }
}
```

---

### ✅ Output

```
Booking complete travel package...
Flight booked from Delhi to Goa
Hotel booked at Goa
Car booked at Goa
Trip booking successful!

Cancelling entire trip...
Car booking cancelled
Hotel booking cancelled
Flight booking cancelled
Trip cancellation successful.
```

---

## ✅ What You Practiced:

| Concept            | Applied As                                 |
| ------------------ | ------------------------------------------ |
| **Facade Pattern** | Unified interface to 3 subsystems          |
| **SRP**            | Each service class handles its own logic   |
| **Abstraction**    | The client sees only one high-level method |

---