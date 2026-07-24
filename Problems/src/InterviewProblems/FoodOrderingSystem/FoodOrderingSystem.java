package InterviewProblems.FoodOrderingSystem;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodOrderingSystem {

    private Map<String, User> users;
    private Map<String, Restaurant> restaurants;
    private Map<String, FoodItem> foodItems;
    private RankingManager rankingManager;

    // Initialize / reset the system
    public void init() {
        users = new HashMap<>();
        restaurants = new HashMap<>();
        foodItems = new HashMap<>();
        rankingManager = new RankingManager();
    }

    // ------------------------------------------------------------
    // Add Food Item
    // ------------------------------------------------------------

    public boolean addFoodItem(FoodItem foodItem) {
        if (foodItem == null) {
            return false;
        }

        String foodItemId = foodItem.getFoodItemId();
        if (foodItems.containsKey(foodItemId)) {
            return false;
        }
        foodItems.put(foodItemId, foodItem);
        return true;
    }

    // ------------------------------------------------------------
    // Add Restaurant
    // ------------------------------------------------------------
    public boolean addRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            return false;
        }

        String restaurantId = restaurant.getRestaurantId();
        if (restaurants.containsKey(restaurantId)) {
            return false;
        }

        restaurants.put(restaurantId, restaurant);

        /*
         * Initially, the restaurant is unrated.
         * It must still be added to the ranking manager because
         * unrated restaurants are also part of the ranking.
         */
        rankingManager.addRestaurant(restaurant);

        return true;
    }

    // ------------------------------------------------------------
    // Add User
    // ------------------------------------------------------------
    public boolean addUser(User user) {
        if (user == null) {
            return false;
        }

        String userId = user.getUserId();
        if (users.containsKey(userId)) {
            return false;
        }

        users.put(userId, user);
        return true;
    }

    // ------------------------------------------------------------
    // Order Food
    // ------------------------------------------------------------
    public boolean orderFood(String orderId, String userId, String restaurantId, String foodItemId) {
        // 1. Find the user
        User user = users.get(userId);

        if (user == null) {
            return false;
        }

        // 2. Find the restaurant
        Restaurant restaurant = restaurants.get(restaurantId);

        if (restaurant == null) {
            return false;
        }

        // 3. Find the food item
        FoodItem foodItem = foodItems.get(foodItemId);

        if (foodItem == null) {
            return false;
        }

        // 4. Check whether the restaurant sells this food item
        if (!restaurant.sellsFood(foodItem)) {
            return false;
        }

        /*
         * 5. Create the order
         *
         * The order stores:
         * - orderId
         * - userId
         * - restaurant
         * - foodItem
         */
        Order order = new Order(orderId, userId, restaurant, foodItem);

        /*
         * 6. User owns the order history,
         * so the order is added to the user.
         */
        return user.addOrder(order);
    }

    // ------------------------------------------------------------
    // Rate Order
    // ------------------------------------------------------------

    public boolean rateOrder(String userId, String orderId, int rating) {

        // 1. Find the user
        User user = users.get(userId);

        if (user == null) {
            return false;
        }

        // 2. Find the order through the user
        Order order = user.getOrder(orderId);

        if (order == null) {
            return false;
        }

        /*
         * 3. Ask the Order to validate and record the rating.
         *
         * The Order checks:
         * - rating is between 1 and 5
         * - order has not already been rated
         */
        if (!order.rate(rating)) {
            return false;
        }

        // 4. Get the restaurant and food item from the order
        Restaurant restaurant = order.getRestaurant();

        FoodItem foodItem = order.getFoodItem();

        /*
         * 5. Update the restaurant's overall rating
         */
        restaurant.addRating(rating);

        /*
         * 6. Update the restaurant's rating
         * for this specific food item
         */
        restaurant.addFoodRating(foodItem, rating);

        /*
         * 7. Notify RankingManager so it can
         * update the sorted ranking structures.
         */
        rankingManager.updateRestaurantRating(restaurant);
        rankingManager.updateFoodRating(restaurant, foodItem);

        return true;
    }

    // ------------------------------------------------------------
    // Ranking Queries
    // ------------------------------------------------------------

    public List<String> getTopRatedRestaurants() {
        return rankingManager.getTopRatedRestaurants();
    }

    public List<String> getTopRestaurantsByFood(String foodItemId) {

        FoodItem foodItem = foodItems.get(foodItemId);

        if (foodItem == null) {
            return List.of();
        }

        return rankingManager.getTopRestaurantsByFood(foodItem);
    }
}
