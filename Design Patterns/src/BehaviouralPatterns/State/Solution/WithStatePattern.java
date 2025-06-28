package BehaviouralPatterns.State.Solution;


public class WithStatePattern {
    public static void main(String[] args) {
        DirectionService service = new DirectionService(new Car());
        // service.setTransportationMode(new Cycling());

        System.out.println("ETA: " + service.getETA());
        System.out.println("Direction: " + service.getDirection());


    }
}
