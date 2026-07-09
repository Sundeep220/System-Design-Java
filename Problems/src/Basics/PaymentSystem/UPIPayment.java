package Basics.PaymentSystem;

public class UPIPayment implements PaymentStrategy{

    @Override
    public void pay(float amount) {
        System.out.println("Paid ₹" + amount + " using UPI.");
    }
}
