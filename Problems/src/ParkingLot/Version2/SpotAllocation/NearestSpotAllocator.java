package ParkingLot.Version2.SpotAllocation;
import ParkingLot.Version2.ParkingSpot;
import ParkingLot.Version2.SpotAllocation.SpotAllocator;
import ParkingLot.Version2.SpotType;
import ParkingLot.Version2.Vehicle;

import java.util.List;

public class NearestSpotAllocator implements SpotAllocator {

    @Override
    public ParkingSpot allocateSpot(String entranceId, Vehicle vehicle, List<ParkingSpot> allSpots) {
        SpotType vehicleRequiredType = mapVehicleToSpotType(vehicle); // externalized logic

        ParkingSpot bestSpot = null;
        int minDistance = Integer.MAX_VALUE;

        for (ParkingSpot spot : allSpots) {
            if (!spot.isOccupied() && spot.getType() == vehicleRequiredType) {
                int distance = spot.getDistanceFrom(entranceId);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestSpot = spot;
                }
            }
        }
        return bestSpot;
    }

    // This method maps vehicle type to required spot type
    private SpotType mapVehicleToSpotType(Vehicle vehicle) {
        return switch (vehicle.getType().getName()) {
            case "CAR" -> SpotType.COMPACT;
            case "BIKE" -> SpotType.BIKE;
            case "TRUCK" -> SpotType.LARGE;
            default -> throw new IllegalArgumentException("Unsupported vehicle type: " + vehicle.getType().getName());
        };
    }
}
