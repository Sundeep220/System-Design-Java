package CreationPatterns.MiniProject.builder;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;
import CreationPatterns.MiniProject.vehicles.Bike;
import CreationPatterns.MiniProject.vehicles.Car;
import CreationPatterns.MiniProject.vehicles.Truck;

/**
 * @Purpose: Builder class for creating different types of vehicles based on user input
 */
public class VehicleBuilder {
    private final String type;     // "car", "bike", "truck"
    private String model;
    private String color;
    private String engineType;
    private String interiorMaterial;
    private boolean infotainment;
    private int wheelCount;

    public VehicleBuilder(String type) {
        this.type = type.toLowerCase();
    }

    public VehicleBuilder setModel(String model) {
        this.model = model;
        return this;
    }

    public VehicleBuilder setColor(String color) {
        this.color = color;
        return this;
    }

    public VehicleBuilder setEngine(String engineType) {
        this.engineType = engineType;
        return this;
    }

    public VehicleBuilder setInterior(String material, boolean infotainment) {
        this.interiorMaterial = material;
        this.infotainment = infotainment;
        return this;
    }

    public VehicleBuilder setWheels(int count) {
        this.wheelCount = count;
        return this;
    }

    public Vehicle build() {
        Engine engine = new Engine(engineType);
        Interior interior = new Interior(interiorMaterial, infotainment);
        WheelConfig wheels = new WheelConfig(wheelCount);

        return switch (type) {
            case "car" -> new Car(model, color, engine, interior, wheels);
            case "bike" -> new Bike(model, color, engine, interior, wheels);
            case "truck" -> new Truck(model, color, engine, interior, wheels);
            default -> throw new IllegalArgumentException("Unknown vehicle type: " + type);
        };
    }
}
