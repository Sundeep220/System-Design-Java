package SplitWiseExpenseAlgorithm;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        // Create Users
        User alice = new User("U1", "Alice");
        User bob = new User("U2", "Bob");
        User charlie = new User("U3", "Charlie");

        // Expense Records (who paid for whom)
        List<ExpenseRecord> expenses = List.of(
                new ExpenseRecord(alice, bob, 50),       // Alice paid ₹50 for Bob
                new ExpenseRecord(bob, charlie, 20),     // Bob paid ₹20 for Charlie
                new ExpenseRecord(charlie, alice, 30)    // Charlie paid ₹30 for Alice
        );

        // Simplify Debts
        DebtSimplifier simplifier = new DebtSimplifier();
        List<TransactionSummary> transactions = simplifier.simplify(expenses);

        // Output result
        for (TransactionSummary t : transactions) {
            System.out.println(t);
        }
    }
}
