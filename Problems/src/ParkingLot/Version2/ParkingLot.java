package ParkingLot.Version2;

import ParkingLot.Version2.SpotAllocation.SpotAllocator;

import java.time.LocalDateTime;
import java.util.*;

public class ParkingLot {
    private static ParkingLot instance;

    private final List<ParkingSpot> allSpots;
    private final Map<String, ParkingSpot> spotMap;
    private final Map<String, Ticket> activeTickets;
    private final SpotAllocator spotAllocator;

    private ParkingLot(List<ParkingSpot> spots, SpotAllocator allocator) {
        this.allSpots = spots;
        this.spotAllocator = allocator;
        this.spotMap = new HashMap<>();
        this.activeTickets = new HashMap<>();
        for (ParkingSpot s : spots) {
            spotMap.put(s.getId(), s);
        }
    }

    public static synchronized ParkingLot init(List<ParkingSpot> spots, SpotAllocator allocator) {
        if (instance == null) {
            instance = new ParkingLot(spots, allocator);
        }
        return instance;
    }

    public static ParkingLot getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ParkingLot not initialized.");
        }
        return instance;
    }

    public Ticket parkVehicle(String entranceId, Vehicle vehicle) {
        ParkingSpot spot = spotAllocator.allocateSpot(entranceId, vehicle, allSpots);
        if (spot == null) {
            System.out.println("No available spot for vehicle: " + vehicle.getType().getName());
            return null;
        }

        spot.setOccupied(true);
        Ticket ticket = new Ticket(UUID.randomUUID().toString(), spot.getId(), LocalDateTime.now(), vehicle, entranceId);
        activeTickets.put(ticket.getTicketId(), ticket);
        System.out.println("Ticket Issued: " + ticket.getTicketId() + " at Spot: " + spot.getId());
        return ticket;
    }

    public Ticket getTicket(String ticketId) {
        return activeTickets.get(ticketId);
    }

    public void unparkVehicle(String ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Invalid ticket.");
        }

        ParkingSpot spot = spotMap.get(ticket.getSpotId());
        if (spot != null) {
            spot.setOccupied(false);
        }

        activeTickets.remove(ticketId);
    }

    public List<ParkingSpot> getAllSpots() {
        return allSpots;
    }

    public Map<String, Ticket> getActiveTickets() {
        return activeTickets;
    }
}
