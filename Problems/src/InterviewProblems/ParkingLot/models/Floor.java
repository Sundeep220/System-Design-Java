package InterviewProblems.ParkingLot.models;

import InterviewProblems.ParkingLot.enums.SpotType;
import InterviewProblems.ParkingLot.enums.VehicleType;

import java.util.*;

public class Floor {

    private final int floorNumber;
    private final ParkingSpot[][] spots;
    private final Map<String, ParkingSpot> spotMap;

    private int freeTwoWheelerSpots;
    private int freeFourWheelerSpots;

    public Floor(int floorNumber, int[][] layout) {

        this.floorNumber = floorNumber;

        if (layout == null || layout.length == 0) {
            throw new IllegalArgumentException(
                    "Invalid layout.");
        }

        int rows = layout.length;
        int cols = layout[0].length;

        spots = new ParkingSpot[rows][cols];
        spotMap = new HashMap<>();

        for (int row = 0; row < rows; row++) {

            for (int col = 0; col < cols; col++) {

                SpotType spotType = convert(layout[row][col]);

                String spotId = floorNumber + "-" + row + "-" + col;

                ParkingSpot spot = new ParkingSpot(spotId, spotType);

                spots[row][col] = spot;
                spotMap.put(spotId, spot);

                if (SpotType.TWO_WHEELER == spotType) {
                    freeTwoWheelerSpots++;
                } else if (SpotType.FOUR_WHEELER == spotType) {
                    freeFourWheelerSpots++;
                }
            }
        }
    }

    private SpotType convert(int value) {

        return switch (value) {
            case 0 -> SpotType.INACTIVE;
            case 2 -> SpotType.TWO_WHEELER;
            case 4 -> SpotType.FOUR_WHEELER;
            default -> throw new IllegalArgumentException(
                    "Unknown Spot Type : " + value);
        };
    }

    public boolean hasAvailableSpot(Vehicle vehicle) {

        if (vehicle.vehicleType() == VehicleType.TWO_WHEELER) {
            return freeTwoWheelerSpots > 0;
        }

        return freeFourWheelerSpots > 0;
    }

    public ParkingSpot findAvailableSpot(Vehicle vehicle) {

        for (int row = 0; row < spots.length; row++) {

            for (int col = 0; col < spots[0].length; col++) {

                ParkingSpot spot = spots[row][col];

                if (spot.canPark(vehicle)) {
                    return spot;
                }
            }
        }

        return null;
    }

    public ParkingSpot getSpot(String spotId) {
        return spotMap.get(spotId);
    }

    public List<List<ParkingSpot>> getSpots() {
        return Arrays.stream(spots)
                .map(row -> Collections.unmodifiableList(Arrays.asList(row)))
                .toList();
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public int getFreeSpotsCount(VehicleType type) {

        if (VehicleType.TWO_WHEELER == type) {
            return freeTwoWheelerSpots;
        }

        return freeFourWheelerSpots;
    }

    public void decrementFreeSpot(VehicleType type) {

        if (VehicleType.TWO_WHEELER == type) {
            freeTwoWheelerSpots--;
        } else {
            freeFourWheelerSpots--;
        }
    }

    public void incrementFreeSpot(VehicleType type) {

        if (VehicleType.TWO_WHEELER == type) {
            freeTwoWheelerSpots++;
        } else {
            freeFourWheelerSpots++;
        }
    }
}