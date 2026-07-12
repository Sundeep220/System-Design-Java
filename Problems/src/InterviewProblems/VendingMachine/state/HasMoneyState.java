package InterviewProblems.VendingMachine.state;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.ProductSlot;
import InterviewProblems.VendingMachine.models.VendingMachine;

public class HasMoneyState implements State {

    @Override
    public void insertMoney(VendingMachine machine, Denomination denomination) {
        machine.getPaymentStrategy().insertMoney(machine.getTransaction(), denomination);
        System.out.println(denomination + " inserted.");
    }

    @Override
    public void selectProduct(VendingMachine machine, String slotId) {
        ProductSlot slot = machine.getInventory().getSlot(slotId);
        if(slot == null){
            throw new IllegalArgumentException("Invalid slot.");
        }
        if(!slot.isAvailable()){
            throw new IllegalStateException("Out of stock.");
        }
        machine.getTransaction().setSelectedSlot(slot);
        machine.setState(machine.getDispensingState());
    }

    @Override
    public void dispense(VendingMachine machine) {
        throw new IllegalStateException("Select product first.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        machine.getPaymentStrategy().cancelPayment(machine.getTransaction());
        machine.setState(machine.getIdleState());
    }

    @Override
    public void complete(VendingMachine machine) {
        throw new IllegalStateException("Transaction not completed yet.");
    }
}
