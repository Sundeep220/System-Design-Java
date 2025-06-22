This one is **very performance-focused**, used when you need to **optimize memory usage** and avoid object duplication — especially in systems with **millions of similar objects**.

---

## 🪶 **Flyweight Design Pattern**

---

### 📖 **Intent:**

> Use sharing to support a large number of fine-grained objects efficiently.

In simple terms:

* **Avoid creating duplicate objects** for identical data.
* **Share common (intrinsic) data**, and separate out unique (extrinsic) data.

---

## 🧠 Why Use It?

* To save **memory** in large-scale systems (e.g., gaming, document editors, UI rendering).
* To manage **many similar objects** (millions) that contain some **shared** and some **unique** data.

---

## 💡 Real-World Analogy

### 📝 **Character Formatting in Word Processors (e.g., MS Word)**

In a document with 1 million characters:

* You don’t need 1 million separate objects with font, size, color, etc.
* Instead:

    * Shared: font, size, color → **flyweight**
    * Unique: character position or value → **extrinsic**

---

## 🔧 Flyweight Pattern Components

| Role             | Description                          |
| ---------------- | ------------------------------------ |
| Flyweight        | The shared object (intrinsic state)  |
| FlyweightFactory | Manages and reuses Flyweight objects |
| Context          | Uses Flyweight + extrinsic state     |

---

## 👨‍💻 Java Example: **Text Editor with Flyweights**

---

### ✅ Step 1: The Flyweight – `CharacterStyle`

```java
public class CharacterStyle {
    private String font;
    private int size;
    private String color;

    public CharacterStyle(String font, int size, String color) {
        this.font = font;
        this.size = size;
        this.color = color;
    }

    public void apply(char c, int position) {
        System.out.println("Rendering '" + c + "' at position " + position +
            " with [" + font + ", " + size + "pt, " + color + "]");
    }
}
```

---

### ✅ Step 2: Flyweight Factory

```java
import java.util.HashMap;
import java.util.Map;

public class StyleFactory {
    private static final Map<String, CharacterStyle> stylePool = new HashMap<>();

    public static CharacterStyle getStyle(String font, int size, String color) {
        String key = font + size + color;

        if (!stylePool.containsKey(key)) {
            stylePool.put(key, new CharacterStyle(font, size, color));
            System.out.println("Creating new style: " + key);
        }

        return stylePool.get(key);
    }
}
```

---

### ✅ Step 3: Context – `TextCharacter`

```java
public class TextCharacter {
    private char character;            // Extrinsic state
    private int position;              // Extrinsic state
    private CharacterStyle style;      // Intrinsic (shared)

    public TextCharacter(char character, int position, CharacterStyle style) {
        this.character = character;
        this.position = position;
        this.style = style;
    }

    public void render() {
        style.apply(character, position);
    }
}
```

---

### ✅ Step 4: Client Code

```java
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<TextCharacter> text = new ArrayList<>();

        // Create flyweight styles
        CharacterStyle headerStyle = StyleFactory.getStyle("Arial", 24, "Black");
        CharacterStyle normalStyle = StyleFactory.getStyle("Arial", 12, "Gray");

        // Create characters with shared styles
        text.add(new TextCharacter('H', 0, headerStyle));
        text.add(new TextCharacter('e', 1, headerStyle));
        text.add(new TextCharacter('l', 2, headerStyle));
        text.add(new TextCharacter('l', 3, headerStyle));
        text.add(new TextCharacter('o', 4, headerStyle));

        text.add(new TextCharacter(' ', 5, normalStyle));
        text.add(new TextCharacter('W', 6, normalStyle));
        text.add(new TextCharacter('o', 7, normalStyle));
        text.add(new TextCharacter('r', 8, normalStyle));
        text.add(new TextCharacter('l', 9, normalStyle));
        text.add(new TextCharacter('d', 10, normalStyle));

        // Render
        for (TextCharacter ch : text) {
            ch.render();
        }
    }
}
```

---

### ✅ Output (Simplified)

```
Creating new style: Arial24Black
Creating new style: Arial12Gray
Rendering 'H' at position 0 with [Arial, 24pt, Black]
Rendering 'e' at position 1 with [Arial, 24pt, Black]
Rendering 'l' at position 2 with [Arial, 24pt, Black]
Rendering 'l' at position 3 with [Arial, 24pt, Black]
Rendering 'o' at position 4 with [Arial, 24pt, Black]
Rendering ' ' at position 5 with [Arial, 12pt, Gray]
Rendering 'W' at position 6 with [Arial, 12pt, Gray]
Rendering 'o' at position 7 with [Arial, 12pt, Gray]
Rendering 'r' at position 8 with [Arial, 12pt, Gray]
Rendering 'l' at position 9 with [Arial, 12pt, Gray]
Rendering 'd' at position 10 with [Arial, 12pt, Gray]
```

---

## ✅ Benefits of Flyweight Pattern

| Advantage              | Description                                                 |
| ---------------------- | ----------------------------------------------------------- |
| Memory Optimization    | Reuse shared objects instead of creating new ones           |
| Separation of Concerns | Shared vs unique data is clearly separated                  |
| Better Performance     | Reduces object creation overhead in high-scale environments |

---

## 🧩 Real-World Use Cases

| Scenario                   | Flyweight (shared)       | Context (unique)    |
| -------------------------- | ------------------------ | ------------------- |
| Document Editors (MS Word) | Font, color, style       | Character, position |
| 2D Games                   | Tree/Enemy sprites       | Position, health    |
| Web Browsers (DOM trees)   | Styles, CSS objects      | DOM node state      |
| Map Rendering              | City icons, marker types | Coordinates         |

---
