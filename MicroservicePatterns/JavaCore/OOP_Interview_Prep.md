# 🎯 OOP Concepts — Interview Preparation Guide
> Comprehensive Q&A for Arctic Wolf and backend engineering interviews.
> Covers all 7 core OOP topics with real-world examples and code.

---

## Table of Contents
1. [Classes & Objects](#1-classes--objects)
2. [Encapsulation](#2-encapsulation)
3. [Inheritance](#3-inheritance)
4. [Polymorphism](#4-polymorphism)
5. [Overloading vs Overriding](#5-overloading-vs-overriding)
6. [Annotations](#6-annotations)
7. [Reflection](#7-reflection)

---

## 1. Classes & Objects

### ❓ What is a class? What is an object?
> **Answer:**
> - **Class** — a blueprint/template that defines the structure (fields) and behavior (methods) of objects
> - **Object** — an instance of a class; a concrete entity with state and behavior
>
> ```java
> // Class — blueprint
> class User {
>     String name;
>     int age;
>     void printInfo() { }
> }
>
> // Objects — instances
> User user1 = new User();  // object 1
> User user2 = new User();  // object 2 (different from user1)
> ```

---

### ❓ What is the difference between instance variables and static variables?
> **Answer:**
>
> | | Instance Variable | Static Variable |
> |---|---|---|
> | Memory | One copy per object | One copy per class (shared) |
> | Access | `obj.field` | `ClassName.field` |
> | Initialization | When object is created | When class is loaded |
> | Use case | Object-specific state | Shared across all objects |
>
> ```java
> class Counter {
>     int count = 0;              // instance — each object has its own
>     static int totalCount = 0;  // static — shared by all objects
>
>     Counter() {
>         count++;
>         totalCount++;
>     }
> }
>
> Counter c1 = new Counter();  // count=1, totalCount=1
> Counter c2 = new Counter();  // c1.count=1, c2.count=1, totalCount=2
> ```

---

### ❓ What is the difference between instance methods and static methods?
> **Answer:**
>
> | | Instance Method | Static Method |
> |---|---|---|
> | Access | `obj.method()` | `ClassName.method()` |
> | Can access | Instance variables, static variables | Only static variables |
> | `this` keyword | Available | Not available |
> | Use case | Object-specific behavior | Utility functions |
>
> ```java
> class MathUtils {
>     int multiplier = 2;
>
>     void instanceMultiply(int x) {
>         return x * multiplier;  // can access instance variable
>     }
>
>     static int staticAdd(int a, int b) {
>         return a + b;  // cannot access multiplier
>     }
> }
>
> MathUtils utils = new MathUtils();
> utils.instanceMultiply(5);      // instance method
> MathUtils.staticAdd(3, 4);      // static method
> ```

---

### ❓ What are constructors? What is constructor overloading?
> **Answer:**
> A constructor is a special method called when an object is created. It initializes the object's state.
>
> **Constructor overloading** — multiple constructors with different parameter lists.
>
> ```java
> class User {
>     String name;
>     int age;
>
>     // Constructor 1 — no args
>     User() {
>         this.name = "Unknown";
>         this.age = 0;
>     }
>
>     // Constructor 2 — name only
>     User(String name) {
>         this.name = name;
>         this.age = 0;
>     }
>
>     // Constructor 3 — name and age
>     User(String name, int age) {
>         this.name = name;
>         this.age = age;
>     }
> }
>
> User u1 = new User();
> User u2 = new User("Alice");
> User u3 = new User("Bob", 30);
> ```

---

### ❓ What is `this` keyword? What is `super` keyword?
> **Answer:**
> - **`this`** — refers to the current object; used to access instance variables and call instance methods
> - **`super`** — refers to the parent class; used to call parent constructor or parent methods
>
> ```java
> class Animal {
>     String name;
>     Animal(String name) {
>         this.name = name;
>     }
>     void sound() { System.out.println("Generic sound"); }
> }
>
> class Dog extends Animal {
>     String breed;
>
>     Dog(String name, String breed) {
>         super(name);        // call parent constructor
>         this.breed = breed; // set instance variable
>     }
>
>     void sound() {
>         super.sound();      // call parent method
>         System.out.println("Woof!");
>     }
> }
> ```

---

### ❓ What are access modifiers? Explain `public`, `private`, `protected`, and default (package-private).
> **Answer:**
>
> | Modifier | Class | Package | Subclass | World |
> |---|---|---|---|---|
> | `public` | ✅ | ✅ | ✅ | ✅ |
> | `protected` | ✅ | ✅ | ✅ | ❌ |
> | default (none) | ✅ | ✅ | ❌ | ❌ |
> | `private` | ✅ | ❌ | ❌ | ❌ |
>
> ```java
> public class User {
>     public String name;          // accessible everywhere
>     protected int age;           // accessible in subclass and package
>     int salary;                  // accessible only in package
>     private String password;     // accessible only in this class
> }
> ```

---

### ❓ What are inner classes? What are the types?
> **Answer:**
> Inner classes are classes defined inside another class.
>
> **Types:**
> 1. **Static inner class** — doesn't need outer object instance
> 2. **Non-static inner class (member inner class)** — needs outer object instance
> 3. **Local inner class** — defined inside a method
> 4. **Anonymous inner class** — no name, defined inline
>
> ```java
> class Outer {
>     int x = 10;
>
>     // Static inner class
>     static class StaticInner {
>         void print() { System.out.println("Static inner"); }
>     }
>
>     // Non-static inner class
>     class NonStaticInner {
>         void print() { System.out.println("x = " + x); }  // can access outer x
>     }
>
>     void method() {
>         // Local inner class
>         class LocalInner {
>             void print() { System.out.println("Local inner"); }
>         }
>         LocalInner li = new LocalInner();
>         li.print();
>     }
> }
>
> // Usage
> Outer.StaticInner si = new Outer.StaticInner();
> Outer o = new Outer();
> Outer.NonStaticInner nsi = o.new NonStaticInner();
> ```

---

## 2. Encapsulation

### ❓ What is encapsulation? Why is it important?
> **Answer:**
> Encapsulation is bundling data (fields) and methods together, and hiding internal details from the outside world.
>
> **Why important:**
> - **Data hiding** — prevent direct access to sensitive fields
> - **Validation** — enforce business rules through setters
> - **Flexibility** — change internal implementation without affecting external code
> - **Maintainability** — easier to modify and debug
>
> ```java
> // BAD — no encapsulation
> class BankAccount {
>     public double balance;  // anyone can modify!
> }
> BankAccount acc = new BankAccount();
> acc.balance = -1000;  // invalid state!
>
> // GOOD — encapsulation
> class BankAccount {
>     private double balance;
>
>     public void deposit(double amount) {
>         if (amount > 0) {
>             balance += amount;
>         }
>     }
>
>     public double getBalance() {
>         return balance;
>     }
> }
> ```

---

### ❓ What is the difference between encapsulation and immutability?
> **Answer:**
>
> | | Encapsulation | Immutability |
> |---|---|---|
> | Definition | Hide internal state, expose controlled interface | Object cannot be changed after creation |
> | Mutability | Object can be modified (via setters) | Object is read-only |
> | Use case | Protect data, enforce validation | Thread-safe, predictable behavior |
>
> ```java
> // Encapsulation (mutable)
> class User {
>     private String name;
>     public void setName(String name) { this.name = name; }  // can change
>     public String getName() { return name; }
> }
>
> // Immutability (immutable)
> final class ImmutableUser {
>     private final String name;
>     public ImmutableUser(String name) { this.name = name; }
>     public String getName() { return name; }
>     // no setters!
> }
> ```

---

### ❓ How do you create an immutable class?
> **Answer:**
> 1. Make the class `final` (prevent subclassing)
> 2. Make all fields `private final`
> 3. Initialize fields in constructor
> 4. Don't provide setters
> 5. If returning mutable objects, return copies
>
> ```java
> final class ImmutableUser {
>     private final String name;
>     private final int age;
>     private final List<String> hobbies;
>
>     public ImmutableUser(String name, int age, List<String> hobbies) {
>         this.name = name;
>         this.age = age;
>         this.hobbies = new ArrayList<>(hobbies);  // defensive copy
>     }
>
>     public String getName() { return name; }
>     public int getAge() { return age; }
>     public List<String> getHobbies() {
>         return new ArrayList<>(hobbies);  // return copy, not original
>     }
> }
> ```

---

## 3. Inheritance

### ❓ What is inheritance? What are the benefits?
> **Answer:**
> Inheritance allows a class (subclass) to inherit fields and methods from another class (superclass).
>
> **Benefits:**
> - **Code reuse** — avoid duplicating common code
> - **Hierarchy** — model "is-a" relationships
> - **Polymorphism** — write generic code that works with multiple types
>
> ```java
> // Superclass
> class Animal {
>     String name;
>     void eat() { System.out.println("Eating..."); }
> }
>
> // Subclass inherits from Animal
> class Dog extends Animal {
>     void bark() { System.out.println("Woof!"); }
> }
>
> Dog dog = new Dog();
> dog.eat();   // inherited from Animal
> dog.bark();  // defined in Dog
> ```

---

### ❓ What is the difference between `extends` and `implements`?
> **Answer:**
>
> | | extends | implements |
> |---|---|---|
> | Used for | Class inheritance | Interface implementation |
> | Inheritance | Single (one superclass) | Multiple (multiple interfaces) |
> | Methods | Can have implementation | Must be abstract (Java 8+ can have default) |
> | Fields | Can have state | Only constants (public static final) |
>
> ```java
> // Inheritance with extends
> class Dog extends Animal { }
>
> // Implementation with implements
> class Dog extends Animal implements Runnable, Comparable { }
> ```

---

### ❓ What is the `final` keyword? How is it used?
> **Answer:**
> `final` prevents modification. Usage depends on context:
>
> - **`final` class** — cannot be subclassed
> - **`final` method** — cannot be overridden
> - **`final` variable** — cannot be reassigned
>
> ```java
> final class ImmutableClass { }  // cannot extend
>
> class Parent {
>     final void criticalMethod() { }  // cannot override
> }
>
> class Child extends Parent {
>     final int MAX_VALUE = 100;  // cannot reassign
> }
> ```

---

### ❓ What is constructor chaining? How does `super()` work?
> **Answer:**
> Constructor chaining is calling one constructor from another using `this()` (same class) or `super()` (parent class).
>
> ```java
> class Animal {
>     String name;
>     Animal(String name) {
>         this.name = name;
>     }
> }
>
> class Dog extends Animal {
>     String breed;
>
>     Dog(String name, String breed) {
>         super(name);        // call parent constructor first
>         this.breed = breed;
>     }
> }
>
> Dog dog = new Dog("Buddy", "Labrador");
> // Order: Animal constructor → Dog constructor
> ```

---

### ❓ What is method overriding? What are the rules?
> **Answer:**
> Method overriding is redefining a parent method in a subclass with the same signature.
>
> **Rules:**
> - Same method name, parameters, and return type
> - Cannot reduce access (e.g., `public` → `private` is invalid)
> - Cannot throw new checked exceptions
> - Must use `@Override` annotation (best practice)
>
> ```java
> class Animal {
>     public void sound() { System.out.println("Generic sound"); }
> }
>
> class Dog extends Animal {
>     @Override
>     public void sound() { System.out.println("Woof!"); }
> }
> ```

---

## 4. Polymorphism

### ❓ What is polymorphism? What are the types?
> **Answer:**
> Polymorphism means "many forms" — the same method can behave differently based on context.
>
> **Types:**
> 1. **Compile-time (Static) Polymorphism** — method overloading, resolved at compile time
> 2. **Runtime (Dynamic) Polymorphism** — method overriding, resolved at runtime
>
> ```java
> // Compile-time polymorphism (overloading)
> class Calculator {
>     int add(int a, int b) { return a + b; }
>     double add(double a, double b) { return a + b; }
> }
>
> // Runtime polymorphism (overriding)
> Animal animal = new Dog();  // reference type: Animal, object type: Dog
> animal.sound();             // calls Dog.sound() at runtime
> ```

---

### ❓ What is upcasting and downcasting?
> **Answer:**
> - **Upcasting** — casting subclass to superclass (safe, implicit)
> - **Downcasting** — casting superclass to subclass (unsafe, explicit, needs `instanceof` check)
>
> ```java
> class Animal { }
> class Dog extends Animal { }
>
> // Upcasting (safe)
> Dog dog = new Dog();
> Animal animal = dog;  // implicit, no cast needed
>
> // Downcasting (unsafe)
> Animal animal = new Dog();
> Dog dog = (Dog) animal;  // explicit cast, can throw ClassCastException
>
> // Safe downcasting
> if (animal instanceof Dog) {
>     Dog dog = (Dog) animal;
> }
> ```

---

### ❓ What is virtual method dispatch?
> **Answer:**
> Virtual method dispatch is the mechanism Java uses to call the correct overridden method at runtime based on the actual object type, not the reference type.
>
> ```java
> Animal animal1 = new Dog();
> Animal animal2 = new Cat();
>
> animal1.sound();  // calls Dog.sound() — determined at runtime
> animal2.sound();  // calls Cat.sound() — determined at runtime
> ```
>
> **How it works internally:**
> - Each class has a **vtable (virtual method table)** listing method addresses
> - At runtime, Java looks up the actual object's vtable to find the correct method
> - This is why overridden methods are called, not parent methods

---

### ❓ What is the difference between reference type and object type?
> **Answer:**
>
> | | Reference Type | Object Type |
> |---|---|---|
> | Definition | Type of the variable (declared type) | Actual class of the object |
> | Determined | At compile time | At runtime |
> | Method call | Resolved based on object type | Resolved based on object type |
>
> ```java
> Animal animal = new Dog();  // reference type: Animal, object type: Dog
>
> // Can call methods from Animal (reference type)
> animal.eat();
>
> // Cannot call Dog-specific methods without downcasting
> // animal.bark();  // compile error
>
> // Can call overridden methods — uses Dog's version
> animal.sound();  // calls Dog.sound()
> ```

---

## 5. Overloading vs Overriding

### ❓ What is method overloading?
> **Answer:**
> Overloading is having multiple methods with the same name but different parameter lists (signature).
>
> **Rules:**
> - Same method name
> - Different parameter count, type, or order
> - Return type can be different (but not the only difference)
> - Resolved at **compile time**
>
> ```java
> class Calculator {
>     int add(int a, int b) { return a + b; }
>     double add(double a, double b) { return a + b; }
>     int add(int a, int b, int c) { return a + b + c; }
> }
>
> Calculator calc = new Calculator();
> calc.add(1, 2);           // calls first method
> calc.add(1.5, 2.5);       // calls second method
> calc.add(1, 2, 3);        // calls third method
> ```

---

### ❓ What is method overriding?
> **Answer:**
> Overriding is redefining a parent method in a subclass with the same signature.
>
> **Rules:**
> - Same method name and parameters
> - Same return type (or covariant return type in Java 5+)
> - Cannot reduce access modifier
> - Resolved at **runtime**
>
> ```java
> class Animal {
>     public void sound() { System.out.println("Generic sound"); }
> }
>
> class Dog extends Animal {
>     @Override
>     public void sound() { System.out.println("Woof!"); }
> }
>
> Animal animal = new Dog();
> animal.sound();  // calls Dog.sound() at runtime
> ```

---

### ❓ Can you override a static method?
> **Answer:**
> No, you cannot override a static method. You can **hide** it, but it's not true overriding.
>
> ```java
> class Parent {
>     static void staticMethod() { System.out.println("Parent static"); }
> }
>
> class Child extends Parent {
>     static void staticMethod() { System.out.println("Child static"); }  // hiding, not overriding
> }
>
> Parent p = new Child();
> p.staticMethod();  // calls Parent.staticMethod() — resolved by reference type
>
> Child c = new Child();
> c.staticMethod();  // calls Child.staticMethod()
> ```

---

### ❓ Can you override a private method?
> **Answer:**
> No, private methods cannot be overridden because they're not inherited.
>
> ```java
> class Parent {
>     private void privateMethod() { System.out.println("Parent private"); }
> }
>
> class Child extends Parent {
>     public void privateMethod() { System.out.println("Child private"); }  // new method, not override
> }
> ```

---

## 6. Annotations

### ❓ What are annotations? What is their purpose?
> **Answer:**
> Annotations are metadata that provide information about the code but don't directly affect the code's operation.
>
> **Purpose:**
> - Provide information to the compiler (e.g., `@Override`)
> - Provide information to build tools and deployment descriptors
> - Provide information to runtime (e.g., Spring's `@Component`)
>
> ```java
> @Override
> public void method() { }
>
> @Deprecated
> public void oldMethod() { }
>
> @FunctionalInterface
> interface MyInterface { }
> ```

---

### ❓ What are the built-in annotations?
> **Answer:**
>
> | Annotation | Purpose |
> |---|---|
> | `@Override` | Marks that a method overrides a parent method |
> | `@Deprecated` | Marks code as outdated, should not be used |
> | `@SuppressWarnings` | Tells compiler to suppress specific warnings |
> | `@FunctionalInterface` | Marks an interface as a functional interface (single abstract method) |
> | `@SafeVarargs` | Suppresses warnings about varargs |
>
> ```java
> @Override
> public String toString() { return "..."; }
>
> @Deprecated
> public void oldMethod() { }
>
> @SuppressWarnings("unchecked")
> List list = new ArrayList();
> ```

---

### ❓ What are retention policies? What are the types?
> **Answer:**
> Retention policy determines how long an annotation is retained.
>
> **Types:**
> - **SOURCE** — discarded by compiler, not in bytecode
> - **CLASS** — in bytecode, not available at runtime (default)
> - **RUNTIME** — available at runtime via reflection
>
> ```java
> @Retention(RetentionPolicy.SOURCE)
> @interface CompileTimeOnly { }
>
> @Retention(RetentionPolicy.RUNTIME)
> @interface RuntimeAnnotation { }
> ```

---

### ❓ How do you create a custom annotation?
> **Answer:**
> ```java
> @Target(ElementType.METHOD)
> @Retention(RetentionPolicy.RUNTIME)
> public @interface MyAnnotation {
>     String value() default "default value";
>     int count() default 0;
> }
>
> class MyClass {
>     @MyAnnotation(value = "test", count = 5)
>     public void myMethod() { }
> }
> ```

---

## 7. Reflection

### ❓ What is reflection? What are its uses?
> **Answer:**
> Reflection is the ability to inspect and manipulate classes, methods, fields, and constructors at runtime.
>
> **Uses:**
> - Frameworks (Spring, Hibernate, JUnit) use reflection for dependency injection, ORM, testing
> - Serialization/deserialization
> - Dynamic proxy creation
> - Plugin systems
>
> ```java
> Class<?> clazz = Class.forName("java.lang.String");
> Method[] methods = clazz.getMethods();
> Field[] fields = clazz.getDeclaredFields();
> Constructor<?>[] constructors = clazz.getConstructors();
> ```

---

### ❓ How do you get a Class object?
> **Answer:**
> Three ways to get a `Class` object:
>
> ```java
> // 1. Using .class
> Class<?> clazz = String.class;
>
> // 2. Using Class.forName()
> Class<?> clazz = Class.forName("java.lang.String");
>
> // 3. Using getClass() on an object
> String str = "hello";
> Class<?> clazz = str.getClass();
> ```

---

### ❓ How do you invoke a method using reflection?
> **Answer:**
> ```java
> class MyClass {
>     public void greet(String name) {
>         System.out.println("Hello, " + name);
>     }
> }
>
> MyClass obj = new MyClass();
> Class<?> clazz = MyClass.class;
>
> // Get the method
> Method method = clazz.getMethod("greet", String.class);
>
> // Invoke it
> method.invoke(obj, "Alice");  // prints "Hello, Alice"
> ```

---

### ❓ How do you access private fields using reflection?
> **Answer:**
> ```java
> class MyClass {
>     private String secret = "hidden";
> }
>
> MyClass obj = new MyClass();
> Field field = MyClass.class.getDeclaredField("secret");
>
> field.setAccessible(true);  // bypass private access
> String value = (String) field.get(obj);  // get value
> field.set(obj, "new value");  // set value
> ```

---

### ❓ What are the performance implications of reflection?
> **Answer:**
> Reflection is **slower** than direct method calls because:
> - Method lookup happens at runtime (not compile time)
> - Type checking is deferred
> - JVM cannot optimize as aggressively
>
> **Performance tips:**
> - Cache `Class` objects and `Method` objects
> - Avoid reflection in tight loops
> - Use reflection only when necessary (e.g., framework initialization)
>
> ```java
> // BAD — reflection in loop
> for (int i = 0; i < 1000000; i++) {
>     Method method = MyClass.class.getMethod("getValue");
>     method.invoke(obj);
> }
>
> // GOOD — cache the method
> Method method = MyClass.class.getMethod("getValue");
> for (int i = 0; i < 1000000; i++) {
>     method.invoke(obj);
> }
> ```

---

### ❓ How do Spring and Hibernate use reflection?
> **Answer:**
>
> **Spring:**
> - Scans classpath for `@Component`, `@Service`, `@Repository` annotations
> - Uses reflection to instantiate beans and inject dependencies
> - Reads `@Autowired` to determine which fields/constructors to inject
>
> **Hibernate:**
> - Reads `@Entity`, `@Column`, `@ManyToOne` annotations on entity classes
> - Uses reflection to access private fields and map them to database columns
> - Dynamically generates SQL based on entity structure
>
> ```java
> @Component
> public class UserService {
>     @Autowired
>     private UserRepository repo;  // Spring uses reflection to inject this
> }
>
> @Entity
> public class User {
>     @Column(name = "user_name")
>     private String name;  // Hibernate uses reflection to map to DB
> }
> ```

---

*Last updated: October 2026 | Good luck with your interview! 🚀*
