package Basics.PaymentSystem;

public class CreditCardPayment implements PaymentStrategy{

    @Override
    public void pay(float amount){
        System.out.println("Paid ₹" + amount + " using Credit Card.");
    }
}
