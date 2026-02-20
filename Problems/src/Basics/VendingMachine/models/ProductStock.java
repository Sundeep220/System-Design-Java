package Basics.VendingMachine.models;

import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.locks.ReentrantLock;

public class ProductStock {

    private final Product product;
    private int availableQuantity;
    private final ReentrantLock lock = new ReentrantLock();

    public ProductStock(Product product, int quantity) {
        this.product = product;
        this.availableQuantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void reduceWithoutLock(int quantity) {
        this.availableQuantity -= quantity;
    }

    public void restock(int quantity) {
        lock.lock();
        try {
            availableQuantity += quantity;
        } finally {
            lock.unlock();
        }
    }
}