package ParkingLot.Version3.services;

import ParkingLot.Version3.dto.DisplayBoard;
import ParkingLot.Version3.dto.ParkingEvent;
import ParkingLot.Version3.enums.ParkingEventType;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.interfaces.DisplayService;
import ParkingLot.Version3.interfaces.Observer;

public class DisplayServiceImpl implements DisplayService, Observer {

//    @Override
    public void update(ParkingSpotEnum type, Integer change) {
        // TODO Auto-generated method stub
        Integer currentCount = DisplayBoard.getInstance().getFreeParkingSpots().get(type);
        if(currentCount == null) currentCount = 0;
        int newCount = currentCount + change;
        DisplayBoard.getInstance().getFreeParkingSpots().replace(type, newCount);

    }

    @Override
    public void update(ParkingEvent event) {
        Integer currentCount = DisplayBoard.getInstance().getFreeParkingSpots().get(event.getParkingSpotEnum());
        if(currentCount == null) currentCount = 0;
        int change = event.getEventType().equals(ParkingEventType.ENTRY) ? -1 : 1;
        int newCount = currentCount + change;
        DisplayBoard.getInstance().getFreeParkingSpots().replace(event.getParkingSpotEnum(), newCount);
    }
}
