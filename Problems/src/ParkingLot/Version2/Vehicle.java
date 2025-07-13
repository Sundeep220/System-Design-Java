package ParkingLot.Version2;

import ParkingLot.Version2.VehicleTypes.VehicleType;

public class Vehicle {
    private final String numberPlate;
    private final VehicleType type;

    public Vehicle(String numberPlate, VehicleType type) {
        this.numberPlate = numberPlate;
        this.type = type;
    }

    public String getNumberPlate() {
        return numberPlate;
    }

    public VehicleType getType() {
        return type;
    }
}
