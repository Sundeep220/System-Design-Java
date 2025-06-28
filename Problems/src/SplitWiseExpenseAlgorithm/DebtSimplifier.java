package SplitWiseExpenseAlgorithm;
import java.util.*;

/**
 * This class contains the logic to simplify group debts (Splitwise-style),
 * minimizing the number of transactions required to settle all balances.
 */
public class DebtSimplifier {

    /**
     * Simplifies the debts between users into the minimal number of transactions.
     *
     * @param expenses List of ExpenseRecord representing who paid for whom and how much.
     * @return A list of TransactionSummary showing minimal transactions to settle debts.
     */
    public List<TransactionSummary> simplify(List<ExpenseRecord> expenses) {
        // Step 1: Calculate net balances for each user
        // A positive balance means the user is a creditor (owed money)
        // A negative balance means the user is a debtor (owes money)
        Map<User, Double> netBalances = new HashMap<>();

        for (ExpenseRecord e : expenses) {
            netBalances.put(e.getPaidBy(), netBalances.getOrDefault(e.getPaidBy(), 0.0) + e.getAmount());
            netBalances.put(e.getPaidFor(), netBalances.getOrDefault(e.getPaidFor(), 0.0) - e.getAmount());
        }

        // Step 2: Prepare two priority queues
        // One min-heap for debtors (the lowest balance first, most negative)
        // One max-heap for creditors (the highest balance first, most positive)
        Comparator<Map.Entry<User, Double>> debtorComparator = Comparator.comparingDouble(Map.Entry::getValue);
        Comparator<Map.Entry<User, Double>> creditorComparator = (a, b) -> Double.compare(b.getValue(), a.getValue());

        PriorityQueue<Map.Entry<User, Double>> debtors = new PriorityQueue<>(debtorComparator);
        PriorityQueue<Map.Entry<User, Double>> creditors = new PriorityQueue<>(creditorComparator);

        // Step 3: Populate queues based on balances
        for (Map.Entry<User, Double> entry : netBalances.entrySet()) {
            if (entry.getValue() < -0.01) {  // using 0.01 to avoid floating point errors we can use 0 as well
                debtors.add(entry);     // owes money
            } else if (entry.getValue() > 0.01) {  // using 0.01 to avoid floating point errors we can use 0 as well
                creditors.add(entry);   // is owed money
            }
        }

        // Final result list of transactions
        List<TransactionSummary> result = new ArrayList<>();

        // Step 4: Match debtors and creditors greedily
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Map.Entry<User, Double> debtor = debtors.poll();     // lowest balance
            Map.Entry<User, Double> creditor = creditors.poll(); // highest balance

            // The max amount we can settle in this transaction
            double settledAmount = Math.min(-debtor.getValue(), creditor.getValue()); // using -debtor.getValue() as in our above Step 1, we have a negative balance for debtor

            // Add this transaction to the result
            result.add(new TransactionSummary(debtor.getKey(), creditor.getKey(), settledAmount));

            // Update balances after this settlement
            double updatedDebtor = debtor.getValue() + settledAmount;
            double updatedCreditor = creditor.getValue() - settledAmount;

            // If any balance remains, put back into their respective queues
            if (updatedDebtor < -0.01) {
                debtors.add(new AbstractMap.SimpleEntry<>(debtor.getKey(), updatedDebtor));  // AbstractMap.SimpleEntry is a Map.Entry implementation, as Map.Entry is an interface and AbstractMap.SimpleEntry is a concrete class
            }
            if (updatedCreditor > 0.01) {
                creditors.add(new AbstractMap.SimpleEntry<>(creditor.getKey(), updatedCreditor));
            }
        }

        // Return the list of minimized transactions
        return result;
    }
}
