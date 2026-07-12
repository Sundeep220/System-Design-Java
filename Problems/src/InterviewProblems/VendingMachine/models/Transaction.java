package InterviewProblems.VendingMachine.models;

import InterviewProblems.VendingMachine.enums.Denomination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class Transaction {

    private ProductSlot selectedSlot;

    private final List<Denomination> insertedMoney = new ArrayList<>();

    public ProductSlot getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(ProductSlot selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    public void addMoney(Denomination denomination) {
        insertedMoney.add(denomination);
    }

    public List<Denomination> getInsertedMoney() {
        return Collections.unmodifiableList(insertedMoney);
    }

    public int getInsertedAmount() {
        return insertedMoney.stream()
                .mapToInt(Denomination::getValue)
                .sum();
    }

    public void reset() {
        selectedSlot = null;
        insertedMoney.clear();
    }
}