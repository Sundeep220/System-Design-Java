package CreationPatterns.MiniProject.familyFactory;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;
import CreationPatterns.MiniProject.vehicles.Car;

public class CarFactory implements VehicleFamilyFactory {
    @Override
    public Vehicle createElectric() {
        return new Car("ElectricCar", "White",
                new Engine("Electric"),
                new Interior("Vegan Leather", true),
                new WheelConfig(4));
    }

    @Override
    public Vehicle createPetrol() {
        return new Car("PetrolCar", "Red",
                new Engine("Petrol"),
                new Interior("Leather", true),
                new WheelConfig(4));
    }

    @Override
    public Vehicle createDiesel() {
        return new Car("DieselCar", "Blue",
                new Engine("Diesel"),
                new Interior("Fabric", true),
                new WheelConfig(4));
    }
}
