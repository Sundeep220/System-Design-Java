package ParkingLot.Version2.VehicleTypes;


import ParkingLot.Version2.VehicleTypes.VehicleType;

public class HandicappedVehicle implements VehicleType {
    public String getName() { return "HANDICAPPED"; }
    public double getHourlyRate() { return 10; }
}