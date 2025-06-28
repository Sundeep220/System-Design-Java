package BehaviouralPatterns.Strategy.prob1;

public class TravelPlanner {
    private TravelStrategy travelStrategy;

    public void setTravelStrategy(TravelStrategy travelStrategy) {
        this.travelStrategy = travelStrategy;
    }

    public void planTravel(int distanceInKm) {
        travelStrategy.calculateTime(distanceInKm);
    }
}
