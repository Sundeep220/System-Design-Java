package CreationPatterns.MiniProject.familyFactory;
import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;
import CreationPatterns.MiniProject.vehicles.Truck;


public class TruckFactory implements VehicleFamilyFactory {
    @Override
    public Vehicle createElectric() {
        return new Truck("E-Truck", "Green",
                new Engine("Electric"),
                new Interior("Vinyl", true),
                new WheelConfig(6));
    }

    @Override
    public Vehicle createPetrol() {
        return new Truck("PetrolTruck", "Red",
                new Engine("Petrol"),
                new Interior("Vinyl", false),
                new WheelConfig(6));
    }

    @Override
    public Vehicle createDiesel() {
        return new Truck("DieselTruck", "Blue",
                new Engine("Diesel"),
                new Interior("Vinyl", false),
                new WheelConfig(6));
    }
}
