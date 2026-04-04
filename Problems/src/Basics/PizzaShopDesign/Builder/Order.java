package Basics.PizzaShopDesign.Builder;

import Basics.PizzaShopDesign.Pizzas.Pizza;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {

    private final List<Pizza> pizzas;
    private final double totalCost;

    private Order(Builder builder) {
        // Defensive copy to maintain immutability
        this.pizzas = Collections.unmodifiableList(
                new ArrayList<>(builder.pizzas)
        );
        this.totalCost = builder.totalCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public List<Pizza> getPizzas() {
        return pizzas;
    }

    public void printSummary() {
        System.out.println("----- ORDER SUMMARY -----");

        for (Pizza pizza : pizzas) {
            System.out.println(pizza.getClass().getSimpleName() + " - " + pizza.getDescription() + " = ₹" + pizza.getCost());
        }

        System.out.println("--------------------------");
        System.out.println("TOTAL = ₹" + totalCost);
    }

    // =========================
    // Static Inner Builder
    // =========================
    public static class Builder {

        private List<Pizza> pizzas = new ArrayList<>();
        private double totalCost;

        public Builder addPizza(Pizza pizza) {
            pizzas.add(pizza);
            return this;
        }

        public Order build() {
            totalCost = pizzas.stream()
                    .mapToDouble(Pizza::getCost)
                    .sum();

            return new Order(this);
        }
    }
}


