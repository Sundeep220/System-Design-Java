package Basics.CoffeeOrderingSystem;

public class Main {

    public static void main(String[] args) {

        Coffee plainCoffee = new PlainCoffee();

        System.out.println(plainCoffee.getDescription());
        System.out.println("Cost: ₹" + plainCoffee.getCost());

        System.out.println();

        Coffee coffeeWithSugarAndMilk = new Milk(new Sugar(new PlainCoffee()));

        System.out.println(coffeeWithSugarAndMilk.getDescription());
        System.out.println("Cost: ₹" + coffeeWithSugarAndMilk.getCost());

        System.out.println();

        Coffee coffeeWithWhippedCreamAndMilk = new Caramel(
                new WhippedCream(
                        new Milk(
                                new PlainCoffee()
                        )
                ));

        System.out.println(coffeeWithWhippedCreamAndMilk.getDescription());
        System.out.println("Cost: ₹" + coffeeWithWhippedCreamAndMilk.getCost());

        System.out.println();

        Coffee coffeeWithSugarAndWhippedCreamAndMilk = new Sugar(
                new Caramel(
                        new Sugar(
                                new Milk(
                                        new PlainCoffee()
                                )
                        )
                ));

        System.out.println(coffeeWithSugarAndWhippedCreamAndMilk.getDescription());
        System.out.println("Cost: ₹" + coffeeWithSugarAndWhippedCreamAndMilk.getCost());
    }
}