package BehaviouralPatterns.Mediator.prob1;

public interface AirTrafficControlTower {
   void requestToLand(Airplane plane);
   void requestToTakeOff(Airplane plane);
   void notifyRunwayClear();
}
