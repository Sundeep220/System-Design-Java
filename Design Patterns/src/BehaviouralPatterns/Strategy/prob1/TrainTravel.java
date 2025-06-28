package BehaviouralPatterns.Strategy.prob1;

public class TrainTravel implements TravelStrategy {
    @Override
    public void calculateTime(double distanceInKm) {
        System.out.println("Train travel time is " + distanceInKm/120 + " hours.");
    }
}
