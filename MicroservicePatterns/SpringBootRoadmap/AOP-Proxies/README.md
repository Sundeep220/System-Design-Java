# Phase 17 -- AOP & Proxies

This is the mechanism behind `@Transactional`, `@Async`, `@Cacheable`, `@PreAuthorize`, and many more. If you don't understand proxies, you will write bugs that are extremely hard to find.

---

## 1. The Problem AOP Solves

```java
// WITHOUT AOP: cross-cutting concerns pollute every method
public class WorkflowService {

    public Workflow create(WorkflowCreateRequest request) {
        long start = System.currentTimeMillis();           // LOGGING
        log.info("Creating workflow: {}", request.name()); // LOGGING
        SecurityContext.checkPermission("WORKFLOW_CREATE"); // SECURITY
        TransactionManager.begin();                         // TRANSACTION
        try {
            Workflow wf = new Workflow(request);
            repository.save(wf);
            TransactionManager.commit();                    // TRANSACTION
            long duration = System.currentTimeMillis() - start;
            metrics.record("workflow.create", duration);    // METRICS
            return wf;
        } catch (Exception e) {
            TransactionManager.rollback();                  // TRANSACTION
            log.error("Failed to create workflow", e);     // LOGGING
            throw e;
        }
    }
    // Every method has the same boilerplate!
}
```

```java
// WITH AOP: clean business logic, cross-cutting concerns handled separately
@Service
@Transactional                    // AOP handles transaction
@PreAuthorize("hasRole('ADMIN')") // AOP handles security
public class WorkflowService {

    @Timed("workflow.create")     // AOP handles metrics
    public Workflow create(WorkflowCreateRequest request) {
        Workflow wf = new Workflow(request);
        return repository.save(wf);
        // ONLY business logic. That's it.
    }
}
```

---

## 2. AOP Terminology

```text
Term           What It Means                     Example
---------------------------------------------------------------------------
Aspect         A module of cross-cutting concern  TransactionAspect, LoggingAspect
Join Point     A point in execution               Method call, field access
Pointcut       Expression matching join points    "all methods in *Service classes"
Advice         Code to run at a join point        "start transaction before method"
Target Object  The real object being proxied      WorkflowService (your code)
Proxy          The wrapper Spring creates         TransactionalProxy<WorkflowService>
Weaving        Applying aspects to targets        At runtime (Spring default)
```

### Advice types

```text
@Before          -- runs BEFORE the target method
@After           -- runs AFTER the method (regardless of outcome)
@AfterReturning  -- runs AFTER the method returns successfully
@AfterThrowing   -- runs AFTER the method throws an exception
@Around          -- wraps the method: runs before AND after
                    (most powerful, you control when/if the method executes)
```

```text
                   @Before
                      |
                      v
              +---------------+
              | Target Method |
              +-------+-------+
                 |         |
          success|         |exception
                 v         v
         @AfterReturning  @AfterThrowing
                 |         |
                 +----+----+
                      |
                      v
                   @After

@Around wraps ALL of this -- you decide when to call proceed()
```

---

## 3. How Spring Creates Proxies

When Spring detects that a bean needs AOP (has `@Transactional`, `@Cacheable`, etc.), it creates a **proxy** that wraps your real bean.

```text
What you THINK happens:
  Controller -> WorkflowService.create()

What ACTUALLY happens:
  Controller -> Proxy$$WorkflowService.create()
                    |
                    |-- begin transaction
                    |-- call REAL WorkflowService.create()
                    |-- commit transaction (or rollback on exception)
                    |-- return result
```

### Two proxy types

```text
JDK Dynamic Proxy:
  - Target class MUST implement an interface
  - Proxy implements the same interface
  - Uses java.lang.reflect.Proxy
  - Slightly faster to create

CGLIB Proxy (Spring Boot default):
  - Creates a SUBCLASS of your bean
  - No interface required
  - Uses bytecode generation (net.sf.cglib)
  - Works with concrete classes
  - Cannot proxy final classes or final methods
```

```text
Spring Boot default: CGLIB (spring.aop.proxy-target-class=true)

Your class:
  public class WorkflowService { ... }

Spring creates:
  public class WorkflowService$$SpringCGLIB$$0 extends WorkflowService {
      @Override
      public Workflow create(WorkflowCreateRequest request) {
          // 1. begin transaction
          // 2. super.create(request)  <- calls YOUR real method
          // 3. commit or rollback
      }
  }
```

---

## 4. The Self-Invocation Trap (CRITICAL BUG)

```java
@Service
public class WorkflowService {

    @Transactional
    public void processAll() {
        List<Workflow> workflows = repository.findAll();
        for (Workflow wf : workflows) {
            processOne(wf);   // THIS CALL BYPASSES THE PROXY!
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Workflow wf) {
        // You EXPECT a new transaction for each workflow
        // But this runs in the SAME transaction as processAll()
        // Because the call goes directly to "this", not through the proxy
    }
}
```

```text
External call:                  Internal call (self-invocation):

Controller                      processAll() method body
    |                               |
    v                               v
  Proxy.processOne()            this.processOne()
    |                               |
    v                               |  <- NO PROXY!
  Begin TX                          |  <- NO new transaction!
    |                               v
  Real processOne()             Real processOne()
    |                               |
  Commit TX                     (runs in parent's transaction)
```

### Fixes for self-invocation

```java
// Fix 1: Inject self (recommended for simple cases)
@Service
public class WorkflowService {
    @Lazy
    private final WorkflowService self;  // injects the PROXY

    public WorkflowService(@Lazy WorkflowService self) {
        this.self = self;
    }

    public void processAll() {
        for (Workflow wf : repository.findAll()) {
            self.processOne(wf);  // goes through the proxy -> new TX
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Workflow wf) { }
}

// Fix 2: Extract to a separate service (cleanest)
@Service
public class WorkflowBatchService {
    private final WorkflowProcessor processor;

    public void processAll() {
        for (Workflow wf : repository.findAll()) {
            processor.processOne(wf);  // different bean -> goes through proxy
        }
    }
}

@Service
public class WorkflowProcessor {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Workflow wf) { }
}

// Fix 3: Use AopContext (less clean)
@Service
public class WorkflowService {
    public void processAll() {
        WorkflowService proxy = (WorkflowService) AopContext.currentProxy();
        for (Workflow wf : repository.findAll()) {
            proxy.processOne(wf);  // goes through proxy
        }
    }
}
```

---

## 5. Why @Transactional on Private Methods Doesn't Work

```java
@Service
public class WorkflowService {

    @Transactional   // THIS DOES NOTHING!
    private void saveInternal(Workflow wf) {
        repository.save(wf);
    }
}
```

```text
CGLIB creates a subclass:
  class WorkflowService$$CGLIB extends WorkflowService {
      @Override
      public void create(...) { ... }  // CAN override public/protected
      
      // CANNOT override private methods!
      // Java doesn't allow subclasses to see private methods
      // So the proxy can't intercept the call
  }

Rule: @Transactional, @Cacheable, @Async etc. only work on:
  - public methods (always works)
  - protected methods (works with CGLIB)
  - package-private methods (works with CGLIB)
  - NEVER on private methods
  - NEVER on final methods (CGLIB can't override them)
  - NEVER on static methods
```

---

## 6. Writing Custom Aspects

### Logging aspect

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Pointcut: all public methods in any class under com.flowforge.service
    @Around("execution(* com.flowforge.service..*(..))")
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("-> {}.{}() called", className, methodName);
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();  // call the real method
            long duration = System.currentTimeMillis() - start;
            log.info("<- {}.{}() returned in {}ms", className, methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("<- {}.{}() threw {} after {}ms",
                className, methodName, e.getClass().getSimpleName(), duration);
            throw e;  // re-throw so normal exception handling works
        }
    }
}
```

### Performance monitoring aspect

```java
@Aspect
@Component
public class PerformanceAspect {

    private final MeterRegistry meterRegistry;

    // Custom annotation
    @Around("@annotation(timed)")
    public Object measureTime(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        String metricName = timed.value();

        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder(metricName)
                .tag("status", "success")
                .register(meterRegistry));
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder(metricName)
                .tag("status", "error")
                .tag("exception", e.getClass().getSimpleName())
                .register(meterRegistry));
            throw e;
        }
    }
}

// Usage
@Service
public class WorkflowService {
    @Timed("workflow.create")
    public Workflow create(WorkflowCreateRequest request) { ... }
}
```

### Retry aspect

```java
@Aspect
@Component
public class RetryAspect {

    @Around("@annotation(retryable)")
    public Object retry(ProceedingJoinPoint joinPoint, Retryable retryable) throws Throwable {
        int maxAttempts = retryable.maxAttempts();
        long delay = retryable.delayMs();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    log.warn("Attempt {}/{} failed for {}, retrying in {}ms",
                        attempt, maxAttempts,
                        joinPoint.getSignature().getName(), delay);
                    Thread.sleep(delay);
                    delay *= 2;  // exponential backoff
                }
            }
        }
        throw lastException;
    }
}

@Target(METHOD)
@Retention(RUNTIME)
public @interface Retryable {
    int maxAttempts() default 3;
    long delayMs() default 1000;
}
```

---

## 7. Pointcut Expressions

```text
Expression                                      Matches
---------------------------------------------------------------------------
execution(* com.flowforge.service.*.*(..))      All methods in service package
execution(public * *(..))                       All public methods
execution(* save*(..))                          All methods starting with "save"
execution(* *(..) throws Exception)             Methods that declare throws Exception

@annotation(com.flowforge.Timed)                Methods annotated with @Timed
@within(org.springframework.stereotype.Service) Methods in @Service classes

bean(workflowService)                           Methods on bean named "workflowService"
bean(*Service)                                  Methods on beans ending with "Service"

within(com.flowforge.service..*)                All methods in service package + sub-packages
```

### Combining pointcuts

```java
@Aspect
@Component
public class AuditAspect {

    // Reusable pointcuts
    @Pointcut("within(com.flowforge.service..*)")
    private void inServiceLayer() {}

    @Pointcut("execution(* create*(..)) || execution(* update*(..)) || execution(* delete*(..))")
    private void writeOperations() {}

    // Combined: audit all write operations in service layer
    @AfterReturning(
        pointcut = "inServiceLayer() && writeOperations()",
        returning = "result")
    public void auditWrite(JoinPoint joinPoint, Object result) {
        String method = joinPoint.getSignature().getName();
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.log(user, method, result);
    }
}
```

---

## 8. Aspect Ordering

When multiple aspects apply to the same method:

```java
@Aspect
@Order(1)   // runs first (outermost)
public class SecurityAspect { }

@Aspect
@Order(2)   // runs second
public class TransactionAspect { }

@Aspect
@Order(3)   // runs last (innermost, closest to real method)
public class LoggingAspect { }
```

```text
Request
  -> SecurityAspect.around()       [Order 1]
    -> TransactionAspect.around()  [Order 2]
      -> LoggingAspect.around()    [Order 3]
        -> REAL METHOD
      <- LoggingAspect returns
    <- TransactionAspect commits
  <- SecurityAspect returns
Response

Like layers of an onion: outermost aspect wraps everything.
```

---

## 9. Spring's Built-in AOP Usage

Every one of these annotations works because of AOP proxies:

```text
@Transactional     -- TransactionInterceptor wraps method in begin/commit/rollback
@Cacheable         -- CacheInterceptor checks cache before calling method
@Async             -- AsyncExecutionInterceptor runs method in a different thread
@PreAuthorize      -- MethodSecurityInterceptor checks authorization
@Scheduled         -- Not AOP, but lifecycle-based (ScheduledAnnotationBeanPostProcessor)
@Retryable         -- RetryInterceptor retries on failure (Spring Retry)
@CircuitBreaker    -- Resilience4j wraps method with circuit breaker logic
```

### Understanding @Transactional as AOP

```java
// What you write:
@Transactional
public void transferMoney(Long from, Long to, BigDecimal amount) {
    accountRepository.debit(from, amount);
    accountRepository.credit(to, amount);
}

// What the proxy does:
public void transferMoney(Long from, Long to, BigDecimal amount) {
    TransactionStatus tx = transactionManager.getTransaction(definition);
    try {
        target.transferMoney(from, to, amount);  // YOUR real method
        transactionManager.commit(tx);
    } catch (RuntimeException e) {
        transactionManager.rollback(tx);
        throw e;
    }
}
```

---

## 10. When AOP Is the Wrong Choice

```text
USE AOP for:
  - Logging / Metrics / Tracing
  - Security checks
  - Transaction management
  - Caching
  - Retry logic
  - Rate limiting

DON'T USE AOP for:
  - Business logic (should be explicit in service code)
  - Data transformation (use mappers)
  - Complex conditional behavior (becomes invisible and hard to debug)
  - Performance-critical hot paths (proxy overhead matters at >100K RPS)
```

---

## 11. Debugging Proxy Issues

### Check if a bean is proxied

```java
@PostConstruct
public void debugProxy() {
    log.info("WorkflowService class: {}", workflowService.getClass().getName());
    // If proxied: com.flowforge.service.WorkflowService$$SpringCGLIB$$0
    // If not:      com.flowforge.service.WorkflowService

    boolean isProxy = AopUtils.isAopProxy(workflowService);
    boolean isCglib = AopUtils.isCglibProxy(workflowService);
    boolean isJdk = AopUtils.isJdkDynamicProxy(workflowService);
    log.info("isProxy={}, isCglib={}, isJdk={}", isProxy, isCglib, isJdk);
}
```

### Common proxy bugs

```text
Bug: @Transactional not working
Check:
  1. Is the method public? (private = no proxy)
  2. Is it a self-invocation? (this.method() bypasses proxy)
  3. Is the exception checked? (only unchecked rolls back by default)
  4. Is the class a Spring bean? (@Component/@Service?)
  5. Is @EnableTransactionManagement present? (auto in Spring Boot)

Bug: @Cacheable returning stale data
Check:
  1. Is it self-invocation? (cache only works through proxy)
  2. Is the cache key correct? (default = method args)
  3. Is the return value serializable? (for Redis cache)

Bug: @Async not running in a separate thread
Check:
  1. Is @EnableAsync present?
  2. Is the method public?
  3. Is it self-invocation?
  4. Is the return type void or Future/CompletableFuture?
```

---

## 12. Interview Questions

```text
Q: What is AOP and why is it used in Spring?
A: AOP (Aspect-Oriented Programming) separates cross-cutting concerns
   (logging, security, transactions) from business logic. Spring
   implements AOP by creating proxy objects that wrap your beans and
   add behavior before/after method calls.

Q: How does Spring create proxies?
A: Two mechanisms: JDK Dynamic Proxy (for interfaces) and CGLIB
   (for concrete classes, creates a subclass). Spring Boot defaults
   to CGLIB. Proxies are created by BeanPostProcessors during
   context refresh.

Q: Why doesn't @Transactional work on a private method?
A: CGLIB proxy creates a subclass. Java doesn't allow overriding
   private methods, so the proxy can't intercept the call.
   Same applies to final methods and static methods.

Q: Explain the self-invocation problem.
A: When method A() calls method B() within the same class using
   "this.B()", the call goes directly to the target object,
   bypassing the proxy. So @Transactional/@Cacheable on B() won't
   work. Fix: extract B() to a separate bean, or inject self with @Lazy.

Q: What is the difference between @Before, @After, and @Around?
A: @Before runs before the method. @After runs after (always).
   @Around wraps the method -- you call proceed() to invoke it,
   giving you full control over whether/when it executes, and you
   can modify the return value.

Q: How is @Transactional implemented?
A: A BeanPostProcessor detects beans with @Transactional methods.
   It wraps the bean in a CGLIB proxy. The proxy's method calls
   TransactionInterceptor, which begins a transaction, calls the
   real method, and commits or rolls back based on the outcome.
```
