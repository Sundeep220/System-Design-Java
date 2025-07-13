package ParkingLot.Version2.Spots;

import ParkingLot.Version2.ParkingSpot;
import ParkingLot.Version2.SpotType;

import java.util.Map;

public class LargeSpot extends ParkingSpot {
    public LargeSpot(String id, Map<String, Integer> distances) {
        super(id, SpotType.LARGE, distances);
    }
}