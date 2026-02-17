package ParkingLot.Version3.parkingStrategy;

import ParkingLot.Version3.dto.ParkingLot;
import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.expections.SpotNotFoundException;

import java.util.List;
import java.util.NavigableSet;

public class FarthestFirstParkingStrategy implements Strategy {

    @Override
    public ParkingSpot findParkingSpot(ParkingSpotEnum parkingSpotEnum)
            throws SpotNotFoundException {

        NavigableSet<ParkingSpot> freeSpots =
                ParkingLot.getInstance()
                        .getFreeparkingSpots()
                        .get(parkingSpotEnum);

        if (freeSpots.isEmpty()) {
            throw new SpotNotFoundException(
                    "Spot not found in Farthest First Strategy");
        }

        return freeSpots.last();  // O(log n)
    }
}

