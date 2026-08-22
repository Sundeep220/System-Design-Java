Absolutely. **Reflection** is one of the most important advanced Java concepts because it connects directly to how frameworks like Spring, Hibernate, JUnit, Jackson, Mockito, and many dependency-injection containers work internally.

Since we're mastering these concepts one by one, let's go deep.

# Reflection in Java

## 1. What is Reflection?

**Reflection is the ability of a Java program to inspect and manipulate classes, methods, fields, constructors, and annotations at runtime.**

Normally, Java code knows what it wants to access at compile time:

```java
User user = new User();
user.getName();
```

With reflection, you can discover things **at runtime**:

```java
Class<?> clazz = user.getClass();

System.out.println(clazz.getName());

for (var method : clazz.getDeclaredMethods()) {
    System.out.println(method.getName());
}
```

The important idea is:

> **Normal Java:** code → compile time → known classes/methods
> **Reflection:** code → runtime → discover classes/methods/fields dynamically

---

# 2. Why do we need Reflection?

Imagine Spring Boot.

You write:

```java
@Service
public class PaymentService {

    public void pay() {
        System.out.println("Payment");
    }
}
```

And somewhere:

```java
@Autowired
private PaymentService paymentService;
```

You don't explicitly write:

```java
PaymentService service = new PaymentService();
```

So how does Spring know:

* which classes are beans?
* which fields have `@Autowired`?
* which constructor should be called?
* which methods have `@Transactional`?
* which annotations exist?

Reflection is one of the mechanisms that allows frameworks to inspect your code at runtime.

For example, conceptually Spring can do:

```java
Class<?> clazz = PaymentService.class;

for (var annotation : clazz.getAnnotations()) {
    System.out.println(annotation);
}
```

and:

```java
for (var field : clazz.getDeclaredFields()) {
    if (field.isAnnotationPresent(Autowired.class)) {
        // inject dependency
    }
}
```

That's the basic reason reflection matters for a backend developer.

---

# 3. The `Class<?>` object

The foundation of Java reflection is:

```java
java.lang.Class
```

Every Java class has a corresponding `Class` object at runtime.

For:

```java
class User {
}
```

the JVM maintains metadata describing that class.

You can obtain the `Class` object in several ways.

## Method 1 — `.class`

```java
Class<User> clazz = User.class;
```

---

## Method 2 — `getClass()`

```java
User user = new User();

Class<?> clazz = user.getClass();
```

---

## Method 3 — `Class.forName()`

```java
Class<?> clazz = Class.forName("com.example.User");
```

This is particularly important because the class name can come dynamically from configuration.

For example:

```java
String className = "com.example.PaymentService";

Class<?> clazz = Class.forName(className);
```

This is one of the mechanisms behind plugin architectures.

---

# 4. What information can Reflection inspect?

A `Class` object can expose information about:

```text
Class
 ├── Name
 ├── Package
 ├── Modifiers
 ├── Superclass
 ├── Interfaces
 ├── Constructors
 ├── Fields
 ├── Methods
 ├── Annotations
 ├── Generic information
 └── Nested classes
```

For example:

```java
Class<?> clazz = User.class;

System.out.println(clazz.getName());
System.out.println(clazz.getSuperclass());
System.out.println(clazz.getModifiers());
```

---

# 5. Inspecting class information

Suppose:

```java
public class User {

    private String name;
    private int age;

    public void login() {
    }
}
```

We can inspect it.

```java
Class<User> clazz = User.class;

System.out.println("Name: " + clazz.getName());

System.out.println(
        "Simple Name: " + clazz.getSimpleName()
);

System.out.println(
        "Package: " + clazz.getPackageName()
);

System.out.println(
        "Superclass: " + clazz.getSuperclass()
);
```

Output conceptually:

```text
Name: com.example.User
Simple Name: User
Package: com.example
Superclass: class java.lang.Object
```

---

# 6. Inspecting fields

```java
Field[] fields = clazz.getDeclaredFields();

for (Field field : fields) {
    System.out.println(field.getName());
}
```

Output:

```text
name
age
```

You can also inspect:

```java
field.getType()
```

For example:

```java
for (Field field : clazz.getDeclaredFields()) {

    System.out.println(
        field.getName() + " : " + field.getType()
    );
}
```

Output:

```text
name : class java.lang.String
age : int
```

---

# 7. `getFields()` vs `getDeclaredFields()`

This distinction is **very important for interviews**.

### `getFields()`

Returns:

> public fields, including inherited public fields.

### `getDeclaredFields()`

Returns:

> fields declared directly inside the class, regardless of visibility.

Example:

```java
class Parent {

    public int a;
}

class Child extends Parent {

    private int b;
}
```

```java
Child.class.getFields();
```

can find:

```text
a
```

because `a` is public and inherited.

But:

```java
Child.class.getDeclaredFields();
```

finds:

```text
b
```

because `b` is declared directly in `Child`.

It does **not** automatically traverse the inheritance hierarchy.

---

# 8. Inspecting methods

```java
Method[] methods = clazz.getDeclaredMethods();

for (Method method : methods) {
    System.out.println(method.getName());
}
```

You can inspect:

```java
method.getName()
method.getReturnType()
method.getParameterTypes()
method.getModifiers()
method.getAnnotations()
```

Example:

```java
for (Method method : clazz.getDeclaredMethods()) {

    System.out.println("Method: " + method.getName());

    System.out.println(
        "Return type: " + method.getReturnType()
    );

    System.out.println(
        "Parameters: " +
        Arrays.toString(method.getParameterTypes())
    );
}
```

---

# 9. `getMethods()` vs `getDeclaredMethods()`

Again, very important.

### `getMethods()`

Returns public methods:

* declared by the class
* inherited from parent classes
* inherited from interfaces

### `getDeclaredMethods()`

Returns methods declared directly in that class, regardless of visibility.

Example:

```java
class Parent {

    public void parentMethod() {}
}

class Child extends Parent {

    private void childMethod() {}
}
```

```java
Child.class.getMethods();
```

can include:

```text
parentMethod
```

But:

```java
Child.class.getDeclaredMethods();
```

includes:

```text
childMethod
```

---

# 10. Invoking a method dynamically

This is where Reflection becomes really powerful.

Normally:

```java
User user = new User();

user.login();
```

Reflection:

```java
User user = new User();

Method method =
        User.class.getDeclaredMethod("login");

method.invoke(user);
```

The flow is:

```text
User.class
     ↓
find method named "login"
     ↓
Method object
     ↓
invoke()
     ↓
user.login()
```

---

# 11. Invoking methods with parameters

Suppose:

```java
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }
}
```

Reflection:

```java
Calculator calculator = new Calculator();

Method method =
        Calculator.class.getDeclaredMethod(
                "add",
                int.class,
                int.class
        );

Object result =
        method.invoke(calculator, 10, 20);

System.out.println(result);
```

Output:

```text
30
```

Notice something important:

```java
Object result
```

Reflection APIs often return `Object` because the method's return type isn't necessarily known statically.

---

# 12. Invoking private methods

Suppose:

```java
class User {

    private void secret() {
        System.out.println("Secret");
    }
}
```

Normally:

```java
user.secret();
```

doesn't compile.

Reflection can access it by overriding Java language access checks:

```java
Method method =
        User.class.getDeclaredMethod("secret");

method.setAccessible(true);

method.invoke(user);
```

Modern Java has additional strong encapsulation/module considerations, so `setAccessible(true)` isn't a universal bypass anymore.

But conceptually:

```text
private
   ↓
Reflection
   ↓
access check override
   ↓
invoke
```

---

# 13. Reflection and constructors

You can inspect constructors:

```java
Constructor<?>[] constructors =
        User.class.getDeclaredConstructors();

for (Constructor<?> constructor : constructors) {
    System.out.println(constructor);
}
```

You can retrieve a specific constructor:

```java
Constructor<User> constructor =
        User.class.getDeclaredConstructor(
                String.class,
                int.class
        );
```

Then create an object:

```java
User user =
        constructor.newInstance("Sundeep", 25);
```

So instead of:

```java
new User("Sundeep", 25);
```

you can dynamically do:

```text
Class
 ↓
Constructor
 ↓
newInstance()
 ↓
Object
```

---

# 14. Reflection and private fields

Suppose:

```java
class User {

    private String name = "Sundeep";
}
```

You can retrieve it:

```java
User user = new User();

Field field =
        User.class.getDeclaredField("name");
```

Make it accessible:

```java
field.setAccessible(true);
```

Read it:

```java
Object value = field.get(user);

System.out.println(value);
```

Output:

```text
Sundeep
```

You can even modify it:

```java
field.set(user, "Rahul");
```

Then:

```java
System.out.println(field.get(user));
```

prints:

```text
Rahul
```

This demonstrates why reflection can be powerful **and dangerous**.

---

# 15. Reflection + annotations

This is probably the most important practical use for Spring developers.

Suppose:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface MyAnnotation {

    String value();
}
```

Then:

```java
class PaymentService {

    @MyAnnotation("payment")
    public void pay() {
    }
}
```

Reflection:

```java
Method method =
        PaymentService.class.getDeclaredMethod("pay");
```

Check annotation:

```java
if (method.isAnnotationPresent(MyAnnotation.class)) {

    MyAnnotation annotation =
            method.getAnnotation(MyAnnotation.class);

    System.out.println(annotation.value());
}
```

Output:

```text
payment
```

This basic mechanism is extremely important for understanding:

```text
Spring
Hibernate
JUnit
Jackson
Mockito
Lombok
JPA
```

and many other frameworks.

---

# 16. Reflection + custom framework

Let's build a tiny framework ourselves.

Suppose we create:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface GET {
}
```

Then:

```java
class UserController {

    @GET
    public void getUsers() {
        System.out.println("Users");
    }

    public void helper() {
    }
}
```

Our framework can inspect:

```java
Class<?> controller =
        UserController.class;

for (Method method :
        controller.getDeclaredMethods()) {

    if (method.isAnnotationPresent(GET.class)) {

        System.out.println(
                "Found GET endpoint: "
                + method.getName()
        );
    }
}
```

Output:

```text
Found GET endpoint: getUsers
```

This is conceptually similar to what web frameworks do.

---

# 17. Reflection and Dependency Injection

Let's create a tiny version of dependency injection.

```java
class Engine {

    public void start() {
        System.out.println("Engine started");
    }
}
```

```java
class Car {

    private Engine engine;

    public void drive() {
        engine.start();
    }
}
```

Suppose we create:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Inject {
}
```

Then:

```java
class Car {

    @Inject
    private Engine engine;

    public void drive() {
        engine.start();
    }
}
```

Our container could do:

```java
Car car = new Car();

for (Field field :
        Car.class.getDeclaredFields()) {

    if (field.isAnnotationPresent(Inject.class)) {

        Object dependency =
                field.getType()
                     .getDeclaredConstructor()
                     .newInstance();

        field.setAccessible(true);

        field.set(car, dependency);
    }
}
```

Now:

```java
car.drive();
```

works.

We just built a **tiny dependency injection mechanism**.

That's essentially the conceptual bridge to:

```text
Spring
    ↓
Component scanning
    ↓
Reflection
    ↓
Bean creation
    ↓
Dependency injection
```

Spring's real implementation is vastly more sophisticated, but this gives you the mental model.

---

# 18. Reflection and Spring

When you write:

```java
@Service
public class OrderService {
}
```

Spring needs to discover this class.

Conceptually:

```text
Classpath
    ↓
Find classes
    ↓
Load Class objects
    ↓
Inspect annotations
    ↓
@Service found
    ↓
Create bean
    ↓
Store in ApplicationContext
```

Then:

```java
@Autowired
private PaymentService paymentService;
```

conceptually:

```text
Find field
     ↓
Check @Autowired
     ↓
Determine required type
     ↓
Find matching bean
     ↓
Inject dependency
```

Reflection is heavily involved in these kinds of runtime operations, although modern Spring also uses other mechanisms such as generated code and proxies.

---

# 19. Reflection and Hibernate/JPA

Consider:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
}
```

Hibernate needs to understand:

```text
User
 ↓
@Entity
 ↓
id → @Id
 ↓
name → column
```

Reflection can inspect:

```java
User.class.getDeclaredFields();
```

and:

```java
field.isAnnotationPresent(Id.class)
```

Then Hibernate can build metadata describing how the Java class maps to the database.

Conceptually:

```text
Java class
     ↓
Reflection
     ↓
Annotations + fields
     ↓
Entity metadata
     ↓
SQL mapping
```

---

# 20. Reflection and Jackson

Suppose:

```java
class User {

    private String name;
    private int age;
}
```

And JSON:

```json
{
  "name": "Sundeep",
  "age": 25
}
```

Jackson needs to map:

```text
JSON name
    ↓
Java field/property
```

Reflection can help inspect fields, constructors, getters/setters, annotations, etc.

For example:

```java
@JsonProperty("user_name")
private String name;
```

Jackson can inspect that annotation and determine the JSON mapping.

Again:

```text
JSON
 ↓
Jackson
 ↓
Class metadata
 ↓
Reflection / generated accessors
 ↓
Object
```

---

# 21. Reflection and JUnit

Consider:

```java
@Test
void shouldCreateUser() {
}
```

You don't manually call:

```java
shouldCreateUser();
```

JUnit discovers test methods.

Conceptually:

```java
for (Method method :
        testClass.getDeclaredMethods()) {

    if (method.isAnnotationPresent(Test.class)) {

        method.invoke(testInstance);
    }
}
```

So annotations + reflection are fundamental to the programming model.

---

# 22. Reflection and method overloading

Suppose:

```java
class Calculator {

    public void add(int a, int b) {}

    public void add(double a, double b) {}

    public void add(String a, String b) {}
}
```

You cannot simply:

```java
getDeclaredMethod("add")
```

because there are multiple methods.

You specify parameter types:

```java
Method method =
        Calculator.class.getDeclaredMethod(
                "add",
                int.class,
                int.class
        );
```

This is how reflection distinguishes overloaded methods.

---

# 23. Reflection and inheritance

Reflection APIs have different behavior regarding inherited members.

Remember this distinction:

```text
getMethods()
       ↓
public methods
including inherited methods

getDeclaredMethods()
       ↓
methods declared in THIS class
regardless of visibility
```

Similarly:

```text
getFields()
       ↓
public fields including inherited

getDeclaredFields()
       ↓
fields declared in this class
```

For constructors:

```text
getConstructors()
       ↓
public constructors

getDeclaredConstructors()
       ↓
all constructors declared in class
```

---

# 24. Reflection and interfaces

You can inspect interfaces implemented by a class:

```java
Class<?>[] interfaces =
        UserService.class.getInterfaces();

for (Class<?> i : interfaces) {
    System.out.println(i.getName());
}
```

You can inspect superclass:

```java
Class<?> parent =
        UserService.class.getSuperclass();
```

You can also check:

```java
UserService.class.isAssignableFrom(
        AdminUserService.class
);
```

This becomes useful when dynamically determining whether a class satisfies a contract.

---

# 25. Reflection and modifiers

You can inspect whether something is:

```text
public
private
protected
static
final
abstract
interface
```

Example:

```java
int modifiers = field.getModifiers();

if (Modifier.isPrivate(modifiers)) {
    System.out.println("Private field");
}
```

Or:

```java
if (Modifier.isStatic(modifiers)) {
    System.out.println("Static field");
}
```

---

# 26. Reflection and generics

Reflection can also inspect generic metadata.

Suppose:

```java
class Repository {

    private List<User> users;
}
```

Calling:

```java
Field field =
        Repository.class.getDeclaredField("users");
```

gives:

```java
field.getType()
```

which gives approximately:

```text
List
```

But:

```java
field.getGenericType()
```

can provide:

```text
List<User>
```

This distinction matters.

```text
getType()
     ↓
raw runtime class

getGenericType()
     ↓
generic type metadata
```

---

# 27. Type erasure connection

This leads directly to Java generics and type erasure.

At runtime:

```java
List<User>
```

does not generally retain the full generic type in the same way a normal class type does.

For example:

```java
List<String>
List<Integer>
```

both have runtime class:

```text
java.util.ArrayList
```

However, generic declarations can leave metadata in the class file, which reflection can sometimes inspect using APIs like:

```java
getGenericType()
getGenericReturnType()
getGenericParameterTypes()
```

This distinction is an important interview topic.

---

# 28. Reflection and class loading

Reflection is closely connected to the JVM class-loading process.

When you do:

```java
Class.forName("com.example.User");
```

the JVM may:

```text
Class name
    ↓
ClassLoader
    ↓
Load .class
    ↓
Link
    ↓
Initialize
    ↓
Class object
```

This is why reflection is strongly connected to the JVM topics you're studying.

The rough relationship is:

```text
.class file
     ↓
ClassLoader
     ↓
JVM
     ↓
Class object
     ↓
Reflection API
```

---

# 29. Reflection is runtime-based

This is the key difference from normal Java.

Normal:

```java
User user = new User();
user.login();
```

The compiler knows:

```text
User
login()
```

Reflection:

```java
Class<?> clazz = Class.forName(className);

Method method =
        clazz.getDeclaredMethod(methodName);

method.invoke(object);
```

The compiler may know only:

```text
className = String
methodName = String
```

The actual class/method can be determined **at runtime**.

That's what makes reflection dynamic.

---

# 30. Advantages of Reflection

### 1. Dynamic behavior

You can determine classes and methods at runtime.

### 2. Framework development

Extremely useful for:

```text
DI containers
ORMs
Testing frameworks
Serialization
Web frameworks
Plugin systems
```

### 3. Extensibility

Applications can load implementations dynamically.

Example:

```text
PaymentProcessor
       ↑
       |
-----------------------
|          |          |
Stripe    PayPal     Razorpay
```

Configuration could specify:

```text
com.example.PayPalProcessor
```

and your application can load it dynamically.

---

# 31. Disadvantages of Reflection

This is equally important.

## 1. Performance overhead

Normal:

```java
calculator.add(10, 20);
```

Reflection:

```java
method.invoke(calculator, 10, 20);
```

Reflection adds runtime machinery.

Modern JVMs and reflection implementations have improved significantly, but direct invocation is still generally preferable on hot paths.

---

## 2. Loss of compile-time safety

Normal:

```java
calculator.add(10, 20);
```

Compiler catches many mistakes.

Reflection:

```java
method.invoke(object, ...);
```

Problems may only appear at runtime.

For example:

```text
NoSuchMethodException
IllegalAccessException
InvocationTargetException
IllegalArgumentException
```

---

## 3. Breaks encapsulation

Reflection can potentially access private implementation details.

That makes code more tightly coupled to internal structure.

---

## 4. Harder to understand

This:

```java
service.process();
```

is obvious.

This:

```java
method.invoke(
    object,
    arguments
);
```

requires understanding the metadata and runtime behavior.

---

## 5. Refactoring problems

Suppose:

```java
method.invoke(object);
```

uses:

```text
"processPayment"
```

and someone renames:

```text
processPayment()
```

to:

```text
executePayment()
```

The compiler may not catch the string-based reference.

That's one of the biggest practical drawbacks.

---

# 32. Reflection exceptions

You should know these.

### `ClassNotFoundException`

```java
Class.forName("com.example.DoesNotExist");
```

Class cannot be found.

---

### `NoSuchMethodException`

```java
clazz.getDeclaredMethod("abc");
```

Method doesn't exist.

---

### `NoSuchFieldException`

```java
clazz.getDeclaredField("abc");
```

Field doesn't exist.

---

### `IllegalAccessException`

You don't have appropriate access.

---

### `InvocationTargetException`

The method you invoked threw an exception.

This one is particularly important.

Suppose:

```java
method.invoke(object);
```

and the underlying method does:

```java
throw new RuntimeException();
```

Reflection can wrap that underlying exception inside:

```text
InvocationTargetException
```

You can inspect:

```java
exception.getCause();
```

to find the original exception.

---

# 33. Complete Reflection Example

Let's put everything together.

```java
import java.lang.reflect.*;
import java.util.Arrays;

class User {

    private String name;

    public User(String name) {
        this.name = name;
    }

    public void greet(String message) {
        System.out.println(message + ", " + name);
    }

    private void secret() {
        System.out.println("Secret method");
    }
}

public class ReflectionDemo {

    public static void main(String[] args)
            throws Exception {

        Class<User> clazz = User.class;

        // Class information
        System.out.println("Class: "
                + clazz.getName());

        System.out.println("Superclass: "
                + clazz.getSuperclass());

        // Fields
        System.out.println("\nFields:");

        for (Field field :
                clazz.getDeclaredFields()) {

            System.out.println(
                    field.getName()
                    + " : "
                    + field.getType()
            );
        }

        // Constructors
        System.out.println("\nConstructors:");

        for (Constructor<?> constructor :
                clazz.getDeclaredConstructors()) {

            System.out.println(constructor);
        }

        // Create object
        Constructor<User> constructor =
                clazz.getDeclaredConstructor(
                        String.class
                );

        User user =
                constructor.newInstance("Sundeep");

        // Method invocation
        Method greet =
                clazz.getDeclaredMethod(
                        "greet",
                        String.class
                );

        greet.invoke(user, "Hello");

        // Private method
        Method secret =
                clazz.getDeclaredMethod("secret");

        secret.setAccessible(true);

        secret.invoke(user);
    }
}
```

Conceptually:

```text
                 User.class
                     │
       ┌─────────────┼──────────────┐
       ↓             ↓              ↓
    Fields      Constructors      Methods
       │             │              │
       ↓             ↓              ↓
  Field object   Constructor      Method
                     │              │
                     ↓              ↓
               newInstance()     invoke()
                     │              │
                     └──────┬───────┘
                            ↓
                         Object
```

---

# 34. Reflection vs Normal Java

| Normal Java             | Reflection                             |
| ----------------------- | -------------------------------------- |
| Compile-time knowledge  | Runtime discovery                      |
| Faster/direct           | More overhead                          |
| Type-safe               | Less type-safe                         |
| Easier to understand    | More complex                           |
| Refactoring friendly    | String-based references can be fragile |
| Preserves encapsulation | Can bypass access checks               |
| Used in business logic  | Mostly infrastructure/framework code   |

---

# 35. When should YOU use Reflection?

As a Spring Boot backend developer:

### Usually DON'T use it for normal application code

Don't unnecessarily write:

```java
Method method =
    service.getClass()
           .getDeclaredMethod("process");

method.invoke(service);
```

when you could simply do:

```java
service.process();
```

Prefer normal Java.

### Use reflection when building infrastructure

Reflection makes sense for:

```text
Frameworks
Dependency injection
ORM
Serialization
Testing tools
Plugin systems
Generic utilities
Annotation processing at runtime
Object mappers
```

---

# 36. Reflection vs Annotation Processing

This distinction is important as you progress.

### Reflection

Mostly:

```text
Runtime
```

Example:

```java
method.isAnnotationPresent(MyAnnotation.class)
```

### Annotation processing

Mostly:

```text
Compile time
```

An annotation processor can inspect source/code metadata and generate code during compilation.

Modern Java frameworks increasingly prefer generated code or build-time processing where possible because it can improve:

* startup time
* runtime performance
* native-image compatibility
* type safety

So don't think:

> "Every framework uses reflection everywhere."

Rather:

> **Reflection is one important runtime mechanism, but modern frameworks often combine reflection with generated/proxied/build-time mechanisms.**

---

# 37. The most important mental model

You should remember this pipeline:

```text
                    COMPILE TIME
                         │
                         ↓
                    .class file
                         │
                         ↓
                    CLASS LOADING
                         │
                         ↓
                       JVM
                         │
                         ↓
                    Class object
                         │
             ┌───────────┼────────────┐
             ↓           ↓            ↓
           Fields      Methods    Constructors
             │           │            │
             └───────────┼────────────┘
                         ↓
                    Reflection
                         │
              ┌──────────┼───────────┐
              ↓          ↓           ↓
           inspect      invoke      create
              │          │           │
              └──────────┼───────────┘
                         ↓
                      Runtime
```

And the framework connection:

```text
Spring Boot
    │
    ├── Classpath scanning
    │
    ├── Class metadata
    │
    ├── Annotations
    │
    ├── Reflection
    │
    ├── Proxies
    │
    └── Generated infrastructure
              │
              ↓
        Application behavior
```

---

# 38. Interview questions you should be able to answer

After mastering Reflection, you should be comfortable answering:

1. **What is Reflection in Java?**
2. Why is reflection required by frameworks?
3. What is `Class<?>`?
4. Different ways to obtain a `Class` object?
5. `getMethods()` vs `getDeclaredMethods()`?
6. `getFields()` vs `getDeclaredFields()`?
7. How do you invoke a method dynamically?
8. How do you instantiate an object using reflection?
9. How do you access a private field?
10. What does `setAccessible(true)` do?
11. What is `InvocationTargetException`?
12. How does reflection work with annotations?
13. How does Spring use reflection?
14. How does Hibernate use reflection?
15. How does JUnit discover test methods?
16. Reflection vs annotation processing?
17. What are the disadvantages of reflection?
18. Why shouldn't reflection be used everywhere?
19. How does `Class.forName()` relate to class loading?
20. How does reflection interact with generics and type erasure?

---

# 39. What you should actually code to master it

Don't just memorize the APIs. Build these **five mini exercises**:

### Exercise 1 — Class Inspector

Create:

```java
inspectClass(Class<?> clazz)
```

and print:

```text
Class name
Package
Superclass
Interfaces
Modifiers
Fields
Methods
Constructors
```

### Exercise 2 — Dynamic Object Factory

Create:

```java
Object create(String className)
```

which:

```text
class name
    ↓
Class.forName()
    ↓
constructor
    ↓
newInstance()
    ↓
object
```

### Exercise 3 — Annotation-based command runner

Create:

```java
@Command("createUser")
public void createUser()
```

and dynamically find and execute commands based on:

```java
runner.execute("createUser");
```

### Exercise 4 — Tiny Dependency Injection Container

Build:

```java
@Inject
private PaymentService paymentService;
```

and have your container automatically instantiate and inject dependencies.

This one will **really cement your understanding of Spring DI**.

### Exercise 5 — Mini JUnit

Create:

```java
@Test
void testAddition() {}
```

Then build your own test runner:

```text
Find @Test methods
       ↓
Create test object
       ↓
Invoke methods
       ↓
Capture exceptions
       ↓
Report PASS/FAIL
```

If you can build those five, you don't just "know Reflection"—you'll understand **why Spring/JUnit/Hibernate are architected the way they are**.

**Next in your original sequence is `Annotations`**, and there's a very useful connection here: we can take Reflection + Annotations together and build a **mini Spring-like DI container**. That would be an excellent hands-on transition before moving on.


Yes — and there is an important **version correction** to make to what you asked.

It is **not Java 25 that makes this impossible**. In Java 25, deep reflection can still mutate many `final` **instance fields**. The major change starts in **JDK 26**: Java begins restricting reflective mutation of final fields, initially with warnings by default, and the restriction is intended to become an exception in a future release. ([OpenJDK][1])

This is directly connected to the Reflection topic we just discussed.

---

# 1. First: what does `final` normally mean?

Consider:

```java
class User {

    private final String name;

    User(String name) {
        this.name = name;
    }
}
```

Normally:

```java
User user = new User("Sundeep");
```

After construction, this is illegal:

```java
user.name = "Rahul"; // compilation error
```

That's the whole point of `final`.

For an **instance final field**, Java's normal rules are essentially:

```text
Object creation
      ↓
constructor
      ↓
final field initialized
      ↓
final field should not be reassigned
```

At the JVM bytecode level, ordinary instance-final writes are restricted to the constructor (`<init>`). ([OpenJDK][2])

---

# 2. But Reflection used to provide a loophole

Before the new restrictions, Java Reflection allowed something surprising.

Suppose:

```java
class User {

    private final String name;

    User(String name) {
        this.name = name;
    }
}
```

Normally:

```java
User user = new User("Sundeep");
```

`name` is final.

But before the new restrictions, you could do:

```java
Field field = User.class.getDeclaredField("name");

field.setAccessible(true);

field.set(user, "Rahul");
```

So:

```text
Before:

final field
    ↓
setAccessible(true)
    ↓
Field.set(...)
    ↓
value changed
```

This was called **deep reflective mutation of final fields**.

JDK 5 and later allowed this for many final instance fields. ([OpenJDK][1])

---

# 3. Let's see the complete example

```java
class User {

    private final String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

Then:

```java
public class Main {

    public static void main(String[] args)
            throws Exception {

        User user = new User("Sundeep");

        System.out.println(user.getName());

        Field field =
                User.class.getDeclaredField("name");

        field.setAccessible(true);

        field.set(user, "Rahul");

        System.out.println(user.getName());
    }
}
```

Historically, this could produce:

```text
Sundeep
Rahul
```

Even though:

```java
name
```

was declared:

```java
private final
```

That's the surprising part of Java Reflection.

---

# 4. Why was Java allowing this?

There were legitimate use cases.

The biggest one was **serialization/deserialization**.

Imagine a library needs to reconstruct:

```java
class User {

    private final String name;
    private final int age;
}
```

without calling the normal constructor.

Historically, serialization frameworks could create an object and then populate final fields reflectively.

Conceptually:

```text
Serialized data
      ↓
Create object
      ↓
Set fields reflectively
      ↓
Object reconstructed
```

That's one reason Java historically tolerated this capability.

JEP 500 explicitly notes serialization as a legitimate use case that needs to continue working. ([OpenJDK][1])

---

# 5. So what's wrong with allowing it?

Because `final` has an important meaning for the JVM.

When Java sees:

```java
private final int age;
```

the JVM/compiler/JIT can reason:

> This value is not going to arbitrarily change after construction.

That allows various optimizations and makes reasoning about immutable objects easier.

But reflection says:

> Surprise! I can change it later.

That creates a conflict.

```text
Java language:

final
 ↓
should not change


Reflection:

final
 ↓
actually can change


JVM/JIT:

Can I trust final?
 ↓
Not completely
```

OpenJDK explicitly identifies this as a problem for both **correctness and optimization**. ([OpenJDK][1])

---

# 6. The problem becomes worse with concurrency

Imagine:

```java
class User {

    final String name;

    User(String name) {
        this.name = name;
    }
}
```

One thread sees:

```text
name = "Sundeep"
```

Then some other code reflectively changes it:

```text
name = "Rahul"
```

Now code can potentially observe behavior that contradicts the assumptions normally associated with final fields.

That's particularly problematic because final fields have special semantics around object initialization and visibility.

So Java wants:

```text
final
 ↓
TRUST ME
```

rather than:

```text
final
 ↓
maybe final
 ↓
unless reflection changes it
```

---

# 7. Important: Java 25 still permits this

This is where your question needs correction.

In **Java 25**, `Field.set()` still allows writing to a non-static final instance field when the necessary access conditions are met.

The Java 25 API explicitly says that a final field has write access when:

* `setAccessible(true)` succeeded
* the field is non-static
* the declaring class isn't a hidden class
* the declaring class isn't a record

([Oracle Docs][3])

So this:

```java
field.setAccessible(true);
field.set(user, "Rahul");
```

can still work for an ordinary final instance field in Java 25.

---

# 8. What changed in Java 26?

This is where **JEP 500 — Prepare to Make Final Mean Final** comes in.

JEP 500 was delivered for **JDK 26**. ([OpenJDK][1])

Starting in JDK 26, Java introduces restrictions around reflective mutation of final fields.

The default behavior initially is:

```text
Attempt to mutate final field
             ↓
       mutation occurs
             +
          WARNING
```

rather than immediately:

```text
Exception
```

The long-term plan is:

```text
Attempt to mutate final field
             ↓
        IllegalAccessException
```

by default.

([OpenJDK][1])

---

# 9. Why did Java choose a gradual approach?

Because there are potentially many existing libraries doing this.

Imagine:

```text
Your application
      ↓
Jackson
      ↓
Some serialization library
      ↓
Reflection
      ↓
final field mutation
```

If Java suddenly broke all of that, applications could fail after upgrading the JDK.

So the migration strategy is:

```text
Older Java
     ↓
mutation allowed

JDK 26
     ↓
mutation restricted
     ↓
warning by default

Future JDK
     ↓
mutation denied by default
```

This gives libraries time to stop depending on the old behavior. ([OpenJDK][1])

---

# 10. The new command-line option

JDK 26 introduces:

```bash
--enable-final-field-mutation
```

For classpath code:

```bash
java --enable-final-field-mutation=ALL-UNNAMED MyApplication
```

This explicitly tells the JVM:

> I understand that this application needs final-field mutation.

Then reflective mutation can be enabled subject to the relevant access/module conditions. ([OpenJDK][1])

This is an important distinction:

**Java isn't saying:**

> Reflection can never mutate final fields.

It's moving toward:

> Reflection cannot mutate final fields **unless the application explicitly opts into that capability**.

---

# 11. There is another option

JDK 26 also introduces:

```bash
--illegal-final-field-mutation
```

with modes such as:

```bash
--illegal-final-field-mutation=warn
```

or:

```bash
--illegal-final-field-mutation=debug
```

or:

```bash
--illegal-final-field-mutation=deny
```

`deny` causes reflective mutation attempts to fail with `IllegalAccessException`. ([Oracle Docs][4])

This is useful for testing whether your application or libraries depend on this old behavior.

---

# 12. Why `setAccessible(true)` is no longer enough

This is the key change.

Previously, conceptually:

```java
Field field = ...;

field.setAccessible(true);

field.set(object, value);
```

could be enough.

New model:

```text
setAccessible(true)
       ↓
Does caller have access?
       ↓
YES
       ↓
Is final-field mutation enabled?
       ↓
YES → mutation allowed
NO  → illegal mutation
```

So:

```java
setAccessible(true)
```

does **not automatically mean**:

```text
"I can modify any final field."
```

anymore.

---

# 13. Static final fields are a different story

This is another important interview point.

Consider:

```java
class Config {

    private static final String VERSION = "1.0";
}
```

Historically, Java Reflection did **not** generally allow arbitrary reflective mutation of `static final` fields.

The Java 25 API classifies static final fields as **non-modifiable final fields**. ([Oracle Docs][3])

So don't think:

```text
final
 ↓
Reflection can always change it
```

That's incorrect.

A better model is:

```text
final instance field
       ↓
Historically mutable through deep reflection
       ↓
Now being restricted


static final field
       ↓
Already protected from reflective mutation
```

---

# 14. Records are also special

Consider:

```java
record User(String name, int age) {
}
```

Record components are backed by final fields.

Reflection cannot use `Field.set()` to modify those final fields.

Java explicitly treats final fields declared in records as non-modifiable. ([Oracle Docs][3])

So:

```java
User user = new User("Sundeep", 25);
```

You cannot do:

```text
Reflection
   ↓
change user.name
```

The correct approach is:

```java
new User("Rahul", 25);
```

Create another record instance.

This fits perfectly with the purpose of records:

```text
Record
 ↓
immutable data carrier
```

---

# 15. Hidden classes are also protected

Similarly, final fields declared in hidden classes are treated as non-modifiable.

So the special cases are important:

```text
final field
   │
   ├── ordinary instance final
   │       └── historically reflectively mutable
   │
   ├── static final
   │       └── non-modifiable
   │
   ├── record final
   │       └── non-modifiable
   │
   └── hidden class final
           └── non-modifiable
```

([Oracle Docs][3])

---

# 16. Why this matters for your Reflection understanding

This is actually a perfect example of why **Reflection isn't magic**.

You might have learned:

```java
field.setAccessible(true);
```

and concluded:

> "Reflection can bypass all access restrictions."

That's not a good mental model.

A better model is:

```text
Reflection
    ↓
Requests privileged runtime access
    ↓
JVM checks access rules
    ↓
Module rules
    ↓
Class/package openness
    ↓
Special restrictions
    ↓
Operation allowed/denied
```

Reflection is powerful, but the JVM still controls what ultimately happens.

---

# 17. Very important interview distinction

If an interviewer asks:

> **"Can Java Reflection modify a final field?"**

Don't simply answer:

> "Yes."

That's incomplete.

A strong 2026 answer would be:

> **Historically, deep reflection using `Field.setAccessible(true)` followed by `Field.set()` could modify many ordinary final instance fields. Java 25 still permits this under the applicable access conditions. Starting with JDK 26, JEP 500 introduces final-field mutation restrictions: by default such mutations produce warnings, and the application must explicitly enable final-field mutation if it needs it. The long-term direction is to deny these mutations by default. Static final fields and final fields of records/hidden classes are already non-modifiable through this mechanism.**

That's the answer I'd expect from someone preparing for a senior Java/Spring interview. ([OpenJDK][1])

---

# 18. The timeline to remember

```text
                 FINAL FIELD MUTATION
                         │
                         ↓
        ┌─────────────────────────────────┐
        │                                 │
   Java ≤ 25                         JDK 26
        │                                 │
        ↓                                 ↓
Many ordinary final             Restricted by default
instance fields                 + warning
could be changed                │
via deep reflection             ↓
        │                    Explicit opt-in
        │                    possible
        │                                 │
        └────────────────┬────────────────┘
                         ↓
                  Future direction
                         ↓
                Denied by default
```

JEP 500 describes this explicitly as a gradual transition toward making `final` mean final by default. ([OpenJDK][1])

---

# 19. One more subtle point: "override final" isn't really the right terminology

You aren't **overriding** a final variable.

"Override" applies primarily to methods:

```java
@Override
void process() {}
```

For fields, the correct terminology is:

> **mutating a final field reflectively**

So in an interview say:

❌ "How do you override a final variable?"

Prefer:

✅ "How could final fields historically be mutated through reflection?"

---

# 20. Connect this to the Reflection code we just learned

Earlier I showed:

```java
Field field =
        User.class.getDeclaredField("name");

field.setAccessible(true);

field.set(user, "Rahul");
```

For an ordinary final instance field:

### Java 25

Conceptually:

```text
getDeclaredField()
       ↓
setAccessible(true)
       ↓
Field.set()
       ↓
Can mutate final instance field
```

subject to Java's access/module rules. ([Oracle Docs][3])

### Java 26+

Conceptually:

```text
getDeclaredField()
       ↓
setAccessible(true)
       ↓
Field.set()
       ↓
Is final-field mutation enabled?
       │
       ├── YES → allowed
       │
       └── NO  → illegal/restricted
```

with JDK 26's default behavior being a warning while the transition proceeds. ([OpenJDK][1])

So this is a **very good real-world example of Reflection + JVM internals + immutability + JDK evolution** all intersecting.

And it also connects directly to the **Immutability → Records → Reflection → JVM/JIT** concepts you're working through.

[1]: https://openjdk.org/jeps/500?utm_source=chatgpt.com "JEP 500: Prepare to Make Final Mean Final"
[2]: https://openjdk.org/jeps/8132243?utm_source=chatgpt.com "JEP draft: Optimize Final Field Loads In Generated Code"
[3]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/Field.html?utm_source=chatgpt.com "Field (Java SE 25 & JDK 25)"
[4]: https://docs.oracle.com/en/java/javase/26/migrate/preparing-final-field-mutation-restrictions.html?utm_source=chatgpt.com "Preparing for final Field Mutation Restrictions"
