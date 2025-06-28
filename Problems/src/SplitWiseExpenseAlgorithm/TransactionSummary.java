package SplitWiseExpenseAlgorithm;

/**
 * @Purpose: This class is used to store the transaction summary our Final Output
 * Like: A pays B: ₹100
 */
public class TransactionSummary {
    private User from;
    private User to;
    private double amount;

    public TransactionSummary(User from, User to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public User getFrom() {
        return from;
    }

    public void setFrom(User from) {
        this.from = from;
    }

    public User getTo() {
        return to;
    }

    public void setTo(User to) {
        this.to = to;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return from.getName() + " pays " + to.getName() + ": ₹" + amount;
    }
}

