package ParkingLot.Version2.VehicleTypes;

import ParkingLot.Version2.VehicleTypes.VehicleType;


public class Bike implements VehicleType {
    public String getName() { return "BIKE"; }
    public double getHourlyRate() { return 10; }
}