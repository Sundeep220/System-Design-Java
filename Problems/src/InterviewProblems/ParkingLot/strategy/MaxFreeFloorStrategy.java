package InterviewProblems.ParkingLot.strategy;

import InterviewProblems.ParkingLot.models.Floor;
import InterviewProblems.ParkingLot.models.Vehicle;

import java.util.List;

public class MaxFreeFloorStrategy implements ParkingStrategy {

    @Override
    public Floor selectFloor(List<Floor> floors,
                             Vehicle vehicle) {

        Floor bestFloor = null;
        int maxFreeSpots = -1;

        for (Floor floor : floors) {

            if (!floor.hasAvailableSpot(vehicle)) {
                continue;
            }

            int freeSpots =
                    floor.getFreeSpotsCount(vehicle.getVehicleType());

            if (freeSpots > maxFreeSpots) {

                maxFreeSpots = freeSpots;
                bestFloor = floor;
            }
        }

        return bestFloor;
    }
}