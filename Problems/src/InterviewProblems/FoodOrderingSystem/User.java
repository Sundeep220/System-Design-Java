package InterviewProblems.FoodOrderingSystem;

import java.util.*;

public class User {

    private final String userId;
    private final Map<String, Order> orderHistory;

    public User(String userId) {
        this.userId = userId;
        this.orderHistory = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public boolean addOrder(Order order) {
        if (orderHistory.containsKey(order.getOrderId())) {
            return false;
        }

        orderHistory.put(order.getOrderId(), order);

        return true;
    }

    public Order getOrder(String orderId) {
        return orderHistory.get(orderId);
    }

    public boolean rateOrder(String orderId, int rating) {
        Order order = orderHistory.get(orderId);

        if (order == null) {
            return false;
        }

        return order.rate(rating);
    }

    public Collection<Order> getOrderHistory() {
        return Collections.unmodifiableCollection(orderHistory.values());
    }
}
