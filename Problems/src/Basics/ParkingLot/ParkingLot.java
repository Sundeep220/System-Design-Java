package Basics.ParkingLot;

import java.util.ArrayList;
import java.util.List;

import java.util.*;

public class ParkingLot {


    private PriorityQueue<ParkingSpot> freeSpots;
    private Map<String, ParkingSpot> vehicleToSpotMap;

    public ParkingLot(int totalSpots) {

        freeSpots = new PriorityQueue<>(Comparator.comparingInt(ParkingSpot::getSpotNumber));
        vehicleToSpotMap = new HashMap<>();

        for (int i = 1; i <= totalSpots; i++) {
            freeSpots.offer(new ParkingSpot(i));
        }
    }


    public void parkVehicle(Vehicle vehicle) {

        String license = vehicle.getLicenseNumber();

        if (vehicleToSpotMap.containsKey(license)) {
            System.out.println("Vehicle already parked.");
            return;
        }

        if (freeSpots.isEmpty()) {
            System.out.println("Parking lot is full.");
            return;
        }

        ParkingSpot spot = freeSpots.poll();
        spot.parkVehicle(vehicle);

        vehicleToSpotMap.put(license, spot);

        System.out.println("Vehicle parked at spot: "
                + spot.getSpotNumber());
    }


    public void removeVehicle(String licenseNumber) {

        ParkingSpot spot = vehicleToSpotMap.get(licenseNumber);

        if (spot == null) {
            System.out.println("Vehicle not found.");
            return;
        }

        spot.removeVehicle();
        vehicleToSpotMap.remove(licenseNumber);

        freeSpots.offer(spot);

        System.out.println("Vehicle removed from spot: "
                + spot.getSpotNumber());
    }


    public void showAvailableSpots() {

        System.out.print("Available spots: ");

        for (ParkingSpot spot : freeSpots) {
            if (!spot.isOccupied()) {
                System.out.print(spot.getSpotNumber() + " ");
            }
        }

        System.out.println();
    }
}
