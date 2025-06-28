package BehaviouralPatterns.State.prob1;

class RedLight implements TrafficLightState {
    public void switchLight(TrafficLight context) {
        System.out.println("Traffic Light is RED. Stop!");
        context.setState(new GreenLight());
    }
}
