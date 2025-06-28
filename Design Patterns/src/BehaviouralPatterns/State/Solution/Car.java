package BehaviouralPatterns.State.Solution;

public class Car implements TransportationMode {
    @Override
    public int calcETA() {
        System.out.println("Calculating ETA (Car)");
        return 3;  // Example ETA for car
    }

    @Override
    public String getDirection() {
        return "Directions for driving";
    }
}
