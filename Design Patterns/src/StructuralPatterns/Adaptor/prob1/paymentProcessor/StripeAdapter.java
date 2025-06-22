package StructuralPatterns.Adaptor.prob1.paymentProcessor;

import StructuralPatterns.Adaptor.prob1.thirdPartylibs.StripeSDK;

public class StripeAdapter implements PaymentProcessor {
    private final StripeSDK stripeSDK;

    public StripeAdapter(StripeSDK stripeSDK) {
        this.stripeSDK = stripeSDK;
    }

    @Override
    public void pay(double amount) {
        stripeSDK.makePayment((int) (amount * 100));
    }
}
