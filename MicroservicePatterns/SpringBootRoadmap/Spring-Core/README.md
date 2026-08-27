# Phase 12 -- Spring Core (IoC, DI, Bean Lifecycle)

This is the foundation of everything Spring. Every annotation you use -- `@Transactional`, `@Cacheable`, `@Async`, `@Scheduled` -- works because of what's explained here.

---

## 1. Inversion of Control (IoC)

### Without IoC (traditional)

```java
public class OrderService {
    // YOU create the dependency
    private final OrderRepository repo = new OrderRepositoryImpl();
    private final PaymentGateway gateway = new StripePaymentGateway();
    private final NotificationService notifier = new EmailNotificationService();
}
```

Problems:
```text
1. Tight coupling    -- OrderService knows exact implementation classes
2. Hard to test      -- Can't swap StripePaymentGateway with a mock
3. Hard to change    -- Switching to PayPal = change OrderService code
4. No lifecycle      -- Who manages creation, destruction, connection pools?
```

### With IoC (Spring)

```java
@Service
public class OrderService {
    // Spring INJECTS the dependency -- you don't create it
    private final OrderRepository repo;
    private final PaymentGateway gateway;
    private final NotificationService notifier;

    // Constructor injection -- Spring finds beans matching these types
    public OrderService(OrderRepository repo,
                        PaymentGateway gateway,
                        NotificationService notifier) {
        this.repo = repo;
        this.gateway = gateway;
        this.notifier = notifier;
    }
}
```

```text
Traditional:    YOU control object creation     (You -> Dependencies)
IoC:            CONTAINER controls creation      (Container -> You)

The "control" of creating objects is "inverted" from your code to the container.
```

### The IoC Container

```text
Spring IoC Container (ApplicationContext)
    |
    |-- reads configuration (annotations, XML, Java config)
    |-- creates beans (objects)
    |-- wires dependencies (injection)
    |-- manages lifecycle (init, destroy)
    |-- provides beans on request
    |
    +-- Bean: OrderService
    |     depends on: OrderRepository, PaymentGateway, NotificationService
    |
    +-- Bean: OrderRepository (JPA implementation)
    |
    +-- Bean: PaymentGateway (Stripe implementation)
    |
    +-- Bean: NotificationService (Email implementation)
```

---

## 2. Dependency Injection (DI)

DI is the mechanism through which IoC is achieved. Spring supports 3 types:

### 2.1 Constructor Injection (RECOMMENDED)

```java
@Service
public class WorkflowService {
    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final EventPublisher publisher;

    // Spring auto-injects all constructor parameters
    // @Autowired is optional when there's only one constructor (Spring 4.3+)
    public WorkflowService(WorkflowRepository repository,
                           WorkflowMapper mapper,
                           EventPublisher publisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.publisher = publisher;
    }
}
```

Why constructor injection is best:
```text
1. Immutability:     fields can be final
2. Required deps:    fails fast at startup if dependency missing
3. Testable:         just pass mocks in constructor
4. No reflection:    works without Spring (plain Java)
5. Thread-safe:      final fields are safely published
```

With Lombok:
```java
@Service
@RequiredArgsConstructor   // generates constructor for all final fields
public class WorkflowService {
    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final EventPublisher publisher;
}
```

### 2.2 Setter Injection

```java
@Service
public class WorkflowService {
    private WorkflowRepository repository;

    @Autowired
    public void setRepository(WorkflowRepository repository) {
        this.repository = repository;
    }
}
```

```text
Use when: dependency is truly optional (rare)
Problem:  mutable, can be changed after construction, nullable
```

### 2.3 Field Injection (AVOID)

```java
@Service
public class WorkflowService {
    @Autowired
    private WorkflowRepository repository;   // injected via reflection
}
```

```text
Problems:
1. Can't make field final
2. Hidden dependencies (not visible in constructor)
3. Requires reflection (can't create without Spring)
4. Hard to test (need reflection or Spring context)
5. Promotes over-injection (easy to add 15 fields nobody notices)
```

### How Spring resolves beans

```text
1. By TYPE:     Spring looks for a bean matching the parameter type
2. By NAME:     If multiple beans of same type, matches by parameter name
3. @Qualifier:  Explicitly specify which bean
4. @Primary:    Default bean when multiple candidates exist
```

---

## 3. Bean Declaration

### 3.1 Stereotype Annotations

```java
@Component          // generic Spring-managed bean
@Service            // business logic layer
@Repository         // data access layer (adds exception translation)
@Controller         // Spring MVC controller (returns views)
@RestController     // @Controller + @ResponseBody (returns JSON)
@Configuration      // declares @Bean methods
```

```text
All of these are @Component internally:

@Service = @Component + "this is business logic" (semantic)
@Repository = @Component + "this is data access" + exception translation
@Controller = @Component + "this handles HTTP requests"

Spring scans for ALL @Component-annotated classes during startup.
```

### 3.2 @Bean in @Configuration (explicit declaration)

```java
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

When to use @Bean vs @Component:
```text
@Component:  YOUR classes that you wrote
@Bean:       THIRD-PARTY classes you can't annotate (RestTemplate, ObjectMapper, etc.)
             Or when you need custom construction logic
```

---

## 4. @Primary and @Qualifier

When multiple beans implement the same interface:

```java
public interface NotificationSender {
    void send(String to, String message);
}

@Service
@Primary                    // default when no qualifier specified
public class EmailSender implements NotificationSender {
    public void send(String to, String message) { /* email */ }
}

@Service
public class SmsSender implements NotificationSender {
    public void send(String to, String message) { /* SMS */ }
}

@Service
public class SlackSender implements NotificationSender {
    public void send(String to, String message) { /* Slack */ }
}
```

### Using @Primary

```java
@Service
public class AlertService {
    private final NotificationSender sender;

    public AlertService(NotificationSender sender) {
        this.sender = sender;  // gets EmailSender (because @Primary)
    }
}
```

### Using @Qualifier

```java
@Service
public class AlertService {
    private final NotificationSender emailSender;
    private final NotificationSender smsSender;

    public AlertService(
            @Qualifier("emailSender") NotificationSender emailSender,
            @Qualifier("smsSender") NotificationSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }
}
```

### Injecting all implementations

```java
@Service
public class BroadcastService {
    private final List<NotificationSender> allSenders;

    // Spring injects ALL beans of type NotificationSender
    public BroadcastService(List<NotificationSender> allSenders) {
        this.allSenders = allSenders;
    }

    public void notifyAll(String to, String message) {
        allSenders.forEach(sender -> sender.send(to, message));
    }
}
```

---

## 5. Bean Scopes

```text
Scope          Instances    Lifecycle                   Use Case
---------------------------------------------------------------------------
singleton      1 per IoC    entire application life     DEFAULT, most beans
prototype      new each     created on request, not     stateful helpers
               request      destroyed by Spring
request        1 per HTTP   per HTTP request            request-scoped data
               request
session        1 per HTTP   per HTTP session            user session data
               session
application    1 per        per ServletContext          shared app state
               servlet ctx
websocket      1 per        per WebSocket session       WebSocket data
               WebSocket
```

### Singleton (default)

```java
@Service    // singleton by default
public class WorkflowService {
    // ONE instance shared by ALL threads
    // MUST be thread-safe
    // MUST NOT have mutable instance state
}
```

```text
Thread A ----> WorkflowService (same instance)
Thread B ----> WorkflowService (same instance)
Thread C ----> WorkflowService (same instance)

WARNING: If you store state in a singleton, you have a race condition.
```

### Prototype

```java
@Component
@Scope("prototype")
public class ReportGenerator {
    private final List<String> data = new ArrayList<>();
    // NEW instance for every injection -- safe to have mutable state
}
```

### Scope mismatch problem

```java
@Service  // singleton
public class OrderService {
    private final ShoppingCart cart;  // prototype scope

    public OrderService(ShoppingCart cart) {
        this.cart = cart;  
        // BUG: cart is created ONCE and shared by all threads!
        // Because OrderService is singleton, its dependencies are resolved once.
    }
}
```

Fix with `ObjectProvider`:
```java
@Service
public class OrderService {
    private final ObjectProvider<ShoppingCart> cartProvider;

    public OrderService(ObjectProvider<ShoppingCart> cartProvider) {
        this.cartProvider = cartProvider;
    }

    public void addItem(Item item) {
        ShoppingCart cart = cartProvider.getObject();  // fresh instance each time
        cart.add(item);
    }
}
```

---

## 6. Bean Lifecycle

```text
Full lifecycle of a Spring Bean:

1. INSTANTIATION
   |  Spring calls constructor (+ injects constructor params)
   v
2. POPULATE PROPERTIES
   |  Spring injects @Autowired fields, setter injections
   v
3. Aware interfaces (setBeanName, setBeanFactory, setApplicationContext)
   |  Bean gets references to the container (rarely used directly)
   v
4. BeanPostProcessor.postProcessBEFOREInitialization()        <-- FIRST PASS
   |  (see explanation below)
   v
5. @PostConstruct / InitializingBean / initMethod
   |  YOUR initialization logic runs here
   v
6. BeanPostProcessor.postProcessAFTERInitialization()         <-- SECOND PASS
   |  (see explanation below)
   v
7. BEAN IS READY
   |  Bean is fully initialized and available for use
   |
   |  ... application runs ...
   |
8. @PreDestroy / DisposableBean / destroyMethod
   |  YOUR cleanup logic runs here
   v
9. BEAN IS DESTROYED
```

### Why does BeanPostProcessor run TWICE?

`BeanPostProcessor` is a single interface with two methods:

```java
public interface BeanPostProcessor {

    // FIRST PASS - called BEFORE @PostConstruct
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;    // return the same bean (or a modified one)
    }

    // SECOND PASS - called AFTER @PostConstruct
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;    // return the same bean (or REPLACE it with a proxy)
    }
}
```

They run at different times for different reasons:

```text
FIRST PASS (before @PostConstruct):
  Purpose:  Inspect or modify bean PROPERTIES before init runs
  What:     The bean is still the REAL object
  Example:  @Autowired field injection happens through a BeanPostProcessor
            (AutowiredAnnotationBeanPostProcessor)

  --- then @PostConstruct runs on the real object ---

SECOND PASS (after @PostConstruct):
  Purpose:  REPLACE the fully-initialized bean with a proxy
  What:     Can return a completely different object (the proxy)
  Example:  @Transactional, @Async, @Cacheable proxies are created here
```

### Concrete walkthrough

```text
Say you have:

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository repo;     // needs field injection

    @PostConstruct
    public void init() {
        log.info("PaymentService ready, repo={}", repo);  // needs repo to be injected
    }

    @Transactional
    public void pay(Order order) { ... }  // needs proxy for TX management
}

Step 4 - BEFORE init:
  AutowiredAnnotationBeanPostProcessor runs
  Injects PaymentRepository into the repo field
  Returns the SAME PaymentService object (not a proxy)
  bean = PaymentService (real)

Step 5 - @PostConstruct:
  init() runs on the REAL PaymentService
  repo is already injected, so log.info works correctly

Step 6 - AFTER init:
  Transaction BeanPostProcessor runs
  Sees @Transactional on pay() method
  Creates a CGLIB proxy that wraps PaymentService
  Returns the PROXY instead of the real bean
  bean = PaymentService$$SpringCGLIB$$0 (proxy wrapping real object)

Container now holds the PROXY.
When anyone calls paymentService.pay(), the proxy intercepts it.
```

### Why not create the proxy in the first pass?

```text
If the proxy was created BEFORE @PostConstruct:

  Step 4: proxy = Proxy(PaymentService)    <-- bean replaced with proxy
  Step 5: proxy.init()                     <-- @PostConstruct runs on PROXY
          proxy has no repo field!         <-- BROKEN: NullPointerException

By creating the proxy AFTER @PostConstruct:

  Step 4: bean = PaymentService (real)     <-- still the real object
  Step 5: bean.init()                      <-- runs on real object, works fine
  Step 6: proxy = Proxy(bean)              <-- now wrap the initialized object
```

The order guarantees that **your init code runs on the real, fully-injected object**,
and then the proxy wraps that ready-to-use object.

### Practical lifecycle hooks

```java
@Service
public class CacheWarmupService {

    private final ProductRepository repository;
    private final CacheManager cacheManager;

    public CacheWarmupService(ProductRepository repository, CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void warmUpCache() {
        // Runs AFTER all dependencies are injected, BEFORE serving requests
        log.info("Warming up product cache...");
        List<Product> popular = repository.findTop100ByOrderByViewCountDesc();
        Cache cache = cacheManager.getCache("products");
        popular.forEach(p -> cache.put(p.getId(), p));
        log.info("Cache warmed with {} products", popular.size());
    }

    @PreDestroy
    public void cleanup() {
        // Runs BEFORE the bean is destroyed (application shutdown)
        log.info("Flushing cache metrics...");
        metricsService.flush();
    }
}
```

### @Bean with lifecycle methods

```java
@Configuration
public class DataSourceConfig {

    @Bean(initMethod = "start", destroyMethod = "close")
    public HikariDataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/flowforge");
        ds.setMaximumPoolSize(20);
        return ds;
        // Spring will call ds.start() after creation
        // Spring will call ds.close() on shutdown
    }
}
```

---

## 7. BeanPostProcessor -- How Spring Magic Works

BeanPostProcessors intercept EVERY bean creation. This is how Spring adds behavior:

```java
// This is conceptually what Spring does for @Transactional
public class TransactionBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // Check if bean has @Transactional methods
        if (hasTransactionalMethods(bean)) {
            // Return a PROXY that wraps the real bean
            return createTransactionalProxy(bean);
        }
        return bean;  // no proxy needed
    }
}
```

```text
Your code:     orderService.createOrder()
Actually:      proxy.createOrder()  -->  begin TX  -->  orderService.createOrder()  -->  commit TX

You think you're calling OrderService directly.
You're actually calling a PROXY that Spring created via BeanPostProcessor.
```

This is why `@Transactional` on a private method doesn't work -- the proxy can't intercept it.

---

## 8. ApplicationContext vs BeanFactory

```text
BeanFactory:
  - Basic IoC container
  - Lazy initialization (creates beans on first request)
  - Low memory footprint
  - No event system, no AOP integration

ApplicationContext (extends BeanFactory):
  - Full-featured container
  - Eager initialization (creates singletons at startup)
  - Event publishing (ApplicationEvent)
  - Message resolution (i18n)
  - AOP integration
  - Environment abstraction
  - ResourceLoader

In practice: you ALWAYS use ApplicationContext.
BeanFactory is just the parent interface for understanding.
```

### ApplicationContext types

```text
AnnotationConfigApplicationContext    -- Java config (@Configuration)
ClassPathXmlApplicationContext        -- XML config (legacy)
GenericWebApplicationContext          -- Web applications (Spring MVC)
```

---

## 9. Conditional Beans

```java
// Only create this bean if Redis is available
@Bean
@ConditionalOnProperty(name = "app.cache.type", havingValue = "redis")
public CacheManager redisCacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory).build();
}

// Fallback: in-memory cache if Redis not configured
@Bean
@ConditionalOnProperty(name = "app.cache.type", havingValue = "memory", matchIfMissing = true)
public CacheManager inMemoryCacheManager() {
    return new ConcurrentMapCacheManager("workflows", "users");
}
```

### Common conditional annotations

```text
@ConditionalOnProperty          -- based on config property
@ConditionalOnClass             -- based on class on classpath
@ConditionalOnMissingBean       -- only if no bean of this type exists
@ConditionalOnBean              -- only if another bean exists
@ConditionalOnMissingClass      -- only if class NOT on classpath
@ConditionalOnWebApplication    -- only in web context
@ConditionalOnExpression        -- based on SpEL expression
@Profile                        -- based on active profile
```

### @Profile

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        // H2 in-memory for development
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2).build();
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() {
        // PostgreSQL for production
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(System.getenv("DATABASE_URL"));
        return ds;
    }
}
```

---

## 10. Component Scanning

```java
@SpringBootApplication   // includes @ComponentScan
// Scans the current package AND all sub-packages
public class FlowForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowForgeApplication.class, args);
    }
}
```

```text
Package structure matters:

com.flowforge                  <- @SpringBootApplication here
  +-- controller/              <- scanned (sub-package)
  +-- service/                 <- scanned
  +-- repository/              <- scanned
  +-- config/                  <- scanned

com.other.package/             <- NOT scanned (different root)
```

### Custom scan paths

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.flowforge",
    "com.shared.lib"           // scan additional packages
})
public class FlowForgeApplication { }
```

---

## 11. Events (ApplicationEvent)

Spring's built-in pub/sub within the same application:

```java
// Define event
public record WorkflowCompletedEvent(Long workflowId, String result) {}

// Publish event
@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final ApplicationEventPublisher publisher;

    public void completeWorkflow(Long id) {
        // ... business logic ...
        publisher.publishEvent(new WorkflowCompletedEvent(id, "SUCCESS"));
    }
}

// Listen for event (any bean can listen)
@Component
public class NotificationListener {

    @EventListener
    public void onWorkflowCompleted(WorkflowCompletedEvent event) {
        log.info("Workflow {} completed: {}", event.workflowId(), event.result());
        // send notification
    }
}

// Async listener (non-blocking)
@Component
public class AuditListener {

    @Async
    @EventListener
    public void onWorkflowCompleted(WorkflowCompletedEvent event) {
        // runs in separate thread, doesn't block the publisher
        auditService.log("WORKFLOW_COMPLETED", event.workflowId());
    }
}

// Transactional listener (runs after transaction commits)
@Component
public class OutboxListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowCompleted(WorkflowCompletedEvent event) {
        // Only runs if the transaction that published this event COMMITS
        // Perfect for: sending emails, publishing to Kafka, external calls
        kafkaTemplate.send("workflow-events", event);
    }
}
```

### @TransactionalEventListener phases

```text
BEFORE_COMMIT     -- runs just before the transaction commits
AFTER_COMMIT      -- runs after successful commit (DEFAULT)
AFTER_ROLLBACK    -- runs after transaction rollback
AFTER_COMPLETION  -- runs after commit OR rollback
```

---

## 12. Circular Dependencies

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) { this.serviceB = serviceB; }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    public ServiceB(ServiceA serviceA) { this.serviceA = serviceA; }
}
// FAILS: BeanCurrentlyInCreationException
// Spring can't create A without B, and can't create B without A
```

### How to fix

```text
1. REDESIGN (best):    Extract shared logic into a third service
2. @Lazy:              Break the cycle with lazy initialization
3. Events:             Use ApplicationEvent instead of direct dependency
4. Interface:          Depend on abstraction, not concrete class
```

```java
// Fix with @Lazy
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB;  // gets a proxy, not the real bean
    }
}
```

---

## 13. Interview Deep Dive Questions

```text
Q: What is IoC? How is it different from DI?
A: IoC is the PRINCIPLE (container controls object creation).
   DI is the MECHANISM (container injects dependencies into objects).
   IoC is the "what", DI is the "how".

Q: Why is constructor injection preferred over field injection?
A: Immutability (final), fail-fast (startup error if missing),
   testability (pass mocks directly), no reflection needed.

Q: What happens if two beans implement the same interface?
A: NoUniqueBeanDefinitionException. Fix with @Primary, @Qualifier,
   or injection by parameter name matching.

Q: What is the difference between @Component and @Bean?
A: @Component = class-level, Spring auto-detects via scanning.
   @Bean = method-level in @Configuration, explicit factory method.
   Use @Bean for third-party classes you can't annotate.

Q: Explain the bean lifecycle.
A: Constructor -> Dependency Injection -> @PostConstruct ->
   BeanPostProcessor -> Ready -> @PreDestroy -> Destroy

Q: What creates the proxy for @Transactional?
A: A BeanPostProcessor runs postProcessAfterInitialization(),
   detects @Transactional, wraps the bean in a CGLIB/JDK proxy.

Q: Why doesn't @Transactional work on private methods?
A: The proxy can't override private methods. The call goes
   directly to the target, bypassing the proxy.

Q: What is the difference between singleton and prototype scope?
A: Singleton = one instance shared, prototype = new instance per injection.
   Prototype beans are NOT managed after creation (no @PreDestroy).

Q: What happens when you inject a prototype into a singleton?
A: The prototype is created ONCE and cached in the singleton.
   Use ObjectProvider or @Lookup to get fresh instances.
```
