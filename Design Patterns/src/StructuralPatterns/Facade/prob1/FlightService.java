package StructuralPatterns.Facade.prob1;

public class FlightService {
    public void bookFlight(String from, String to) {
        System.out.println("Flight booked from " + from + " to " + to);
    }

    public void cancelFlight() {
        System.out.println("Flight booking cancelled");
    }
}
