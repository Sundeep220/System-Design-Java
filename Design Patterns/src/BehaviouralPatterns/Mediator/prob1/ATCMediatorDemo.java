package BehaviouralPatterns.Mediator.prob1;

public class ATCMediatorDemo {
    public static void main(String[] args) {
        AirTrafficControlTower atc = new ConcreteATC();

        Airplane plane1 = new Airplane("A", atc);
        Airplane plane2 = new Airplane("B", atc);
        Airplane plane3 = new Airplane("C", atc);

        plane1.requestLanding();
        plane2.requestLanding();
        plane3.requestTakeOff();

        plane1.land(); // This should free up the runway and let next plane land
        plane2.land();
        plane3.takeOff();
    }
}

