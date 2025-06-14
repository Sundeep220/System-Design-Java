package CreationPatterns.Builder.prob2;

import java.util.List;

public class Main {
    public static void main(String[] args) {
//        Pizza pizza = new Pizza.PizzaBuilder("Large", "CheeseBurst")
//                .extraCheese(true)
//                .isTakeAway(true)
//                .sauce("Pesto")
//                .baseFlavor("Peri Peri")
//                .toppings(List.of("Jalapeno", "Olives", "Capsicum"))
//                .build();
//        Pizza pizza2 = new Pizza.PizzaBuilder("Medium", "Thin").build();
////        pizza.showPizza();
//        pizza2.showPizza();

        Pizza pizza1 = new Pizza.PizzaBuilder("Large", "CheeseBurst")
                .extraCheese(true)
                .isTakeAway(true)
                .sauce("Pesto")
                .baseFlavor("Peri Peri")
                .toppings(List.of("Jalapeno", "Olives", "Capsicum"))
                .build();

        Pizza pizza2 = new Pizza.PizzaBuilder("Medium", "Thin")
                .toppings(List.of("Onion", "Mushroom"))
                .build();

        PizzaOrderBuilder order = new PizzaOrderBuilder()
                .addPizza(pizza1)
                .addPizza(pizza2);

        order.showOrder();
    }
}
