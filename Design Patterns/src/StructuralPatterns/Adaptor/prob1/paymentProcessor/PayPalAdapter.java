package StructuralPatterns.Adaptor.prob1.paymentProcessor;

import StructuralPatterns.Adaptor.prob1.thirdPartylibs.PayPal;

public class PayPalAdapter implements PaymentProcessor {
    private final PayPal api;

    public PayPalAdapter(PayPal api) {
        this.api = api;
    }

    @Override
    public void pay(double amount) {
        api.authenticate();
        api.sendPayment(amount);
    }

}
