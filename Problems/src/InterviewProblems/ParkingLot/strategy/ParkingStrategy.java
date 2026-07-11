package InterviewProblems.ParkingLot.strategy;

import InterviewProblems.ParkingLot.models.Floor;
import InterviewProblems.ParkingLot.models.Vehicle;

import java.util.List;

public interface ParkingStrategy {

    Floor selectFloor(
            List<Floor> floors,
            Vehicle vehicle
    );

}
