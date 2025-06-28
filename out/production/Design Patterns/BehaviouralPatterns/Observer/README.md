# 👁️‍🗨️ Observer Design Pattern – Full Explanation

### 🔍 **Intent**:

> *"Define a one-to-many dependency between objects so that when one object (the subject) changes state, all its dependents (observers) are notified and updated automatically."*

---

## 🏠 Real-World Analogy

### 📢 **YouTube Channel Subscribers**:

* A **YouTube channel** is the **Subject**.
* **Users who subscribe** are the **Observers**.
* When the channel posts a new video (state changes), **all subscribers are notified**.

YouTube doesn’t care *who* the users are — it just **broadcasts** the update to all registered observers.

---

## 👷 Problem Before Observer

* In tightly coupled systems, objects that need to respond to changes must **continuously poll** other objects or maintain direct references to them.
* This violates **Open/Closed** and **Single Responsibility Principles**.

---

## ✅ Observer Pattern to the Rescue

* The **Subject** maintains a list of **observers**.
* When its state changes, it **notifies all observers** by calling an update method.
* Observers **react independently** without needing tight coupling.

---

## 🧱 Participants

| Role                 | Description                                                                                         |
| -------------------- | --------------------------------------------------------------------------------------------------- |
| **Subject**          | Knows its observers. Provides methods to attach/detach observers. Notifies them when state changes. |
| **Observer**         | Defines an `update()` method that is called when the subject's state changes.                       |
| **ConcreteSubject**  | Holds the actual state and notifies observers.                                                      |
| **ConcreteObserver** | Implements `update()` to perform specific actions on change.                                        |

---

## 🧑‍💻 Java Example – News Agency and Subscribers

### ✅ 1. `Observer` Interface

```java
interface Observer {
    void update(String news);
}
```

---

### ✅ 2. `Subject` Interface

```java
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers();
}
```

---

### ✅ 3. `NewsAgency` (ConcreteSubject)

```java
class NewsAgency implements Subject {
    private List<Observer> observers = new ArrayList<>();
    private String news;

    public void setNews(String news) {
        this.news = news;
        notifyObservers(); // Notify when state changes
    }

    public String getNews() {
        return news;
    }

    public void attach(Observer observer) {
        observers.add(observer);
    }

    public void detach(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(news);
        }
    }
}
```

---

### ✅ 4. `Subscriber` (ConcreteObserver)

```java
class Subscriber implements Observer {
    private String name;

    public Subscriber(String name) {
        this.name = name;
    }

    public void update(String news) {
        System.out.println(name + " received news: " + news);
    }
}
```

---

### ✅ 5. Demo

```java
public class ObserverPatternDemo {
    public static void main(String[] args) {
        NewsAgency agency = new NewsAgency();

        Observer user1 = new Subscriber("Alice");
        Observer user2 = new Subscriber("Bob");
        Observer user3 = new Subscriber("Charlie");

        agency.attach(user1);
        agency.attach(user2);

        agency.setNews("Breaking: Java 22 Released!");
        
        agency.attach(user3);
        agency.setNews("Weather Update: It's raining in Bengaluru.");

        agency.detach(user2);
        agency.setNews("Sports: India wins the Test match!");
    }
}
```

---

### ✅ Output:

```
Alice received news: Breaking: Java 22 Released!
Bob received news: Breaking: Java 22 Released!
Alice received news: Weather Update: It's raining in Bengaluru.
Bob received news: Weather Update: It's raining in Bengaluru.
Charlie received news: Weather Update: It's raining in Bengaluru.
Alice received news: Sports: India wins the Test match!
Charlie received news: Sports: India wins the Test match!
```

---

## ✅ When to Use Observer Pattern

* Implementing **event systems**, e.g., GUI components, messaging
* **Publish-Subscribe** mechanisms
* **Notification systems** (e.g., email/SMS alerts)
* Situations where **many parts** of a system should respond to **state changes** in one component

---