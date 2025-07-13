package ParkingLot.Version2.Spots;

import ParkingLot.Version2.ParkingSpot;
import ParkingLot.Version2.SpotType;

import java.util.Map;

public class CompactSpot extends ParkingSpot {
    public CompactSpot(String id, Map<String, Integer> distances) {
        super(id, SpotType.COMPACT, distances);
    }
}
