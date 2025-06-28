Excellent! Let’s dive into the **Mediator Pattern**, another powerful **Behavioral Design Pattern** that helps reduce complexity and coupling between objects.

---

## 🧠 Mediator Pattern – Full Explanation

### 🔍 **Intent**:

> *Define an object that encapsulates how a set of objects interact. Mediator promotes loose coupling by keeping objects from referring to each other explicitly and allows their interaction to vary independently.*

---

## 🏠 Real-World Analogy

### 👩‍🏫 Classroom with a Teacher

Imagine a classroom:

* Students don’t directly talk to each other.
* They **communicate through the teacher** (the **mediator**).
* The teacher controls who speaks and when.

✅ This avoids chaos (tight coupling between students).

---

## ✅ Problem Before Mediator Pattern

* Components communicate directly with each other → **tight coupling**.
* Changing interaction logic means **changing multiple classes**.
* Hard to reuse components independently.

---

## ✅ Mediator Pattern to the Rescue

* Introduces a **Mediator object** that handles communication between components.
* Components talk **to the mediator**, **not to each other**.
* Promotes **loose coupling** and centralized control.

---

## 🧱 Participants

| Role                       | Description                                           |
| -------------------------- | ----------------------------------------------------- |
| **Mediator (interface)**   | Declares communication methods.                       |
| **ConcreteMediator**       | Implements coordination logic.                        |
| **Colleague (components)** | Individual objects that communicate via the mediator. |

---

## 🧑‍💻 Java Example – Chat Room (Basic Mediator)

---

### ✅ 1. `ChatMediator` Interface

```java
interface ChatMediator {
    void sendMessage(String message, User user);
    void addUser(User user);
}
```

---

### ✅ 2. `ConcreteChatMediator`

```java
class ChatRoom implements ChatMediator {
    private List<User> users = new ArrayList<>();

    public void addUser(User user) {
        users.add(user);
    }

    public void sendMessage(String message, User sender) {
        for (User user : users) {
            if (user != sender) {
                user.receive(message);
            }
        }
    }
}
```

---

### ✅ 3. `User` (Colleague)

```java
abstract class User {
    protected ChatMediator mediator;
    protected String name;

    public User(ChatMediator mediator, String name) {
        this.mediator = mediator;
        this.name = name;
    }

    public abstract void send(String message);
    public abstract void receive(String message);
}
```

---

### ✅ 4. `ConcreteUser`

```java
class ConcreteUser extends User {
    public ConcreteUser(ChatMediator mediator, String name) {
        super(mediator, name);
    }

    public void send(String message) {
        System.out.println(name + " sends: " + message);
        mediator.sendMessage(message, this);
    }

    public void receive(String message) {
        System.out.println(name + " receives: " + message);
    }
}
```

---

### ✅ 5. Demo

```java
public class MediatorPatternDemo {
    public static void main(String[] args) {
        ChatMediator chatRoom = new ChatRoom();

        User user1 = new ConcreteUser(chatRoom, "Alice");
        User user2 = new ConcreteUser(chatRoom, "Bob");
        User user3 = new ConcreteUser(chatRoom, "Charlie");

        chatRoom.addUser(user1);
        chatRoom.addUser(user2);
        chatRoom.addUser(user3);

        user1.send("Hello everyone!");
        user3.send("Hi Alice!");
    }
}
```

---

### ✅ Output

```
Alice sends: Hello everyone!
Bob receives: Hello everyone!
Charlie receives: Hello everyone!

Charlie sends: Hi Alice!
Alice receives: Hi Alice!
Bob receives: Hi Alice!
```

---

## ✅ When to Use Mediator Pattern

* You want to **centralize complex communication** logic between many objects.
* You want to **decouple components** from each other.
* You want to **reuse components independently** of their communication logic.

---

## 🔍 Mediator vs. Observer – Key Differences

| Aspect               | **Mediator Pattern**                                                         | **Observer Pattern**                                                   |
| -------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **Intent**           | Encapsulate complex communication logic between objects in a central object. | Notify multiple objects (observers) when one object (subject) changes. |
| **Communication**    | **Two-way**: Components talk to each other *through* the mediator.           | **One-way**: Subject notifies all observers of a change.               |
| **Control**          | Mediator has full **central control** over who talks to whom and when.       | Subject simply **broadcasts** updates to all subscribers.              |
| **Coupling**         | Reduces **tight coupling between components** that interact.                 | Reduces **tight coupling between subject and observers**.              |
| **Typical Use Case** | GUI components interaction, air traffic control, chat rooms.                 | Event listeners, data-binding, notification systems.                   |
| **Example**          | Chat Room → Users send messages via the ChatRoom mediator.                   | Weather Station → Observers update when temperature changes.           |

---

## 🧠 Analogy to Clarify

### 🗣 Mediator:

> “Hey ChatRoom, please deliver this message to others for me.”
> → The mediator (ChatRoom) decides *who* gets the message and *how*.

### 🔔 Observer:

> “I’m subscribed to the weather station. If temperature changes, notify me.”
> → The subject (Weather Station) **broadcasts** an event to all observers, unaware of what they do with it.

---

## ✅ When to Use Which?

### Use **Observer** when:

* You need to **broadcast changes** to many objects.
* You want **decoupled, one-to-many notification**.

### Use **Mediator** when:

* You want to **orchestrate complex interactions** between many objects.
* You want to **reduce pairwise dependencies** among interacting objects.

---

## 🧪 Summary:

| ❓                                                      | Mediator | Observer |
| ------------------------------------------------------ | -------- | -------- |
| Central coordinator of communication?                  | ✅        | ❌        |
| Event broadcasting to subscribers?                     | ❌        | ✅        |
| Good for decoupling many-to-many object communication? | ✅        | ❌        |
| Each component reacts independently?                   | ❌        | ✅        |

---
