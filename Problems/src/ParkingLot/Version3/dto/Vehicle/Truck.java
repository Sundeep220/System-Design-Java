package ParkingLot.Version3.dto.Vehicle;

import ParkingLot.Version3.enums.ParkingSpotEnum;

public class Truck extends Vehicle {
    public Truck() {
        super(ParkingSpotEnum.LARGE);
    }
}
