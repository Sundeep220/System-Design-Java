package StructuralPatterns.Adaptor.prob1.thirdPartylibs;

public class PayPal {
    public void authenticate() {
        System.out.println("Authenticated with PayPal");
    }

    public void sendPayment(double amount) {
        System.out.println("Paid $" + amount + " using PayPal");
    }
}