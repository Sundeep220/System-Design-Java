package ParkingLot.Version1;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParkingLot {
    private final List<Spot> spots;
    private final Map<Ticket, Spot> activeTickets; // <ticket, spot>
    private final PaymentService paymentService = new PaymentService();
    private static ParkingLot instance;

    private ParkingLot(List<Spot> spots) {
        this.spots = spots;
        this.activeTickets = new HashMap<>();
    }

    public static synchronized ParkingLot getInstance(List<Spot> initialSpots) {
        if (instance == null) {
            instance = new ParkingLot(initialSpots);
        }
        return instance;
    }

    public Ticket parkVehicle(Vehicle vehicle) {
        for (Spot spot : spots) {
            if (spot.canFitVehicle(vehicle)) {
                spot.setOccupied(true);
                Ticket ticket = new Ticket(UUID.randomUUID().toString(), spot.getId(), LocalDateTime.now(), vehicle);
                activeTickets.put(ticket, spot);
                System.out.println("Ticket Issued: " + ticket.getTicketId() + " for Spot: " + spot.getId());
                return ticket;
            }
        }
        System.out.println("No available spot for vehicle type: " + vehicle.getType());
        return null;
    }

    public void exitVehicle(Ticket ticket, String paymentMethod) {
        if (!activeTickets.containsKey(ticket)) {
            System.out.println("Invalid ticket.");
            return;
        }

        Spot spot = activeTickets.get(ticket);
        spot.setOccupied(false);
        activeTickets.remove(ticket);

        long hours = Duration.between(ticket.getEntryTime(), LocalDateTime.now()).toHours();
        if (hours == 0) hours = 1;  // minimum 1 hour

        double hourlyRate = ticket.getVehicle().getType().getHourlyRate();
        double totalFee = hourlyRate * hours;

        System.out.println("Total Fee for " + hours + " hours: ₹" + totalFee);
        paymentService.pay(totalFee, paymentMethod);
    }

    private double getRate(VehicleTypeEnum type) {
        return switch (type) {
            case BIKE -> 10;
            case CAR -> 20;
            case TRUCK -> 50;
            default -> 30;
        };
    }
}
