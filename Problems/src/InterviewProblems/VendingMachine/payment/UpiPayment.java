package InterviewProblems.VendingMachine.payment;

import InterviewProblems.VendingMachine.models.Transaction;

public class UpiPayment implements PaymentStrategy {

    @Override
    public void collectPayment(Transaction transaction) {
        System.out.println("Opening UPI...");
    }

    @Override
    public void cancelPayment(Transaction transaction) {
        System.out.println("Cancelling UPI...");
    }
}