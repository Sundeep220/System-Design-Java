package CreationPatterns.Builder.prob2;

import java.util.ArrayList;
import java.util.List;

public class Pizza {
    //Required Fields
    private final String size;
    private final String crust;
    //Optional Fields
    private final boolean extraCheese;
    private final boolean isTakeAway;
    private final List<String> toppings;
    private final String sauce;
    private final String baseFlavor;

    private final double price;

    private Pizza(PizzaBuilder builder) {
        this.size = builder.size;
        this.crust = builder.crust;
        this.extraCheese = builder.extraCheese;
        this.isTakeAway = builder.isTakeAway;
        this.toppings = builder.toppings != null ? builder.toppings : new ArrayList<>();
        this.sauce = builder.sauce != null ? builder.sauce : "Tomato";
        this.baseFlavor = builder.baseFlavor != null ? builder.baseFlavor : "Classic";
        this.price = builder.calculatePrice();
    }

    public void showPizza() {
        System.out.println("🍕 Pizza Details:");
        System.out.println("Size: " + size);
        System.out.println("Crust: " + crust);
        System.out.println("Extra Cheese: " + (extraCheese ? "Yes" : "No"));
        System.out.println("Take Away: " + (isTakeAway ? "Yes" : "No"));
        System.out.println("Sauce: " + sauce);
        System.out.println("Base Flavor: " + baseFlavor);
        if (!toppings.isEmpty()) {
            System.out.println("Toppings: " + String.join(", ", toppings));
        } else {
            System.out.println("Toppings: None");
        }
    }

    public double getPrice() {
        return price;
    }

    public static class PizzaBuilder {
        private final String size;
        private final String crust;
        private boolean extraCheese = false;
        private boolean isTakeAway = false;
        private List<String> toppings;
        private String sauce;
        private String baseFlavor;

        public PizzaBuilder(String size, String crust) {
            this.size = size;
            this.crust = crust;
        }

        public PizzaBuilder extraCheese(boolean extraCheese) {
            this.extraCheese = extraCheese;
            return this;
        }

        public PizzaBuilder isTakeAway(boolean isTakeAway) {
            this.isTakeAway = isTakeAway;
            return this;
        }

        public PizzaBuilder toppings(List<String> toppings) {
            this.toppings = toppings;
            return this;
        }

        public PizzaBuilder sauce(String sauce) {
            this.sauce = sauce;
            return this;
        }

        public PizzaBuilder baseFlavor(String baseFlavor) {
            this.baseFlavor = baseFlavor;
            return this;
        }

        private double calculatePrice() {
            double price = 0;

            // Base price based on size
            switch (size.toLowerCase()) {
                case "small" -> price += 150;
                case "medium" -> price += 200;
                case "large" -> price += 250;
                default -> price += 200; // fallback
            }

            // Crust adds to price
            switch (crust.toLowerCase()) {
                case "thin" -> price += 0;
                case "thick" -> price += 20;
                case "cheeseburst" -> price += 50;
            }

            if (extraCheese) price += 40;
            if (isTakeAway) price += 10; // box charge

            if (toppings != null) price += toppings.size() * 15;
            if (sauce != null && !sauce.equalsIgnoreCase("tomato")) price += 10;
            if (baseFlavor != null && !baseFlavor.equalsIgnoreCase("classic")) price += 15;

            return price;
        }

        public Pizza build() {
            return new Pizza(this);
        }
    }

}
