package StructuralPatterns.Facade.prob1;

public class HotelService {
    public void bookHotel(String location) {
        System.out.println("Hotel booked at " + location);
    }

    public void cancelHotel() {
        System.out.println("Hotel booking cancelled");
    }
}
