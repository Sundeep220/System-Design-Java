package ParkingLot.Version3.enums;

import ParkingLot.Version3.dto.ParkingSpot.Compact;
import ParkingLot.Version3.dto.ParkingSpot.Large;
import ParkingLot.Version3.dto.ParkingSpot.Mini;

public enum ParkingSpotEnum {
    COMPACT(Compact.class),
    MINI(Mini.class),
    LARGE(Large.class);

    private Class parkingSpot;

    ParkingSpotEnum(Class parkingSpot) {
        this.parkingSpot = parkingSpot;
    }

    public Class getParkingSpot() {
        return parkingSpot;
    }
}
