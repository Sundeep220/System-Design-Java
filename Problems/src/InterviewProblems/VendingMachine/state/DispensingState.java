package InterviewProblems.VendingMachine.state;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.ProductSlot;
import InterviewProblems.VendingMachine.models.VendingMachine;

public class DispensingState implements State {

    @Override
    public void insertMoney(VendingMachine machine, Denomination denomination) {
        throw new IllegalStateException("Already dispensing.");
    }

    @Override
    public void selectProduct(VendingMachine machine, String slotId) {
        throw new IllegalStateException("Already selected.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        throw new IllegalStateException("Cannot cancel now.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        ProductSlot slot = machine.getTransaction().getSelectedSlot();

        double inserted = machine.getTransaction().getInsertedAmount();
        double price = slot.getProduct().price();
        if(inserted < price){
            throw new IllegalStateException("Insufficient money.");
        }
        // Commit payment
        machine.getPaymentStrategy().collectPayment(machine.getTransaction());
        // Dispense product
        slot.dispense();
        double change = inserted - price;
        System.out.println("Dispensed : " + slot.getProduct().name());
        System.out.println("Change : ₹" + change);
        machine.setState(machine.getReturnChangeState());
    }

    @Override
    public void complete(VendingMachine machine) {
        throw new IllegalStateException(
                "Dispensing still in progress."
        );
    }
}