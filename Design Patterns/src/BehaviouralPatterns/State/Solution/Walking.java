package BehaviouralPatterns.State.Solution;

public class Walking implements TransportationMode{
    @Override
    public int calcETA() {
        System.out.println("Calculating ETA (Walking)");
        return 10;  // Example ETA for walking
    }

    @Override
    public String getDirection() {
        return "Directions for walking";
    }
}
