package ParkingLot.Version3.dto.ParkingSpot.spotDecorator;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;

public class WashDecorator extends SpotDecorator {

    public WashDecorator(ParkingSpot decoratedSpot) {
        super(decoratedSpot);
    }

    @Override
    public int cost(int hours) {
        return decoratedSpot.cost(hours) + 20;
    }
}
