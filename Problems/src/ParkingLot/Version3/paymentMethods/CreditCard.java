package ParkingLot.Version3.paymentMethods;

public class CreditCard extends PaymentMethod {
    private String cardNumber;
    private String cvv;

    public CreditCard(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    @Override
    public boolean initiatePayment(int amount) {
        // Process credit card payment logic here
        System.out.println("Credit card payment of $" + amount + " initiated.");
        return true; // Payment successful
    }
}
