package ParkingLot.Version3.interfaces;

import ParkingLot.Version3.dto.ParkingEvent;

public interface Observer {
    void update(ParkingEvent event);
}
