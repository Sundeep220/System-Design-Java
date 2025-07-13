package ParkingLot.Version1;

/**
 * @Purpose: This class represents a payment service in a parking lot.
 */
public class PaymentService {
    public void pay(double amount, String method) {
        System.out.println("Paid ₹" + amount + " via " + method);
    }
}
