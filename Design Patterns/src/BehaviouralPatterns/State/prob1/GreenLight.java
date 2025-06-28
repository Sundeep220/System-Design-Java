package BehaviouralPatterns.State.prob1;

class GreenLight implements TrafficLightState {
    public void switchLight(TrafficLight context) {
        System.out.println("Traffic Light is GREEN. Go!");
        context.setState(new YellowLight());
    }
}