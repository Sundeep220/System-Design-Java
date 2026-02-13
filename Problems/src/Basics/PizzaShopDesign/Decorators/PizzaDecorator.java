package Basics.PizzaShopDesign.Decorators;

import Basics.PizzaShopDesign.Pizzas.Pizza;

public abstract class PizzaDecorator implements Pizza {

    protected Pizza pizza;

    public PizzaDecorator(Pizza pizza) {
        this.pizza = pizza;
    }
}
