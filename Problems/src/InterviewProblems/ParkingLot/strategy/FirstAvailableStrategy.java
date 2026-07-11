package InterviewProblems.ParkingLot.strategy;

import InterviewProblems.ParkingLot.models.Floor;
import InterviewProblems.ParkingLot.models.Vehicle;

import java.util.List;

public class FirstAvailableStrategy implements ParkingStrategy {

    @Override
    public Floor selectFloor(List<Floor> floors, Vehicle vehicle) {

        for (Floor floor : floors) {
            if (floor.hasAvailableSpot(vehicle)) {
                return floor;
            }
        }
        return null;
    }
}