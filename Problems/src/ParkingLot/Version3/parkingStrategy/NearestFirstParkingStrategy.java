package ParkingLot.Version3.parkingStrategy;

import ParkingLot.Version3.dto.ParkingLot;
import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.expections.SportNotFoundException;

import java.util.List;

public class NearestFirstParkingStrategy implements Strategy {
    @Override
    public ParkingSpot findParkingSpot(ParkingSpotEnum parkingSpotEnum) throws SportNotFoundException {
        List<ParkingSpot> freeparkingSpots = ParkingLot.getInstance().getFreeparkingSpots().get(parkingSpotEnum);
        if(freeparkingSpots.size() == 0){
            throw new SportNotFoundException("Sport not found in Nearest First Parking Strategy");
        }
        return freeparkingSpots.get(0);
    }
}
