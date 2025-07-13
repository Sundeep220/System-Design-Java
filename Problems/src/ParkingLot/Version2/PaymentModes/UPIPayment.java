package ParkingLot.Version2.PaymentModes;

import ParkingLot.Version2.PaymentService;

public class UPIPayment implements PaymentService {
    public void pay(double amount) {
        System.out.println("📱 Paid ₹" + amount + " via UPI");
    }
}
