package ParkingLot.Version2;

import java.time.LocalDateTime;

public class Ticket {
    private final String ticketId;
    private final String spotId;
    private final LocalDateTime entryTime;
    private final Vehicle vehicle;
    private final String entranceGateId;

    public Ticket(String ticketId, String spotId, LocalDateTime entryTime, Vehicle vehicle, String entranceGateId) {
        this.ticketId = ticketId;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.vehicle = vehicle;
        this.entranceGateId = entranceGateId;
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

    public String getEntranceGateId() {
        return entranceGateId;
    }
}

