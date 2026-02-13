package Basics.PizzaShopDesign.Pizzas;

public class Farmhouse implements Pizza {

    private final double basePrice;

    public Farmhouse(double basePrice) {
        this.basePrice = basePrice;
    }

    @Override
    public String getDescription() {
        return "Farmhouse";
    }

    @Override
    public double getCost() {
        return basePrice;
    }
}

