# 🧠 Memento Design Pattern – Detailed Explanation

### 🔍 **Intent**:

> *"Without violating encapsulation, capture and externalize an object's internal state so that it can be restored to that state later."*

---

## 🏠 Real-World Analogy

Think of **Ctrl+Z (Undo)** in Microsoft Word.

* When you make changes, Word saves a "snapshot" of the document.
* If you hit undo, it restores the previous snapshot.
* You (user) never know *how* Word stores it — but the system can "restore" your content easily.

The **document** is the object whose state is captured,
the **snapshot** is the memento,
and the **undo manager** is the caretaker.

---

## 🤕 Problem Before Memento

* Suppose an object has private fields, and we want to **revert to a previous state**.
* Exposing fields violates **encapsulation**.
* Duplicating the entire object is inefficient or messy.
* No centralized way to manage state history.

---

## ✅ How Memento Solves This

* The **Originator** creates a **Memento** (a snapshot of its state).
* The **Caretaker** keeps track of these mementos.
* When needed, the originator can be restored to a previous state using a saved memento.
* Internal state is kept **private and safe** from external classes.

---

## 🧱 Participants

| Role         | Responsibility                                                     |
| ------------ | ------------------------------------------------------------------ |
| `Originator` | The object whose state needs to be saved and restored.             |
| `Memento`    | Stores the internal state of the originator (snapshot).            |
| `Caretaker`  | Manages and stores mementos but doesn’t operate on their contents. |

---

## 🧑‍💻 Simple Java Example

### Scenario: A Text Editor with Undo Feature

```java
// Memento class (snapshot)
class TextMemento {
    private final String state;

    public TextMemento(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}

// Originator
class TextEditor {
    private String content;

    public void type(String words) {
        content += words;
    }

    public String getContent() {
        return content;
    }

    public TextMemento save() {
        return new TextMemento(content);
    }

    public void restore(TextMemento memento) {
        content = memento.getState();
    }
}

// Caretaker
class EditorHistory {
    private Stack<TextMemento> history = new Stack<>();

    public void save(TextMemento memento) {
        history.push(memento);
    }

    public TextMemento undo() {
        if (!history.isEmpty()) return history.pop();
        return new TextMemento("");
    }
}

// Demo
public class MementoDemo {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        EditorHistory history = new EditorHistory();

        editor.type("Hello ");
        history.save(editor.save());

        editor.type("World!");
        history.save(editor.save());

        editor.type(" This is extra text.");

        System.out.println("Current content: " + editor.getContent());

        // Undo twice
        editor.restore(history.undo());
        System.out.println("After 1st undo: " + editor.getContent());

        editor.restore(history.undo());
        System.out.println("After 2nd undo: " + editor.getContent());
    }
}
```

### ✅ Output:

```
Current content: Hello World! This is extra text.
After 1st undo: Hello World!
After 2nd undo: Hello 
```

---

## ✅ When to Use Memento Pattern

* You want to implement **undo/redo**.
* You want to maintain a **history of states**.
* You need to save and restore objects' states **without violating encapsulation**.

---

## ⚠️ Limitations

* If object state is large, **memory usage** can be high.
* Requires careful **management of saved states** (pruning old ones, etc.).

---