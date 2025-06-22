# 🌉 **Bridge Design Pattern**

---

### 📖 Intent:

> **Decouple an abstraction from its implementation** so that the two can vary independently.

---

## 🧠 Why Use It?

* To **avoid a class explosion** when you have multiple variations across two dimensions (like shape and color).
* To **separate concerns**: abstraction and implementation evolve separately.
* To allow **composition over inheritance** when you have cross-cutting hierarchies.

---

## 💡 Real-World Analogy

### 🖥️ **Remote Control & Devices**

A **Remote Control (Abstraction)** can control multiple types of **Devices (Implementation)** like:

* TV
* Set-top box
* Projector

You don’t want to create separate classes for `TVRemote`, `ProjectorRemote`, `SmartRemoteTV`, etc.

Instead, the remote should **delegate the operation to the device**, and both evolve independently.

---

## 🧱 Structure

```
Abstraction --------> Implementor
     ↑                     ↑
 RefinedAbstraction   ConcreteImplementor
```

---

## 👨‍💻 Java Example: **Remote Control & Device**

---

### ✅ Step 1: Implementor Interface

```java
public interface Device {
    void turnOn();
    void turnOff();
    void setVolume(int level);
}
```

---

### ✅ Step 2: Concrete Implementors

```java
public class TV implements Device {
    @Override
    public void turnOn() {
        System.out.println("Turning on TV");
    }

    @Override
    public void turnOff() {
        System.out.println("Turning off TV");
    }

    @Override
    public void setVolume(int level) {
        System.out.println("Setting TV volume to " + level);
    }
}
```

```java
public class Projector implements Device {
    @Override
    public void turnOn() {
        System.out.println("Turning on Projector");
    }

    @Override
    public void turnOff() {
        System.out.println("Turning off Projector");
    }

    @Override
    public void setVolume(int level) {
        System.out.println("Setting Projector volume to " + level);
    }
}
```

---

### ✅ Step 3: Abstraction

```java
public abstract class RemoteControl {
    protected Device device;

    public RemoteControl(Device device) {
        this.device = device;
    }

    public abstract void powerOn();
    public abstract void powerOff();
    public abstract void setVolume(int level);
}
```

---

### ✅ Step 4: Refined Abstraction

```java
public class BasicRemote extends RemoteControl {

    public BasicRemote(Device device) {
        super(device);
    }

    @Override
    public void powerOn() {
        device.turnOn();
    }

    @Override
    public void powerOff() {
        device.turnOff();
    }

    @Override
    public void setVolume(int level) {
        device.setVolume(level);
    }
}
```

---

### ✅ Step 5: Client Code

```java
public class Main {
    public static void main(String[] args) {
        Device tv = new TV();
        RemoteControl tvRemote = new BasicRemote(tv);

        tvRemote.powerOn();
        tvRemote.setVolume(15);
        tvRemote.powerOff();

        System.out.println();

        Device projector = new Projector();
        RemoteControl projectorRemote = new BasicRemote(projector);

        projectorRemote.powerOn();
        projectorRemote.setVolume(30);
        projectorRemote.powerOff();
    }
}
```

---

### ✅ Output

```
Turning on TV
Setting TV volume to 15
Turning off TV

Turning on Projector
Setting Projector volume to 30
Turning off Projector
```

---

### ✅ Why Keep `Device` in `RemoteControl`?

1. **All remotes (abstractions) need to control some device**, right?

   So, the abstraction (`RemoteControl`) **requires** a reference to the `Device`. Putting it in `RemoteControl`:

    * Allows all subclasses to **inherit** the `device` field.
    * Ensures consistency across all types of remotes.

---

2. **Avoids duplication**
   If you had multiple types of remotes (`BasicRemote`, `VoiceRemote`, `SmartRemote`, etc.), each would have to define and manage the `Device` reference separately — which violates **DRY (Don't Repeat Yourself)**.

---

3. **Makes the abstraction self-sufficient**

Even if you never instantiate `RemoteControl` directly, it still **represents the abstraction** of a remote that works on any device. Having `device` inside ensures it knows what it’s abstracting over.

---

### ✅ Analogy:

Imagine:

* `RemoteControl` is an abstract **concept** of a remote
* `Device` is **any appliance** (TV, Fan, Projector)

The **abstract remote** knows *something* about controlling devices — even if it doesn’t know how to turn them on or off (that’s left to its concrete subclass).

So it makes sense that **every remote abstraction "has a" device** — this relationship lives in the base class.

---

### 🔁 If you moved `Device device` to `BasicRemote` instead:

* You'd have to **duplicate it** in every other remote subclass.
* You’d **violate the Bridge Pattern's purpose** — the decoupling of abstraction from implementation.

---

## ✅ Benefits of Bridge Pattern

| Feature                | Description                                    |
| ---------------------- | ---------------------------------------------- |
| Decouples abstraction  | Device and RemoteControl evolve independently  |
| Avoids class explosion | Combats the Cartesian product of combinations  |
| Favors composition     | Composition over inheritance                   |
| Extendable             | Easily add new devices or new types of remotes |

---

## 💡 Real-World Use Cases

| Domain           | Abstraction    | Implementation        |
| ---------------- | -------------- | --------------------- |
| UI Themes        | Button, Window | Windows, MacOS, Linux |
| Report Generator | Report         | PDF, HTML, Excel      |
| Message Sender   | Message        | Email, SMS, Push      |
| Remote Control   | Remote         | TV, Projector, etc.   |

---
That's a **great question** — and it gets to the heart of why the **Bridge Pattern** exists in the first place.

Let’s carefully clarify **why the `Device` object is injected in the abstract class (`RemoteControl`)** instead of just being used in `BasicRemote`.

---

## 🎯 Quick Summary of Your Doubt:

> "Why is `device` declared in `RemoteControl` (abstract class) and not just in `BasicRemote`?"

---

## ✅ The Reason: **Abstraction should be decoupled from implementation**

The **Bridge Pattern** is all about **separating abstraction from implementation**, and it achieves this by:

* **Putting shared behaviors & references in the abstract base class**
* Allowing **refined abstractions** (like `BasicRemote`, `AdvancedRemote`) to **reuse the connection to the implementor**

---

### 🧱 Let’s break this into roles:

| Component         | Role                                                |
| ----------------- | --------------------------------------------------- |
| `Device`          | Implementor interface                               |
| `TV`, `Projector` | Concrete implementations of `Device`                |
| `RemoteControl`   | **Abstract bridge class** (decouples from `Device`) |
| `BasicRemote`     | Concrete abstraction using the bridge               |

---

## ✅ Conclusion:

Keeping `Device device` in the **abstract class `RemoteControl`**:

* Promotes **reuse**
* Enables **loose coupling**
* Follows **Bridge pattern philosophy**
* Prepares the system for future remote types without redundancy

--- 
### ✅ What we Practiced:

| Concept                      | How You Used It                            |
| ---------------------------- | ------------------------------------------ |
| Bridge Pattern               | Separated report types from output formats |
| Composition over inheritance | Avoided class explosion with bridge        |
| Extensibility                | Easily add new formats or report types     |


