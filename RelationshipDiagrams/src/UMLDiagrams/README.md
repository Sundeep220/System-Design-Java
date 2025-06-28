## UML and Class Diagrams

### ✅ UML (Unified Modeling Language)

UML is a standardized **visual modeling language** used in software engineering to:

* Visualize and design system architecture.
* Describe system behavior and interactions.
* Serve as a blueprint for developers.

### 📘 Types of UML Diagrams

#### Structural Diagrams

* **Class Diagram**
* Object Diagram
* Component Diagram
* Deployment Diagram

#### Behavioral Diagrams

* Use Case Diagram
* Sequence Diagram
* Activity Diagram
* State Diagram

---

## ✅ Class Diagrams

A **Class Diagram** shows:

* Classes
* Attributes (fields)
* Methods (operations)
* Relationships between classes

### 📦 Class Diagram Example: Shopping System

```
+-------------------+           +------------------+
|     Product       |           |   ShoppingCart   |
+-------------------+           +------------------+
| - id: int         |           | - items: List<>  |
| - name: String    |<--------->|                  |
| - price: double   |           +------------------+
+-------------------+           | +addProduct()    |
| +getDetails()     |           | +removeProduct() |
+-------------------+           +------------------+

             ▲
             |
             |
   +-------------------+
   |  Electronics       |
   +-------------------+
   | - warranty: int    |
   +-------------------+
   | +getWarrantyInfo() |
   +-------------------+
```

---

## 🔁 Relationships in Class Diagrams

### 1. Association

**Definition**: A generic "has-a" relationship. Both classes can exist independently.

**Example**:

```java
class Teacher {}
class School {
    List<Teacher> teachers;
}
```

**UML**:

```
Teacher  -------------------  School
```

---

### 2. Inheritance (Generalization)

**Definition**: "Is-a" relationship. Subclass inherits attributes and behaviors from a superclass.

**Example**:

```java
class Animal {
    void makeSound() {}
}
class Dog extends Animal {
    void makeSound() { System.out.println("Bark"); }
}
```

**UML**:

```
       Animal
         ▲
         |
        Dog
```

---

### 3. Aggregation

**Definition**: "Whole-part" relationship. Parts can exist independently of the whole.

**Example**:

```java
class Professor {}
class Department {
    List<Professor> professors;
}
```

**UML**:

```
Department ◇--------- Professor
```

---

### 4. Composition

**Definition**: Strong whole-part relationship. Parts cannot exist without the whole.

**Example**:

```java
class Room {}
class House {
    List<Room> rooms = new ArrayList<>();
}
```

**UML**:

```
House ◆--------- Room
```

---

### 5. Dependency

**Definition**: A class uses another class temporarily (e.g., method parameters).

**Example**:

```java
class Car {}
class CarService {
    void repair(Car car) {}
}
```

**UML**:

```
CarService ---> Car
```

---

## 📊 Summary Table

| Relationship | Meaning           | Symbol     | Object Lifecycle | Real-World Example       |
| ------------ | ----------------- | ---------- | ---------------- | ------------------------ |
| Association  | Has-a             | Solid Line | Independent      | School ↔ Teacher         |
| Inheritance  | Is-a              | Triangle   | Inherited        | Dog → Animal             |
| Aggregation  | Weak Whole-Part   | ◇          | Independent      | Department ↔ Professor   |
| Composition  | Strong Whole-Part | ◆          | Dependent        | House → Room             |
| Dependency   | Uses              | Dashed →   | Temporary        | Service uses a parameter |

---

### ✅ Why Use UML & Class Diagrams?

* **Visual Communication** between developers and stakeholders.
* Acts as **documentation**.
* Helps in **architecture planning**.
* Supports **refactoring and scaling**.
* Some tools allow **code generation** from diagrams.

### 🛠 Tools to Draw UML

* Draw\.io
* Lucidchart
* StarUML
* PlantUML
* Visual Paradigm
* IntelliJ IDEA / Eclipse (built-in)

---
