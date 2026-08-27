# Validation Groups in Spring Boot — A Complete Guide

## What Are Validation Groups?

Validation groups are **tags** (empty Java interfaces) that you attach to validation annotations.
They let you control **which validations run** depending on the operation.

Think of them like light switches in a room — you can turn on "CREATE lights" or "UPDATE lights"
independently, even though the switches are on the same wall.

---

## The Problem: Same Field, Different Rules

Consider a `Workflow` with a `name` field. Depending on the API operation, you want different behavior:

| Operation | `name` field rule |
|-----------|-------------------|
| **POST** (Create) | Required — must not be blank |
| **PUT** (Full Update) | Required — must not be blank |
| **PATCH** (Partial Update) | Optional — only validate if provided |

Without groups, `@NotBlank` runs **every time**, making PATCH impossible
(it would reject any request that doesn't include `name`).

---

## How Groups Work — Step by Step

### Step 1: Create Marker Interfaces

These are just empty interfaces. They act as **labels**.

```java
// "This rule applies when CREATING"
public interface OnCreate {
}

// "This rule applies when UPDATING"
public interface OnUpdate {
}
```

That's it. No methods, no logic. Just a name for Spring to reference.

---

### Step 2: Tag Your Annotations

Attach the group to any validation annotation using the `groups` parameter:

```java
public record WorkflowCreateRequest(

        // Runs on CREATE and UPDATE, but NOT on PATCH
        @NotBlank(groups = {OnCreate.class, OnUpdate.class},
                  message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        // No groups = belongs to "Default" group
        @Min(value = 0)
        int maxRetries
) {}
```

**Key rule:** An annotation with **no `groups`** belongs to the `Default` group.

---

### Step 3: Activate a Group in the Controller

Use `@Validated(GroupName.class)` instead of `@Valid`:

```java
@PostMapping
public ResponseEntity<?> create(
        @Validated(OnCreate.class) @RequestBody WorkflowCreateRequest request) {
    // Only annotations tagged with OnCreate.class will run
}

@PutMapping("/{id}")
public ResponseEntity<?> update(
        @PathVariable UUID id,
        @Validated(OnUpdate.class) @RequestBody WorkflowUpdateRequest request) {
    // Only annotations tagged with OnUpdate.class will run
}
```

---

## The Default Group Trap

This is the most common mistake with validation groups.

When you write:
```java
@NotBlank(groups = OnCreate.class)   // tagged with OnCreate
@Size(max = 100)                     // no tag = Default group
String name;
```

And your controller says:
```java
@Validated(OnCreate.class) @RequestBody ...
```

**What runs?**
- ✅ `@NotBlank` — tagged with `OnCreate`
- ❌ `@Size` — tagged with `Default`, but we only asked for `OnCreate`

**The `@Size` check is silently skipped!** You could submit a 10,000 character name.

### The Fix: Extend Default

Make your group interface extend `jakarta.validation.groups.Default`:

```java
public interface OnCreate extends Default {
}
```

Now `@Validated(OnCreate.class)` runs **both**:
- All `OnCreate`-tagged rules
- All `Default`-tagged rules (annotations with no group)

---

## Real-World Example: PATCH Endpoint

This is the most common use case for validation groups.

### The DTO

```java
public record WorkflowPatchRequest(

        // On PATCH, name is optional. So NO @NotBlank here.
        // But if provided, it must still follow size + pattern rules.
        @Size(max = 100, message = "Name must be at most 100 characters")
        @ValidWorkflowName
        Optional<String> name,

        @Size(max = 500)
        Optional<String> description,

        @Min(0) @Max(10)
        Optional<Integer> maxRetries
) {}
```

### The Controller

```java
@PatchMapping("/{id}")
public ResponseEntity<?> patch(
        @PathVariable UUID id,
        @Valid @RequestBody WorkflowPatchRequest request) {
    // @Valid = Default group
    // @NotBlank is NOT on this DTO, so blank name won't be rejected
    // But @Size and @ValidWorkflowName still run if name is provided
}
```

### Contrast with CREATE

```java
public record WorkflowCreateRequest(

        @NotBlank(message = "Name is required")   // ← This IS here
        @Size(max = 100)
        @ValidWorkflowName
        String name
) {}
```

```java
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody WorkflowCreateRequest request) {
    // @NotBlank runs — name is required
}
```

**Same field, different rules, different DTOs.** Groups let you share rules
across DTOs when they overlap.

---

## When to Use Groups vs Separate DTOs

| Approach | When to use |
|----------|-------------|
| **Separate DTOs** (what we do) | Different fields per operation (CREATE has no `id`, PATCH uses `Optional<>`) |
| **Validation Groups** | Same DTO shared across operations, but rules differ per operation |
| **Both** | Complex APIs where some DTOs are shared but rules vary |

In practice, most Spring Boot apps use **separate DTOs** for each operation
and only reach for groups when they need conditional validation within a single DTO.

---

## Summary Cheat Sheet

```
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

---

## In Our FlowForge Project

We created `OnCreate` and `OnUpdate` groups but currently use `@Valid` (Default group)
in the controller. The groups will be wired in Step 7 when we add the PATCH endpoint,
where `name` needs to be optional — the first real case where groups make a difference.
