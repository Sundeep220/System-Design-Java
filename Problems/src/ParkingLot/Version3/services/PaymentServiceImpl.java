package ParkingLot.Version3.services;

import ParkingLot.Version3.interfaces.PaymentService;
import ParkingLot.Version3.paymentMethods.Cash;
import ParkingLot.Version3.paymentMethods.CreditCard;
import ParkingLot.Version3.paymentMethods.PaymentMethod;

public class PaymentServiceImpl implements PaymentService {

    @Override
    public void acceptCash(int amount) {
        PaymentMethod cash = new Cash();
        cash.initiatePayment(amount);
    }

    @Override
    public void acceptCard(String cardNumber, String cvv, int amount) {
        PaymentMethod creditCard = new CreditCard(cardNumber, cvv);
        creditCard.initiatePayment(amount);
    }
}
