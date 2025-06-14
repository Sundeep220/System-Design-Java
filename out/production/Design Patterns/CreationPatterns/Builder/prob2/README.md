# 🍕 Builder Pattern Assignment 2: Pizza Builder System

## 🎯 Problem Statement

Design a **Pizza Builder System** for a food delivery app where users can build a customized pizza.

The pizza has both required and optional fields. The builder should support chaining methods to customize a pizza.

---

## 📄 Pizza Fields

### ✅ Required:
- `String size` (e.g., Small, Medium, Large)
- `String crust` (e.g., Thin, Thick, CheeseBurst)

### 🧂 Optional:
- `boolean extraCheese`
- `boolean isTakeAway`
- `List<String> toppings` (e.g., Jalapeno, Olives, Onion, Capsicum)
- `String sauce` (e.g., Tomato, Pesto, Alfredo)
- `String baseFlavor` (e.g., Peri Peri, BBQ)

---

## 🧱 Requirements

1. Use **Builder Pattern** with chained methods.
2. Use a private constructor for `Pizza` and make the builder class a static nested class.
3. Provide a `build()` method to construct the pizza.
4. Add a method `showPizza()` to display the final configuration.
5. Use reasonable default values where applicable (e.g., no toppings, tomato sauce, etc.).

---

## 🧪 Sample Usage

```java
Pizza pizza = new Pizza.PizzaBuilder("Large", "CheeseBurst")
                    .extraCheese(true)
                    .isTakeAway(true)
                    .sauce("Pesto")
                    .baseFlavor("Peri Peri")
                    .toppings(List.of("Jalapeno", "Olives", "Capsicum"))
                    .build();

pizza.showPizza();
```

---

## ✅ Expected Output

```
🍕 Pizza Details:
Size: Large
Crust: CheeseBurst
Extra Cheese: Yes
Take Away: Yes
Sauce: Pesto
Base Flavor: Peri Peri
Toppings: Jalapeno, Olives, Capsicum
```

---

## 🔄 Bonus Challenge

- Add pricing logic inside the builder: each ingredient adds cost.
- Add `calculatePrice()` method.
- Support `PizzaOrderBuilder` that builds multiple pizzas into one order.
