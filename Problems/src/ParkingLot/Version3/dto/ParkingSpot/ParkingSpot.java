package ParkingLot.Version3.dto.ParkingSpot;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ParkingSpot {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private int id;
    private boolean isFree;
    private int floor;
    private int amount;

    public ParkingSpot(int floor, int amount) {
        this.floor = floor;
        this.amount = amount;
        this.id = counter.incrementAndGet();
        this.isFree = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
