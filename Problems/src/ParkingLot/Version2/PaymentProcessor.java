package ParkingLot.Version2;

public class PaymentProcessor {
    public void processPayment(double amount, PaymentService paymentService) {
        paymentService.pay(amount);
    }
}