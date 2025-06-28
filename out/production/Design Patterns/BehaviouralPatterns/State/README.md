# 🧠 State Pattern – Full Explanation

### 🔍 **Intent**:

> *"Allow an object to alter its behavior when its internal state changes. The object will appear to change its class."*

---

## 🏠 Real-World Analogy

### 🔌 **Fan with Speed Settings**

Imagine a **ceiling fan** with:

* OFF state
* LOW speed
* MEDIUM speed
* HIGH speed

Each press of the button **transitions to a new state**, and the fan behaves differently:

* OFF → LOW
* LOW → MEDIUM
* MEDIUM → HIGH
* HIGH → OFF

Rather than using `if-else` or `switch`, each state is a class with its own logic.

---

## ✅ Problem Before State Pattern

* State transitions are usually handled with **big if-else or switch statements**.
* Violates **Open/Closed Principle** — adding new states means modifying existing code.
* State-specific behavior is scattered and hard to manage.

---

## ✅ State Pattern to the Rescue

* Encapsulates each state into its own **class**.
* The context object **delegates behavior** to the current state object.
* State transitions are **handled inside the state classes** themselves.

---

## 🧱 Participants

| Role                  | Description                                                              |
| --------------------- | ------------------------------------------------------------------------ |
| **Context**           | Maintains a reference to the current state and delegates behavior to it. |
| **State (interface)** | Declares methods representing actions.                                   |
| **Concrete States**   | Implement state-specific behavior and transition logic.                  |

---

## 🧑‍💻 Java Example – Fan State Machine

---

### ✅ 1. `State` Interface

```java
interface FanState {
    void pressButton(FanContext context);
}
```

---

### ✅ 2. Concrete States

```java
class OffState implements FanState {
    public void pressButton(FanContext context) {
        System.out.println("Turning fan to LOW speed.");
        context.setState(new LowState());
    }
}

class LowState implements FanState {
    public void pressButton(FanContext context) {
        System.out.println("Turning fan to MEDIUM speed.");
        context.setState(new MediumState());
    }
}

class MediumState implements FanState {
    public void pressButton(FanContext context) {
        System.out.println("Turning fan to HIGH speed.");
        context.setState(new HighState());
    }
}

class HighState implements FanState {
    public void pressButton(FanContext context) {
        System.out.println("Turning fan OFF.");
        context.setState(new OffState());
    }
}
```

---

### ✅ 3. Context – `FanContext`

```java
class FanContext {
    private FanState currentState;

    public FanContext() {
        currentState = new OffState(); // default
    }

    public void setState(FanState state) {
        this.currentState = state;
    }

    public void pressButton() {
        currentState.pressButton(this);
    }
}
```

---

### ✅ 4. Demo

```java
public class StatePatternDemo {
    public static void main(String[] args) {
        FanContext fan = new FanContext();

        fan.pressButton(); // OFF -> LOW
        fan.pressButton(); // LOW -> MEDIUM
        fan.pressButton(); // MEDIUM -> HIGH
        fan.pressButton(); // HIGH -> OFF
    }
}
```

---

### ✅ Output

```
Turning fan to LOW speed.
Turning fan to MEDIUM speed.
Turning fan to HIGH speed.
Turning fan OFF.
```

---

## ✅ When to Use State Pattern

* When an object must change behavior **at runtime** based on internal state.
* When code is cluttered with **many conditionals (if/switch)** based on state.
* When you want **each state to handle its own logic** independently.

---
