package InterviewProblems.FoodOrderingSystem;

public class FoodItem {

    private final String foodItemId;

    public FoodItem(String foodItemId) {
        this.foodItemId = foodItemId;
    }

    public String getFoodItemId() {
        return foodItemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof FoodItem foodItem)) {
            return false;
        }

        return foodItemId.equals(foodItem.foodItemId);
    }

    @Override
    public int hashCode() {
        return foodItemId.hashCode();
    }
}