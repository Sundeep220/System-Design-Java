# Phase 16 -- Exception Handling & Validation

How to validate input at every layer and return consistent, useful error responses.

---

## 1. Three Layers of Validation

```text
Layer 1: INPUT VALIDATION (Controller)
   "Is the JSON well-formed? Is name non-blank? Is age >= 0?"
   -> Bean Validation (@Valid, @NotBlank, @Min, @Email)
   -> Reject BEFORE hitting business logic
   -> Returns 400 Bad Request

Layer 2: BUSINESS VALIDATION (Service)
   "Does the workflow exist? Can this user modify it? Is the state transition valid?"
   -> Custom exceptions (ResourceNotFoundException, InvalidStateException)
   -> Reject because business rules are violated
   -> Returns 404, 409, 422

Layer 3: DATABASE CONSTRAINTS (Repository/DB)
   "Is the name unique? Does the foreign key reference exist?"
   -> Unique constraints, FK constraints, check constraints
   -> Last line of defense
   -> Returns 409 Conflict
```

```text
Why all three matter:

Input validation alone:     catches typos but not business logic violations
Business validation alone:  catches logic errors but misses malformed input
DB constraints alone:       catches everything but with ugly SQL exceptions

All three together:         correct, user-friendly, and safe
```

---

## 2. Bean Validation (Jakarta Validation)

### Common annotations

```java
public record WorkflowCreateRequest(

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be 1-100 characters")
    String name,

    @Size(max = 500, message = "Description must be <= 500 characters")
    String description,

    @NotNull(message = "Max retries is required")
    @Min(value = 0, message = "Max retries must be >= 0")
    @Max(value = 10, message = "Max retries must be <= 10")
    Integer maxRetries,

    @NotNull
    @Min(1) @Max(3600)
    Integer timeoutSeconds,

    @Email(message = "Must be a valid email")
    String notificationEmail,

    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
    String slug,

    @NotEmpty(message = "At least one tag required")
    List<@NotBlank String> tags,      // validate EACH element in the list

    @Valid                             // validate nested object
    @NotNull
    RetryConfig retryConfig
) {}

public record RetryConfig(
    @Min(0) @Max(10) int maxAttempts,
    @Min(100) @Max(60000) long delayMs
) {}
```

### Annotation reference

```text
@NotNull      -- not null (but can be empty string "")
@NotEmpty     -- not null AND not empty (string, collection, map)
@NotBlank     -- not null AND not empty AND not whitespace (strings only)
@Size         -- length/size within range
@Min / @Max   -- numeric min/max
@Email        -- valid email format
@Pattern      -- regex match
@Past         -- date must be in the past
@Future       -- date must be in the future
@Positive     -- number > 0
@PositiveOrZero -- number >= 0
@Valid        -- validate nested object recursively
```

### @Valid vs @Validated -- Deep Dive

In the most common use case (controller `@RequestBody`), they behave **identically**:

```java
// These two do the EXACT SAME THING:
@PostMapping("/workflows")
public void create(@Valid @RequestBody WorkflowCreateRequest req) {}

@PostMapping("/workflows")
public void create(@Validated @RequestBody WorkflowCreateRequest req) {}

// Both validate the body. Both throw MethodArgumentNotValidException if invalid.
// No difference at all for this basic case.
```

So **why does `@Validated` exist?** It solves two problems that `@Valid` cannot.

#### Problem 1: Different validation rules for Create vs Update (Validation Groups)

Imagine this for FlowForge:

```text
POST /workflows   (create)  ->  ID must NOT be in the body (server generates it)
PUT  /workflows/5 (update)  ->  ID is required, name must NOT be blank
```

With `@Valid`, you'd need two almost-identical DTO classes. With `@Validated` + groups,
one class handles both:

```java
// Step 1: Define marker interfaces (empty, just tags)
public interface OnCreate {}
public interface OnUpdate {}

// Step 2: One DTO with conditional rules
public record WorkflowRequest(

    @Null(groups = OnCreate.class, message = "Don't send ID when creating")
    @NotNull(groups = OnUpdate.class, message = "ID is required when updating")
    Long id,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    String name,

    String description,

    @NotNull(groups = OnCreate.class, message = "Timeout required when creating")
    Integer timeoutSeconds
) {}
```

```java
// Step 3: Tell Spring WHICH rules to run

@PostMapping("/workflows")
public void create(@Validated(OnCreate.class) @RequestBody WorkflowRequest req) {
    // Runs ONLY the validations tagged with OnCreate:
    //   id    -> @Null(OnCreate)     -> id must be null          CHECKED
    //   name  -> @NotBlank(OnCreate) -> name must not be blank   CHECKED
    //   timeoutSeconds -> @NotNull(OnCreate) -> must be set      CHECKED
    //   id    -> @NotNull(OnUpdate)  ->                          SKIPPED (wrong group)
}

@PutMapping("/{id}")
public void update(@Validated(OnUpdate.class) @RequestBody WorkflowRequest req) {
    // Runs ONLY the validations tagged with OnUpdate:
    //   id    -> @NotNull(OnUpdate)  -> id must be present       CHECKED
    //   name  -> @NotBlank(OnUpdate) -> name must not be blank   CHECKED
    //   id    -> @Null(OnCreate)     ->                          SKIPPED (wrong group)
    //   timeoutSeconds -> @NotNull(OnCreate) ->                  SKIPPED (wrong group)
}
```

What happens with `@Valid` instead?

```text
@PostMapping("/workflows")
public void create(@Valid @RequestBody WorkflowRequest req) {
    // @Valid ALWAYS runs the DEFAULT group.
    // @Null(groups = OnCreate.class) is NOT in the DEFAULT group.
    // So it is COMPLETELY IGNORED. No validation runs on id at all.
    // Client sends {"id": 999, "name": "test"} -> passes validation!
    // That's a bug.
}

Key insight:
  @Valid                  -> always runs DEFAULT group (annotations WITHOUT explicit groups)
  @Validated(OnCreate)   -> runs ONLY OnCreate group
  @Validated(OnUpdate)   -> runs ONLY OnUpdate group

  An annotation with groups = OnCreate is NOT in the DEFAULT group.
  So @Valid simply ignores it.
```

#### The Default Group Trap

This is the most common mistake with validation groups.

```java
@NotBlank(groups = OnCreate.class)   // tagged with OnCreate
@Size(max = 100)                     // no tag = Default group
String name;
```

```java
@Validated(OnCreate.class) @RequestBody ...
```

What actually runs?
```text
✅ @NotBlank — tagged with OnCreate
❌ @Size    — tagged with Default, but we only asked for OnCreate

The @Size check is silently skipped! A 10,000 character name passes validation.
```

**Fix:** make your group interface extend `jakarta.validation.groups.Default`:

```java
public interface OnCreate extends Default {}
```

Now `@Validated(OnCreate.class)` runs **both**:
- All `OnCreate`-tagged rules
- All `Default`-tagged rules (annotations with no group)

#### Real-World Example: PATCH with Validation Groups

PATCH is the most common use case for validation groups — `name` is required on CREATE
but optional on PATCH.

```java
// PATCH DTO — name is optional, but if provided, still validated
public record WorkflowPatchRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        @ValidWorkflowName
        Optional<String> name,

        @Size(max = 500)
        Optional<String> description,

        @Min(0) @Max(10)
        Optional<Integer> maxRetries
) {}
```

```java
@PatchMapping("/{id}")
public ResponseEntity<?> patch(
        @PathVariable UUID id,
        @Valid @RequestBody WorkflowPatchRequest request) {
    // @Valid = Default group
    // No @NotBlank on this DTO, so missing name is fine
    // @Size and @ValidWorkflowName still run if name is provided
}
```

Contrast with CREATE where `name` is required:

```java
public record WorkflowCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        @ValidWorkflowName
        String name
) {}

@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody WorkflowCreateRequest request) {
    // @NotBlank runs — name is required
}
```

Same field, different rules, different DTOs. Groups let you share rules across DTOs
when they overlap.

#### When to Use Groups vs Separate DTOs

```text
Approach              When to use
--------------------  ------------------------------------------------------------
Separate DTOs         Different fields per operation (CREATE has no id, PATCH
                      uses Optional<>). This is what most Spring Boot apps do.

Validation Groups     Same DTO shared across operations, but rules differ per
                      operation.

Both                  Complex APIs where some DTOs are shared but rules vary.
```

#### Validation Groups Cheat Sheet

```text
┌─────────────────────────────────────────────────────────┐
│ @Valid              → runs Default group only            │
│ @Validated          → runs Default group only            │
│ @Validated(X.class) → runs group X only                  │
│                       (add "extends Default" to also     │
│                        run Default-group annotations)    │
├─────────────────────────────────────────────────────────┤
│ @NotBlank                        → Default group         │
│ @NotBlank(groups = OnCreate.class) → OnCreate group      │
│ @NotBlank(groups = {OnCreate.class, Default.class})      │
│                                  → both groups           │
├─────────────────────────────────────────────────────────┤
│ interface OnCreate {}            → standalone group      │
│ interface OnCreate extends Default {} → includes Default │
└─────────────────────────────────────────────────────────┘
```

#### How Groups Actually Work — They Are Gates, Not Conditions

A common misconception is that `@NotBlank(groups = OnCreate.class)` adds an extra condition.
It does NOT. The group is a **gate/filter** that controls whether the annotation runs at all.

```text
@NotBlank(groups = OnCreate.class)

This means: "Only CHECK @NotBlank when OnCreate group is activated."
NOT: "Check @NotBlank AND check if it's a create operation."

The group is a SWITCH — on or off:

┌──────────────────────────────────┬──────────────────┬───────────────────┐
│ Controller annotation            │ OnCreate active? │ @NotBlank runs?   │
├──────────────────────────────────┼──────────────────┼───────────────────┤
│ @Validated(OnCreate.class)       │ ✅ Yes           │ ✅ Yes — checks   │
│ @Validated(OnUpdate.class)       │ ❌ No            │ ❌ Skipped        │
│ @Valid                           │ ❌ No            │ ❌ Skipped        │
└──────────────────────────────────┴──────────────────┴───────────────────┘
```

#### FlowForge Implementation — Groups in Action

```java
// Group interfaces extend Default so non-grouped annotations still run
public interface OnCreate extends Default {}
public interface OnUpdate extends Default {}
```

```java
// CREATE DTO — name is required
public record WorkflowCreateRequest(
        @NotBlank(groups = OnCreate.class)    // gate: only runs when OnCreate is active
        @Size(max = 100)                      // no group = Default = always runs
        @ValidWorkflowName                    // no group = Default = always runs
        String name, ...
)

// UPDATE DTO — name is also required
public record WorkflowUpdateRequest(
        @NotBlank(groups = OnUpdate.class)    // gate: only runs when OnUpdate is active
        @Size(max = 100)
        @ValidWorkflowName
        String name, ...
)

// PATCH DTO — name is OPTIONAL (no @NotBlank at all)
public record WorkflowPatchRequest(
        // NO @NotBlank here
        Optional<@Size(max = 100) @ValidWorkflowName String> name, ...
)
```

```java
// Controller activates the right group per endpoint
@PostMapping
public ... create(@Validated(OnCreate.class) @RequestBody WorkflowCreateRequest req) {
    // OnCreate active → @NotBlank fires → name required
}

@PutMapping("/{id}")
public ... update(@Validated(OnUpdate.class) @RequestBody WorkflowUpdateRequest req) {
    // OnUpdate active → @NotBlank fires → name required
}

@PatchMapping("/{id}")
public ... patch(@Valid @RequestBody WorkflowPatchRequest req) {
    // Default group only → no @NotBlank exists on this DTO → name optional
    // But @Size and @ValidWorkflowName still run if name is provided
}
```

```text
Result:

POST  {"name": ""}           →  400 — @NotBlank fires (OnCreate group active)
PUT   {"name": ""}           →  400 — @NotBlank fires (OnUpdate group active)
PATCH {}                     →  200 — no @NotBlank on PatchRequest, Optional.empty() = skip
PATCH {"name": "Bad@Name!"}  →  400 — @ValidWorkflowName fires (Default group, always active)
PATCH {"name": "Good Name"}  →  200 — passes @Size and @ValidWorkflowName
```

#### Problem 2: Validating method parameters in Service classes

```java
@Service
public class WorkflowService {

    // Does @NotNull @Positive actually validate here? NO.
    public Workflow findById(@NotNull @Positive Long id) {
        return repo.findById(id).orElseThrow();
    }

    // Does @Valid actually validate here? NO.
    public void create(@Valid WorkflowCreateRequest request) { }
}

// workflowService.findById(null);   -> NullPointerException (no validation happened)
// workflowService.findById(-5L);    -> runs with bad data (no validation happened)
```

Why? `@Valid` on `@RequestBody` works because **Spring MVC** has special argument
resolution code. But calling a service method directly is just a regular Java call --
no Spring MVC involved.

Fix: add `@Validated` on the **class**:

```java
@Service
@Validated   // THIS activates a validation PROXY around the entire class
public class WorkflowService {

    public Workflow findById(@NotNull @Positive Long id) {
        return repo.findById(id).orElseThrow();
    }

    public void create(@Valid WorkflowCreateRequest request) { }
}

// NOW:
// workflowService.findById(null);  -> ConstraintViolationException BEFORE method runs
// workflowService.findById(-5L);   -> ConstraintViolationException BEFORE method runs
// workflowService.create(badReq);  -> validates WorkflowCreateRequest BEFORE method runs
```

This matters when the service is called from multiple entry points:

```text
Your service is called from:
  1. REST Controller    -> @Valid on @RequestBody works (Spring MVC handles it)
  2. Kafka consumer     -> No Spring MVC -> @Valid does NOTHING without @Validated on class
  3. Scheduler          -> No Spring MVC -> @Valid does NOTHING without @Validated on class
  4. Another service    -> No Spring MVC -> @Valid does NOTHING without @Validated on class

With @Validated on the service CLASS:
  ALL four callers get validation, because the proxy intercepts every call.
```

#### Summary table

```text
Scenario                                    @Valid             @Validated
---------------------------------------------------------------------------
Validate @RequestBody in controller         Works              Works (same result)

Different rules for create vs update        CANNOT             @Validated(OnCreate.class)
(validation groups)                         (always DEFAULT)   picks which rules to run

Validate params in @Service methods         DOES NOTHING       Put on CLASS to activate
(called from Kafka, scheduler, etc.)        (no proxy)         validation proxy

Validate nested objects inside a DTO        @Valid on field     Does NOT do nesting
                                            (recursive)        (use @Valid for this)

Simple rule:
  - Start with @Valid everywhere (works for most controllers)
  - Switch to @Validated when you need groups or service-layer validation
  - For nested objects in DTOs, always use @Valid (never @Validated)
```

---

## 3. Custom Validators

### Annotation-based custom validator

```java
// Custom annotation
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = NoProfanityValidator.class)
public @interface NoProfanity {
    String message() default "Name contains prohibited words";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private static final Set<String> BLOCKED = Set.of("badword1", "badword2");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;  // @NotNull handles null check
        return BLOCKED.stream().noneMatch(word ->
            value.toLowerCase().contains(word));
    }
}

// Usage
public record WorkflowCreateRequest(
    @NotBlank
    @NoProfanity
    String name
) {}
```

### Cross-field validation

```java
// Class-level validator: compare two fields
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class DateRangeValidator
        implements ConstraintValidator<ValidDateRange, ExecutionFilterRequest> {

    @Override
    public boolean isValid(ExecutionFilterRequest req, ConstraintValidatorContext ctx) {
        if (req.startedAfter() == null || req.startedBefore() == null) return true;
        return req.startedBefore().isAfter(req.startedAfter());
    }
}

@ValidDateRange
public record ExecutionFilterRequest(
    Instant startedAfter,
    Instant startedBefore
) {}
```

### Validator with Spring dependency injection

```java
public class UniqueWorkflowNameValidator
        implements ConstraintValidator<UniqueWorkflowName, String> {

    private final WorkflowRepository repository;

    // Spring injects the repository
    public UniqueWorkflowNameValidator(WorkflowRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isValid(String name, ConstraintValidatorContext ctx) {
        if (name == null) return true;
        return !repository.existsByName(name);
    }
}
```

---

## 4. Exception Hierarchy

### Design your exceptions

```java
// Base exception
public abstract class FlowForgeException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    protected FlowForgeException(String message, ErrorCode errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    // getters
}

// Specific exceptions
public class ResourceNotFoundException extends FlowForgeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(
            String.format("%s not found with id: %s", resource, id),
            ErrorCode.RESOURCE_NOT_FOUND,
            HttpStatus.NOT_FOUND
        );
    }
}

public class ConflictException extends FlowForgeException {
    public ConflictException(String message) {
        super(message, ErrorCode.CONFLICT, HttpStatus.CONFLICT);
    }
}

public class InvalidStateTransitionException extends FlowForgeException {
    public InvalidStateTransitionException(String from, String to) {
        super(
            String.format("Cannot transition from %s to %s", from, to),
            ErrorCode.INVALID_STATE,
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}

public class RateLimitExceededException extends FlowForgeException {
    public RateLimitExceededException() {
        super("Rate limit exceeded", ErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS);
    }
}
```

### Error codes enum

```java
public enum ErrorCode {
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    INVALID_STATE,
    UNAUTHORIZED,
    FORBIDDEN,
    RATE_LIMITED,
    INTERNAL_ERROR,
    SERVICE_UNAVAILABLE
}
```

---

## 5. Global Exception Handler (@ControllerAdvice)

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // --- Bean Validation errors (400) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(
                fe.getField(),
                fe.getRejectedValue(),
                fe.getDefaultMessage()))
            .toList();

        return buildResponse(request, HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR, "Validation failed", fieldErrors);
    }

    // --- @RequestParam / @PathVariable type mismatch (400) ---
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format("Parameter '%s' must be of type %s",
            ex.getName(), ex.getRequiredType().getSimpleName());

        return buildResponse(request, HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR, message, null);
    }

    // --- Malformed JSON (400) ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return buildResponse(request, HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR, "Malformed JSON request body", null);
    }

    // --- Missing required @RequestParam (400) ---
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        return buildResponse(request, HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR,
            "Missing required parameter: " + ex.getParameterName(), null);
    }

    // --- HTTP method not allowed (405) ---
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return buildResponse(request, HttpStatus.METHOD_NOT_ALLOWED,
            ErrorCode.VALIDATION_ERROR,
            "Method " + ex.getMethod() + " not supported", null);
    }

    // --- All custom business exceptions ---
    @ExceptionHandler(FlowForgeException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            FlowForgeException ex,
            HttpServletRequest request) {

        return buildResponse(request, ex.getHttpStatus(),
            ex.getErrorCode(), ex.getMessage(), null);
    }

    // --- Optimistic locking (409) ---
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        return buildResponse(request, HttpStatus.CONFLICT,
            ErrorCode.CONFLICT,
            "Resource was modified by another request. Please retry.", null);
    }

    // --- DB unique constraint violation (409) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String message = "Data integrity violation";
        if (ex.getCause() instanceof ConstraintViolationException cve) {
            message = "Duplicate or invalid data: " + cve.getConstraintName();
        }

        return buildResponse(request, HttpStatus.CONFLICT,
            ErrorCode.CONFLICT, message, null);
    }

    // --- CATCH-ALL: unhandled exceptions (500) ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        // LOG the real error server-side
        log.error("Unhandled exception at {} {}: {}",
            request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        // NEVER expose internals to client
        return buildResponse(request, HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_ERROR,
            "An unexpected error occurred", null);
    }

    // --- Builder ---
    private ResponseEntity<ApiError> buildResponse(
            HttpServletRequest request,
            HttpStatus status,
            ErrorCode code,
            String message,
            List<ApiError.FieldError> details) {

        ApiError error = new ApiError(
            Instant.now(),
            status.value(),
            code.name(),
            message,
            request.getRequestURI(),
            MDC.get("traceId"),
            details
        );
        return ResponseEntity.status(status).body(error);
    }
}
```

### Standard error response

```java
public record ApiError(
    Instant timestamp,
    int status,
    String error,          // error code enum name
    String message,        // human-readable message
    String path,           // request URI
    String traceId,        // correlation ID for log lookup
    List<FieldError> details  // field-level errors (for validation)
) {
    public record FieldError(
        String field,
        Object rejected,
        String message
    ) {}
}
```

### Example responses

```json
// 400 - Validation error
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/workflows",
  "traceId": "abc-123",
  "details": [
    { "field": "name", "rejected": "", "message": "Name is required" },
    { "field": "maxRetries", "rejected": -1, "message": "Max retries must be >= 0" }
  ]
}

// 404 - Not found
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Workflow not found with id: 42",
  "path": "/api/v1/workflows/42",
  "traceId": "abc-124",
  "details": null
}

// 500 - Internal error (NEVER expose stack trace)
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 500,
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "path": "/api/v1/workflows",
  "traceId": "abc-125",
  "details": null
}
```

---

## 6. Service-Level Validation (Method Validation)

```java
@Service
@Validated   // enables method-level validation
public class WorkflowService {

    public Workflow findById(@NotNull @Positive Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow", id));
    }

    public Workflow create(@Valid WorkflowCreateRequest request) {
        // @Valid re-validates even if controller already validated
        // useful when service is called from non-HTTP context (Kafka, scheduler)
    }

    public void updateStatus(
            @NotNull Long id,
            @NotNull ExecutionStatus newStatus) {

        Workflow workflow = findById(id);
        if (!workflow.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                workflow.getStatus().name(), newStatus.name());
        }
        workflow.setStatus(newStatus);
        repository.save(workflow);
    }
}
```

---

## 7. Common Anti-Patterns

### Catching exceptions to return success

```java
// BAD
@PostMapping
public ResponseEntity<?> create(@RequestBody WorkflowCreateRequest request) {
    try {
        Workflow wf = workflowService.create(request);
        return ResponseEntity.ok(Map.of("success", true, "data", wf));
    } catch (Exception e) {
        return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        // Status 200 but failed! Client can't use HTTP status for error handling.
    }
}

// GOOD -- let exceptions propagate to @ControllerAdvice
@PostMapping
public ResponseEntity<WorkflowResponse> create(
        @Valid @RequestBody WorkflowCreateRequest request) {
    WorkflowResponse created = workflowService.create(request);
    return ResponseEntity.status(201).body(created);
    // Exceptions -> GlobalExceptionHandler -> proper 4xx/5xx status
}
```

### Leaking stack traces

```text
BAD:  "message": "NullPointerException at WorkflowService.java:42"
GOOD: "message": "An unexpected error occurred"  (log the real error server-side)
```

### Different error shapes per endpoint

```text
BAD:
  POST /users   -> { "error": "invalid" }
  POST /orders  -> { "message": "bad request", "code": 400 }
  GET  /products -> { "errors": ["name required"] }

GOOD:
  ALL endpoints -> same ApiError record shape (see Section 5)
```

---

## 8. Interview Questions

```text
Q: What is the difference between input validation and business validation?
A: Input validation checks data format (not blank, valid email, within range).
   Business validation checks domain rules (user has permission, state
   transition is valid, account has sufficient balance). Input validation
   is in the DTO/controller layer, business validation is in the service layer.

Q: How do you handle validation errors in Spring Boot?
A: @Valid on @RequestBody triggers Bean Validation. Failures throw
   MethodArgumentNotValidException. A @RestControllerAdvice catches it
   and returns a standardized 400 response with field-level error details.

Q: @Valid vs @Validated?
A: @Valid is Jakarta standard, works on parameters and nested fields.
   @Validated is Spring-specific, adds group support and method-level
   validation on service classes.

Q: How do you ensure consistent error responses across the API?
A: Use a single @RestControllerAdvice with handlers for all exception types.
   All handlers return the same ApiError record. Map each exception to the
   appropriate HTTP status code.

Q: What exception would you throw for a duplicate resource?
A: ConflictException (409). But also have a DB unique constraint as the
   last line of defense. Handle DataIntegrityViolationException in the
   global handler to return 409 if the constraint fires.
```
