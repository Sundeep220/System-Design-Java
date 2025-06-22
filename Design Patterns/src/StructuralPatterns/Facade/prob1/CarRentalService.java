package StructuralPatterns.Facade.prob1;

public class CarRentalService {
    public void bookCar(String location) {
        System.out.println("Car booked at " + location);
    }

    public void cancelCar() {
        System.out.println("Car booking cancelled");
    }
}

