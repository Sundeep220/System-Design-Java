package ParkingLot.Version3.interfaces;

import ParkingLot.Version3.dto.ParkingTicket;
import ParkingLot.Version3.dto.Vehicle.Vehicle;
import ParkingLot.Version3.expections.InvalidTicketException;

public interface ParkingService {
    ParkingTicket entry(Vehicle vehicle);
    int exit(ParkingTicket ticket, Vehicle vehicle) throws InvalidTicketException;
}
