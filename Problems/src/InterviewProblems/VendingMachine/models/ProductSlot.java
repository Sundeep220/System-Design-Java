package InterviewProblems.VendingMachine.models;

public class ProductSlot {

    private final String slotId;
    private final Product product;
    private int quantity;

    public ProductSlot(String slotId, Product product, int quantity) {
        this.slotId = slotId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getSlotId() {
        return slotId;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isAvailable() {
        return quantity > 0;
    }

    public void dispense() {

        if (!isAvailable()) {
            throw new IllegalStateException("Product out of stock");
        }

        quantity--;
    }

    public void restock(int count) {

        if (count <= 0) {
            throw new IllegalArgumentException("Invalid quantity");
        }

        quantity += count;
    }
}