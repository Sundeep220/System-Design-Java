package BehaviouralPatterns.State.Solution;

public class DirectionService {
    TransportationMode transportationMode;
    public DirectionService(TransportationMode transportationMode){
        this.transportationMode = transportationMode;
    }
    public void setTransportationMode(TransportationMode mode){
        this.transportationMode = mode;
    }
    //delegating the work current state's concrete class
    public int getETA(){
        return transportationMode.calcETA();
    }
    public String getDirection(){
        return transportationMode.getDirection();
    }
}
