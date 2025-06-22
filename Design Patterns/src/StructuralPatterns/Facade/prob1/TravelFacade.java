package StructuralPatterns.Facade.prob1;

public class TravelFacade {
    private final FlightService flightService;
    private final HotelService hotelService;
    private final CarRentalService carService;
    private final PaymentService paymentService;
    private final ItineraryService itineraryService;

    private double totalCost = 0;
    private String from;
    private String to;

    public TravelFacade() {
        this.flightService = new FlightService();
        this.hotelService = new HotelService();
        this.carService = new CarRentalService();
        this.paymentService = new PaymentService();
        this.itineraryService = new ItineraryService();
    }

    public void bookTrip(String from, String to) {
        this.from = from;
        this.to = to;
        System.out.println("\nBooking complete travel package...");
        flightService.bookFlight(from, to);
        hotelService.bookHotel(to);
        carService.bookCar(to);

        totalCost = 10000 + 5000 + 3000; // Simulated total
        paymentService.makePayment(totalCost);

        itineraryService.printItinerary(from, to);
        System.out.println("Trip booking successful!\n");
    }

    public void cancelTrip() {
        System.out.println("Cancelling entire trip...");
        carService.cancelCar();
        hotelService.cancelHotel();
        flightService.cancelFlight();

        paymentService.refundPayment(totalCost);
        System.out.println("Trip cancellation successful.\n");
    }
}
