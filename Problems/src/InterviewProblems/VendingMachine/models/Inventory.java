package InterviewProblems.VendingMachine.models;
import java.util.HashMap;
import java.util.Map;

public class Inventory {

    private final Map<String, ProductSlot> slots;

    public Inventory() {
        this.slots = new HashMap<>();
    }

    public void addSlot(ProductSlot slot) {
        slots.put(slot.getSlotId(), slot);
    }

    public ProductSlot getSlot(String slotId) {
        return slots.get(slotId);
    }

    public boolean containsSlot(String slotId) {
        return slots.containsKey(slotId);
    }

    public void restock(String slotId, int quantity) {

        ProductSlot slot = getSlot(slotId);

        if (slot == null) {
            throw new IllegalArgumentException("Invalid Slot");
        }

        slot.restock(quantity);
    }

    public void displayProducts() {

        System.out.println("------------------------------");

        for (ProductSlot slot : slots.values()) {

            System.out.printf(
                    "%s | %s | ₹%.2f | Qty: %d%n",
                    slot.getSlotId(),
                    slot.getProduct().name(),
                    slot.getProduct().price(),
                    slot.getQuantity()
            );
        }

        System.out.println("------------------------------");
    }
}