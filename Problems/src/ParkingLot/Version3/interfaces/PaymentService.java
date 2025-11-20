package ParkingLot.Version3.interfaces;

public interface PaymentService {
    void acceptCash(int amount);
    void acceptCard(String cardNumber, String cvv, int amount);
}
