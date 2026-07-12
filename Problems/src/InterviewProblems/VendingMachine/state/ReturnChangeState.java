package InterviewProblems.VendingMachine.state;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.ProductSlot;
import InterviewProblems.VendingMachine.models.VendingMachine;

import java.util.List;
import java.util.stream.Collectors;

public class ReturnChangeState implements State {

    @Override
    public void insertMoney(VendingMachine machine, Denomination denomination) {
        throw new IllegalStateException("Wait...");
    }

    @Override
    public void selectProduct(VendingMachine machine, String slotId) {
        throw new IllegalStateException("Transaction finishing.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        throw new IllegalStateException("Already dispensed.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        machine.getTransaction().reset();
        machine.setState(machine.getIdleState());
        System.out.println("Ready for next customer.");
    }

    @Override
    public void complete(VendingMachine machine) {
        ProductSlot slot = machine.getTransaction().getSelectedSlot();

        int insertedAmount = machine.getTransaction().getInsertedAmount();

        int price = (int) slot.getProduct().price();

        int changeAmount = insertedAmount - price;

        if (changeAmount > 0) {

            List<Denomination> change = machine.getChangeCalculator().calculateChange(machine.getCashInventory(), changeAmount);
            int totalChange = change.stream().mapToInt(Denomination::getValue).sum();
            String denominationCounts = change.stream()
                    .collect(Collectors.groupingBy(d -> d, Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));

            System.out.println("Total change returned = " + totalChange + ", with denominations: " + denominationCounts);
            // Remove dispensed change from cash inventory
            for (Denomination denomination : change) {
                machine.getCashInventory().removeMoney(denomination);
            }
        }

        machine.getTransaction().reset();
        machine.setState(machine.getIdleState());
        System.out.println("Transaction completed.");
    }
}