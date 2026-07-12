package InterviewProblems.VendingMachine.models;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.payment.PaymentStrategy;
import InterviewProblems.VendingMachine.service.ChangeCalculator;
import InterviewProblems.VendingMachine.state.*;

public class VendingMachine {

    private final Inventory inventory;
    private final CashInventory cashInventory;
    private final Transaction transaction;
    private final ChangeCalculator changeCalculator;

    private PaymentStrategy paymentStrategy;

    private State currentState;

    // Reusable State Objects
    private final State idleState;
    private final State hasMoneyState;
    private final State dispensingState;
    private final State returnChangeState;

    public VendingMachine(PaymentStrategy paymentStrategy) {

        this.inventory = new Inventory();
        this.cashInventory = new CashInventory();
        this.transaction = new Transaction();
        this.changeCalculator = new ChangeCalculator();
        this.paymentStrategy = paymentStrategy;

        this.idleState = new IdleState();
        this.hasMoneyState = new HasMoneyState();
        this.dispensingState = new DispensingState();
        this.returnChangeState = new ReturnChangeState();

        this.currentState = idleState;
    }

    // ---------------- Public APIs ----------------

    public void insertMoney(Denomination denomination) {
        currentState.insertMoney(this, denomination);
    }

    public void selectProduct(String slotId) {
        currentState.selectProduct(this, slotId);
    }

    public void dispense() {
        currentState.dispense(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }

    public void complete() {
        currentState.complete(this);
    }

    // ---------------- Getters ----------------

    public Inventory getInventory() {
        return inventory;
    }

    public CashInventory getCashInventory() {
        return cashInventory;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public PaymentStrategy getPaymentStrategy() {
        return paymentStrategy;
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public ChangeCalculator getChangeCalculator() {
        return changeCalculator;
    }

    // ---------------- State Management ----------------

    public State getCurrentState() {
        return currentState;
    }

    public void setState(State currentState) {
        this.currentState = currentState;
    }

    public State getIdleState() {
        return idleState;
    }

    public State getHasMoneyState() {
        return hasMoneyState;
    }

    public State getDispensingState() {
        return dispensingState;
    }

    public State getReturnChangeState() {
        return returnChangeState;
    }


}
