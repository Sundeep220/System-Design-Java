# 🧠 Problem: **Traffic Light System**

### 📘 Scenario:

You’re designing a **traffic light controller**. The traffic light has 3 states:

* **Red** → Next → Green
* **Green** → Next → Yellow
* **Yellow** → Next → Red

Each light lasts for a fixed time (you can simulate that with a print statement).

You need to:

* Create a state interface `TrafficLightState`
* Create concrete state classes: `RedLight`, `GreenLight`, `YellowLight`
* Create a context class `TrafficLight`
* Each state knows how to transition to the next

---

## ✅ Goal Output

```java
Traffic Light is RED. Stop!
Traffic Light is GREEN. Go!
Traffic Light is YELLOW. Caution!
Traffic Light is RED. Stop!
...
```

---

## ✅ Step-by-Step Plan

### 1. `TrafficLightState` Interface

```java
interface TrafficLightState {
    void switchLight(TrafficLight context);
}
```

---

### 2. Concrete State Classes

```java
class RedLight implements TrafficLightState {
    public void switchLight(TrafficLight context) {
        System.out.println("Traffic Light is RED. Stop!");
        context.setState(new GreenLight());
    }
}

class GreenLight implements TrafficLightState {
    public void switchLight(TrafficLight context) {
        System.out.println("Traffic Light is GREEN. Go!");
        context.setState(new YellowLight());
    }
}

class YellowLight implements TrafficLightState {
    public void switchLight(TrafficLight context) {
        System.out.println("Traffic Light is YELLOW. Caution!");
        context.setState(new RedLight());
    }
}
```

---

### 3. Context Class

```java
class TrafficLight {
    private TrafficLightState currentState;

    public TrafficLight() {
        currentState = new RedLight(); // Initial state
    }

    public void setState(TrafficLightState state) {
        currentState = state;
    }

    public void change() {
        currentState.switchLight(this);
    }
}
```

---

### 4. Demo

```java
public class TrafficLightDemo {
    public static void main(String[] args) {
        TrafficLight light = new TrafficLight();

        for (int i = 0; i < 6; i++) {
            light.change();
        }
    }
}
```

---

### ✅ Output

```
Traffic Light is RED. Stop!
Traffic Light is GREEN. Go!
Traffic Light is YELLOW. Caution!
Traffic Light is RED. Stop!
Traffic Light is GREEN. Go!
Traffic Light is YELLOW. Caution!
```

---

## ✅ What You Practiced

* Object behavior changes **dynamically at runtime**
* No need for `if/else` or `switch` in `TrafficLight`
* Each state is **self-contained and handles its own transition**

---
