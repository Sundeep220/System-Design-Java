package SplitWiseExpenseAlgorithm;

/**
 * @Purpose: This class is used as a log to our transactions
 * Like: A paid ₹100 for B
 */
public class ExpenseRecord {
    private User paidBy;
    private User paidFor;
    private double amount;

    public ExpenseRecord(User paidBy, User paidFor, double amount) {
        this.paidBy = paidBy;
        this.paidFor = paidFor;
        this.amount = amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(User paidBy) {
        this.paidBy = paidBy;
    }

    public User getPaidFor() {
        return paidFor;
    }

    public void setPaidFor(User paidFor) {
        this.paidFor = paidFor;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return paidBy.getName() + " paid ₹" + amount + " for " + paidFor.getName();
    }
}
