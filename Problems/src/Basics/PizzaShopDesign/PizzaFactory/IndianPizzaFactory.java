package Basics.PizzaShopDesign.PizzaFactory;

import Basics.PizzaShopDesign.Pizzas.Farmhouse;
import Basics.PizzaShopDesign.Pizzas.Margherita;
import Basics.PizzaShopDesign.Pizzas.Pizza;

public class IndianPizzaFactory implements PizzaFactory {

    @Override
    public Pizza createMargherita() {
        return new Margherita(200);
    }

    @Override
    public Pizza createFarmhouse() {
        return new Farmhouse(250);
    }
}
