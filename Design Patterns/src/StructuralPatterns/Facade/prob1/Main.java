package StructuralPatterns.Facade.prob1;

public class Main {
    public static void main(String[] args) {
        TravelFacade travelFacade = new TravelFacade();

        travelFacade.bookTrip("Delhi", "Goa");
        travelFacade.cancelTrip();
    }
}
