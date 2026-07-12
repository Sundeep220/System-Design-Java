package InterviewProblems.VendingMachine.models;


import InterviewProblems.VendingMachine.enums.Denomination;

import java.util.EnumMap;
import java.util.Map;

public class CashInventory {

    private final Map<Denomination, Integer> cash;

    public CashInventory() {
        cash = new EnumMap<>(Denomination.class);

        for (Denomination denomination : Denomination.values()) {
            cash.put(denomination, 0);
        }
    }

    public void addMoney(Denomination denomination) {
        cash.put(denomination, cash.get(denomination) + 1);
    }

    public void removeMoney(Denomination denomination) {

        int count = getCount(denomination);

        if (count == 0) {
            throw new IllegalStateException("Denomination unavailable");
        }

        cash.put(denomination, count - 1);
    }

    public int getCount(Denomination denomination) {
        return cash.get(denomination);
    }

    public void restock(Denomination denomination, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity");
        }

        cash.put(
                denomination,
                cash.get(denomination) + quantity
        );
    }

    public Map<Denomination, Integer> getCash() {
        return Map.copyOf(cash);
    }
}