package BehaviouralPatterns.State.prob1;

class YellowLight implements TrafficLightState {
    public void switchLight(TrafficLight context) {
        System.out.println("Traffic Light is YELLOW. Caution!");
        context.setState(new RedLight());
    }
}