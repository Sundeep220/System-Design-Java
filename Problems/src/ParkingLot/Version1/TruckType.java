package ParkingLot.Version1;

public class TruckType implements VehicleType {
    public double getHourlyRate() {
        return 50;
    }

    public String getTypeName() {
        return "TRUCK";
    }
}
