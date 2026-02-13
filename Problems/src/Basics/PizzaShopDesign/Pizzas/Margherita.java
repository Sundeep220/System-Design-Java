package Basics.PizzaShopDesign.Pizzas;

public class Margherita implements Pizza {

    private final double basePrice;

    public Margherita(double basePrice) {
        this.basePrice = basePrice;
    }

    @Override
    public String getDescription() {
        return "Margherita";
    }

    @Override
    public double getCost() {
        return basePrice;
    }
}
