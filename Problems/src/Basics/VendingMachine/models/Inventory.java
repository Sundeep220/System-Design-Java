package Basics.VendingMachine.models;
import java.util.*;

import java.util.*;
import java.util.stream.Collectors;

public class Inventory {

    private final Map<String, ProductStock> stockMap = new HashMap<>();

    public void addProduct(Product product, int quantity) {
        stockMap.put(product.getId(), new ProductStock(product, quantity));
    }

    public void restock(String productId, int quantity) {
        ProductStock stock = stockMap.get(productId);
        if (stock != null) {
            stock.restock(quantity);
        }
    }

    public void removeProduct(String productId) {
        stockMap.remove(productId);
    }

    public Product getProduct(String productId) {
        ProductStock stock = stockMap.get(productId);
        if (stock == null) {
            throw new RuntimeException("Product not found");
        }
        return stock.getProduct();
    }

    // 🔥 Atomic deduction with deadlock prevention
    public boolean deductMultiple(List<CartItem> items) {

        List<String> sortedIds = items.stream()
                .map(CartItem::getProductId)
                .sorted()
                .collect(Collectors.toList());

        List<ProductStock> lockedStocks = new ArrayList<>();

        try {
            // Lock in sorted order
            for (String id : sortedIds) {
                ProductStock stock = stockMap.get(id);
                if (stock == null) return false;

                stock.getLock().lock();
                lockedStocks.add(stock);
            }

            // Phase 1: Check availability
            for (CartItem item : items) {
                ProductStock stock = stockMap.get(item.getProductId());
                if (stock.getAvailableQuantity() < item.getQuantity()) {
                    return false;
                }
            }

            // Phase 2: Deduct
            for (CartItem item : items) {
                ProductStock stock = stockMap.get(item.getProductId());
                stock.reduceWithoutLock(item.getQuantity());
            }

            return true;

        } finally {
            for (ProductStock stock : lockedStocks) {
                stock.getLock().unlock();
            }
        }
    }
}