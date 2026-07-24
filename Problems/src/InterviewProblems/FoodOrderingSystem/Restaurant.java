package InterviewProblems.FoodOrderingSystem;

import java.util.*;

public class Restaurant {

    private final String restaurantId;
    private final Set<FoodItem> foodItems;
    private final RatingStats overallRating;
    private final Map<FoodItem, RatingStats> foodRatings;

    public Restaurant(String restaurantId) {
        this.restaurantId = restaurantId;
        this.foodItems = new HashSet<>();
        this.overallRating = new RatingStats();
        this.foodRatings = new HashMap<>();
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public boolean addFoodItem(FoodItem foodItem) {
        if (!foodItems.add(foodItem)) {
            return false;
        }

        foodRatings.put(foodItem, new RatingStats());
        return true;
    }

    public boolean sellsFood(FoodItem foodItem) {
        return foodItems.contains(foodItem);
    }

    public void addRating(int rating) {
        overallRating.addRating(rating);
    }

    public void addFoodRating(FoodItem foodItem, int rating) {
        RatingStats stats = foodRatings.get(foodItem);

        if (stats == null) {
            throw new IllegalArgumentException("Restaurant does not sell this food item");
        }

        stats.addRating(rating);
    }

    public RatingStats getOverallRating() {
        return overallRating;
    }

    public RatingStats getFoodRating(FoodItem foodItem) {
        return foodRatings.get(foodItem);
    }

    public Set<FoodItem> getFoodItems() {
        return Collections.unmodifiableSet(foodItems);
    }
}