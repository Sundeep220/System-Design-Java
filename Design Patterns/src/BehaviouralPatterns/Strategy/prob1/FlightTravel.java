package BehaviouralPatterns.Strategy.prob1;

public class FlightTravel implements TravelStrategy {
    @Override
    public void calculateTime(double distanceInKm) {
        System.out.println("Flight travel time is " + distanceInKm/800 + " hours.");
    }
}
