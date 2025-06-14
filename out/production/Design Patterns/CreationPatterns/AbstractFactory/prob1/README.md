# 🏭 Abstract Factory Pattern Assignment

## 🎯 Problem: Cross-Platform UI Component Factory

You're designing a cross-platform UI rendering engine that supports two platforms:

- `Windows`
- `MacOS`

Each platform has its own version of UI components like:

- **Button**
- **Checkbox**

You need to use the **Abstract Factory Pattern** to create families of related components **without coupling the client to the actual classes**.

---

## 📦 Requirements

### 1. Define UI Component Interfaces:

```java
public interface Button {
    void render();
}

public interface Checkbox {
    void render();
}
```

---

### 2. Implement Concrete Classes:

#### For Windows:
- `WindowsButton implements Button`
- `WindowsCheckbox implements Checkbox`

#### For MacOS:
- `MacButton implements Button`
- `MacCheckbox implements Checkbox`

Each component should override `render()` to print:
```
Rendering <Platform> <Component>
```
For example:
```
Rendering Windows Button
Rendering Mac Checkbox
```

---

### 3. Create an Abstract Factory:

```java
public interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
}
```

---

### 4. Implement Concrete Factories:

- `WindowsFactory implements GUIFactory`
- `MacFactory implements GUIFactory`

Each factory returns the correct platform-specific components.

---

### 5. Create a `UIRenderer` Class (Client)

- Accepts a `GUIFactory` in its constructor.
- Has a method `renderUI()` which:
    - Creates a `Button` and `Checkbox` from the factory.
    - Calls their `render()` methods.

---

## 🧪 Testing

In your `Main` class:

1. Pass either `WindowsFactory` or `MacFactory` to the `UIRenderer`.
2. Run the app and check if the correct components are rendered.

---

## ✅ Sample Output (for MacOS):

```
Rendering Mac Button
Rendering Mac Checkbox
```

---

## 🔄 Bonus (Optional)

- Add another family: `LinuxFactory`
- Use an `enum Platform { WINDOWS, MAC, LINUX }` and a `FactoryProvider.getFactory(Platform platform)` method to decide dynamically.
