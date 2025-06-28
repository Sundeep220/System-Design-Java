package BehaviouralPatterns.Mediator.prob1;

class Airplane {
    private String id;
    private AirTrafficControlTower tower;

    public Airplane(String id, AirTrafficControlTower tower) {
        this.id = id;
        this.tower = tower;
    }

    public String getId() {
        return id;
    }

    public void requestLanding() {
        System.out.println("Plane " + id + " requests to land.");
        tower.requestToLand(this);
    }

    public void requestTakeOff() {
        System.out.println("Plane " + id + " requests to take off.");
        tower.requestToTakeOff(this);
    }

    public void land() {
        System.out.println("Plane " + id + " has landed.");
        tower.notifyRunwayClear();
    }

    public void takeOff() {
        System.out.println("Plane " + id + " has taken off.");
        tower.notifyRunwayClear();
    }

    public void notifyClearance(String action) {
        System.out.println("ATC: Plane " + id + ", you are cleared to " + action + ".");
    }
}

