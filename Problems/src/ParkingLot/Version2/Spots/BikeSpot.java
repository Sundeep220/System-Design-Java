package ParkingLot.Version2.Spots;

import ParkingLot.Version2.ParkingSpot;
import ParkingLot.Version2.SpotType;

import java.util.Map;

public class BikeSpot extends ParkingSpot {
    public BikeSpot(String id, Map<String, Integer> distances) {
        super(id, SpotType.BIKE, distances);
    }
}