package CreationPatterns.MiniProject.factory;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;
import CreationPatterns.MiniProject.vehicles.Bike;
import CreationPatterns.MiniProject.vehicles.Car;
import CreationPatterns.MiniProject.vehicles.Truck;

/**
 * @Purpose: Factory class for creating different types of vehicles based on user input
 */
public class VehicleFactory {
    public static Vehicle createVehicle(String type) {
        return switch (type.toLowerCase()) {
            case "car" -> new Car("Sedan", "Black", new Engine("Petrol"), new Interior("Leather", true), new WheelConfig(4));
            case "bike" -> new Bike("SportBike", "Red", new Engine("Petrol"),new Interior("Fabric", false), new WheelConfig(2));
            case "truck" -> new Truck("CargoX", "Blue", new Engine("Diesel"), new Interior("Vinyl", false), new WheelConfig(6));
            default -> throw new IllegalArgumentException("Unknown vehicle type: " + type);
        };
//        Switch Statement
//        switch (type.toLowerCase()) {
//            case "car":
//                return new Car("Sedan", "Black",
//                        new Engine("Petrol"),
//                        new Interior("Leather", true),
//                        new WheelConfig(4)
//                );
//
//            case "bike":
//                return new Bike("SportBike", "Red",
//                        new Engine("Petrol"),
//                        new Interior("Fabric", false),
//                        new WheelConfig(2)
//                );
//
//            case "truck":
//                return new Truck("CargoX", "Blue",
//                        new Engine("Diesel"),
//                        new Interior("Vinyl", false),
//                        new WheelConfig(6)
//                );
//
//            default:
//                throw new IllegalArgumentException("Unknown vehicle type: " + type);
//        }
    }
}
