package StructuralPatterns.Adaptor.prob1.paymentProcessor;

public class CreditCardProcessor implements PaymentProcessor {
    @Override
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " using Credit Card");
    }


}
