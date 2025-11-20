package ParkingLot.Version3.dto.ParkingSpot;

public class Mini extends ParkingSpot {
    public Mini(Integer floor) {
        super(floor, 20);
    }

    @Override
    public int cost(int hours) {
        return hours * this.getAmount();
    }
}
