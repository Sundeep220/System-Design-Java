# 📝 LLD Practice Question

## 🍕 Design a Pizza Ordering System

### 📌 Problem Statement

Design a Pizza Ordering System that supports multiple types of pizzas, customizable toppings, and order building.

The system should allow:

1. Different categories/families of pizzas (e.g., Indian style, Italian style).
2. Each category can produce multiple pizza types (e.g., Margherita, Farmhouse).
3. Customers can dynamically add multiple toppings (e.g., Extra Cheese, Jalapeño, Olives, Paneer, etc.).
4. Each topping:

    * Adds additional cost
    * Updates pizza description
5. An order can contain multiple pizzas.
6. The system should generate a final order summary with total cost.

---

## 🎯 Functional Requirements

1. Support multiple pizza families (e.g., Indian, Italian).
2. Support multiple pizza types within each family.
3. Allow unlimited topping combinations.
4. Toppings must be applied dynamically at runtime.
5. Order must:

    * Add multiple pizzas
    * Calculate total cost
    * Print summary

---

## 🚫 Constraints

* Avoid class explosion (e.g., `MargheritaWithCheeseAndOlives`).
* Follow SOLID principles.
* System should be easily extensible:

    * Add new pizza family
    * Add new pizza type
    * Add new topping
    * Modify pricing logic

---

## 🧠 Design Requirements

The solution must:

* Use appropriate Design Patterns.
* Be scalable and extensible.
* Use composition over inheritance where applicable.
* Keep responsibilities separated.

---

## 🔍 Expected Patterns to Consider

* Abstract Factory (for pizza families)
* Decorator (for toppings)
* Builder (for order creation)

---

## 💡 Follow-up Questions (Interview Extensions)

1. How would you restrict certain toppings to specific pizzas?
2. How would you support different pizza sizes affecting price?
3. How would you apply discount coupons?
4. How would you allow removal of toppings?
5. How would you persist orders in a database?

---

# 📚 Short Version (For Quick Revision)

> Design a Pizza Ordering System supporting multiple pizza families, dynamic toppings using composition, and order building with total cost calculation. The system must be extensible and avoid class explosion.

---

Excellent 👏
These follow-ups are what actually separate **mid-level** from **senior-level LLD thinking**.

Let’s answer them architecturally — not just conceptually.

---

# 💡 1️⃣ How would you restrict certain toppings to specific pizzas?

### 🚨 Problem

Example:

* Pineapple not allowed on Farmhouse
* Chicken only allowed on non-veg pizzas
* Paneer only allowed on Indian pizzas

---

## ❌ Bad Approach

Put `if` checks inside each topping decorator.

That tightly couples toppings with concrete pizzas.

---

## ✅ Clean Approach → Validation Strategy

### Introduce:

```
ToppingValidator (Strategy)
```

```
interface ToppingValidator {
    boolean isValid(Pizza pizza);
}
```

Each topping gets its own validator.

Example:

```
class ChickenValidator implements ToppingValidator {
    public boolean isValid(Pizza pizza) {
        return pizza instanceof NonVegPizza;
    }
}
```

Now decorator checks:

```
if (!validator.isValid(pizza)) {
    throw new IllegalArgumentException("Invalid topping");
}
```

---

### 🏆 Why This Is Good

* Topping logic separated
* No modification in Pizza classes
* Easy to extend
* Follows Open/Closed

---

# 💡 2️⃣ How would you support different pizza sizes affecting price?

### 🚨 Problem

Small / Medium / Large

Price affects:

* Base pizza
* Toppings (large cheese costs more)

---

## ✅ Best Design → Composition + Size Enum

Add:

```
enum Size {
    SMALL, MEDIUM, LARGE
}
```

Add size inside Pizza:

```
interface Pizza {
    Size getSize();
}
```

Now pricing can depend on size.

---

### Option A — Size-aware pricing in each class

```
@Override
public double getCost() {
    switch(size) {
        case SMALL: return 200;
        case MEDIUM: return 250;
        case LARGE: return 300;
    }
}
```

---

### Better Option (Cleaner) → Pricing Strategy

Introduce:

```
interface PricingStrategy {
    double calculatePrice(Pizza pizza);
}
```

This keeps pricing separate from pizza object.

---

### 🏆 Senior-Level Insight

Size is a property of Pizza → not a separate subclass.

Avoid:

```
SmallMargherita
LargeMargherita
```

That causes class explosion.

---

# 💡 3️⃣ How would you apply discount coupons?

### 🚨 Problem

* Flat discount
* Percentage discount
* Buy 1 Get 1
* Veg-only discount

---

## ✅ Best Pattern → Strategy Pattern

Create:

```
interface DiscountStrategy {
    double applyDiscount(Order order);
}
```

Implementations:

```
FlatDiscount
PercentageDiscount
BuyOneGetOne
VegOnlyDiscount
```

Order builder accepts strategy:

```
OrderBuilder applyDiscount(DiscountStrategy strategy)
```

---

### 🏆 Why Strategy?

* Coupons vary independently
* No if-else explosion
* Easy to add new discount types

This keeps Order clean.

---

# 💡 4️⃣ How would you allow removal of toppings?

🚨 This is tricky because Decorator wraps objects.

```
Jalapeno(
   Cheese(
      Margherita
   )
)
```

How do we remove Cheese?

---

## ❌ Problem with Pure Decorator

Decorators are nested.
Removing one in the middle is complex.

---

## ✅ Better Production Approach

Instead of pure wrapping structure,
maintain:

```
class CustomPizza {
    BasePizza basePizza;
    List<Topping> toppings;
}
```

Now:

* Add topping → add to list
* Remove topping → remove from list
* getCost() → calculate dynamically

This sacrifices strict Decorator but increases flexibility.

---

### 🏆 Interview Insight

Decorator is perfect for immutable stacking.
If removal is required → prefer composition + list.

---

# 💡 5️⃣ How would you persist orders in a database?

Now we move to system design territory.

---

## Add:

```
OrderRepository
```

Interface:

```
interface OrderRepository {
    void save(Order order);
    Order findById(String id);
}
```

Implementation:

```
JdbcOrderRepository
JpaOrderRepository
MongoOrderRepository
```

---

## Database Design (Simplified)

### Tables:

### Orders

| id | customer_id | total_cost | status |

### Order_Items

| id | order_id | pizza_type | size |

### Toppings

| id | order_item_id | topping_name | cost |

---

## Important Design Decision

We should persist:

* Base pizza type
* Size
* List of toppings
* Final price snapshot

Never recalculate price from current logic.
Prices may change in future.

---

# 🏆 Final Pattern Map (Advanced Version)

| Concern            | Pattern                 |
| ------------------ | ----------------------- |
| Pizza families     | Abstract Factory        |
| Toppings           | Decorator / Composition |
| Order creation     | Builder                 |
| Coupon logic       | Strategy                |
| Topping validation | Strategy                |
| Pricing logic      | Strategy                |
| Persistence        | Repository Pattern      |

---

# 🧠 This Is What Senior Engineers Do

They don’t just use patterns.
They combine:

* Strategy
* Factory
* Decorator
* Builder
* Repository

to solve evolving constraints.

---


