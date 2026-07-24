package InterviewProblems.FoodOrderingSystem;



import java.util.*;

/**
 * Maintains:
 *
 * 1. Overall restaurant ranking
 * 2. Restaurant ranking for every food item
 *
 * Ranking order:
 *
 * 1. Rated restaurants come before unrated restaurants.
 * 2. Higher average rating comes first.
 * 3. If ratings are equal, lexicographically smaller restaurant ID comes first.
 */
public class RankingManager {

    /*
     * Overall sorted ranking.
     *
     * Example:
     *
     * R1 -> 5.0
     * R3 -> 4.8
     * R2 -> 4.8
     * R4 -> Unrated
     */
    private final TreeSet<RestaurantRankingEntry> overallRanking;

    /*
     * Current ranking entry of every restaurant.
     *
     * Required so that we can remove the exact old entry
     * from the TreeSet before inserting the updated entry.
     *
     * restaurantId -> current ranking entry
     */
    private final Map<String, RestaurantRankingEntry> overallEntries;

    /*
     * Food-specific rankings.
     *
     * Example:
     *
     * PIZZA ->
     *      R2 -> 5.0
     *      R1 -> 4.8
     *      R3 -> Unrated
     *
     * BURGER ->
     *      R1 -> 5.0
     *      R4 -> 4.5
     */
    private final Map<FoodItem, TreeSet<RestaurantRankingEntry>> foodRankings;

    /*
     * Current ranking entry for each restaurant
     * for each food item.
     *
     * FoodItem ->
     *      RestaurantId ->
     *          Current Ranking Entry
     */
    private final Map<FoodItem, Map<String, RestaurantRankingEntry>> foodEntries;


    public RankingManager() {
        overallRanking = new TreeSet<>();
        overallEntries = new HashMap<>();
        foodRankings = new HashMap<>();
        foodEntries = new HashMap<>();
    }


    // ============================================================
    // ADD RESTAURANT
    // ============================================================

    /**
     * Adds a restaurant to:
     *
     * 1. Overall ranking
     * 2. Ranking of every food item sold by the restaurant
     *
     * Initially, the restaurant is unrated.
     */
    public void addRestaurant(Restaurant restaurant) {

        String restaurantId = restaurant.getRestaurantId();

        /*
         * Create the initial overall ranking entry.
         */
        RestaurantRankingEntry overallEntry = createOverallEntry(restaurant);
        overallRanking.add(overallEntry);
        overallEntries.put(restaurantId, overallEntry);


        /*
         * Add the restaurant to the ranking
         * of every food item it sells.
         */
        for (FoodItem foodItem : restaurant.getFoodItems()) {
            TreeSet<RestaurantRankingEntry> ranking = foodRankings.computeIfAbsent(foodItem, key -> new TreeSet<>());

            Map<String, RestaurantRankingEntry> entries = foodEntries.computeIfAbsent(foodItem, key -> new HashMap<>());

            RestaurantRankingEntry foodEntry = createFoodEntry(restaurant, foodItem);

            ranking.add(foodEntry);
            entries.put(restaurantId, foodEntry);
        }
    }


    // ============================================================
    // UPDATE OVERALL RESTAURANT RATING
    // ============================================================

    /**
     * Called after the restaurant's overall RatingStats
     * has already been updated.
     *
     * Flow:
     *
     * 1. Get old entry
     * 2. Remove old entry from TreeSet
     * 3. Create new entry using updated rating
     * 4. Add new entry to TreeSet
     * 5. Update the map
     */
    public void updateRestaurantRating(Restaurant restaurant) {

        String restaurantId = restaurant.getRestaurantId();

        /*
         * Get the exact old entry.
         */
        RestaurantRankingEntry oldEntry = overallEntries.get(restaurantId);

        if (oldEntry != null) {
            overallRanking.remove(oldEntry);
        }

        /*
         * Create a new entry using
         * the updated restaurant rating.
         */
        RestaurantRankingEntry newEntry = createOverallEntry(restaurant);

        overallRanking.add(newEntry);
        overallEntries.put(restaurantId, newEntry);
    }


    // ============================================================
    // UPDATE FOOD-SPECIFIC RATING
    // ============================================================

    /**
     * Called after the restaurant's rating for a
     * particular food item has already been updated.
     */
    public void updateFoodRating(Restaurant restaurant, FoodItem foodItem) {

        String restaurantId = restaurant.getRestaurantId();
        TreeSet<RestaurantRankingEntry> ranking = foodRankings.get(foodItem);
        Map<String, RestaurantRankingEntry> entries = foodEntries.get(foodItem);

        /*
         * The restaurant should already exist in this ranking
         * because it must sell the food item.
         */
        if (ranking == null || entries == null) {
            return;
        }

        /*
         * Get and remove the exact old entry.
         */
        RestaurantRankingEntry oldEntry = entries.get(restaurantId);

        if (oldEntry != null) {
            ranking.remove(oldEntry);
        }

        /*
         * Create a new entry using
         * the updated food rating.
         */
        RestaurantRankingEntry newEntry = createFoodEntry(restaurant, foodItem);
        ranking.add(newEntry);
        entries.put(restaurantId, newEntry);
    }


    // ============================================================
    // GET TOP 20 OVERALL RESTAURANTS
    // ============================================================

    public List<String> getTopRatedRestaurants() {
        List<String> result = new ArrayList<>();
        int count = 0;
        for (RestaurantRankingEntry entry : overallRanking) {
            if (count == 20) {
                break;
            }

            result.add(entry.getRestaurantId());
            count++;
        }

        return result;
    }


    // ============================================================
    // GET TOP 20 RESTAURANTS FOR A FOOD ITEM
    // ============================================================

    public List<String> getTopRestaurantsByFood(FoodItem foodItem) {
        List<String> result = new ArrayList<>();
        TreeSet<RestaurantRankingEntry> ranking = foodRankings.get(foodItem);

        if (ranking == null) {
            return result;
        }

        int count = 0;
        for (RestaurantRankingEntry entry : ranking) {
            if (count == 20) {
                break;
            }

            result.add(entry.getRestaurantId());
            count++;
        }

        return result;
    }


    // ============================================================
    // CREATE OVERALL RANKING ENTRY
    // ============================================================

    private RestaurantRankingEntry createOverallEntry(Restaurant restaurant) {
        RatingStats ratingStats = restaurant.getOverallRating();
        return new RestaurantRankingEntry(restaurant.getRestaurantId(), ratingStats.hasRatings(), ratingStats.getAverageRating());
    }


    // ============================================================
    // CREATE FOOD-SPECIFIC RANKING ENTRY
    // ============================================================

    private RestaurantRankingEntry createFoodEntry(Restaurant restaurant, FoodItem foodItem) {
        RatingStats ratingStats = restaurant.getFoodRating(foodItem);
        return new RestaurantRankingEntry(restaurant.getRestaurantId(), ratingStats.hasRatings(), ratingStats.getAverageRating());
    }
}




