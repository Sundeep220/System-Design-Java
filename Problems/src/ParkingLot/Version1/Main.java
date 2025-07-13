package ParkingLot.Version1;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<Spot> initialSpots = Arrays.asList(
                new Spot("S1", "CAR"),
                new Spot("S2", "BIKE"),
                new Spot("S3", "TRUCK")
        );

        ParkingLot.getInstance(initialSpots); // Initialize singleton first

        EntranceGate entrance = new EntranceGate();
        ExitGate exit = new ExitGate();

        Vehicle car = new Vehicle("DL-1111", new CarType());
        Vehicle bike = new Vehicle("DL-2222", new BikeType());
        Vehicle truck = new Vehicle("DL-3333", new TruckType());

        Ticket t1 = entrance.generateTicket(car);
        Ticket t2 = entrance.generateTicket(bike);
        Ticket t3 = entrance.generateTicket(truck);

        Thread.sleep(1000); // simulate delay

        exit.processExit(t1, "CARD");
        exit.processExit(t2, "UPI");
        exit.processExit(t3, "UPI");
    }
}
