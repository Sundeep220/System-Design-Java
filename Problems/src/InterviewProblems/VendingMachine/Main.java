package InterviewProblems.VendingMachine;

import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.enums.ProductType;
import InterviewProblems.VendingMachine.models.Product;
import InterviewProblems.VendingMachine.models.ProductSlot;
import InterviewProblems.VendingMachine.models.VendingMachine;
import InterviewProblems.VendingMachine.payment.CashPayment;


public class Main {

    public static void main(String[] args) {

        scenarioExactAmount();

        scenarioReturnChange();

        scenarioInsufficientFunds();

        scenarioCancelTransaction();

        scenarioInvalidSlot();

        scenarioOutOfStock();

        scenarioMultiplePurchases();

        scenarioInvalidStateTransition();

        scenarioSelectBeforePayment();

        scenarioInventoryUpdate();
    }

    // ---------------------------------------------------------
    // Helper Method
    // ---------------------------------------------------------

    private static VendingMachine createMachine() {

        Product coke = new Product(
                "P101",
                ProductType.COKE,
                "Coke",
                40
        );

        Product chips = new Product(
                "P102",
                ProductType.CHIPS,
                "Chips",
                25
        );

        Product chocolate = new Product(
                "P103",
                ProductType.CHOCOLATE,
                "Chocolate",
                15
        );

        VendingMachine machine =
                new VendingMachine(new CashPayment());

        machine.getInventory().addSlot(
                new ProductSlot("A1", coke, 5));

        machine.getInventory().addSlot(
                new ProductSlot("A2", chips, 3));

        // Out of Stock Product
        machine.getInventory().addSlot(
                new ProductSlot("A3", chocolate, 0));

        // Machine Cash
        machine.getCashInventory().restock(Denomination.ONE, 20);
        machine.getCashInventory().restock(Denomination.TWO, 20);
        machine.getCashInventory().restock(Denomination.FIVE, 20);
        machine.getCashInventory().restock(Denomination.TEN, 20);
        machine.getCashInventory().restock(Denomination.TWENTY, 20);
        machine.getCashInventory().restock(Denomination.FIFTY, 10);

        return machine;
    }

    // ---------------------------------------------------------
    // Scenario 1
    // ---------------------------------------------------------

    private static void scenarioExactAmount() {

        System.out.println("\n==============================");
        System.out.println("Scenario 1 : Exact Amount");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        machine.insertMoney(Denomination.TWENTY);
        machine.insertMoney(Denomination.TWENTY);

        machine.selectProduct("A1");

        machine.dispense();

        machine.complete();
    }

    // ---------------------------------------------------------
    // Scenario 2
    // ---------------------------------------------------------

    private static void scenarioReturnChange() {

        System.out.println("\n==============================");
        System.out.println("Scenario 2 : Return Change");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        machine.insertMoney(Denomination.TWENTY);
        machine.insertMoney(Denomination.TWENTY);
        machine.insertMoney(Denomination.TEN);

        machine.selectProduct("A1");

        machine.dispense();

        machine.complete();

        // Expected Change : ₹10
    }

    // ---------------------------------------------------------
    // Scenario 3
    // ---------------------------------------------------------

    private static void scenarioInsufficientFunds() {

        System.out.println("\n==============================");
        System.out.println("Scenario 3 : Insufficient Funds");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        try {

            machine.insertMoney(Denomination.TWENTY);

            machine.selectProduct("A1");

            machine.dispense();

        } catch (Exception ex) {

            System.out.println(ex.getMessage());

        }
    }

    // ---------------------------------------------------------
    // Scenario 4
    // ---------------------------------------------------------

    private static void scenarioCancelTransaction() {

        System.out.println("\n==============================");
        System.out.println("Scenario 4 : Cancel Transaction");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        machine.insertMoney(Denomination.TEN);
        machine.insertMoney(Denomination.TWENTY);

        machine.cancel();
    }

    // ---------------------------------------------------------
    // Scenario 5
    // ---------------------------------------------------------

    private static void scenarioInvalidSlot() {

        System.out.println("\n==============================");
        System.out.println("Scenario 5 : Invalid Slot");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        try {

            machine.insertMoney(Denomination.FIFTY);

            machine.selectProduct("Z9");

        } catch (Exception ex) {

            System.out.println(ex.getMessage());

        }
    }

    // ---------------------------------------------------------
    // Scenario 6
    // ---------------------------------------------------------

    private static void scenarioOutOfStock() {

        System.out.println("\n==============================");
        System.out.println("Scenario 6 : Out Of Stock");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        try {

            machine.insertMoney(Denomination.TWENTY);

            machine.selectProduct("A3");

        } catch (Exception ex) {

            System.out.println(ex.getMessage());

        }
    }

    // ---------------------------------------------------------
    // Scenario 7
    // ---------------------------------------------------------

    private static void scenarioMultiplePurchases() {

        System.out.println("\n==============================");
        System.out.println("Scenario 7 : Multiple Purchases");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        // Purchase 1

        machine.insertMoney(Denomination.TWENTY);
        machine.insertMoney(Denomination.TWENTY);

        machine.selectProduct("A1");

        machine.dispense();

        machine.complete();

        // Purchase 2

        machine.insertMoney(Denomination.TWENTY);
        machine.insertMoney(Denomination.FIVE);

        machine.selectProduct("A2");

        machine.dispense();

        machine.complete();
    }

    // ---------------------------------------------------------
    // Scenario 8
    // ---------------------------------------------------------

    private static void scenarioInvalidStateTransition() {

        System.out.println("\n==============================");
        System.out.println("Scenario 8 : Invalid State Transition");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        try {

            machine.dispense();

        } catch (Exception ex) {

            System.out.println(ex.getMessage());

        }
    }

    // ---------------------------------------------------------
    // Scenario 9
    // ---------------------------------------------------------

    private static void scenarioSelectBeforePayment() {

        System.out.println("\n==============================");
        System.out.println("Scenario 9 : Select Product Before Payment");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        try {

            machine.selectProduct("A1");

        } catch (Exception ex) {

            System.out.println(ex.getMessage());

        }
    }

    // ---------------------------------------------------------
    // Scenario 10
    // ---------------------------------------------------------

    private static void scenarioInventoryUpdate() {

        System.out.println("\n==============================");
        System.out.println("Scenario 10 : Inventory Update");
        System.out.println("==============================");

        VendingMachine machine = createMachine();

        System.out.println("\nInventory Before Purchase\n");

        machine.getInventory().displayProducts();

        machine.insertMoney(Denomination.TWENTY);
        machine.insertMoney(Denomination.TWENTY);

        machine.selectProduct("A1");

        machine.dispense();

        machine.complete();

        System.out.println("\nInventory After Purchase\n");

        machine.getInventory().displayProducts();
    }
}