package CreationPatterns.MiniProject;

import CreationPatterns.MiniProject.PriceCalculator.PriceEngine;

public record VersionedVehicle(Vehicle vehicle, String version) {

    public void display() {
        System.out.println("Version: " + version);
        vehicle.drive();
        double price = PriceEngine.calculatePrice(vehicle);
        System.out.println("Price: $" + price);
    }
}
