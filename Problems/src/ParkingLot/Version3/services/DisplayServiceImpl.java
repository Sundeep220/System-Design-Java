package ParkingLot.Version3.services;

import ParkingLot.Version3.dto.DisplayBoard;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.interfaces.DisplayService;

public class DisplayServiceImpl implements DisplayService {

    @Override
    public void update(ParkingSpotEnum type, Integer change) {
        // TODO Auto-generated method stub
        int currentCount = DisplayBoard.getInstance().getFreeParkingSpots().get(type);
        int newCount = currentCount + change;
        DisplayBoard.getInstance().getFreeParkingSpots().replace(type, newCount);

    }
}
