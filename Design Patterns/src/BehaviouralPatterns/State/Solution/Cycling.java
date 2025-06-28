package BehaviouralPatterns.State.Solution;

public class Cycling implements TransportationMode{
    @Override
    public int calcETA() {
        System.out.println("Calculating ETA (Cycling)");
        return 5;  // Example ETA for cycling
    }

    @Override
    public String getDirection() {
        return "Directions for cycling";
    }
}
