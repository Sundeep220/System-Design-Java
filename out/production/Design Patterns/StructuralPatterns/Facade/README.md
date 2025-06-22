# 🏛️ **Facade Design Pattern**

---

### 📖 **Intent:**
> Facade Pattern — one of the most practical and frequently used patterns in real-world systems, especially in microservices, libraries, and APIs.
> Provide a **unified, simplified interface** to a set of interfaces in a subsystem.
> It **hides the complexity** of the subsystem from the client.

---

## 🧠 Why Use It?

* To **simplify access** to complex systems (e.g., subsystems, frameworks, APIs).
* To **shield clients** from inner complexity or low-level logic.
* To improve **readability, usability, and maintainability** of code.
* Great for creating **entry points in libraries**, **microservice aggregators**, or **starter wrappers**.

---

## 💡 Real-World Analogy

### 🎥 **Home Theater System**

A home theater includes:

* Projector
* Sound System
* DVD Player
* Screen

Using each separately is complex. Instead, you use a **remote (Facade)** that has one button: `startMovie()`, which internally turns everything on in sequence.

---

## 👨‍💻 Java Example: **Home Theater System Facade**

---

### ✅ Step 1: Subsystem Classes

```java
public class Projector {
    public void on() { System.out.println("Projector turned ON"); }
    public void off() { System.out.println("Projector turned OFF"); }
}
```

```java
public class SoundSystem {
    public void on() { System.out.println("Sound system ON"); }
    public void setVolume(int level) {
        System.out.println("Volume set to " + level);
    }
    public void off() { System.out.println("Sound system OFF"); }
}
```

```java
public class DVDPlayer {
    public void on() { System.out.println("DVD Player ON"); }
    public void play(String movie) {
        System.out.println("Playing movie: " + movie);
    }
    public void off() { System.out.println("DVD Player OFF"); }
}
```

```java
public class Screen {
    public void down() { System.out.println("Screen going down"); }
    public void up() { System.out.println("Screen going up"); }
}
```

---

### ✅ Step 2: Facade Class

```java
public class HomeTheaterFacade {
    private Projector projector;
    private SoundSystem soundSystem;
    private DVDPlayer dvdPlayer;
    private Screen screen;

    public HomeTheaterFacade(Projector projector, SoundSystem soundSystem,
                             DVDPlayer dvdPlayer, Screen screen) {
        this.projector = projector;
        this.soundSystem = soundSystem;
        this.dvdPlayer = dvdPlayer;
        this.screen = screen;
    }

    public void startMovie(String movie) {
        System.out.println("\nStarting movie night...");
        screen.down();
        projector.on();
        soundSystem.on();
        soundSystem.setVolume(10);
        dvdPlayer.on();
        dvdPlayer.play(movie);
    }

    public void endMovie() {
        System.out.println("\nShutting down movie...");
        dvdPlayer.off();
        soundSystem.off();
        projector.off();
        screen.up();
    }
}
```

---

### ✅ Step 3: Client Code

```java
public class Main {
    public static void main(String[] args) {
        // Create subsystem components
        Projector projector = new Projector();
        SoundSystem sound = new SoundSystem();
        DVDPlayer dvd = new DVDPlayer();
        Screen screen = new Screen();

        // Create the facade
        HomeTheaterFacade theater = new HomeTheaterFacade(projector, sound, dvd, screen);

        // Use simple methods
        theater.startMovie("The Matrix");
        theater.endMovie();
    }
}
```

---

### ✅ Output

```
Starting movie night...
Screen going down
Projector turned ON
Sound system ON
Volume set to 10
DVD Player ON
Playing movie: The Matrix

Shutting down movie...
DVD Player OFF
Sound system OFF
Projector turned OFF
Screen going up
```

---

## ✅ Benefits of Facade Pattern

| Feature                  | Benefit                                               |
| ------------------------ | ----------------------------------------------------- |
| Simplifies subsystem     | One unified interface to multiple internal components |
| Reduces coupling         | Client doesn’t know how subsystem components work     |
| Great entry-point        | Commonly used in libraries, frameworks, microservices |
| Improves maintainability | Internal logic changes don't affect the client        |
| Centralized control      | One place to manage complex subsystems                |

---

## 🧩 Real-World Use Cases

| Domain         | Facade                   | Subsystems                             |
| -------------- | ------------------------ | -------------------------------------- |
| Spring Boot    | `@SpringBootApplication` | AutoConfig, ComponentScan, etc.        |
| AWS SDK        | `AmazonS3Client`         | REST, Auth, Headers, Requests, Parsers |
| Kafka Producer | `KafkaTemplate`          | Producer configs, serializers, brokers |
| Travel Booking | BookingFacade            | FlightService, HotelService, CarRental |

