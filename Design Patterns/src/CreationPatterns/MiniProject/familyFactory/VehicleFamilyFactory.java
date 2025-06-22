package CreationPatterns.MiniProject.familyFactory;

import CreationPatterns.MiniProject.Vehicle;

public interface VehicleFamilyFactory {
    Vehicle createElectric();
    Vehicle createPetrol();
    Vehicle createDiesel();
}
