package ParkingLot.Version3.services;

import ParkingLot.Version3.dto.ParkingLot;
import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.interfaces.DisplayService;
import ParkingLot.Version3.interfaces.ParkingSpotService;

import java.lang.reflect.InvocationTargetException;

public class ParkingSpotServiceImpl implements ParkingSpotService {

    DisplayService displayService = new DisplayServiceImpl();

    @Override
    public ParkingSpot create(ParkingSpotEnum type, Integer floor) {
        try {
            ParkingSpot parkingSpot = (ParkingSpot) type.getParkingSpot().getConstructor(Integer.class).newInstance(floor);
            ParkingLot.getInstance().getFreeparkingSpots().get(type).add(parkingSpot);
            displayService.update(type, 1);
            return parkingSpot;

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
