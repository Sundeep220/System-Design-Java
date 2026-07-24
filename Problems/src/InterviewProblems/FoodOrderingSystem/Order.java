package InterviewProblems.FoodOrderingSystem;

public class Order {

    private final String orderId;
    private final String userId;
    private final Restaurant restaurant;
    private final FoodItem foodItem;
    private boolean rated;
    private int rating;

    public Order(String orderId, String userId, Restaurant restaurant, FoodItem foodItem) {
        this.orderId = orderId;
        this.userId = userId;
        this.restaurant = restaurant;
        this.foodItem = foodItem;
        this.rated = false;
    }

    public boolean rate(int rating) {
        if (this.rated) {
            return false;
        }

        if (rating < 1 || rating > 5) {
            return false;
        }

        this.rating = rating;
        this.rated = true;

        return true;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public boolean isRated() {
        return rated;
    }

    public int getRating() {
        return rating;
    }
}