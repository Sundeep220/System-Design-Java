package ParkingLot.Version3.dto.ParkingSpot.spotDecorator;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;

public class ElectricChargeDecorator extends SpotDecorator {

    public ElectricChargeDecorator(ParkingSpot decoratedSpot) {
        super(decoratedSpot);
    }

    @Override
    public int cost(int hours) {
        return decoratedSpot.cost(hours) + 50;
    }
}
