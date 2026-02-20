package Basics.VendingMachine.service;

import Basics.VendingMachine.models.PaymentResult;
import Basics.VendingMachine.strategy.PaymentStrategy;

public class PaymentService {

    public PaymentResult processPayment(double amount, PaymentStrategy strategy) {
        return strategy.processPayment(amount);
    }
}