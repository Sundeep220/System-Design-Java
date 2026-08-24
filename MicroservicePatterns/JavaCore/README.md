# Java Core — Navigation Guide

Follow this order. Each topic builds the foundation for the next. OOP concepts must be understood before annotations and reflection.

---

## Layer 1 — Object-Oriented Foundations

Start with the core building blocks of Java OOP. Each concept depends on the previous.

| # | File | What You Learn |
|---|---|---|
| 1 | `Classes/README.md` | Class anatomy: fields, methods, constructors, static vs instance members, access modifiers, inner classes, object instantiation, `this` and `super` |
| 2 | `Encapsulation/README.md` | Why encapsulation exists, private fields + public getters/setters, information hiding, data validation through setters, encapsulation vs immutability |
| 3 | `Inheritance/README.md` | `extends`, superclass / subclass, constructor chaining with `super()`, method inheritance, `final` class and methods, when NOT to use inheritance |

---

## Layer 2 — Polymorphism & Method Resolution

Once you have classes and inheritance, understand how Java decides which method to call at runtime.

| # | File | What You Learn |
|---|---|---|
| 4 | `Polymorphism/README.md` | Compile-time vs runtime polymorphism, upcasting, virtual method dispatch, vtable, reference type vs object type, `instanceof` |
| 5 | `OverloadingVsOverriding/README.md` | Overloading: same name, different signature, resolved at compile time. Overriding: same signature, resolved at runtime. Common interview traps with both. |

---

## Layer 3 — Metadata & Reflection

After understanding how classes and methods work, learn how to inspect and drive them programmatically.

| # | File | What You Learn |
|---|---|---|
| 6 | `Annotations/README.md` | What annotations are, built-in annotations (`@Override`, `@Deprecated`, `@FunctionalInterface`), retention policies (SOURCE / CLASS / RUNTIME), custom annotations, how Spring and JUnit use them |
| 7 | `Reflection/README.md` | `Class<?>`, `Method`, `Field`, `Constructor` — inspecting class structure at runtime, invoking methods dynamically, accessing private fields, how Spring/Hibernate use reflection, performance implications |

---

## Why This Order

```
Classes → Encapsulation: you need to understand class fields before hiding them
Encapsulation → Inheritance: you need to understand the base structure before extending it
Inheritance → Polymorphism: you need inheritance before understanding method dispatch
Polymorphism → Overloading vs Overriding: these are the two forms of polymorphism
Overriding → Annotations: annotations are metadata on classes, methods, and fields
Annotations → Reflection: reflection reads annotations and inspects class structure at runtime
```

---

## Quick Reference — Topic by Interview Context

```
"What is OOP?" → Classes → Encapsulation → Inheritance → Polymorphism

"Explain polymorphism" → Polymorphism → OverloadingVsOverriding

"How does Spring work internally?" → Annotations → Reflection

"What is method overriding vs overloading?" → OverloadingVsOverriding

"How does Hibernate map entities?" → Annotations → Reflection
```
