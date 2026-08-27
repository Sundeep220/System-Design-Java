# Phase 15 -- Spring MVC Deep Dive

Understanding the full request lifecycle from the moment an HTTP request hits your server to the moment a JSON response leaves. This is what interviewers mean when they ask "what happens when you call a REST endpoint?"

---

## 1. The Complete Request Lifecycle

```text
Client (browser / Postman / mobile app)
    |
    |  HTTP Request: POST /api/v1/workflows  { "name": "Flow A" }
    |
    v
TOMCAT (Embedded Server)
    |
    |  1. Accepts TCP connection
    |  2. Parses HTTP request (method, headers, body)
    |  3. Creates HttpServletRequest + HttpServletResponse
    |
    v
FILTER CHAIN
    |
    |  CorsFilter              -> handle CORS preflight
    |  SecurityFilterChain     -> authenticate (JWT check)
    |  RequestLoggingFilter    -> log request (custom)
    |  CharacterEncodingFilter -> UTF-8 encoding
    |
    v
DISPATCHER SERVLET (Front Controller)
    |
    |  4. Receives ALL requests (single entry point)
    |
    v
HANDLER MAPPING
    |
    |  5. Maps URL + HTTP method to a Controller method
    |  POST /api/v1/workflows -> WorkflowController.create()
    |
    v
HANDLER INTERCEPTORS (preHandle)
    |
    |  6. Cross-cutting logic BEFORE controller
    |  RateLimitInterceptor    -> check rate limit
    |  AuditInterceptor        -> log who called what
    |  TenantInterceptor       -> set tenant context
    |
    v
ARGUMENT RESOLVERS
    |
    |  7. Convert raw HTTP data to Java method parameters
    |  @RequestBody  -> Jackson deserializes JSON to WorkflowCreateRequest
    |  @PathVariable -> extracts {id} from URL
    |  @RequestParam -> extracts query params
    |  Pageable      -> builds PageRequest from ?page=0&size=20&sort=...
    |  @Valid         -> runs Bean Validation, throws MethodArgumentNotValidException
    |
    v
CONTROLLER METHOD
    |
    |  8. Your business logic executes
    |  WorkflowController.create(request)
    |    -> WorkflowService.create(request)
    |      -> WorkflowRepository.save(entity)
    |
    v
RETURN VALUE HANDLERS
    |
    |  9. Convert return value to HTTP response
    |  ResponseEntity<WorkflowResponse>
    |    -> HttpMessageConverter (Jackson) serializes to JSON
    |    -> Sets Content-Type: application/json
    |    -> Sets status code (201 Created)
    |
    v
HANDLER INTERCEPTORS (postHandle)
    |
    |  10. After controller, before response committed
    |
    v
HANDLER INTERCEPTORS (afterCompletion)
    |
    |  11. After response sent (cleanup, metrics)
    |
    v
FILTER CHAIN (reverse order)
    |
    |  12. Filters run again on the way out
    |
    v
TOMCAT
    |
    |  13. Writes HTTP response to socket
    |
    v
Client receives: HTTP 201  { "id": 1, "name": "Flow A", "status": "DRAFT" }
```

---

## 2. DispatcherServlet -- The Front Controller

Every HTTP request goes through this one servlet:

```text
                    DispatcherServlet
                          |
          +---------------+---------------+
          |               |               |
    HandlerMapping  HandlerAdapter  ViewResolver
          |               |               |
    "which method?"  "call it"     "render response"
```

### What DispatcherServlet does

```text
1. doDispatch(request, response)
   |
   |-- getHandler(request)
   |     -> iterates HandlerMappings
   |     -> RequestMappingHandlerMapping matches @GetMapping, @PostMapping, etc.
   |     -> returns HandlerExecutionChain (handler + interceptors)
   |
   |-- getHandlerAdapter(handler)
   |     -> RequestMappingHandlerAdapter for annotated controllers
   |
   |-- applyPreHandle(interceptors)
   |     -> calls interceptor.preHandle() for each
   |     -> if any returns false, request is short-circuited
   |
   |-- handle(request, response, handler)
   |     -> resolves method arguments (ArgumentResolvers)
   |     -> invokes controller method
   |     -> processes return value (ReturnValueHandlers)
   |
   |-- applyPostHandle(interceptors)
   |
   |-- processDispatchResult()
   |     -> if exception -> calls @ExceptionHandler
   |     -> if view -> ViewResolver resolves and renders
   |     -> if @ResponseBody -> already written by MessageConverter
   |
   |-- afterCompletion(interceptors)
```

---

## 3. Filters vs Interceptors

```text
                  FILTERS                    INTERCEPTORS
Scope:            Servlet level              Spring MVC level
Interface:        javax.servlet.Filter       HandlerInterceptor
Access to:        Request/Response only      Handler method, ModelAndView
Runs for:         ALL requests               Only mapped handler requests
Order:            @Order or FilterRegistration   @Order or registry.addInterceptor()
Use cases:        Auth, CORS, Logging,       Rate limiting, Audit,
                  Encoding, Compression      Tenant context, Metrics
```

### Custom Filter

```java
@Component
@Order(1)   // lower number = earlier execution
public class RequestTimingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // Add to MDC for structured logging
        MDC.put("requestId", requestId);
        response.addHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);   // continue chain
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} {} {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
            MDC.clear();
        }
    }
}
```

### Custom Interceptor

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiter;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String clientIp = request.getRemoteAddr();
        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setStatus(429);
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return false;   // short-circuit -- controller never called
        }
        return true;   // continue to controller
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        // cleanup, metrics, etc.
    }
}

// Register interceptor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")           // apply to API paths
            .excludePathPatterns("/actuator/**");  // skip actuator
    }
}
```

---

## 4. Argument Resolvers

Spring converts raw HTTP data into Java method parameters:

### @RequestBody -- JSON body to Java object

```java
@PostMapping("/workflows")
public ResponseEntity<WorkflowResponse> create(
        @Valid @RequestBody WorkflowCreateRequest request) {
    // Jackson deserializes JSON -> WorkflowCreateRequest
    // @Valid triggers Bean Validation
}
```

```text
What happens:
1. Spring finds @RequestBody on parameter
2. Picks RequestResponseBodyMethodProcessor (argument resolver)
3. Reads request body as InputStream
4. Finds HttpMessageConverter for Content-Type: application/json
5. MappingJackson2HttpMessageConverter deserializes JSON
6. If @Valid present, runs validators
7. If validation fails, throws MethodArgumentNotValidException
8. If valid, passes object to your method
```

### @PathVariable -- URL path segment

```java
@GetMapping("/workflows/{workflowId}/executions/{executionId}")
public ExecutionResponse get(
        @PathVariable Long workflowId,
        @PathVariable Long executionId) {
    // Spring extracts from URL: /workflows/42/executions/99
    // workflowId = 42, executionId = 99
}
```

### @RequestParam -- Query parameters

```java
@GetMapping("/workflows")
public PageResponse<WorkflowResponse> list(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    // GET /workflows?status=ACTIVE&page=0&size=20
}
```

### @RequestHeader -- HTTP headers

```java
@PostMapping("/orders")
public OrderResponse create(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
        @RequestBody OrderCreateRequest request) {
}
```

### @ModelAttribute -- form data / query params to object

```java
// Binds ALL matching query params to DTO fields
@GetMapping("/executions")
public PageResponse<ExecutionResponse> search(
        @ModelAttribute ExecutionFilterRequest filter,
        Pageable pageable) {
    // GET /executions?status=FAILED&workflowId=42&startedAfter=2025-01-01
    // -> ExecutionFilterRequest(status=FAILED, workflowId=42, startedAfter=...)
}
```

### Pageable -- auto-resolved from query params

```java
@GetMapping("/workflows")
public Page<WorkflowResponse> list(
        @PageableDefault(size = 20, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
    // GET /workflows?page=0&size=20&sort=createdAt,desc
    // Spring auto-resolves Pageable from query params
}
```

### Custom Argument Resolver

```java
// Resolve current user from JWT token
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        HttpServletRequest request =
            (HttpServletRequest) webRequest.getNativeRequest();
        String token = request.getHeader("Authorization");
        return userService.fromToken(token);
    }
}

// Custom annotation
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}

// Usage
@PostMapping("/workflows")
public WorkflowResponse create(
        @CurrentUser User user,           // auto-resolved from JWT
        @Valid @RequestBody WorkflowCreateRequest request) {
    return workflowService.create(request, user);
}

// Register
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
```

---

## 5. HttpMessageConverters (Jackson Serialization)

Spring uses `HttpMessageConverter` to convert between HTTP body and Java objects.

### Default converters (auto-configured)

```text
MappingJackson2HttpMessageConverter  -- JSON (application/json)
StringHttpMessageConverter           -- plain text (text/plain)
ByteArrayHttpMessageConverter        -- binary (application/octet-stream)
FormHttpMessageConverter             -- form data (application/x-www-form-urlencoded)
```

### Jackson customization

```java
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .serializationInclusion(JsonInclude.Include.NON_NULL)  // skip null fields
            .featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,   // ISO-8601 dates
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES // ignore extra fields
            )
            .modules(new JavaTimeModule());  // support java.time types
    }
}
```

### Common Jackson annotations

```java
public record WorkflowResponse(
    Long id,

    @JsonProperty("workflow_name")    // rename in JSON output
    String name,

    @JsonIgnore                        // never include in JSON
    String internalNote,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    Instant createdAt,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)  // skip if empty list
    List<String> tags
) {}
```

### Serialization flow

```text
Controller returns ResponseEntity<WorkflowResponse>
    |
    v
ReturnValueHandler detects @ResponseBody (implicit in @RestController)
    |
    v
Content negotiation: client Accept header -> application/json
    |
    v
MappingJackson2HttpMessageConverter selected
    |
    v
ObjectMapper.writeValueAsBytes(workflowResponse)
    |
    v
Response body written + Content-Type: application/json header set
```

---

## 6. Content Negotiation

How Spring decides which format to return:

```text
Client sends:    Accept: application/json     -> Jackson converts to JSON
Client sends:    Accept: application/xml      -> (if JAXB on classpath) XML
Client sends:    Accept: text/plain           -> toString()
```

```java
// Produce multiple formats
@GetMapping(value = "/workflows/{id}",
    produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
public WorkflowResponse get(@PathVariable Long id) {
    return workflowService.findById(id);
}
```

In practice, most APIs only support JSON:

```java
@RestController
@RequestMapping(value = "/api/v1/workflows",
    produces = MediaType.APPLICATION_JSON_VALUE)  // always JSON
public class WorkflowController { }
```

---

## 7. @RestController vs @Controller

```java
// @Controller -- returns VIEW names (Thymeleaf, JSP)
@Controller
public class PageController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("workflows", workflowService.findAll());
        return "dashboard";    // -> resolves to templates/dashboard.html
    }
}

// @RestController = @Controller + @ResponseBody
// Returns data directly (JSON/XML), NOT view names
@RestController
public class WorkflowController {
    @GetMapping("/api/v1/workflows")
    public List<WorkflowResponse> list() {
        return workflowService.findAll();  // -> serialized to JSON
    }
}
```

```text
@Controller + @ResponseBody on every method = @RestController
For REST APIs, always use @RestController.
```

---

## 8. ResponseEntity -- Full Response Control

```java
// Simple return -- Spring sets 200 OK automatically
@GetMapping("/{id}")
public WorkflowResponse get(@PathVariable Long id) {
    return workflowService.findById(id);
}

// ResponseEntity -- control status, headers, body
@PostMapping
public ResponseEntity<WorkflowResponse> create(
        @Valid @RequestBody WorkflowCreateRequest request) {

    WorkflowResponse created = workflowService.create(request);

    URI location = URI.create("/api/v1/workflows/" + created.id());

    return ResponseEntity
        .created(location)                      // 201 + Location header
        .header("X-Workflow-Id", String.valueOf(created.id()))
        .body(created);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    workflowService.delete(id);
    return ResponseEntity.noContent().build();  // 204 No Content
}
```

---

## 9. WebMvcConfigurer -- Customize MVC

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS configuration
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }

    // Custom argument resolvers
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    // Interceptors
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**");
    }

    // Static resources
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/docs/**")
            .addResourceLocations("classpath:/static/docs/");
    }

    // Custom message converters
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // customize or add converters
    }
}
```

---

## 10. Error Handling in MVC

When things go wrong, Spring MVC handles it in this order:

```text
1. @ExceptionHandler in the SAME controller
2. @ExceptionHandler in @ControllerAdvice (global)
3. ResponseStatusException (Spring 5+)
4. DefaultHandlerExceptionResolver (Spring defaults)
5. BasicErrorController (/error page)
```

```java
// ResponseStatusException -- inline, no separate class needed
@GetMapping("/{id}")
public WorkflowResponse get(@PathVariable Long id) {
    return workflowRepository.findById(id)
        .map(mapper::toResponse)
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Workflow not found: " + id));
}

// Custom exception with @ResponseStatus
@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(Long id) {
        super("Workflow not found: " + id);
    }
}
```

---

## 11. Interview Questions

```text
Q: Explain the full request lifecycle in Spring MVC.
A: Client -> Tomcat -> Filter Chain -> DispatcherServlet -> HandlerMapping
   -> Interceptors (preHandle) -> ArgumentResolvers -> Controller
   -> ReturnValueHandlers -> MessageConverter (Jackson) -> Interceptors
   (postHandle/afterCompletion) -> Filter Chain -> Client

Q: What is the DispatcherServlet?
A: The front controller. ALL requests go through it. It delegates to
   HandlerMapping (find controller), HandlerAdapter (invoke method),
   and ViewResolver/MessageConverter (format response).

Q: Filter vs Interceptor?
A: Filter = servlet spec, wraps entire request, runs for all requests.
   Interceptor = Spring MVC, has access to handler info, runs only for
   mapped controllers. Use filters for auth/CORS, interceptors for
   business cross-cutting concerns.

Q: How does @RequestBody work?
A: Spring uses RequestResponseBodyMethodProcessor (ArgumentResolver).
   It reads the request body, uses Content-Type to pick an
   HttpMessageConverter (Jackson for JSON), deserializes to the target
   type, then runs @Valid if present.

Q: How does Spring resolve Pageable from query params?
A: PageableHandlerMethodArgumentResolver reads page, size, sort
   from query params and creates a PageRequest object. You can
   configure defaults with @PageableDefault.

Q: What happens when a controller method throws an exception?
A: DispatcherServlet catches it, looks for @ExceptionHandler in the
   controller, then in @ControllerAdvice classes. If found, the
   exception handler method produces the error response. If not
   found, Spring's DefaultHandlerExceptionResolver handles common
   exceptions (405, 400, etc.).
```
