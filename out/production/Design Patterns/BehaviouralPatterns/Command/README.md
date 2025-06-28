# 🧠 Command Design Pattern – Full Explanation

### 🔍 **Intent**:

> *"Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations."*

---

## 🏠 Real-World Analogy

### 💡 **Remote Control**

* A TV remote control lets you **press buttons (commands)** to turn the TV on, change the channel, etc.
* The remote doesn't know how the TV works internally.
* Each button **encapsulates a request** (e.g., `TurnOnCommand`, `ChangeChannelCommand`).

> The remote (invoker) sends a command to the device (receiver) using a command object.

---

## 🧱 Problem Before Command Pattern

* If the invoker knows exactly **how to perform the operation**, it's tightly coupled to the receiver.
* Adding new operations means **modifying** the invoker's code — breaks **Open/Closed Principle**.
* Undo/redo logic becomes hard to track.

---

## ✅ Command Pattern to the Rescue

* Encapsulate each action into a separate **command object**.
* Decouple the **caller (invoker)** from the **executor (receiver)**.
* Support **undo/redo**, **macro commands**, and **logging** easily.

---

## 🧱 Participants

| Role                | Description                                                                      |
| ------------------- | -------------------------------------------------------------------------------- |
| **Command**         | Interface that declares `execute()` (and optionally `undo()`).                   |
| **ConcreteCommand** | Implements the command and defines the link between the Receiver and the action. |
| **Receiver**        | Performs the actual work (e.g., Light, TV, File).                                |
| **Invoker**         | Asks the command to carry out a request (e.g., RemoteControl).                   |
| **Client**          | Creates command objects and sets them in the invoker.                            |

---

## 🧑‍💻 Java Example – Smart Home Remote Control

---

### ✅ 1. Command Interface

```java
interface Command {
    void execute();
    void undo(); // optional
}
```

---

### ✅ 2. Receiver – Light

```java
class Light {
    public void turnOn() {
        System.out.println("Light is ON");
    }

    public void turnOff() {
        System.out.println("Light is OFF");
    }
}
```

---

### ✅ 3. Concrete Commands

```java
class TurnOnLightCommand implements Command {
    private Light light;

    public TurnOnLightCommand(Light light) {
        this.light = light;
    }

    public void execute() {
        light.turnOn();
    }

    public void undo() {
        light.turnOff();
    }
}

class TurnOffLightCommand implements Command {
    private Light light;

    public TurnOffLightCommand(Light light) {
        this.light = light;
    }

    public void execute() {
        light.turnOff();
    }

    public void undo() {
        light.turnOn();
    }
}
```

---

### ✅ 4. Invoker – Remote Control

```java
class RemoteControl {
    private Command command;

    public void setCommand(Command command) {
        this.command = command;
    }

    public void pressButton() {
        command.execute();
    }

    public void pressUndo() {
        command.undo();
    }
}
```

---

### ✅ 5. Demo

```java
public class CommandPatternDemo {
    public static void main(String[] args) {
        Light livingRoomLight = new Light();

        Command turnOn = new TurnOnLightCommand(livingRoomLight);
        Command turnOff = new TurnOffLightCommand(livingRoomLight);

        RemoteControl remote = new RemoteControl();

        remote.setCommand(turnOn);
        remote.pressButton();   // Output: Light is ON
        remote.pressUndo();     // Output: Light is OFF

        remote.setCommand(turnOff);
        remote.pressButton();   // Output: Light is OFF
        remote.pressUndo();     // Output: Light is ON
    }
}
```

---

### ✅ Output:

```
Light is ON  
Light is OFF  
Light is OFF  
Light is ON  
```

---

## ✅ When to Use Command Pattern

* You want to parameterize objects with operations.
* You want to queue operations, log them, or support undo/redo.
* You want to **decouple** the object that invokes the operation from the one that knows how to perform it.
* You want to support **macros** (e.g., execute a batch of commands).

---
