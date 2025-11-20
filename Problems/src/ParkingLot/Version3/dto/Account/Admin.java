package ParkingLot.Version3.dto.Account;

import ParkingLot.Version3.dto.ParkingLot;

public class Admin extends Account {
    private ParkingLot parkingLot = ParkingLot.getInstance();  // hot start (initiliazing at the time of object creation)

}
