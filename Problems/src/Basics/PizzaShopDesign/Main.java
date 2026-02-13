package Basics.PizzaShopDesign;

import Basics.PizzaShopDesign.Builder.Order;
import Basics.PizzaShopDesign.Decorators.ExtraCheese;
import Basics.PizzaShopDesign.Decorators.Jalapeno;
import Basics.PizzaShopDesign.PizzaFactory.IndianPizzaFactory;
import Basics.PizzaShopDesign.PizzaFactory.PizzaFactory;
import Basics.PizzaShopDesign.Pizzas.Pizza;

public class Main {

    public static void main(String[] args) {

        PizzaFactory factory = new IndianPizzaFactory();

        Pizza pizza1 = factory.createMargherita();
        pizza1 = new ExtraCheese(pizza1);
        pizza1 = new Jalapeno(pizza1);

        Pizza pizza2 = factory.createFarmhouse();
        pizza2 = new ExtraCheese(pizza2);

        Order order = new Order.Builder()
                .addPizza(pizza1)
                .addPizza(pizza2)
                .build();

        order.printSummary();
    }
}
