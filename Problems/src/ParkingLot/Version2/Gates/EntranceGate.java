package ParkingLot.Version2.Gates;

import ParkingLot.Version2.ParkingLot;
import ParkingLot.Version2.Ticket;
import ParkingLot.Version2.Vehicle;

public class EntranceGate {
    private final String gateId;

    public EntranceGate(String gateId) {
        this.gateId = gateId;
    }

    public Ticket enterVehicle(Vehicle vehicle) {
        System.out.println("[Entrance: " + gateId + "] Vehicle " + vehicle.getNumberPlate() + " trying to enter...");
        ParkingLot lot = ParkingLot.getInstance();
        return lot.parkVehicle(gateId, vehicle);
    }
}

