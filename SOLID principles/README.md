# 🧠 SOLID Principles Cheatsheet (Java + Real-world Analogies)

SOLID is an acronym representing five design principles for writing maintainable and scalable object-oriented software.

## 🟨 S – Single Responsibility Principle (SRP)

**Definition:**  
A class should have only one reason to change, meaning it should have only one job or responsibility.

**Why:**  
Keeps classes focused and easier to test, debug, and maintain.

**Real-world Analogy:**  
A chef only cooks food. A waiter only serves. Don’t make your chef take orders and serve food.

**Java Example:**
```java
// BAD
class Invoice {
    void calculateTotal() {}
    void printInvoice() {}
    void saveToDatabase() {}
}

// GOOD
class Invoice {
    void calculateTotal() {}
}

class InvoicePrinter {
    void print(Invoice invoice) {}
}

class InvoiceRepository {
    void save(Invoice invoice) {}
}
```

## 🟩 O – Open/Closed Principle (OCP)

**Definition:**  
Software entities (classes, modules, functions) should be open for extension but closed for modification.

**Why:**  
Allows behavior changes without touching existing, stable code.

**Real-world Analogy:**  
A plug point lets you attach different devices without changing the socket.

**Java Example using Interface:**
```java
interface Notification {
    void send(String message);
}

class EmailNotification implements Notification {
    public void send(String message) {
        System.out.println("Sending Email: " + message);
    }
}

class SMSNotification implements Notification {
    public void send(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

class Notifier {
    public void notifyUser(Notification notification, String msg) {
        notification.send(msg);
    }
}
```


## 🟦 L – Liskov Substitution Principle (LSP)

**Definition:**  
Objects of a superclass should be replaceable with objects of its subclasses without affecting the correctness of the program.

**Why:**  
Ensures that objects of a superclass can be used interchangeably with objects of its subclasses without breaking the program.Promotes reliable inheritance.

**Real-world Analogy:**  
A square *is a* rectangle, but setting height ≠ width breaks logic in some designs. Be careful with inheritance!

**Java Example:**
```java
class Shape {
    void draw() {}
}

class Rectangle extends Shape {
    void draw() {}
}

class Square extends Rectangle {
    void draw() {}
}
```

## 🟪 I – Interface Segregation Principle (ISP)

**Definition:**  
A client should not be forced to depend on methods it does not use.

**Why:**  
Separates interfaces, making them easier to implement and maintain. Reduces side effects from changes and bloated interfaces.

**Real-world Analogy:**  
A printer shouldn't be forced to have a scan() method if it can't scan.

**Java Example:**
```java
interface Printer {
    void print();
    void scan();
    void fax();
}

class MultiFunctionPrinter implements Printer {
    public void print() {}
    public void scan() {}
    public void fax() {}
}
```

## 🟧 D – Dependency Inversion Principle (DIP)

**Definition:**  
High-level modules should not depend on low-level modules. Both should depend on abstractions.

**Why:**  
Encourages loose coupling, promotes modularity, and makes code more testable and maintainable. Promotes decoupling, making the code more flexible and testable.

**Real-world Analogy:**
A remote control shouldn't depend on the specific TV brand.

**Java Example:**
```java
interface RemoteControl {
    void turnOn();
    void turnOff();
}

class TV implements RemoteControl {
    public void turnOn() {}
    public void turnOff() {}
}

class Radio implements RemoteControl {
    public void turnOn() {}
    public void turnOff() {}
}
```
**Another Example:**
```java
// Abstraction
interface Keyboard {
    void type();
}

// Low-level class
class MechanicalKeyboard implements Keyboard {
    public void type() {
        System.out.println("Typing on mechanical keyboard...");
    }
}

// High-level module
class Computer {
    private Keyboard keyboard;

    public Computer(Keyboard keyboard) {
        this.keyboard = keyboard;
    }

    void use() {
        keyboard.type();
    }
}
```

## 🔁 Summary Table

| Principle | Responsibility                     | Key Benefit                        |
|-----------|-------------------------------------|-------------------------------------|
| SRP       | One class = One responsibility      | Better cohesion, easier to maintain |
| OCP       | Extend without modify               | Easy to add features safely         |
| LSP       | Subclasses should behave like base  | Reliable polymorphism               |
| ISP       | Use focused interfaces              | Avoids unused methods               |
| DIP       | Depend on abstractions              | Flexibility and decoupling          |

---

## ✅ Tips for Applying SOLID in Java Projects

- Use **interfaces** wherever possible for OCP, DIP, and ISP.
- Use **composition over inheritance** to avoid violating LSP.
- Structure your code into **layers**: service, repository, controller.
- Apply **SRP** first, as it often leads to better application of other principles.
- Use **Spring Framework** and **Spring Boot** — they naturally encourage SOLID via Dependency Injection and annotations.

---

🧑‍💻 **Pro Tip:**  
Mastering SOLID is a step toward becoming a design-oriented developer. Practice with real-world scenarios and refactor existing code to align with SOLID.
