package ParkingLot.Version3.paymentMethods;

public class Cash extends PaymentMethod {
    @Override
    public boolean initiatePayment(int amount) {
        System.out.println("💵 Paid ₹" + amount + " via Cash");
        return true;
    }
}
