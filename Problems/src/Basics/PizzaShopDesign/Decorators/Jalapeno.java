package Basics.PizzaShopDesign.Decorators;

import Basics.PizzaShopDesign.Pizzas.Pizza;

public class Jalapeno extends PizzaDecorator {

    public Jalapeno(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + ", Jalapeno";
    }

    @Override
    public double getCost() {
        return pizza.getCost() + 30;
    }
}
