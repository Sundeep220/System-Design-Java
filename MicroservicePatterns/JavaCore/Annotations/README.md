Absolutely. **Annotations are one of the most important Java topics for interviews**, especially because they connect directly to **Spring Boot, Hibernate/JPA, validation, testing, dependency injection, reflection, AOP, Lombok, and frameworks**.

Since you're already going through reflection, this is the right place to connect the two:

> **Annotation = metadata attached to Java code.**
> **Reflection = mechanism used to inspect that metadata at runtime.**

Let's go from fundamentals → custom annotations → reflection → real Spring-style use cases → interview traps.

---

# 1. What is an Annotation?

An annotation is **metadata** that you attach to Java program elements.

For example:

```java
@Override
public String toString() {
    return "Hello";
}
```

Here:

```java
@Override
```

is an annotation.

It doesn't directly execute business logic.

Instead, it tells the compiler, JVM, or some framework:

> "This class/method/field has some additional information associated with it."

---

# 2. Why Do We Need Annotations?

Before annotations, frameworks often required configuration in separate files.

Imagine you have:

```java
class UserService {

    public void createUser() {
    }
}
```

Suppose you want to tell Spring:

> "Create and manage an object of this class."

You could have external configuration:

```xml
<bean id="userService"
      class="com.example.UserService"/>
```

Annotations allow the metadata to live next to the code:

```java
@Service
class UserService {

    public void createUser() {
    }
}
```

Much cleaner.

So annotations are essentially a way of saying:

> **"Attach declarative metadata to my code."**

---

# 3. Annotations Don't Automatically Do Anything

This is an extremely important interview point.

Consider:

```java
@MyAnnotation
public void hello() {
    System.out.println("Hello");
}
```

Putting:

```java
@MyAnnotation
```

doesn't automatically execute anything.

Something must **process the annotation**.

There are several possibilities:

### Compiler

Example:

```java
@Override
```

The compiler checks it.

### Annotation processor

Example:

```java
@Getter
@Setter
```

from Lombok.

Lombok processes annotations during compilation and generates code.

### Runtime framework

Example:

```java
@Service
@RestController
@Transactional
@Autowired
```

Spring reads these annotations and performs framework-specific behavior.

### Your own reflection code

You can write:

```java
method.isAnnotationPresent(MyAnnotation.class)
```

and process it yourself.

---

# 4. Anatomy of an Annotation

Consider:

```java
@Override
```

Conceptually:

```text
        Annotation
             |
          @Override
             |
      metadata attached
             |
          method
```

A custom annotation looks like:

```java
@interface MyAnnotation {
}
```

Notice something interesting.

It looks somewhat like an interface:

```java
interface MyInterface {
}
```

But:

```java
@interface MyAnnotation {
}
```

declares an **annotation type**.

---

# 5. Creating Your Own Annotation

Let's create a simple annotation.

```java
public @interface Important {
}
```

Now:

```java
@Important
public class PaymentService {

}
```

That's it.

We've created our own annotation.

But currently it doesn't have any behavior.

We need something to consume it.

---

# 6. Processing Our Annotation

Let's create:

```java
@Important
public class PaymentService {

    public void pay() {
        System.out.println("Payment processed");
    }
}
```

Now we can use reflection.

```java
import java.lang.annotation.Annotation;

public class Main {

    public static void main(String[] args) {

        Class<PaymentService> clazz = PaymentService.class;

        if (clazz.isAnnotationPresent(Important.class)) {
            System.out.println("This is an important service");
        }
    }
}
```

Output:

```text
This is an important service
```

Now we have the fundamental connection:

```text
Annotation
     ↓
attached to Java element
     ↓
Reflection
     ↓
inspect annotation
     ↓
execute custom behavior
```

---

# 7. Annotation Elements

Annotations can contain values.

For example:

```java
public @interface Author {

    String name();

}
```

Now:

```java
@Author(name = "Sundeep")
public class PaymentService {

}
```

We can read it:

```java
Author author =
        PaymentService.class.getAnnotation(Author.class);

System.out.println(author.name());
```

Output:

```text
Sundeep
```

---

# 8. Multiple Annotation Properties

You can define multiple properties:

```java
public @interface ApiInfo {

    String name();

    String version();

    String description();

}
```

Use:

```java
@ApiInfo(
    name = "Payment API",
    version = "1.0",
    description = "Handles payments"
)
public class PaymentController {

}
```

Read:

```java
ApiInfo info =
        PaymentController.class.getAnnotation(ApiInfo.class);

System.out.println(info.name());
System.out.println(info.version());
System.out.println(info.description());
```

Output:

```text
Payment API
1.0
Handles payments
```

---

# 9. Default Values

You can provide defaults.

```java
public @interface ApiInfo {

    String name();

    String version() default "1.0";

}
```

Now:

```java
@ApiInfo(name = "Payment API")
class PaymentController {
}
```

`version` becomes:

```text
1.0
```

You can override it:

```java
@ApiInfo(
    name = "Payment API",
    version = "2.0"
)
class PaymentController {
}
```

---

# 10. Special Case: `value()`

There is a special convention.

Suppose:

```java
public @interface Role {
    String value();
}
```

Then you can write:

```java
@Role("ADMIN")
class AdminService {
}
```

instead of:

```java
@Role(value = "ADMIN")
class AdminService {
}
```

Because the element is named:

```java
value()
```

This is commonly seen in frameworks.

For example:

```java
@RequestMapping("/users")
```

instead of:

```java
@RequestMapping(value = "/users")
```

---

# 11. @Target — Where Can Annotation Be Used?

This is extremely important.

You can restrict where your annotation can appear.

Example:

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface Audit {
}
```

Now:

```java
@Audit
public void createUser() {
}
```

works.

But:

```java
@Audit
class UserService {
}
```

will produce a compilation error.

---

# 12. Common ElementType Values

`ElementType` includes things such as:

```java
TYPE
METHOD
FIELD
PARAMETER
CONSTRUCTOR
LOCAL_VARIABLE
ANNOTATION_TYPE
PACKAGE
MODULE
TYPE_PARAMETER
TYPE_USE
RECORD_COMPONENT
```

Examples:

### Class

```java
@Target(ElementType.TYPE)
public @interface ServiceInfo {
}
```

### Method

```java
@Target(ElementType.METHOD)
public @interface Audit {
}
```

### Field

```java
@Target(ElementType.FIELD)
public @interface Encrypted {
}
```

### Parameter

```java
@Target(ElementType.PARAMETER)
public @interface UserId {
}
```

---

# 13. Multiple Targets

You can allow multiple locations:

```java
@Target({
    ElementType.TYPE,
    ElementType.METHOD
})
public @interface Important {
}
```

Now:

```java
@Important
class PaymentService {

    @Important
    public void pay() {
    }
}
```

Both are valid.

---

# 14. @Retention — How Long Does Annotation Exist?

This is **one of the most important interview questions**.

Java provides:

```java
@Retention(...)
```

There are three major retention policies.

---

## SOURCE

```java
@Retention(RetentionPolicy.SOURCE)
```

Annotation exists only in source code.

It is discarded during compilation.

Example:

```java
@Override
```

Conceptually:

```text
.java source
   ↓
compiler
   ↓
.class
   ↓
annotation gone
```

Useful for:

* compiler tools
* source-level processing
* static analysis

---

# 15. CLASS

```java
@Retention(RetentionPolicy.CLASS)
```

Annotation is stored in the `.class` file.

But normally it isn't available through runtime reflection.

Flow:

```text
.java
 ↓
compiler
 ↓
.class
 ↓
annotation exists
 ↓
JVM runtime
 ↓
reflection cannot normally see it
```

This is the default retention policy if you don't specify one.

---

# 16. RUNTIME

```java
@Retention(RetentionPolicy.RUNTIME)
```

Annotation survives until runtime.

Therefore reflection can inspect it.

Example:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audit {
}
```

Then:

```java
class PaymentService {

    @Audit
    public void pay() {
    }
}
```

Reflection:

```java
Method method =
        PaymentService.class.getMethod("pay");

if (method.isAnnotationPresent(Audit.class)) {
    System.out.println("Audit required");
}
```

Output:

```text
Audit required
```

---

# 17. Very Important Interview Question

### What is the difference between `@Target` and `@Retention`?

Easy way to remember:

```text
@Target
   ↓
WHERE can annotation be used?

@Retention
   ↓
HOW LONG does annotation survive?
```

For example:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
}
```

means:

> `@Audit` can only be attached to methods and must remain available at runtime.

---

# 18. @Documented

Another meta-annotation:

```java
@Documented
public @interface Important {
}
```

This tells Javadoc tools to include the annotation in generated documentation.

---

# 19. @Inherited

This one has an important trap.

Suppose:

```java
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Important {
}
```

Then:

```java
@Important
class Parent {
}
```

and:

```java
class Child extends Parent {
}
```

Now:

```java
Child.class.isAnnotationPresent(Important.class)
```

can return:

```text
true
```

because `@Important` is inherited.

### Important:

`@Inherited` applies to **class annotations**.

It does **not** mean:

> "All annotations are inherited everywhere."

Method annotations aren't inherited this way.

---

# 20. Meta-Annotations

Annotations that annotate other annotations are called **meta-annotations**.

For example:

```java
@Target
@Retention
@Documented
@Inherited
```

are meta-annotations.

Example:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
}
```

Here:

```java
@Target
@Retention
```

are annotations applied to our annotation.

Conceptually:

```text
@Target
@Retention
    ↓
@Audit
    ↓
method
```

---

# 21. Real Example: Custom @Audit Annotation

Now let's build something interview-worthy.

We want:

```java
@Audit
public void transferMoney() {
    System.out.println("Money transferred");
}
```

Whenever this method is executed, we want:

```text
AUDIT: transferMoney called
Money transferred
```

First:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audit {
}
```

Service:

```java
public class PaymentService {

    @Audit
    public void transferMoney() {
        System.out.println("Money transferred");
    }

    public void checkBalance() {
        System.out.println("Balance checked");
    }
}
```

Reflection can identify the method:

```java
Method[] methods =
        PaymentService.class.getDeclaredMethods();

for (Method method : methods) {

    if (method.isAnnotationPresent(Audit.class)) {

        System.out.println(
            "Audited method: " + method.getName()
        );
    }
}
```

Output:

```text
Audited method: transferMoney
```

---

# 22. But Can Annotation Execute Code?

No.

This is an important distinction.

This:

```java
@Audit
public void transferMoney() {
}
```

doesn't mean Java automatically executes auditing.

Something like:

```text
Spring AOP
        OR
Reflection
        OR
Annotation Processor
        OR
Custom framework
```

must interpret the annotation.

This distinction is frequently tested in interviews.

---

# 23. Annotation + Reflection Example

Let's create a more useful annotation.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RoleAllowed {

    String value();

}
```

Use:

```java
class UserService {

    @RoleAllowed("ADMIN")
    public void deleteUser() {
        System.out.println("User deleted");
    }

    @RoleAllowed("USER")
    public void viewProfile() {
        System.out.println("Profile viewed");
    }
}
```

Now inspect:

```java
Method[] methods =
        UserService.class.getDeclaredMethods();

for (Method method : methods) {

    if (method.isAnnotationPresent(RoleAllowed.class)) {

        RoleAllowed annotation =
                method.getAnnotation(RoleAllowed.class);

        System.out.println(
                method.getName()
                + " requires role "
                + annotation.value()
        );
    }
}
```

Output:

```text
deleteUser requires role ADMIN
viewProfile requires role USER
```

Now we're basically building a tiny framework.

---

# 24. This Is How Frameworks Think

Consider Spring:

```java
@Service
class PaymentService {
}
```

Spring can inspect classes and see:

```text
@Service
    ↓
metadata
    ↓
Spring scans classes
    ↓
finds @Service
    ↓
creates bean
    ↓
stores bean in ApplicationContext
```

Similarly:

```java
@Autowired
private PaymentService paymentService;
```

Spring sees:

```text
@Autowired
     ↓
reflection / framework metadata processing
     ↓
dependency required
     ↓
find matching bean
     ↓
inject dependency
```

The actual Spring implementation is much more sophisticated than simply calling reflection, but **the conceptual model is important for interviews**.

---

# 25. Spring Annotations You Should Understand

As a Spring Boot developer, you should be comfortable with:

### Component registration

```java
@Component
@Service
@Repository
@Controller
@RestController
@Configuration
```

### Dependency injection

```java
@Autowired
@Qualifier
@Primary
```

### Web

```java
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@RequestMapping
@PathVariable
@RequestParam
@RequestBody
```

### Transactions

```java
@Transactional
```

### Validation

```java
@NotNull
@NotBlank
@Size
@Min
@Max
@Valid
```

### JPA

```java
@Entity
@Table
@Id
@GeneratedValue
@Column
@OneToMany
@ManyToOne
@OneToOne
```

### Testing

```java
@Test
@BeforeEach
@AfterEach
@Mock
@InjectMocks
```

Almost all of these are annotation-driven metadata.

---

# 26. Why Annotations Are So Powerful

Compare:

### Traditional configuration

```text
Java code
+
XML
+
configuration files
+
framework configuration
```

versus:

```java
@Service
@Transactional
@Validated
public class PaymentService {
}
```

Annotations allow configuration to sit directly next to the code it describes.

This is called **declarative programming/configuration**.

Instead of:

> "Execute these steps."

you say:

> "This method is transactional."

And the framework handles the mechanism.

---

# 27. Annotation vs Interface

This is a common conceptual question.

### Interface

Defines a contract.

```java
interface PaymentProcessor {

    void pay();
}
```

A class must provide behavior:

```java
class StripeProcessor
        implements PaymentProcessor {

    public void pay() {
    }
}
```

### Annotation

Provides metadata.

```java
@PaymentProvider("STRIPE")
class StripeProcessor {
}
```

The annotation doesn't define the required implementation of `pay()`.

---

# 28. Annotation vs Comment

Why not just use comments?

```java
// This method requires ADMIN
public void deleteUser() {
}
```

Because comments are not structured metadata.

An annotation:

```java
@RoleAllowed("ADMIN")
public void deleteUser() {
}
```

can be:

* inspected
* validated
* processed
* used by frameworks
* accessed through reflection
* processed during compilation

Comments generally cannot provide this structured behavior.

---

# 29. Annotation Processing at Compile Time

There is another important category:

**Annotation Processors.**

They process annotations during compilation.

For example, Lombok:

```java
@Getter
@Setter
public class User {

    private String name;
}
```

You don't explicitly write:

```java
public String getName() {
    return name;
}
```

Lombok processes:

```java
@Getter
```

and generates the required code during compilation.

Conceptually:

```text
Source Code
    ↓
Annotation Processor
    ↓
Generated code
    ↓
Compiler
    ↓
.class
```

This is different from runtime reflection.

---

# 30. Runtime Annotation vs Compile-Time Annotation

This distinction is excellent for interviews.

| Feature    | Runtime Annotation | Compile-time Processing |
| ---------- | ------------------ | ----------------------- |
| Processing | Runtime            | Compilation             |
| Reflection | Can be used        | Usually not required    |
| Retention  | `RUNTIME`          | Often `SOURCE`          |
| Example    | Spring annotations | Lombok                  |
| Purpose    | Framework behavior | Generate/check code     |

---

# 31. Annotation Processing vs Reflection

They are not the same.

### Reflection

Runtime:

```java
Method method = ...;

method.getAnnotation(Audit.class);
```

### Annotation Processor

Compile time:

```text
javac
 ↓
annotation processor
 ↓
inspect source/model
 ↓
generate code/errors
```

Think:

```text
Annotation
     |
     +---- Compiler checks
     |
     +---- Annotation Processor
     |
     +---- Runtime Reflection
     |
     +---- Framework processing
```

---

# 32. Repeatable Annotations

Java supports repeatable annotations.

Suppose you want:

```java
@Role("ADMIN")
@Role("MANAGER")
public void approvePayment() {
}
```

Define:

```java
@Repeatable(Roles.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Role {

    String value();
}
```

Container:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Roles {

    Role[] value();
}
```

Now:

```java
@Role("ADMIN")
@Role("MANAGER")
public void approvePayment() {
}
```

You can retrieve them:

```java
Role[] roles =
        method.getAnnotationsByType(Role.class);
```

---

# 33. Annotation with Enum

Annotations can have enum properties.

```java
enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

Annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Alert {

    Severity severity();

}
```

Usage:

```java
@Alert(severity = Severity.CRITICAL)
public void paymentFailure() {
}
```

---

# 34. Annotation with Arrays

You can also use arrays.

```java
public @interface Roles {

    String[] value();

}
```

Usage:

```java
@Roles({
    "ADMIN",
    "MANAGER",
    "AUDITOR"
})
public void approvePayment() {
}
```

---

# 35. What Types Can Annotation Elements Have?

Annotation element types are restricted.

They can generally be:

```text
primitive
String
Class
enum
annotation
arrays of these types
```

For example:

```java
public @interface Example {

    String name();

    int age();

    boolean active();

    Class<?> type();

    Severity severity();

    Role role();

    String[] tags();
}
```

You cannot arbitrarily use:

```java
List<String>
Map<String, String>
Object
```

as annotation element types.

---

# 36. Class as Annotation Property

Example:

```java
public @interface Handler {

    Class<?> value();

}
```

Usage:

```java
@Handler(PaymentHandler.class)
class PaymentService {
}
```

This pattern is common in framework design.

---

# 37. Real Interview-Level Custom Annotation

Let's design:

```java
@Cacheable
public User getUser(Long id) {
}
```

Our annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyCacheable {

    String keyPrefix() default "";

}
```

Usage:

```java
@MyCacheable(keyPrefix = "user")
public User getUser(Long id) {
    return repository.findById(id);
}
```

A framework could inspect:

```java
MyCacheable annotation =
        method.getAnnotation(MyCacheable.class);

String prefix = annotation.keyPrefix();
```

Then create:

```text
user:123
user:456
user:789
```

This is essentially the idea behind many framework annotations.

---

# 38. Annotation + Proxy/AOP

This is where annotations become extremely powerful.

Suppose:

```java
@Audit
public void transferMoney() {
}
```

We want:

```text
Before method:
    Audit log

Execute method

After method:
    Audit log
```

A framework can:

```text
           @Audit
              |
              ↓
        inspect metadata
              |
              ↓
         create proxy
              |
        ┌─────┴─────┐
        ↓           ↓
    before()     actual method
                    |
                    ↓
                 after()
```

This is closely related to Spring AOP.

For example:

```java
@Transactional
public void transferMoney() {
}
```

Conceptually:

```text
caller
   ↓
Spring proxy
   ↓
start transaction
   ↓
transferMoney()
   ↓
commit / rollback
```

The annotation itself isn't performing the transaction.

**Spring infrastructure interprets the annotation and creates the behavior.**

---

# 39. Annotations in Spring Boot: The Mental Model

When you see:

```java
@RestController
@RequestMapping("/users")
public class UserController {
}
```

don't think:

> "Java automatically knows this is a REST controller."

Think:

```text
Annotation
    ↓
Spring startup
    ↓
component scanning / configuration processing
    ↓
metadata discovered
    ↓
Spring registers infrastructure
    ↓
HTTP request mapping created
```

This mental model will help you understand Spring deeply rather than memorizing annotations.

---

# 40. Why `@Transactional` Is a Great Interview Example

Suppose:

```java
@Transactional
public void transferMoney() {

    debit();

    credit();
}
```

You might say:

> "`@Transactional` starts a transaction."

That's incomplete.

Better interview answer:

> "`@Transactional` is metadata that describes transactional semantics. Spring's transaction infrastructure detects the annotation and typically applies transaction behavior through proxies/interceptors around the method."

Then:

```text
caller
 ↓
proxy
 ↓
transaction begin
 ↓
target method
 ↓
success?
 ├── yes → commit
 └── no  → rollback according to rules
```

That's a much stronger answer.

---

# 41. Important Interview Question: Is Annotation Inheritance Automatic?

No.

Consider:

```java
class Parent {

    @Important
    void test() {}
}
```

Child:

```java
class Child extends Parent {
}
```

You should **not assume** `@Important` is inherited by the overridden/inherited method.

`@Inherited` specifically concerns certain **class-level annotations**.

It does not make method annotations magically inherited.

---

# 42. Important Interview Question: Can an Annotation Have Logic?

No.

You cannot do:

```java
public @interface Audit {

    void execute(); // invalid
}
```

Annotations describe metadata.

Behavior belongs to:

* framework
* reflection code
* annotation processor
* proxy/AOP
* compiler

---

# 43. Important Interview Question: Can Annotation Extend Another Annotation?

You cannot use normal annotation inheritance like:

```java
@interface A {
}

@interface B extends A { // invalid
}
```

Annotations do not support normal class/interface inheritance in this way.

However, annotations can be **meta-annotated**.

For example, Spring's composed annotations make heavy use of meta-annotation concepts.

---

# 44. Spring's Composed Annotations

This is a really useful concept.

You can create an annotation that itself uses other annotations.

Conceptually:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface MyService {
}
```

Now:

```java
@MyService
public class PaymentService {
}
```

Your annotation is effectively acting as a composed stereotype.

This idea is used extensively in Spring.

---

# 45. When Should You Create a Custom Annotation?

Don't create annotations just because you can.

Good use cases:

### 1. Cross-cutting concerns

```java
@Audit
@Retry
@Metrics
@RateLimit
```

### 2. Declarative configuration

```java
@Cacheable
@Transactional
@Async
```

### 3. Validation

```java
@StrongPassword
@ValidUserId
```

### 4. Security

```java
@RequiresRole("ADMIN")
```

### 5. Framework metadata

```java
@MessageHandler
@EventListener
```

### 6. Testing

```java
@IntegrationTest
```

### 7. Code generation

```java
@Getter
@Builder
```

---

# 46. When Should You NOT Create One?

Don't create:

```java
@DoSomething
```

just to replace a method call.

Bad:

```java
@SendEmail
public void register() {
}
```

if the annotation processor is complicated and a simple method call would be clearer.

Prefer:

```java
emailService.sendWelcomeEmail();
```

when the behavior is explicit business logic.

Annotations are best when the behavior is **declarative, reusable, and framework-like**.

---

# 47. Annotation Design Rule

A good rule for interviews:

> **Use annotations to describe metadata or declarative behavior, not to hide ordinary business logic.**

Good:

```java
@Transactional
@Audit
@RequiresRole("ADMIN")
```

Less appropriate:

```java
@CalculateDiscount
@CreateInvoice
@SendPayment
```

if these are ordinary business operations that should be explicit in the code.

---

# 48. Complete Custom Annotation Example

Let's put everything together.

### Annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresRole {

    String value();

}
```

### Service

```java
public class UserService {

    @RequiresRole("ADMIN")
    public void deleteUser() {
        System.out.println("User deleted");
    }

    @RequiresRole("USER")
    public void viewProfile() {
        System.out.println("Profile viewed");
    }
}
```

### Annotation processor using reflection

```java
import java.lang.reflect.Method;

public class AuthorizationChecker {

    public static void check(
            Class<?> clazz,
            String currentUserRole) throws Exception {

        for (Method method : clazz.getDeclaredMethods()) {

            RequiresRole annotation =
                    method.getAnnotation(RequiresRole.class);

            if (annotation == null) {
                continue;
            }

            String requiredRole = annotation.value();

            System.out.println(
                method.getName()
                + " requires "
                + requiredRole
            );

            if (!requiredRole.equals(currentUserRole)) {

                System.out.println(
                    "Access denied"
                );
            } else {

                System.out.println(
                    "Access granted"
                );
            }
        }
    }
}
```

Run:

```java
public class Main {

    public static void main(String[] args)
            throws Exception {

        AuthorizationChecker.check(
                UserService.class,
                "ADMIN"
        );
    }
}
```

Output:

```text
deleteUser requires ADMIN
Access granted

viewProfile requires USER
Access denied
```

Now you've implemented a very small version of a framework feature.

---

# 49. The Complete Annotation Architecture

For interviews, remember this architecture:

```text
                   Annotation
                       |
          ┌────────────┼─────────────┐
          ↓            ↓             ↓
      Compiler    Annotation      Runtime
                    Processor      Reflection
          |            |             |
          ↓            ↓             ↓
      validation   code generation  framework
                                  behavior
```

And in Spring:

```text
                 @Transactional
                       |
                       ↓
                 Spring startup
                       |
                       ↓
              annotation metadata
                       |
                       ↓
                transaction
                infrastructure
                       |
                       ↓
                    Proxy
                       |
                       ↓
                  interceptor
                       |
                       ↓
                actual method
```

---

# 50. Interview Questions You Should Master

For Java/Spring interviews, be prepared for these:

### Fundamentals

1. What is an annotation?
2. Why do we use annotations?
3. How are annotations different from comments?
4. How do you create a custom annotation?
5. Can annotations contain methods?
6. What types can annotation attributes have?

### Meta-annotations

7. What is `@Target`?
8. What is `@Retention`?
9. Difference between `SOURCE`, `CLASS`, and `RUNTIME`?
10. What is `@Documented`?
11. What is `@Inherited`?
12. What is `@Repeatable`?

### Reflection

13. How do you read annotations using reflection?
14. Difference between `getAnnotation()` and `getAnnotations()`?
15. What happens if retention is `SOURCE` and you try runtime reflection?
16. Why does a runtime annotation require `RetentionPolicy.RUNTIME`?

### Frameworks

17. How does Spring use annotations?
18. Does `@Transactional` itself execute transaction logic?
19. How does Spring discover `@Service`?
20. How does `@Autowired` work conceptually?
21. How do annotations interact with Spring AOP?
22. Why are annotations useful for declarative programming?

### Advanced

23. What is an annotation processor?
24. Annotation processor vs reflection?
25. How does Lombok use annotations?
26. Can annotations be inherited?
27. Can method annotations be inherited?
28. Can one annotation extend another?
29. What are composed/meta-annotations?
30. When should you avoid creating a custom annotation?

---

# 51. The Most Important Things to Remember

If you have only 5 minutes before an interview, remember these:

```text
Annotation
    =
Metadata attached to Java code
```

```text
@Target
    =
Where can annotation be applied?
```

```text
@Retention
    =
How long does annotation survive?
```

```text
SOURCE
    =
source code only
```

```text
CLASS
    =
.class file, normally unavailable through runtime reflection
```

```text
RUNTIME
    =
available to runtime reflection
```

```text
@Inherited
    =
class-level annotation inheritance,
not general method/field inheritance
```

And the most important conceptual point:

> **An annotation doesn't automatically perform behavior. Something has to interpret it.**

That "something" can be:

```text
Compiler
Annotation Processor
Reflection
Framework
AOP / Proxy
```

And for your **Java + Spring Boot interview preparation**, the most valuable connection is:

```text
Annotations
     ↓
Reflection / metadata
     ↓
Spring infrastructure
     ↓
Bean creation / DI / AOP / Transactions
     ↓
Application behavior
```

This is also why learning **annotations immediately after reflection** is a very good sequence: once you understand reflection, custom annotations become much easier to understand, and then Spring's annotation-driven programming stops feeling like magic.



Yes. This is the part that makes annotations **actually click**.

The key thing to understand is that **"annotation processing" is not one single mechanism in Java**. There are several ways annotations can be consumed:

```text
                    Annotation
                        |
        ┌───────────────┼────────────────┐
        ↓               ↓                ↓
   Compiler        Annotation        Runtime
   built-in        Processor         Reflection
        |               |                |
   @Override       @Getter etc.      @Transactional
   @Deprecated      Lombok            @Entity
   @SuppressWarnings MapStruct         @MyAnnotation
```

Let's understand exactly what happens, starting with `@Override`.

---

# 1. What actually happens with `@Override`?

Consider:

```java
class Parent {

    void print() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    @Override
    void print() {
        System.out.println("Child");
    }
}
```

When you run:

```bash
javac Child.java
```

the Java compiler parses the source code.

Conceptually, it creates an internal representation of your program:

```text
Source code
    ↓
Lexer / Parser
    ↓
Abstract Syntax Tree (AST)
    ↓
Semantic analysis
    ↓
Type checking
    ↓
Bytecode generation
```

During semantic analysis, the compiler sees:

```java
@Override
void print()
```

and asks:

> "Does this method actually override a method from a superclass or interface?"

---

# 2. What if you make a mistake?

Suppose:

```java
class Parent {

    void print() {
    }
}
```

and:

```java
class Child extends Parent {

    @Override
    void prints() {
    }
}
```

Notice:

```java
prints()
```

instead of:

```java
print()
```

The compiler says:

```text
method does not override or implement a method from a supertype
```

The program doesn't compile.

So:

> `@Override` is primarily a **compile-time annotation** whose semantics are understood by the Java compiler.

---

# 3. Does `@Override` execute?

No.

There is no runtime code like:

```java
@Override.execute();
```

Nothing like that happens.

The compiler uses the annotation as an instruction/check during compilation.

After compilation, the annotation isn't needed to make overriding work.

For example:

```java
Child child = new Child();
child.print();
```

works because Java's **virtual method dispatch** handles overriding.

The annotation merely helps the compiler catch mistakes.

---

# 4. What is the retention of `@Override`?

You can inspect its definition conceptually as:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```

So:

```text
@Override
    ↓
compiler sees it
    ↓
compiler validates it
    ↓
annotation is discarded
    ↓
.class file doesn't need it
```

This is why:

```java
Child.class
```

doesn't need `@Override` at runtime to perform polymorphism.

---

# 5. `@Deprecated` Is Slightly Different

Consider:

```java
@Deprecated
public void oldMethod() {
}
```

The compiler can use this information to produce warnings:

```text
warning: oldMethod() is deprecated
```

So again:

```text
Source
  ↓
Compiler
  ↓
sees @Deprecated
  ↓
generates warning
```

The annotation communicates metadata to compiler/tools.

---

# 6. There Are Actually Three Important Annotation Processing Models

This is the most important conceptual distinction.

## Model 1 — Compiler understands annotation

Examples:

```java
@Override
@Deprecated
@SuppressWarnings
```

The Java compiler has special knowledge/semantics for these.

---

## Model 2 — Annotation Processor

Examples:

```java
@Getter
@Setter
@Mapper
@Builder
```

A separate annotation processor can inspect annotations **during compilation**.

Examples include Lombok and MapStruct.

---

## Model 3 — Runtime processing

Examples:

```java
@Entity
@Service
@Transactional
@Autowired
```

Frameworks can inspect annotations at runtime or during framework startup.

Typically:

```text
Annotation
    ↓
.class metadata
    ↓
framework
    ↓
reflection / metadata APIs
    ↓
framework behavior
```

---

# 7. How Does Java Know About `@Override`?

This is an important interview distinction.

It's tempting to think:

> "Java has a generic annotation engine that automatically processes every annotation."

That's not really how it works.

The compiler has **specific rules and logic** for standard language annotations such as:

```java
@Override
@Deprecated
@SuppressWarnings
```

For `@Override`, the compiler essentially performs a check equivalent to:

```text
Is this method overriding
a superclass/interface method?

       |
   ┌───┴────┐
   YES      NO
    |        |
 compile    ERROR
```

---

# 8. Now Let's Create Our Own Annotation

Suppose we create:

```java
public @interface Important {
}
```

and:

```java
@Important
class PaymentService {
}
```

Will Java automatically do anything?

**No.**

This is critical.

Java does not know what `@Important` means.

It simply stores the annotation according to its retention policy.

You have to provide the processing mechanism.

There are two major ways you can do this yourself:

```text
             @Important
                  |
          ┌───────┴────────┐
          ↓                ↓
      Reflection       Annotation
      at runtime        Processor
                         compile time
```

Let's build both.

---

# 9. Approach 1 — Process Your Annotation Using Reflection

This is the easiest way to understand custom annotation processing.

Create:

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Important {
}
```

Then:

```java
@Important
public class PaymentService {

    public void pay() {
        System.out.println("Payment");
    }
}
```

Now create your processor:

```java
public class AnnotationProcessor {

    public static void process(Class<?> clazz) {

        if (clazz.isAnnotationPresent(Important.class)) {
            System.out.println(
                clazz.getSimpleName()
                + " is important"
            );
        }
    }
}
```

Run:

```java
public class Main {

    public static void main(String[] args) {

        AnnotationProcessor.process(
            PaymentService.class
        );
    }
}
```

Output:

```text
PaymentService is important
```

---

# 10. What Happened Internally?

This:

```java
clazz.isAnnotationPresent(Important.class)
```

causes the runtime reflection system to inspect the class metadata.

Conceptually:

```text
PaymentService.class
       ↓
class metadata
       ↓
annotation metadata
       ↓
@Important found?
       ↓
YES
```

That's runtime annotation processing.

---

# 11. Processing Method Annotations

Let's make it more interesting.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audit {
}
```

Then:

```java
public class PaymentService {

    @Audit
    public void transferMoney() {
        System.out.println("Transfer");
    }

    public void checkBalance() {
        System.out.println("Balance");
    }
}
```

Now process it:

```java
import java.lang.reflect.Method;

public class AnnotationProcessor {

    public static void process(Class<?> clazz) {

        for (Method method : clazz.getDeclaredMethods()) {

            if (method.isAnnotationPresent(Audit.class)) {

                System.out.println(
                    "Audit required for: "
                    + method.getName()
                );
            }
        }
    }
}
```

Output:

```text
Audit required for: transferMoney
```

---

# 12. Reading Annotation Values

Now let's create:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audit {

    String action();
}
```

Use:

```java
public class PaymentService {

    @Audit(action = "TRANSFER_MONEY")
    public void transferMoney() {
    }
}
```

Process:

```java
for (Method method : clazz.getDeclaredMethods()) {

    Audit audit = method.getAnnotation(Audit.class);

    if (audit != null) {

        System.out.println(
            "Method: " + method.getName()
        );

        System.out.println(
            "Action: " + audit.action()
        );
    }
}
```

Output:

```text
Method: transferMoney
Action: TRANSFER_MONEY
```

This is exactly the fundamental mechanism behind many annotation-driven frameworks.

---

# 13. But Reflection Doesn't Automatically Run the Method

Suppose:

```java
@Audit(action = "TRANSFER")
public void transferMoney() {
    System.out.println("Money transferred");
}
```

Finding:

```java
method.isAnnotationPresent(Audit.class)
```

doesn't cause:

```java
transferMoney();
```

to execute.

You have to explicitly invoke it:

```java
method.invoke(object);
```

For example:

```java
PaymentService service = new PaymentService();

Method method =
    PaymentService.class.getDeclaredMethod(
        "transferMoney"
    );

method.invoke(service);
```

---

# 14. We Can Build Our Own Mini Framework

This is where things get interesting.

Suppose we want:

```java
@Command
public void createUser() {
    System.out.println("User created");
}
```

And we want our framework to automatically find all commands and execute one based on its name.

Annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {

    String value();
}
```

Service:

```java
public class UserCommands {

    @Command("create")
    public void createUser() {
        System.out.println("User created");
    }

    @Command("delete")
    public void deleteUser() {
        System.out.println("User deleted");
    }
}
```

Framework:

```java
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {

    private final Map<String, Method> commands =
            new HashMap<>();

    private final Object target;

    public CommandRegistry(Object target) {

        this.target = target;

        for (Method method :
                target.getClass().getDeclaredMethods()) {

            Command command =
                    method.getAnnotation(Command.class);

            if (command != null) {

                commands.put(
                    command.value(),
                    method
                );
            }
        }
    }

    public void execute(String command)
            throws Exception {

        Method method = commands.get(command);

        if (method == null) {
            throw new IllegalArgumentException(
                "Unknown command"
            );
        }

        method.invoke(target);
    }
}
```

Now:

```java
public class Main {

    public static void main(String[] args)
            throws Exception {

        UserCommands commands =
                new UserCommands();

        CommandRegistry registry =
                new CommandRegistry(commands);

        registry.execute("create");

        registry.execute("delete");
    }
}
```

Output:

```text
User created
User deleted
```

Congratulations: you've essentially created a tiny annotation-driven framework.

---

# 15. Now the More Advanced Way: Annotation Processors

Reflection happens at:

```text
RUNTIME
```

But Java also allows you to process annotations at:

```text
COMPILE TIME
```

using:

```java
javax.annotation.processing.AbstractProcessor
```

Modern Java uses:

```java
jakarta?
```

No—be careful here.

For annotation processing APIs, the standard package is:

```java
javax.annotation.processing
```

even in modern Java versions.

For example:

```java
import javax.annotation.processing.AbstractProcessor;
```

---

# 16. What Is an Annotation Processor?

An annotation processor is a program that runs **during compilation** and can inspect source-level program elements carrying particular annotations.

Think:

```text
javac
  |
  |---- parse source
  |
  |---- discover annotations
  |
  ↓
Annotation Processor
  |
  |---- inspect @MyAnnotation
  |
  |---- validate
  |
  |---- generate source files
  |
  ↓
compiler continues
```

This is very different from reflection.

---

# 17. Basic Annotation Processor

Suppose:

```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateInfo {
}
```

Then:

```java
@GenerateInfo
public class User {
}
```

We can create:

```java
public class GenerateInfoProcessor
        extends AbstractProcessor {
}
```

The main method we implement is:

```java
@Override
public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv) {
    
    return false;
}
```

---

# 18. What Does `process()` Receive?

Two important parameters:

```java
Set<? extends TypeElement> annotations
```

and:

```java
RoundEnvironment roundEnv
```

Think:

### `annotations`

Which annotation types are being processed?

For example:

```text
@GenerateInfo
```

### `RoundEnvironment`

What program elements were annotated?

For example:

```text
User class
Order class
Payment class
```

---

# 19. Register Which Annotation You Process

Use:

```java
@Override
public Set<String> getSupportedAnnotationTypes() {

    return Set.of(
        "com.example.GenerateInfo"
    );
}
```

And source version:

```java
@Override
public SourceVersion getSupportedSourceVersion() {

    return SourceVersion.latestSupported();
}
```

Now processor:

```java
public class GenerateInfoProcessor
        extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {

        return Set.of(
            "com.example.GenerateInfo"
        );
    }

    @Override
    public SourceVersion
            getSupportedSourceVersion() {

        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {

        return false;
    }
}
```

---

# 20. Finding Classes With Our Annotation

Now:

```java
for (Element element :
        roundEnv.getElementsAnnotatedWith(
            GenerateInfo.class)) {

    processingEnv.getMessager()
        .printMessage(
            Diagnostic.Kind.NOTE,
            "Found: " + element.getSimpleName()
        );
}
```

If you compile:

```java
@GenerateInfo
public class User {
}
```

you might see:

```text
Note: Found: User
```

This happens during compilation.

---

# 21. `Element` Is Important

With runtime reflection you work with:

```java
Class
Method
Field
Constructor
```

With annotation processing, you work with the **language model API**:

```text
Element
TypeElement
ExecutableElement
VariableElement
```

Think:

```text
Reflection                    Annotation Processing

Class                         TypeElement
Method                        ExecutableElement
Field                         VariableElement
Constructor                   ExecutableElement
```

This distinction is extremely useful in interviews.

---

# 22. Why Not Just Use Reflection During Compilation?

Because the class might not even exist yet.

Imagine:

```java
@GenerateRepository
class User {
}
```

The processor may want to generate:

```java
UserRepository.java
```

But we're currently compiling `User.java`.

There may not be a compiled:

```text
User.class
```

yet.

Annotation processing works with the compiler's representation of the source program.

That's why it uses:

```java
Element
TypeMirror
TypeElement
```

rather than ordinary runtime reflection.

---

# 23. Annotation Processor Can Generate Code

This is one of its biggest advantages.

Suppose:

```java
@GenerateRepository
class User {
}
```

Processor could generate:

```java
class UserRepository {

    public void save(User user) {
        // ...
    }
}
```

You can create a source file through:

```java
JavaFileObject file =
    processingEnv.getFiler()
        .createSourceFile(
            "com.example.UserRepository"
        );
```

Then:

```java
try (Writer writer = file.openWriter()) {

    writer.write("""
        package com.example;

        public class UserRepository {

            public void save(User user) {
                System.out.println("Saved");
            }
        }
        """);
}
```

The generated source becomes part of the compilation process.

---

# 24. This Is the Basic Idea Behind MapStruct

Suppose:

```java
@Mapper
public interface UserMapper {

    UserDto toDto(User user);
}
```

You don't manually write:

```java
UserDto toDto(User user) {
    UserDto dto = new UserDto();
    dto.setName(user.getName());
    ...
    return dto;
}
```

MapStruct's annotation processor sees:

```java
@Mapper
```

and generates an implementation during compilation.

Conceptually:

```text
@Mapper
   ↓
Annotation Processor
   ↓
inspect UserMapper
   ↓
generate UserMapperImpl
   ↓
compile generated code
```

That's compile-time annotation processing.

---

# 25. Lombok Works in a Similar Compile-Time Space

When you write:

```java
@Getter
@Setter
public class User {

    private String name;
}
```

Lombok processes those annotations during compilation and modifies/generates compiler-visible structures so that code using the generated members can compile.

Conceptually:

```text
@Getter
   ↓
Lombok processor
   ↓
getter information/code
   ↓
compiler
```

The exact implementation details are more complex than simply generating a `.java` file, but the important interview concept is:

> Lombok relies on compile-time annotation processing/compiler integration rather than runtime reflection.

---

# 26. Runtime vs Compile-Time: The Big Picture

This is worth memorizing:

```text
                         Annotation
                              |
             ┌────────────────┼────────────────┐
             |                |                |
             ↓                ↓                ↓
         Compiler        Annotation        Runtime
         semantics        Processor        Reflection
             |                |                |
         @Override         Lombok          Spring
         @Deprecated       MapStruct       Hibernate
         @Suppress...                     JUnit
```

---

# 27. Let's Compare `@Override` With Your Own Annotation

### `@Override`

```java
@Override
void print() {}
```

The Java compiler understands what this means.

```text
javac
 ↓
find @Override
 ↓
check method
 ↓
is it overriding?
 ↓
NO → compilation error
```

### Your annotation

```java
@Important
class PaymentService {}
```

Java compiler doesn't automatically know what `Important` means.

Unless you provide an annotation processor:

```text
@Important
   ↓
javac
   ↓
your AnnotationProcessor
   ↓
your logic
```

or process it at runtime:

```text
@Important
   ↓
.class
   ↓
Reflection
   ↓
your logic
```

---

# 28. Very Important: `@Override` Isn't a Normal Annotation Processor Example

This is a subtle interview point.

You shouldn't say:

> "`@Override` is processed by an annotation processor."

A better answer is:

> "`@Override` is a standard annotation with compiler-defined semantics. The Java compiler specifically recognizes it and validates that the annotated method overrides or implements a method from a supertype."

Whereas:

```java
@Getter
```

is typically associated with an annotation processor/compiler integration.

And:

```java
@Transactional
```

is processed by Spring's runtime/framework infrastructure.

---

# 29. What Happens to a Runtime Annotation?

Suppose:

```java
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
}
```

and:

```java
@Audit
class PaymentService {
}
```

Compile:

```text
PaymentService.java
       ↓
      javac
       ↓
PaymentService.class
       ↓
annotation metadata stored
```

At runtime:

```java
Class<?> clazz = PaymentService.class;
```

Then:

```java
clazz.getAnnotation(Audit.class);
```

asks the JVM's reflection implementation:

> "Does this class have runtime-visible `Audit` metadata?"

If yes, it returns an annotation instance/proxy representing that metadata.

---

# 30. Are Annotation Objects Stored Like Normal Objects?

This is a deeper question.

When you do:

```java
Audit audit =
    clazz.getAnnotation(Audit.class);
```

you get an object implementing the annotation interface.

Conceptually:

```java
public @interface Audit {
    String action();
}
```

can be treated through:

```java
Audit audit = ...
audit.action();
```

But you shouldn't think the `.class` file literally stores an ordinary Java object.

The class file stores **annotation metadata**, and the runtime reflection machinery exposes that metadata through annotation objects.

---

# 31. Annotation Processing With Parameters

Let's build a realistic processor.

Annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retry {

    int attempts() default 3;

    long delayMs() default 1000;
}
```

Usage:

```java
@Retry(
    attempts = 5,
    delayMs = 500
)
public void callExternalService() {
}
```

Reflection:

```java
Retry retry =
    method.getAnnotation(Retry.class);

int attempts = retry.attempts();
long delay = retry.delayMs();
```

Now you could implement:

```java
for (int i = 0; i < retry.attempts(); i++) {

    try {
        method.invoke(target);
        break;
    } catch (Exception e) {

        Thread.sleep(retry.delayMs());
    }
}
```

You've now created the basic conceptual foundation of a retry framework.

---

# 32. How Spring Takes This Further

Suppose:

```java
@Retry(attempts = 3)
public void callPaymentService() {
}
```

A production framework wouldn't necessarily manually invoke the method like our simple example.

It could create a proxy:

```text
Caller
   |
   ↓
Proxy
   |
   ↓
Does method have @Retry?
   |
   ↓
Yes
   |
   ↓
try
  target.callPaymentService()
catch
  retry
```

This is why annotations and AOP are so closely connected.

---

# 33. A Very Important Interview Distinction

If interviewer asks:

> "How does `@Transactional` work?"

Don't answer:

> "Reflection sees `@Transactional` and starts a transaction."

That's too simplistic.

A better answer:

> "`@Transactional` is metadata describing transaction semantics. Spring's transaction infrastructure detects that metadata and applies transaction interception, typically through proxies. The proxy/interceptor starts and completes or rolls back the transaction around the target method."

Then you can mention reflection/metadata inspection as part of the framework's machinery without claiming that annotation itself performs the transaction.

---

# 34. How Would You Implement Your Own Annotation in an Interview?

If interviewer says:

> "Create a custom `@LogExecutionTime` annotation."

I'd structure the answer in three layers.

### Step 1 — Define annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {
}
```

### Step 2 — Mark method

```java
@LogExecutionTime
public void processPayment() {
    // logic
}
```

### Step 3 — Process it

For a simple demonstration:

```java
Method method =
    PaymentService.class
        .getDeclaredMethod("processPayment");

if (method.isAnnotationPresent(
        LogExecutionTime.class)) {

    long start = System.nanoTime();

    method.invoke(service);

    long end = System.nanoTime();

    System.out.println(
        "Execution time = "
        + (end - start)
        + " ns"
    );
}
```

Then I'd say:

> "In a real Spring application I'd generally implement this using AOP rather than manually scanning and invoking methods with reflection."

That is a very strong interview answer.

---

# 35. Three Ways You Can Process Your Own Annotation

Remember this framework:

## Option 1 — Reflection

```text
Annotation
   ↓
.class
   ↓
runtime
   ↓
reflection
```

Use when:

* runtime behavior is needed
* dynamic inspection is useful
* building lightweight frameworks/tools

---

## Option 2 — Annotation Processor

```text
Annotation
   ↓
javac
   ↓
processor
   ↓
validate/generate code
```

Use when:

* code generation is needed
* compile-time validation is desired
* runtime reflection would be unnecessary overhead

Examples:

* Lombok
* MapStruct
* Dagger
* QueryDSL-style code generation

---

## Option 3 — Framework/AOP

```text
Annotation
   ↓
framework
   ↓
proxy/interceptor
   ↓
behavior
```

Use for:

* transactions
* security
* logging
* caching
* retries
* metrics
* authorization

Examples:

```java
@Transactional
@Cacheable
@PreAuthorize
@Async
```

---

# 36. The Interview Mental Model

When you see:

```java
@Something
```

ask four questions:

### 1. Where can it be applied?

Look at:

```java
@Target(...)
```

### 2. How long does it survive?

Look at:

```java
@Retention(...)
```

### 3. Who processes it?

Could be:

```text
Compiler
Annotation Processor
Framework
Reflection
IDE/static analysis tool
```

### 4. What does the processor do?

For example:

```text
@Override
    → compiler validation

@Getter
    → generated/accessor code

@Entity
    → ORM mapping metadata

@Transactional
    → transaction interception

@Test
    → test discovery/execution
```

This four-question model will let you reason about almost any annotation you've never seen before.

---

# 37. One Final End-to-End Example

Imagine we create:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {

    int requestsPerMinute();
}
```

Then:

```java
@RateLimit(requestsPerMinute = 10)
public void payment() {
}
```

### Compilation

```text
Java compiler
      ↓
checks syntax/types
      ↓
stores runtime annotation metadata
      ↓
PaymentService.class
```

### Application startup

A framework can inspect:

```text
PaymentService.class
      ↓
find @RateLimit
      ↓
read requestsPerMinute = 10
      ↓
create configuration
```

### Runtime request

```text
HTTP request
     ↓
proxy/interceptor
     ↓
check rate limit
     ↓
allowed?
   /     \
 YES      NO
  ↓       ↓
method   429
```

The annotation itself never:

```text
starts a rate limiter
```

Instead:

```text
annotation
    ↓
metadata
    ↓
processor/framework
    ↓
actual behavior
```

That is the fundamental concept you want to carry into Spring Boot and system design.

---

## The one diagram I'd memorize for interviews

```text
                    JAVA ANNOTATION
                          |
             ┌────────────┼─────────────┐
             ↓            ↓             ↓
         Compiler      Compile-time    Runtime
         semantics      Processor      Framework
             |             |             |
        @Override       Lombok        Spring
        @Deprecated     MapStruct      Hibernate
             |             |             |
             ↓             ↓             ↓
          validate      generate       metadata
                        code           / proxy
                                         |
                                         ↓
                                      behavior
```

And the golden rule:

> **An annotation is metadata. The annotation itself doesn't provide behavior. The compiler, an annotation processor, reflection-based code, or a framework must interpret that metadata and turn it into some effect.**

For your Java/Spring interview preparation, the next level after this is **how annotation processors actually work internally with `AbstractProcessor`, `Element`, `TypeMirror`, `RoundEnvironment`, `Filer`, and `Messager`, and then building a complete custom processor that generates Java code**. That is where you go from *using annotations* to genuinely understanding how Lombok/MapStruct-style tooling works.
