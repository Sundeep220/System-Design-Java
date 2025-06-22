package StructuralPatterns.Facade.prob1;

public class ItineraryService {
    public void printItinerary(String from, String to) {
        System.out.println("\n=== Travel Itinerary ===");
        System.out.println("From: " + from);
        System.out.println("To: " + to);
        System.out.println("Mode: Flight + Hotel + Car Rental");
        System.out.println("Enjoy your trip!");
        System.out.println("========================\n");
    }
}
