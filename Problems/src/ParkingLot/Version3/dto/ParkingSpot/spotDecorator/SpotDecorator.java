package ParkingLot.Version3.dto.ParkingSpot.spotDecorator;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;

public abstract class SpotDecorator extends ParkingSpot {
    protected ParkingSpot decoratedSpot;

    public SpotDecorator(ParkingSpot decoratedSpot) {
        super();
        this.decoratedSpot = decoratedSpot;
    }
}
