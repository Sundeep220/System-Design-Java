package InterviewProblems.ParkingLot;

import InterviewProblems.ParkingLot.enums.StrategyType;
import InterviewProblems.ParkingLot.enums.VehicleType;
import InterviewProblems.ParkingLot.factory.ParkingStrategyFactory;
import InterviewProblems.ParkingLot.models.Floor;
import InterviewProblems.ParkingLot.models.ParkingSpot;
import InterviewProblems.ParkingLot.models.Ticket;
import InterviewProblems.ParkingLot.models.Vehicle;
import InterviewProblems.ParkingLot.strategy.ParkingStrategy;

import java.util.*;

public class ParkingLot {

    private static final ParkingLot INSTANCE = new ParkingLot();

    private final List<Floor> floors;
    private final Map<String, Ticket> vehicleMap;
    private final Map<String, Ticket> ticketMap;
    private final Map<String, Floor> spotToFloorMap;

    private ParkingLot() {
        this.floors = new ArrayList<>();
        this.vehicleMap = new HashMap<>();
        this.ticketMap = new HashMap<>();
        this.spotToFloorMap = new HashMap<>();
    }

    public static ParkingLot getInstance() {
        return INSTANCE;
    }

    /**
     * layouts.get(i) represents the layout of floor i
     */
    public void init(List<int[][]> layouts) {

        floors.clear();
        vehicleMap.clear();
        ticketMap.clear();
        spotToFloorMap.clear();

        for (int i = 0; i < layouts.size(); i++) {
            Floor floor = new Floor(i, layouts.get(i));
            floors.add(floor);
            for (List<ParkingSpot> row : floor.getSpots()) {
                for (ParkingSpot spot : row) {
                    spotToFloorMap.put(spot.getSpotId(), floor);
                }
            }
        }
    }

    public Ticket park(VehicleType vehicleType,
                       String vehicleNumber,
                       String ticketId,
                       StrategyType strategyType) {

        // Vehicle already parked
        if (vehicleMap.containsKey(vehicleNumber)) {
            System.out.println("Vehicle already parked.");
            return null;
        }

        if (ticketMap.containsKey(ticketId)) {
            System.out.println("Ticket Id already exists.");
            return null;
        }

        Vehicle vehicle = new Vehicle(vehicleNumber, vehicleType);

        ParkingStrategy strategy = ParkingStrategyFactory.getStrategy(strategyType);

        Floor floor = strategy.selectFloor(floors, vehicle);

        if (floor == null) {
            System.out.println("Parking Full.");
            return null;
        }

        ParkingSpot spot = floor.findAvailableSpot(vehicle);

        if (spot == null) {
            System.out.println("Parking Full.");
            return null;
        }

        spot.parkVehicle(vehicle);

        floor.decrementFreeSpot(vehicleType);

        Ticket ticket = new Ticket(
                ticketId,
                vehicle,
                spot
        );

        vehicleMap.put(vehicleNumber, ticket);
        ticketMap.put(ticketId, ticket);

        return ticket;
    }

    public void removeVehicle(String spotId) {

        Floor floor = spotToFloorMap.get(spotId);

        if (floor == null) {
            System.out.println("Invalid Spot Id.");
            return;
        }

        ParkingSpot spot = floor.getSpot(spotId);

        if (spot == null) {
            System.out.println("Invalid Spot Id.");
            return;
        }

        Vehicle vehicle;
        try {
            vehicle = spot.removeVehicle();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }

        floor.incrementFreeSpot(vehicle.vehicleType());

        Ticket ticket = vehicleMap.remove(vehicle.vehicleNumber());

        if (ticket != null) {
            ticketMap.remove(ticket.getTicketId());
        }
    }

    public Ticket searchVehicle(String query) {

        Ticket ticket = vehicleMap.get(query);

        if (ticket != null) {
            return ticket;
        }

        return ticketMap.get(query);
    }

    public int getFreeSpotsCount(int floorNumber,
                                 VehicleType vehicleType) {

        if (floorNumber < 0 || floorNumber >= floors.size()) {
            throw new IllegalArgumentException("Invalid floor number");
        }

        return floors.get(floorNumber)
                .getFreeSpotsCount(vehicleType);
    }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }
}
