package ParkingLot.Version2;

import ParkingLot.Version2.Gates.ExitGate;
import ParkingLot.Version2.PaymentModes.CardPayment;
import ParkingLot.Version2.PaymentModes.CashPayment;
import ParkingLot.Version2.PaymentModes.UPIPayment;
import ParkingLot.Version2.SpotAllocation.NearestSpotAllocator;
import ParkingLot.Version2.SpotAllocation.SpotAllocator;
import ParkingLot.Version2.Spots.BikeSpot;
import ParkingLot.Version2.Spots.CompactSpot;
import ParkingLot.Version2.Spots.HandicappedSpot;
import ParkingLot.Version2.Spots.LargeSpot;
import ParkingLot.Version2.Gates.EntranceGate;
import ParkingLot.Version2.VehicleTypes.Bike;
import ParkingLot.Version2.VehicleTypes.Car;
import ParkingLot.Version2.VehicleTypes.Truck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Step 1: Setup all parking spots with distances from 2 entrances: A, B
        List<ParkingSpot> spots = new ArrayList<>();

        spots.add(new CompactSpot("S1", Map.of("A", 2, "B", 5)));
        spots.add(new CompactSpot("S2", Map.of("A", 4, "B", 2)));
        spots.add(new BikeSpot("S3", Map.of("A", 1, "B", 6)));
        spots.add(new LargeSpot("S4", Map.of("A", 7, "B", 3)));
        spots.add(new HandicappedSpot("S6", Map.of("A", 2, "B", 2)));

        // Step 2: Init parking lot (Singleton)
        SpotAllocator allocator = new NearestSpotAllocator();
        ParkingLot.init(spots, allocator);

        // Step 3: Create Entrance and Exit gates
        EntranceGate gateA = new EntranceGate("A");
        EntranceGate gateB = new EntranceGate("B");

        PaymentProcessor paymentProcessor = new PaymentProcessor();
        ExitGate exitA = new ExitGate("A", paymentProcessor);
        ExitGate exitB = new ExitGate("B", paymentProcessor);

        // Step 4: Simulate Vehicle Entries
        Vehicle car = new Vehicle("DL-01-1234", new Car());
        Vehicle bike = new Vehicle("DL-02-5678", new Bike());
        Vehicle truck = new Vehicle("DL-03-9999", new Truck());

        Ticket t1 = gateA.enterVehicle(car);   // Enters from A
        Ticket t2 = gateB.enterVehicle(bike);  // Enters from B
        Ticket t3 = gateA.enterVehicle(truck);    // Enters from A

        // Wait to simulate time passage (1 second = 1 hour here for demo)
        Thread.sleep(1000);

        // Step 5: Simulate Exits from different gates with different payment modes
        exitA.exitVehicle(t1.getTicketId(), new CardPayment());
        exitB.exitVehicle(t2.getTicketId(), new CashPayment());
        exitA.exitVehicle(t3.getTicketId(), new UPIPayment());
    }
}
