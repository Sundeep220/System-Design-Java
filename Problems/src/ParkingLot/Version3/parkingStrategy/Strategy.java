package ParkingLot.Version3.parkingStrategy;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;

public interface Strategy {
    ParkingSpot findParkingSpot(ParkingSpotEnum parkingSpotEnum);
}
