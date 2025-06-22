package CreationPatterns.MiniProject.familyFactory;
import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;
import CreationPatterns.MiniProject.vehicles.Bike;


public class BikeFactory implements VehicleFamilyFactory {
    @Override
    public Vehicle createElectric() {
        return new Bike("E-Bike", "White",
                new Engine("Electric"),
                new Interior("Fabric", false),
                new WheelConfig(2));
    }

    @Override
    public Vehicle createPetrol() {
        return new Bike("PetrolBike", "Black",
                new Engine("Petrol"),
                new Interior("Fabric", false),
                new WheelConfig(2));
    }

    @Override
    public Vehicle createDiesel() {
        return new Bike("DieselBike", "Grey",
                new Engine("Diesel"),
                new Interior("Fabric", false),
                new WheelConfig(2));
    }
}
