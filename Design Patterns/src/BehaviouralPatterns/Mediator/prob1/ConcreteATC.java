package BehaviouralPatterns.Mediator.prob1;

import java.util.LinkedList;
import java.util.Queue;

class ConcreteATC implements AirTrafficControlTower {
    private boolean runwayFree = true;
    private Queue<Airplane> waitingToLand = new LinkedList<>();
    private Queue<Airplane> waitingToTakeOff = new LinkedList<>();

    @Override
    public void requestToLand(Airplane airplane) {
        // TODO: If runway is free, allow to land. Else, enqueue.
        if (runwayFree) {
            runwayFree = false;
            airplane.notifyClearance("land");
        } else {
            System.out.println("ATC: Runway is busy. Plane " + airplane.getId() + ", please wait to land.");
            waitingToLand.add(airplane);
        }
    }

    @Override
    public void requestToTakeOff(Airplane airplane) {
        // TODO: Same logic for takeoff
        if (runwayFree) {
            runwayFree = false;
            airplane.notifyClearance("take off");
        } else {
            System.out.println("ATC: Runway is busy. Plane " + airplane.getId() + ", please wait to take off.");
            waitingToTakeOff.add(airplane);
        }
    }

    @Override
    public void notifyRunwayClear() {
        // TODO: Check queues and notify next plane in line
        if (!waitingToLand.isEmpty()) {
            Airplane nextToLand = waitingToLand.poll();
            nextToLand.notifyClearance("land");
            runwayFree = false;
        } else if (!waitingToTakeOff.isEmpty()) {
            Airplane nextToTakeOff = waitingToTakeOff.poll();
            nextToTakeOff.notifyClearance("take off");
            runwayFree = false;
        } else {
            runwayFree = true;
            System.out.println("ATC: Runway is now free.");
        }
    }
}
