package InterviewProblems.ParkingLot.factory;

import InterviewProblems.ParkingLot.enums.StrategyType;
import InterviewProblems.ParkingLot.strategy.FirstAvailableStrategy;
import InterviewProblems.ParkingLot.strategy.MaxFreeFloorStrategy;
import InterviewProblems.ParkingLot.strategy.ParkingStrategy;

public class ParkingStrategyFactory {
    private static final ParkingStrategy FIRST = new FirstAvailableStrategy();

    private static final ParkingStrategy MAX_FREE = new MaxFreeFloorStrategy();

    public static ParkingStrategy getStrategy(StrategyType strategyType) {

        return switch (strategyType) {
            case FIRST_AVAILABLE -> FIRST;
            case MAX_FREE_FLOOR -> MAX_FREE;
            default -> throw new IllegalArgumentException(
                    "Unknown Strategy");
        };
    }
}