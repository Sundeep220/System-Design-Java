package BehaviouralPatterns.Strategy.prob1;

public class Main {
    public static void main(String[] args) {
        TravelPlanner planner = new TravelPlanner();

        planner.setTravelStrategy(new CarTravel());
        planner.planTravel(300); // Output: Estimated travel time by Car: 5.0 hrs

        planner.setTravelStrategy(new TrainTravel());
        planner.planTravel(300); // Output: Estimated travel time by Train: 2.5 hrs

        planner.setTravelStrategy(new FlightTravel());
        planner.planTravel(300); // Output: Estimated travel time by Flight: 0.375 hrs
    }
}
