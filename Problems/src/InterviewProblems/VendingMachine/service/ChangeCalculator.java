package InterviewProblems.VendingMachine.service;



import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.CashInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChangeCalculator {

    public List<Denomination> calculateChange(
            CashInventory cashInventory,
            int changeAmount) {

        List<Denomination> change = new ArrayList<>();

        if (changeAmount == 0) {
            return change;
        }

        // Work on a copy so we don't modify the inventory
        int remaining = changeAmount;

        List<Denomination> denominations =
                new ArrayList<>(List.of(Denomination.values()));

        denominations.sort(
                Comparator.comparingInt(Denomination::getValue)
                        .reversed());

        for (Denomination denomination : denominations) {

            int available =
                    cashInventory.getCount(denomination);

            while (available > 0 &&
                    remaining >= denomination.getValue()) {

                change.add(denomination);

                remaining -= denomination.getValue();

                available--;
            }
        }

        if (remaining != 0) {

            throw new IllegalStateException(
                    "Unable to return exact change."
            );
        }

        return change;
    }
}