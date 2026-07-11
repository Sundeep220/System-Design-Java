package InterviewProblems.ParkingLot.models;

import InterviewProblems.ParkingLot.enums.SpotType;
import InterviewProblems.ParkingLot.enums.VehicleType;

public class ParkingSpot {

    private final String spotId;
    private final SpotType spotType;

    private Vehicle parkedVehicle;

    public ParkingSpot(String spotId, SpotType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
    }

    public String getSpotId() {
        return spotId;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public boolean isAvailable() {
        return parkedVehicle == null;
    }

    public boolean canPark(Vehicle vehicle) {

        if (vehicle == null) {
            return false;
        }

        if (!isAvailable()) {
            return false;
        }

        if (spotType == SpotType.INACTIVE) {
            return false;
        }

        return isCompatible(vehicle);
    }

    private boolean isCompatible(Vehicle vehicle) {

        if (vehicle.getVehicleType() == VehicleType.TWO_WHEELER &&
                spotType == SpotType.TWO_WHEELER) {
            return true;
        }

        if (vehicle.getVehicleType() == VehicleType.FOUR_WHEELER &&
                spotType == SpotType.FOUR_WHEELER) {
            return true;
        }

        return false;
    }

    public void parkVehicle(Vehicle vehicle) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Parking spot is already occupied.");
        }

        this.parkedVehicle = vehicle;
    }
    public Vehicle removeVehicle() {
        if (parkedVehicle == null) {
            throw new IllegalStateException(
                    "Parking spot is already empty.");
        }

        Vehicle vehicle = parkedVehicle;
        parkedVehicle = null;

        return vehicle;
    }
}