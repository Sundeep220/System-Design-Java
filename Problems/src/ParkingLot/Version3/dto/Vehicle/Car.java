package ParkingLot.Version3.dto.Vehicle;

import ParkingLot.Version3.enums.ParkingSpotEnum;

public class Car extends Vehicle {
    public Car() {
        super(ParkingSpotEnum.COMPACT);
    }
}
