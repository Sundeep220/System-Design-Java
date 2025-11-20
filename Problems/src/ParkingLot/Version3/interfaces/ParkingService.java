package ParkingLot.Version3.interfaces;

import ParkingLot.Version3.dto.ParkingTicket;
import ParkingLot.Version3.dto.Vehicle.Vehicle;

public interface ParkingService {
    ParkingTicket entry(Vehicle vehicle);
    void exit(ParkingTicket ticket, Vehicle vehicle);
}
