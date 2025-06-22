package StructuralPatterns.Adaptor.prob1;

import StructuralPatterns.Adaptor.prob1.paymentProcessor.CreditCardProcessor;
import StructuralPatterns.Adaptor.prob1.paymentProcessor.PayPalAdapter;
import StructuralPatterns.Adaptor.prob1.paymentProcessor.PaymentProcessor;
import StructuralPatterns.Adaptor.prob1.paymentProcessor.StripeAdapter;
import StructuralPatterns.Adaptor.prob1.thirdPartylibs.PayPal;
import StructuralPatterns.Adaptor.prob1.thirdPartylibs.StripeSDK;

public class Main {
    public static void main(String[] args) {
        // 1. Credit Card
        PaymentProcessor creditCard = new CreditCardProcessor();
        PaymentService creditCardService = new PaymentService(creditCard);
        creditCardService.processPayment(150.0);

        // 2. PayPal
        PayPal paypalAPI = new PayPal();
        PaymentProcessor paypal = new PayPalAdapter(paypalAPI);
        PaymentService paypalService = new PaymentService(paypal);
        paypalService.processPayment(200.0);

        // 3. Stripe
        StripeSDK stripeSDK = new StripeSDK();
        PaymentProcessor stripe = new StripeAdapter(stripeSDK);
        PaymentService stripeService = new PaymentService(stripe);
        stripeService.processPayment(99.99);
    }
}
