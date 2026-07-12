package InterviewProblems.VendingMachine.state;



import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.VendingMachine;

public class IdleState implements State {

    @Override
    public void insertMoney(VendingMachine machine, Denomination denomination) {
        machine.getPaymentStrategy().insertMoney(machine.getTransaction(), denomination);
        System.out.println(denomination + " inserted.");
        machine.setState(machine.getHasMoneyState());
    }

    @Override
    public void selectProduct(VendingMachine machine, String slotId) {
        throw new IllegalStateException("Insert money first.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        throw new IllegalStateException("No transaction started.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Nothing to cancel.");
    }

    @Override
    public void complete(VendingMachine machine) {
        throw new IllegalStateException("Nothing to complete.");
    }
}
