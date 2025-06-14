package CreationPatterns.Builder.prob2;

import java.util.ArrayList;
import java.util.List;

public class PizzaOrderBuilder {
    private final List<Pizza> pizzas = new ArrayList<>();

    public PizzaOrderBuilder addPizza(Pizza pizza) {
        pizzas.add(pizza);
        return this;
    }

    public void showOrder() {
        System.out.println("🧾 Pizza Order Summary:");
        double total = 0;
        int count = 1;
        for (Pizza pizza : pizzas) {
            System.out.println("\n--- Pizza " + count + " ---");
            pizza.showPizza();
            total += pizza.getPrice();
            count++;
        }
        System.out.printf("\n💰 Total Order Price: ₹%.2f%n", total);
    }
}
