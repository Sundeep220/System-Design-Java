# Auditing & Audit Tables

What auditing is, why you need audit tables, and how to implement
them in Spring Boot (from simple to advanced).

---

## 1. What Is Auditing

```text
Auditing = tracking WHO changed WHAT and WHEN in your database.

  WITHOUT auditing:
    Someone changed a user's email from "john@gmail.com" to "hacker@evil.com"
    → You have NO idea who did it, when, or what the old value was.

  WITH auditing:
    audit_log says:
      WHO:   admin_user_42
      WHAT:  users.email changed from "john@gmail.com" to "hacker@evil.com"
      WHEN:  2026-08-15 14:32:01 UTC
      HOW:   UPDATE via /api/users/7

  WHY YOU NEED IT:
    - Compliance (GDPR, SOX, HIPAA, PCI-DSS require audit trails)
    - Debugging (what changed and caused the bug?)
    - Security (detect unauthorized changes)
    - Business (undo mistakes, resolve disputes)
    - Legal (prove what happened and when)
```

---

## 2. Types of Auditing

```text
1. BASIC FIELD AUDITING (created/modified metadata)
   Track createdBy, createdDate, lastModifiedBy, lastModifiedDate on every entity.
   → Answers: "Who created this? When was it last changed?"
   → Spring Boot: @CreatedBy, @CreatedDate, @LastModifiedBy, @LastModifiedDate

2. AUDIT LOG TABLE (event-based)
   Separate table that logs every change as a new row.
   → Answers: "What was the full history of changes to this record?"
   → Spring Boot: Custom implementation or Hibernate Envers

3. CHANGE DATA CAPTURE (CDC)
   Database-level tracking of every INSERT/UPDATE/DELETE.
   → Answers: "Replay every state change in order"
   → Tools: Debezium, PostgreSQL logical replication, database triggers

────────────────────────────────────────────────────────────────

WHICH TO USE:

  Basic fields      → Every project (minimal overhead, always useful)
  Audit log table   → When you need history (who changed what, old vs new value)
  CDC               → Event-driven systems, data pipelines, cross-service sync
```

---

## 3. Basic Field Auditing with Spring Data JPA

### Step 1: Enable JPA Auditing

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    // This activates @CreatedDate, @LastModifiedDate, etc.
}
```

### Step 2: Create a Base Entity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // getters, setters
}
```

### Step 3: Provide the Current User (AuditorAware)

```java
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of("SYSTEM")); // fallback for scheduled jobs
    }
}
```

```text
HOW IT WORKS:
  1. @EnableJpaAuditing tells Spring to look for AuditingEntityListener
  2. AuditingEntityListener listens to JPA lifecycle events (@PrePersist, @PreUpdate)
  3. On INSERT → fills createdAt, createdBy, updatedAt, updatedBy
  4. On UPDATE → fills updatedAt, updatedBy (createdAt/createdBy unchanged)
  5. AuditorAware provides the "who" — pulls from SecurityContext

  Spring calls AuditorAware.getCurrentAuditor() automatically
  before every save. No manual code needed.
```

### Step 4: Extend in Your Entities

```java
@Entity
@Table(name = "orders")
public class Order extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;
    private BigDecimal amount;

    // All audit fields (createdAt, updatedAt, createdBy, updatedBy)
    // are inherited automatically.
}
```

### Resulting Table

```sql
CREATE TABLE orders (
    id          BIGSERIAL PRIMARY KEY,
    product     VARCHAR(255),
    amount      NUMERIC(19,2),
    created_at  TIMESTAMP NOT NULL,       -- auto-filled
    updated_at  TIMESTAMP,                -- auto-filled
    created_by  VARCHAR(255),             -- auto-filled
    updated_by  VARCHAR(255)              -- auto-filled
);
```

---

## 4. Audit Log Table (Full History)

```text
Basic fields only tell you the LATEST change.
An audit log table records EVERY change — full history with old and new values.

  orders table:                          audit_log table:
  ┌────┬─────────┬────────┐              ┌────────────┬────────┬──────────────────┐
  │ id │ product │ amount │              │ action     │ entity │ changes          │
  ├────┼─────────┼────────┤              ├────────────┼────────┼──────────────────┤
  │ 1  │ Laptop  │ 1200   │              │ INSERT     │ Order  │ {product:Laptop} │
  └────┴─────────┴────────┘              │ UPDATE     │ Order  │ {amount:999→1200}│
                                         │ UPDATE     │ Order  │ {product:Mac→..} │
                                         └────────────┴────────┴──────────────────┘
```

### Approach 1: Custom Audit Log with JPA Entity Listeners

#### Audit Log Entity

```java
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityName;     // "Order", "User", etc.

    @Column(nullable = false)
    private String entityId;       // ID of the changed record

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;    // INSERT, UPDATE, DELETE

    @Column(columnDefinition = "TEXT")
    private String oldValue;       // JSON of old state

    @Column(columnDefinition = "TEXT")
    private String newValue;       // JSON of new state

    @Column(columnDefinition = "TEXT")
    private String changedFields;  // which fields changed

    @Column(nullable = false)
    private String performedBy;    // who made the change

    @Column(nullable = false)
    private Instant performedAt;   // when

    private String ipAddress;      // optional: from where

    // getters, setters
}

public enum AuditAction {
    INSERT, UPDATE, DELETE
}
```

#### Audit Log Repository

```java
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByPerformedAtDesc(
            String entityName, String entityId);

    List<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy);

    List<AuditLog> findByPerformedAtBetween(Instant from, Instant to);
}
```

#### Generic Audit Listener

```java
@Component
public class AuditListener {

    private static AuditLogRepository auditLogRepository;
    private static AuditorAware<String> auditorAware;
    private static ObjectMapper objectMapper;

    @Autowired
    public void init(AuditLogRepository repo,
                     AuditorAware<String> auditor,
                     ObjectMapper mapper) {
        AuditListener.auditLogRepository = repo;
        AuditListener.auditorAware = auditor;
        AuditListener.objectMapper = mapper;
    }

    @PostPersist
    public void onInsert(Object entity) {
        log(entity, AuditAction.INSERT, null, entity);
    }

    @PreUpdate
    public void onUpdate(Object entity) {
        // Note: getting old value requires extra work (see Envers below)
        log(entity, AuditAction.UPDATE, null, entity);
    }

    @PreRemove
    public void onDelete(Object entity) {
        log(entity, AuditAction.DELETE, entity, null);
    }

    private void log(Object entity, AuditAction action,
                     Object oldVal, Object newVal) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityName(entity.getClass().getSimpleName());
            auditLog.setEntityId(getEntityId(entity));
            auditLog.setAction(action);
            auditLog.setOldValue(oldVal != null ? objectMapper.writeValueAsString(oldVal) : null);
            auditLog.setNewValue(newVal != null ? objectMapper.writeValueAsString(newVal) : null);
            auditLog.setPerformedBy(auditorAware.getCurrentAuditor().orElse("UNKNOWN"));
            auditLog.setPerformedAt(Instant.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Log error but don't fail the main transaction
            log.error("Failed to write audit log", e);
        }
    }

    private String getEntityId(Object entity) {
        // Use reflection or a common interface to extract ID
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object id = field.get(entity);
            return id != null ? id.toString() : "NEW";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
```

#### Attach Listener to Entities

```java
@Entity
@Table(name = "orders")
@EntityListeners({AuditingEntityListener.class, AuditListener.class})
public class Order extends BaseAuditEntity {
    // ...
}
```

```text
TRADE-OFFS OF CUSTOM APPROACH:
  ✅ Full control over audit log format
  ✅ Can add custom fields (IP address, request ID, etc.)
  ✅ Simple to query and understand
  ❌ Getting OLD values on UPDATE is hard (need to query before save)
  ❌ Runs in same transaction (if audit fails, main TX could fail)
  ❌ Manual work for each entity

  GETTING OLD VALUES:
  Use @PreUpdate + EntityManager to load the original:
    Object original = em.find(entity.getClass(), entityId);
  Or use Hibernate Envers (next section) which handles this automatically.
```

---

## 5. Hibernate Envers (Recommended for Full Audit)

```text
Hibernate Envers = automatic, full history tracking for JPA entities.
It creates a REVISION table for EACH audited entity.

  orders → orders_AUD (audit copy with revision info)

  Every INSERT, UPDATE, DELETE creates a new row in orders_AUD
  with the full entity state at that point in time.

  You can query: "What did this order look like on March 15th?"
```

### Step 1: Add Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-envers</artifactId>
</dependency>
<!-- Spring Boot auto-configures it — no version needed -->
```

### Step 2: Annotate Entities

```java
@Entity
@Table(name = "orders")
@Audited                          // ← This is all you need!
public class Order extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;
    private BigDecimal amount;

    @NotAudited                   // ← Skip fields you don't want to track
    private String internalNotes;
}
```

### Auto-Generated Tables

```sql
-- Envers creates these automatically:

-- Revision metadata (one row per transaction)
CREATE TABLE revinfo (
    rev         INTEGER PRIMARY KEY,   -- revision number
    revtstmp    BIGINT                 -- timestamp of the revision
);

-- Audit table (one row per entity change)
CREATE TABLE orders_aud (
    id          BIGINT,
    rev         INTEGER REFERENCES revinfo(rev),
    revtype     SMALLINT,              -- 0=INSERT, 1=UPDATE, 2=DELETE
    product     VARCHAR(255),
    amount      NUMERIC(19,2),
    PRIMARY KEY (id, rev)
);
```

```text
REVTYPE values:
  0 = ADD (INSERT)
  1 = MOD (UPDATE)
  2 = DEL (DELETE)

For every transaction that modifies an @Audited entity:
  1. A new row is added to revinfo (revision number + timestamp)
  2. A snapshot of the entity is saved to orders_aud with that revision
```

### Step 3: Custom Revision Entity (Add Who Made the Change)

```java
@Entity
@Table(name = "revinfo")
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "username")
    private String username;

    @Column(name = "ip_address")
    private String ipAddress;

    // getters, setters
}
```

```java
public class CustomRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        // Get current user from Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        rev.setUsername(auth != null ? auth.getName() : "SYSTEM");

        // Get IP address from request context
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            rev.setIpAddress(sra.getRequest().getRemoteAddr());
        }
    }
}
```

### Step 4: Querying Audit History

```java
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final EntityManager entityManager;

    // Get all revisions of an order
    public List<Order> getOrderHistory(Long orderId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        List<Number> revisions = reader.getRevisions(Order.class, orderId);

        return revisions.stream()
                .map(rev -> reader.find(Order.class, orderId, rev))
                .toList();
    }

    // Get entity at a specific revision
    public Order getOrderAtRevision(Long orderId, int revision) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.find(Order.class, orderId, revision);
    }

    // Get entity at a specific date
    public Order getOrderAtDate(Long orderId, Instant date) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        Number rev = reader.getRevisionNumberForDate(Date.from(date));
        return reader.find(Order.class, orderId, rev);
    }

    // Query audit data with conditions
    public List<Order> getDeletedOrders() {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        return reader.createQuery()
                .forRevisionsOfEntity(Order.class, true, true)
                .add(AuditEntity.revisionType().eq(RevisionType.DEL))
                .getResultList();
    }

    // Get full change history with metadata
    public List<AuditRevisionDTO> getFullHistory(Long orderId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        List<Object[]> results = reader.createQuery()
                .forRevisionsOfEntity(Order.class, false, true)
                .add(AuditEntity.id().eq(orderId))
                .getResultList();

        return results.stream().map(row -> {
            Order entity = (Order) row[0];
            CustomRevisionEntity rev = (CustomRevisionEntity) row[1];
            RevisionType type = (RevisionType) row[2];

            return new AuditRevisionDTO(
                    entity,
                    rev.getId(),
                    rev.getUsername(),
                    Instant.ofEpochMilli(rev.getTimestamp()),
                    type.name()
            );
        }).toList();
    }
}
```

### Expose via REST API

```java
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/orders/{orderId}/history")
    public List<AuditRevisionDTO> getOrderHistory(@PathVariable Long orderId) {
        return auditQueryService.getFullHistory(orderId);
    }

    @GetMapping("/orders/{orderId}/revision/{rev}")
    public Order getOrderAtRevision(@PathVariable Long orderId,
                                     @PathVariable int rev) {
        return auditQueryService.getOrderAtRevision(orderId, rev);
    }
}
```

---

## 6. Envers vs Custom Audit Log

```text
FEATURE                  HIBERNATE ENVERS         CUSTOM AUDIT TABLE
──────────────────────────────────────────────────────────────────────
Setup effort             Very low (@Audited)      High (write listeners)
Old/new value tracking   Automatic                Manual (need pre-load)
Query historical state   Built-in AuditReader     Custom queries
Custom fields            Via RevisionEntity       Full control
Performance              Moderate (extra writes)  Configurable
Async support            No (same TX)             Can be async
Cross-service auditing   No                       Yes (via events/Kafka)
Schema control           Auto-generated _AUD      Full control

RECOMMENDATION:
  Single Spring Boot app → Hibernate Envers (fast setup, powerful queries)
  Microservices          → Custom audit events via Kafka/RabbitMQ
  Compliance-heavy       → Envers + immutable audit DB (append-only)
```

---

## 7. Database-Level Auditing (Triggers)

```text
For maximum security, audit at the DATABASE level.
Application-level auditing can be bypassed by direct SQL queries.
```

```sql
-- PostgreSQL: audit trigger for the orders table

CREATE TABLE orders_audit (
    audit_id     BIGSERIAL PRIMARY KEY,
    action       VARCHAR(10) NOT NULL,     -- INSERT, UPDATE, DELETE
    table_name   VARCHAR(50) NOT NULL,
    record_id    BIGINT NOT NULL,
    old_data     JSONB,
    new_data     JSONB,
    changed_by   VARCHAR(100) DEFAULT current_user,
    changed_at   TIMESTAMP DEFAULT now()
);

CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO orders_audit (action, table_name, record_id, new_data)
        VALUES ('INSERT', TG_TABLE_NAME, NEW.id, row_to_json(NEW)::jsonb);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO orders_audit (action, table_name, record_id, old_data, new_data)
        VALUES ('UPDATE', TG_TABLE_NAME, OLD.id,
                row_to_json(OLD)::jsonb, row_to_json(NEW)::jsonb);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO orders_audit (action, table_name, record_id, old_data)
        VALUES ('DELETE', TG_TABLE_NAME, OLD.id, row_to_json(OLD)::jsonb);
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_func();
```

```text
PROS:
  ✅ Cannot be bypassed (even direct SQL is audited)
  ✅ Works regardless of application layer
  ✅ Very fast (same DB, no network call)

CONS:
  ❌ Database-specific (PostgreSQL triggers ≠ MySQL triggers)
  ❌ Harder to maintain and test
  ❌ No application context (user info needs SET LOCAL or app_name)
  ❌ Can slow down writes on high-traffic tables
```

---

## 8. Audit Table Design Best Practices

```text
1. MAKE AUDIT TABLES APPEND-ONLY
   Never UPDATE or DELETE from audit tables.
   Use separate schema or permissions: GRANT INSERT ONLY.

2. USE JSONB FOR OLD/NEW VALUES
   Don't create columns for every field — use JSONB.
   Schema changes in main table won't break audit table.

3. INDEX WISELY
   Index: entity_name + entity_id (lookup history of one record)
   Index: performed_at (time-range queries)
   Index: performed_by (who did what)
   Don't over-index — audits are write-heavy.

4. PARTITION BY TIME
   Audit tables grow FAST. Partition by month/quarter.
   Archive old partitions to cold storage.

   CREATE TABLE audit_log (
       ...
       performed_at TIMESTAMP NOT NULL
   ) PARTITION BY RANGE (performed_at);

   CREATE TABLE audit_log_2026_q1 PARTITION OF audit_log
       FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');

5. SEPARATE DATABASE FOR COMPLIANCE
   For SOX/HIPAA: store audit logs in a separate, immutable database.
   App database admins should NOT have access to delete audit records.

6. RETENTION POLICY
   Define how long to keep audit data (7 years for financial, etc.)
   Automate archival and deletion.

7. ASYNC AUDITING FOR HIGH-THROUGHPUT SYSTEMS
   If audit writes slow down main operations:
     Main TX → publish event to Kafka/RabbitMQ → audit consumer writes log
   Trade-off: eventual consistency (small delay before audit appears)
```

---

## 9. Complete Example: Spring Boot Auditing Setup

```text
Combining basic field auditing + Hibernate Envers:

  project-structure/
  ├── config/
  │   ├── JpaAuditConfig.java           ← @EnableJpaAuditing
  │   └── SpringSecurityAuditorAware.java ← AuditorAware impl
  ├── entity/
  │   ├── BaseAuditEntity.java          ← @MappedSuperclass with audit fields
  │   ├── Order.java                    ← @Audited entity
  │   └── CustomRevisionEntity.java     ← Custom revinfo with username
  ├── listener/
  │   └── CustomRevisionListener.java   ← Fills username in revision
  ├── service/
  │   └── AuditQueryService.java        ← Query historical data
  └── controller/
      └── AuditController.java          ← REST API for audit history
```

```yaml
# application.yml
spring:
  jpa:
    properties:
      org.hibernate.envers:
        audit_table_suffix: _AUD
        revision_field_name: REV
        revision_type_field_name: REVTYPE
        store_data_at_delete: true       # save entity state on DELETE
        default_schema: audit            # separate schema for audit tables
```

---

## 10. Interview Questions

```text
Q: What is database auditing?
A: Tracking who changed what data, when, and what the old/new values were.
   Used for compliance, debugging, security, and business dispute resolution.

Q: How does Spring Data JPA auditing work?
A: @EnableJpaAuditing activates AuditingEntityListener which listens to
   JPA lifecycle events (@PrePersist, @PreUpdate). It auto-fills
   @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy.
   AuditorAware<String> provides the current user.

Q: What is Hibernate Envers?
A: A Hibernate module that automatically creates audit (_AUD) tables.
   Annotate entity with @Audited and Envers snapshots every change
   with a revision number. Supports querying entity state at any point in time.

Q: Envers vs custom audit table — when to use which?
A: Envers: single app, fast setup, built-in historical queries.
   Custom: microservices, async auditing via Kafka, full control over schema.

Q: Why audit at the database level (triggers)?
A: Application-level auditing can be bypassed by direct SQL.
   DB triggers audit ALL changes regardless of source.
   Trade-off: DB-specific, harder to maintain, no app context.

Q: How do you handle audit table growth?
A: Partition by time (monthly/quarterly), archive old partitions,
   define retention policy, use JSONB instead of wide columns,
   consider async writes for high-throughput systems.

Q: Should audit tables allow UPDATE/DELETE?
A: No. Audit tables must be append-only for integrity.
   Use DB-level permissions: GRANT INSERT ONLY on audit tables.
```
