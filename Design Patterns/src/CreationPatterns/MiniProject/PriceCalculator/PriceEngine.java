package CreationPatterns.MiniProject.PriceCalculator;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.vehicles.Bike;
import CreationPatterns.MiniProject.vehicles.Car;
import CreationPatterns.MiniProject.vehicles.Truck;

public class PriceEngine {
    public static double calculatePrice(Vehicle vehicle) {
        double price = 0;
        // Base price
        if (vehicle instanceof Car) price += 20000;
        else if (vehicle instanceof Bike) price += 5000;
        else if (vehicle instanceof Truck) price += 30000;

        // Additional price based on a vehicle engine type
        String engineType = vehicle.getEngine().getEngineType();
        if (engineType.equalsIgnoreCase("electric")) price += 8000;
        else if (engineType.equalsIgnoreCase("diesel")) price += 4000;
        else price += 2000;

        // Additional price based on interior material
        String material = vehicle.getInterior().getMaterial();
        if (material.equalsIgnoreCase("leather")) price += 3000;
        else if (material.equalsIgnoreCase("vegan leather")) price += 4000;

        // Additional price based on infotainment (or what we call Car's head unit)
        if (vehicle.getInterior().hasInfotainment()) price += 1500;

        return price;
    }
}
