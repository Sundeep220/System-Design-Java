# FlowForge

A **workflow automation engine** built from scratch to practice Java backend engineering concepts end-to-end.

FlowForge lets you define, manage, and (eventually) execute multi-step workflows — think CI/CD pipelines, approval chains, or data processing pipelines — all exposed through a clean REST API.

## Why This Project?

This isn't a toy CRUD app. It's a deliberately designed project that covers the full spectrum of backend concepts — from REST API design and JPA entities all the way to AOP, resilience patterns, and observability — using the latest stack available.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 (LTS) |
| Framework | Spring Boot 4.x (Spring Framework 7) |
| ORM | Hibernate 7.1 / JPA 3.2 |
| Database | PostgreSQL |
| Validation | Bean Validation 3.1 (Jakarta EE 11) |
| Serialization | Jackson 3 |
| Build | Maven |

## Planned Features

### Phase 1 — Core API (current)

- **Workflow Management** — Create, update, patch, delete workflows with full lifecycle (Draft → Active → Paused → Archived)
- **Workflow Steps** — Define ordered steps within a workflow, each with a type and configuration
- **Pagination, Filtering & Search** — Offset and cursor pagination, dynamic JPA Specification filters, multi-column sorting, text search
- **Validation** — Bean Validation 3.1 with custom validators and validation groups
- **Global Exception Handling** — Consistent error responses with `@RestControllerAdvice`
- **Request Filters & Interceptors** — Request timing, `X-Request-Id` header injection, in-memory rate limiting
- **AOP & Resilience** — Custom `@LogExecution` aspect, built-in `@Retryable` (Spring Framework 7), `@ConcurrencyLimit`
- **Configuration Management** — Profiles, `@ConfigurationProperties`, virtual threads, Jackson 3 customization

### Future Phases

- **Database** — Migrations (Flyway), transactions, optimistic locking, connection pooling
- **Caching** — Spring Cache abstraction, Redis integration
- **Security** — Spring Security 7, JWT authentication, RBAC
- **Async & Messaging** — Event-driven execution, message queues
- **Observability** — OpenTelemetry (metrics + traces), structured logging
- **Containerization & Deployment** — Docker, Kubernetes, CI/CD

## Java 25 & Spring Boot 4 Features Used

- **Flexible Constructor Bodies (JEP 513)** — Validate constructor args before `super()`
- **Module Import Declarations (JEP 511)** — Cleaner imports
- **Scoped Values (JEP 506)** — Request context without ThreadLocal
- **Jackson 3** — `tools.jackson` package, `JsonMapper` as default
- **Built-in API Versioning** — Declarative `spring.mvc.apiversion.*`
- **Built-in @Retryable** — No more `spring-retry` dependency
- **JSpecify Null Safety** — Portfolio-wide `@Nullable` / `@NonNull`
- **Virtual Threads** — `spring.threads.virtual.enabled=true`

## Getting Started

### Prerequisites

- JDK 25
- PostgreSQL 16+
- Maven 3.9+

### Setup

```bash
# Create the database
psql -U postgres -c "CREATE DATABASE flowforge;"
psql -U postgres -c "CREATE USER flowforge WITH PASSWORD 'flowforge';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE flowforge TO flowforge;"

# Run the app
cd flowforge
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Environment Variables (optional)

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `flowforge` | PostgreSQL username |
| `DB_PASSWORD` | `flowforge` | PostgreSQL password |

## Project Structure

```
flowforge/
├── src/main/java/com/flowforge/flowforge/
│   ├── entity/          # JPA entities and enums
│   ├── repository/      # Spring Data JPA repositories
│   ├── dto/             # Request/response records
│   ├── mapper/          # Entity ↔ DTO mapping
│   ├── service/         # Business logic
│   ├── controller/      # REST controllers
│   ├── exception/       # Custom exceptions + global handler
│   ├── filter/          # Servlet filters
│   ├── interceptor/     # Spring MVC interceptors
│   ├── aspect/          # AOP aspects
│   └── config/          # Configuration classes
└── src/main/resources/
    ├── application.yaml
    ├── application-dev.yaml
    └── application-prod.yaml
```
