# API Design — Interview Walkthrough

A step-by-step thinking framework for designing REST APIs in
interviews, demonstrated with a real problem: **Order Management API
for an E-Commerce Platform**.

---

## Part A: The Framework — How to Think About API Design

```text
When an interviewer says "Design an API for X", follow this sequence:

  STEP 1 — CLARIFY REQUIREMENTS
    Ask questions. Don't jump into endpoints.
    Who are the consumers? (mobile, web, internal services)
    What are the core operations? (CRUD? workflows? both?)
    Scale expectations? (100 req/s or 100K req/s?)
    Consistency needs? (eventual OK? strong consistency?)

  STEP 2 — IDENTIFY RESOURCES (nouns, not verbs)
    REST is resource-oriented. Find the core entities.
    /orders, /products, /users — NOT /createOrder, /getUser

  STEP 3 — DEFINE ENDPOINTS (HTTP methods)
    Map CRUD to HTTP methods.
    Design non-CRUD operations carefully.

  STEP 4 — REQUEST/RESPONSE DESIGN
    What goes in the body? What are query params?
    Pagination, filtering, sorting.
    Envelope vs flat response.

  STEP 5 — ERROR HANDLING
    Consistent error format. Proper HTTP status codes.

  STEP 6 — CROSS-CUTTING CONCERNS
    Authentication, rate limiting, versioning,
    idempotency, caching, pagination.

  STEP 7 — WALK THROUGH A USER FLOW
    Pick a real scenario. Trace the API calls end-to-end.
    This proves your design WORKS.
```

```mermaid
flowchart LR
    C["1. Clarify<br/>Requirements"] --> R["2. Identify<br/>Resources"]
    R --> E["3. Define<br/>Endpoints"]
    E --> RR["4. Request/<br/>Response"]
    RR --> ERR["5. Error<br/>Handling"]
    ERR --> CC["6. Cross-cutting<br/>Concerns"]
    CC --> W["7. Walk Through<br/>a Flow"]
```

---

## Part B: The Problem — Order Management API

```text
INTERVIEWER:
  "Design a REST API for an e-commerce order management system.
  Customers can browse products, add items to a cart, place orders,
  track order status, and cancel orders. Admins can update order
  status and view all orders."
```

---

## Step 1: Clarify Requirements

```text
QUESTIONS I'D ASK THE INTERVIEWER:

  Q: Who consumes this API?
  A: Mobile app, web frontend, and internal admin dashboard.

  Q: Do we need real-time updates (e.g., order status)?
  A: Not for now. Polling is fine.

  Q: Payment processing — in scope?
  A: No. Assume payment is handled separately. We just record
     the payment status.

  Q: Multi-currency, multi-language?
  A: No. Single currency (USD), English only.

  Q: Expected scale?
  A: ~1000 orders/day, 50 req/s peak. Moderate scale.

  Q: Authentication?
  A: JWT-based. Customers and admins have different roles.

NOW I KNOW:
  - Standard CRUD + workflow (order lifecycle)
  - Two consumer types: customer (limited access) and admin (full access)
  - Moderate scale, no real-time needed
  - Payment is out of scope
```

---

## Step 2: Identify Resources

```text
Core resources (nouns):

  PRODUCT     — what can be bought
  CART        — temporary collection of items before ordering
  CART ITEM   — an item in a cart (product + quantity)
  ORDER       — a placed order
  ORDER ITEM  — a line item in an order (product + quantity + price)

Relationships:
  Cart has many CartItems
  Order has many OrderItems
  CartItem references a Product
  OrderItem snapshots product info at time of order (price can change)

Sub-resources:
  /carts/{id}/items        — items belong to a cart
  /orders/{id}/items       — items belong to an order
```

```mermaid
erDiagram
    PRODUCT {
        string id PK
        string name
        string description
        decimal price
        int stockQuantity
        string category
    }

    CART {
        string id PK
        string customerId FK
        datetime createdAt
        datetime updatedAt
    }

    CART_ITEM {
        string id PK
        string cartId FK
        string productId FK
        int quantity
    }

    ORDER {
        string id PK
        string customerId FK
        string status
        decimal totalAmount
        string shippingAddress
        datetime createdAt
    }

    ORDER_ITEM {
        string id PK
        string orderId FK
        string productId
        string productName
        decimal unitPrice
        int quantity
    }

    CART ||--o{ CART_ITEM : contains
    CART_ITEM }o--|| PRODUCT : references
    ORDER ||--o{ ORDER_ITEM : contains
```

---

## Step 3: Define Endpoints

### Products (read-only for customers)

```text
GET    /api/v1/products                  List products (paginated, filterable)
GET    /api/v1/products/{productId}      Get product details
POST   /api/v1/products                  [ADMIN] Create product
PUT    /api/v1/products/{productId}      [ADMIN] Update product
DELETE /api/v1/products/{productId}      [ADMIN] Delete product
```

### Cart

```text
GET    /api/v1/cart                       Get current customer's cart
POST   /api/v1/cart/items                 Add item to cart
PUT    /api/v1/cart/items/{itemId}        Update item quantity
DELETE /api/v1/cart/items/{itemId}        Remove item from cart
DELETE /api/v1/cart                       Clear entire cart
```

```text
DESIGN DECISION: /cart instead of /carts/{cartId}

  Why? Each customer has exactly ONE active cart.
  The cart is implicitly scoped to the authenticated user.
  GET /cart = "get MY cart" (user from JWT token).
  No need to expose cartId in the URL.

  This is the "current user's resource" pattern.
  Similar: GET /me instead of GET /users/{userId}.
```

### Orders

```text
POST   /api/v1/orders                    Place an order (from cart)
GET    /api/v1/orders                    List my orders (paginated)
GET    /api/v1/orders/{orderId}          Get order details
POST   /api/v1/orders/{orderId}/cancel   Cancel an order

[ADMIN]
GET    /api/v1/admin/orders              List all orders (paginated, filterable)
PATCH  /api/v1/admin/orders/{orderId}    Update order status
```

```text
DESIGN DECISIONS:

  1. POST /orders (not POST /orders/create):
     The HTTP method already means "create". No verb in URL.

  2. POST /orders/{id}/cancel (not DELETE /orders/{id}):
     Cancellation is a WORKFLOW ACTION, not a resource deletion.
     The order still exists (with status CANCELLED).
     POST on a sub-resource = action on the resource.
     Alternative: PATCH /orders/{id} with { "status": "CANCELLED" }

  3. /admin/orders separate from /orders:
     Admin endpoints have different authorization, filtering, and
     response shape. Separating them is cleaner than overloading
     /orders with role-based behavior.

  4. PATCH for status update (not PUT):
     PATCH = partial update (only status field).
     PUT = full replacement (would need entire order body).
     Admin only changes status, not the full order.
```

```mermaid
flowchart TD
    subgraph "Customer Flow"
        A["Browse products<br/>GET /products"] --> B["Add to cart<br/>POST /cart/items"]
        B --> C["View cart<br/>GET /cart"]
        C --> D["Place order<br/>POST /orders"]
        D --> E["Track order<br/>GET /orders/{id}"]
        E --> F["Cancel order<br/>POST /orders/{id}/cancel"]
    end

    subgraph "Admin Flow"
        G["View all orders<br/>GET /admin/orders"] --> H["Update status<br/>PATCH /admin/orders/{id}"]
    end
```

---

## Step 4: Request and Response Design

### POST /api/v1/orders — Place Order

**Request:**

```json
POST /api/v1/orders
Authorization: Bearer <jwt-token>
Idempotency-Key: ord-req-uuid-12345

{
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "IN"
  },
  "paymentMethod": "CREDIT_CARD",
  "notes": "Please leave at door"
}
```

```text
DESIGN DECISIONS:

  1. NO items in the request body:
     Items come from the cart. The cart IS the order draft.
     POST /orders means "convert my current cart into an order".
     Simpler. No mismatch between cart and order.

  2. Idempotency-Key header:
     If the client retries (network timeout), the server checks:
     "Did I already process this idempotency key?"
     Yes → return the same response. No duplicate order.

  3. shippingAddress in the body (not from user profile):
     Customer may want to ship to a different address.
     Explicit is better than implicit.
```

**Response (201 Created):**

```json
HTTP/1.1 201 Created
Location: /api/v1/orders/ord-789

{
  "id": "ord-789",
  "status": "CREATED",
  "items": [
    {
      "id": "item-1",
      "productId": "prod-101",
      "productName": "Wireless Mouse",
      "unitPrice": 29.99,
      "quantity": 2,
      "subtotal": 59.98
    },
    {
      "id": "item-2",
      "productId": "prod-205",
      "productName": "USB-C Hub",
      "unitPrice": 45.00,
      "quantity": 1,
      "subtotal": 45.00
    }
  ],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "IN"
  },
  "totalAmount": 104.98,
  "createdAt": "2024-07-15T10:30:00Z",
  "estimatedDelivery": "2024-07-20",
  "_links": {
    "self": "/api/v1/orders/ord-789",
    "cancel": "/api/v1/orders/ord-789/cancel",
    "track": "/api/v1/orders/ord-789"
  }
}
```

```text
DESIGN DECISIONS:

  1. 201 Created (not 200 OK):
     A new resource was created. 201 is semantically correct.
     Include Location header pointing to the new resource.

  2. Order items SNAPSHOT product data:
     productName and unitPrice are COPIED at order time.
     If product price changes later, the order is unaffected.
     This is a critical business rule.

  3. _links (HATEOAS):
     Tells the client what actions are available.
     Client doesn't hardcode URLs — follows links.
     Optional but impressive in interviews.

  4. ISO 8601 timestamps:
     "2024-07-15T10:30:00Z" — always UTC with Z suffix.
     No ambiguity. No timezone confusion.
```

### GET /api/v1/orders — List Orders (Paginated)

**Request:**

```text
GET /api/v1/orders?page=0&size=20&sort=createdAt,desc&status=SHIPPED
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "ord-789",
      "status": "SHIPPED",
      "totalAmount": 104.98,
      "itemCount": 2,
      "createdAt": "2024-07-15T10:30:00Z"
    },
    {
      "id": "ord-456",
      "status": "SHIPPED",
      "totalAmount": 29.99,
      "itemCount": 1,
      "createdAt": "2024-07-10T14:20:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 47,
    "totalPages": 3
  }
}
```

```text
DESIGN DECISIONS:

  1. PAGE-BASED PAGINATION:
     page=0&size=20 — consistent with Spring Data conventions.
     Include totalElements and totalPages for UI pagination.

     Alternative: CURSOR-BASED pagination.
       ?cursor=eyJpZCI6Im9yZC00NTYifQ==&limit=20
       Better for real-time data (no skipping/duplicates on insert).
       Use for: feeds, timelines, infinite scroll.
       Use page-based for: admin dashboards, search results.

  2. LIST RESPONSE IS A SUMMARY (not full details):
     List returns: id, status, totalAmount, itemCount, createdAt.
     GET /orders/{id} returns FULL details (items, address, etc.).
     Less data transferred. Faster. Client fetches details on demand.

  3. SORTING:
     sort=createdAt,desc — Spring Data format.
     Newest first by default.

  4. FILTERING:
     status=SHIPPED — filter by order status.
     Can add: createdAfter, createdBefore, minAmount, maxAmount.
```

### GET /api/v1/products — Filterable Product List

```text
GET /api/v1/products?category=electronics&minPrice=10&maxPrice=100
    &search=wireless&sort=price,asc&page=0&size=20

  Filters as QUERY PARAMETERS (not in the body).
  GET requests should have no body (some proxies strip it).

  Filter parameters:
    category    — exact match
    search      — full-text search (name, description)
    minPrice    — range filter (inclusive)
    maxPrice    — range filter (inclusive)
    inStock     — boolean (true = stockQuantity > 0)
    sort        — field,direction
    page, size  — pagination
```

### PATCH /api/v1/admin/orders/{orderId} — Update Status

**Request:**

```json
PATCH /api/v1/admin/orders/ord-789
Authorization: Bearer <admin-jwt>

{
  "status": "SHIPPED",
  "trackingNumber": "FX-123456789"
}
```

**Response (200 OK):**

```json
{
  "id": "ord-789",
  "status": "SHIPPED",
  "previousStatus": "PROCESSING",
  "trackingNumber": "FX-123456789",
  "updatedAt": "2024-07-17T08:00:00Z",
  "updatedBy": "admin-user-1"
}
```

```text
DESIGN DECISION — Order Status Machine:

  Valid transitions:
    CREATED → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    CREATED → CANCELLED
    CONFIRMED → CANCELLED
    PROCESSING → CANCELLED (with conditions)

  Invalid transitions should return 422 Unprocessable Entity:
    SHIPPED → CREATED   ❌ (can't go backwards)
    DELIVERED → CANCELLED ❌ (too late to cancel)
    CANCELLED → CONFIRMED ❌ (cancelled is terminal)
```

```mermaid
stateDiagram-v2
    [*] --> CREATED : POST /orders
    CREATED --> CONFIRMED : Admin confirms
    CREATED --> CANCELLED : Customer cancels

    CONFIRMED --> PROCESSING : Admin starts processing
    CONFIRMED --> CANCELLED : Customer cancels

    PROCESSING --> SHIPPED : Admin ships
    PROCESSING --> CANCELLED : Admin cancels (with refund)

    SHIPPED --> DELIVERED : Delivery confirmed

    DELIVERED --> [*]
    CANCELLED --> [*]
```

---

## Step 5: Error Handling

### Consistent Error Response Format

```json
{
  "error": {
    "code": "ORDER_NOT_CANCELLABLE",
    "message": "Order cannot be cancelled because it has already been shipped",
    "status": 422,
    "timestamp": "2024-07-17T08:00:00Z",
    "path": "/api/v1/orders/ord-789/cancel",
    "details": [
      {
        "field": "status",
        "message": "Current status SHIPPED does not allow cancellation"
      }
    ]
  }
}
```

### HTTP Status Code Usage

```text
  ┌──────┬─────────────────────────────────────────────────────────────┐
  │ Code │ When to Use                                                 │
  ├──────┼─────────────────────────────────────────────────────────────┤
  │ 200  │ Successful GET, PUT, PATCH, DELETE                          │
  │ 201  │ Successful POST that creates a resource                     │
  │ 204  │ Successful DELETE with no response body                     │
  │ 400  │ Malformed request (bad JSON, missing required field)        │
  │ 401  │ Not authenticated (no token, expired token)                 │
  │ 403  │ Authenticated but not authorized (customer ≠ admin)         │
  │ 404  │ Resource not found                                          │
  │ 409  │ Conflict (duplicate order, concurrent update)               │
  │ 422  │ Valid request but business rule violated                    │
  │      │ (cancel shipped order, add out-of-stock item)               │
  │ 429  │ Rate limit exceeded                                         │
  │ 500  │ Unexpected server error (bug, crash)                        │
  │ 503  │ Service temporarily unavailable (maintenance, overload)     │
  └──────┴─────────────────────────────────────────────────────────────┘

  KEY DISTINCTION:
    400 = your REQUEST is malformed (syntax error)
    422 = your request is well-formed but BUSINESS LOGIC rejects it
```

### Validation Errors (400)

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "status": 400,
    "details": [
      { "field": "shippingAddress.zipCode", "message": "must not be blank" },
      { "field": "shippingAddress.country", "message": "must be a valid ISO country code" }
    ]
  }
}
```

---

## Step 6: Cross-Cutting Concerns

### Authentication & Authorization

```text
  AUTH: JWT Bearer token in Authorization header.
    Authorization: Bearer eyJhbGciOiJSUzI1NiIs...

  ROLES:
    CUSTOMER — can manage own cart, place/view/cancel own orders
    ADMIN    — can view all orders, update status, manage products

  RESOURCE OWNERSHIP:
    GET /orders → only returns the authenticated customer's orders.
    The server filters by customerId from the JWT token.
    Customer cannot see another customer's orders.

  ADMIN ENDPOINTS:
    /admin/* routes require ADMIN role.
    Return 403 if customer tries to access.
```

### Idempotency

```text
  POST (create) operations should support idempotency:

  POST /orders
  Idempotency-Key: uuid-12345

  Server logic:
    1. Check if idempotency key exists in store (Redis, DB)
    2. If yes → return stored response (no duplicate order created)
    3. If no → process request → store response with key → return

  WHY: Client sends POST /orders, server creates order, response
  lost (network timeout). Client retries. Without idempotency key
  → second order created. With key → same order returned.

  Idempotency key TTL: 24 hours (enough for retries, not forever).
```

### Rate Limiting

```text
  Protect the API from abuse:

  Response headers:
    X-RateLimit-Limit: 100          (max requests per window)
    X-RateLimit-Remaining: 73       (requests left)
    X-RateLimit-Reset: 1721209200   (epoch when window resets)

  When exceeded:
    HTTP 429 Too Many Requests
    Retry-After: 30                 (try again in 30 seconds)

  Different limits per role:
    Customer: 100 req/min
    Admin:    500 req/min
    Internal: 5000 req/min
```

### Versioning

```text
  Three common strategies:

  1. URL PATH (recommended for simplicity):
     /api/v1/orders
     /api/v2/orders
     Easy to understand. Easy to route. Easy to document.

  2. HEADER:
     Accept: application/vnd.myapp.v1+json
     Cleaner URLs. Harder to test (can't paste in browser).

  3. QUERY PARAMETER:
     /api/orders?version=1
     Easy to use. Mixes concern into query params.

  RECOMMENDATION: URL path (/api/v1/) for most cases.
  It's the most widely used and easiest to implement.
```

### Caching

```text
  Products change rarely → cache aggressively:
    GET /products/{id}
    Cache-Control: public, max-age=300       (5 minutes)
    ETag: "a1b2c3d4"

  Orders change → don't cache (or short cache):
    GET /orders/{id}
    Cache-Control: private, no-cache

  Conditional requests (bandwidth saving):
    Client sends: If-None-Match: "a1b2c3d4"
    Server: data unchanged → 304 Not Modified (no body)
    Server: data changed → 200 OK with new ETag
```

---

## Step 7: Walk Through a Complete Flow

### Scenario: Customer Places an Order

```mermaid
sequenceDiagram
    participant C as Customer (Mobile App)
    participant GW as API Gateway
    participant API as Order Service
    participant DB as Database
    participant Q as Message Queue

    C->>GW: GET /api/v1/products?category=electronics
    GW->>API: Forward (auth validated)
    API->>DB: SELECT products
    API-->>C: 200 OK (product list)

    C->>GW: POST /api/v1/cart/items<br/>{productId: "prod-101", quantity: 2}
    GW->>API: Forward
    API->>DB: INSERT cart_item
    API-->>C: 201 Created

    C->>GW: POST /api/v1/cart/items<br/>{productId: "prod-205", quantity: 1}
    GW->>API: Forward
    API->>DB: INSERT cart_item
    API-->>C: 201 Created

    C->>GW: GET /api/v1/cart
    GW->>API: Forward
    API->>DB: SELECT cart + items
    API-->>C: 200 OK (cart with 2 items, total: $104.98)

    C->>GW: POST /api/v1/orders<br/>Idempotency-Key: uuid-123<br/>{shippingAddress: {...}}
    GW->>API: Forward
    API->>DB: Check idempotency key
    API->>DB: Validate stock availability
    API->>DB: BEGIN TX: create order + order_items, clear cart, decrement stock
    API->>Q: Publish OrderCreated event
    API-->>C: 201 Created (order details)

    Note over C: 5 minutes later...
    C->>GW: GET /api/v1/orders/ord-789
    API-->>C: 200 OK (status: CONFIRMED)
```

```text
WHAT HAPPENS INSIDE POST /orders:

  1. VALIDATE idempotency key (prevent duplicate orders)
  2. FETCH cart for authenticated user
  3. VALIDATE cart is not empty
  4. FOR EACH cart item:
     a. Fetch current product price (may have changed since add-to-cart)
     b. Check stock availability
     c. If out of stock → 422 with details
  5. CALCULATE total amount
  6. BEGIN DATABASE TRANSACTION:
     a. Create order (status = CREATED)
     b. Create order items (snapshot price, name)
     c. Decrement product stock
     d. Clear the cart
     e. Store idempotency key → response mapping
  7. COMMIT TRANSACTION
  8. PUBLISH OrderCreated event (async, via outbox or direct)
  9. RETURN 201 with order details

  All in ONE transaction. Either everything succeeds or nothing.
  Cart clearing is part of the transaction — if order fails,
  cart is not cleared.
```

---

## Step 8: Spring Boot Implementation (Key Parts)

### Controller

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal user) {

        OrderResponse order = orderService.placeOrder(
            user.getId(), request, idempotencyKey);

        return ResponseEntity
            .created(URI.create("/api/v1/orders/" + order.getId()))
            .body(order);
    }

    @GetMapping
    public Page<OrderSummaryResponse> listOrders(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        return orderService.getOrders(user.getId(), status, pageable);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal user) {
        return orderService.getOrder(orderId, user.getId());
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal user) {
        return orderService.cancelOrder(orderId, user.getId());
    }
}
```

### DTOs

```java
public record PlaceOrderRequest(
    @NotNull @Valid ShippingAddress shippingAddress,
    @NotBlank String paymentMethod,
    String notes
) {}

public record ShippingAddress(
    @NotBlank String street,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank String zipCode,
    @NotBlank @Size(min = 2, max = 2) String country
) {}

public record OrderResponse(
    String id,
    OrderStatus status,
    List<OrderItemResponse> items,
    ShippingAddress shippingAddress,
    BigDecimal totalAmount,
    Instant createdAt,
    String estimatedDelivery,
    Map<String, String> links
) {}

public record OrderSummaryResponse(
    String id,
    OrderStatus status,
    BigDecimal totalAmount,
    int itemCount,
    Instant createdAt
) {}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            OrderNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(ErrorResponse.of(
            "ORDER_NOT_FOUND", ex.getMessage(), 404, request.getRequestURI()));
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<ErrorResponse> handleNotCancellable(
            OrderNotCancellableException ex, HttpServletRequest request) {
        return ResponseEntity.status(422).body(ErrorResponse.of(
            "ORDER_NOT_CANCELLABLE", ex.getMessage(), 422, request.getRequestURI()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(
            InsufficientStockException ex, HttpServletRequest request) {
        return ResponseEntity.status(422).body(ErrorResponse.of(
            "INSUFFICIENT_STOCK", ex.getMessage(), 422, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> details = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(f -> new FieldError(f.getField(), f.getDefaultMessage()))
            .toList();
        return ResponseEntity.status(400).body(ErrorResponse.of(
            "VALIDATION_ERROR", "Request validation failed",
            400, request.getRequestURI(), details));
    }
}
```

---

## Checklist — API Design Interview Quick Reference

```text
Use this checklist to make sure you've covered everything:

  ☐ RESOURCES identified (nouns, not verbs)
  ☐ HTTP methods mapped correctly (GET/POST/PUT/PATCH/DELETE)
  ☐ URL structure is consistent and RESTful
  ☐ Plural nouns for collections (/orders, not /order)
  ☐ Nested resources where appropriate (/orders/{id}/items)
  ☐ Request body designed (what fields, validation)
  ☐ Response body designed (what to include, what to omit)
  ☐ Pagination for list endpoints (page/size or cursor)
  ☐ Filtering via query parameters
  ☐ Sorting support
  ☐ HTTP status codes used correctly (201, 204, 400, 404, 422)
  ☐ Error response format consistent
  ☐ Validation errors return field-level details
  ☐ Authentication specified (JWT, API key, OAuth)
  ☐ Authorization model (roles, resource ownership)
  ☐ Idempotency for POST/PUT operations
  ☐ Rate limiting strategy
  ☐ API versioning approach
  ☐ Caching strategy (ETag, Cache-Control)
  ☐ Timestamps in ISO 8601 UTC
  ☐ IDs are UUIDs or opaque strings (not sequential integers)
  ☐ Walked through at least one end-to-end flow
```

---

## Common API Design Mistakes to Avoid

```text
  ❌ VERBS IN URLs:
     /createOrder, /getUser, /deleteProduct
     ✅ POST /orders, GET /users/{id}, DELETE /products/{id}

  ❌ INCONSISTENT NAMING:
     /orders, /ProductList, /user-accounts
     ✅ /orders, /products, /user-accounts (pick one convention)

  ❌ RETURNING DIFFERENT SHAPES FOR SAME RESOURCE:
     GET /orders returns { order: {...} }
     GET /orders/{id} returns { data: {...} }
     ✅ Same wrapper structure everywhere

  ❌ EXPOSING INTERNAL IDs:
     Sequential IDs (1, 2, 3) expose data volume and are guessable.
     ✅ Use UUIDs or opaque strings (ord-a1b2c3d4)

  ❌ IGNORING PARTIAL FAILURES:
     POST /orders succeeds but email fails → 500?
     ✅ Order created (201). Email is async. Don't fail the order.

  ❌ NOT VERSIONING:
     Breaking change deployed → all clients break simultaneously.
     ✅ /api/v1/ from day one. Gives migration path.

  ❌ 200 FOR EVERYTHING:
     { "status": 200, "data": null, "error": "Not found" }
     ✅ Use proper HTTP status codes. 404 means not found.

  ❌ RETURNING ENTIRE OBJECTS IN LISTS:
     GET /orders returns full order with all items, addresses, etc.
     ✅ Return summary in list. Full details on GET /orders/{id}.

  ❌ NO PAGINATION:
     GET /orders returns ALL 50,000 orders.
     ✅ Always paginate collections. Default page size (20).
```

---

## Interview Tips

```text
  1. START BY ASKING QUESTIONS:
     Don't jump into endpoints. Clarify scope, consumers, scale.
     This shows you think before coding.

  2. DRAW THE RESOURCE MODEL:
     Whiteboard the entities and relationships first.
     Endpoints flow naturally from the resource model.

  3. THINK ABOUT THE CLIENT:
     "If I were building the mobile app, what API calls would I need?"
     Design outside-in, not database-out.

  4. HANDLE EDGE CASES:
     "What happens if stock runs out between add-to-cart and checkout?"
     "What if two users try to buy the last item?"
     Discussing edge cases shows production thinking.

  5. MENTION IDEMPOTENCY:
     Most candidates forget this. Mentioning it stands out.
     "POST /orders with an Idempotency-Key header prevents
     duplicate orders on retry."

  6. MENTION PAGINATION EARLY:
     Shows you think about scale.
     "All list endpoints return paginated responses."

  7. WALK THROUGH A FLOW:
     Don't just list endpoints. Show them working together.
     "Customer browses → adds to cart → places order → tracks it."

  8. DISCUSS TRADEOFFS:
     "I chose URL versioning because it's simpler. Header versioning
     is cleaner but harder to test."
     Interviewers love tradeoff discussions.
```
