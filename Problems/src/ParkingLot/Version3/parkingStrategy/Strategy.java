package ParkingLot.Version3.parkingStrategy;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.expections.SpotNotFoundException;

public interface Strategy {
    ParkingSpot findParkingSpot(ParkingSpotEnum parkingSpotEnum) throws SpotNotFoundException;
}
