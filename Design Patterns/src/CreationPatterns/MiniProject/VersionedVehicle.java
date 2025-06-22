package CreationPatterns.MiniProject;

import CreationPatterns.MiniProject.PriceCalculator.PriceEngine;

public class VersionedVehicle {
    private final Vehicle vehicle;
    private final String version;

    public VersionedVehicle(Vehicle vehicle, String version) {
        this.vehicle = vehicle;
        this.version = version;
    }

    public void display() {
        System.out.println("Version: " + version);
        vehicle.drive();
        double price = PriceEngine.calculatePrice(vehicle);
        System.out.println("Price: $" + price);
    }

    public Vehicle getVehicle() { return vehicle; }
    public String getVersion() { return version; }
}
