package InterviewProblems.VendingMachine.payment;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.Transaction;

public class CashPayment implements PaymentStrategy {

    public void insertMoney(Transaction transaction, Denomination denomination) {
        transaction.addMoney(denomination);
    }

    @Override
    public void collectPayment(Transaction transaction) {
        System.out.println("Collected ₹" + transaction.getInsertedAmount());
    }

    @Override
    public void cancelPayment(Transaction transaction) {
        System.out.println("Refunding: " + transaction.getInsertedMoney());
        transaction.reset();
    }
}