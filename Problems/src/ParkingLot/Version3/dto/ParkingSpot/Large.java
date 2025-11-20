package ParkingLot.Version3.dto.ParkingSpot;

public class Large extends ParkingSpot {
    public Large(Integer floor) {
        super(floor, 50);
    }

    @Override
    public int cost(int hours) {
        return hours * this.getAmount();
    }
}
