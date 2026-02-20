package Basics.VendingMachine.models;

import java.util.*;

public class Cart {
    private final List<CartItem> items = new ArrayList<>();

    public void addItem(String productId, int quantity) {
        items.add(new CartItem(productId, quantity));
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double calculateTotal(Inventory inventory) {
        double total = 0;
        for (CartItem item : items) {
            Product product = inventory.getProduct(item.getProductId());
            total += product.getPrice() * item.getQuantity();
        }
        return total;
    }
}