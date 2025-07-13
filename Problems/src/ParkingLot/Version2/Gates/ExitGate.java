package ParkingLot.Version2.Gates;

import ParkingLot.Version2.ParkingLot;
import ParkingLot.Version2.PaymentProcessor;
import ParkingLot.Version2.PaymentService;
import ParkingLot.Version2.Ticket;

import java.time.Duration;
import java.time.LocalDateTime;

public class ExitGate {
    private final String gateId;
    private final PaymentProcessor paymentProcessor;

    public ExitGate(String gateId, PaymentProcessor paymentProcessor) {
        this.gateId = gateId;
        this.paymentProcessor = paymentProcessor;
    }

    public void exitVehicle(String ticketId, PaymentService paymentMethod) {
        System.out.println("[Exit: " + gateId + "] Ticket ID: " + ticketId + " exiting...");

        ParkingLot lot = ParkingLot.getInstance();
        Ticket ticket = lot.getTicket(ticketId);

        if (ticket == null) {
            System.out.println("❌ Invalid Ticket.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long hours = Duration.between(ticket.getEntryTime(), now).toHours();
        if (hours == 0) hours = 1; // charge minimum 1 hour

        double rate = ticket.getVehicle().getType().getHourlyRate();
        double fee = rate * hours;

        paymentProcessor.processPayment(fee, paymentMethod);

        lot.unparkVehicle(ticketId);

        System.out.println("✅ Vehicle " + ticket.getVehicle().getNumberPlate() + " exited. Paid ₹" + fee);
    }
}
