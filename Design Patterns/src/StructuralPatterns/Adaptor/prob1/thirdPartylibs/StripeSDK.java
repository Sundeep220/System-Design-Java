package StructuralPatterns.Adaptor.prob1.thirdPartylibs;

public class StripeSDK {
    public void makePayment(int cents) {
        System.out.println("Paid $" + cents / 100.0 + " using Stripe");
    }
}