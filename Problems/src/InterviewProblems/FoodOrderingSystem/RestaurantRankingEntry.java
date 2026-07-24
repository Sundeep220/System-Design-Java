package InterviewProblems.FoodOrderingSystem;

/**
 * Immutable snapshot of a restaurant's ranking state.
 *
 * This object should never be mutated after being inserted
 * into a TreeSet.
 */
class RestaurantRankingEntry implements Comparable<RestaurantRankingEntry> {

    private final String restaurantId;
    private final boolean rated;
    private final double averageRating;


    public RestaurantRankingEntry(
            String restaurantId,
            boolean rated,
            double averageRating
    ) {
        this.restaurantId = restaurantId;
        this.rated = rated;
        this.averageRating = averageRating;
    }


    public String getRestaurantId() {
        return restaurantId;
    }


    public boolean isRated() {
        return rated;
    }


    public double getAverageRating() {
        return averageRating;
    }


    @Override
    public int compareTo(RestaurantRankingEntry other) {

        /*
         * Rule 1:
         *
         * Rated restaurants come before
         * unrated restaurants.
         */
        if (this.rated != other.rated) {
            return this.rated ? -1 : 1;
        }


        /*
         * Rule 2:
         *
         * Higher rating comes first.
         *
         * Example:
         *
         * this = 4.5
         * other = 5.0
         *
         * Double.compare(5.0, 4.5) -> positive
         *
         * Therefore this comes after other.
         */
        int ratingComparison = Double.compare(other.averageRating, this.averageRating);
        if (ratingComparison != 0) {
            return ratingComparison;
        }


        /*
         * Rule 3:
         *
         * Lexicographically smaller restaurant ID
         * comes first.
         */
        return this.restaurantId.compareTo(other.restaurantId);
    }
}