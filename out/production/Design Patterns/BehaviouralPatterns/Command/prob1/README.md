# 💻 Problem: **Text Editor with Undo (Command Pattern)**

### 📘 Scenario:

You are building a **simple text editor** that supports:

* Typing text
* Deleting text
* Undoing the last command

Your goal is to **encapsulate all operations as commands**, so the editor can:

* **Execute commands**
* **Undo commands**
* Maintain command history

---

## 🧱 Requirements

1. Implement a `Command` interface with `execute()` and `undo()`.
2. Implement two commands:

    * `AppendTextCommand`
    * `DeleteTextCommand`
3. The `Editor` class acts as the **Receiver** with a `StringBuilder` or similar structure.
4. A `CommandManager` acts as the **Invoker** and manages:

    * Executing commands
    * Undoing commands (via a stack)

---

## 🧑‍💻 Example Usage

```java
Editor editor = new Editor();
CommandManager manager = new CommandManager();

manager.executeCommand(new AppendTextCommand(editor, "Hello"));
        manager.executeCommand(new AppendTextCommand(editor, " World"));
        System.out.println(editor.getText());  // Output: Hello World

        manager.undo();
System.out.println(editor.getText());  // Output: Hello

        manager.executeCommand(new DeleteTextCommand(editor, 2));
        System.out.println(editor.getText());  // Output: He

        manager.undo();
System.out.println(editor.getText());  // Output: Hello
```

---
