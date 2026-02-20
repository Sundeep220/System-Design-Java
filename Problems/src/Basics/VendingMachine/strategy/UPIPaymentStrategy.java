package Basics.VendingMachine.strategy;

import Basics.VendingMachine.models.PaymentResult;

import java.util.UUID;

public class UPIPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(double amount) {
        // Simulate success
        return new PaymentResult(true, UUID.randomUUID().toString(), null);
    }
}