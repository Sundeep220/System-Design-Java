package BehaviouralPatterns.Strategy.prob1;

public class CarTravel implements TravelStrategy {

    @Override
    public void calculateTime(double distanceInKm) {
        System.out.println("Car travel time: " + distanceInKm / 60 + " hours");
    }
}
