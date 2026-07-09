package Basics.PaymentSystem;

public class Main {
    public static void main(String[] args) {
        PaymentStrategy upi = new UPIPayment();
        PaymentStrategy creditCard = new CreditCardPayment();

        upi.pay(100);
        creditCard.pay(200);
    }
}
