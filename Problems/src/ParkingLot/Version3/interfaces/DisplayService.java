package ParkingLot.Version3.interfaces;

import ParkingLot.Version3.enums.ParkingSpotEnum;

public interface DisplayService {
    void update(ParkingSpotEnum type, Integer change);
}
