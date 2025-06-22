# 🌳 **Composite Design Pattern**

---

### 📖 **Intent:**

> Compose objects into **tree structures** to represent part-whole hierarchies.
> **Clients can treat individual objects and compositions uniformly.**

---

## 🧠 Why Use It?

* To work with **hierarchical structures** like menus, file systems, organization charts, GUIs, etc.
* To **treat leaf nodes and groups of nodes (composites)** in the same way.
* To **reduce complex client logic** when dealing with object trees.

---

## 💡 Real-World Analogy

### 🗂️ **File System Analogy**

A file system has:

* **Files (leaf)** – cannot contain other elements
* **Folders (composite)** – can contain files or other folders

You can perform the same operation (like `open`, `delete`, `size`) on both — and the system handles whether it’s a file or folder internally.

---

## 👨‍💻 Java Implementation: **File System Example**

---

### ✅ Step 1: Common Interface (Component)

```java
public interface FileSystemItem {
    void display(String indent);
}
```

---

### ✅ Step 2: Leaf - File

```java
public class File implements FileSystemItem {
    private String name;

    public File(String name) {
        this.name = name;
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "- File: " + name);
    }
}
```

---

### ✅ Step 3: Composite - Folder

```java
import java.util.ArrayList;
import java.util.List;

public class Folder implements FileSystemItem {
    private String name;
    private List<FileSystemItem> children = new ArrayList<>();

    public Folder(String name) {
        this.name = name;
    }

    public void add(FileSystemItem item) {
        children.add(item);
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "+ Folder: " + name);
        for (FileSystemItem item : children) {
            item.display(indent + "  ");
        }
    }
}
```

---

### ✅ Step 4: Client Code

```java
public class Main {
    public static void main(String[] args) {
        FileSystemItem file1 = new File("resume.pdf");
        FileSystemItem file2 = new File("photo.png");
        FileSystemItem file3 = new File("notes.txt");

        Folder personal = new Folder("Personal");
        personal.add(file1);
        personal.add(file2);

        Folder work = new Folder("Work");
        work.add(file3);

        Folder root = new Folder("Root");
        root.add(personal);
        root.add(work);

        root.display("");
    }
}
```

---

### ✅ Output

```
+ Folder: Root
  + Folder: Personal
    - File: resume.pdf
    - File: photo.png
  + Folder: Work
    - File: notes.txt
```

---

## ✅ Benefits of Composite Pattern

| Advantage                   | Description                         |
|-----------------------------| ----------------------------------- |
| Uniformity                  | Treat leaf and composite objects uniformly |
| Extensibility               | Add new types of components easily  |
| Hierarchical Representation | Perfect for trees, menus, GUI elements |
| Simplicity for Clients      | Clients don't worry if it's a group or single |
| Scalability                 | No need to add new methods to existing classes |
| Code Reusability            | Reuse existing code for new structures |

---

## 🧩 Other Real-World Use Cases

| Use Case                     | Composite     | Leaf          |
| ---------------------------- | ------------- | ------------- |
| UI Components (Swing/JavaFX) | Panel         | Button, Label |
| Organization Chart           | Manager       | Employee      |
| Menu System                  | Menu/Dropdown | MenuItem      |
| Project Management           | Project/Phase | Task          |

---