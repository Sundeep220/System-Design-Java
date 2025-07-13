package ParkingLot.Version2.SpotAllocation;

import ParkingLot.Version2.ParkingSpot;
import ParkingLot.Version2.Vehicle;

import java.util.List;

public interface SpotAllocator {
    ParkingSpot allocateSpot(String entranceId, Vehicle vehicle, List<ParkingSpot> allSpots);
}
