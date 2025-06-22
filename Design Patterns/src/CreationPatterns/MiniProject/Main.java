package CreationPatterns.MiniProject;

import CreationPatterns.MiniProject.builder.VehicleBuilder;
import CreationPatterns.MiniProject.familyFactory.CarFactory;
import CreationPatterns.MiniProject.familyFactory.VehicleFamilyFactory;
import CreationPatterns.MiniProject.logging.LogService;
import CreationPatterns.MiniProject.registry.VehicleRegistry;

public class Main {
    public static void main(String[] args) {
        LogService.log("--- Registering Vehicle Prototypes ---");

        // Creating a prototype using builder
        Vehicle electricTruck = new VehicleBuilder("truck")
                .setModel("MaxTruck")
                .setColor("Green")
                .setEngine("Electric")
                .setInterior("Vinyl", true)
                .setWheels(6)
                .build();

        // Instantiating the registry
        VehicleRegistry registry = VehicleRegistry.getInstance();
        // Registering the prototype to the registry
        registry.register("maxtruck", electricTruck);

        LogService.log("--- Cloning from Registry ---");
        Vehicle cloned1 = registry.getClone("maxtruck");
        Vehicle cloned2 = registry.getClone("maxtruck");

        VersionedVehicle v1 = new VersionedVehicle(cloned1, "v1.0");
        VersionedVehicle v2 = new VersionedVehicle(cloned2, "v1.1");

        v1.display();
        v2.display();

        LogService.log("--- Using Abstract Factory ---");
        VehicleFamilyFactory carFactory = new CarFactory();
        Vehicle eCar = carFactory.createElectric();
        Vehicle pCar = carFactory.createPetrol();

        VersionedVehicle ve1 = new VersionedVehicle(eCar, "Car-E1");
        VersionedVehicle ve2 = new VersionedVehicle(pCar, "Car-P1");

        ve1.display();
        ve2.display();
    }
}
