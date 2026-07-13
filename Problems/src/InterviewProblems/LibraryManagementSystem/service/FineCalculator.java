package InterviewProblems.LibraryManagementSystem.service;


public final class FineCalculator {
    public static int BORROW_DURATION = 14;
    public static int FINE_PER_DAY = 20;

    private FineCalculator() {
        // Prevent instantiation
    }


    public static int calculateFine(int dueDay, int returnDay){
        int lateDays = Math.max(0, returnDay - dueDay);
        return lateDays * FINE_PER_DAY;
    }
}
