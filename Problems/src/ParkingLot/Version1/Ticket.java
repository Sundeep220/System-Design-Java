package ParkingLot.Version1;

import java.time.LocalDateTime;

/**
 * @Purpose: This class represents a ticket in a parking lot.
 */
public class Ticket {
    private String ticketId;
    private String spotId;
    private LocalDateTime entryTime;
    private Vehicle vehicle;

    public Ticket(String ticketId, String spotId, LocalDateTime entryTime, Vehicle vehicle) {
        this.ticketId = ticketId;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.vehicle = vehicle;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getSpotId() {
        return spotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
