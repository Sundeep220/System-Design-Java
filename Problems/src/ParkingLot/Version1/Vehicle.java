package ParkingLot.Version1;

/**
 * @Purpose: This class represents a vehicle in a parking lot.
 */
public class Vehicle {
    private final String plateNumber;
    private final VehicleType type;

    public Vehicle(String plateNumber, VehicleType type) {
        this.plateNumber = plateNumber;
        this.type = type;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public VehicleType getType() {
        return type;
    }
}
