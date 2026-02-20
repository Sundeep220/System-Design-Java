package Basics.VendingMachine.strategy;

import Basics.VendingMachine.models.PaymentResult;

public interface PaymentStrategy {
    PaymentResult processPayment(double amount);
}
