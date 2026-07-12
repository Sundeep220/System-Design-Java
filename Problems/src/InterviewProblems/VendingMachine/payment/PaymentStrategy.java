package InterviewProblems.VendingMachine.payment;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.Transaction;

public interface PaymentStrategy {

    default void insertMoney(Transaction transaction, Denomination denomination) {

        throw new UnsupportedOperationException(
                "Payment type doesn't support cash."
        );
    }

    void collectPayment(Transaction transaction);

    void cancelPayment(Transaction transaction);
}