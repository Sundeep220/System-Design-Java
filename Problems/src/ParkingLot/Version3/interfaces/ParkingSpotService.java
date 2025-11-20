package ParkingLot.Version3.interfaces;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;

public interface ParkingSpotService {
    ParkingSpot create(ParkingSpotEnum type, Integer floor);
}
