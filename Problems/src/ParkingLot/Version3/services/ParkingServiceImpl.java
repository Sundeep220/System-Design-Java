package ParkingLot.Version3.services;

import ParkingLot.Version3.dto.ParkingLot;
import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.dto.ParkingTicket;
import ParkingLot.Version3.dto.Vehicle.Vehicle;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.expections.SportNotFoundException;
import ParkingLot.Version3.interfaces.DisplayService;
import ParkingLot.Version3.interfaces.ParkingService;
import ParkingLot.Version3.parkingStrategy.Strategy;

import java.util.List;

public class ParkingServiceImpl implements ParkingService {
    Strategy parkingStrategy;
    ParkingLot parkingLot;
    DisplayService displayService;

    public ParkingServiceImpl(Strategy parkingStrategy) {
        this.parkingStrategy = parkingStrategy;
        parkingLot = ParkingLot.getParkingLot();
        displayService = new DisplayServiceImpl();
    }

    @Override
    public ParkingTicket entry(Vehicle vehicle) {
        ParkingSpotEnum parkingSpotEnum = vehicle.getParkingSpotEnum();
        List<ParkingSpot> freeparkingSpots = parkingLot.getFreeparkingSpots().get(parkingSpotEnum);
        List<ParkingSpot> occupiedParkingSpots = parkingLot.getOccupiedParkingSpots().get(parkingSpotEnum);
        try{
            ParkingSpot parkingSpot = parkingStrategy.findParkingSpot(parkingSpotEnum);
            if(parkingSpot.isFree()){
                synchronized (parkingSpot) {
                    if (parkingSpot.isFree()) {
                        parkingSpot.setFree(false);
                        occupiedParkingSpots.add(parkingSpot);
                        freeparkingSpots.remove(parkingSpot);
                        ParkingTicket ticket = new ParkingTicket(vehicle, parkingSpot);
                        displayService.update(parkingSpotEnum, -1);
                        return ticket;
                    }
                    entry(vehicle);
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void exit(ParkingTicket ticket, Vehicle vehicle) {
    
    }
}
