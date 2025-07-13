package ParkingLot.Version1;

public class EntranceGate {
    private final ParkingLot parkingLot;

    public EntranceGate() {
        this.parkingLot = ParkingLot.getInstance(null); // Already initialized in Main
    }

    public Ticket generateTicket(Vehicle vehicle) {
        return parkingLot.parkVehicle(vehicle);
    }
}
