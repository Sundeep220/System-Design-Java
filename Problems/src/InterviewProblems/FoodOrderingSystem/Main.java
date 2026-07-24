package InterviewProblems.FoodOrderingSystem;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        // ========================================================
        // 1. INITIALIZE SYSTEM
        // ========================================================

        FoodOrderingSystem system = new FoodOrderingSystem();

        system.init();

        System.out.println(
                "===== SYSTEM INITIALIZED ====="
        );


        // ========================================================
        // 2. CREATE FOOD ITEMS
        // ========================================================

        FoodItem pizza =
                new FoodItem("PIZZA");

        FoodItem burger =
                new FoodItem("BURGER");

        FoodItem biryani =
                new FoodItem("BIRYANI");

        FoodItem pasta =
                new FoodItem("PASTA");


        // Add food items to the system

        System.out.println(
                "\n===== ADD FOOD ITEMS ====="
        );

        System.out.println(
                "Add PIZZA: "
                        + system.addFoodItem(pizza)
        );

        System.out.println(
                "Add BURGER: "
                        + system.addFoodItem(burger)
        );

        System.out.println(
                "Add BIRYANI: "
                        + system.addFoodItem(biryani)
        );

        System.out.println(
                "Add PASTA: "
                        + system.addFoodItem(pasta)
        );


        // Duplicate food item

        FoodItem duplicatePizza =
                new FoodItem("PIZZA");

        System.out.println(
                "Add duplicate PIZZA: "
                        + system.addFoodItem(duplicatePizza)
        );


        // ========================================================
        // 3. CREATE RESTAURANTS
        // ========================================================

        Restaurant r1 =
                new Restaurant("R1");

        Restaurant r2 =
                new Restaurant("R2");

        Restaurant r3 =
                new Restaurant("R3");

        Restaurant r4 =
                new Restaurant("R4");


        // ========================================================
        // 4. RESTAURANTS ADD THEIR OWN FOOD ITEMS
        // ========================================================

        /*
         * R1 sells:
         *
         * PIZZA
         * BURGER
         */
        r1.addFoodItem(pizza);
        r1.addFoodItem(burger);


        /*
         * R2 sells:
         *
         * PIZZA
         * BIRYANI
         */
        r2.addFoodItem(pizza);
        r2.addFoodItem(biryani);


        /*
         * R3 sells:
         *
         * PIZZA
         * BURGER
         * BIRYANI
         */
        r3.addFoodItem(pizza);
        r3.addFoodItem(burger);
        r3.addFoodItem(biryani);


        /*
         * R4 sells:
         *
         * PASTA
         */
        r4.addFoodItem(pasta);


        // ========================================================
        // 5. ADD RESTAURANTS
        // ========================================================

        System.out.println(
                "\n===== ADD RESTAURANTS ====="
        );

        System.out.println(
                "Add R1: "
                        + system.addRestaurant(r1)
        );

        System.out.println(
                "Add R2: "
                        + system.addRestaurant(r2)
        );

        System.out.println(
                "Add R3: "
                        + system.addRestaurant(r3)
        );

        System.out.println(
                "Add R4: "
                        + system.addRestaurant(r4)
        );


        /*
         * Test duplicate restaurant.
         */
        Restaurant duplicateR1 =
                new Restaurant("R1");

        System.out.println(
                "Add duplicate R1: "
                        + system.addRestaurant(duplicateR1)
        );


        // ========================================================
        // 6. CREATE USERS
        // ========================================================

        User u1 =
                new User("U1");

        User u2 =
                new User("U2");

        User u3 =
                new User("U3");


        // ========================================================
        // 7. ADD USERS
        // ========================================================

        System.out.println(
                "\n===== ADD USERS ====="
        );

        System.out.println(
                "Add U1: "
                        + system.addUser(u1)
        );

        System.out.println(
                "Add U2: "
                        + system.addUser(u2)
        );

        System.out.println(
                "Add U3: "
                        + system.addUser(u3)
        );


        /*
         * Test duplicate user.
         */
        User duplicateU1 =
                new User("U1");

        System.out.println(
                "Add duplicate U1: "
                        + system.addUser(duplicateU1)
        );


        // ========================================================
        // 8. INITIAL RANKING
        // ========================================================

        System.out.println(
                "\n===== INITIAL RANKING ====="
        );

        printList(
                "Overall",
                system.getTopRatedRestaurants()
        );

        printList(
                "PIZZA",
                system.getTopRestaurantsByFood(
                        "PIZZA"
                )
        );

        printList(
                "BURGER",
                system.getTopRestaurantsByFood(
                        "BURGER"
                )
        );

        printList(
                "BIRYANI",
                system.getTopRestaurantsByFood(
                        "BIRYANI"
                )
        );

        printList(
                "PASTA",
                system.getTopRestaurantsByFood(
                        "PASTA"
                )
        );


        // ========================================================
        // 9. PLACE ORDERS
        // ========================================================

        System.out.println(
                "\n===== PLACE ORDERS ====="
        );


        /*
         * U1 orders PIZZA from R1.
         */
        System.out.println(
                "O1: "
                        + system.orderFood(
                        "O1",
                        "U1",
                        "R1",
                        "PIZZA"
                )
        );


        /*
         * Same user orders BURGER
         * from the same restaurant.
         */
        System.out.println(
                "O2: "
                        + system.orderFood(
                        "O2",
                        "U1",
                        "R1",
                        "BURGER"
                )
        );


        /*
         * Same user orders from
         * a different restaurant.
         */
        System.out.println(
                "O3: "
                        + system.orderFood(
                        "O3",
                        "U1",
                        "R2",
                        "BIRYANI"
                )
        );


        /*
         * U2 orders PIZZA from R2.
         */
        System.out.println(
                "O4: "
                        + system.orderFood(
                        "O4",
                        "U2",
                        "R2",
                        "PIZZA"
                )
        );


        /*
         * U3 orders BURGER from R3.
         */
        System.out.println(
                "O5: "
                        + system.orderFood(
                        "O5",
                        "U3",
                        "R3",
                        "BURGER"
                )
        );


        // ========================================================
        // 10. INVALID ORDER TESTS
        // ========================================================

        System.out.println(
                "\n===== INVALID ORDER TESTS ====="
        );


        /*
         * Non-existing user.
         */
        System.out.println(
                "Invalid user: "
                        + system.orderFood(
                        "O6",
                        "INVALID_USER",
                        "R1",
                        "PIZZA"
                )
        );


        /*
         * Non-existing restaurant.
         */
        System.out.println(
                "Invalid restaurant: "
                        + system.orderFood(
                        "O7",
                        "U1",
                        "INVALID_RESTAURANT",
                        "PIZZA"
                )
        );


        /*
         * Non-existing food item.
         */
        System.out.println(
                "Invalid food item: "
                        + system.orderFood(
                        "O8",
                        "U1",
                        "R1",
                        "INVALID_FOOD"
                )
        );


        /*
         * Restaurant does not sell this food.
         *
         * R1 does not sell BIRYANI.
         */
        System.out.println(
                "Restaurant does not sell food: "
                        + system.orderFood(
                        "O9",
                        "U1",
                        "R1",
                        "BIRYANI"
                )
        );


        /*
         * Duplicate order ID for the same user.
         */
        System.out.println(
                "Duplicate order ID: "
                        + system.orderFood(
                        "O1",
                        "U1",
                        "R1",
                        "PIZZA"
                )
        );


        // ========================================================
        // 11. RATE ORDERS
        // ========================================================

        System.out.println(
                "\n===== RATE ORDERS ====="
        );


        /*
         * R1:
         *
         * PIZZA = 5
         * Overall = 5
         */
        System.out.println(
                "Rate O1 with 5: "
                        + system.rateOrder(
                        "U1",
                        "O1",
                        5
                )
        );


        /*
         * R1:
         *
         * BURGER = 4
         * Overall ratings:
         *
         * 5
         * 4
         *
         * Average = 4.5
         */
        System.out.println(
                "Rate O2 with 4: "
                        + system.rateOrder(
                        "U1",
                        "O2",
                        4
                )
        );


        /*
         * R2:
         *
         * BIRYANI = 3
         * Overall = 3
         */
        System.out.println(
                "Rate O3 with 3: "
                        + system.rateOrder(
                        "U1",
                        "O3",
                        3
                )
        );


        /*
         * R2:
         *
         * PIZZA = 5
         * Overall ratings:
         *
         * 3
         * 5
         *
         * Average = 4.0
         */
        System.out.println(
                "Rate O4 with 5: "
                        + system.rateOrder(
                        "U2",
                        "O4",
                        5
                )
        );


        /*
         * R3:
         *
         * BURGER = 4
         * Overall = 4
         */
        System.out.println(
                "Rate O5 with 4: "
                        + system.rateOrder(
                        "U3",
                        "O5",
                        4
                )
        );


        // ========================================================
        // 12. RATING SAME ORDER TWICE
        // ========================================================

        System.out.println(
                "\n===== DUPLICATE RATING TEST ====="
        );

        System.out.println(
                "Rate O1 again: "
                        + system.rateOrder(
                        "U1",
                        "O1",
                        1
                )
        );


        // ========================================================
        // 13. INVALID RATING TESTS
        // ========================================================

        System.out.println(
                "\n===== INVALID RATING TESTS ====="
        );


        /*
         * Rating less than 1.
         */
        System.out.println(
                "Rating 0: "
                        + system.rateOrder(
                        "U1",
                        "O1",
                        0
                )
        );


        /*
         * Rating greater than 5.
         */
        System.out.println(
                "Rating 6: "
                        + system.rateOrder(
                        "U1",
                        "O1",
                        6
                )
        );


        /*
         * Non-existing order.
         */
        System.out.println(
                "Invalid order: "
                        + system.rateOrder(
                        "U1",
                        "INVALID_ORDER",
                        5
                )
        );


        /*
         * User U2 tries to rate U1's order.
         *
         * This should fail because orders belong
         * to the user who placed them.
         */
        System.out.println(
                "Wrong user rates O1: "
                        + system.rateOrder(
                        "U2",
                        "O1",
                        5
                )
        );


        /*
         * Non-existing user.
         */
        System.out.println(
                "Invalid user rates order: "
                        + system.rateOrder(
                        "INVALID_USER",
                        "O1",
                        5
                )
        );


        // ========================================================
        // 14. CHECK OVERALL RANKING
        // ========================================================

        System.out.println(
                "\n===== FINAL OVERALL RANKING ====="
        );

        printList(
                "Overall",
                system.getTopRatedRestaurants()
        );


        /*
         * Expected:
         *
         * R1 -> 4.5
         * R3 -> 4.0
         * R2 -> 4.0
         * R4 -> Unrated
         *
         * Since R2 and R3 both have 4.0:
         *
         * R2 < R3 lexicographically
         *
         * Therefore:
         *
         * R1
         * R2
         * R3
         * R4
         */


        // ========================================================
        // 15. CHECK FOOD-SPECIFIC RANKING
        // ========================================================

        System.out.println(
                "\n===== PIZZA RANKING ====="
        );

        printList(
                "PIZZA",
                system.getTopRestaurantsByFood(
                        "PIZZA"
                )
        );


        /*
         * Expected:
         *
         * R1 -> 5.0
         * R2 -> 5.0
         *
         * Tie:
         *
         * R1 < R2
         *
         * Result:
         *
         * R1
         * R2
         */


        System.out.println(
                "\n===== BURGER RANKING ====="
        );

        printList(
                "BURGER",
                system.getTopRestaurantsByFood(
                        "BURGER"
                )
        );


        /*
         * Expected:
         *
         * R1 -> 4.0
         * R3 -> 4.0
         *
         * Tie:
         *
         * R1 < R3
         */


        System.out.println(
                "\n===== BIRYANI RANKING ====="
        );

        printList(
                "BIRYANI",
                system.getTopRestaurantsByFood(
                        "BIRYANI"
                )
        );


        System.out.println(
                "\n===== PASTA RANKING ====="
        );

        printList(
                "PASTA",
                system.getTopRestaurantsByFood(
                        "PASTA"
                )
        );


        // ========================================================
        // 16. UNKNOWN FOOD RANKING
        // ========================================================

        System.out.println(
                "\n===== UNKNOWN FOOD RANKING ====="
        );

        printList(
                "UNKNOWN FOOD",
                system.getTopRestaurantsByFood(
                        "NOODLES"
                )
        );


        // ========================================================
        // 17. USER ORDER HISTORY
        // ========================================================

        System.out.println(
                "\n===== USER ORDER HISTORY ====="
        );

        printUserOrders(
                "U1",
                u1
        );

        printUserOrders(
                "U2",
                u2
        );

        printUserOrders(
                "U3",
                u3
        );
    }


    // ============================================================
    // HELPER: PRINT RESTAURANT LIST
    // ============================================================

    private static void printList(
            String title,
            List<String> restaurants
    ) {

        System.out.println(
                title + ": " + restaurants
        );
    }


    // ============================================================
    // HELPER: PRINT USER ORDER HISTORY
    // ============================================================

    private static void printUserOrders(
            String userId,
            User user
    ) {

        System.out.println(
                userId + " order history:"
        );

        for (Order order :
                user.getOrderHistory()) {

            System.out.println(
                    "  Order ID: "
                            + order.getOrderId()
                            + ", Restaurant: "
                            + order.getRestaurant()
                            .getRestaurantId()
                            + ", Food: "
                            + order.getFoodItem()
                            .getFoodItemId()
                            + ", Rated: "
                            + order.isRated()
                            + ", Rating: "
                            + order.getRating()
            );
        }
    }
}

