package Basics.VendingMachine;

import Basics.VendingMachine.enums.TransactionStatus;
import Basics.VendingMachine.models.*;
import Basics.VendingMachine.service.PaymentService;
import Basics.VendingMachine.strategy.PaymentStrategy;

public class VendingMachine {

    private final Inventory inventory;
    private final PaymentService paymentService;

    // Private constructor
    private VendingMachine() {
        this.inventory = new Inventory();
        this.paymentService = new PaymentService();
    }

    // Static inner helper class
    private static class Holder {
        private static final VendingMachine INSTANCE = new VendingMachine();
    }

    // Global access point
    public static VendingMachine getInstance() {
        return Holder.INSTANCE;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Transaction checkout(Cart cart, PaymentStrategy strategy) {

        double totalAmount = cart.calculateTotal(inventory);
        Transaction transaction = new Transaction(cart.getItems(), totalAmount);

        PaymentResult paymentResult =
                paymentService.processPayment(totalAmount, strategy);

        if (!paymentResult.isSuccess()) {
            transaction.setStatus(TransactionStatus.PAYMENT_FAILED);
            return transaction;
        }

        transaction.setStatus(TransactionStatus.PAYMENT_SUCCESS);

        boolean deducted = inventory.deductMultiple(cart.getItems());

        if (!deducted) {
            transaction.setStatus(TransactionStatus.FAILED);
            return transaction;
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        dispense(cart);

        return transaction;
    }

    private void dispense(Cart cart) {
        for (CartItem item : cart.getItems()) {
            System.out.println("Dispensed Product ID: "
                    + item.getProductId()
                    + " Quantity: " + item.getQuantity());
        }
    }
}