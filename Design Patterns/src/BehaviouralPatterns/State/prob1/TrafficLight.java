package BehaviouralPatterns.State.prob1;

public class TrafficLight {
    private TrafficLightState currentState;

    public TrafficLight() {
        currentState = new RedLight(); // Initial state
    }

    public void setState(TrafficLightState state) {
        currentState = state;
    }

    public void change() {
        currentState.switchLight(this);
    }

}
