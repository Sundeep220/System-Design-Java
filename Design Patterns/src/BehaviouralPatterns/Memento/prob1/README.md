# 🧪 Problem Statement: Advanced Text Editor with Multi-Field State & Undo/Redo

You are building a **rich text editor** that stores not only the **text content**, but also the **font size** and **font style**. The editor should support:

* Editing all three properties (`content`, `fontSize`, `fontStyle`)
* Undoing the last change
* Redoing a previously undone change
* Memento should preserve **entire state**
* Only the `Editor` should have access to internal fields (encapsulation must be respected)

---

## 🧱 Classes to Implement

* `RichTextEditor` (Originator)
* `EditorState` (Memento)
* `EditorHistory` (Caretaker)

---

### 🧑‍💻 Your Task:

Implement this setup in Java so that:

* You can edit content, font size, and style
* You can `undo()` and `redo()` and get the correct full state restored
* Caretaker (`EditorHistory`) does **not access** internal fields of the state
* Restore should work even if the state is complex (multi-field)

---

## 🧑‍💼 Example Usage:

```java
RichTextEditor editor = new RichTextEditor();
EditorHistory history = new EditorHistory(editor);

editor.setContent("Hello");
editor.setFontSize(12);
editor.setFontStyle("Arial");
history.save(); // Save initial state

editor.setContent("Hello World");
editor.setFontSize(14);
history.save(); // Save new state

editor.setFontStyle("Courier New");
System.out.println(editor); // should print updated full state

history.undo();
System.out.println(editor); // restored to: Hello World, 14, Arial

history.undo();
System.out.println(editor); // restored to: Hello, 12, Arial

history.redo();
System.out.println(editor); // Redo to Hello World, 14, Arial
```

