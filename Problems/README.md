# 🧠 The LLD Thinking Framework (How Experts Solve It)

For *every* LLD problem, follow this order:

### ✅ Step 1: Clarify Requirements

* Functional requirements (What should system do?)
* Non-functional (Ignore initially unless asked)
* Constraints (scale, concurrency, etc.)

### ✅ Step 2: Identify Core Entities (Nouns)

Extract objects from the problem statement.

Example:

> “User books a movie ticket”

Entities:

* User
* Movie
* Ticket
* Seat
* Booking

### ✅ Step 3: Define Responsibilities (SRP mindset)

For each class:

* What should it own?
* What should it NOT own?

### ✅ Step 4: Identify Relationships

* Association
* Aggregation
* Composition
* Inheritance (if needed)

### ✅ Step 5: Define Behaviors (Methods)

What actions can system perform?

### ✅ Step 6: Apply SOLID + Patterns (if required)

Don’t force patterns.
Use them only when they naturally fit.

### ✅ Step 7: Draw UML

Then write code.

---