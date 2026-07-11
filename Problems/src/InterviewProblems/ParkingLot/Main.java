package InterviewProblems.ParkingLot;

import InterviewProblems.ParkingLot.enums.StrategyType;
import InterviewProblems.ParkingLot.enums.VehicleType;
import InterviewProblems.ParkingLot.models.Ticket;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        ParkingLot parkingLot = ParkingLot.getInstance();

        List<int[][]> layouts = new ArrayList<>();

        layouts.add(new int[][]{
                {2, 2, 4},
                {4, 4, 0}
        });

        layouts.add(new int[][]{
                {2, 4, 4},
                {2, 2, 4}
        });

        parkingLot.init(layouts);

        //-----------------------------------------
        // Park Bike
        //-----------------------------------------

        Ticket t1 = parkingLot.park(
                VehicleType.TWO_WHEELER,
                "KA01AA1111",
                "T1",
                StrategyType.FIRST_AVAILABLE
        );

        System.out.println("Bike parked at : "
                + t1.getParkingSpot().getSpotId());

        //-----------------------------------------
        // Park Car
        //-----------------------------------------

        Ticket t2 = parkingLot.park(
                VehicleType.FOUR_WHEELER,
                "KA01BB2222",
                "T2",
                StrategyType.FIRST_AVAILABLE
        );

        System.out.println("Car parked at : "
                + t2.getParkingSpot().getSpotId());

        //-----------------------------------------
        // Park another Bike
        //-----------------------------------------

        Ticket t3 = parkingLot.park(
                VehicleType.TWO_WHEELER,
                "KA01CC3333",
                "T3",
                StrategyType.MAX_FREE_FLOOR
        );

        System.out.println("Bike parked at : "
                + t3.getParkingSpot().getSpotId());

        //-----------------------------------------
        // Search by Vehicle Number
        //-----------------------------------------

        Ticket ticket =
                parkingLot.searchVehicle("KA01AA1111");

        if (ticket != null) {
            System.out.println(
                    "Found Vehicle at : "
                            + ticket.getParkingSpot().getSpotId());
        }

        //-----------------------------------------
        // Search by Ticket Id
        //-----------------------------------------

        Ticket ticket2 =
                parkingLot.searchVehicle("T2");

        if (ticket2 != null) {
            System.out.println(
                    "Found Ticket at : "
                            + ticket2.getParkingSpot().getSpotId());
        }

        //-----------------------------------------
        // Free Spot Count
        //-----------------------------------------

        System.out.println(
                "Floor 0 Bike Spots : "
                        + parkingLot.getFreeSpotsCount(
                        0,
                        VehicleType.TWO_WHEELER));

        System.out.println(
                "Floor 0 Car Spots : "
                        + parkingLot.getFreeSpotsCount(
                        0,
                        VehicleType.FOUR_WHEELER));

        //-----------------------------------------
        // Remove Vehicle
        //-----------------------------------------

        parkingLot.removeVehicle(
                t1.getParkingSpot().getSpotId());

        System.out.println(
                "Vehicle Removed.");

        //-----------------------------------------
        // Search Again
        //-----------------------------------------

        Ticket removed =
                parkingLot.searchVehicle("KA01AA1111");

        System.out.println(
                removed == null
                        ? "Vehicle Not Found"
                        : "Still Exists");

        //-----------------------------------------
        // Park Again
        //-----------------------------------------

        Ticket t4 = parkingLot.park(
                VehicleType.TWO_WHEELER,
                "KA01DD4444",
                "T4",
                StrategyType.FIRST_AVAILABLE
        );

        System.out.println(
                "New Bike Parked at : "
                        + t4.getParkingSpot().getSpotId());

        //-----------------------------------------
        // Final Free Counts
        //-----------------------------------------

        System.out.println(
                "Floor0 Bike Free : "
                        + parkingLot.getFreeSpotsCount(
                        0,
                        VehicleType.TWO_WHEELER));

        System.out.println(
                "Floor1 Bike Free : "
                        + parkingLot.getFreeSpotsCount(
                        1,
                        VehicleType.TWO_WHEELER));
    }
}
