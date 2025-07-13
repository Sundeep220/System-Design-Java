package ParkingLot.Version1;

public class ExitGate {
    private final ParkingLot parkingLot;

    public ExitGate() {
        this.parkingLot = ParkingLot.getInstance(null);
    }

    public void processExit(Ticket ticketId, String paymentMethod) {
        parkingLot.exitVehicle(ticketId, paymentMethod);
    }
}