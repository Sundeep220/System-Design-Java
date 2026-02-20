package Basics.VendingMachine;

import Basics.VendingMachine.models.Cart;
import Basics.VendingMachine.models.Inventory;
import Basics.VendingMachine.models.Product;
import Basics.VendingMachine.models.Transaction;
import Basics.VendingMachine.service.PaymentService;
import Basics.VendingMachine.strategy.PaymentStrategy;
import Basics.VendingMachine.strategy.UPIPaymentStrategy;

public class Main {

    public static void main(String[] args) {

        VendingMachine machine = VendingMachine.getInstance();

        Inventory inventory = machine.getInventory();

        Product coke = new Product("P1", "Coke", 40);
        Product chips = new Product("P2", "Chips", 30);

        inventory.addProduct(coke, 5);
        inventory.addProduct(chips, 3);

        Cart cart = new Cart();
        cart.addItem("P1", 2);
        cart.addItem("P2", 1);

        PaymentStrategy strategy = new UPIPaymentStrategy();

        Transaction txn = machine.checkout(cart, strategy);

        System.out.println("Transaction Status: " + txn.getStatus());
    }
}