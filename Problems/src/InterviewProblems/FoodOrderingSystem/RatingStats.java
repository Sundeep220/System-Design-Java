package InterviewProblems.FoodOrderingSystem;

public class RatingStats {

    private int totalRating;
    private int ratingCount;

    public void addRating(int rating) {
        totalRating += rating;
        ratingCount++;
    }

    public double getAverageRating() {
        if (ratingCount == 0) {
            return 0.0;
        }

        return (double) totalRating / ratingCount;
    }

    public boolean hasRatings() {
        return ratingCount > 0;
    }
}