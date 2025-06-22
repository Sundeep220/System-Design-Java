package StructuralPatterns.Adaptor.prob1;

import StructuralPatterns.Adaptor.prob1.paymentProcessor.PaymentProcessor;

public class PaymentService {
    private final PaymentProcessor processor;

    public PaymentService(PaymentProcessor processor) {
        this.processor = processor;
    }

    public void processPayment(double amount) {
        processor.pay(amount);
    }
}
