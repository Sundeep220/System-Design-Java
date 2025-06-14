# 🧱 Builder Pattern Assignment

## 🎯 Problem: Resume Builder System

You’re designing a **Resume Builder** for a job portal. Users can build custom resumes by selecting various optional sections. Since the object (`Resume`) is complex and has many optional fields, the **Builder Pattern** is the ideal fit.

---

## 📄 Requirements

### 1. Create a `Resume` class with the following fields:

- `String name` ✅ *(Required)*
- `String email` ✅ *(Required)*
- `String phone` ❌ *(Optional)*
- `String linkedin` ❌ *(Optional)*
- `String github` ❌ *(Optional)*
- `String summary` ❌ *(Optional)*
- `List<String> skills` ❌ *(Optional)*
- `List<String> experiences` ❌ *(Optional)*

> ❗ Do not allow direct instantiation of the `Resume` class. Use `ResumeBuilder` instead.

---

### 2. Create a `ResumeBuilder` class that:

- Has **chained setter methods** for all fields.
- Ensures that required fields (`name`, `email`) are passed to the builder's constructor.
- Has a `build()` method that returns the final `Resume` object.

---

### 3. The `Resume` class should have a `toString()` method that prints all fields in a nice format.
If optional fields are not set, they should be skipped in the output.

---

## 🧪 Sample Usage

```java
Resume resume = new ResumeBuilder("Alice", "alice@example.com")
                    .phone("1234567890")
                    .linkedin("linkedin.com/in/alice")
                    .skills(List.of("Java", "Spring", "SQL"))
                    .build();

System.out.println(resume);
```

---

## ✅ Expected Output

```
Name: Alice
Email: alice@example.com
Phone: 1234567890
LinkedIn: linkedin.com/in/alice
Skills: Java, Spring, SQL
```

---

## 🔄 Bonus Challenge

- Add a method `exportToPDF(String filename)` inside the `Resume` class that simulates exporting the resume to a file.
