package ParkingLot.Version3.services;

import ParkingLot.Version3.dto.ParkingEvent;
import ParkingLot.Version3.dto.ParkingLot;
import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.dto.ParkingSpot.spotDecorator.WashDecorator;
import ParkingLot.Version3.dto.ParkingTicket;
import ParkingLot.Version3.dto.Vehicle.Vehicle;
import ParkingLot.Version3.enums.ParkingEventType;
import ParkingLot.Version3.enums.ParkingSpotEnum;
import ParkingLot.Version3.expections.InvalidTicketException;
import ParkingLot.Version3.interfaces.DisplayService;
import ParkingLot.Version3.interfaces.Observer;
import ParkingLot.Version3.interfaces.ParkingService;
import ParkingLot.Version3.parkingStrategy.Strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

public class ParkingServiceImpl implements ParkingService {
    Strategy parkingStrategy;
    ParkingLot parkingLot;
    DisplayService displayService;

    private List<Observer> observers;

    public ParkingServiceImpl(Strategy parkingStrategy) {
        this.parkingStrategy = parkingStrategy;
        parkingLot = ParkingLot.getParkingLot();
        displayService = new DisplayServiceImpl();
        observers = new ArrayList<>();
    }

    @Override
    public ParkingTicket entry(Vehicle vehicle) {
        ParkingSpotEnum parkingSpotEnum = vehicle.getParkingSpotEnum();
        NavigableSet<ParkingSpot> freeparkingSpots = parkingLot.getFreeparkingSpots().get(parkingSpotEnum);
        Set<ParkingSpot> occupiedParkingSpots = parkingLot.getOccupiedParkingSpots().get(parkingSpotEnum);
        try{
            ParkingSpot parkingSpot = parkingStrategy.findParkingSpot(parkingSpotEnum);
            if(parkingSpot.isFree()){
                synchronized (parkingSpot) {
                    if (parkingSpot.isFree()) {
                        parkingSpot.setFree(false);
                        occupiedParkingSpots.add(parkingSpot);
                        freeparkingSpots.remove(parkingSpot);
                        ParkingTicket ticket = new ParkingTicket(vehicle, parkingSpot);

                        ParkingEvent event = new ParkingEvent(ParkingEventType.ENTRY, parkingSpotEnum);
                        notifyObservers(event);
//                        displayService.update(parkingSpotEnum, -1);  no need to manually do this
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

    public void addObserver(Observer observer){
        observers.add(observer);
    }

    public void notifyObservers(ParkingEvent event){
        observers.forEach(observer -> observer.update(event));
    }

    private void addParkingSportInFreeList(List<ParkingSpot> parkingSpots, ParkingSpot parkingSpot){
        parkingSpots.add(parkingSpot);
    }

    public void addWash(ParkingTicket ticket){
        ticket.setParkingSpot(new WashDecorator(ticket.getParkingSpot()));
        return;
    }

    @Override
    public int exit(ParkingTicket ticket, Vehicle vehicle) throws InvalidTicketException {
        if(ticket.getVehicle().equals(vehicle)){
            ParkingSpot parkingSpot = ticket.getParkingSpot();
            int amount = parkingSpot.getAmount();
            parkingSpot.setFree(true);
            parkingLot.getOccupiedParkingSpots().get(vehicle.getParkingSpotEnum()).remove(parkingSpot);

            // add it back to free list
            parkingLot.getFreeparkingSpots()
                    .get(vehicle.getParkingSpotEnum())
                    .add(parkingSpot);  // O(log n)

//            addParkingSportInFreeList(parkingLot.getFreeparkingSpots().get(vehicle.getParkingSpotEnum()), parkingSpot);

            ParkingEvent event = new ParkingEvent(ParkingEventType.EXIT, vehicle.getParkingSpotEnum());
            notifyObservers(event);
//            displayService.update(vehicle.getParkingSpotEnum(), 1);
            return amount;
        }else{
            throw new InvalidTicketException("This is an invalid ticket.");
        }
    }
}
