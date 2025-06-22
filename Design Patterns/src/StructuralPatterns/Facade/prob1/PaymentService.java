package StructuralPatterns.Facade.prob1;

public class PaymentService {
    public void makePayment(double amount) {
        System.out.println("Payment of ₹" + amount + " completed.");
    }

    public void refundPayment(double amount) {
        System.out.println("Refund of ₹" + amount + " initiated.");
    }
}

