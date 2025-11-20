package ParkingLot.Version3.dto.ParkingSpot;

public class Compact extends ParkingSpot {

    public Compact(Integer floor) {
        super(floor, 30);
    }

    @Override
    public int cost(int hours) {
        return hours * this.getAmount();
    }
}
